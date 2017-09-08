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
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import java.util.HashMap;

import java.util.List;
import java.util.Map;

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
    private final Map<Integer, MqttPublishMessage> qos2pendingPublish = new HashMap<>();

    public InputPortHandler(MqttProtocol mp) {
	this.mp = mp;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, CommMessage in,
	    List<Object> out) throws Exception {
        // TODO: Manage faults, e.g., non-correlating messages etc. We need to match those messages with topics
		MqttPublishMessage mpm = mp.send_response( in );
		if( !mpm.fixedHeader().qosLevel().equals( MqttQoS.EXACTLY_ONCE ) || in.isFault() ){
			cc.attr( NioSocketCommChannel.SEND_RELEASE ).get().get( (int) in.id() ).release();
		}
		out.add(mpm);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, MqttMessage in,
	    List<Object> out) throws Exception {

//	init(ctx);
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
		mp.recv_pub(cc, mpmIn);
		if (mpmIn.fixedHeader().qosLevel().equals(MqttQoS.EXACTLY_ONCE)) {
		    qos2pendingPublish.put( mpmIn.variableHeader().messageId(), mpmIn );
		} else {
		    CommMessage cmReq = mp.recv_request(mpmIn);
		    out.add(cmReq);
		}

		break;
	    case PUBREC:
		mp.handlePubrec(cc, in);
		break;
	    case PUBREL:
		mp.handlePubrel(cc, in);
                int messageID = ( (MqttMessageIdVariableHeader) in.variableHeader() ).messageId();
                MqttPublishMessage pendigPublishReception = qos2pendingPublish.remove( messageID );
		if ( pendigPublishReception != null) {
		    CommMessage cmReq = mp.recv_request( pendigPublishReception );
		    out.add(cmReq);
		}
                break;
            case PUBCOMP:
                messageID = ( (MqttMessageIdVariableHeader) in.variableHeader() ).messageId();
                cc.attr( NioSocketCommChannel.SEND_RELEASE ).get().get( messageID ).release();
                break;
	}
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
		cc = ctx.channel();
		((CommCore.ExecutionContextThread) Thread.currentThread())
			.executionThread(cc
				.attr(NioSocketCommChannel.EXECUTION_CONTEXT).get());
		cc.writeAndFlush(mp.connectMsg());
		mp.checkDebug(ctx.pipeline());
    }

//	@Override
//	public void channelRegistered( ChannelHandlerContext ctx ){
//		cc = ctx.channel();
//		((CommCore.ExecutionContextThread) Thread.currentThread())
//			.executionThread(cc
//				.attr(NioSocketCommChannel.EXECUTION_CONTEXT).get());
//		cc.writeAndFlush(mp.connectMsg());
//		mp.checkDebug(ctx.pipeline());
//	}
	
//    private void init(ChannelHandlerContext ctx) {
//	cc = ctx.channel();
//    }
}
