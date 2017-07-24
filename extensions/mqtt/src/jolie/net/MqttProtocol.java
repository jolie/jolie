package jolie.net;

import jolie.net.mqtt.OuputPortHandler;
import jolie.net.mqtt.InputPortHandler;
import jolie.net.mqtt.PingHandler;
import jolie.net.protocols.AsyncCommProtocol;

import jolie.runtime.VariablePath;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.typing.OneWayTypeDescription;
import jolie.runtime.typing.RequestResponseTypeDescription;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectPayload;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttConnectVariableHeader;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttSubscribePayload;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import io.netty.handler.codec.mqtt.MqttVersion;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jolie.net.ports.InputPort;
import jolie.runtime.FaultException;

public class MqttProtocol extends AsyncCommProtocol {

    public MqttProtocol(VariablePath configurationPath) {
	super(configurationPath);
    }

    @Override
    public void setupPipeline(ChannelPipeline p) {
	p.addLast(new LoggingHandler(LogLevel.INFO));
	p.addLast(MqttEncoder.INSTANCE);
	p.addLast(new MqttDecoder());
	p.addLast("Ping", new PingHandler());
	if (channel().parentPort() instanceof InputPort) {
	    p.addLast(new InputPortHandler(this));
	} else {
	    p.addLast(new OuputPortHandler(this));
	}
    }

    @Override
    public String name() {
	return "mqtt";
    }

    @Override
    public boolean isThreadSafe() {
	return checkBooleanParameter(Parameters.CONCURRENT);
    }

    public MqttConnectMessage connectMsg() {
	return connectMsg(false);
    }

    /**
     * TODO isWillRetain is set to false by default; connection qos is set to 0;
     * will qos is set to 0 by default; remaining length is not 0
     *
     * @param isDup boolean for future connection refused purpose
     * @return MqttConnectMessage
     */
    public MqttConnectMessage connectMsg(boolean isDup) {

	Random random = new Random();
	String clientId = "jolie-mqtt/";
	String[] options = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".split("");
	for (int i = 0; i < 8; i++) {
	    clientId += options[random.nextInt(options.length)];
	}

	MqttFixedHeader mfh = new MqttFixedHeader(
		MqttMessageType.CONNECT,
		isDup,
		MqttQoS.AT_MOST_ONCE,
		false,
		0);
	MqttConnectVariableHeader vh = new MqttConnectVariableHeader(
		Parameters.MQTT_VERSION.protocolName(),
		Parameters.MQTT_VERSION.protocolLevel(),
		checkBooleanParameter(Parameters.USERNAME),
		checkBooleanParameter(Parameters.PASSWORD),
		false,
		MqttQoS.AT_MOST_ONCE.value(),
		checkBooleanParameter(Parameters.WILL_TOPIC),
		true,
		2);
	MqttConnectPayload p = new MqttConnectPayload(
		clientId,
		getStringParameter(Parameters.WILL_TOPIC),
		getStringParameter(Parameters.WILL_MESSAGE),
		getStringParameter(Parameters.USERNAME),
		getStringParameter(Parameters.PASSWORD));

	return new MqttConnectMessage(mfh, vh, p);
    }

    public MqttPublishMessage publishMsg(long messageId, String topic, Value message, MqttQoS pubQos) {
	return publishMsg(messageId, topic, message, false, pubQos);
    }

    public MqttPublishMessage publishMsg(long messageId, String topic, Value message, boolean isDup, MqttQoS pubQos) {

	MqttFixedHeader mfh = new MqttFixedHeader(
		MqttMessageType.PUBLISH,
		isDup,
		pubQos,
		false,
		0);
	MqttPublishVariableHeader vh = new MqttPublishVariableHeader(topic, (int) messageId);
	ByteBuf bb = Unpooled.wrappedBuffer(parseValue(message).getBytes(CharsetUtil.UTF_8));

	return new MqttPublishMessage(mfh, vh, bb);
    }

    public MqttSubscribeMessage subscribeMsg(long messageId, List<String> topics, MqttQoS subQos) {
	return subscribeMsg(messageId, topics, false, subQos);
    }

    public MqttSubscribeMessage subscribeMsg(long messageId, List<String> topics, boolean isDup, MqttQoS subQos) {

	List<MqttTopicSubscription> tmsL = new ArrayList<>();
	for (String t : topics) {
	    tmsL.add(new MqttTopicSubscription(t, MqttProtocol.this.qos(operation(t), MqttQoS.EXACTLY_ONCE)));
	}
	MqttFixedHeader mfh = new MqttFixedHeader(
		MqttMessageType.SUBSCRIBE,
		isDup,
		subQos,
		false,
		0);
	MqttMessageIdVariableHeader vh = MqttMessageIdVariableHeader.from((int) messageId);
	MqttSubscribePayload p = new MqttSubscribePayload(tmsL);

	return new MqttSubscribeMessage(mfh, vh, p);
    }

