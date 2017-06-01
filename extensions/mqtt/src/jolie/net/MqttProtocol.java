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
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import java.util.Random;
import jolie.net.protocols.AsyncCommProtocol;
import jolie.runtime.VariablePath;

/**
 * Implementation of the { @link AsyncCommProtocol } for MQTT protocol relying
 * on TCP/IP socket, uses netty and Non blocking Sockets
 *
 * TODO 
 * MOdificare { @link CommCore}
 * 1. in caso di una InputPort: 
 *  1.1 in caso di protocollo PublishSubscribeProtocol (e.g. 
 *      MqttProtocol extends PublishSubscribeProtocol) 
 *      si dovrà creare un CommChannel (che rimarrà aperto)
 *  2.1 altrimenti creò SocketListener e faccio la solita roba
 * 
 * @author stefanopiozingaro
 */
public class MqttProtocol extends AsyncCommProtocol {

    /*
    Build deafult client id in a random fashion way
     */
    private static final String DEFAULT_CLIENT_ID = RandomClientId();

    private static String RandomClientId() {
        Random random = new Random();
        String id = "jolie-mqtt/";
        String[] options
                = ("abcdefghijklmnopqrstuvwxyzABCDEF"
                        + "GHIJKLMNOPQRSTUVWXYZ0123456789").split("");
        for (int i = 0; i < 8; i++) {
            id += options[random.nextInt(options.length)];
        }
        return id;
    }

    /*
    Default value for the broker address, used for testing purpose
    TODO remove DEFAULT_BROKER_ADDRESS in production
     */
    private static final String DEFAULT_BROKER_ADDRESS = "iot.eclipse.org";

    /*
    Default value for the broker port is 1883, used for testing purpose,
    TODO remove DEFAULT_BROKER_PORT in production
     */
    private static final short DEFAULT_BROKER_PORT = 1883;

    /*
    Client Id taken from the protocol parameters
     */
    private String clientId;

    /**
     * Method get for the clientId String
     *
     * @return the client id
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * Method set for the clientId String
     *
     * @param clientId the string you want to be the id of the client
     */
    public void setClientId(String clientId) {
        this.clientId = clientId;
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

    /*
     * Inner class Parameters inherit from { @link HttpProtocol }
     * Since now (01/06/2017) we use only the concurrent param
     */
    private static class Parameters {

        private static final String CONCURRENT = "concurrent";

    }

    /**
     * Method overrinding setupPipeline of { @link AsyncCommProtocol } Default
     * pipeline for Mqtt use Encoder and Decoder, we added ChannelHandler
     *
     * @param pipeline the pipeline to fill with specific protocol handlers
     */
    @Override
    public void setupPipeline(ChannelPipeline pipeline) {

        pipeline.addLast("Logger", new LoggingHandler(LogLevel.INFO));
        pipeline.addLast("mqttDecoder", new MqttDecoder());
        pipeline.addLast("mqttEncoder", MqttEncoder.INSTANCE);
        /*
        pipeline.addLast("idleStateHandler", 
        new IdleStateHandler(mqtt_client.getTimeout(), 
        mqtt_client.getTimeout(), 0));
        pipeline.addLast("mqttPingHandler", 
        new MqttPingHandler(mqtt_client.getTimeout()));
         */
        pipeline.addLast("mqttHandler", new MqttChannelHandler());
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

    /*
    Implementing the MqttChannelHandler_old
     */
    /**
     * For the time being this inner class is a channel inboud handler that need
     * to implement just the channel read 0 (something about printing
     */
    private static class MqttChannelHandler extends
            SimpleChannelInboundHandler<MqttMessage> {

        /**
         * This channel read implement all the possible message that mqtt
         * retrieve wth a switch case
         * For the time being (01/06/2017) this case 
         * simply handle the connection case
         * @param chc the channel handler context 
         * for the { @link MqttChannelHandler}
         * @param i the message received
         * @throws Exception the generic exception if something goes wrong
         */
        @Override
        protected void channelRead0(ChannelHandlerContext chc, MqttMessage i)
                throws Exception {
            switch (i.fixedHeader().messageType()) {
                case CONNACK:
                    //MqttConnAckMessageHandler(chc, (MqttConnAckMessage) i);
                    break;
                case SUBACK:
                    break;
                case PUBLISH:
                    break;
                case UNSUBACK:
                    break;
                case PUBACK:
                    break;
                case PUBREC:
                    break;
                case PUBREL:
                    break;
                case PUBCOMP:
                    break;
                case CONNECT:
                    break;
                case SUBSCRIBE:
                    break;
                case UNSUBSCRIBE:
                    break;
                case PINGREQ:
                    break;
                case PINGRESP:
                    break;
                case DISCONNECT:
                    break;
                default:
                    throw new AssertionError(i.fixedHeader().messageType().name());
            }

        }

        /**
         * This method handle the arrive of a Connection acknowledgment message
         * from the Mqtt broker
         * @param chc
         * @param mqttConnAckMessage 
         */
        private void MqttConnAckMessageHandler(ChannelHandlerContext chc, 
                MqttConnAckMessage mqttConnAckMessage) {
            System.out.println("Connection Message Arrived!");
            System.out.println("Remote Address: " + chc.channel().remoteAddress());
            System.out.println("Message: " + mqttConnAckMessage.toString());
        }

    }
}
