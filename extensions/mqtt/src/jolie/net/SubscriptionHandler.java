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
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttSubscribePayload;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import jolie.net.ports.InputPort;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;

/**
 *
 * @author stefanopiozingaro
 */
public class SubscriptionHandler extends MessageToMessageCodec<MqttMessage, CommMessage> {

    private final MqttProtocol prt;
    private final InputPort ip;
    private final List<MqttSubscribeMessage> pendingSubscriptions;

    SubscriptionHandler(MqttProtocol protocol, InputPort inputPort) {
	this.prt = protocol;
	this.ip = inputPort;
	this.pendingSubscriptions = new ArrayList<>();
    }

    /**
     * TODO handle on demand subscriptions
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
	super.channelActive(ctx);
	handleSubscriptionsOnStartUp();
	ctx.channel().writeAndFlush(prt.buildConnect(false));
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, CommMessage msg, List<Object> out) throws Exception {

	((CommCore.ExecutionContextThread) Thread.currentThread()).executionThread(
		ctx.channel().attr(NioSocketCommChannel.EXECUTION_CONTEXT).get());

	// REQ RESP CHANNEL
	out.add(new MqttMessage(new MqttFixedHeader(MqttMessageType.PINGREQ, false, MqttQoS.AT_MOST_ONCE, false, 0)));
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, MqttMessage msg, List<Object> out) throws Exception {

	((CommCore.ExecutionContextThread) Thread.currentThread()).executionThread(
		ctx.channel().attr(NioSocketCommChannel.EXECUTION_CONTEXT).get());

	if (prt.connectionAccepted(msg)) {
	    ctx.channel().pipeline().addBefore("Ping", "IdleState", new IdleStateHandler(0, 2, 0));
	    for (Iterator<MqttSubscribeMessage> it = pendingSubscriptions.iterator(); it.hasNext();) {
		MqttSubscribeMessage i = it.next();
		ctx.channel().write(i);
		it.remove();
	    }
	    ctx.channel().flush();
	}
	if (publishReceived(msg)) {
	    handleIncomingPublish(ctx.channel(), (MqttPublishMessage) msg, out);
	}
	if (msg.fixedHeader().messageType().equals(MqttMessageType.PUBREL)) {
	    handlePubrel(ctx.channel(), msg);
	}
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
	super.exceptionCaught(ctx, cause);
	cause.printStackTrace();
	ctx.channel().close();
    }

    /**
     * TODO update build subscription to support topics list and pass all the
     * list to the builder from this method
     */
    private void handleSubscriptionsOnStartUp() {
	List<String> topicList = getTopicList();
	for (String sub : topicList) {
	    pendingSubscriptions.add(buildSubscription(
		    CommMessage.getNewMessageId(),
		    sub,
		    false,
		    prt.getCurrentQos(prt.getCurrentOperationName(sub))));
	}
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
    private MqttSubscribeMessage buildSubscription(long messageId, String topic, boolean isDup, MqttQoS subQos) {

	MqttFixedHeader mfh = new MqttFixedHeader(
		MqttMessageType.SUBSCRIBE,
		isDup,
		subQos,
		false,
		0);
	MqttMessageIdVariableHeader vh = MqttMessageIdVariableHeader.from((int) messageId);
	MqttSubscribePayload p = new MqttSubscribePayload(
		Collections.singletonList(
			new MqttTopicSubscription(topic, MqttQoS.EXACTLY_ONCE)));

	return new MqttSubscribeMessage(mfh, vh, p);
    }

    private List<String> getTopicList() {
	List<String> opL = new ArrayList<>();
	ip.getInterface().oneWayOperations().forEach((owon, u1) -> {
	    opL.add(getAliasForOperation(owon));

	});
	ip.getInterface().requestResponseOperations().forEach((rron, u1) -> {
	    opL.add(getAliasForOperation(rron));
	});
	return opL;
    }

    private String getAliasForOperation(String on) {
	for (Iterator<Map.Entry<String, ValueVector>> it = ip.protocolConfigurationPath().getValue().getFirstChild("osc").children().entrySet().iterator(); it.hasNext();) {
	    Map.Entry<String, ValueVector> i = it.next();
	    if (on.equals(i.getKey())) {
		return i.getValue().first().getFirstChild("alias").strValue();
	    }
	    it.remove();
	}
	return on;
    }

    private boolean publishReceived(MqttMessage msg) {
	return msg.fixedHeader().messageType().equals(MqttMessageType.PUBLISH);
    }

    /**
     * TODO handle qos2 publications incoming waiting for pubrel
     *
     * @param channel Channel
     * @param mpm MqttPublishMessage
     * @param out List of Objects
     */
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

    private void handlePubrel(Channel channel, MqttMessage msg) {
	MqttFixedHeader fh = new MqttFixedHeader(MqttMessageType.PUBCOMP, false, MqttQoS.AT_MOST_ONCE, false, 0);
	MqttMessageIdVariableHeader vh = MqttMessageIdVariableHeader.from(((MqttMessageIdVariableHeader) msg.variableHeader()).messageId());
	channel.writeAndFlush(new MqttMessage(fh, vh));
    }
}
