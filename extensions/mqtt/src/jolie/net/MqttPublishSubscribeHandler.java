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
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
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
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttVersion;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.ScheduledFuture;
import java.util.ListIterator;
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

    /*
    From jk5
     */
    private ScheduledFuture<?> pingRespTimeout;
    /*
    End jk5
     */
    private final MqttProtocol mp;
    private final String clientId;

    public MqttPublishSubscribeHandler(MqttProtocol mp) {

        this.mp = mp;
        this.clientId = "jolie/" + GenerateRandomId();

        mp.setKeepAliveConnectTimeSeconds(2);
        mp.setVersion(MqttVersion.MQTT_3_1_1);
        mp.setWillTopic("");
        mp.setWillMessage("");
        mp.setUserName("");
        mp.setPassword("");
    }

    /**
     * Generate random client id for default Method taken and little modified @
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
                                        }, 10, TimeUnit.SECONDS);
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
    @SuppressWarnings("Convert2Lambda")
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
                        for (ListIterator li = mp.getPendingPublishes().listIterator(); li.hasNext();) {
                            channel.writeAndFlush(li).addListener(new ChannelFutureListener() {
                                @Override
                                public void operationComplete(ChannelFuture future) throws Exception {
                                    if (future.isSuccess()) {
                                        System.out.println("Master Yoda says "
                                                + "poweful you become, the dark force strong is in you!");
                                    }
                                }
                            });
                        }
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
            case PINGREQ:
                channel.writeAndFlush(
                        new MqttMessage(
                                new MqttFixedHeader(
                                        MqttMessageType.PINGRESP,
                                        false,
                                        MqttQoS.AT_MOST_ONCE,
                                        false,
                                        0
                                )
                        )
                );
                break;
            case PINGRESP:
                if (this.pingRespTimeout != null
                        && !this.pingRespTimeout.isCancelled()
                        && !this.pingRespTimeout.isDone()) {
                    this.pingRespTimeout.cancel(true);
                    this.pingRespTimeout = null;
                }
                break;
            case PUBACK:
                break;
            default:
                ctx.fireChannelRead(ReferenceCountUtil.retain(msg));
        }
    }

}
