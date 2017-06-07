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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttVersion;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import jolie.net.protocols.AsyncCommProtocol;
import jolie.runtime.VariablePath;

/**
 * Implementation of the { @link AsyncCommProtocol } for MQTT protocol relying
 * on TCP/IP socket, uses netty and Non blocking Sockets
 *
 * TODO MOdificare { @link CommCore} 1. in caso di una InputPort: 1.1 in caso di
 * protocollo PublishSubscribeProtocol (e.g. MqttProtocol extends
 * PublishSubscribeProtocol) si dovrà creare un CommChannel (che rimarrà aperto)
 * 2.1 altrimenti creò SocketListener e faccio la solita roba
 *
 * @author stefanopiozingaro
 */
public class MqttProtocol extends AsyncCommProtocol {

    /*
    to set in the main behaviour for mqtt
    mandatory
    */
    private Channel mqttChannel;
    private String mqttTopic;
    
    /*
    to set in the main behaviour for mqtt
    optional
    */
    private MqttVersion version;
    private String willTopic;
    private String willMessage;
    private String userName;
    private String password;
    private int keepAliveConnectTimeSeconds;
    
    /*
    to publish and to subscribe
    */
    private ArrayList<MqttPublishMessage> pendingPublishes = new ArrayList<>();
    private ArrayList<MqttSubscribeMessage> pendingSubscriptions = new ArrayList<>();

    /*
    set & get
    */
    public Channel getMqttChannel() {
        return mqttChannel;
    }

    public void setMqttChannel(Channel mqttChannel) {
        this.mqttChannel = mqttChannel;
    }

    public String getMqttTopic() {
        return mqttTopic;
    }

    public void setMqttTopic(String mqttTopic) {
        this.mqttTopic = mqttTopic;
    }

    public List<MqttPublishMessage> getPendingPublishes() {
        return pendingPublishes;
    }

    public void setPendingPublishes(ArrayList<MqttPublishMessage> pendingPublishes) {
        this.pendingPublishes = pendingPublishes;
    }

    public ArrayList<MqttSubscribeMessage> getPendingSubscriptions() {
        return pendingSubscriptions;
    }

    public void setPendingSubscriptions(ArrayList<MqttSubscribeMessage> pendingSubscriptions) {
        this.pendingSubscriptions = pendingSubscriptions;
    }
    
    public MqttVersion getVersion() {
        return version;
    }

    public void setVersion(MqttVersion version) {
        this.version = version;
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

    public int getKeepAliveConnectTimeSeconds() {
        return keepAliveConnectTimeSeconds;
    }

    public void setKeepAliveConnectTimeSeconds(int keepAliveConnectTimeSeconds) {
        this.keepAliveConnectTimeSeconds = keepAliveConnectTimeSeconds;
    }

    /**
     * Default Constructor for MqttProtocol going super Look at the { @link
     * HttpProtocol.java} one
     *
     * @param configurationPath
     */
    public MqttProtocol(VariablePath configurationPath) {
        super(configurationPath);
    }

    /**
     * To publish, just take a future MqttMessage object
     * TODO implement msgToMqttMsgCodec()
     * @param string
     */
    void publish(String message) {

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
                        mqttTopic,
                        messageId
                );

        ByteBuf payload = parseObject(message);

        MqttPublishMessage mpm = new MqttPublishMessage(
                mqttFixedHeader,
                variableHeader,
                payload
        );

        pendingPublishes.add(mpm);
    }

    /*
     * Inner class Parameters copied from { @link HttpProtocol }
     * Since now (01/06/2017) we use only the concurrent param
     */
    private static class Parameters {

        private static final String CONCURRENT = "concurrent";

    }

    /**
     * Method overrinding setupPipeline of { @link AsyncCommProtocol } Default
     * pipeline for Mqtt use Encoder and Decoder, we added { @link MqttProtocolInboundHandler
     * }
     *
     * @param pipeline the pipeline to fill with specific protocol handlers
     */
    @Override
    public void setupPipeline(ChannelPipeline pipeline) {

        pipeline.addLast("MqttDecoder", new MqttDecoder());
        pipeline.addLast("MqttEncoder", MqttEncoder.INSTANCE);
        pipeline.addLast("IdleState", new IdleStateHandler(4, 5, 0, TimeUnit.SECONDS));
        pipeline.addLast("MqttPublishSubscribe", new MqttPublishSubscribeHandler(this));

    }

    /**
     *
     * @return the name of the protocol, in which case is mqtt
     */
    @Override
    public String name() {
        return "mqtt";
    }

    /**
     *
     * @return if the behaviour is concurrent or not (i guess @author
     * stefanopiozingaro)
     */
    @Override
    public boolean isThreadSafe() {
        return checkBooleanParameter(Parameters.CONCURRENT);
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
        String[] options = "0123456789".split("");
        for (int i = 0; i < 8; i++) {
            id += options[random.nextInt(options.length)];
        }
        return id;
    }

    /**
     * TODO parse object according to object type passed
     *
     * @param message
     * @return ByteBuf
     */
    private ByteBuf parseObject(String message) {
        return Unpooled.buffer().writeBytes(message.getBytes(CharsetUtil.UTF_8));
    }

}
