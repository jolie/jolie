package jolie.net.mqtt;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.mqtt.*;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents an MqttClient connected to a single MQTT server. Will try to keep
 * the connection going at all times
 */
public class MqttClient {

    private final Set<String> serverSubscribtions = new HashSet<>();
    private final IntObjectHashMap<MqttPendingUnsubscribtion> pendingServerUnsubscribes = new IntObjectHashMap<>();
    private final IntObjectHashMap<MqttIncomingQos2Publish> qos2PendingIncomingPublishes = new IntObjectHashMap<>();
    private final IntObjectHashMap<MqttPendingPublish> pendingPublishes = new IntObjectHashMap<>();
    private final HashMultimap<String, MqttSubscribtion> subscriptions = HashMultimap.create();
    private final IntObjectHashMap<MqttPendingSubscribtion> pendingSubscribtions = new IntObjectHashMap<>();
    private final Set<String> pendingSubscribeTopics = new HashSet<>();
    private final HashMultimap<MqttHandler, MqttSubscribtion> handlerToSubscribtion = HashMultimap.create();
    private final AtomicInteger nextMessageId = new AtomicInteger(1);

    private EventLoopGroup eventLoop;
    private Channel channel;
    
    private final String clientId;
    private final int timeout;
    private final MqttVersion protocolVersion;
    private final String username = null;
    private final String password = null;
    private final boolean cleanSession;
    private final MqttLastWill lastWill = null;
    private final Class<? extends Channel> channelClass;

    /**
     * Setting default parameters for the connection
     */
    public MqttClient() {
        this.clientId = RandomClientId();
        this.timeout = 10;
        this.protocolVersion = MqttVersion.MQTT_3_1_1;
        this.cleanSession = false;
        this.channelClass = NioSocketChannel.class;
        //this.clientConfig = new MqttClientConfig();
    }

    /**
     * Connect to the specified hostname/ip using the specified port
     *
     * @param host The ip address or host to connect to
     * @param port The tcp port to connect to
     * @return A future which will be completed when the connection is opened
     * and we received a CONNACK
     *
     * TODO add localAddress bound to localhost
     */
    public Future<MqttConnectResult> connect(String host, int port) {

        if (this.eventLoop == null) {
            this.eventLoop = new NioEventLoopGroup();
        }
        Promise<MqttConnectResult> connectFuture
                = new DefaultPromise<>(this.eventLoop.next());

        Bootstrap b = new Bootstrap();
        b.group(this.eventLoop)
                .channel(this.channelClass)
                .remoteAddress(host, port)
                .handler(new MqttChannelInitializer(connectFuture));

        ChannelFuture future = b.connect();
        future.addListener((ChannelFutureListener) f
                -> MqttClient.this.channel = f.channel()
        );

        return connectFuture;
    }

    /**
     * Retrieve the netty {@link EventLoopGroup} we are using
     *
     * @return The netty {@link EventLoopGroup} we use for the connection
     */
    public EventLoopGroup getEventLoop() {
        return eventLoop;
    }

    /**
     * By default we use the netty {@link NioEventLoopGroup}. If you change the
     * EventLoopGroup to another type, make sure to change the {@link Channel}
     * class using {@link MqttClientConfig#setChannelClass(Class)} If you want
     * to force the MqttClient to use another {@link EventLoopGroup}, call this
     * function before calling {@link #connect(String, int)}
     *
     * @param eventLoop The new eventloop to use
     */
    public void setEventLoop(EventLoopGroup eventLoop) {
        this.eventLoop = eventLoop;
    }

    /**
     * Subscribe on the given topic. When a message is received, MqttClient will
     * invoke the {@link MqttHandler#onMessage(String, ByteBuf)} function of the
     * given handler
     *
     * @param topic The topic filter to subscribe to
     * @param handler The handler to invoke when we receive a message
     * @return A future which will be completed when the server acknowledges our
     * subscribe request
     */
    public Future<Void> on(String topic, MqttHandler handler) {
        return on(topic, handler, MqttQoS.AT_MOST_ONCE);
    }

