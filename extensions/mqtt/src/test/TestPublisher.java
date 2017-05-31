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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.Future;
import java.nio.charset.Charset;
import jolie.net.mqtt.MqttClient;
import jolie.net.mqtt.MqttConnectResult;

/**
 *
 * @author stefanopiozingaro
 */
public class TestPublisher {

    private static final String host = "iot.eclipse.org";
    private static final int port = 1883;
    private static final Charset utf8 = CharsetUtil.UTF_8;
    private static final String pubTopic = "jolie";
    private static final String toPublish = "Mqtt now works on Jolie!";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        /*
        Publish
        */
        System.out.println("Testing the Publisher for Mqtt in Jolie!");

        MqttClient client = new MqttClient();

        // connect to mqtt host sandboxes
        client.connect(host, port)
                .addListener((Future<? super MqttConnectResult> f) -> {
                    if (f.isSuccess()) {
                        System.out.println("Connection established!");
                    } else {
                        System.err.println("Connection attempt failed!");
                        f.cause().printStackTrace();
                    }
                });

        // publish on topic
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeBytes(toPublish.getBytes(utf8));
        
        client.publish(
                        pubTopic,
                        buffer,
                        MqttQoS.AT_LEAST_ONCE
                )
                .addListener((Future<? super Void> f) -> {
                    if (f.isSuccess()) {
                        System.out.println("Publish made, with payload: " 
                                + buffer);
                    } else {
                        System.err.println("Publish attempt failed");
                        f.cause().printStackTrace();
                    }
                });
    }
}
