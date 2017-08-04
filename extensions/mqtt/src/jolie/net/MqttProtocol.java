/*
 * The MIT License
 *
 * Copyright 2017 Stefano Pio Zingaro <stefanopio.zingaro@unibo.it>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package jolie.net;

import jolie.net.protocols.AsyncCommProtocol;
import jolie.net.ports.InputPort;
import jolie.net.mqtt.InputPortHandler;
import jolie.net.mqtt.OuputPortHandler;
import jolie.net.mqtt.PingHandler;
import jolie.runtime.VariablePath;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.typing.OneWayTypeDescription;
import jolie.runtime.typing.RequestResponseTypeDescription;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectPayload;
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
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jolie.runtime.ValuePrettyPrinter;

public class MqttProtocol extends AsyncCommProtocol {

    private String topicResponse;

    /**
     *
     * @return String
     */
    public String getTopicResponse() {
	return topicResponse;
    }

    /**
     * ****************************** PROTOCOL *****************************
     *
     * @param configurationPath
     */
    public MqttProtocol(VariablePath configurationPath) {

	super(configurationPath);
    }

    @Override
    public void setupPipeline(ChannelPipeline p) {

	//p.addLast("LOGGER", new LoggingHandler(LogLevel.INFO));
	p.addLast("ENCODER", MqttEncoder.INSTANCE);
	p.addLast("DECODER", new MqttDecoder());
	p.addLast("PING", new PingHandler(2));
	if (channel().parentPort() instanceof InputPort) {
	    p.addLast("INPUT", new InputPortHandler(this));
	} else {
	    p.addLast("OUTPUT", new OuputPortHandler(this));
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

    /**
     * ******************************** PUBLIC ********************************
     *
     * @param operationName
     * @return boolean
     */
    public boolean isOneWay(String operationName) {

	return channel().parentPort().getInterface().oneWayOperations()
		.containsKey(operationName);
    }

    /**
     *
     * @param cm
     * @throws IOException
     */
    public void prettyPrintCommMessage(CommMessage cm) throws IOException {
	System.out.println("CommMessage with id " + cm.id() + " for operation " + cm.operationName() + " has arrived, here we have the value:");
	Writer writer = new StringWriter();
	ValuePrettyPrinter printer = new ValuePrettyPrinter(cm.value(), writer, "Value");
	printer.run();
	System.out.println(writer.toString());
    }

    /**
     *
     * Invoke the client connection message to the borker method.
     *
     * @return MqttConnectMessage
     */
    public MqttConnectMessage connectMsg() {

	return connectMsg(false);
    }

    /**
     * Invoke the publish message builder method.
     *
     * @param messageId
     * @param topic
     * @param message
     * @param pubQos
     * @return MqttPublishMessage
     */
    public MqttPublishMessage publishMsg(long messageId, String topic,
	    Value message, MqttQoS pubQos) {

	return publishMsg(messageId, topic, message, false, pubQos);
    }

    /**
     * Invoke the subscription message builder method.
     *
     * @param messageId
     * @param topics - the list of topics to subscribe at startup.
     * @param subQos - the QoS for the subscription.
     * @return MqttSubscribeMessage - The message for subscription.
     */
    public MqttSubscribeMessage subscribeMsg(long messageId,
	    List<String> topics, MqttQoS subQos) {

	return subscribeMsg(messageId, topics, false, subQos);
    }

    /**
     * Method handling the incoming publishes from the broker.
     *
     * @param channel - The channel where to write the eventual Comm Message
     * @param mpm - The publish message received.
     */
    public void recPub(Channel channel, MqttPublishMessage mpm) {

	switch (mpm.fixedHeader().qosLevel()) {
	    case AT_MOST_ONCE:
		break;
	    case AT_LEAST_ONCE:
		if (mpm.variableHeader().messageId() != -1) {
		    MqttFixedHeader fh = new MqttFixedHeader(
			    MqttMessageType.PUBACK,
			    false,
			    MqttQoS.AT_MOST_ONCE,
			    false,
			    0);
		    MqttMessageIdVariableHeader vh
			    = MqttMessageIdVariableHeader.from(
				    mpm.variableHeader().messageId());
		    channel.writeAndFlush(new MqttPubAckMessage(fh, vh));
		}
		break;
	    case EXACTLY_ONCE:
		if (mpm.variableHeader().messageId() != -1) {
		    MqttFixedHeader fh = new MqttFixedHeader(
			    MqttMessageType.PUBREC,
			    false,
			    MqttQoS.AT_MOST_ONCE,
			    false,
			    0);
		    MqttMessageIdVariableHeader vh
			    = MqttMessageIdVariableHeader.from(
				    mpm.variableHeader().messageId());
		    channel.writeAndFlush(new MqttMessage(fh, vh));
		}
		break;
	}
    }

    /**
     * Invoke the method for retrieving the specific topic in case of a request
     * comm message.
     *
     * @param cm CommMessage
     * @return String - the topic.
     */
    public String topic_one_way(CommMessage cm) {

	return topic(cm, true);
    }

    /**
     * Inovke the method for retrieving the specific topic in case of a response
     * comm message.
     *
     * @param cm CommMessage
     * @return String - The topic.
     */
    public String topic_request_response(CommMessage cm) {

	return topic(cm, false);
    }

    /**
     * Invoke the method for retrieving the general QoS for the port.
     *
     * @return MqttQoS - The Quality of Service.
     */
    public MqttQoS qos() {

	return qos(MqttQoS.AT_LEAST_ONCE);
    }

    /**
     * Invoke the method retrieving the specific operation QoS.
     *
     * @param operationName
     * @return MqttQoS - The Quality of Service.
     */
    public MqttQoS qos(String operationName) {

	return qos(operationName, MqttQoS.AT_LEAST_ONCE);
    }

    /**
     * Check if the given incoming Mqtt Message has the related requested QoS.
     *
     * @param mm MqttMessage
     * @param toBeChecked MqttQoS
     * @return boolean - True if the QoS is the same, False otherwise.
     */
    public boolean checkQoS(MqttMessage mm, MqttQoS toBeChecked) {

	if (mm instanceof MqttPublishMessage) {
	    MqttPublishMessage mpm = (MqttPublishMessage) mm;
	    return qos(operation(mpm.variableHeader().topicName()))
		    .equals(toBeChecked);
	} else {
	    return qos().equals(toBeChecked);
	}
    }

    /**
     *
     * @return List of String - The list of topics for port interface
     */
    public List<String> topics() {

	List<String> opL = new ArrayList<>();
	for (Map.Entry<String, OneWayTypeDescription> owon
		: channel().parentPort().getInterface().oneWayOperations()
			.entrySet()) {
	    opL.add(alias(owon.getKey()));
	}
	for (Map.Entry<String, RequestResponseTypeDescription> rron
		: channel().parentPort().getInterface()
			.requestResponseOperations()
			.entrySet()) {
	    opL.add(alias(rron.getKey()));
	}
	return opL;
    }

    /**
     *
     * @param p ChannelPipeline
     * @return ChannelPipeline
     */
    public ChannelPipeline startPing(ChannelPipeline p) {

	return p.addAfter("DECODER", "IDLE_STATE",
		new IdleStateHandler(0, 2, 0));
    }

    /**
     *
     * @param ch Channel
     * @param cm CommMessage
     * @return ChannelFuture
     */
    public ChannelFuture recAck(Channel ch, CommMessage cm) {

	if (isOneWay(cm.operationName())) {

	    return ch.writeAndFlush(new CommMessage(
		    cm.id(),
		    cm.operationName(),
		    "/",
		    Value.create(),
		    null));
	}
	return null;
    }

    /**
     *
     * @param ch Channel
     * @param vh MqttMessageIdVariableHeader
     * @param ack MqttMessageType
     * @return ChannelFuture
     */
    public ChannelFuture sendAck(Channel ch, MqttMessageIdVariableHeader vh,
	    MqttMessageType ack) {

	return ch.writeAndFlush(new MqttMessage(
		new MqttFixedHeader((ack.equals(MqttMessageType.PUBREL))
			? MqttMessageType.PUBCOMP
			: MqttMessageType.PUBREL,
			false,
			MqttQoS.AT_MOST_ONCE, false, 0),
		MqttMessageIdVariableHeader.from(vh.messageId())));
    }

    /**
     * Add the response topic to the value.
     *
     * @param commMessage CommMessage
     * @return Value - The new value with the topic for resonse added.
     */
    public Value responseValue(CommMessage commMessage) {

	commMessage.value().add(Value.create(Parameters.BOUNDARY
		+ topic_request_response(commMessage)
		+ Parameters.BOUNDARY));

	return commMessage.value();
    }

    /**
     *
     * @param mpm MqttPublishMessage
     * @return CommMessage
     */
    public CommMessage requestCommMsg(MqttPublishMessage mpm) {
	CommMessage cmReq = CommMessage.createRequest(
		operation(mpm.variableHeader().topicName()),
		"/",
		value(mpm.payload()));
	return cmReq;
    }

    /**
     *
     * @param request CommMessage
     * @param mpm MqttPublishMessage
     * @return CommMessage
     */
    public CommMessage responseCommMsg(CommMessage request, MqttPublishMessage mpm) {

	CommMessage cmResp = new CommMessage(
		CommMessage.GENERIC_ID,
		operation(mpm.variableHeader().topicName()),
		"/",
		value(mpm.payload()),
		null);

	return cmResp;
    }

    /**
     * ******************************* PRIVATE *******************************
     */
    private static class Parameters {

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
	private static final String BOUNDARY = "$";
	private static final MqttVersion MQTT_VERSION = MqttVersion.MQTT_3_1_1;

    }

    private ByteBuf parseValue(Value value) {

	String str = value.strValue();
	if (value.isInt()) {
	    str = String.valueOf(value.intValue());
	}
	if (value.isBool()) {
	    str = String.valueOf(value.boolValue());
	}
	if (value.isDouble()) {
	    str = String.valueOf(value.doubleValue());
	}
	if (value.isLong()) {
	    str = String.valueOf(value.longValue());
	}
	return Unpooled.copiedBuffer(str.getBytes(CharsetUtil.UTF_8));
    }

    private Value value(ByteBuf payload) {

	String str = Unpooled.copiedBuffer(payload).toString(CharsetUtil.UTF_8);
	Matcher m = Pattern.compile("\\$(.*?)\\$").matcher(str);

	if (m.find()) {
	    topicResponse = m.group(1);
	    str = str.replace(str.substring(m.start(), m.end()), "");
	}

	try {
	    int intRes = Integer.parseInt(str);
	    return Value.create(intRes);
	} catch (NumberFormatException nfe) {
	    return Value.create(str);
	}
    }

    private String alias(String operationName) {

	for (Iterator<Map.Entry<String, ValueVector>> it = configurationPath()
		.getValue().getFirstChild("osc").children().entrySet()
		.iterator();
		it.hasNext();) {
	    Map.Entry<String, ValueVector> i = it.next();
	    if (operationName.equals(i.getKey())) {
		return i.getValue().first().getFirstChild("alias").strValue();
	    }
	}
	return operationName;
    }

    private String operation(String topic) {

	if (configurationPath().getValue().hasChildren("osc")) {
	    for (Map.Entry<String, ValueVector> i : configurationPath()
		    .getValue()
		    .getFirstChild("osc").children().entrySet()) {
		for (Map.Entry<String, ValueVector> j : i.getValue().first()
			.children().entrySet()) {
		    if (j.getKey().equals("alias") && j.getValue().first()
			    .strValue().equals(topic)) {
			return i.getKey();
		    } else if (j.getKey().equals("aliasResponse") && j.getValue().first()
			    .strValue().equals(topic)) {
			return i.getKey();
		    }
		}
	    }
	}

	String result = topic;
	if (result.contains("/response")) {
	    result = topic.substring(0, topic.indexOf("response") - 1);
	}
	return result;
    }

    private MqttQoS qos(String operationName, MqttQoS defaultQoS) {

	return hasOperationSpecificParameter(operationName, Parameters.QOS)
		? MqttQoS.valueOf(getOperationSpecificParameterFirstValue(
			operationName, Parameters.QOS).intValue()) : defaultQoS;
    }

    private MqttQoS qos(MqttQoS defaultQoS) {

	return hasParameter(Parameters.QOS) ? MqttQoS.valueOf(
		getIntParameter(Parameters.QOS)) : defaultQoS;
    }

    private String topic(CommMessage cm, boolean request) {

	String alias;
	String pattern = "%(!)?\\{[^\\}]*\\}";

	// build alias
	if (request) {
	    if (hasOperationSpecificParameter(cm.operationName(),
		    Parameters.ALIAS)) {
		alias = getOperationSpecificStringParameter(cm.operationName(),
			Parameters.ALIAS);
	    } else {
		return cm.operationName();
	    }
	} else {
	    if (hasOperationSpecificParameter(cm.operationName(),
		    Parameters.ALIAS_RESPONSE)) {
		alias = getOperationSpecificStringParameter(cm.operationName(),
			Parameters.ALIAS_RESPONSE);
	    } else {
		return cm.operationName() + "/response";
	    }
	}

	// find pattern
	int offset = 0;
	List< String> aliasKeys = new ArrayList<>();
	String currStrValue;
	String currKey;
	StringBuilder result = new StringBuilder(alias);
	Matcher m = Pattern.compile(pattern).matcher(alias);

	// substitute in alias
	while (m.find()) {
	    currKey = alias.substring(m.start() + 3, m.end() - 1);
	    currStrValue = cm.value().getFirstChild(currKey).strValue();
	    aliasKeys.add(currKey);
	    result.replace(
		    m.start() + offset, m.end() + offset,
		    currStrValue
	    );
	    offset += currStrValue.length() - 3 - currKey.length();
	}

	// remove from value
	for (String aliasKey : aliasKeys) {
	    cm.value().children().remove(aliasKey);
	}

	return result.toString();
    }

    private MqttSubscribeMessage subscribeMsg(
	    long messageId,
	    List<String> topics,
	    boolean isDup,
	    MqttQoS subQos) {

	List<MqttTopicSubscription> tmsL = new ArrayList<>();
	for (String t : topics) {
	    tmsL.add(new MqttTopicSubscription(t,
		    MqttProtocol.this.qos(operation(t),
			    MqttQoS.EXACTLY_ONCE)));
	}
	MqttFixedHeader mfh = new MqttFixedHeader(
		MqttMessageType.SUBSCRIBE,
		isDup,
		subQos,
		false,
		0);
	MqttMessageIdVariableHeader vh
		= MqttMessageIdVariableHeader.from((int) messageId);
	MqttSubscribePayload p = new MqttSubscribePayload(tmsL);

	return new MqttSubscribeMessage(mfh, vh, p);
    }

    private MqttPublishMessage publishMsg(long messageId, String topic,
	    Value message, boolean isDup, MqttQoS pubQos) {
	MqttFixedHeader mfh = new MqttFixedHeader(
		MqttMessageType.PUBLISH,
		isDup,
		pubQos,
		false,
		0);
	MqttPublishVariableHeader vh = new MqttPublishVariableHeader(topic,
		(int) messageId);

	return new MqttPublishMessage(mfh, vh, parseValue(message));
    }

    private MqttConnectMessage connectMsg(boolean isDup) {

	Random random = new Random();
	String clientId = "jolie-mqtt/";
	String[] options
		= ("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456"
			+ "789").split("");
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
}
