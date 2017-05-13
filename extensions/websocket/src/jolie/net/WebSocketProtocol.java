/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jolie.net;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import java.nio.charset.Charset;
import java.util.List;
import jolie.StatefulContext;
import jolie.net.protocols.AsyncCommProtocol;
import jolie.net.protocols.CommProtocol;
import jolie.runtime.VariablePath;
/**
 *
 * @author martin
 */
public class WebSocketProtocol extends AsyncCommProtocol
{
	private final AsyncCommProtocol wrappedProtocol;
	
	public WebSocketProtocol( 
		VariablePath configurationPath,
		CommProtocol wrappedProtocol)
	{
		super( configurationPath );
		this.wrappedProtocol = (AsyncCommProtocol)wrappedProtocol;
	}

	public class WebSocketContentCodec extends MessageToMessageCodec<WebSocketFrame, EncodedJsonRpcContent> {
		
		@Override
		protected void encode( ChannelHandlerContext ctx, EncodedJsonRpcContent content, List<Object> out ) throws Exception
		{
			out.add( new TextWebSocketFrame( content.content().retain() ) );
		}

		@Override
		protected void decode( ChannelHandlerContext ctx, WebSocketFrame frame, List<Object> out ) throws Exception
		{
			out.add( new EncodedJsonRpcContent( frame.content().retain(), Charset.forName( "utf-8" ), null ) );
		}
	
	}
	
	@Override
	public void setupPipeline( ChannelPipeline pipeline )
	{
		pipeline.addLast(new WebSocketContentCodec() );
		wrappedProtocol.setupWrappablePipeline( pipeline );
	}

	@Override
	public String name()
	{
		return wrappedProtocol.name();
	}

	@Override
	public boolean isThreadSafe()
	{
		return wrappedProtocol.isThreadSafe();
	}

	@Override
	public void initialize( StatefulContext ctx )
	{
		wrappedProtocol.initialize( ctx );
	}
	
}
