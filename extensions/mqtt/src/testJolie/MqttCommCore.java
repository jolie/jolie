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
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectPayload;
import io.netty.handler.codec.mqtt.MqttConnectVariableHeader;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttVersion;
import java.net.InetSocketAddress;
import java.util.Random;
import jolie.net.MqttProtocol;
import jolie.runtime.VariablePath;

/**
 *
 * @author stefanopiozingaro
 */
public class MqttCommCore {

    private ChannelFuture future;
    private Channel channel;
    private String host;
    private String topic;
    private String message;
    private MqttProtocol mp;
    private String clientId;
    private MqttVersion version;
    private int keepAliveTimeSeconds;
    private String willTopic;
    private String willMessage;
    private String userName;
    private String password;

    /**
     * Costructor in the implementation is retrieved from Jolie
     * protocol specific parameters or set by default
     * For the time being (05/06/2017) it is defualt 
     * 
     * @throws InterruptedException
     */
    public MqttCommCore() throws InterruptedException {

        VariablePath configurationPath = null;
        this.mp = new MqttProtocol(configurationPath);
        this.host = "test.mosquitto.org";
        this.topic = "temp/random";
        // Master Joda says object this should be
        this.message = "23.0";
        this.clientId = GenerateRandomId();
        this.version = MqttVersion.MQTT_3_1_1;
        this.keepAliveTimeSeconds = 10;
        this.willTopic = this.topic;
        this.willMessage = this.message;
        this.userName = "";
        this.password = "";

        CreateCommChannel(this.mp, this.host);

    }

    /**
     * Generate random client id for default Method 
     * stolen and little modified @ github from jk5
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
     * Override CommeChannel creation in CommCore, that is create the 
     * Nio socket chanenl
     * @param mp
     * @throws InterruptedException
     */
    private void CreateCommChannel(MqttProtocol mp, String host)
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
                    mp.setupPipeline(ch.pipeline());
                }
            });

            /*
            questo dobiamo farlo indipendentemente se siamo in una inputPort 
            oppure in una outputPort, non dobbiamo cioè rimanere solo in ascolto
            ma dobbiamo comunque creare un canale con un endpoint
            anche per la inputPort ( la modifica credo vada fatta in CommCore)
             */
            this.future = b.connect().sync();
            //this.future.channel().closeFuture().sync();
            this.future.addListener((ChannelFutureListener) new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture f) throws Exception {
                    setChannel(f.channel());
                }
            });
        } finally {
            workerGroup.shutdownGracefully().sync();
        }

    }

    /**
     *
     * @param ctx ChannelHandlerContext
     */
    private MqttConnectMessage buildConnectMqttMessage() {

        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(
                MqttMessageType.CONNECT,
                Boolean.FALSE, // isDup
                MqttQoS.AT_LEAST_ONCE, // qos
                Boolean.FALSE, // isRetain
                0
        );

        MqttConnectVariableHeader variableHeader
                = new MqttConnectVariableHeader(
                        version.protocolName(),
                        version.protocolLevel(),
                        !"".equals(getUserName()),
                        !"".equals(getPassword()),
                        Boolean.TRUE, // isWillRetain
                        MqttQoS.AT_LEAST_ONCE.value(), //willQoS
                        !"".equals(getWillMessage()),
                        Boolean.FALSE, // isCleanSession
                        getKeepAliveTimeSeconds()
                );

        MqttConnectPayload payload = new MqttConnectPayload(
                getClientId(),
                getWillTopic(),
                getWillMessage(),
                getUserName(),
                getPassword()
        );

        MqttConnectMessage mcm = new MqttConnectMessage(
                mqttFixedHeader,
                variableHeader,
                payload
        );

        return mcm;
    }

    private MqttPublishMessage buildPublishMqttMessage() {

        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(
                MqttMessageType.PUBLISH,
                Boolean.FALSE, // isDup
                MqttQoS.AT_LEAST_ONCE, // qos
                Boolean.FALSE, // isRetain
                0
        );

        MqttPublishVariableHeader variableHeader
                = new MqttPublishVariableHeader(
                        getTopic(),
                        Integer.parseInt(GenerateRandomId())
                );

        /*
        Message should be serialized in a byte buffer here
         */
        ByteBuf payload = parseObject(getMessage());

        MqttPublishMessage mpm = new MqttPublishMessage(
                mqttFixedHeader,
                variableHeader,
                payload
        );

        return mpm;
    }

    private void sendToBroker(MqttMessage mm) {

        if (this.channel.isActive() && this.channel.isWritable()) {
            this.channel.writeAndFlush(mm);
        }
    }

    public ChannelFuture getFuture() {
        return future;
    }

    public void setFuture(ChannelFuture future) {
        this.future = future;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Object getMessage() {
        return message;
    }

    /**
     * TODO Message object should be serialized and not casted!
     *
     * @param message
     */
    public void setMessage(Object message) {
        this.message = (String) message;
    }

    public MqttProtocol getMqttProtocol() {
        return mp;
    }

    public void setMqttProtocol(MqttProtocol mp) {
        this.mp = mp;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = "jolie-mqtt/" + clientId;
    }

    public MqttVersion getVersion() {
        return version;
    }

    public void setVersion(MqttVersion version) {
        this.version = version;
    }

    public int getKeepAliveTimeSeconds() {
        return keepAliveTimeSeconds;
    }

    public void setKeepAliveTimeSeconds(int keepAliveTimeSeconds) {
        this.keepAliveTimeSeconds = keepAliveTimeSeconds;
    }

    public String getWillTopic() {
        return willTopic;
    }

    public void setWillTopic(String willTopic) {
        this.willTopic = willTopic;
    }

    public String getWillMessage() {
        return willMessage;
    }

    public void setWillMessage(String willMessage) {
        this.willMessage = willMessage;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Channel getChannel() {
        return channel;
    }

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
     * @return
     */
    private ByteBuf parseObject(Object message) {
        return Unpooled.copiedBuffer(message.toString().getBytes());
    }

}
