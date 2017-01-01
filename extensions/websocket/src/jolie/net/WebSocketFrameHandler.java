/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jolie.net;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import java.util.List;

/**
 *
 * @author martin
 */
class WebSocketFrameHandler extends MessageToMessageCodec<WebSocketFrame, ByteBuf> {

	@Override
	protected void encode( ChannelHandlerContext ctx, ByteBuf msg, List<Object> out ) throws Exception
	{
		out.add( new TextWebSocketFrame( msg.retain() ) );
	}

	@Override
	protected void decode( ChannelHandlerContext ctx, WebSocketFrame msg, List<Object> out ) throws Exception
	{
		out.add( msg.content().retain() );
	}
}