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

import java.util.List;

import jolie.net.CommCore;
import jolie.net.CommMessage;
import jolie.net.MqttProtocol;
import jolie.net.NioSocketCommChannel;

/**
 *
 * @author stefanopiozingaro
 */
public class InputPortHandler
	extends MessageToMessageCodec<MqttMessage, CommMessage> {

    private final MqttProtocol mp;
    private Channel cc;

    public InputPortHandler(MqttProtocol mp) {
	this.mp = mp;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, CommMessage in,
	    List<Object> out) throws Exception {

	init(ctx);
	out.add(mp.send_response(in));
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
		    mp.send_subRequest(cc);
		}
		break;
	    case PUBLISH:
		// TODO support wildcards and variables
		MqttPublishMessage mpmIn = ((MqttPublishMessage) in).copy();
		mp.recPub(cc, mpmIn);
		CommMessage cmReq = mp.rec_request(mpmIn);
		out.add(cmReq);
		break;
	    case PUBREC:
		mp.handlePubrec(cc, in);
	    case PUBREL:
		mp.handlePubrel(cc, in);
		break;
	}
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

	init(ctx);
	cc.writeAndFlush(mp.connectMsg());
    }

    private void init(ChannelHandlerContext ctx) {

	cc = ctx.channel();

	((CommCore.ExecutionContextThread) Thread.currentThread())
		.executionThread(cc
			.attr(NioSocketCommChannel.EXECUTION_CONTEXT).get());
    }
}
