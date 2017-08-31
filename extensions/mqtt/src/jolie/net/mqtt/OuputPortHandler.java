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
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import java.util.ArrayList;

import java.util.Collections;
import java.util.List;

import jolie.net.CommCore;
import jolie.net.CommMessage;
import jolie.net.MqttProtocol;
import jolie.net.NioSocketCommChannel;
import jolie.runtime.Value;

/**
 *
 * @author stefanopiozingaro
 */
public class OuputPortHandler
	extends MessageToMessageCodec<MqttMessage, CommMessage> {

    private final MqttProtocol mp;
    private Channel cc;
    private MqttPublishMessage pendingMpm;
    private MqttSubscribeMessage pendingMsm;

    private CommMessage cmReq;

    /**
     *
     * @param mp MqttProtocol
     */
    public OuputPortHandler(MqttProtocol mp) {

	this.mp = mp;
	this.cc = null;
	pendingMpm = null;
	pendingMsm = null;
	cmReq = null;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, CommMessage in,
	    List<Object> out) throws Exception {

	init(ctx);
	out.add(mp.connectMsg());
	cmReq = CommMessage.createRequest(in.operationName(), "/", in.value());
	/*
	two distinct sitautions arise at thits point, each one could be in 
	a subsituation of client connection or not.
	The first in the case of a oneway is a send pub request, whilst in 
	a request response situation is a send sub request followed by a 
	send pub request, in this order.
	 */
	if (mp.isOneWay(in.operationName())) {
	    MqttPublishMessage mpm = mp.pubOneWayRequest(in);
	    pendingMpm = mpm;
	} else {
	    MqttSubscribeMessage msm = mp.subRequestResponseRequest(in);
	    MqttPublishMessage mpm = mp.pubRequestResponseRequest(in);
	    pendingMpm = mpm;
	    pendingMsm = msm;
	}
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, MqttMessage in,
	    List<Object> out) throws Exception {

	init(ctx);

	switch (in.fixedHeader().messageType()) {
	    case CONNACK:
		MqttConnectReturnCode crc
			= ((MqttConnAckMessage) in).variableHeader()
				.connectReturnCode();
		if (crc.equals(MqttConnectReturnCode.CONNECTION_ACCEPTED)) {
		    mp.startPing(cc.pipeline());
		    if (mp.isOneWay(cmReq.operationName())) {
			if (pendingMpm != null) {
			    cc.writeAndFlush(pendingMpm);
			    if (mp.checkQoS(pendingMpm, MqttQoS.AT_MOST_ONCE)) {
				cc.writeAndFlush(new CommMessage(cmReq.id(),
					cmReq.operationName(), "/",
					Value.create(), null));
				mp.stopPing(cc.pipeline());
			    }
			}
		    } else {
			if (pendingMsm != null) {
			    cc.writeAndFlush(pendingMsm);
			    if (mp.checkQoS(pendingMsm, MqttQoS.AT_MOST_ONCE)) {
				cc.writeAndFlush(pendingMpm);
			    }
			}
		    }
		}
		break;
	    case PUBLISH:
		// TODO support wildcards and variabili
		mp.recPub(cc, ((MqttPublishMessage) in));
		mp.stopPing(cc.pipeline());
		out.add(mp.pubRequestResponseResponse((MqttPublishMessage) in,
			cmReq));
		break;
	    case PUBACK:
	    case PUBCOMP:
		if (mp.isOneWay(cmReq.operationName())) {
		    out.add(new CommMessage(
			    cmReq.id(),
			    cmReq.operationName(),
			    "/",
			    Value.create(),
			    null));
		    mp.stopPing(cc.pipeline());
		}
		break;
	    case PUBREC:
		mp.handlePubrec(cc, in);
	    case PUBREL:
		mp.handlePubrel(cc, in);
		break;
	    case SUBACK:
		if (pendingMpm != null) {
		    cc.writeAndFlush(pendingMpm);
		}
		break;
	}
    }

    private void init(ChannelHandlerContext ctx) {

	if (cc == null) {
	    cc = ctx.channel();
	}

	((CommCore.ExecutionContextThread) Thread.currentThread())
		.executionThread(cc
			.attr(NioSocketCommChannel.EXECUTION_CONTEXT).get());
    }
}
