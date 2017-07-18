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

import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import jolie.runtime.ValueVector;

public class MqttProtocol extends AsyncCommProtocol {

    private static final MqttVersion mqttVersion = MqttVersion.MQTT_3_1_1;
    final boolean inputPort;

    public MqttProtocol(boolean inputPort, VariablePath configurationPath) {
	super(configurationPath);
	this.inputPort = inputPort;
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
		mqttVersion.protocolName(),
		mqttVersion.protocolLevel(),
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
    String getCurrentTopic(String operationName) {
	return hasOperationSpecificParameter(operationName, Parameters.ALIAS) ? getOperationSpecificStringParameter(operationName, Parameters.ALIAS) : operationName;
    }

    MqttQoS getCurrentQos(String operationName) {
	return hasOperationSpecificParameter(operationName, Parameters.QOS) ? MqttQoS.valueOf(getOperationSpecificParameterFirstValue(operationName, Parameters.QOS).intValue()) : MqttQoS.AT_LEAST_ONCE;
    }

    boolean connectionAccepted(MqttMessage msg) {
	return msg.fixedHeader().messageType().equals(MqttMessageType.CONNACK) && ((MqttConnAckMessage) msg).variableHeader().connectReturnCode().equals(MqttConnectReturnCode.CONNECTION_ACCEPTED);
    }

    String getCurrentOperationName(String topic) {
	for (Iterator<Map.Entry<String, ValueVector>> it = configurationPath().getValue().children().entrySet().iterator(); it.hasNext();) {
	    Map.Entry<String, ValueVector> i = it.next();
	    if (i.getKey().equals("osc")) {
		for (Iterator<Map.Entry<String, ValueVector>> it1 = i.getValue().first().children().entrySet().iterator(); it1.hasNext();) {
		    Map.Entry<String, ValueVector> j = it1.next();
		    for (Iterator<Map.Entry<String, ValueVector>> it2 = j.getValue().first().children().entrySet().iterator(); it2.hasNext();) {
			Map.Entry<String, ValueVector> k = it2.next();
			if (k.getKey().equals("alias") && k.getValue().first().strValue().equals(topic)) {
			    return j.getKey();
			}
			it2.remove();
		    }
		    it1.remove();
		}
	    }
	    it.remove();
	}
	return topic;
    }
}
