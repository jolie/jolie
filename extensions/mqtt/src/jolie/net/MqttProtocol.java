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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;

public class MqttProtocol extends AsyncCommProtocol {

    private static final MqttVersion mqttVersion = MqttVersion.MQTT_3_1_1;
    private final List<MqttPublishMessage> pendingPublishes;
    private final List<MqttSubscribeMessage> pendingSubscriptions;
    private final boolean inputPort;
    private boolean connected;

    public MqttProtocol(boolean inputPort, VariablePath configurationPath) {
	super(configurationPath);
	this.pendingPublishes = new ArrayList<>();
	this.pendingSubscriptions = new ArrayList<>();
	this.inputPort = inputPort;
	this.connected = false;
    }

    @Override
    public void setupPipeline(ChannelPipeline p) {
	p.addLast(MqttEncoder.INSTANCE);
	p.addLast(new MqttDecoder());
	p.addLast("Ping", new MqttPingHandler());
	p.addLast(new MqttCommMessageCodec());
    }

    @Override
    public String name() {
	return "mqtt";
    }

    @Override
    public boolean isThreadSafe() {
	return checkBooleanParameter(Parameters.CONCURRENT);
    }

    public boolean isConnected() {
	return connected;
    }

    private ByteBuf parseObject(Object obj) {
	return Unpooled.wrappedBuffer(String.valueOf(obj).getBytes(CharsetUtil.UTF_8));
    }

    private static class Parameters {

