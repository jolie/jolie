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
package testJolie;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectPayload;
import io.netty.handler.codec.mqtt.MqttConnectVariableHeader;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttVersion;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import java.net.InetSocketAddress;
import java.util.Random;
import jolie.net.MqttPingHandler;
import jolie.net.MqttProtocolInboundHandler;

/**
 *
 * @author stefanopiozingaro
 */
public class MqttCommCore {

    private Channel channel;

    private ChannelFuture future;
    private final String host;
    private final String topic;
    private final String message;
    private final String clientId;
    private final MqttVersion version;
    private final int keepAliveTimeSeconds;
    private final String willTopic;
    private final String willMessage;
    private final String userName;
    private final String password;
    
    private final int DEFAULT_TIMEOUT = 30;

    /**
     * Costructor in the implementation is retrieved from Jolie protocol
     * specific parameters or set by default For the time being (05/06/2017) it
     * is defualt
     *
     * @throws InterruptedException
     */
    public MqttCommCore() throws InterruptedException {

        //VariablePath configurationPath = null;
        //this.mp = new MqttProtocol(configurationPath);
        this.host = "test.mosquitto.org";
        this.topic = "temp/random";
        // Master Joda says object this should be
        this.message = "23.0";
        this.clientId = GenerateRandomId();
        this.version = MqttVersion.MQTT_3_1_1;
        this.keepAliveTimeSeconds = 10;
        this.willTopic = "";
        this.willMessage = "";
        this.userName = "";
        this.password = "";

        CreateCommChannel(this.host);

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
     * Override CommeChannel creation in CommCore, that is create the Nio socket
     * chanenl
     *
     * @param mp
     * @throws InterruptedException
     */
    private void CreateCommChannel(String host)
            throws InterruptedException {

        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup);
            b.channel(NioSocketChannel.class);
            // optionally set the local Address 
            b.remoteAddress(new InetSocketAddress(host, 1883));
            b.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ChannelPipeline pipeline = ch.pipeline();
                    pipeline.addLast("Logger", new LoggingHandler(LogLevel.INFO));
                    pipeline.addLast("decoder", new MqttDecoder());
                    pipeline.addLast("encoder", MqttEncoder.INSTANCE);
                    pipeline.addLast("idle", new IdleStateHandler(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT, 0));
                    pipeline.addLast("ping", new MqttPingHandler(DEFAULT_TIMEOUT));
                    pipeline.addLast("inbound", new MqttProtocolInboundHandler());
                }
            });

            this.future = b.connect().sync();
            this.future.addListener((ChannelFutureListener) new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture f) throws Exception {
                    setChannel(f.channel());
                }
            });
            this.future.channel().closeFuture().sync();

        } finally {
            workerGroup.shutdownGracefully();
        }

    }

    /**
     *
     * @return MqttConnectMessage
     */
    private MqttConnectMessage buildConnectMqttMessage() {

        boolean isDup = Boolean.FALSE;
        MqttQoS connectQoS = MqttQoS.AT_LEAST_ONCE;
        boolean isConnectRetain = Boolean.FALSE;
        boolean isCleanSession = Boolean.FALSE;
        boolean isWillRetain = Boolean.FALSE;
        int willQoS = MqttQoS.AT_LEAST_ONCE.value();

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
                        willQoS,
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

        return mcm;
    }

    /**
     *
     * @return MqttPublishMessage
     */
    private MqttPublishMessage buildPublishMqttMessage() {

        boolean isDup = Boolean.FALSE;
        MqttQoS publishQoS = MqttQoS.AT_LEAST_ONCE;
        boolean isConnectRetain = Boolean.FALSE;
        int messageId = Integer.parseInt(GenerateRandomId());

        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(
                MqttMessageType.PUBLISH,
                isDup,
                publishQoS,
                isConnectRetain,
                0
        );

        MqttPublishVariableHeader variableHeader
                = new MqttPublishVariableHeader(
                        this.topic,
                        messageId
                );

        ByteBuf payload = parseObject(this.message);

        MqttPublishMessage mpm = new MqttPublishMessage(
                mqttFixedHeader,
                variableHeader,
                payload
        );

        return mpm;
    }

    /**
     *
     * @param mm MqttMessage
     */
    private void sendToBroker(MqttMessage mm) {

        if (this.channel.isActive() && this.channel.isWritable()) {
            this.channel.writeAndFlush(mm);
        }
    }

    /**
     *
     * @param channel Channel
     */
    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    /**
     * @param args the command line arguments
     * @throws java.lang.InterruptedException
     */
    public static void main(String[] args) throws InterruptedException {
        MqttCommCore mqttCommCore = new MqttCommCore();
        mqttCommCore.testMqtt();
    }

    /**
     *
     * @throws InterruptedException
     */
    private void testMqtt() throws InterruptedException {

        MqttConnectMessage mcm = buildConnectMqttMessage();
        sendToBroker(mcm);

        MqttPublishMessage mpm = buildPublishMqttMessage();
        sendToBroker(mpm);

    }

    /**
     * TODO parse object according to object type passed
     *
     * @param message
     * @return ByteBuf
     */
    private ByteBuf parseObject(Object message) {
        return Unpooled.copiedBuffer(message.toString().getBytes());
    }

}
