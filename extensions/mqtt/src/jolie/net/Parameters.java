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

import io.netty.handler.codec.mqtt.MqttVersion;

/**
 *
 * @author stefanopiozingaro
 */
public class Parameters {

    static final String BROKER = "broker";
    static final String CONCURRENT = "concurrent";
    static final String ALIAS = "alias";
    static final String QOS = "QoS";
    static final String WILL_TOPIC = "willTopic";
    static final String WILL_MESSAGE = "willMessage";
    static final String USERNAME = "username";
    static final String PASSWORD = "password";
    static final String FORMAT = "format";
    static final String BOUNDARY = "$"; // MAPPING FOT REQUEST RESPONSE TOPIC
    static final MqttVersion MQTT_VERSION = MqttVersion.MQTT_3_1_1;

}