    /**
     * Subscribe on the given topic, with the given qos. When a message is
     * received, MqttClient will invoke the
     * {@link MqttHandler#onMessage(String, ByteBuf)} function of the given
     * handler
     *
     * @param topic The topic filter to subscribe to
     * @param handler The handler to invoke when we receive a message
     * @param qos The qos to request to the server
     * @return A future which will be completed when the server acknowledges our
     * subscribe request
     */
    public Future<Void> on(String topic, MqttHandler handler, MqttQoS qos) {
        return createSubscribtion(topic, handler, false, qos);
    }

    /**
     * Subscribe on the given topic. When a message is received, MqttClient will
     * invoke the {@link MqttHandler#onMessage(String, ByteBuf)} function of the
     * given handler This subscribtion is only once. If the MqttClient has
     * received 1 message, the subscribtion will be removed
     *
     * @param topic The topic filter to subscribe to
     * @param handler The handler to invoke when we receive a message
     * @return A future which will be completed when the server acknowledges our
     * subscribe request
     */
    public Future<Void> once(String topic, MqttHandler handler) {
        return once(topic, handler, MqttQoS.AT_MOST_ONCE);
    }

    /**
     * Subscribe on the given topic, with the given qos. When a message is
     * received, MqttClient will invoke the
     * {@link MqttHandler#onMessage(String, ByteBuf)} function of the given
     * handler This subscribtion is only once. If the MqttClient has received 1
     * message, the subscribtion will be removed
     *
     * @param topic The topic filter to subscribe to
     * @param handler The handler to invoke when we receive a message
     * @param qos The qos to request to the server
     * @return A future which will be completed when the server acknowledges our
     * subscribe request
     */
    public Future<Void> once(String topic, MqttHandler handler, MqttQoS qos) {
        return createSubscribtion(topic, handler, true, qos);
    }

    /**
     * Remove the subscribtion for the given topic and handler If you want to
     * unsubscribe from all handlers known for this topic, use
     * {@link #off(String)}
     *
     * @param topic The topic to unsubscribe for
     * @param handler The handler to unsubscribe
     * @return A future which will be completed when the server acknowledges our
     * unsubscribe request
     */
    public Future<Void> off(String topic, MqttHandler handler) {
        Promise<Void> future = new DefaultPromise<>(this.eventLoop.next());
        this.handlerToSubscribtion.get(handler).forEach((subscribtion) -> {
            this.subscriptions.remove(topic, subscribtion);
        });
        this.handlerToSubscribtion.removeAll(handler);
        this.checkSubscribtions(topic, future);
        return future;
    }

    /**
     * Remove all subscribtions for the given topic. If you want to specify
     * which handler to unsubscribe, use {@link #off(String, MqttHandler)}
     *
     * @param topic The topic to unsubscribe for
     * @return A future which will be completed when the server acknowledges our
     * unsubscribe request
     */
    public Future<Void> off(String topic) {

        Promise<Void> future = new DefaultPromise<>(this.eventLoop.next());
        ImmutableSet<MqttSubscribtion> subscribtions
                = ImmutableSet.copyOf(this.subscriptions.get(topic));
        subscribtions
                .stream()
                .map((subscribtion) -> {
                    this.handlerToSubscribtion
                            .get(subscribtion.getHandler())
                            .forEach((handSub) -> {
                                this.subscriptions.remove(topic, handSub);
                            });
                    return subscribtion;
                })
                .forEachOrdered((subscribtion) -> {
                    this.handlerToSubscribtion
                            .remove(subscribtion.getHandler(), subscribtion);
                });
        this.checkSubscribtions(topic, future);

        return future;
    }

    /**
     * Publish a message to the given payload
     *
     * @param topic The topic to publish to
     * @param payload The payload to send
     * @return A future which will be completed when the message is sent out of
     * the MqttClient
     */
    public Future<Void> publish(String topic, ByteBuf payload) {
        return publish(topic, payload, MqttQoS.AT_MOST_ONCE, false);
    }

    /**
     * Publish a message to the given payload, using the given qos
     *
     * @param topic The topic to publish to
     * @param payload The payload to send
     * @param qos The qos to use while publishing
     * @return A future which will be completed when the message is delivered to
     * the server
     */
    public Future<Void> publish(String topic, ByteBuf payload, MqttQoS qos) {
        return publish(topic, payload, qos, false);
    }

