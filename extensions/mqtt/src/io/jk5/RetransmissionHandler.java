package io.jk5;

import io.netty.channel.EventLoop;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.util.concurrent.ScheduledFuture;

import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

final class RetransmissionHandler<T extends MqttMessage> {

    private ScheduledFuture<?> timer;
    private int timeout = 10;
    private BiConsumer<MqttFixedHeader, T> handler;
    private T originalMessage;

    void start(EventLoop eventLoop){
        if(eventLoop == null){
            throw new NullPointerException("eventLoop");
        }
        if(this.handler == null){
            throw new NullPointerException("handler");
        }
        this.timeout = 10;
        this.startTimer(eventLoop);
    }

    private void startTimer(EventLoop eventLoop){
        this.timer = eventLoop.schedule(() -> {
            this.timeout += 5;
            MqttFixedHeader fixedHeader = new MqttFixedHeader(this.originalMessage.fixedHeader().messageType(), true, this.originalMessage.fixedHeader().qosLevel(), this.originalMessage.fixedHeader().isRetain(), this.originalMessage.fixedHeader().remainingLength());
            handler.accept(fixedHeader, originalMessage);
            startTimer(eventLoop);
        }, timeout, TimeUnit.SECONDS);
    }

    void stop(){
        if(this.timer != null){
            this.timer.cancel(true);
        }
    }

    void setHandle(BiConsumer<MqttFixedHeader, T> runnable) {
        this.handler = runnable;
    }

    void setOriginalMessage(T originalMessage) {
        this.originalMessage = originalMessage;
    }
}
