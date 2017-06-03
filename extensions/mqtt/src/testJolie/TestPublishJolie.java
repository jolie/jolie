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
package testJolie;

import io.moquette.Client;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.mqtt.MqttMessage;
import jolie.net.MqttProtocol;

/**
 *
 * @author stefanopiozingaro
 */
public class TestPublishJolie {
    
    Channel channel;

    public TestPublishJolie(MqttProtocol mp) {
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {

            Bootstrap b = new Bootstrap();
            b.group(workerGroup);
            b.channel(NioSocketChannel.class);
            b.option(ChannelOption.SO_KEEPALIVE, true); // try to keepalive ?
            b.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    mp.setupPipeline(ch.pipeline());
                }
            });

            /*
            questo dobiamo farlo indipendentemente se siamo in una inputPort 
            oppure in una outputPort, non dobbiamo cioè rimanere solo in ascolto
            ma dobbiamo comunque creare un canale con un endpoint
            anche per la inputPort ( la modifica credo vada fatta in CommCore)
            */ 
            channel = b.connect("test.mosquitto.org", 1883).sync().channel();

        } catch (InterruptedException ex) {
            ex.printStackTrace();
        } finally {
            workerGroup.shutdownGracefully();
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        MqttProtocol mp = new MqttProtocol(null);
        TestPublishJolie testJolieMqttProtocol = 
                new TestPublishJolie(mp);
        // cominciamo a far passare roba dentro al tubo
        
    }

}
