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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectPayload;
import io.netty.handler.codec.mqtt.MqttConnectVariableHeader;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttSubscribePayload;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import io.netty.handler.codec.mqtt.MqttVersion;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.CharsetUtil;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import jolie.runtime.Value;
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
	//p.addLast(new LoggingHandler(LogLevel.INFO));
	p.addLast(new ConnectionHandler(configurationPath()));
	p.addLast(MqttEncoder.INSTANCE);
	p.addLast(new MqttDecoder());
	p.addLast("Ping", new PingHandler());
	p.addLast(new CodecHandler(this));
    }

    @Override
    public String name() {
	return "mqtt";
    }

    @Override
    public boolean isThreadSafe() {
	return checkBooleanParameter(Parameters.CONCURRENT);
    }

    private static class Parameters {

	private static final String BROKER = "broker";
	private static final String CONCURRENT = "concurrent";
	private static final String ALIAS = "alias";
	private static final String QOS = "QoS";
	private static final String WILL_TOPIC = "willTopic";
	private static final String WILL_MESSAGE = "willMessage";
	private static final String USERNAME = "username";
	private static final String PASSWORD = "password";
	private static final String FORMAT = "format";
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

    /**
     * TODO is duplicate set to 0 by default; implement serialization for
     * message; remaining lentgh not 0
     *
     * @param messageId long
     * @param topic String
     * @param message String
     * @param isDup boolean
     * @param pubQos MqttQoS
     * @return MqttPublishMessage
     */
    public MqttPublishMessage buildPublication(long messageId, String topic, Value message, boolean isDup, MqttQoS pubQos) {

	MqttFixedHeader mfh = new MqttFixedHeader(
		MqttMessageType.PUBLISH,
		isDup,
		pubQos,
		false,
		0);
	MqttPublishVariableHeader vh = new MqttPublishVariableHeader(topic, (int) messageId);

	return new MqttPublishMessage(mfh, vh, parseValue(message));
    }

    private ByteBuf parseValue(Value value) {
	if (value.isInt()) {
	    return Unpooled.wrappedBuffer(String.valueOf(value.intValue()).getBytes(CharsetUtil.UTF_8));
	}
	if (value.isBool()) {
	    return Unpooled.wrappedBuffer(String.valueOf(value.boolValue()).getBytes(CharsetUtil.UTF_8));
	}
	if (value.isDouble()) {
	    return Unpooled.wrappedBuffer(String.valueOf(value.doubleValue()).getBytes(CharsetUtil.UTF_8));
	}
	if (value.isLong()) {
	    return Unpooled.wrappedBuffer(String.valueOf(value.longValue()).getBytes(CharsetUtil.UTF_8));
	}
	return Unpooled.wrappedBuffer(value.strValue().getBytes(CharsetUtil.UTF_8));
    }

    /**
     * TODO remaining lentgh not 0; maximum qos for every topic is set to
     * exactly once by default;
     *
     * @param messageId long
     * @param topic String
     * @param isDup boolean
     * @param subQos MqttQoS
     * @return MqttSubscribeMessage
     */
    public MqttSubscribeMessage buildSubscription(long messageId, String topic, boolean isDup, MqttQoS subQos) {

	MqttFixedHeader mfh = new MqttFixedHeader(
		MqttMessageType.SUBSCRIBE,
		false,
		subQos,
		false,
		0);
	MqttMessageIdVariableHeader vh = MqttMessageIdVariableHeader.from((int) messageId);
	MqttSubscribePayload p = new MqttSubscribePayload(
		Collections.singletonList(
			new MqttTopicSubscription(topic, MqttQoS.EXACTLY_ONCE)));

	return new MqttSubscribeMessage(mfh, vh, p);
    }

    /*
    PACKAGE METHODS
     */
    String getCurrentTopic(CommMessage message) {
	return hasOperationSpecificParameter(message.operationName(), Parameters.ALIAS) ? getOperationSpecificStringParameter(message.operationName(), Parameters.ALIAS) : message.operationName();
    }

    MqttQoS getCurrentQos(CommMessage message) {
	return hasOperationSpecificParameter(message.operationName(), MqttProtocol.Parameters.QOS) ? MqttQoS.valueOf(getOperationSpecificParameterFirstValue(message.operationName(), MqttProtocol.Parameters.QOS).intValue()) : MqttQoS.AT_LEAST_ONCE;
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
