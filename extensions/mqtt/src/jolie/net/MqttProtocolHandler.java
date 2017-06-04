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

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttSubAckMessage;
import io.netty.handler.codec.mqtt.MqttUnsubAckMessage;

/**
 * This Class extends { @link ChannelInboundHanlderAdapter }
 * in order to override the 2 basic methods of Inbound Handlers: null {@link channelRead }
 * {@link channelInactive }
 *
 * @author stefanopiozingaro
 */
@ChannelHandler.Sharable
public class MqttProtocolHandler extends ChannelInboundHandlerAdapter {

    /**
     * The channelRead that provides parameters to read from the channel inbound
     * buffer the MqttMessage arriving, calls messageReceived from the { @link MqttProtocolClient
     * } implementation
     *
     * @param ctx is the Context of the handler
     * @param message the Object (MqttMessage in this case) arriving
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object message) {
        MqttMessage msg = (MqttMessage) message;
        switch (msg.fixedHeader().messageType()) {
            case CONNACK:
                handleConack(ctx.channel(), (MqttConnAckMessage) msg);
                break;
            case SUBACK:
                handleSubAck((MqttSubAckMessage) msg);
                break;
            case PUBLISH:
                handlePublish(ctx.channel(), (MqttPublishMessage) msg);
                break;
            case UNSUBACK:
                handleUnsuback((MqttUnsubAckMessage) msg);
                break;
            case PUBACK:
                handlePuback((MqttPubAckMessage) msg);
                break;
            case PUBREC:
                handlePubrec(ctx.channel(), msg);
                break;
            case PUBREL:
                handlePubrel(ctx.channel(), msg);
                break;
            case PUBCOMP:
                handlePubcomp(msg);
                break;
            default:
                throw new AssertionError(msg.fixedHeader().messageType().name());
        }
    }

    /**
     * The Channel Inactive capture the inactivness of the connection with the
     * broker TODO implement a way to keep alive the connection
     *
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.close();
    }

    /*
    GESTISCI OGNI CASO
    */
    private void handleConack(Channel channel, MqttConnAckMessage mqttConnAckMessage) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private void handleSubAck(MqttSubAckMessage mqttSubAckMessage) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private void handleUnsuback(MqttUnsubAckMessage mqttUnsubAckMessage) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private void handlePuback(MqttPubAckMessage mqttPubAckMessage) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private void handlePubrec(Channel channel, MqttMessage msg) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private void handlePubrel(Channel channel, MqttMessage msg) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private void handlePubcomp(MqttMessage msg) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
