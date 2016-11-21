package jolie.net;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import jolie.net.protocols.AsyncCommProtocol;
import jolie.util.Helpers;

/**
 *
 * @author martin
 */
public class NioSocketCommChannel extends StreamingCommChannel
{

	public static AttributeKey<CommChannel> COMMCHANNEL = AttributeKey.valueOf( "CommChannel" );

	private Bootstrap bootstrap;
	private static final int SO_LINGER = 10000;
	protected CompletableFuture<CommMessage> waitingForMsg = null;
	protected final JolieCommChannelHandler jolieCommChannelHandler;

	public NioSocketCommChannel( URI location, AsyncCommProtocol protocol )
	{
		super( location, protocol );
		jolieCommChannelHandler = new JolieCommChannelHandler( this );
	}

	public static NioSocketCommChannel CreateChannel( URI location, AsyncCommProtocol protocol, EventLoopGroup workerGroup )
	{
		NioSocketCommChannel channel = new NioSocketCommChannel( location, protocol );
		channel.bootstrap = new Bootstrap();
		channel.bootstrap.group( workerGroup )
			.channel( NioSocketChannel.class )
			.option( ChannelOption.SO_LINGER, SO_LINGER )
			.handler(new ChannelInitializer()
			{
				@Override
				protected void initChannel( Channel ch ) throws Exception
				{
					ChannelPipeline p = ch.pipeline();
					protocol.setupPipeline( p );
					p.addLast(channel.jolieCommChannelHandler );
					ch.attr( COMMCHANNEL ).set( channel );
				}
			} );
		return channel;
	}

	protected ChannelFuture connect( URI location ) throws InterruptedException
	{
		return bootstrap
			.connect( new InetSocketAddress( location.getHost(), location.getPort() ) );
	}

	@Override
	protected CommMessage recvImpl() throws IOException
	{
		return null;
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
	protected void sendImpl( StatefulMessage msg, Function<Void, Void> completionHandler ) throws IOException
	{
		jolieCommChannelHandler.write( msg )
			.addListener(( ChannelFuture future1 ) -> {
				Helpers.lockAndThen( lock, () -> {
					
				});
				if ( future1.isSuccess() ) {
					if (completionHandler != null)
						completionHandler.apply( null );
				} else {
					throw new IOException( future1.cause() );
				}
			});
	}

	@Override
	protected void closeImpl() throws IOException
	{
		jolieCommChannelHandler.close();
	}

}
