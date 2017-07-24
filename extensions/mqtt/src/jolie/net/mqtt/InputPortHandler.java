package jolie.net.mqtt;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import jolie.net.CommCore;
import jolie.net.CommMessage;
import jolie.net.MqttProtocol;
import jolie.net.NioSocketCommChannel;

public class InputPortHandler extends MessageToMessageCodec<MqttMessage, CommMessage> {

    private final MqttProtocol prt;
    private Channel ch;
    private final List<MqttSubscribeMessage> pendingSubscriptions;

    public InputPortHandler(MqttProtocol protocol) {
	this.prt = protocol;
	this.pendingSubscriptions = new ArrayList<>();
    }

    private void init(ChannelHandlerContext ctx) {
	ch = ctx.channel();
	((CommCore.ExecutionContextThread) Thread.currentThread()).executionThread(
		ch.attr(NioSocketCommChannel.EXECUTION_CONTEXT).get());
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, CommMessage msg, List<Object> out) throws Exception {

	init(ctx);
	commMsgToMqttMsg(msg);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, MqttMessage msg, List<Object> out) throws Exception {

	init(ctx);
	mqttMsgToCommMsg(msg);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {

	super.channelActive(ctx);
	pendingSubscriptions.add(prt.subscribeMsg(
		CommMessage.getNewMessageId(),
		prt.topics(),
		false,
		prt.qos(MqttQoS.AT_LEAST_ONCE)));
	ctx.channel().writeAndFlush(prt.connectMsg(false));
    }

    private void commMsgToMqttMsg(CommMessage cm) {
	throw new UnsupportedOperationException("Not supported yet.");
    }

    private void mqttMsgToCommMsg(MqttMessage mm) {
	if (prt.connectionAccepted(mm)) {
	    prt.startPing(ch.pipeline());
	    if (!(prt.isSubscriptionOnDemand(false) || pendingSubscriptions.isEmpty())) {
		for (Iterator<MqttSubscribeMessage> it = pendingSubscriptions.iterator(); it.hasNext();) {
		    MqttSubscribeMessage i = it.next();
		    ch.write(i);
		    it.remove(); //remove from the queue
		}
		ch.flush();
	    }
	}
	if (mm.fixedHeader().messageType().equals(MqttMessageType.PUBLISH)) {
	    prt.recPub(ch, (MqttPublishMessage) mm);
	} else {
	    if (mm.fixedHeader().messageType().equals(MqttMessageType.PUBREL)) {
		prt.sendAck(ch, (MqttMessageIdVariableHeader) mm.variableHeader(), mm.fixedHeader().messageType());
	    }
	}
    }
}
