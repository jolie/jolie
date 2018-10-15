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
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.MessageToMessageEncoder;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import jolie.net.ports.InputPort;
import jolie.net.ports.OutputPort;
import jolie.net.ports.Port;
import jolie.net.protocols.AsyncCommProtocol;

public class DatagramCommChannel extends StreamingCommChannel
{

	public final static String CHANNEL_HANDLER_NAME
		= "STREAMING-CHANNEL-HANDLER";

	private Bootstrap b;
	protected CompletableFuture<CommMessage> waitingForMsg = null;
	protected StreamingCommChannelHandler commChannelHandler;
	private ChannelPipeline channelPipeline;

	public DatagramCommChannel( URI location, AsyncCommProtocol protocol )
	{
		super( location, protocol );
		this.commChannelHandler = new StreamingCommChannelHandler( this );
	}

	@Override
	public StreamingCommChannelHandler getChannelHandler()
	{
		return commChannelHandler;
	}

	/**
	 *
	 * @param channelPipeline
	 */
	public void setChannelPipeline( ChannelPipeline channelPipeline )
	{
		this.channelPipeline = channelPipeline;
	}

	/**
	 *
	 * @return
	 */
	public ChannelPipeline getChannelPipeline()
	{
		return channelPipeline;
	}

	/**
	 *
	 * @param location
	 * @param protocol
	 * @param workerGroup
	 * @param port
	 * @return
	 */
	public static DatagramCommChannel createChannel( URI location,
		AsyncCommProtocol protocol, EventLoopGroup workerGroup, Port port )
	{

//    ExecutionThread ethread = ExecutionThread.currentThread();
		DatagramCommChannel c = new DatagramCommChannel( location, protocol );

		c.b = new Bootstrap();
		c.b.group( workerGroup );
		c.b.channel( NioDatagramChannel.class );
		c.b.handler( new ChannelInitializer<NioDatagramChannel>()
		{
			@Override
			protected void initChannel( NioDatagramChannel ch ) throws Exception
			{
				ChannelPipeline p = ch.pipeline();
				if ( port instanceof InputPort ) {
					c.setParentInputPort( (InputPort) port );
				}
				if ( port instanceof OutputPort ) {
					c.setParentOutputPort( (OutputPort) port );
				}
				protocol.setChannel( c );
				c.setChannelPipeline( p );
				// inbound handler per datagram packet 
				p.addLast( new SimpleChannelInboundHandler<DatagramPacket>()
				{
					@Override
					protected void channelRead0( ChannelHandlerContext ctx, DatagramPacket msg ) throws Exception
					{
						ctx.fireChannelRead( Unpooled.copiedBuffer( msg.content() ) );
					}
				} );
				p.addLast( new MessageToMessageEncoder<ByteBuf>()
				{
					@Override
					protected void encode( ChannelHandlerContext ctx, ByteBuf msg, List<Object> out ) throws Exception
					{
						out.add( new DatagramPacket( Unpooled.copiedBuffer( (ByteBuf) msg ), new InetSocketAddress( location.getHost(), location.getPort() ) ) );
					}

				} );
				protocol.setupPipeline( p );
				p.addLast( CHANNEL_HANDLER_NAME, c.commChannelHandler );
			}
		} );

		return c;
	}

	/**
	 *
	 * @param location
	 * @return
	 * @throws InterruptedException
	 */
	public ChannelFuture connect( URI location )
		throws InterruptedException
	{

		return b.bind( new InetSocketAddress( 0 ) );
	}

	/**
	 *
	 * @param location
	 * @return
	 * @throws InterruptedException
	 */
	public ChannelFuture bind( InetSocketAddress location )
		throws InterruptedException
	{

		return b.bind( location );
	}

	/**
	 * This is blocking to integrate with existing CommCore and ExecutionThreads.
	 *
	 * @return
	 * @throws IOException
	 */
	@Override
	protected CommMessage recvImpl() throws IOException
	{
		try {
			if ( waitingForMsg != null ) {
				throw new UnsupportedOperationException( "Waiting for multiple "
					+ "messages is currently not supported!" );
			}
			waitingForMsg = new CompletableFuture<>();
			CommMessage msg = waitingForMsg.get();
			waitingForMsg = null;
			return msg;
		} catch( InterruptedException | ExecutionException ex ) {
			throw new IOException( ex );
		}
	}

	protected void completeRead( CommMessage message )
	{
		while( waitingForMsg == null ) {
			// spinlock
		}
		if ( waitingForMsg == null ) {
			throw new IllegalStateException( "No pending read to complete!" );
		} else {
			waitingForMsg.complete( message );
		}
	}

	@Override
	protected void sendImpl( CommMessage message ) throws IOException
	{
		try {
			commChannelHandler.write( message ).sync();
		} catch( InterruptedException ex ) {
			throw new IOException( ex );
		}
	}

	@Override
	protected void closeImpl() throws IOException
	{
		try {
			commChannelHandler.close().sync();
		} catch( InterruptedException ex ) {
			throw new IOException( ex );
		}
	}
}