	//private static final String BROKER = "broker";
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
     * @param isDup boolean
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
    public MqttPublishMessage buildPublication(long messageId, String topic, Object message, boolean isDup, MqttQoS pubQos) {

	MqttFixedHeader mfh = new MqttFixedHeader(
		MqttMessageType.PUBLISH,
		isDup,
		pubQos,
		false,
		0);
	MqttPublishVariableHeader vh = new MqttPublishVariableHeader(topic, (int) messageId);

	return new MqttPublishMessage(mfh, vh, parseObject(message));
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

    private class MqttPingHandler extends ChannelInboundHandlerAdapter {

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

	    if (evt instanceof IdleStateEvent) {
		IdleStateEvent event = (IdleStateEvent) evt;
		switch (event.state()) {
		    case READER_IDLE:
			break;
		    case WRITER_IDLE:
			MqttFixedHeader fixedHeader = new MqttFixedHeader(
				MqttMessageType.PINGREQ,
				false,
				MqttQoS.AT_MOST_ONCE,
				false,
				0);
			ctx.channel().writeAndFlush(new MqttMessage(fixedHeader));
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

	    MqttQoS qos = hasOperationSpecificParameter(message.operationName(), Parameters.QOS) ? MqttQoS.valueOf(getOperationSpecificParameterFirstValue(message.operationName(), Parameters.QOS).intValue()) : MqttQoS.AT_LEAST_ONCE;

	    if (isConnected()) {
		if (inputPort) {
		    out.add(buildSubscription(message.id(), topic, false, qos));
		} else {
		    out.add(buildPublication(message.id(), topic, message.value().strValue(), false, qos));
		}
	    } else {
		if (inputPort) {
		    pendingSubscriptions.add(buildSubscription(message.id(), topic, false, qos));
		} else {
		    pendingPublishes.add(buildPublication(message.id(), topic, message.value().strValue(), false, qos));
		}
		out.add(buildConnect(false));
	    }
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, MqttMessage msg, List<Object> out) throws Exception {

	    ((CommCore.ExecutionContextThread) Thread.currentThread()).executionThread(
		    ctx.channel().attr(NioSocketCommChannel.EXECUTION_CONTEXT).get());

	    switch (msg.fixedHeader().messageType()) {
		case CONNACK:
		    handleConnack(ctx.channel(), (MqttConnAckMessage) msg, out);
		    break;
		case PUBLISH:
		    if (inputPort) {
			handlePublish(ctx.channel(), (MqttPublishMessage) msg, out);
		    }
		    break;
		case PUBREC:
		    handlePubrec(ctx.channel(), msg);
		    break;
		case PUBREL:
		    handlePubrel(ctx.channel(), msg);
		    break;
	    }
	}

	/**
	 *
	 * @param channel Channel
	 * @param msg MqttConnAckMessage
	 * @param out List of Object to send
	 */
	private void handleConnack(Channel channel, MqttConnAckMessage msg, List<Object> out) {
	    switch (msg.variableHeader().connectReturnCode()) {
		case CONNECTION_ACCEPTED:
		    connected = true;
		    if (inputPort) {
			channel.pipeline().addBefore("Ping", "IdleState", new IdleStateHandler(0, 2, 0));
			for (Iterator<MqttSubscribeMessage> it = pendingSubscriptions.iterator(); it.hasNext();) {
			    MqttSubscribeMessage i = it.next();
			    channel.write(i);
			    it.remove();
			}
		    } else {
			for (Iterator<MqttPublishMessage> it = pendingPublishes.iterator(); it.hasNext();) {
			    MqttPublishMessage j = it.next();
			    channel.write(j);
			    out.add(new CommMessage(j.variableHeader().messageId(), getOperationName(configurationPath().getValue(), j.variableHeader().topicName()), "/", Value.create(), null));
			    it.remove();
			}
		    }
		    channel.flush();
		    break;
		case CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION:
		case CONNECTION_REFUSED_IDENTIFIER_REJECTED:
		case CONNECTION_REFUSED_SERVER_UNAVAILABLE:
		case CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD:
		case CONNECTION_REFUSED_NOT_AUTHORIZED:
		    channel.close();
		    break;
	    }
	}

	/**
	 * TODO handle qos2 publications incoming waiting for pubrel
	 *
	 * @param channel Channel
	 * @param mpm MqttPublishMessage
	 * @param out List of Objects
	 */
	private void handlePublish(Channel channel, MqttPublishMessage mpm, List<Object> out) {
	    switch (mpm.fixedHeader().qosLevel()) {
		case AT_MOST_ONCE:
		    break;
		case AT_LEAST_ONCE:
		    if (mpm.variableHeader().messageId() != -1) {
			MqttFixedHeader fh = new MqttFixedHeader(MqttMessageType.PUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
			MqttMessageIdVariableHeader vh = MqttMessageIdVariableHeader.from(mpm.variableHeader().messageId());
			channel.writeAndFlush(new MqttPubAckMessage(fh, vh));
		    }
		    break;
		case EXACTLY_ONCE:
		    if (mpm.variableHeader().messageId() != -1) {
			MqttFixedHeader fh = new MqttFixedHeader(MqttMessageType.PUBREC, false, MqttQoS.AT_MOST_ONCE, false, 0);
			MqttMessageIdVariableHeader vh = MqttMessageIdVariableHeader.from(mpm.variableHeader().messageId());
			channel.writeAndFlush(new MqttMessage(fh, vh));
		    }
		    break;
	    }
	    out.add(new CommMessage(CommMessage.getNewMessageId(), getOperationName(configurationPath().getValue(), mpm.variableHeader().topicName()), "/", Value.create(Unpooled.copiedBuffer(mpm.payload()).toString(CharsetUtil.UTF_8)), null));
	}

	private void handlePubrec(Channel channel, MqttMessage msg) {
	    MqttFixedHeader fh = new MqttFixedHeader(MqttMessageType.PUBREL, false, MqttQoS.AT_LEAST_ONCE, false, 0);
	    MqttMessageIdVariableHeader vh = (MqttMessageIdVariableHeader) msg.variableHeader();
	    channel.writeAndFlush(new MqttMessage(fh, vh));
	}

	private void handlePubrel(Channel channel, MqttMessage msg) {
	    MqttFixedHeader fh = new MqttFixedHeader(MqttMessageType.PUBCOMP, false, MqttQoS.AT_MOST_ONCE, false, 0);
	    MqttMessageIdVariableHeader vh = MqttMessageIdVariableHeader.from(((MqttMessageIdVariableHeader) msg.variableHeader()).messageId());
	    channel.writeAndFlush(new MqttMessage(fh, vh));
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
	    super.exceptionCaught(ctx, cause);
	    cause.printStackTrace();
	    ctx.close().sync();
	}

	private String getOperationName(Value value, String topic) {
	    for (Iterator<Map.Entry<String, ValueVector>> it = value.children().entrySet().iterator(); it.hasNext();) {
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
}
