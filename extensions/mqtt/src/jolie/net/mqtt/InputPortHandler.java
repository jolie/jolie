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
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import java.util.List;
import jolie.net.CommCore;
import jolie.net.CommMessage;
import jolie.net.MqttProtocol;
import jolie.net.NioSocketCommChannel;

public class InputPortHandler
	extends MessageToMessageCodec<MqttMessage, CommMessage> {

    private final MqttProtocol protocol;
    private Channel channel;
    private CommMessage cmResp;

    public InputPortHandler(MqttProtocol protocol) {
	this.protocol = protocol;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, CommMessage cm,
	    List<Object> out) throws Exception {

	init(ctx);

	cmResp = CommMessage.createResponse(cm, cm.value());

	MqttPublishMessage mpm = protocol.publishMsg(cmResp.id(),
		protocol.getTopicResponse(),
		cmResp.value(),
		protocol.qos(cmResp.operationName()));

	out.add(mpm);
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
		    protocol.startPing(channel.pipeline());
		    MqttSubscribeMessage msm = protocol.subscribeMsg(
			    CommMessage.getNewMessageId(),
			    protocol.topics(),
			    protocol.qos());
		    channel.writeAndFlush(msm);
		}
		break;
	    case PUBLISH:

		MqttPublishMessage mpm = (MqttPublishMessage) mm;
		CommMessage cmReq = protocol.requestCommMsg(mpm);
		out.add(cmReq);
		protocol.recPub(channel, mpm);
		break;
	    case PUBREC:
	    case PUBREL:

		protocol.sendAck(channel,
			(MqttMessageIdVariableHeader) mm.variableHeader(),
			mm.fixedHeader().messageType());
		break;
	}
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

	init(ctx);

	MqttConnectMessage mcm = protocol.connectMsg();
	channel.writeAndFlush(mcm);
    }

    private void init(ChannelHandlerContext ctx) {

	channel = ctx.channel();
	((CommCore.ExecutionContextThread) Thread.currentThread())
		.executionThread(
			channel.attr(NioSocketCommChannel.EXECUTION_CONTEXT)
				.get());
    }
}
