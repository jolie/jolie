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
package jolie.net;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.net.InetSocketAddress;

/**
 *
 * @author stefanopiozingaro
 */
public class BootstrapChannel {

    private final String brokerHost;
    private final int brokerPort;
    private final MqttProtocol mqttProtocol;

    private Channel channel;

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public BootstrapChannel(String brokerHost, int brokerPort) {

        this.brokerHost = brokerHost;
        this.brokerPort = brokerPort;
        this.mqttProtocol = new MqttProtocol(null);
        doBootstrap(this.mqttProtocol);

    }

    /**
     *
     */
    private void doBootstrap(MqttProtocol mp) {

        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup);
            b.channel(NioSocketChannel.class);
            // optionally set the local Address 
            b.remoteAddress(new InetSocketAddress(brokerHost, brokerPort));
            b.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {

                    ChannelPipeline pipeline = ch.pipeline();
                    mp.setupPipeline(pipeline);
                    //pipeline.addLast("Generic", new GenericHandler());

                }
            });

            ChannelFuture future = b.connect().sync();
            future.addListener((ChannelFutureListener) new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture f) throws Exception {
                    if (f.isSuccess()) {
                        setChannel(f.channel());
                    }
                }
            });
            future.channel().closeFuture().sync();

        } catch (InterruptedException ioe) {

            ioe.printStackTrace();

        } finally {

            workerGroup.shutdownGracefully();

        }
    }

    private static class GenericHandler extends ChannelOutboundHandlerAdapter {

        public GenericHandler() {
        }
    }

}