    public void recPub(Channel channel, MqttPublishMessage mpm) {
	switch (mpm.fixedHeader().qosLevel()) {
	    case AT_MOST_ONCE:
		break;
	    case AT_LEAST_ONCE:
		if (mpm.variableHeader().messageId() != -1) {
		    MqttFixedHeader fh = new MqttFixedHeader(MqttMessageType.PUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0);
		    MqttMessageIdVariableHeader vh = MqttMessageIdVariableHeader.from(mpm.variableHeader().messageId());
		    channel.writeAndFlush(new MqttPubAckMessage(fh, vh));
		}
		break;
	    case EXACTLY_ONCE:
		if (mpm.variableHeader().messageId() != -1) {
		    MqttFixedHeader fh = new MqttFixedHeader(MqttMessageType.PUBREC, false, MqttQoS.AT_MOST_ONCE, false, 0);
		    MqttMessageIdVariableHeader vh = MqttMessageIdVariableHeader.from(mpm.variableHeader().messageId());
		    channel.writeAndFlush(new MqttMessage(fh, vh));
		}
		break;
	}
	channel.writeAndFlush(new CommMessage(
		CommMessage.GENERIC_ID,
		operation(mpm.variableHeader().topicName()),
		"/",
		value(mpm.payload()),
		new FaultException(mpm.decoderResult().cause())));
    }

    public String topic(CommMessage message) {

	StringBuilder sb = new StringBuilder();
	String alias;

	if (isOneWay(message.operationName())) {
	    if (hasOperationSpecificParameter(message.operationName(), Parameters.ALIAS)) {
		alias = getOperationSpecificStringParameter(message.operationName(), Parameters.ALIAS);
	    } else {
		return message.operationName();
	    }
	} else {
	    if (hasOperationSpecificParameter(message.operationName(), Parameters.ALIAS_RESPONSE)) {
		alias = getOperationSpecificStringParameter(message.operationName(), Parameters.ALIAS_RESPONSE);
	    } else {
		return message.operationName() + "/response";
	    }
	}

	int offset = 0;
	List< String> aliasKeys = new ArrayList<>();
	String currStrValue;
	String currKey;
	StringBuilder result = new StringBuilder(alias);
	Matcher m = Pattern.compile("%(!)?\\{[^\\}]*\\}").matcher(alias);

	while (m.find()) {
	    currKey = alias.substring(m.start() + 3, m.end() - 1);
	    currStrValue = message.value().getFirstChild(currKey).strValue();
	    aliasKeys.add(currKey);
	    result.replace(
		    m.start() + offset, m.end() + offset,
		    currStrValue
	    );
	    offset += currStrValue.length() - 3 - currKey.length();
	}

	for (String aliasKey : aliasKeys) {
	    message.value().children().remove(aliasKey);
	}
	sb.append(result);

	return sb.toString();
    }

    public MqttQoS qos(MqttQoS defaultQoS) {

	return hasParameter(Parameters.QOS) ? MqttQoS.valueOf(getIntParameter(Parameters.QOS)) : defaultQoS;
    }

    public MqttQoS qos(String operationName, MqttQoS defaultQoS) {

	return hasOperationSpecificParameter(operationName, Parameters.QOS) ? MqttQoS.valueOf(getOperationSpecificParameterFirstValue(operationName, Parameters.QOS).intValue()) : defaultQoS;
    }

    public boolean connectionAccepted(MqttMessage msg) {

	return msg.fixedHeader().messageType().equals(MqttMessageType.CONNACK) && ((MqttConnAckMessage) msg).variableHeader().connectReturnCode().equals(MqttConnectReturnCode.CONNECTION_ACCEPTED);
    }

    public String operation(String topic) {

	if (configurationPath().getValue().hasChildren("osc")) {
	    for (Map.Entry<String, ValueVector> i : configurationPath().getValue().getFirstChild("osc").children().entrySet()) {
		for (Map.Entry<String, ValueVector> j : i.getValue().first().children().entrySet()) {
		    if (j.getKey().equals("alias") && j.getValue().first().strValue().equals(topic)) {
			return i.getKey();
		    }
		}
	    }
	}
	return topic;
    }

