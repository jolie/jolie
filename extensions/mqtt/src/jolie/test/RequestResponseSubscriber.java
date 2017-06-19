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
package jolie.test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import io.netty.util.CharsetUtil;
import java.util.Collections;
import java.util.List;
import jolie.net.PublishHandler;

public class RequestResponseSubscriber {

    public RequestResponseSubscriber(List<MqttTopicSubscription> topics, String message) {
        new Subscriber(topics, new PublishHandler() {

            @Override
            public void handleMessage(String topic, ByteBuf payload) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        new Publisher(Unpooled.copiedBuffer(payload).toString(CharsetUtil.UTF_8), message);
                    }
                }).start();
            }
        });
    }

    public static void main(String[] args) {
        new RequestResponseSubscriber(
                Collections.singletonList(
                        new MqttTopicSubscription("jolie/temperature/request", MqttQoS.AT_LEAST_ONCE)
                ), "23.0");
    }
}
