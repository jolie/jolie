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
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.MessageToMessageCodec;
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
import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttSubscribePayload;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import io.netty.handler.codec.mqtt.MqttVersion;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import jolie.runtime.Value;

/**
 *
 * @author stefanopiozingaro
 */
public class MqttProtocol extends AsyncCommProtocol {

    private static final MqttVersion mqttVersion = MqttVersion.MQTT_3_1_1;
    private final List<MqttPublishMessage> pendingPublishes;
    private final List<MqttSubscribeMessage> pendingSubscriptions;
    private final boolean inInputPort;

    private static class Parameters {

	private static final String BROKER = "broker";
	private static final String CONCURRENT = "concurrent";
	private static final String ALIAS = "alias";
	private static final String QOS = "qos";
	private static final String KEEP_ALIVE = "keepAlive"; // TODO test
	private static final String WILL_TOPIC = "willTopic";
	private static final String WILL_MESSAGE = "willMessage";
	private static final String USERNAME = "username";
	private static final String PASSWORD = "password";
	private static final String CLIENT_ID = "clientId";
    }

    /**
     * Constructor of the Mqtt Protocol class
     *
     * @param inInputPort boolean
     * @param configurationPath VariablePath
     */
    public MqttProtocol(boolean inInputPort, VariablePath configurationPath) {
	super(configurationPath);
	this.pendingPublishes = new ArrayList<>();
	this.pendingSubscriptions = new ArrayList<>();
	this.inInputPort = inInputPort;
    }

    @Override
    public void setupPipeline(ChannelPipeline pipeline) {
	pipeline.addLast(MqttEncoder.INSTANCE);
	pipeline.addLast(new MqttDecoder());
	pipeline.addLast("Ping", new MqttPingHandler());
	pipeline.addLast(new MqttCommMessageCodec());
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
     * Build the connection message to a broker, client identifier is set to
     * empty string by default and clean session is set to true accordingly,
     * otherwise clean session will be false. KeepAlive is set to 2 seconds
     *
     * @return MqttConnectMessage
     */
    public MqttConnectMessage buildConnect() {

	String mqttUserName = getStringParameter(Parameters.USERNAME);
	String mqttPassword = getStringParameter(Parameters.PASSWORD);
	String willTopic = getStringParameter(Parameters.WILL_TOPIC);
	String clientId = getStringParameter(Parameters.CLIENT_ID, "");

	MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.CONNECT, false, MqttQoS.AT_MOST_ONCE, false, 0);
	MqttConnectVariableHeader variableHeader = new MqttConnectVariableHeader(
		mqttVersion.protocolName(),
		mqttVersion.protocolLevel(),
		checkBooleanParameter(Parameters.USERNAME),
		checkBooleanParameter(Parameters.PASSWORD),
		false,
		MqttQoS.AT_MOST_ONCE.value(),
		checkBooleanParameter(Parameters.WILL_TOPIC),
		clientId.equals(""),
		2
	);

	String willMessage = getStringParameter(Parameters.WILL_MESSAGE);
	MqttConnectPayload payload = new MqttConnectPayload(clientId, willTopic, willMessage, mqttUserName, mqttPassword);

	return new MqttConnectMessage(mqttFixedHeader, variableHeader, payload);
    }

    public MqttPublishMessage buildPublication(long messageId, String topic, Object message) {

	MqttQoS pubQos = hasParameter(Parameters.QOS) ? MqttQoS.valueOf(getIntParameter(Parameters.QOS)) : MqttQoS.AT_LEAST_ONCE;

	MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, false, pubQos, false, 0);
	MqttPublishVariableHeader variableHeader = new MqttPublishVariableHeader(topic, (int) messageId);
	ByteBuf payload = Unpooled.copiedBuffer(((String) message).getBytes(CharsetUtil.UTF_8));

