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

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.net.InetSocketAddress;

/**
 * The Echo client will 1 Connect to the server 2 Send one or more messages 3
 * For each message, wait for and receive the same message back from the server
 * 4 Close the connection
 *
 * @author stefanopiozingaro
 */
public class EchoClient {

    private final int port;
    private final String host;

    public EchoClient(int port, String host) {
        this.port = port;
        this.host = host;
    }

    public static void main(String[] args) throws Exception {
        int port = 9999; // 1883
        String host = "0.0.0.0"; // iot.eclipse.org
        EchoClient ec = new EchoClient(port, host);
        ec.start();
    }

    private void start() throws Exception {
        EventLoopGroup group = new NioEventLoopGroup();

        try {
            /*
            Creates Bootstrap
             */
            Bootstrap b = new Bootstrap();
            b.group(group);

            /*
            Channel type is the one for NIO transport.
             */
            b.channel(NioSocketChannel.class);

            /*
            Sets the server’s InetSocket- Address
             */
            InetSocketAddress isa = new InetSocketAddress(host, port);
            b.remoteAddress(isa);

            /*
            Adds an EchoClient- Handler to the pipeline 
            when a Channel is created
             */
            ChannelHandler handler
                    = new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel c) throws Exception {
                    c.pipeline().addLast(new EchoClientHandler());
                }
            };
            b.handler(handler);

            ChannelFuture f = b.connect().sync();
            f.channel().closeFuture().sync();

        } finally {
            group.shutdownGracefully().sync();
        }
    }

}
