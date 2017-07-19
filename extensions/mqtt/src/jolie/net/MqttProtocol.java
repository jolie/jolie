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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import jolie.net.mqtt.PublicationHandler;
import jolie.net.mqtt.SubscriptionHandler;
import jolie.net.mqtt.PingHandler;
import jolie.net.mqtt.Parameters;
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
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttSubscribePayload;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.CharsetUtil;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.typing.OneWayTypeDescription;
import jolie.runtime.typing.RequestResponseTypeDescription;

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
	    p.addLast(new SubscriptionHandler(this));
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

    public MqttPublishMessage buildPublication(long messageId, String topic, Value message, boolean isDup, MqttQoS pubQos) {

	MqttFixedHeader mfh = new MqttFixedHeader(
		MqttMessageType.PUBLISH,
		isDup,
		pubQos,
		false,
		0);
	MqttPublishVariableHeader vh = new MqttPublishVariableHeader(topic, (int) messageId);
	ByteBuf bb = Unpooled.wrappedBuffer(parseValue(message).getBytes(CharsetUtil.UTF_8));

	return new MqttPublishMessage(mfh, vh, bb);
    }

    private String parseValue(Value value) {
	if (value.isInt()) {
	    return String.valueOf(value.intValue());
	}
	if (value.isBool()) {
	    return String.valueOf(value.boolValue());
	}
	if (value.isDouble()) {
	    return String.valueOf(value.doubleValue());
	}
	if (value.isLong()) {
	    return String.valueOf(value.longValue());
	}
	return value.strValue();
    }

    public MqttSubscribeMessage buildSubscription(long messageId, List<String> topics, boolean isDup, MqttQoS subQos) {

	List<MqttTopicSubscription> tmsL = new ArrayList<>();
	for (String t : topics) {
	    tmsL.add(new MqttTopicSubscription(t, getCurrentOperationQos(getCurrentOperationName(t), MqttQoS.EXACTLY_ONCE)));
	}
	MqttFixedHeader mfh = new MqttFixedHeader(
		MqttMessageType.SUBSCRIBE,
		isDup,
		subQos,
		false,
		0);
	MqttMessageIdVariableHeader vh = MqttMessageIdVariableHeader.from((int) messageId);
	MqttSubscribePayload p = new MqttSubscribePayload(tmsL);

	return new MqttSubscribeMessage(mfh, vh, p);
    }

    public String getCurrentTopic(CommMessage message) {

	StringBuilder sb = new StringBuilder();

	if (hasOperationSpecificParameter(message.operationName(), Parameters.ALIAS)) {
	    int offset = 0;
	    List< String> aliasKeys = new ArrayList<>();
	    String currStrValue;
	    String currKey;
	    String alias = getOperationSpecificStringParameter(message.operationName(), Parameters.ALIAS);
	    StringBuilder result = new StringBuilder(alias);
	    Matcher m = Pattern.compile("%(!)?\\{[^\\}]*\\}").matcher(alias);

	    while (m.find()) {
		currKey = alias.substring(m.start() + 3, m.end() - 1);
		currStrValue = message.value().getFirstChild(currKey).strValue();
		aliasKeys.add(currKey);
		result.replace(
			m.start() + offset, m.end() + offset,
			currStrValue
		);
		offset += currStrValue.length() - 3 - currKey.length();
	    }

	    for (String aliasKey : aliasKeys) {
		message.value().children().remove(aliasKey);
	    }
	    sb.append(result);

	} else {
	    sb.append(message.operationName());
	}

	return sb.toString();
    }

    public MqttQoS getQos(MqttQoS defaultQoS) {

	return hasParameter(Parameters.QOS) ? MqttQoS.valueOf(getIntParameter(Parameters.QOS)) : defaultQoS;
    }

    public MqttQoS getCurrentOperationQos(String operationName, MqttQoS defaultQoS) {

	return hasOperationSpecificParameter(operationName, Parameters.QOS) ? MqttQoS.valueOf(getOperationSpecificParameterFirstValue(operationName, Parameters.QOS).intValue()) : defaultQoS;
    }

    public boolean connectionAccepted(MqttMessage msg) {

	return msg.fixedHeader().messageType().equals(MqttMessageType.CONNACK) && ((MqttConnAckMessage) msg).variableHeader().connectReturnCode().equals(MqttConnectReturnCode.CONNECTION_ACCEPTED);
    }

    public String getCurrentOperationName(String topic) {

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

    public boolean isSubscriptionOnDemand(boolean defaultSubscriptionOnDemand) {
	return hasParameter(Parameters.SUBSCRIPTION_ON_DEMAND) ? Boolean.getBoolean(getStringParameter(Parameters.SUBSCRIPTION_ON_DEMAND)) : defaultSubscriptionOnDemand;
    }

    public List<String> getTopicList() {

	List<String> opL = new ArrayList<>();
	for (Map.Entry<String, OneWayTypeDescription> owon : channel().parentPort().getInterface().oneWayOperations().entrySet()) {
	    opL.add(getAliasForOperation(owon.getKey()));

	}
	for (Map.Entry<String, RequestResponseTypeDescription> rron : channel().parentPort().getInterface().requestResponseOperations().entrySet()) {
	    opL.add(getAliasForOperation(rron.getKey()));

	}
	return opL;
    }

    public String getAliasForOperation(String operationName) {

	for (Iterator<Map.Entry<String, ValueVector>> it = getConfigurationPath().getValue().getFirstChild("osc").children().entrySet().iterator(); it.hasNext();) {
	    Map.Entry<String, ValueVector> i = it.next();
	    if (operationName.equals(i.getKey())) {
		return i.getValue().first().getFirstChild("alias").strValue();
	    }
	}
	return operationName;
    }
}