    public boolean isSubscriptionOnDemand(boolean defaultSubscriptionOnDemand) {
	return hasParameter(Parameters.SUBSCRIPTION_ON_DEMAND) ? Boolean.getBoolean(getStringParameter(Parameters.SUBSCRIPTION_ON_DEMAND)) : defaultSubscriptionOnDemand;
    }

    public List<String> topics() {

	List<String> opL = new ArrayList<>();
	for (Map.Entry<String, OneWayTypeDescription> owon : channel().parentPort().getInterface().oneWayOperations().entrySet()) {
	    opL.add(alias(owon.getKey()));

	}
	for (Map.Entry<String, RequestResponseTypeDescription> rron : channel().parentPort().getInterface().requestResponseOperations().entrySet()) {
	    opL.add(alias(rron.getKey()));

	}
	return opL;
    }

    public String alias(String operationName) {

	for (Iterator<Map.Entry<String, ValueVector>> it = configurationPath().getValue().getFirstChild("osc").children().entrySet().iterator(); it.hasNext();) {
	    Map.Entry<String, ValueVector> i = it.next();
	    if (operationName.equals(i.getKey())) {
		return i.getValue().first().getFirstChild("alias").strValue();
	    }
	}
	return operationName;
    }

    public boolean isOneWay(String operationName) {

	return channel().parentPort().getInterface().oneWayOperations().containsKey(operationName);
    }

    public void startPing(ChannelPipeline p) {
	p.addBefore("Ping", "IdleState", new IdleStateHandler(0, 2, 0));

    }

    public void recAck(Channel ch, CommMessage cm) {
	ch.writeAndFlush(new CommMessage(
		cm.id(),
		cm.operationName(),
		"/",
		Value.create(),
		null));
    }

    public void sendAck(Channel ch, MqttMessageIdVariableHeader vh, MqttMessageType ack) {
	ch.writeAndFlush(new MqttMessage(new MqttFixedHeader((ack.equals(MqttMessageType.PUBREL)) ? MqttMessageType.PUBCOMP : MqttMessageType.PUBREL, false, MqttQoS.AT_MOST_ONCE, false, 0), MqttMessageIdVariableHeader.from(vh.messageId())));
    }

    public boolean send(Channel ch, CommMessage msg) {
	if (isOneWay(msg.operationName())) {
	    ch.writeAndFlush(publishMsg(
		    msg.id(),
		    topic(msg),
		    msg.value(),
		    false,
		    qos(msg.operationName(), MqttQoS.AT_LEAST_ONCE)));
	    if (qos(msg.operationName(), MqttQoS.AT_LEAST_ONCE).equals(MqttQoS.AT_MOST_ONCE)) {
		ch.writeAndFlush(new CommMessage( // qos 0
			msg.id(),
			msg.operationName(),
			"/",
			Value.create(),
			null));
	    }
	} else {
	    ch.writeAndFlush(subscribeMsg(
		    msg.id(),
		    Collections.singletonList(topic(msg)),
		    false,
		    qos(MqttQoS.AT_LEAST_ONCE)));
	}
	return true;
    }

    public void send(Channel ch, MqttConnectMessage msg) {
	ch.writeAndFlush(msg);
    }

    static class Parameters {

	private static final String BROKER = "broker";
	private static final String CONCURRENT = "concurrent";
	private static final String ALIAS = "alias";
	private static final String QOS = "QoS";
	private static final String WILL_TOPIC = "willTopic";
	private static final String WILL_MESSAGE = "willMessage";
	private static final String USERNAME = "username";
	private static final String PASSWORD = "password";
	private static final String FORMAT = "format";
	private static final String ALIAS_RESPONSE = "aliasResponse";
	private static final String SUBSCRIPTION_ON_DEMAND = "onDemand";
	private static final String BOUNDARY = "$"; // MAPPING FOT REQUEST RESPONSE TOPIC
	private static final MqttVersion MQTT_VERSION = MqttVersion.MQTT_3_1_1;

    }

    /*
    PRIVATE METHODS
     */
    private String parseValue(Value value) {
	if (value.isInt()) {
	    return String.valueOf(value.intValue());
	}
	if (value.isBool()) {
	    return String.valueOf(value.boolValue());
	}
	if (value.isDouble()) {
	    return String.valueOf(value.doubleValue());
	}
	if (value.isLong()) {
	    return String.valueOf(value.longValue());
	}
	return value.strValue();
    }

    private Value value(ByteBuf payload) {
	return Value.create(Unpooled.copiedBuffer(payload).toString(CharsetUtil.UTF_8));
    }
}
