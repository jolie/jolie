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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import java.net.InetSocketAddress;

/**
 *
 * @author stefanopiozingaro
 */
public class EchoServer {

    private final int port;

    public EchoServer(int port) {
        this.port = port;
    }

    public static void main(String[] args) throws Exception {
        int port = 5004;
        new EchoServer(port).start();
    }

    private void start() throws Exception {
        EchoServerHandler serverHandler = new EchoServerHandler();
        EventLoopGroup group = new NioEventLoopGroup();
            
        try {
            
            ServerBootstrap b = new ServerBootstrap();
            b.group(group);
            
            /* 
            Set the type of channel to NioServerSocket 
            */
            b.channel(NioServerSocketChannel.class);
            
            /* 
            Set the local address of the server to the port yet defined 
            */
            InetSocketAddress isa = new InetSocketAddress(port);
            b.localAddress(isa);

            /*
            Set the channel handler to our EchoServerHandler by istanciating 
            a new Channel Initializer of type SocketChannel (?)
            */ 
            ChannelHandler childHandler = 
                    new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel c) throws Exception {
                    c.pipeline().addLast(serverHandler);
                }

            };
            b.childHandler(childHandler);
            
            /*
            In one case we close the channel with future channel but first 
            we bind the server asynchronously
            */
            ChannelFuture f = b.bind().sync();
            f.channel().closeFuture().sync();
        } finally {
            /*
            In the other case we close the event loop group and release 
            resources
            */
            group.shutdownGracefully().sync();
        }
    }
}
