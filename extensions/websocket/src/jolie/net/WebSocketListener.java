/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jolie.net;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import java.net.InetSocketAddress;
import jolie.Interpreter;
import jolie.net.ext.CommProtocolFactory;
import jolie.net.ports.InputPort;
import jolie.net.protocols.AsyncCommProtocol;
import jolie.net.protocols.CommProtocol;

/**
 *
 * @author martin
 */
public class WebSocketListener extends CommListener
{
	private final ServerBootstrap bootstrap;
	private Channel serverChannel;
	private final EventLoopGroup bossGroup;
	private final EventLoopGroup workerGroup;

	public WebSocketListener( Interpreter interpreter, CommProtocolFactory protocolFactory, InputPort inputPort, EventLoopGroup bossGroup, EventLoopGroup workerGroup )
	{
		super( interpreter, protocolFactory, inputPort );
		bootstrap = new ServerBootstrap();
		this.bossGroup = bossGroup;
		this.workerGroup = workerGroup;
	}

	@Override
	public void shutdown()
	{
		if ( serverChannel != null ) {
			try {
				serverChannel.close().sync();
			} catch( InterruptedException ex ) {
				interpreter().logWarning( ex );
			}
		}
	}

	@Override
	public void init()
	{
		try {
			bootstrap.group( bossGroup, workerGroup )
				.channel( NioServerSocketChannel.class )
				.option( ChannelOption.SO_BACKLOG, 4096 )
				.option( ChannelOption.SO_LINGER, NioSocketCommChannel.SO_LINGER )
				.childHandler( new ChannelInitializer<SocketChannel>()
				{

					@Override
					protected void initChannel( SocketChannel ch ) throws Exception
					{
						CommProtocol protocol = createProtocol();
						assert (protocol instanceof AsyncCommProtocol);
						
						WebSocketProtocol websocketProtocol = new WebSocketProtocol( inputPort().protocolConfigurationPath(), protocol );

						WebSocketCommChannel channel = new WebSocketCommChannel( null, (AsyncCommProtocol) protocol );
						channel.setParentInputPort( inputPort() );

						ChannelPipeline p = ch.pipeline();
						p.addLast(new HttpServerCodec());
						p.addLast(new HttpObjectAggregator(65536));
						p.addLast(new WebSocketServerCompressionHandler());
						p.addLast(new WebSocketServerProtocolHandler( "/", null, true ));
						
						websocketProtocol.initialize( interpreter().initContext() ); // Use init context to initialize protocol.
						websocketProtocol.setupPipeline( p );
						
						channel.setChanel( ch );
						p.addLast(channel.getJolieHandler() );
						ch.attr( NioSocketCommChannel.COMMCHANNEL ).set( channel );
					}
				} );
			ChannelFuture f = bootstrap.bind( new InetSocketAddress( inputPort().location().getPort() ) ).sync();
			serverChannel = f.channel();
		} catch( InterruptedException ioe ) {
			interpreter().logWarning( ioe );
		}
	}

	@Override
	public void run()
	{
//		try {
//			serverChannel.closeFuture().sync();
//		} catch( InterruptedException ioe ) {
//			interpreter().logWarning( ioe );
//		}
	}
}
