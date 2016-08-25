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
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jolie.SessionContext;
import jolie.net.protocols.AsyncCommProtocol;

/**
 *
 * @author martin
 */
public class NioSocketCommChannel extends StreamingCommChannel
{

	public static AttributeKey<NioSocketCommChannel> COMMCHANNEL = AttributeKey.valueOf( "CommChannel" );

	private Bootstrap bootstrap;
	private static final int SO_LINGER = 10000;
	protected CompletableFuture<CommMessage> waitingForMsg = null;
	protected final NioSocketCommChannelHandler nioSocketCommChannelHandler;
	public SessionContext context;

	public NioSocketCommChannel( URI location, AsyncCommProtocol protocol )
	{
		super( location, protocol );
		nioSocketCommChannelHandler = new NioSocketCommChannelHandler( this );
	}

	public static NioSocketCommChannel CreateChannel( URI location, AsyncCommProtocol protocol, EventLoopGroup workerGroup, SessionContext ctx )
	{
		NioSocketCommChannel channel = new NioSocketCommChannel( location, protocol );
		channel.sessionContext( ctx );
		channel.bootstrap = new Bootstrap();
		channel.bootstrap.group( workerGroup )
			.channel( NioSocketChannel.class )
			.option( ChannelOption.SO_LINGER, SO_LINGER )
			.handler( new ChannelInitializer()
			{
				@Override
				protected void initChannel( Channel ch ) throws Exception
				{
					ChannelPipeline p = ch.pipeline();
					protocol.setupPipeline( p );
					p.addLast( channel.nioSocketCommChannelHandler );
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
		// This is blocking to integrate with existing CommCore and ExecutionThreads.
		try {
			if ( waitingForMsg != null ) {
				throw new UnsupportedOperationException( "Waiting for multiple messages is currently not supported!" );
			}
			waitingForMsg = new CompletableFuture<>();
			CommMessage msg = waitingForMsg.get();
			waitingForMsg = null;
			return msg;
		} catch( InterruptedException | ExecutionException ex ) {
			Logger.getLogger( NioSocketCommChannel.class.getName() ).log( Level.SEVERE, null, ex );
		}
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
	protected void sendImpl( CommMessage message, SessionContext ctx ) throws IOException
	{
		ctx.pauseExecution();
		ChannelFuture future = nioSocketCommChannelHandler.write( message );
		future.addListener(( ChannelFuture future1 ) -> {
			if ( future1.isSuccess() ) {
				ctx.start();
			} else {
				throw new IOException( future1.cause() );
			}
		});
	}

	@Override
	protected void closeImpl() throws IOException
	{
//		try {
//			nioSocketCommChannelHandler.close().sync();
//		} catch( InterruptedException ex ) {
//			throw new IOException( ex );
//		}
	}
	
	public SessionContext sessionContext() {
		return context;
	}
	
	public void sessionContext(SessionContext ctx) {
		this.context = ctx;
	}

}
