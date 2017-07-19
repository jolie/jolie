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
package jolie.net.mqtt;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import java.util.List;
import jolie.net.CommCore;
import jolie.net.CommMessage;
import jolie.net.MqttProtocol;
import jolie.net.NioSocketCommChannel;
import jolie.runtime.Value;

public class PublicationHandler extends MessageToMessageCodec<MqttMessage, CommMessage> {

    private final MqttProtocol prt;
    private boolean connected;
    private MqttPublishMessage toBePublished;

    public PublicationHandler(MqttProtocol protocol) {
	this.prt = protocol;
	this.connected = false;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, CommMessage msg, List<Object> out) throws Exception {

	((CommCore.ExecutionContextThread) Thread.currentThread()).executionThread(
		ctx.channel().attr(NioSocketCommChannel.EXECUTION_CONTEXT).get());

	commMsgToMqttMsg(ctx.channel(), msg, out);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, MqttMessage msg,
	    List<Object> out) throws Exception {

	((CommCore.ExecutionContextThread) Thread.currentThread()).executionThread(
		ctx.channel().attr(NioSocketCommChannel.EXECUTION_CONTEXT).get());

	mqttMsgToCommMsg(ctx.channel(), msg, out);
    }

    private void commMsgToMqttMsg(Channel ch, CommMessage msg, List<Object> out) {

	toBePublished = prt.buildPublication(
		msg.id(),
		prt.getCurrentTopic(msg),
		msg.value(),
		false,
		prt.getCurrentOperationQos(msg.operationName(), MqttQoS.AT_LEAST_ONCE));

	if (connected) {
	    ch.writeAndFlush(toBePublished);
	} else {
	    out.add(prt.buildConnect(false));
	}
    }

    private void mqttMsgToCommMsg(Channel ch, MqttMessage msg, List<Object> out) {

	if (prt.connectionAccepted(msg)) {
	    connected = true;
	    ch.writeAndFlush(toBePublished);
	    if (toBePublished.fixedHeader().qosLevel().equals(MqttQoS.AT_MOST_ONCE)) {
		out.add(new CommMessage( // qos 0
			toBePublished.variableHeader().messageId(),
			prt.getCurrentOperationName(toBePublished.variableHeader().topicName()),
			"/",
			Value.create(),
			null));
	    }
	} else {
	    switch (msg.fixedHeader().messageType()) {
		case PUBACK: // qos 1
		    out.add(new CommMessage(
			    toBePublished.variableHeader().messageId(),
			    prt.getCurrentOperationName(toBePublished.variableHeader().topicName()),
			    "/",
			    Value.create(),
			    null));
		    break;
		case PUBREC:
		    MqttFixedHeader fh = new MqttFixedHeader(MqttMessageType.PUBREL, false, MqttQoS.AT_LEAST_ONCE, false, 0);
		    MqttMessageIdVariableHeader vh = (MqttMessageIdVariableHeader) msg.variableHeader();
		    ch.writeAndFlush(new MqttMessage(fh, vh));
		    break;
		case PUBCOMP: // qos 2
		    out.add(new CommMessage(
			    toBePublished.variableHeader().messageId(),
			    prt.getCurrentOperationName(toBePublished.variableHeader().topicName()),
			    "/",
			    Value.create(),
			    null));
		    break;
	    }
	}
    }
}
