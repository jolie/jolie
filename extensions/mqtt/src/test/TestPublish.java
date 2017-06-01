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

/* TODO 
 * 1. Fare la sottoscrizione (subscribe a topic temperature sulla sandbox)
 * 2. Fare la pubblicazione (QoS2) 
 * 3. Fare la subscribe su topic + publish su topic + stampa subscriber 
 * 4. Serializzare un oggetto in ByteBuf (cerca su Mqtt)
 */
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.concurrent.Future;
import jolie.net.mqtt.MqttClient;
import jolie.net.mqtt.MqttConnectResult;

/**
 *
 * @author stefanopiozingaro
 */
public class TestPublish {

    // from http://test.mosquitto.org
    private static final String host = "test.mosquitto.org";
    // from http://test.mosquitto.org/gauge/ 
    private static final String topic = "temp/random";
    private static final String msg = "23.0";

    public TestPublish() {
        MqttClient client = MqttClient.create();

        /*
        CONNECT
         */
        Future<MqttConnectResult> connect = client.connect(host);

        /*
        PUBLISH ON A KNOWN TOPIC
         */
        connect.addListener((Future<? super MqttConnectResult> f) -> {

            if (f.isSuccess()) {
                ByteBuf payload = Unpooled.copiedBuffer(msg.getBytes());
                client.publish(topic, payload);
            } else {
                System.out.println("Publishing attempt failed!");
            }
        });
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        new TestPublish();
    }
}
