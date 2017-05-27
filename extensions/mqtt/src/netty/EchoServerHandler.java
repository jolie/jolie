package netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.util.CharsetUtil;

import java.text.MessageFormat;

/**
 * Released Under Creative Common License
 * Created by stefanopiozingaro on 27/05/17.
 */
@ChannelHandler.Sharable
public class EchoServerHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        // effettuo il casting di msg che è un oggetto generico
        ByteBuf buffer = (ByteBuf) msg;
        System.out.println(MessageFormat.format("Il buffer di byte che è arrivato al server è: {0}"
                , buffer.toString(CharsetUtil.UTF_8)));
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ChannelFuture cf;
        cf = ctx.writeAndFlush(Unpooled.EMPTY_BUFFER);
        cf.addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

}
