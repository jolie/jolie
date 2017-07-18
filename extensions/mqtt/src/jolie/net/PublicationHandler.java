/*
 * Copyright (C) 2017 stefanopiozingaro
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
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
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.util.CharsetUtil;
import java.util.List;
import jolie.runtime.Value;

/**
 *
 * @author stefanopiozingaro
 */
public class PublicationHandler extends MessageToMessageCodec<MqttMessage, CommMessage> {

    private final MqttProtocol prt;
    private boolean connected;
    private MqttPublishMessage toBePublished;

    public PublicationHandler(MqttProtocol protocol) {
	this.prt = protocol;
	this.connected = false;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, CommMessage message, List<Object> out) throws Exception {

	((CommCore.ExecutionContextThread) Thread.currentThread()).executionThread(
		ctx.channel().attr(NioSocketCommChannel.EXECUTION_CONTEXT).get());

	toBePublished = buildPublication(
		message.id(),
		prt.getCurrentTopic(message.operationName()),
		message.value(),
		false,
		prt.getCurrentQos(message.operationName()));

	if (connected) {
	    ctx.channel().writeAndFlush(toBePublished);
	} else {
	    out.add(prt.buildConnect(false));
	}
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, MqttMessage msg,
	    List<Object> out) throws Exception {

	((CommCore.ExecutionContextThread) Thread.currentThread()).executionThread(
		ctx.channel().attr(NioSocketCommChannel.EXECUTION_CONTEXT).get());

	if (prt.connectionAccepted(msg)) {
	    connected = true;
	    ctx.channel().writeAndFlush(toBePublished);
	    if (toBePublished.fixedHeader().qosLevel().equals(MqttQoS.AT_MOST_ONCE)) {
		out.add(new CommMessage( // qos 0
			toBePublished.variableHeader().messageId(),
			prt.getCurrentOperationName(toBePublished.variableHeader().topicName()),
			"/",
			Value.create(),
			null));
	    }
	} else {
	    switch (msg.fixedHeader().messageType()) {
		case PUBACK: // qos 1
		    out.add(new CommMessage(
			    toBePublished.variableHeader().messageId(),
			    prt.getCurrentOperationName(toBePublished.variableHeader().topicName()),
			    "/",
			    Value.create(),
			    null));
		    break;
		case PUBREC:
		    handlePubrec(ctx.channel(), msg);
		    break;
		case PUBCOMP: // qos 2
		    out.add(new CommMessage(
			    toBePublished.variableHeader().messageId(),
			    prt.getCurrentOperationName(toBePublished.variableHeader().topicName()),
			    "/",
			    Value.create(),
			    null));
		    break;
	    }
	}

    }

    private void handlePubrec(Channel channel, MqttMessage msg) {
	MqttFixedHeader fh = new MqttFixedHeader(MqttMessageType.PUBREL, false, MqttQoS.AT_LEAST_ONCE, false, 0);
	MqttMessageIdVariableHeader vh = (MqttMessageIdVariableHeader) msg.variableHeader();
	channel.writeAndFlush(new MqttMessage(fh, vh));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
	super.exceptionCaught(ctx, cause);
	cause.printStackTrace();
	ctx.channel().close();
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
}
