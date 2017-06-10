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
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectPayload;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttConnectVariableHeader;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttVersion;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;

/**
 * This Class extends { @link SimpleChannelInboundHandler }
 * in order to override the method {@link channelRead0 }
 *
 * @author stefanopiozingaro
 */
@ChannelHandler.Sharable
public class MqttHandler
        extends ChannelInboundHandlerAdapter {

    private final MqttProtocol mp;
    private final String clientId;
    private final boolean isSubscriber;

    public MqttHandler(MqttProtocol mp) {

        this.mp = mp;
        this.clientId = "jolie/" + (int) (Math.random() * 65536);
        this.isSubscriber = mp.isInInputPort();

        mp.setVersion(MqttVersion.MQTT_3_1_1);
        mp.setWillTopic("");
        mp.setWillMessage("");
        mp.setUserName("");
        mp.setPassword("");
    }

    /**
     * Calls {@link ChannelHandlerContext#fireChannelActive()} to forward to the
     * next {@link ChannelInboundHandler} in the {@link ChannelPipeline}.
     *
     * Sub-classes may override this method to change behavior.
     *
     * @param ctx
     * @throws java.lang.Exception
     */
    @Override
    @SuppressWarnings("Convert2Lambda")
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

        boolean isDup = Boolean.FALSE;
        MqttQoS connectQoS = MqttQoS.AT_MOST_ONCE;
        boolean isConnectRetain = Boolean.FALSE;
        boolean isCleanSession = Boolean.FALSE;
        boolean isWillRetain = Boolean.FALSE;
        MqttQoS willQoS = MqttQoS.AT_MOST_ONCE;

        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(
                MqttMessageType.CONNECT,
                isDup,
                connectQoS,
                isConnectRetain,
                0
        );

        MqttConnectVariableHeader variableHeader
                = new MqttConnectVariableHeader(
                        mp.getVersion().protocolName(),
                        mp.getVersion().protocolLevel(),
                        !"".equals(mp.getUserName()),
                        !"".equals(mp.getPassword()),
                        isWillRetain,
                        willQoS.value(),
                        !"".equals(mp.getWillMessage()),
                        isCleanSession,
                        mp.getKeepAliveConnectTimeSeconds()
                );

        MqttConnectPayload payload = new MqttConnectPayload(
                this.clientId,
                mp.getWillTopic(),
                mp.getWillMessage(),
                mp.getUserName(),
                mp.getPassword()
        );

        MqttConnectMessage mcm = new MqttConnectMessage(
                mqttFixedHeader,
                variableHeader,
                payload
        );

        if (ctx.channel().isWritable()) {
            ctx.channel().writeAndFlush(mcm);
        }
    }

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
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {

        MqttMessageType mmt = ((MqttMessage) msg).fixedHeader().messageType();
        Channel channel = ctx.channel();
        //System.out.println(mmt);
        switch (mmt) {
            case CONNACK:
                MqttConnectReturnCode mcrc
                        = ((MqttConnAckMessage) msg).variableHeader().connectReturnCode();

                //System.out.println(mcrc);
                switch (mcrc) {
                    case CONNECTION_ACCEPTED:
                        if (isSubscriber) {
                            channel.pipeline().addBefore("Ping", "IdleState", new IdleStateHandler(1, 1, 1));
                        }
                        /*
                        Connection alive, tell the thread that we're ready
                         */
                        mp.sendAndFlush(channel);
                        break;
                    case CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD:
                    case CONNECTION_REFUSED_IDENTIFIER_REJECTED:
                    case CONNECTION_REFUSED_NOT_AUTHORIZED:
                    case CONNECTION_REFUSED_SERVER_UNAVAILABLE:
                    case CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION:
                        channel.closeFuture();
                        break;
                }
                break;
            case PINGRESP:
                //System.out.println("Ping response received!");
                break;
            case PUBLISH:
                MqttPublishMessage mpm = (MqttPublishMessage) msg;
                mp.notifyPublication(mpm);
            case SUBACK:
            //System.out.println("Mi sono iscritto!");
            case CONNECT:
                break;
            case PUBACK:
                break;
            case PUBREC:
                break;
            case PUBREL:
                break;
            case PUBCOMP:
                break;
            case SUBSCRIBE:
                break;
            case UNSUBSCRIBE:
                break;
            case UNSUBACK:
                break;
            case PINGREQ:
                break;
            case DISCONNECT:
                break;
            default:
                ctx.fireChannelRead(ReferenceCountUtil.retain(msg));
        }
    }

}
