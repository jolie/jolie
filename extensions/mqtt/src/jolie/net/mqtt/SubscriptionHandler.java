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
package jolie.net.mqtt;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
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
import jolie.net.CommCore;
import jolie.net.CommMessage;
import jolie.net.MqttProtocol;
import jolie.net.NioSocketCommChannel;
import jolie.runtime.Value;

public class SubscriptionHandler extends MessageToMessageCodec<MqttMessage, CommMessage> {

    private final MqttProtocol prt;
    private final List<MqttSubscribeMessage> pendingSubscriptions;

    public SubscriptionHandler(MqttProtocol protocol) {
	this.prt = protocol;
	this.pendingSubscriptions = new ArrayList<>();
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, CommMessage msg, List<Object> out) throws Exception {

	((CommCore.ExecutionContextThread) Thread.currentThread()).executionThread(
		ctx.channel().attr(NioSocketCommChannel.EXECUTION_CONTEXT).get());

	commMsgToMqttMsg(ctx.channel(), msg, out);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, MqttMessage msg, List<Object> out) throws Exception {

	((CommCore.ExecutionContextThread) Thread.currentThread()).executionThread(
		ctx.channel().attr(NioSocketCommChannel.EXECUTION_CONTEXT).get());

	mqttMsgToCommMsg(ctx.channel(), msg, out);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

	super.channelActive(ctx);
	pendingSubscriptions.add(prt.buildSubscription(
		CommMessage.getNewMessageId(),
		prt.getTopicList(),
		false,
		prt.getQos(MqttQoS.AT_LEAST_ONCE)));
	ctx.channel().writeAndFlush(prt.buildConnect(false));
    }

    private void commMsgToMqttMsg(Channel ch, CommMessage msg, List<Object> out) {
	throw new UnsupportedOperationException("Not supported yet.");
    }

    private void mqttMsgToCommMsg(Channel ch, MqttMessage msg, List<Object> out) {
	if (prt.connectionAccepted(msg)) {
	    ch.pipeline().addBefore("Ping", "IdleState", new IdleStateHandler(0, 2, 0));
	    if (!(prt.isSubscriptionOnDemand(false) || pendingSubscriptions.isEmpty())) {
		for (Iterator<MqttSubscribeMessage> it = pendingSubscriptions.iterator(); it.hasNext();) {
		    MqttSubscribeMessage i = it.next();
		    ch.write(i);
		    it.remove(); //remove from the queue
		}
		ch.flush();
	    }
	} else {
	    if (msg.fixedHeader().messageType().equals(MqttMessageType.PUBLISH)) {
		handleIncomingPublish(ch, (MqttPublishMessage) msg, out);
	    } else {
		if (msg.fixedHeader().messageType().equals(MqttMessageType.PUBREL)) {
		    MqttFixedHeader fh = new MqttFixedHeader(MqttMessageType.PUBCOMP, false, MqttQoS.AT_MOST_ONCE, false, 0);
		    MqttMessageIdVariableHeader vh = MqttMessageIdVariableHeader.from(((MqttMessageIdVariableHeader) msg.variableHeader()).messageId());
		    ch.writeAndFlush(new MqttMessage(fh, vh));
		}
	    }
	}
    }

    private void handleIncomingPublish(Channel channel, MqttPublishMessage mpm, List<Object> out) {
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
		CommMessage.GENERIC_ID,
		prt.getCurrentOperationName(mpm.variableHeader().topicName()),
		"/",
		Value.create(Unpooled.copiedBuffer(mpm.payload()).toString(CharsetUtil.UTF_8)),
		null));
    }
}
