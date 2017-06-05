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

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectPayload;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttConnectVariableHeader;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttVersion;
import java.util.Random;

/**
 * ConnectionHandler handler inbound che con la channelActive gestisce la
 * connect del client channelRead gestisce la connacl
 *
 * @author stefanopiozingaro
 */
public class MqttOnActiveHandler extends SimpleChannelInboundHandler<MqttMessage> {

    private final String clientId;
    private final MqttVersion version;
    private final int keepAliveTimeSeconds;
    private final String willTopic;
    private final String willMessage;
    private final String userName;
    private final String password;

    public MqttOnActiveHandler() {

        this.clientId = "jolie/" + GenerateRandomId();
        this.version = MqttVersion.MQTT_3_1_1;
        this.keepAliveTimeSeconds = 2;
        this.willTopic = "";
        this.willMessage = "";
        this.userName = "";
        this.password = "";

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
        String[] options
                = "0123456789".split("");
        for (int i = 0; i < 8; i++) {
            id += options[random.nextInt(options.length)];
        }
        return id;
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
                        this.version.protocolName(),
                        this.version.protocolLevel(),
                        !"".equals(this.userName),
                        !"".equals(this.password),
                        isWillRetain,
                        willQoS.value(),
                        !"".equals(this.willMessage),
                        isCleanSession,
                        this.keepAliveTimeSeconds
                );

        MqttConnectPayload payload = new MqttConnectPayload(
                this.clientId,
                this.willTopic,
                this.willMessage,
                this.userName,
                this.password
        );

        MqttConnectMessage mcm = new MqttConnectMessage(
                mqttFixedHeader,
                variableHeader,
                payload
        );

        ctx.writeAndFlush(mcm); 
    }

    /**
     * In case of acceptance of the connection Replace ConnectionHandler da
     * MqttOnActiveHandler a MqttPingHandler
     *
     * @param ctx ChannelHandlerContext
     * @param msg MqttMessage
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MqttMessage msg)
            throws Exception {

        MqttMessageType msgType = msg.fixedHeader().messageType();

        if (msgType.equals(MqttMessageType.CONNACK)) {

            MqttConnAckMessage mmsg = (MqttConnAckMessage) msg;
            MqttConnectReturnCode connectReturnCode
                    = mmsg.variableHeader().connectReturnCode();

            if (connectReturnCode.equals(MqttConnectReturnCode.CONNECTION_ACCEPTED)) {

                ctx.pipeline().remove(this);
                ctx.pipeline().addLast("MqttPing", new MqttPingHandler(10));
                ctx.pipeline().addLast("MqttPublishSubscribe", new MqttPublishSubscribeHandler());

            }
        }
    }
}
