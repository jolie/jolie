/*
 *   Copyright (C) 2017 by Stefano Pio Zingaro <stefanopio.zingaro@unibo.it>  
 *   Copyright (C) 2017 by Saverio Giallorenzo <saverio.giallorenzo@gmail.com>
 *   Copyright (C) 2017 by Ivan Lanese <ivan.lanese@unibo.com>                 
 *   Copyright (C) 2017 by Maurizio Gabbrielli <maurizio.gabbrielli@unibo.com> 
 *                                                                             
 *   This program is free software; you can redistribute it and/or modify      
 *   it under the terms of the GNU Library General Public License as           
 *   published by the Free Software Foundation; either version 2 of the        
 *   License, or (at your option) any later version.                           
 *                                                                             
 *   This program is distributed in the hope that it will be useful,           
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of            
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             
 *   GNU General Public License for more details.                              
 *                                                                             
 *   You should have received a copy of the GNU Library General Public         
 *   License along with this program; if not, write to the                     
 *   Free Software Foundation, Inc.,                                           
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.                 
 *                                                                             
 *   For details about the authors of this software, see the AUTHORS file.     
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
import java.util.HashMap;

import java.util.List;
import java.util.Map;

import jolie.net.CommCore;
import jolie.net.CommMessage;
import jolie.net.MqttProtocol;
import jolie.net.NioSocketCommChannel;
import jolie.net.protocols.AsyncCommProtocol;

public class InputResponseHandler
	extends MessageToMessageCodec<MqttMessage, CommMessage> {

	private final MqttProtocol mp;
	private Channel cc;
	private final Map<Integer, MqttPublishMessage> qos2pendingPublish = new HashMap<>();
	private String topicResponse = "";
	private CommMessage cmResp;
	private CommMessage cmReq;
//    private CompletableFuture<Void> active;

	public InputResponseHandler( AsyncCommProtocol mp ) {
		this.mp = (MqttProtocol) mp;
	}

	public InputResponseHandler setTopicResponse( String topicResponse ) {
		this.topicResponse = topicResponse;
		return this;
	}

	public InputResponseHandler setRequestCommMessage( CommMessage m ) {
		this.cmReq = m;
		return this;
	}

	@Override
	protected void encode( ChannelHandlerContext ctx, CommMessage in,
		List<Object> out ) throws Exception {
		init( ctx );
    cmResp = in;
//		if ( mp.isOneWay( cmResp.operationName() ) ) {
//			mp.markAsSentAndStopPing( cc, ( int ) cmResp.id() );
//		}
		out.add( mp.connectMsg() );
	}

	@Override
	protected void decode( ChannelHandlerContext ctx, MqttMessage in,
		List<Object> out ) throws Exception {

		switch ( in.fixedHeader().messageType() ) {
			case CONNACK:
				MqttConnectReturnCode crc = ( ( MqttConnAckMessage ) in ).variableHeader().connectReturnCode();
				if ( crc.equals( MqttConnectReturnCode.CONNECTION_ACCEPTED ) ) {
					try {
						mp.startPing( cc.pipeline() );
						MqttPublishMessage mpm = mp.send_response(
							new CommMessage(
								cmReq.id(),
								cmResp.operationName(),
								cmResp.resourcePath(),
								cmResp.value(),
								cmResp.fault() ), topicResponse );
						// if the response has QoS = 0 we can directly mark the message as sent
						cc.writeAndFlush( mpm );
						if ( MqttProtocol.getQoS( mpm ).equals( MqttQoS.AT_MOST_ONCE ) /*|| in.isFault() */ ) {
							mp.markAsSentAndStopPing( cc, ( int ) cmResp.id() );
						}
					} catch ( Exception e ) {
						e.printStackTrace();
					}
				}
				break;
			case PUBLISH:
				System.out.println( "InputResponseHandlers should not receive PUBLISHs" );
				break;
			case PUBREC:
				mp.handlePubrec( cc, in );
				break;
			case PUBREL:
				System.out.println( "InputResponseHandlers should not receive PUBRELs" );
				break;
      case PUBACK:
      case PUBCOMP:
        // WE MARK THE MESSAGE with QoS = 1 as sent
				mp.markAsSentAndStopPing( cc, ( int ) cmResp.id() );
//				int messageID = MqttProtocol.getMessageID( in );
				break;
		}
	}

	public void init( ChannelHandlerContext ctx ) throws Exception {
		cc = ctx.channel();
		( ( CommCore.ExecutionContextThread ) Thread.currentThread() )
			.executionThread( cc.attr( NioSocketCommChannel.EXECUTION_CONTEXT ).get() );
		mp.checkDebug( cc.pipeline() );
	}

}
