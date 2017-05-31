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
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.Future;
import java.nio.charset.Charset;
import jolie.net.mqtt.MqttClient;
import jolie.net.mqtt.MqttConnectResult;
import jolie.net.mqtt.MqttHandler;

/**
 *
 * @author stefanopiozingaro
 */
public class TestSubscriber {

    private static final String host = "iot.eclipse.org";
    private static final int port = 1883;
    private static final Charset utf8 = CharsetUtil.UTF_8;
    private static final String subTopic = "jolie";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        /*
        Subscription
         */
        System.out.println("Testing the Subscriber for Mqtt in Jolie!");

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

        // subscribe on topic
        MqttHandler subHandler = (String topic, ByteBuf payload) -> {
            System.out.println("They publish on the topic " + topic);
            System.out.println("this message " + payload.toString(utf8));
        };
        client.on(subTopic, subHandler)
                .addListener((Future<? super Void> f) -> {
                    if (f.isSuccess()) {
                        System.out.println("Subscription made!");
                    } else {
                        System.err.println("Subscription attempt failed!");
                        f.cause().printStackTrace();
                    }
                });
    }
}
