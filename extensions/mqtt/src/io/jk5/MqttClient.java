package io.jk5;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.util.concurrent.Future;

public interface MqttClient {

    /**
     * Connect to the specified hostname/ip. By default uses port 1883.
     * If you want to change the port number, see {@link #connect(String, int)}
     *
     * @param host The ip address or host to connect to
     * @return A future which will be completed when the connection is opened and we received an CONNACK
     */
    Future<MqttConnectResult> connect(String host);

    /**
     * Connect to the specified hostname/ip using the specified port
     *
     * @param host The ip address or host to connect to
     * @param port The tcp port to connect to
     * @return A future which will be completed when the connection is opened and we received an CONNACK
     */
    Future<MqttConnectResult> connect(String host, int port);

    /**
     * Retrieve the netty {@link EventLoopGroup} we are using
     * @return The netty {@link EventLoopGroup} we use for the connection
     */
    EventLoopGroup getEventLoop();

    /**
     * By default we use the netty {@link NioEventLoopGroup}.
     * If you change the EventLoopGroup to another type, make sure to change the {@link Channel} class using {@link MqttClientConfig#setChannelClass(Class)}
     * If you want to force the MqttClient to use another {@link EventLoopGroup}, call this function before calling {@link #connect(String, int)}
     *
     * @param eventLoop The new eventloop to use
     */
    void setEventLoop(EventLoopGroup eventLoop);

    /**
     * Subscribe on the given topic. When a message is received, MqttClient will invoke the {@link MqttHandler#onMessage(String, ByteBuf)} function of the given handler
     *
     * @param topic The topic filter to subscribe to
     * @param handler The handler to invoke when we receive a message
     * @return A future which will be completed when the server acknowledges our subscribe request
     */
    Future<Void> on(String topic, MqttHandler handler);

    /**
     * Subscribe on the given topic, with the given qos. When a message is received, MqttClient will invoke the {@link MqttHandler#onMessage(String, ByteBuf)} function of the given handler
     *
     * @param topic The topic filter to subscribe to
     * @param handler The handler to invoke when we receive a message
     * @param qos The qos to request to the server
     * @return A future which will be completed when the server acknowledges our subscribe request
     */
    Future<Void> on(String topic, MqttHandler handler, MqttQoS qos);

    /**
     * Subscribe on the given topic. When a message is received, MqttClient will invoke the {@link MqttHandler#onMessage(String, ByteBuf)} function of the given handler
     * This subscribtion is only once. If the MqttClient has received 1 message, the subscribtion will be removed
     *
     * @param topic The topic filter to subscribe to
     * @param handler The handler to invoke when we receive a message
     * @return A future which will be completed when the server acknowledges our subscribe request
     */
    Future<Void> once(String topic, MqttHandler handler);

    /**
     * Subscribe on the given topic, with the given qos. When a message is received, MqttClient will invoke the {@link MqttHandler#onMessage(String, ByteBuf)} function of the given handler
     * This subscribtion is only once. If the MqttClient has received 1 message, the subscribtion will be removed
     *
     * @param topic The topic filter to subscribe to
     * @param handler The handler to invoke when we receive a message
     * @param qos The qos to request to the server
     * @return A future which will be completed when the server acknowledges our subscribe request
     */
    Future<Void> once(String topic, MqttHandler handler, MqttQoS qos);

    /**
     * Remove the subscribtion for the given topic and handler
     * If you want to unsubscribe from all handlers known for this topic, use {@link #off(String)}
     *
     * @param topic The topic to unsubscribe for
     * @param handler The handler to unsubscribe
     * @return A future which will be completed when the server acknowledges our unsubscribe request
     */
    Future<Void> off(String topic, MqttHandler handler);

    /**
     * Remove all subscribtions for the given topic.
     * If you want to specify which handler to unsubscribe, use {@link #off(String, MqttHandler)}
     *
     * @param topic The topic to unsubscribe for
     * @return A future which will be completed when the server acknowledges our unsubscribe request
     */
    Future<Void> off(String topic);

    /**
     * Publish a message to the given payload
     * @param topic The topic to publish to
     * @param payload The payload to send
     * @return A future which will be completed when the message is sent out of the MqttClient
     */
    Future<Void> publish(String topic, ByteBuf payload);

    /**
     * Publish a message to the given payload, using the given qos
     * @param topic The topic to publish to
     * @param payload The payload to send
     * @param qos The qos to use while publishing
     * @return A future which will be completed when the message is delivered to the server
     */
    Future<Void> publish(String topic, ByteBuf payload, MqttQoS qos);

    /**
     * Publish a message to the given payload, using optional retain
     * @param topic The topic to publish to
     * @param payload The payload to send
     * @param retain true if you want to retain the message on the server, false otherwise
     * @return A future which will be completed when the message is sent out of the MqttClient
     */
    Future<Void> publish(String topic, ByteBuf payload, boolean retain);

    /**
     * Publish a message to the given payload, using the given qos and optional retain
     * @param topic The topic to publish to
     * @param payload The payload to send
     * @param qos The qos to use while publishing
     * @param retain true if you want to retain the message on the server, false otherwise
     * @return A future which will be completed when the message is delivered to the server
     */
    Future<Void> publish(String topic, ByteBuf payload, MqttQoS qos, boolean retain);

    /**
     * Retrieve the MqttClient configuration
     * @return The {@link MqttClientConfig} instance we use
     */
    MqttClientConfig getClientConfig();

    /**
     * Construct the MqttClientImpl with default config
     * @return the mqtt client 
     */
    static MqttClient create(){
        return new MqttClientImpl();
    }

    /**
     * Construct the MqttClientImpl with additional config.
     * This config can also be changed using the {@link #getClientConfig()} function
     *
     * @param config The config object to use while looking for settings
     * @return the mqtt client
     */
    static MqttClient create(MqttClientConfig config){
        return new MqttClientImpl(config);
    }
}
