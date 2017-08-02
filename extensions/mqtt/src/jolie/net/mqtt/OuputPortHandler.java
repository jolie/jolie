/*
 * The MIT License
 *
 * Copyright 2017 Stefano Pio Zingaro <stefanopio.zingaro@unibo.it>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package jolie.net.mqtt;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.timeout.IdleStateEvent;

import java.util.List;

import jolie.net.CommCore;
import jolie.net.CommMessage;
import jolie.net.MqttProtocol;
import jolie.net.NioSocketCommChannel;
import jolie.runtime.Value;

public class OuputPortHandler extends MessageToMessageCodec<MqttMessage, CommMessage> {

    private final MqttProtocol protocol;
    private Channel channel;
    private boolean isConnect;
    private CommMessage pendingCm;
    private MqttPublishMessage pendingMpm;
    private MqttSubscribeMessage pendingMsm;

    public OuputPortHandler(MqttProtocol protocol) {
	this.protocol = protocol;
	this.isConnect = false;
	this.pendingCm = CommMessage.UNDEFINED_MESSAGE;
	this.pendingMpm = null;
	this.pendingMsm = null;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, CommMessage commMessage,
	    List<Object> out) throws Exception {

	init(ctx);

	if (!isConnect) {
	    out.add(protocol.connectMsg());
	}

	if (protocol.isOneWay(commMessage.operationName())) {
	    MqttPublishMessage mpm = protocol.publishMsg(
		    commMessage.id(),
		    protocol.topic_one_way(commMessage),
		    commMessage.value(),
		    protocol.qos(commMessage.operationName()));
	    if (isConnect) {
		out.add(mpm);
		if (protocol.checkQoS(mpm, MqttQoS.AT_MOST_ONCE)) {
		    protocol.recAck(channel, commMessage);
		}
	    } else {
		pendingMpm = mpm;
	    }
	} else {
	    commMessage.value().add(Value.create(protocol.topic_request_response(commMessage)));
	    MqttPublishMessage mpm = protocol.publishMsg(
		    commMessage.id(),
		    protocol.topic_one_way(commMessage),
		    commMessage.value(),
		    protocol.qos(commMessage.operationName()));
	    MqttSubscribeMessage msm = protocol.subscribeMsg(
		    commMessage.id(),
		    protocol.topics(),
		    protocol.qos());
	    pendingMpm = mpm;
	    if (isConnect) {
		out.add(msm);
	    } else {
		pendingMsm = msm;
	    }
	}
	pendingCm = commMessage;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, MqttMessage mm,
	    List<Object> out) throws Exception {

	init(ctx);

	switch (mm.fixedHeader().messageType()) {
	    case CONNACK:
		MqttConnectReturnCode crc
			= ((MqttConnAckMessage) mm).variableHeader().connectReturnCode();
		if (crc.equals(MqttConnectReturnCode.CONNECTION_ACCEPTED)) {
		    isConnect = true;
		    protocol.startPing(channel.pipeline());
		    if (protocol.isOneWay(pendingCm.operationName())) {
			if (pendingMpm != null) {
			    channel.writeAndFlush(pendingMpm);
			    if (protocol.checkQoS(pendingMpm, MqttQoS.AT_MOST_ONCE)) {
				protocol.recAck(channel, pendingCm);
			    }
			}
		    } else {
			if (pendingMsm != null) {
			    channel.writeAndFlush(pendingMsm);
			}
		    }
		}
		break;
	    case PUBLISH:
		protocol.recPub(channel, (MqttPublishMessage) mm);
		break;
	    case PUBACK:
	    case PUBCOMP:
		protocol.recAck(channel, pendingCm);
		break;
	    case PUBREC:
	    case PUBREL:
		protocol.sendAck(channel, (MqttMessageIdVariableHeader) mm.variableHeader(), mm.fixedHeader().messageType());
		break;
	    case SUBACK:
		channel.writeAndFlush(pendingMpm);
		if (protocol.checkQoS(pendingMpm, MqttQoS.AT_MOST_ONCE)) {
		    protocol.recAck(channel, pendingCm);
		}
		break;
	}
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx,
	    Object evt) throws Exception {

	init(ctx);

	if (evt instanceof IdleStateEvent) {
	    IdleStateEvent event = (IdleStateEvent) evt;
	    switch (event.state()) {
		case READER_IDLE:
		    break;
		case WRITER_IDLE:
		    MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PINGREQ, false, MqttQoS.AT_MOST_ONCE, false, 0);
		    channel.writeAndFlush(new MqttMessage(fixedHeader));
	    }
	}
    }

    private void init(ChannelHandlerContext ctx) {

	channel = ctx.channel();
	((CommCore.ExecutionContextThread) Thread.currentThread()).executionThread(
		channel.attr(NioSocketCommChannel.EXECUTION_CONTEXT).get());
    }
}
