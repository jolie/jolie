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

import io.netty.handler.codec.mqtt.MqttPublishMessage;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author stefanopiozingaro
 */
class TestDevice implements Runnable {

    private MqttProtocol mp;

    @Override
    public void run() {

        try {
            Thread.sleep(5000);
            send("temp/random", "23.0");
        } catch (InterruptedException ex) {
            Logger.getLogger(TestDevice.class.getName()).log(Level.SEVERE, null, ex);
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
}
