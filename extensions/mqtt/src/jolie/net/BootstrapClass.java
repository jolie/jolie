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
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/*
outputPort Broker { 
    location: "socket://test.mosquitto.org:1883" 
    protocol: mqtt {
        .osc.publish.alias = "temp/random" 
    }
    operation: publish( "23.0" ) 
}
 */
/**
 * @author stefanopiozingaro
 */
class BootstrapClass {

    private final MqttProtocol mp;

    private final String locationHost;
    private final int locationPort;
    
    public BootstrapClass() {
        
        this.mp = new MqttProtocol(null);
        this.locationPort = 1883;
        this.locationHost = "test.mosquitto.org";

        mp.setMqttTopic("temp/random");
        mp.publish("23.0");

        doBootstrap(locationHost, locationPort);

    }

    void doBootstrap(String brokerHost, int brokerPort) {

        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup);
            b.channel(NioSocketChannel.class);
            b.remoteAddress(new InetSocketAddress(brokerHost, brokerPort));
            b.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {

                    ChannelPipeline pipeline = ch.pipeline();
                    pipeline.addLast("Logger", new LoggingHandler(LogLevel.INFO));
                    pipeline.addLast("MqttDecoder", new MqttDecoder());
                    pipeline.addLast("MqttEncoder", MqttEncoder.INSTANCE);
                    pipeline.addLast("IdleState", new IdleStateHandler(4, 5, 0, TimeUnit.SECONDS));
                    pipeline.addLast("MqttPublishSubscribe", new MqttPublishSubscribeHandler(mp));

                }
            });

            ChannelFuture future = b.connect().sync();
            future.addListener((ChannelFutureListener) new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture f) throws Exception {
                    if (f.isSuccess()) {
                        mp.setMqttChannel(f.channel());
                    }
                }
            });
            future.channel().closeFuture().sync();

        } catch (InterruptedException ie) {

            ie.printStackTrace();

        } finally {

            workerGroup.shutdownGracefully();

        }
    }

    public static void main(String[] args) {
        new BootstrapClass();
    }
}
