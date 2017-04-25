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
				//.handler( new LoggingHandler( LogLevel.INFO ) )
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
						((AsyncCommProtocol) protocol).initialize( interpreter().initContext() ); // Use init context to initialize protocol.
						((AsyncCommProtocol) protocol).setupPipeline( p );
						channel.jolieCommChannelHandler.setChannel( ch );
						p.addLast(channel.jolieCommChannelHandler );
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
			//serverChannel.closeFuture().sync();
//		} catch( InterruptedException ioe ) {
//			interpreter().logWarning( ioe );
//		}
	}
}
