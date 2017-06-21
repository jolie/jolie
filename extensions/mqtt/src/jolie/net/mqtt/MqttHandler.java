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
package jolie.net.mqtt;

import io.netty.buffer.Unpooled;
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
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttVersion;
import io.netty.handler.timeout.IdleStateHandler;
import jolie.net.MqttProtocol;

@ChannelHandler.Sharable
public class MqttHandler extends ChannelInboundHandlerAdapter {

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

    @Override
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

        ctx.channel().writeAndFlush(mcm);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {

        MqttMessageType mmt = ((MqttMessage) msg).fixedHeader().messageType();
        Channel channel = ctx.channel();

        switch (mmt) {
            case CONNACK:
                MqttConnectReturnCode mcrc
                        = ((MqttConnAckMessage) msg).variableHeader().connectReturnCode();

                switch (mcrc) {
                    case CONNECTION_ACCEPTED:
                        //if (isSubscriber) { // in case of subscriber start pinging
                            channel.pipeline().addBefore("Ping", "IdleState", new IdleStateHandler(1, 1, 1));
                        //}
                        mp.sendAndFlush(channel); // flush pending publish and pending subscriptions 
                        break;
                    case CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD:
                    case CONNECTION_REFUSED_IDENTIFIER_REJECTED:
                    case CONNECTION_REFUSED_NOT_AUTHORIZED:
                    case CONNECTION_REFUSED_SERVER_UNAVAILABLE:
                    case CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION:
                        System.out.println(mcrc.name());
                        break;
                    default:
                        System.out.println(mcrc.name());
                }
                break;
            case PUBLISH:
                if (isSubscriber) {
                    handlePublish(channel, (MqttPublishMessage) msg);
                }
                break;
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
            case SUBACK:
                break;
            case UNSUBSCRIBE:
                break;
            case UNSUBACK:
                break;
            case PINGREQ:
                break;
            case PINGRESP:
                break;
            case DISCONNECT:
                break;
            default:
                System.out.println(mmt.name());
        }
    }

    private void handlePublish(Channel channel, MqttPublishMessage mpm) {

        switch (mpm.fixedHeader().qosLevel()) {
            case AT_MOST_ONCE:
                handleIncomingPublish(mpm);
                break;
            case AT_LEAST_ONCE:
                handleIncomingPublish(mpm);

                if (mpm.variableHeader().messageId() != -1) {

                    MqttFixedHeader fixedHeader
                            = new MqttFixedHeader(MqttMessageType.PUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
                    MqttMessageIdVariableHeader variableHeader
                            = MqttMessageIdVariableHeader.from(mpm.variableHeader().messageId());
                    channel.writeAndFlush(new MqttPubAckMessage(fixedHeader, variableHeader));
                }
                break;
            case EXACTLY_ONCE:
                break;
            case FAILURE:
                System.out.println("Publish has Failed QoS, it should be retrasmitted!");
                break;
            default:
                System.out.println(mpm.fixedHeader().qosLevel().name());
        }
    }

    private void handleIncomingPublish(MqttPublishMessage mpm) {
        mp.getSubscriptions().get(mpm.variableHeader().topicName())
                .handleMessage(mpm.variableHeader().topicName(), Unpooled.copiedBuffer(mpm.payload()));
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        ctx.channel().closeFuture();    
        cause.printStackTrace();
    }
}
