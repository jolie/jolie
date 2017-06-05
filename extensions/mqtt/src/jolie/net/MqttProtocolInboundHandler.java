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
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnAckVariableHeader;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttMessage;

/**
 * This Class extends { @link ChannelInboundHanlderAdapter }
 * in order to override the 2 basic methods of Inbound Handlers: null {@link channelRead }
 * {@link channelInactive }
 *
 * @author stefanopiozingaro
 */
@ChannelHandler.Sharable
public class MqttProtocolInboundHandler extends ChannelInboundHandlerAdapter {

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
        readInboundMessage(ctx, msg);
    }

    /**
     * Calls {@link ChannelHandlerContext#fireChannelInactive()} to forward to
     * the next {@link ChannelInboundHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     *
     * @param ctx
     * @throws java.lang.Exception
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.close();
    }

    /**
     * Calls {@link ChannelHandlerContext#fireExceptionCaught(Throwable)} to
     * forward to the next {@link ChannelHandler} in the
     * {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     *
     * @param ctx
     * @param cause
     * @throws java.lang.Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    /**
     * For each one of the { @link MqttMessage } type the method send the
     * request to the inbound handler method, that is, all the acknowledgement
     * from the broker
     *
     * @param ctx ChannelHandlerContext
     * @param msg MqttMessage
     */
    private void readInboundMessage(ChannelHandlerContext ctx, MqttMessage msg) {
        switch (msg.fixedHeader().messageType()) {
            case CONNACK: // connection response
                handleConnAck(ctx, (MqttConnAckMessage) msg);
                break;
            case SUBACK: // subscription response
                ctx.channel().flush();
                //handleSubAck((MqttSubAckMessage) msg);
                break;
            case PUBLISH: // in case of publish from the server
                ctx.channel().flush();
                //handlePublish(ctx, (MqttPublishMessage) msg);
                break;
            case UNSUBACK: // unsubscription response
                ctx.channel().flush();
                //handleUnsubAck((MqttUnsubAckMessage) msg);
                break;
            case PUBACK: // publication response
                ctx.channel().flush();
                break;
            case PUBREC:
                //handlePubRec(ctx, msg);
                ctx.channel().flush();
                break;
            case PUBREL:
                //handlePubRel(ctx, msg);
                ctx.channel().flush();
                break;
            case PUBCOMP:
                //handlePubComp(msg);
                ctx.channel().flush();
                break;
            default:
                throw new AssertionError(msg.fixedHeader().messageType().name());
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

        /* MqttFixedHeader mfh = mqttConnAckMessage.fixedHeader(); */
        MqttConnAckVariableHeader mcvh = mqttConnAckMessage.variableHeader();

        MqttConnectReturnCode connectReturnCode = mcvh.connectReturnCode();

        switch (connectReturnCode) {
            case CONNECTION_ACCEPTED:
                ctx.channel().flush();
                break;
            case CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION:
            case CONNECTION_REFUSED_IDENTIFIER_REJECTED:
            case CONNECTION_REFUSED_SERVER_UNAVAILABLE:
            case CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD:
            case CONNECTION_REFUSED_NOT_AUTHORIZED:
                handleConnectionRefused(ctx);
                break;
            default:
                throw new AssertionError(connectReturnCode.name());

        }
    }

    /**
     * Handle the server unavailable situation, wrong username or password and
     * non authorized user, that is, close everything
     *
     * @param ctx ChannelHandlerContext
     */
    private void handleConnectionRefused(ChannelHandlerContext ctx) {

        System.out.println("Error! Me closing ...");
        ctx.close();
    }

}