    /**
     * Publish a message to the given payload, using optional retain
     *
     * @param topic The topic to publish to
     * @param payload The payload to send
     * @param retain true if you want to retain the message on the server, false
     * otherwise
     * @return A future which will be completed when the message is sent out of
     * the MqttClient
     */
    public Future<Void> publish(String topic, ByteBuf payload, boolean retain) {
        return publish(topic, payload, MqttQoS.AT_MOST_ONCE, retain);
    }

    /**
     * Publish a message to the given payload, using the given qos and optional
     * retain
     *
     * @param topic The topic to publish to
     * @param payload The payload to send
     * @param qos The qos to use while publishing
     * @param retain true if you want to retain the message on the server, false
     * otherwise
     * @return A future which will be completed when the message is delivered to
     * the server
     */
    public Future<Void> publish(String topic,
            ByteBuf payload, MqttQoS qos, boolean retain) {

        Promise<Void> future
                = new DefaultPromise<>(this.eventLoop.next());
        MqttFixedHeader fixedHeader
                = new MqttFixedHeader(MqttMessageType.PUBLISH,
                        false,
                        qos,
                        retain,
                        0);
        MqttPublishVariableHeader variableHeader
                = new MqttPublishVariableHeader(topic,
                        getNewMessageId().messageId());
        MqttPublishMessage message
                = new MqttPublishMessage(fixedHeader, variableHeader, payload);

        MqttPendingPublish pendingPublish
                = new MqttPendingPublish(variableHeader.messageId(),
                        future,
                        payload.retain(),
                        message, qos);

        pendingPublish.setSent(this.sendAndFlushPacket(message) != null);

        if (pendingPublish.isSent()
                && pendingPublish.getQos() == MqttQoS.AT_MOST_ONCE) {

            //We don't get an ACK for QOS 0
            pendingPublish.getFuture().setSuccess(null);

        } else if (pendingPublish.isSent()) {

            this.pendingPublishes
                    .put(pendingPublish.getMessageId(), pendingPublish);
            pendingPublish
                    .startPublishRetransmissionTimer(
                            this.eventLoop.next(),
                            this::sendAndFlushPacket
                    );
        }

        return future;
    }

    /*
    PRIVATE API
     */
    ChannelFuture sendAndFlushPacket(Object message) {

        if (this.channel == null) {
            return null;
        }
        if (this.channel.isActive()) {
            return this.channel.writeAndFlush(message);
        }
        return this.channel
                .newFailedFuture(new RuntimeException("Channel is closed"));
    }

    private MqttMessageIdVariableHeader getNewMessageId() {

        this.nextMessageId.compareAndSet(0xffff, 1);

        return MqttMessageIdVariableHeader.from(this.nextMessageId.getAndIncrement());
    }

    private Future<Void> createSubscribtion(String topic,
            MqttHandler handler,
            boolean once,
            MqttQoS qos) {

        if (this.pendingSubscribeTopics.contains(topic)) {

            Optional<Map.Entry<Integer, MqttPendingSubscribtion>> subscribtionEntry
                    = this.pendingSubscribtions
                            .entrySet()
                            .stream()
                            .filter(
                                    (e) -> e.getValue()
                                            .getTopic()
                                            .equals(topic)
                            )
                            .findAny();

            if (subscribtionEntry.isPresent()) {

                subscribtionEntry.get()
                        .getValue()
                        .addHandler(handler, once);

                return subscribtionEntry
                        .get()
                        .getValue()
                        .getFuture();
            }
        }
        if (this.serverSubscribtions.contains(topic)) {

            MqttSubscribtion subscribtion = new MqttSubscribtion(topic, handler, once);
            this.subscriptions.put(topic, subscribtion);
            this.handlerToSubscribtion.put(handler, subscribtion);
            return this.channel.newSucceededFuture();
        }

        Promise<Void> future = new DefaultPromise<>(this.eventLoop.next());
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.SUBSCRIBE, false, MqttQoS.AT_LEAST_ONCE, false, 0);
        MqttTopicSubscription subscription = new MqttTopicSubscription(topic, qos);
        MqttMessageIdVariableHeader variableHeader = getNewMessageId();
        MqttSubscribePayload payload = new MqttSubscribePayload(Collections.singletonList(subscription));
        MqttSubscribeMessage message = new MqttSubscribeMessage(fixedHeader, variableHeader, payload);