	return new MqttPublishMessage(mqttFixedHeader, variableHeader, payload);
    }

    public MqttSubscribeMessage buildSubscription(long messageId, List<MqttTopicSubscription> topics) {

	MqttQoS subQos = hasParameter(Parameters.QOS) ? MqttQoS.valueOf(getIntParameter(Parameters.QOS)) : MqttQoS.AT_LEAST_ONCE;

	MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.SUBSCRIBE, false, subQos, false, 0);
	MqttMessageIdVariableHeader variableHeader = MqttMessageIdVariableHeader.from((int) messageId);
	MqttSubscribePayload payload = new MqttSubscribePayload(topics);

	return new MqttSubscribeMessage(mqttFixedHeader, variableHeader, payload);
    }

    private class MqttPingHandler extends ChannelInboundHandlerAdapter {

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

	    if (evt instanceof IdleStateEvent) {
		IdleStateEvent event = (IdleStateEvent) evt;
		switch (event.state()) {
		    case READER_IDLE:
		    case WRITER_IDLE:
		    case ALL_IDLE:
			MqttFixedHeader fixedHeader = new MqttFixedHeader(
				MqttMessageType.PINGREQ,
				false,
				MqttQoS.AT_MOST_ONCE,
				false,
				0
			);
			ctx.channel().writeAndFlush(new MqttMessage(fixedHeader));
			break;
		}
	    }
	}
    }

    private class MqttCommMessageCodec extends MessageToMessageCodec<MqttMessage, CommMessage> {

	@Override
	protected void encode(ChannelHandlerContext ctx, CommMessage message, List<Object> out) throws Exception {

	    ((CommCore.ExecutionContextThread) Thread.currentThread()).executionThread(
		    ctx.channel().attr(NioSocketCommChannel.EXECUTION_CONTEXT).get());

	    String topic = hasOperationSpecificParameter(message.operationName(), Parameters.ALIAS) ? getOperationSpecificStringParameter(message.operationName(), Parameters.ALIAS) : message.operationName();

	    if (inInputPort) {
		MqttQoS subQos = hasParameter(Parameters.QOS) ? MqttQoS.valueOf(getIntParameter(Parameters.QOS)) : MqttQoS.AT_LEAST_ONCE;
		List<MqttTopicSubscription> topics = Collections.singletonList(new MqttTopicSubscription(topic, subQos));
		pendingSubscriptions.add(buildSubscription(message.id(), topics));
	    } else {
		pendingPublishes.add(buildPublication(message.id(), topic, message.value().strValue()));
	    }
	    out.add(buildConnect());
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, MqttMessage msg, List<Object> out) throws Exception {

	    Channel channel = ctx.channel();

	    ((CommCore.ExecutionContextThread) Thread.currentThread()).executionThread(
		    channel.attr(NioSocketCommChannel.EXECUTION_CONTEXT).get());

	    switch (msg.fixedHeader().messageType()) {
		case CONNACK:
		    if (((MqttConnAckMessage) msg).variableHeader().connectReturnCode().equals(MqttConnectReturnCode.CONNECTION_ACCEPTED)) {
			if (inInputPort) {
			    if (checkBooleanParameter(Parameters.KEEP_ALIVE)) {
				channel.pipeline().addBefore("Ping", "IdleState", new IdleStateHandler(1, 1, 1));
			    }
			    for (ListIterator<MqttSubscribeMessage> i = pendingSubscriptions.listIterator(); i.hasNext();) {
				channel.writeAndFlush(i.next());
				i.remove();
			    }
			} else {
			    for (ListIterator<MqttPublishMessage> j = pendingPublishes.listIterator(); j.hasNext();) {
				channel.writeAndFlush(j.next());
				j.remove();
			    }
			}
		    }
		    break;
		case PUBLISH:
		    if (inInputPort) {
			MqttPublishMessage mpm = (MqttPublishMessage) msg;
			switch (mpm.fixedHeader().qosLevel()) {
			    case AT_LEAST_ONCE:
				if (mpm.variableHeader().messageId() != -1) {
				    MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
				    channel.writeAndFlush(new MqttPubAckMessage(fixedHeader, MqttMessageIdVariableHeader.from(mpm.variableHeader().messageId())));
				}
				break;
			    case EXACTLY_ONCE:
				if (mpm.variableHeader().messageId() != -1) {
				    MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBREC, false, MqttQoS.AT_MOST_ONCE, false, 0);
				    channel.writeAndFlush(new MqttPubAckMessage(fixedHeader, MqttMessageIdVariableHeader.from(mpm.variableHeader().messageId())));
				}
				break;
			}
			Value v = Value.create(Unpooled.copiedBuffer(mpm.payload()).toString(CharsetUtil.UTF_8));
			CommMessage message = new CommMessage(mpm.variableHeader().messageId(), null, null, v, null);
		    }
		    break;
		case PUBREC:
		    if (!inInputPort) {
			MqttPublishMessage mpm = (MqttPublishMessage) msg;
			if (mpm.variableHeader().messageId() != -1) {
			    MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBREL, false, MqttQoS.AT_MOST_ONCE, false, 0);
			    channel.writeAndFlush(new MqttPubAckMessage(fixedHeader, MqttMessageIdVariableHeader.from(mpm.variableHeader().messageId())));
			}
		    }
		    break;
		case PUBREL:
		    if (inInputPort) {
			MqttPublishMessage mpm = (MqttPublishMessage) msg;
			if (mpm.variableHeader().messageId() != -1) {
			    MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBCOMP, false, MqttQoS.AT_MOST_ONCE, false, 0);
			    channel.writeAndFlush(new MqttPubAckMessage(fixedHeader, MqttMessageIdVariableHeader.from(mpm.variableHeader().messageId())));
			}
		    }
		    break;
	    }
	    CommMessage message = CommMessage.UNDEFINED_MESSAGE;
	    out.add(message);
	}
    }
}
