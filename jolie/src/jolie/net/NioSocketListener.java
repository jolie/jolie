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
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
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
public class NioSocketListener extends CommListener
{

	private final ServerBootstrap bootstrap;
	private Channel serverChannel;
	private final EventLoopGroup bossGroup;
	private final EventLoopGroup workerGroup;

	public NioSocketListener( Interpreter interpreter, CommProtocolFactory protocolFactory, InputPort inputPort, EventLoopGroup bossGroup, EventLoopGroup workerGroup )
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
			serverChannel.close();
		}
	}

	@Override
	public void run()
	{
		try {

			bootstrap.group( bossGroup, workerGroup )
				.channel( NioServerSocketChannel.class )
				.option( ChannelOption.SO_BACKLOG, 100 )
				.handler( new LoggingHandler( LogLevel.INFO ) )
				.childHandler( new ChannelInitializer<SocketChannel>()
				{

					@Override
					protected void initChannel( SocketChannel ch ) throws Exception
					{
						CommProtocol protocol = createProtocol();
						assert (protocol instanceof AsyncCommProtocol);

						NioSocketCommChannel channel = new NioSocketCommChannel( null, (AsyncCommProtocol) protocol );
						channel.setParentInputPort( inputPort() );

						//interpreter().commCore().scheduleReceive(channel, inputPort());
						ChannelPipeline p = ch.pipeline();
						((AsyncCommProtocol) protocol).setupPipeline( p );
						p.addLast( channel.nioSocketCommChannelHandler );
						ch.attr( NioSocketCommChannel.COMMCHANNEL ).set( channel );
					}
				} );
			ChannelFuture f = bootstrap.bind( new InetSocketAddress( inputPort().location().getPort() ) ).sync();
			serverChannel = f.channel();
			serverChannel.closeFuture().sync();
		} catch( InterruptedException ioe ) {
			interpreter().logWarning( ioe );
		} finally {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}

	}

}
