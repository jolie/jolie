/*
 *   Copyright (C) 2017 by Stefano Pio Zingaro <stefanopio.zingaro@unibo.it>  
 *   Copyright (C) 2017 by Saverio Giallorenzo <saverio.giallorenzo@gmail.com>
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
package jolie.net;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;

import jolie.net.ports.Port;
import jolie.net.protocols.AsyncCommProtocol;
import jolie.ExecutionThread;
import jolie.net.ports.InputPort;
import jolie.net.ports.OutputPort;
import jolie.net.serial.JsscChannel;
import jolie.net.serial.JsscDeviceAddress;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SerialCommChannel extends StreamingCommChannel {

	public final static String CHANNEL_HANDLER_NAME = "STREAMING-CHANNEL-HANDLER";

	private Bootstrap bootstrap;
	protected CompletableFuture<CommMessage> waitingForMsg = null;
	protected StreamingCommChannelHandler commChannelHandler;
	private ChannelPipeline channelPipeline;

	public SerialCommChannel( URI location, AsyncCommProtocol protocol ) {
		super( location, protocol );
		this.commChannelHandler = new StreamingCommChannelHandler( this );
	}

	@Override
	public StreamingCommChannelHandler getChannelHandler() {
		return commChannelHandler;
	}

	@Override
	protected CommMessage recvImpl() throws IOException {
		// This is blocking to integrate with existing CommCore and ExecutionThreads.
		try {
			if ( waitingForMsg != null ) {
				throw new UnsupportedOperationException( "Waiting for multiple messages is currently not supported!" );
			}
			waitingForMsg = new CompletableFuture<>();
			CommMessage msg = waitingForMsg.get();
			waitingForMsg = null;
			return msg;
		} catch ( InterruptedException | ExecutionException ex ) {
			Logger.getLogger( NioSocketCommChannel.class.getName() ).log( Level.SEVERE, null, ex );
		}
		return null;
	}

	protected void completeRead( CommMessage message ) {
		while ( waitingForMsg == null ) {
			// spinlock
		}
		if ( waitingForMsg == null ) {
			throw new IllegalStateException( "No pending read to complete!" );
		} else {
			waitingForMsg.complete( message );
		}
	}

	@Override
	protected void sendImpl( CommMessage message ) throws IOException {
		try {
			commChannelHandler.write( message.setExecutionThread( ExecutionThread.currentThread() ) ).sync();
		} catch ( InterruptedException ex ) {
			throw new IOException( ex );
		}
	}

	@Override
	protected void closeImpl() throws IOException {
		try {
			commChannelHandler.close().sync();
		} catch ( InterruptedException ex ) {
			throw new IOException( ex );
		}
	}

	static SerialCommChannel createChannel( URI location, AsyncCommProtocol protocol, EventLoopGroup workerGroup, Port port ) {

		SerialCommChannel channel = new SerialCommChannel( location, protocol );

		channel.bootstrap = new Bootstrap()
			.group( workerGroup )
			.channel( JsscChannel.class )
			.handler( new ChannelInitializer<JsscChannel>() {
				@Override
				public void initChannel( JsscChannel ch ) throws Exception {

					ChannelPipeline pipeline = ch.pipeline();
					if ( port instanceof InputPort ) {
						channel.setParentInputPort( ( InputPort ) port );
					}
					if ( port instanceof OutputPort ) {
						channel.setParentOutputPort( ( OutputPort ) port );
					}
					protocol.setChannel( channel );
					channel.setChannelPipeline( pipeline );
					protocol.setupPipeline( ch.pipeline() );
					pipeline.addLast( "commMessageDecoder", channel.commChannelHandler );
				}
			} );

		return channel;
	}

	public ChannelFuture connect( URI location ) {
		String port = System.getProperty( "port", location.toString().substring( 7 ) );
		return bootstrap.connect( new JsscDeviceAddress( port ) );
	}

	public ChannelFuture initChannel() {
		return bootstrap.register();
	}

	public ChannelPipeline getChannelPipeline() {
		return this.channelPipeline;
	}

	private void setChannelPipeline( ChannelPipeline p ) {
		this.channelPipeline = p;
	}
}
