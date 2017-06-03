package io.jk5;

import io.netty.buffer.ByteBuf;

public interface MqttHandler {

    void onMessage(String topic, ByteBuf payload);
}
