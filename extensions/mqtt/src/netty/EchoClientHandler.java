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
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;

/**
 * Business Logic for the client
 *
 * @author stefanopiozingaro
 */
@Sharable
public class EchoClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

    /**
     * When notified that the channel is active, sends a message
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.writeAndFlush(Unpooled.copiedBuffer("Hello!", CharsetUtil.UTF_8));
    }

    /**
     * Logs a dump of the received message
     * @param chc
     * @param i
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext chc, ByteBuf i) 
            throws Exception {
        System.out.println("Il client dice: " + i.toString(CharsetUtil.UTF_8));
    }

    /**
     * On exception, logs the error and closes channel
     * @param ctx
     * @param cause
     * @throws Exception
     */
    @Override
    @SuppressWarnings("CallToPrintStackTrace")
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) 
            throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
