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
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;

/**
 * This Class extends { @link SimpleChannelInboundHandler }
 * in order to override the method {@link channelRead0 }
 *
 * @author stefanopiozingaro
 */
@ChannelHandler.Sharable
public class MqttProtocolInboundHandler
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
            case CONNACK: // connection response
                handleConnAck(ctx, (MqttConnAckMessage) msg);
                break;
            case SUBACK: // subscription response
                //handleSubAck((MqttSubAckMessage) msg);
                break;
            case PUBLISH: // in case of publish from the server
                //handlePublish(ctx, (MqttPublishMessage) msg);
                break;
            case UNSUBACK: // unsubscription response
                //handleUnsubAck((MqttUnsubAckMessage) msg);
                break;
            case PUBACK: // publication response
                break;
            case PUBREC:
                //handlePubRec(ctx, msg);
                break;
            case PUBREL:
                //handlePubRel(ctx, msg);
                break;
            case PUBCOMP:
                //handlePubComp(msg);
                break;
            default:
                throw new AssertionError(msgType.name());
        }
    }

    /**
     * Method for the handling of the connection of the client keeping in mind
     * that the channel must be active in order to complete the request
     *
     * @param ctx ChannelHandlerContext
     * @param mqttConnAckMessage MqttConnAckMessage
     */
    private void handleConnAck(ChannelHandlerContext ctx,
            MqttConnAckMessage mqttConnAckMessage) {

        MqttConnectReturnCode connectReturnCode
                = mqttConnAckMessage.variableHeader().connectReturnCode();

        switch (connectReturnCode) {
            case CONNECTION_ACCEPTED:
                ctx.channel().flush();
                break;
            case CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION:
            case CONNECTION_REFUSED_IDENTIFIER_REJECTED:
            case CONNECTION_REFUSED_SERVER_UNAVAILABLE:
            case CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD:
            case CONNECTION_REFUSED_NOT_AUTHORIZED:
                ctx.channel().close();
                break;
            default:
                throw new AssertionError(connectReturnCode.name());

        }
    }

}
