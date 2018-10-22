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
package jolie.net;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import jolie.Interpreter;
import jolie.net.ext.CommProtocolFactory;
import jolie.net.ports.InputPort;
import jolie.net.protocols.AsyncCommProtocol;
import jolie.net.protocols.CommProtocol;

public class DatagramListener extends CommListener
{

	private final int inboundPort;
	private final String inboundAddress;
	private final EventLoopGroup workerGroup;
	private final ReentrantReadWriteLock responseChannels;

	private Channel serverChannel;

	public DatagramListener(
		Interpreter interpreter,
		CommProtocolFactory protocolFactory,
		InputPort inputPort,
		EventLoopGroup workerGroup
	)
	{
		super( interpreter, protocolFactory, inputPort );
		this.responseChannels = new ReentrantReadWriteLock();
		this.inboundPort = inputPort.location().getPort();
		this.inboundAddress = inputPort.location().getHost();
		this.workerGroup = workerGroup;
	}

	@Override
	public void shutdown()
	{
		responseChannels.writeLock().lock();
		try {
			if ( serverChannel != null ) {
				serverChannel.close();
			}
		} finally {
			workerGroup.shutdownGracefully();
			responseChannels.writeLock().unlock();
		}
	}

	@Override
	public void run()
	{
		try {
			ServerBootstrap bootstrap = new ServerBootstrap()
				.group( workerGroup )
				.channel( UdpServerChannel.class )
				.childHandler( new ChannelInitializer<Channel>()
				{
					@Override
					protected void initChannel( Channel ch ) throws Exception
					{
						CommProtocol protocol = createProtocol();
						assert (protocol instanceof AsyncCommProtocol);

						ChannelPipeline p = ch.pipeline();
						((AsyncCommProtocol) protocol).setupPipeline( p );

						InetSocketAddress isa = (InetSocketAddress) ch.remoteAddress();
						URI location = new URI( "datagram", null, isa.getHostName(), isa.getPort(), null, null, null );
						DatagramCommChannel channel = DatagramCommChannel.createChannel( location, ((AsyncCommProtocol) protocol), workerGroup, inputPort() );

						protocol.setChannel( channel );
						channel.setParentInputPort( inputPort() );

						channel.bind( new InetSocketAddress( 0 ) ).sync();
						p.addFirst( new ChannelOutboundHandlerAdapter()
						{
							@Override
							public void flush( ChannelHandlerContext ctx ) throws Exception
							{
								ctx.flush();
							}
						} );
						p.addLast( "COMM MESSAGE INBOUND", channel.commChannelHandler );
						p.addLast( new ChannelInboundHandlerAdapter()
						{
							@Override
							public void exceptionCaught( ChannelHandlerContext ctx, Throwable cause ) throws Exception
							{
								//cause.printStackTrace();
								ctx.close();
								serverChannel.close();
							}
						} );
						((AsyncCommProtocol) protocol).setInitExecutionThread( interpreter().initThread() );
					}
				} );

			ChannelFuture f = bootstrap.bind( inboundAddress, inboundPort ).syncUninterruptibly();
			this.serverChannel = f.channel();
			serverChannel.closeFuture().sync();
		} catch( InterruptedException ioe ) {
			interpreter().logWarning( ioe );
		} finally {
			shutdown();
		}
	}
}
