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
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectPayload;
import io.netty.handler.codec.mqtt.MqttConnectVariableHeader;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttVersion;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
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

    private final String clientId = GenerateRandomClientId(); // mandatory
    private final MqttVersion version = MqttVersion.MQTT_3_1_1;
    private final int keepAliveTimeSeconds = 5;
    /*
    These should be checked
     */
    private final String willTopic = "myWill"; // optional
    private final String willMessage = "goodbye"; // optional
    private final String userName = "username"; // optional
    private final String password = "password"; // optional

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
     * Once the communication channel is established beetween local port and the
     * broker, the single client should connect with the broker with fixed,
     * variable header and payload containing basically the identifier
     *
     * @param channel
     */
    public void buildMqttConnectMessage(Channel channel) {

        MqttConnectPayload payload
                = new MqttConnectPayload(
                        clientId,
                        willTopic,
                        willMessage,
                        userName,
                        password
                );
        MqttFixedHeader mqttFixedHeader
                = new MqttFixedHeader(
                        MqttMessageType.CONNECT,
                        Boolean.FALSE, // isDup
                        MqttQoS.AT_LEAST_ONCE, // qos
                        Boolean.FALSE, // isRetain
                        10 + (payload.toString().getBytes(CharsetUtil.UTF_8).length)
                );
        MqttConnectVariableHeader variableHeader
                = new MqttConnectVariableHeader(
                        version.protocolName(),
                        version.protocolLevel(),
                        checkBooleanParameter(userName),
                        checkBooleanParameter(password),
                        Boolean.TRUE, // isWillRetain
                        MqttQoS.AT_LEAST_ONCE.value(), //willQoS
                        checkBooleanParameter(willMessage),
                        Boolean.FALSE, // isCleanSession
                        keepAliveTimeSeconds
                );
        MqttConnectMessage mcm
                = new MqttConnectMessage(mqttFixedHeader, variableHeader, payload);
        
        channel.writeAndFlush(mcm);
    }

    /**
     * Generate random client id for default Method taken from github of jk5
     *
     * @return the random generated client id
     */
    private String GenerateRandomClientId() {
        Random random = new Random();
        String id = "jolie-mqtt/";
        String[] options
                = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".split("");
        for (int i = 0; i < 8; i++) {
            id += options[random.nextInt(options.length)];
        }
        return id;
    }

    /*
     * Inner class Parameters inherit from { @link HttpProtocol }
     * Since now (01/06/2017) we use only the concurrent param
     */
    private static class Parameters {

        private static final String CONCURRENT = "concurrent";

    }

    /**
     * Method overrinding setupPipeline of { @link AsyncCommProtocol } Default
     * pipeline for Mqtt use Encoder and Decoder, we added { @link MqttProtocolHandler
     * }
     *
     * @param pipeline the pipeline to fill with specific protocol handlers
     */
    @Override
    public void setupPipeline(ChannelPipeline pipeline) {

        pipeline.addLast("Logger", new LoggingHandler(LogLevel.INFO));
        pipeline.addLast("decoder", new MqttDecoder());
        pipeline.addLast("encoder", MqttEncoder.INSTANCE);
        pipeline.addLast("idle", new IdleStateHandler(0, 0, 60, TimeUnit.SECONDS));
        pipeline.addLast("handler", new MqttProtocolHandler());
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
}
