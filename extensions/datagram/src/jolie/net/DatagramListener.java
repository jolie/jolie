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

import jolie.Interpreter;

import jolie.net.ext.CommProtocolFactory;
import jolie.net.ports.InputPort;
import jolie.net.protocols.AsyncCommProtocol;
import jolie.net.protocols.CommProtocol;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;

import java.net.InetSocketAddress;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DatagramListener extends CommListener {

	private final EventLoopGroup workerGroup;
	private final InetSocketAddress localAddress;
	private Channel clientChannel;
	private Channel serverChannel;
	public static final ReentrantReadWriteLock responseChannels
		= new ReentrantReadWriteLock();

	public DatagramListener(
		Interpreter interpreter,
		CommProtocolFactory protocolFactory,
		InputPort inputPort,
		EventLoopGroup workerGroup
	) {
		super( interpreter, protocolFactory, inputPort );
		this.workerGroup = workerGroup;
		this.localAddress = new InetSocketAddress( inputPort().location().getHost(),
			inputPort().location().getPort() );
	}

	@Override
	public void shutdown() {
		if ( serverChannel.isOpen() ) {
//      System.out.println("--- PRENDO WRITE LOCK --- " + Thread.currentThread());
			responseChannels.writeLock().lock();
			try {
//        System.out.println("!!! Canale Chiuso !!!");
				serverChannel.close();
			} finally {
//        System.out.println("--- RILASCIO WRITE LOCK --- " + Thread.currentThread());
				responseChannels.writeLock().unlock();
			}
		}
	}

	public static void addResponseChannel() {
//    System.out.println("--- PRENDO READ LOCK --- " + Thread.currentThread());
		responseChannels.readLock().lock();
	}

	public static void removeResponseChannel() {
//    System.out.println("--- RILASCIO READ LOCK --- " + Thread.currentThread());
		responseChannels.readLock().unlock();
	}

	@Override
	public void run() {

		Bootstrap b = new Bootstrap();
		b.group( workerGroup ).channel( NioDatagramChannel.class );
		b.localAddress( localAddress );
		b.handler( new SimpleChannelInboundHandler<DatagramPacket>() {

			@Override
			protected void channelRead0( ChannelHandlerContext ctx, DatagramPacket msg )
				throws Exception {

				CommProtocol protocol = createProtocol();
				if ( !( protocol instanceof AsyncCommProtocol ) ) {
					throw new UnsupportedCommProtocolException( "Use an async protocol" );
				}

				DatagramCommChannel commChannel = DatagramCommChannel.createChannel(
					null,
					( AsyncCommProtocol ) protocol,
					workerGroup,
					inputPort(),
					new DatagramPacketEncoder( msg.sender() )
				);

				ChannelFuture f = commChannel.bind( new InetSocketAddress( 0 ) ).sync();
				clientChannel = f.channel();

				clientChannel.pipeline().addFirst( "FLUSHER", new ChannelOutboundHandlerAdapter() {

					@Override
					public void flush( ChannelHandlerContext ctx )
						throws Exception {
						removeResponseChannel();
						super.flush( ctx );
					}
				} );
				clientChannel.pipeline().fireChannelRead( msg.content().retain() );
			}
		} );

		try {
			ChannelFuture f = b.bind().sync();
			serverChannel = f.channel();
			serverChannel.closeFuture().sync();
		} catch ( InterruptedException ex ) {
			Interpreter.getInstance().logWarning( ex );
		} finally {
			workerGroup.shutdownGracefully();
		}
	}
}
