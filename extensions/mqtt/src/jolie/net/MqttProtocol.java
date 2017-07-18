/*
 * Copyright (C) 2017 stefanopiozingaro
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either mqttVersion 3 of the License, or
 * (at your option) any later mqttVersion.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package jolie.net;

import jolie.net.protocols.AsyncCommProtocol;
import jolie.runtime.VariablePath;

import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectPayload;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttConnectVariableHeader;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttVersion;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jolie.Interpreter;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;

public class MqttProtocol extends AsyncCommProtocol {

    final boolean inputPort;
    final VariablePath configurationPath;

    public MqttProtocol(boolean inputPort, VariablePath configurationPath) {
	super(configurationPath);
	this.inputPort = inputPort;
	this.configurationPath = configurationPath;
    }

    @Override
    public void setupPipeline(ChannelPipeline p) {
	p.addLast(new LoggingHandler(LogLevel.INFO));
	p.addLast(MqttEncoder.INSTANCE);
	p.addLast(new MqttDecoder());
	if (inputPort) {
	    p.addLast("Ping", new PingHandler());
	    p.addLast(new SubscriptionHandler(this, channel().parentInputPort()));
	} else {
	    p.addLast(new PublicationHandler(this));
	}

    }

    @Override
    public String name() {
	return "mqtt";
    }

    @Override
    public boolean isThreadSafe() {
	return checkBooleanParameter(Parameters.CONCURRENT);
    }

    public VariablePath getConfigurationPath() {
	return configurationPath;
    }

    /**
     * TODO isWillRetain is set to false by default; connection qos is set to 0;
     * will qos is set to 0 by default; remaining length is not 0
     *
     * @param isDup boolean for future connection refused purpose
     * @return MqttConnectMessage
     */
    public MqttConnectMessage buildConnect(boolean isDup) {

	Random random = new Random();
	String clientId = "jolie-mqtt/";
	String[] options = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".split("");
	for (int i = 0; i < 8; i++) {
	    clientId += options[random.nextInt(options.length)];
	}

	MqttFixedHeader mfh = new MqttFixedHeader(
		MqttMessageType.CONNECT,
		isDup,
		MqttQoS.AT_MOST_ONCE,
		false,
		0);
	MqttConnectVariableHeader vh = new MqttConnectVariableHeader(
		Parameters.MQTT_VERSION.protocolName(),
		Parameters.MQTT_VERSION.protocolLevel(),
		checkBooleanParameter(Parameters.USERNAME),
		checkBooleanParameter(Parameters.PASSWORD),
		false,
		MqttQoS.AT_MOST_ONCE.value(),
		checkBooleanParameter(Parameters.WILL_TOPIC),
		true,
		2);
	MqttConnectPayload p = new MqttConnectPayload(
		clientId,
		getStringParameter(Parameters.WILL_TOPIC),
		getStringParameter(Parameters.WILL_MESSAGE),
		getStringParameter(Parameters.USERNAME),
		getStringParameter(Parameters.PASSWORD));

	return new MqttConnectMessage(mfh, vh, p);
    }

    /*
    PACKAGE METHODS
     */
    String getCurrentTopic(CommMessage message) {
	StringBuilder sb = new StringBuilder();

	if (hasOperationSpecificParameter(message.operationName(), Parameters.ALIAS)) {
	    String alias = getOperationSpecificStringParameter(message.operationName(), Parameters.ALIAS);
	    try {
		send_appendParsedAlias(alias, message.value(), sb);
	    } catch (IOException ex) {
		Interpreter.getInstance().logWarning(message.fault().getCause());
	    }
	} else {
	    sb.append(message.operationName());
	}

	return sb.toString();
    }

    MqttQoS getQos(MqttQoS defaultQoS) {
	return hasParameter(Parameters.QOS) ? MqttQoS.valueOf(getIntParameter(Parameters.QOS)) : defaultQoS;
    }

    MqttQoS getCurrentOperationQos(String operationName, MqttQoS defaultQoS) {
	return hasOperationSpecificParameter(operationName, Parameters.QOS) ? MqttQoS.valueOf(getOperationSpecificParameterFirstValue(operationName, Parameters.QOS).intValue()) : defaultQoS;
    }

    boolean connectionAccepted(MqttMessage msg) {
	return msg.fixedHeader().messageType().equals(MqttMessageType.CONNACK) && ((MqttConnAckMessage) msg).variableHeader().connectReturnCode().equals(MqttConnectReturnCode.CONNECTION_ACCEPTED);
    }

    String getCurrentOperationName(String topic) {
	if (configurationPath.getValue().hasChildren("osc")) {
	    for (Map.Entry<String, ValueVector> i : configurationPath.getValue().getFirstChild("osc").children().entrySet()) {
		for (Map.Entry<String, ValueVector> j : i.getValue().first().children().entrySet()) {
		    if (j.getKey().equals("alias") && j.getValue().first().strValue().equals(topic)) {
			return i.getKey();
		    }
		}
	    }
	}
	return topic;
    }

    private static void send_appendParsedAlias(String alias, Value value, StringBuilder builder)
	    throws IOException {
	int offset = 0;
	List< String> aliasKeys = new ArrayList<>();
	String currStrValue;
	String currKey;
	StringBuilder result = new StringBuilder(alias);
	Matcher m = Pattern.compile("%(!)?\\{[^\\}]*\\}").matcher(alias);

	while (m.find()) {
	    if (m.group(1) == null) { // ! is missing after %: We have to use URLEncoder
		currKey = alias.substring(m.start() + 2, m.end() - 1);
		if ("$".equals(currKey)) {
		    currStrValue = URLEncoder.encode(value.strValue(), "UTF-8");
		} else {
		    currStrValue = URLEncoder.encode(value.getFirstChild(currKey).strValue(), "UTF-8");
		    aliasKeys.add(currKey);
		}
	    } else { // ! is given after %: We have to insert the string raw
		currKey = alias.substring(m.start() + 3, m.end() - 1);
		if ("$".equals(currKey)) {
		    currStrValue = value.strValue();
		} else {
		    currStrValue = value.getFirstChild(currKey).strValue();
		    aliasKeys.add(currKey);
		}
	    }

	    result.replace(
		    m.start() + offset, m.end() + offset,
		    currStrValue
	    );
	    offset += currStrValue.length() - 3 - currKey.length();
	}
	// removing used keys
	for (String aliasKey : aliasKeys) {
	    value.children().remove(aliasKey);
	}
	builder.append(result);
    }
}