        final MqttPendingSubscribtion pendingSubscribtion = new MqttPendingSubscribtion(future, topic, message);
        pendingSubscribtion.addHandler(handler, once);
        this.pendingSubscribtions.put(variableHeader.messageId(), pendingSubscribtion);
        this.pendingSubscribeTopics.add(topic);
        pendingSubscribtion.setSent(this.sendAndFlushPacket(message) != null); //If not sent, we will send it when the connection is opened

        pendingSubscribtion.startRetransmitTimer(this.eventLoop.next(), this::sendAndFlushPacket);

        return future;
    }

    private void checkSubscribtions(String topic, Promise<Void> promise) {
        if (!(this.subscriptions.containsKey(topic) && !this.subscriptions.get(topic).isEmpty()) && this.serverSubscribtions.contains(topic)) {
            MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.UNSUBSCRIBE, false, MqttQoS.AT_LEAST_ONCE, false, 0);
            MqttMessageIdVariableHeader variableHeader = getNewMessageId();
            MqttUnsubscribePayload payload = new MqttUnsubscribePayload(Collections.singletonList(topic));
            MqttUnsubscribeMessage message = new MqttUnsubscribeMessage(fixedHeader, variableHeader, payload);

            MqttPendingUnsubscribtion pendingUnsubscribtion = new MqttPendingUnsubscribtion(promise, topic, message);
            this.pendingServerUnsubscribes.put(variableHeader.messageId(), pendingUnsubscribtion);
            pendingUnsubscribtion.startRetransmissionTimer(this.eventLoop.next(), this::sendAndFlushPacket);

            this.sendAndFlushPacket(message);
        } else {
            promise.setSuccess(null);
        }
    }

    IntObjectHashMap<MqttPendingSubscribtion> getPendingSubscribtions() {
        return pendingSubscribtions;
    }

    HashMultimap<String, MqttSubscribtion> getSubscriptions() {
        return subscriptions;
    }

    Set<String> getPendingSubscribeTopics() {
        return pendingSubscribeTopics;
    }

    HashMultimap<MqttHandler, MqttSubscribtion> getHandlerToSubscribtion() {
        return handlerToSubscribtion;
    }

    Set<String> getServerSubscribtions() {
        return serverSubscribtions;
    }

    IntObjectHashMap<MqttPendingUnsubscribtion> getPendingServerUnsubscribes() {
        return pendingServerUnsubscribes;
    }

    IntObjectHashMap<MqttPendingPublish> getPendingPublishes() {
        return pendingPublishes;
    }

    IntObjectHashMap<MqttIncomingQos2Publish> getQos2PendingIncomingPublishes() {
        return qos2PendingIncomingPublishes;
    }

    private String RandomClientId() {
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

    private class MqttChannelInitializer extends ChannelInitializer<SocketChannel> {

        private final Promise<MqttConnectResult> connectFuture;

        MqttChannelInitializer(Promise<MqttConnectResult> connectFuture) {
            this.connectFuture = connectFuture;
        }

        @Override
        protected void initChannel(SocketChannel ch) throws Exception {
            ch.pipeline().addLast("mqttDecoder", new MqttDecoder());
            ch.pipeline().addLast("mqttEncoder", MqttEncoder.INSTANCE);
            ch.pipeline().addLast("idleStateHandler",
                    new IdleStateHandler(
                            MqttClient.this.timeout,
                            MqttClient.this.timeout,
                            0
                    )
            );
            ch.pipeline().addLast("mqttPingHandler",
                    new MqttPingHandler(MqttClient.this.timeout)
            );
            ch.pipeline().addLast("mqttHandler",
                    new MqttChannelHandler(
                            MqttClient.this,
                            connectFuture
                    )
            );
        }
    }

}
