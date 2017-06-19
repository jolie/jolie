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
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;
import java.util.List;
import jolie.ExecutionThread;
import jolie.Interpreter;
import jolie.runtime.FaultException;
import jolie.runtime.Value;

class MqttCommMessageCodec extends MessageToMessageCodec<MqttMessage, CommMessage> {

    private final MqttProtocol mp;

    public MqttCommMessageCodec(MqttProtocol mqttProtocol) {
        super();
        this.mp = mqttProtocol;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, CommMessage message, List<Object> out)
            throws Exception {

        ((CommCore.ExecutionContextThread) Thread.currentThread()).executionThread(
                ctx.channel().attr(NioSocketCommChannel.EXECUTION_CONTEXT).get());

        Interpreter.getInstance().logInfo("Sending: " + message.toString());
        MqttMessage msg = buildMqttMessage(message);
        out.add(msg);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, MqttMessage msg, List<Object> out)
            throws Exception {

        ((CommCore.ExecutionContextThread) Thread.currentThread()).executionThread(
                ctx.channel().attr(NioSocketCommChannel.EXECUTION_CONTEXT).get());

        Interpreter.getInstance().logInfo("Mqtt message recv: " + ExecutionThread.currentThread());
        CommMessage message = recv_internal(msg);
        Interpreter.getInstance().logInfo("Decoded Mqtt request for operation: " + message.operationName());
        out.add(message);
    }

    private MqttMessage buildMqttMessage(CommMessage message) {
        MqttMessageType mmt = MqttMessageType.PINGREQ;
        boolean isDup = false;
        MqttQoS qos = MqttQoS.AT_LEAST_ONCE;
        boolean isRetain = false;
        int remainingLength = 0;
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(mmt, isDup, qos, isRetain, remainingLength);

        MqttMessage msg = new MqttMessage(mqttFixedHeader);
        return msg;
    }

    private CommMessage recv_internal(MqttMessage msg) {
        int id = 0;
        String operationName = "";
        String resourcePath = "";
        Value value = null;
        FaultException fault = new FaultException("");
        CommMessage message = new CommMessage(id, operationName, resourcePath, value, fault);
        return message;
    }
}
