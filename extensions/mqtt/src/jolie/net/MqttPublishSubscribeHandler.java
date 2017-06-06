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

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectPayload;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttConnectVariableHeader;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttVersion;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.ScheduledFuture;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * This Class extends { @link SimpleChannelInboundHandler }
 * in order to override the method {@link channelRead0 }
 *
 * @author stefanopiozingaro
 */
@ChannelHandler.Sharable
public class MqttPublishSubscribeHandler
        extends ChannelInboundHandlerAdapter {

    private ScheduledFuture<?> pingRespTimeout;

    private final String clientId;
    private final MqttVersion version;
    private final int keepAliveConnectTimeSeconds;
    private final int keepAlivePingTimeSeconds;
    private final String willTopic;
    private final String willMessage;
    private final String userName;
    private final String password;

    public MqttPublishSubscribeHandler(int keepAlivePingTimeSeconds) {

        this.clientId = "jolie/" + GenerateRandomId();
        this.version = MqttVersion.MQTT_3_1_1;
        this.keepAlivePingTimeSeconds = keepAlivePingTimeSeconds;
        this.keepAliveConnectTimeSeconds = 2;
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
                        this.keepAliveConnectTimeSeconds
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

        ctx.channel().writeAndFlush(mcm);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt)
            throws Exception {

        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            switch (event.state()) {
                case READER_IDLE:
                    break;
                case WRITER_IDLE:
                    MqttFixedHeader fixedHeader = new MqttFixedHeader(
                            MqttMessageType.PINGREQ,
                            false,
                            MqttQoS.AT_MOST_ONCE,
                            false,
                            0
                    );
                    ctx.channel().writeAndFlush(new MqttMessage(fixedHeader));

                    if (this.pingRespTimeout != null) {
                        this.pingRespTimeout
                                = ctx.channel()
                                        .eventLoop()
                                        .schedule(() -> {
                                            MqttFixedHeader disconnectFixedHeader
                                                    = new MqttFixedHeader(
                                                            MqttMessageType.DISCONNECT,
                                                            false,
                                                            MqttQoS.AT_MOST_ONCE,
                                                            false,
                                                            0
                                                    );
                                            ctx.channel().writeAndFlush(
                                                    new MqttMessage(disconnectFixedHeader)
                                            ).addListener(ChannelFutureListener.CLOSE);
                                        }, this.keepAlivePingTimeSeconds, TimeUnit.SECONDS);
                    }
                    break;
            }
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

        if (!(msg instanceof MqttMessage)) {
            ctx.fireChannelRead(msg);
            return;
        }
        MqttMessage message = (MqttMessage) msg;
        MqttMessageType messageType = message.fixedHeader().messageType();

        switch (messageType) {
            case CONNACK:
                switch (message.variableHeader().connectReturnCode()) {
                    case CONNECTION_ACCEPTED:
                        this.connectFuture.setSuccess(new MqttConnectResult(true, MqttConnectReturnCode.CONNECTION_ACCEPTED, channel.closeFuture()));

                        this.client.getPendingSubscribtions().entrySet().stream().filter((e) -> !e.getValue().isSent()).forEach((e) -> {
                            channel.write(e.getValue().getSubscribeMessage());
                            e.getValue().setSent(true);
                        });

                        this.client.getPendingPublishes().forEach((id, publish) -> {
                            if (publish.isSent()) {
                                return;
                            }
                            channel.write(publish.getMessage());
                            publish.setSent(true);
                            if (publish.getQos() == MqttQoS.AT_MOST_ONCE) {
                                publish.getFuture().setSuccess(null); //We don't get an ACK for QOS 0
                                this.client.getPendingPublishes().remove(publish.getMessageId());
                            }
                        });
                        channel.flush();
                        break;

                    case CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD:
                    case CONNECTION_REFUSED_IDENTIFIER_REJECTED:
                    case CONNECTION_REFUSED_NOT_AUTHORIZED:
                    case CONNECTION_REFUSED_SERVER_UNAVAILABLE:
                    case CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION:
                        this.connectFuture.setSuccess(new MqttConnectResult(false, message.variableHeader().connectReturnCode(), channel.closeFuture()));
                        channel.close();
                        // Don't start reconnect logic here
                        break;
                }
                break;
            case PINGREQ:
                MqttFixedHeader fixedHeader = new MqttFixedHeader(
                        MqttMessageType.PINGRESP,
                        false,
                        MqttQoS.AT_MOST_ONCE,
                        false,
                        0
                );
                ctx.channel().writeAndFlush(new MqttMessage(fixedHeader));
                break;
            case PINGRESP:
                if (this.pingRespTimeout != null
                        && !this.pingRespTimeout.isCancelled()
                        && !this.pingRespTimeout.isDone()) {
                    this.pingRespTimeout.cancel(true);
                    this.pingRespTimeout = null;
                }
                break;
            case CONNECT:
                break;
            case PUBLISH:
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
            case DISCONNECT:
                break;
            default:
                ctx.fireChannelRead(ReferenceCountUtil.retain(msg));

        }
    }

    /**
     *
     * @param ctx ChannelHandlerContext
     * @throws Exception
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx)
            throws Exception {
        /*
        controlla se ci sono operazioni pending
         */
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx)
            throws Exception {
        super.channelInactive(ctx);
    }

}
