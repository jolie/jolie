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
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import java.io.IOException;
import java.net.URI;
import jolie.ExecutionThread;
import jolie.net.ports.InputPort;
import jolie.net.ports.OutputPort;
import jolie.net.ports.Port;
import jolie.net.protocols.AsyncCommProtocol;
import jolie.net.serial.JSCC;
import jolie.net.serial.JSCDeviceAddress;

public class SerialCommChannel extends StreamingCommChannel {

	protected StreamingCommChannelHandler commChannelHandler;
	private Bootstrap b;
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
		throw new UnsupportedOperationException( "Not supported yet." );
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

		SerialCommChannel c = new SerialCommChannel( location, protocol );

		c.b = new Bootstrap();
		c.b.group( workerGroup );
		c.b.channel( JSCC.class );
		c.b.handler( new ChannelInitializer<JSCC>() {

			@Override
			public void initChannel( JSCC ch ) throws Exception {

				ChannelPipeline p = ch.pipeline();
				if ( port instanceof InputPort ) {
					c.setParentInputPort( ( InputPort ) port );
				}
				if ( port instanceof OutputPort ) {
					c.setParentOutputPort( ( OutputPort ) port );
				}
				protocol.setChannel( c );
				c.setChannelPipeline( p );
				p.addLast( new LoggingHandler( LogLevel.INFO ) );
				protocol.setupPipeline( p );
				p.addLast( "STREAMING-CHANNEL-HANDLER", c.commChannelHandler );
			}

		} );

		return c;
	}

	ChannelFuture connect( URI location ) {
		String port = System.getProperty( "port", location.toString().substring( 7 ) );
		return b.connect( new JSCDeviceAddress( port ) );
	}

	private void setChannelPipeline( ChannelPipeline p ) {
		this.channelPipeline = p;
	}

	public ChannelPipeline getChannelPipeline() {
		return channelPipeline;
	}
}
