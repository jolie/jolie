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
import io.netty.channel.Channel;
import jolie.Interpreter;

import jolie.net.ext.CommProtocolFactory;
import jolie.net.ports.InputPort;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;
import jolie.net.protocols.AsyncCommProtocol;
import jolie.net.serial.JsscChannel;
import jolie.net.serial.JsscDeviceAddress;

public class SerialListener extends CommListener {

	private final EventLoopGroup workerGroup;
	private Channel serverChannel;
	private URI location;

	public SerialListener( Interpreter interpreter, CommProtocolFactory protocolFactory, InputPort inputPort, EventLoopGroup workerGroup ) {

		super( interpreter, protocolFactory, inputPort );
		this.workerGroup = workerGroup;
		this.location = inputPort.location();
	}

	@Override
	public void run() {

		try {

			Bootstrap bootstrap = new Bootstrap()
				.group( workerGroup )
				.channel( JsscChannel.class )
				.handler( new ChannelInitializer<JsscChannel>() {
					@Override
					public void initChannel( JsscChannel ch ) throws Exception {

						ChannelPipeline pipeline = ch.pipeline();
						AsyncCommProtocol protocol = ( AsyncCommProtocol ) createProtocol();
						protocol.setupPipeline( ch.pipeline() );
						SerialCommChannel commChannel = new SerialCommChannel( location, protocol );
						protocol.setChannel( commChannel );
						commChannel.setParentInputPort( inputPort() );
						pipeline.addLast( "commMessageDecoder", commChannel.commChannelHandler );
					}
				} );

			String port = System.getProperty( "port", location.toString().substring( 7 ) );
			ChannelFuture f = bootstrap.connect( new JsscDeviceAddress( port ) ).sync();
			serverChannel = f.channel();
			serverChannel.closeFuture().sync();

		} catch ( InterruptedException ex ) {
			Logger.getLogger( SerialListener.class.getName() ).log( Level.SEVERE, null, ex );
		} finally {
			workerGroup.shutdownGracefully();
		}
	}

	@Override
	public void shutdown() {
		serverChannel.close();
	}
}