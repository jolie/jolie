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

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import jolie.Interpreter;
import jolie.runtime.Value;

/**
 *
 * @author stefanopiozingaro
 */
public class CodecHandler extends MessageToMessageCodec<MqttMessage, CommMessage> {

    private final MqttProtocol prt;
    private final List<MqttPublishMessage> pendingPublishes;
    private final List<MqttSubscribeMessage> pendingSubscriptions;

    public CodecHandler(MqttProtocol protocol) {
	this.prt = protocol;
	this.pendingPublishes = new ArrayList<>();
	this.pendingSubscriptions = new ArrayList<>();
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, CommMessage message, List<Object> out) throws Exception {

	((CommCore.ExecutionContextThread) Thread.currentThread()).executionThread(
		ctx.channel().attr(NioSocketCommChannel.EXECUTION_CONTEXT).get());

	if (prt.inputPort) {
	    pendingSubscriptions.add(prt.buildSubscription(
		    message.id(),
		    prt.getCurrentTopic(message),
		    false,
		    prt.getCurrentQos(message)));
	} else {
	    pendingPublishes.add(prt.buildPublication(
		    message.id(),
		    prt.getCurrentTopic(message),
		    message.value(),
		    false,
		    prt.getCurrentQos(message)));
	}
	out.add(prt.buildConnect(false));
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, MqttMessage msg,
	    List<Object> out) throws Exception {

	((CommCore.ExecutionContextThread) Thread.currentThread()).executionThread(
		ctx.channel().attr(NioSocketCommChannel.EXECUTION_CONTEXT).get());

	switch (msg.fixedHeader().messageType()) {
	    case CONNACK:
		handleConnack(ctx.channel(), (MqttConnAckMessage) msg, out);
		break;
	    case PUBLISH:
		if (prt.inputPort) {
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
		if (prt.inputPort) {
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
			out.add(new CommMessage(
				j.variableHeader().messageId(),
				prt.getCurrentOperationName(j.variableHeader().topicName()),
				"/",
				Value.create(),
				null));
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
		Interpreter.getInstance().logSevere("Connection refused from the broker: "
			+ msg.variableHeader().connectReturnCode().name());
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
	out.add(new CommMessage(
		CommMessage.getNewMessageId(),
		prt.getCurrentOperationName(
			mpm.variableHeader().topicName()),
		"/",
		Value.create(Unpooled.copiedBuffer(mpm.payload()).toString(CharsetUtil.UTF_8)),
		null));
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
	ctx.channel().close();
	cause.printStackTrace();
    }
}
