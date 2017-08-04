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
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import java.util.Collections;

import java.util.List;

import jolie.net.CommCore;
import jolie.net.CommMessage;
import jolie.net.MqttProtocol;
import jolie.net.NioSocketCommChannel;
import jolie.runtime.Value;

public class OuputPortHandler
	extends MessageToMessageCodec<MqttMessage, CommMessage> {

    private final MqttProtocol protocol;
    private Channel channel;
    private boolean isConnect;
    private CommMessage cmReq;
    private MqttPublishMessage pendingMpm;
    private MqttSubscribeMessage pendingMsm;

    public OuputPortHandler(MqttProtocol protocol) {
	this.protocol = protocol;
	this.isConnect = false;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, CommMessage cm,
	    List<Object> out) throws Exception {

	init(ctx);

	cmReq = CommMessage.createRequest(
		cm.operationName(),
		"/",
		cm.value());

	MqttConnectMessage mcm = protocol.connectMsg();
	if (!isConnect) {
	    out.add(mcm);
	}

	if (protocol.isOneWay(cmReq.operationName())) {
	    MqttPublishMessage mpm = protocol.publishMsg(
		    cmReq.id(),
		    protocol.topic_one_way(cmReq),
		    cmReq.value(),
		    protocol.qos(cmReq.operationName()));
	    if (isConnect) {
		out.add(mpm);
		if (protocol.checkQoS(mpm, MqttQoS.AT_MOST_ONCE)) {
		    protocol.recAck(channel, cmReq);
		}
	    } else {
		pendingMpm = mpm;
	    }
	} else {
	    MqttPublishMessage mpm = protocol.publishMsg(
		    cmReq.id(),
		    protocol.topic_one_way(cmReq),
		    protocol.responseValue(cmReq),
		    protocol.qos(cmReq.operationName()));
	    MqttSubscribeMessage msm = protocol.subscribeMsg(
		    cmReq.id(),
		    Collections.singletonList(
			    protocol.topic_request_response(cmReq)),
		    protocol.qos());
	    pendingMpm = mpm;
	    if (isConnect) {
		out.add(msm);
		if (protocol.checkQoS(msm, MqttQoS.AT_MOST_ONCE)) {
		    out.add(mpm);
		}
	    } else {
		pendingMsm = msm;
	    }
	}
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, MqttMessage mm,
	    List<Object> out) throws Exception {

	init(ctx);

	switch (mm.fixedHeader().messageType()) {
	    case CONNACK:

		MqttConnectReturnCode crc
			= ((MqttConnAckMessage) mm).variableHeader()
				.connectReturnCode();
		if (crc.equals(MqttConnectReturnCode.CONNECTION_ACCEPTED)) {
		    isConnect = true;
		    protocol.startPing(channel.pipeline());
		    if (protocol.isOneWay(cmReq.operationName())) {
			if (pendingMpm != null) {
			    channel.writeAndFlush(pendingMpm);
			    if (protocol.checkQoS(pendingMpm,
				    MqttQoS.AT_MOST_ONCE)) {
				protocol.recAck(channel, cmReq);
			    }
			}
		    } else {
			if (pendingMsm != null) {
			    channel.writeAndFlush(pendingMsm);
			    if (protocol.checkQoS(pendingMsm, MqttQoS.AT_MOST_ONCE)) {
				channel.writeAndFlush(pendingMpm);
			    }
			}
		    }
		}
		break;
	    case PUBLISH:

		MqttPublishMessage mpm = (MqttPublishMessage) mm;
		CommMessage cmResp = protocol.responseCommMsg(cmReq, mpm);
		out.add(cmResp);
		protocol.recPub(channel, mpm);
		break;
	    case PUBACK:
	    case PUBCOMP:

		if (protocol.isOneWay(cmReq.operationName())) {

		    out.add(new CommMessage(
			    cmReq.id(),
			    cmReq.operationName(),
			    "/",
			    Value.create(),
			    null));
		}
		break;
	    case PUBREC:
	    case PUBREL:

		protocol.sendAck(channel,
			(MqttMessageIdVariableHeader) mm.variableHeader(),
			mm.fixedHeader().messageType());
		break;
	    case SUBACK:

		channel.writeAndFlush(pendingMpm);
		break;
	}
    }

    private void init(ChannelHandlerContext ctx) {

	channel = ctx.channel();
	((CommCore.ExecutionContextThread) Thread.currentThread())
		.executionThread(
			channel.attr(NioSocketCommChannel.EXECUTION_CONTEXT)
				.get());
    }
}
