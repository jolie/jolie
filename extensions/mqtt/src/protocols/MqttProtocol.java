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
package protocols;

/**
 *
 * @author stefanopiozingaro
 */
public class MqttProtocol {
    /**
     *
     * TODO Write a runnable example using netty library iplementing an MQTT
     * sender using channel and pipelines: 
     * 1.Create a socket channel 
     * 2.Get the pipeline from the created channel 
     * 3.Add the following steps to the pipeline using addLast method, for 
     *  each method simply print the current status:
     * 3.1 Handle the socket creation event (e.g. new SocketHandler) 
     * 3.2 Handle the Netty message to Mqtt message codec event (extends
     *      MessageToMessageCodec<NettyMsg,MqttMsg>) and encode or decode 
     *      the event, finally out.add(message) 
     * 3.3Handle the outbound of a message event (override write()) 
     * 3.4Handle the event that close the channel 
     */

}
