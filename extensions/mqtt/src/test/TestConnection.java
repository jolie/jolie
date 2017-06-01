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
package test;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.CharsetUtil;
import java.nio.charset.Charset;
import jolie.MqttProtocol;
import jolie.runtime.VariablePath;

/**
 *
 * @author stefanopiozingaro
 */
public class TestConnection {

    private static final String host = "iot.eclipse.org";
    private static final int port = 1883;
    
    private static final Class<? extends Channel> channelClass = NioSocketChannel.class;
    private static final EventLoopGroup eventLoop = new NioEventLoopGroup();

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        VariablePath configurationPath = null;

        MqttProtocol mqtt = new MqttProtocol(configurationPath);

        Bootstrap b = new Bootstrap();

        b.group(eventLoop);
        b.channel(channelClass);
        b.remoteAddress(host, port);
        b.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                mqtt.setupPipeline(ch.pipeline());
            }
        });

        ChannelFuture future = b.connect();
        future.addListener((ChannelFutureListener) (ChannelFuture f) -> {
            if (f.isSuccess()) {
                System.out.println("Connection established!");
            } else {
                System.err.println("Connection attempt failed!");
                f.cause().printStackTrace();
                f.channel().closeFuture();
                System.exit(0);
            }
        });

    }
}
