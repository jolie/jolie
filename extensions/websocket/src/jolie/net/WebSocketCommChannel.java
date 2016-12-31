/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jolie.net;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.pool.ChannelPool;
import java.net.URI;
import java.util.concurrent.ExecutionException;
import static jolie.net.NioSocketCommChannel.clearChannelPipeline;
import jolie.net.protocols.AsyncCommProtocol;

/**
 *
 * @author martin
 */
public class WebSocketCommChannel extends NioSocketCommChannel
{
	public WebSocketCommChannel( URI location, AsyncCommProtocol protocol ) {
		this( location, protocol, null );
	}
	
	public WebSocketCommChannel( URI location, AsyncCommProtocol protocol, ChannelPool pool )
	{
		super( location, protocol, pool );
	}
	
	public static WebSocketCommChannel CreateChannelFromPool( ChannelPool pool, URI location, AsyncCommProtocol protocol, EventLoopGroup workerGroup ) throws InterruptedException, ExecutionException
	{
		Channel ch = pool.acquire().get();
		clearChannelPipeline(ch);
		WebSocketCommChannel channel = new WebSocketCommChannel( location, protocol, pool );
		channel.jolieCommChannelHandler.setChannel( ch );
		ChannelPipeline p = ch.pipeline();
		
		protocol.setupPipeline( p );
		p.addLast(channel.jolieCommChannelHandler );
		ch.attr( COMMCHANNEL ).set( channel );
		channel.channel = ch;
		return channel;
	}
	
}
