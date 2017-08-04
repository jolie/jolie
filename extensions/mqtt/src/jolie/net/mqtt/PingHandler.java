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
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.ScheduledFuture;

import java.util.concurrent.TimeUnit;

public class PingHandler extends ChannelInboundHandlerAdapter {

    private final int keepaliveSeconds;
    private ScheduledFuture<?> pingRespTimeout;

    public PingHandler(int keepaliveSeconds) {
	this.keepaliveSeconds = keepaliveSeconds;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
	    throws Exception {

	if (!(msg instanceof MqttMessage)) {
	    ctx.fireChannelRead(msg);
	    return;
	}

	MqttMessage message = (MqttMessage) msg;

	if (message.fixedHeader().messageType().equals(MqttMessageType.PINGREQ)) {
	    this.handlePingReq(ctx.channel());
	} else if (message.fixedHeader().messageType()
		.equals(MqttMessageType.PINGRESP)) {
	    this.handlePingResp();
	} else {
	    ctx.fireChannelRead(ReferenceCountUtil.retain(msg));
	}
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt)
	    throws Exception {

	super.userEventTriggered(ctx, evt);

	if (evt instanceof IdleStateEvent) {
	    IdleStateEvent event = (IdleStateEvent) evt;
	    switch (event.state()) {
		case READER_IDLE:

		    break;
		case WRITER_IDLE:

		    this.sendPingReq(ctx.channel());
		    break;
	    }
	}
    }

    private void sendPingReq(Channel channel) {

	MqttFixedHeader fixedHeader = new MqttFixedHeader(
		MqttMessageType.PINGREQ,
		false,
		MqttQoS.AT_MOST_ONCE,
		false,
		0);
	channel.writeAndFlush(new MqttMessage(fixedHeader));

	if (this.pingRespTimeout != null) {
	    this.pingRespTimeout = channel.eventLoop().schedule(() -> {
		MqttFixedHeader fixedHeader2 = new MqttFixedHeader(
			MqttMessageType.DISCONNECT,
			false,
			MqttQoS.AT_MOST_ONCE,
			false,
			0);
		channel.writeAndFlush(new MqttMessage(fixedHeader2))
			.addListener(ChannelFutureListener.CLOSE);
	    }, this.keepaliveSeconds, TimeUnit.SECONDS
	    );
	}
    }

    private void handlePingReq(Channel channel) {

	MqttFixedHeader fixedHeader = new MqttFixedHeader(
		MqttMessageType.PINGRESP,
		false,
		MqttQoS.AT_MOST_ONCE,
		false,
		0);
	channel.writeAndFlush(new MqttMessage(fixedHeader));
    }

    private void handlePingResp() {

	if (this.pingRespTimeout != null
		&& !this.pingRespTimeout.isCancelled()
		&& !this.pingRespTimeout.isDone()) {
	    this.pingRespTimeout.cancel(true);
	    this.pingRespTimeout = null;
	}
    }
}
