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
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;

import java.util.List;
import jolie.Interpreter;

import jolie.net.CommCore;
import jolie.net.CommMessage;
import jolie.net.MqttProtocol;
import jolie.net.NioSocketCommChannel;

import jolie.runtime.Value;

/**
 *
 * @author stefanopiozingaro
 */
public class OutputPortHandler
	extends MessageToMessageCodec<MqttMessage, CommMessage> {

    private final MqttProtocol mp;
    private Channel cc;
    private MqttPublishMessage pendingMpm;
    private CommMessage cmReq;
    private MqttPublishMessage qos2pendingPublish;

    /**
     *
     * @param mp MqttProtocol
     */
    public OutputPortHandler(MqttProtocol mp) {
	this.mp = mp;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, CommMessage in,
	    List<Object> out) throws Exception {

	init(ctx);
	out.add(mp.connectMsg());
        cmReq = in;
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
			cc.writeAndFlush(mp.pubOneWayRequest(cmReq));
			if (mp.checkQoS(cmReq, MqttQoS.AT_MOST_ONCE)) {
			    cc.writeAndFlush(new CommMessage(cmReq.id(),
				    cmReq.operationName(), "/",
				    Value.create(), null));
			    mp.stopPing(cc.pipeline());
                            cc.attr( NioSocketCommChannel.SEND_RELEASE ).get().get( (int) cmReq.id() );
			}
		    } else {
			cc.writeAndFlush(mp.subRequestResponseRequest(cmReq));
			pendingMpm = mp.pubRequestResponseRequest(cmReq);
		    }
		}
		break;
	    case PUBLISH:
		// TODO support wildcards and variables
		MqttPublishMessage mpmIn = ((MqttPublishMessage) in);
		mp.recv_pub(cc, mpmIn);
		if (mpmIn.fixedHeader().qosLevel().equals(MqttQoS.EXACTLY_ONCE)) {
		    qos2pendingPublish = mpmIn.retain(); // we retain the message as it will be used by another "channel"
		} else {
		    mp.stopPing(cc.pipeline());
		    CommMessage cmResp = mp.recv_pubReqResp(mpmIn, cmReq);
                    // TODO RILASCIARE IL SEMAFORO PER IL MESSAGGIO
		    out.add(cmResp);
		}
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
		break;
	    case PUBREL:
		mp.handlePubrel(cc, in);
		if (qos2pendingPublish != null) {
		    mp.stopPing(cc.pipeline());
		    CommMessage cmResp = mp.recv_pubReqResp(qos2pendingPublish,
			    cmReq);
		    out.add(cmResp);
		}
		break;
	    case SUBACK:
		cc.write(pendingMpm);
                Interpreter.getInstance().logInfo( "Releasing semaphore for #" + pendingMpm.variableHeader().messageId() );
		cc.attr( NioSocketCommChannel.SEND_RELEASE ).get().get( pendingMpm.variableHeader().messageId() ).release();
		break;
	}
    }

    private void init(ChannelHandlerContext ctx) {

	cc = ctx.channel();

	((CommCore.ExecutionContextThread) Thread.currentThread())
		.executionThread(cc
			.attr(NioSocketCommChannel.EXECUTION_CONTEXT).get());

	mp.checkDebug(ctx.pipeline());

    }
}
