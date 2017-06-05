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

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;

/**
 * This Class extends { @link SimpleChannelInboundHandler }
 * in order to override the method {@link channelRead0 }
 *
 * @author stefanopiozingaro
 */
@ChannelHandler.Sharable
public class MqttPublishSubscribeHandler
        extends SimpleChannelInboundHandler<MqttMessage> {

    /**
     * For each one of the { @link MqttMessage } type the method send the
     * request to the inbound handler method, that is, all the acknowledgement
     * from the broker
     *
     * @param ctx ChannelHandlerContext
     * @param msg MqttMessage
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MqttMessage msg)
            throws Exception {

        MqttMessageType msgType = msg.fixedHeader().messageType();
        switch (msgType) {
            case PUBACK:
                break;
            case PUBREC:
                break;
            case PUBREL:
                break;
            case PUBCOMP:
                break;
            case PUBLISH:
                break;
            case SUBACK:
                break;
            case UNSUBACK:
                break;
            default:
                throw new AssertionError(msgType.name());
        }
    }

}
