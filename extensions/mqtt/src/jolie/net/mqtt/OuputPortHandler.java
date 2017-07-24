package jolie.net.mqtt;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;

import java.util.List;

import jolie.net.CommCore;
import jolie.net.CommMessage;
import jolie.net.MqttProtocol;
import jolie.net.NioSocketCommChannel;

public class OuputPortHandler extends MessageToMessageCodec<MqttMessage, CommMessage> {

    private final MqttProtocol prt;
    private Channel ch;
    private boolean connected;
    private CommMessage pendingCm;

    public OuputPortHandler(MqttProtocol protocol) {
	this.prt = protocol;
	this.connected = false;
    }

    private void init(ChannelHandlerContext ctx) {
	ch = ctx.channel();
	((CommCore.ExecutionContextThread) Thread.currentThread()).executionThread(
		ch.attr(NioSocketCommChannel.EXECUTION_CONTEXT).get());
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, CommMessage cm,
	    List<Object> out) throws Exception {

	init(ctx);
	if (connected) {
	    prt.send(ch, cm);
	} else {
	    pendingCm = cm;
	    prt.send(ch, prt.connectMsg());
	}
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, MqttMessage mm,
	    List<Object> out) throws Exception {

	init(ctx);
	switch (mm.fixedHeader().messageType()) {
	    case CONNACK:
		MqttConnectReturnCode crc = ((MqttConnAckMessage) mm).variableHeader().connectReturnCode();
		if (crc.equals(MqttConnectReturnCode.CONNECTION_ACCEPTED)) {
		    connected = prt.send(ch, pendingCm);
		    prt.startPing(ch.pipeline());
		}
		break;
	    case PUBLISH:
		prt.recPub(ch, (MqttPublishMessage) mm);
		break;
	    case PUBACK:
	    case PUBCOMP:
		prt.recAck(ch, pendingCm);
		break;
	    case PUBREC:
	    case PUBREL:
		prt.sendAck(ch, (MqttMessageIdVariableHeader) mm.variableHeader(), mm.fixedHeader().messageType());
		break;
	    case SUBACK:
		ch.writeAndFlush(prt.publishMsg(
			pendingCm.id(),
			prt.topic(pendingCm),
			pendingCm.value(),
			false,
			prt.qos(pendingCm.operationName(), MqttQoS.AT_LEAST_ONCE)));
		break;
	}
    }
}
