package jolie.net;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.pool.AbstractChannelPoolMap;
import io.netty.channel.pool.ChannelPool;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.pool.ChannelPoolMap;
import io.netty.channel.pool.SimpleChannelPool;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import jolie.StatefulContext;
import jolie.net.ext.CommChannelFactory;
import jolie.net.ports.OutputPort;
import jolie.net.protocols.AsyncCommProtocol;
import jolie.net.protocols.CommProtocol;
import jolie.util.Pair;

/**
 *
 * @author martin
 */
public class NioSocketCommChannelFactory extends CommChannelFactory
{

	private static HashMap<String, Pair> poolKeys = new HashMap<>();
	
	EventLoopGroup workerGroup;
	ChannelPoolMap<String , ChannelPool> pools = new AbstractChannelPoolMap<String, ChannelPool>()
	{
		
		@Override
		protected ChannelPool newPool( String key )
		{
			URI location;
			try {
				location = new URI( key );
			} catch( URISyntaxException ex ) {
				throw new IllegalArgumentException( "Key is not a valid URI" );
			}
			InetSocketAddress remoteAddress = new InetSocketAddress( location.getHost(), location.getPort() );
			Bootstrap b = new Bootstrap();
			b.group( workerGroup )
				.channel( NioSocketChannel.class )
				.option( ChannelOption.SO_LINGER, NioSocketCommChannel.SO_LINGER )
				.remoteAddress( remoteAddress )
				.handler(new ChannelInitializer()
				{
					@Override
					protected void initChannel( Channel ch ) throws Exception
					{
						
					}
				} );
			
			return new SimpleChannelPool(b, new ChannelPoolHandler()
			{
				int count = 0;
				
				@Override
				public void channelReleased( Channel ch ) throws Exception
				{
//					count++;
//					System.out.println( "Available channels: " + count + " (release)" );
				}

				@Override
				public void channelAcquired( Channel ch ) throws Exception
				{
//					if(count > 0)
//						count--;
//					System.out.println( "Available channels: " + count + " (aquire)");
					
				}

				@Override
				public void channelCreated( Channel ch ) throws Exception
				{
				}
			} );
		}
		
	};

	public NioSocketCommChannelFactory( CommCore commCore, EventLoopGroup workerGroup )
	{
		super( commCore );
		this.workerGroup = workerGroup;
	}

	@Override
	public CommChannel createChannel( URI location, OutputPort port, StatefulContext ctx )
		throws IOException
	{
		CommProtocol protocol;
		try {
			protocol = port.getProtocol( ctx );
		} catch( URISyntaxException e ) {
			throw new IOException( e );
		}

		if ( !(protocol instanceof AsyncCommProtocol) ) {
			throw new UnsupportedCommProtocolException( "Use an async protocol" );
		}

		String key = location.toString();
		NioSocketCommChannel channel;
		try {
			channel = NioSocketCommChannel.CreateChannelFromPool(pools.get( key ), location, (AsyncCommProtocol) protocol, workerGroup );
		} catch( InterruptedException | ExecutionException ex ) {
			throw new IOException("Unable to aquire channel for " + location);
		}
		
//		try {
//			ChannelFu
//			f.sync();
//			if ( !f.isSuccess() ) {
//				throw (IOException) f.cause();
//			}
//		} catch( InterruptedException e ) {
//			throw new IOException( e );
//		} catch (ChannelException e) {
//			throw new IOException( e );
//		}
		return channel;
	}

}
