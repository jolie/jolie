/*
 * Copyright (C) 2017 stefanopiozingaro
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.util.CharsetUtil;

/**
 * Business Logic for the server
 * @author stefanopiozingaro
 */
@Sharable
public class EchoServerHandler extends ChannelInboundHandlerAdapter {
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        // effettuo il casting di msg che è un oggetto generico
        ByteBuf buffer = (ByteBuf) msg;
        System.out.println("Il server ha ricevuto: " 
                + buffer.toString(CharsetUtil.UTF_8));
    }

    /**
     *
     * @param ctx
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ChannelFuture cf;
        cf = ctx.writeAndFlush(Unpooled.EMPTY_BUFFER);
        cf.addListener(ChannelFutureListener.CLOSE);
    }

    /**
     *
     * @param ctx
     * @param cause
     */
    @Override
    @SuppressWarnings("CallToPrintStackTrace")
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

}
