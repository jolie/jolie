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

import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.util.CharsetUtil;
import java.util.List;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author stefanopiozingaro
 */
class TestRequestResponse implements Runnable {

    private MqttProtocol mp;
    private final String reqTopic = "temp/random";
    private final String resTopic = "temp/response";
    private final String resMsg = "23.0";

    @Override
    public void run() {

        requestResponse(reqTopic, resTopic);

    }

    void receive(String topic) {

        mp = new MqttProtocol(Boolean.TRUE, null);
        MqttSubscribeMessage sub
                = mp.buildSubscription(topic);

        if (mp.isSubscribeReady()) {
            mp.getConnectedChannel().writeAndFlush(sub);
        } else {
            mp.getPendingSubscriptions().add(sub);
        }
        new ClientBootstrap(mp);

        for (ListIterator<MqttPublishMessage> i = mp.getPublishesReceived().listIterator(); i.hasNext();) {
            consumeMessage(i.next());
        }
    }

    void send(String topic, String message) {

        mp = new MqttProtocol(Boolean.FALSE, null);
        MqttPublishMessage pub
                = mp.buildPublication(topic, message);

        if (mp.isPublishReady()) {
            mp.getConnectedChannel().writeAndFlush(pub);
        } else {
            mp.getPendingPublishes().add(pub);
        }
        new ClientBootstrap(mp);
    }

    private void requestResponse(String reqTopic, String resTopic) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                receive(resTopic);
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                send(reqTopic, resTopic);
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                receive(reqTopic);
            }
        }).start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                send(resTopic, resMsg);
            }
        }).start();
    }

    private void consumeMessage(MqttPublishMessage msg) {
        System.out.println(msg.content().toString(CharsetUtil.UTF_8));
        try {
            Thread.currentThread().join(5000);
        } catch (InterruptedException ex) {
            Logger.getLogger(TestRequestResponse.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
