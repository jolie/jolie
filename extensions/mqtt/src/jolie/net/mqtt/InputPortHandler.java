/*******************************************************************************
 *   Copyright (C) 2017 by Stefano Pio Zingaro <stefanopio.zingaro@unibo.it>   *
 *   Copyright (C) 2017 by Saverio Giallorenzo <saverio.giallorenzo@gmail.com> *
 *                                                                             *
 *   This program is free software; you can redistribute it and/or modify      *
 *   it under the terms of the GNU Library General Public License as           *
 *   published by the Free Software Foundation; either version 2 of the        *
 *   License, or (at your option) any later version.                           *
 *                                                                             *
 *   This program is distributed in the hope that it will be useful,           *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of            *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             *
 *   GNU General Public License for more details.                              *
 *                                                                             *
 *   You should have received a copy of the GNU Library General Public         *
 *   License along with this program; if not, write to the                     *
 *   Free Software Foundation, Inc.,                                           *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.                 *
 *                                                                             *
 *   For details about the authors of this software, see the AUTHORS file.     *
 *******************************************************************************/

package jolie.net.mqtt;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import java.net.URI;
import java.util.HashMap;

import java.util.List;
import java.util.Map;
import jolie.net.CommChannel;

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
		private final CommChannel commChannel;

    public InputPortHandler( MqttProtocol mp, CommChannel cm ) {
        this.mp = mp;
				this.commChannel = cm;
    }

    @Override
    protected void encode( ChannelHandlerContext ctx, CommMessage in,
      List<Object> out ) throws Exception {
        // THE ACK TO A ONE-WAY COMING FROM COMMCORE, RELEASING AND SENDING PING INSTEAD
        mp.releaseMessage( ( int ) in.id() );
        out.add( MqttProtocol.getPingMessage() );
    }

    @Override
    protected void decode(
      ChannelHandlerContext ctx,
      MqttMessage in,
      List<Object> out )
      throws Exception {
        switch ( in.fixedHeader().messageType() ) {
            case CONNACK:
                MqttConnectReturnCode crc = ( ( MqttConnAckMessage ) in ).variableHeader().connectReturnCode();
                if ( crc.equals( MqttConnectReturnCode.CONNECTION_ACCEPTED ) ) {
                    // WE ARE CONNECTED, WE CAN PROCEED TO SUBSCRIBE TO ALL MAPPED TOPICS IN THE INPUTPORT
                    // AND START PINGING
                    mp.send_subRequest( cc );
                }
                break;
            case PUBLISH:
                // TODO support wildcards and variables
                MqttPublishMessage mpmIn = ( ( MqttPublishMessage ) in ).copy();
                // we send back the appropriate response (PUBACK, PUBREC)
                mp.recv_pub( cc, mpmIn );
                // we handle the reception of the message (and possibly wait for message release)
                handleRecepitonPolicy( ctx, out, mpmIn );
                break;
            case PUBREC:
                System.out.println( "InputHandlers should not receive PUBRECs" );
                //mp.handlePubrec( cc, in );
                break;
            case PUBREL:
                // we send back a PUBCOMP
                mp.handlePubrel( cc, in );
                // we get the message to be handled (handleReceivedMessage will take care of the removal)
                MqttPublishMessage pendigPublishReception = qos2pendingPublish.get( MqttProtocol.getMessageID( in ) );
                if ( pendigPublishReception != null ) {
                    handleRecepitonPolicy( ctx, out, pendigPublishReception );
                }
                break;
            case PUBCOMP:
                System.out.println( "InputHandlers should not receive PUBCOMPs" );
                break;
        }
    }

    @Override
    public void channelActive( ChannelHandlerContext ctx ) throws Exception {
        cc = ctx.channel();
        ( ( CommCore.ExecutionContextThread ) Thread.currentThread() )
          .executionThread( cc
            .attr( NioSocketCommChannel.EXECUTION_CONTEXT ).get() );
        mp.checkDebug( ctx.pipeline() );
        cc.writeAndFlush( mp.connectMsg() );
    }

    private void handleRecepitonPolicy(
      ChannelHandlerContext ctx,
      List<Object> out,
      MqttPublishMessage m )
      throws InterruptedException, Exception {
        if ( MqttProtocol.getQoS( m ).equals( MqttQoS.EXACTLY_ONCE ) ) {
            if ( qos2pendingPublish.containsKey( MqttProtocol.getMessageID( m ) ) ) {
                // we can remove it because we are handling the PUBREL
                qos2pendingPublish.remove( MqttProtocol.getMessageID( m ) );
                // and we handle the reception of the message
                handleMessageReception( ctx, out, m );
            } else {
                // we store the message and wait for its release PUBREL
                qos2pendingPublish.put( MqttProtocol.getMessageID( m ), m );
            }
        } else {
            // it is either QoS 0 or 1 and we can handle it direcly
            handleMessageReception( ctx, out, m );
        }
    }

    private void handleMessageReception(
      ChannelHandlerContext ctx,
      List<Object> out,
      MqttPublishMessage m ) throws Exception {
        CommMessage cm = mp.recv_request( m );
        // if it is a one-way, we handle it directly
        if ( mp.isOneWay( cm.operationName() ) ) {
            out.add( cm );
        } else {
            // else we forward the message to a new channel pipeline
            InputResponseHandler ih = new InputResponseHandler( mp );
            // we store the response topic into the InputResponseHandler
            ih.setTopicResponse( mp.extractTopicResponse( m ) ).setRequestCommMessage( cm );
            // we forward the received message to the new CommChannel
						
            System.out.println( "Creating response channel" );
            NioSocketCommChannel fc = NioSocketCommChannel.createInputChannel( 
							new URI( commChannel.parentInputPort().protocolConfigurationPath().evaluate().getFirstChild( "broker" ).strValue() ),
							mp,
							ctx.channel().eventLoop().parent(), 
							commChannel.parentInputPort(), 
							ih);
            System.out.println( "Waiting for channel pipeline" );
						ChannelPipeline p = fc.getChannelPipeline();
							do{
								p = fc.getChannelPipeline();
							} while ( p == null );
              System.out.println( "Sending message to new pipeline" );
							p.fireChannelRead( cm );
//            ctx.channel().attr( NioSocketCommChannel.LISTENER ).get()
//              .createNewPubSubChannel( ih ).pipeline().fireChannelRead( cm );
        }
    }

}
