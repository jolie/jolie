package jolie.net;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;
import jolie.net.protocols.AsyncCommProtocol;
import jolie.util.Helpers;

/**
 *
 * @author martin
 */
public class NioSocketCommChannel extends StreamingCommChannel
{

	public static AttributeKey<CommChannel> COMMCHANNEL = AttributeKey.valueOf( "CommChannel" );

	static NioSocketCommChannel CreateChannel( ChannelPool get, URI location, AsyncCommProtocol asyncCommProtocol, EventLoopGroup workerGroup )
	{
		throw new UnsupportedOperationException( "Not supported yet." );
	}

	private Bootstrap bootstrap;
	protected static final int SO_LINGER = 60 * 1000;
	protected CompletableFuture<CommMessage> waitingForMsg = null;
	protected final JolieCommChannelHandler jolieCommChannelHandler;
	protected Channel channel;
	private final ChannelPool channelPool;

	public NioSocketCommChannel( URI location, AsyncCommProtocol protocol )
	{
		this( location, protocol, null );
	}
	
	public NioSocketCommChannel( URI location, AsyncCommProtocol protocol, ChannelPool pool )
	{
		super( location, protocol );
		jolieCommChannelHandler = new JolieCommChannelHandler( this );
		channelPool = pool;
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
					channel.setChanel( ch );
					p.addLast(channel.jolieCommChannelHandler );
					ch.attr( COMMCHANNEL ).set( channel );
				}
			} );
		return channel;
	}
	
	public static NioSocketCommChannel CreateChannelFromPool( ChannelPool pool, URI location, AsyncCommProtocol protocol, EventLoopGroup workerGroup ) throws InterruptedException, ExecutionException
	{
		Channel ch = pool.acquire().get();
		clearChannelPipeline(ch);
		NioSocketCommChannel channel = new NioSocketCommChannel( location, protocol, pool );
		ChannelPipeline p = ch.pipeline();
		protocol.setupPipeline( p );
		channel.setChanel( ch );
		p.addLast(channel.jolieCommChannelHandler );
		ch.attr( COMMCHANNEL ).set( channel );
		return channel;
	}
	
	public void setChanel(Channel ch) {
		channel = ch;
		jolieCommChannelHandler.setChannel( ch );
	}
	
	public JolieCommChannelHandler getJolieHandler() {
		return jolieCommChannelHandler;
	}
	
	protected static void clearChannelPipeline( Channel ch ) {
		ChannelPipeline pipeline = ch.pipeline();
		for( Entry<String, ChannelHandler> entry: pipeline.toMap().entrySet()) {
			pipeline.remove( entry.getValue() );
		}
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
	protected void sendImpl( StatefulMessage msg, Runnable successHandler, Consumer<Throwable> failureHandler ) throws IOException
	{
		jolieCommChannelHandler.write( msg )
			.addListener( f -> {
				Helpers.lockAndThen( lock, () -> {
					if ( f.isSuccess() ) {
						successHandler.run();
					} else if (f.cause() instanceof RejectedExecutionException) {
						// Ignore. Interpreter must be shutting down.
					} else {
						failureHandler.accept( f.cause() );
					}
				});
			});
	}

	@Override
	protected void closeImpl() throws IOException
	{
		jolieCommChannelHandler.close();
	}
	
//	@Override
//	protected void disposeForInputImpl()
//		throws IOException
//	{
//		Helpers.lockAndThen( lock, () -> Interpreter.getInstance().commCore().registerForSelection( this ) );
//	}
//
	@Override
	protected void releaseImpl()
		throws IOException
	{
		Helpers.lockAndThen( lock, () -> {
			if (channelPool != null)
				channelPool.release( channel );
			else
				super.releaseImpl();
		} );
	}
}
