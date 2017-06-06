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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.util.CharsetUtil;
import java.util.Random;

/**
 *
 * @author stefanopiozingaro
 */
class Main {

    public Main() {
        String topic = "temp/random";
        String message = "23.0";
        String brokerHost = "test.mosquitto.org";
        int brokerPort = 1883;

        MqttPublishMessage mpm = buildPublishMqttMessage(topic, message);
        BootstrapChannel bc = new BootstrapChannel(brokerHost, brokerPort);
        if (bc.getChannel().isActive() && bc.getChannel().isWritable()) {
            bc.getChannel().writeAndFlush(mpm);
        }
    }

    /**
     *
     * @return MqttPublishMessage
     */
    MqttPublishMessage buildPublishMqttMessage(String topic, String message) {

        boolean isDup = Boolean.FALSE;
        MqttQoS publishQoS = MqttQoS.AT_MOST_ONCE;
        boolean isConnectRetain = Boolean.FALSE;
        int messageId = Integer.parseInt(GenerateRandomId());

        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(
                MqttMessageType.PUBLISH,
                isDup,
                publishQoS,
                isConnectRetain,
                0
        );

        MqttPublishVariableHeader variableHeader
                = new MqttPublishVariableHeader(
                        topic,
                        messageId
                );

        ByteBuf payload = parseObject(message);

        MqttPublishMessage mpm = new MqttPublishMessage(
                mqttFixedHeader,
                variableHeader,
                payload
        );

        return mpm;
    }

    /**
     * Generate random client id for default Method stolen and little modified @
     * github from jk5
     *
     * @return the random generated client id
     */
    private String GenerateRandomId() {
        Random random = new Random();
        String id = "";
        String[] options = "0123456789".split("");
        for (int i = 0; i < 8; i++) {
            id += options[random.nextInt(options.length)];
        }
        return id;
    }

    /**
     * TODO parse object according to object type passed
     *
     * @param message
     * @return ByteBuf
     */
    private ByteBuf parseObject(String message) {
        return Unpooled.buffer().writeBytes(message.getBytes(CharsetUtil.UTF_8));
    }

    public static void main(String[] args) {
        new Main();
    }

}
