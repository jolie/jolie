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

import jolie.net.mqtt.InputPortHandler;
import jolie.net.mqtt.OuputPortHandler;
import jolie.net.protocols.AsyncCommProtocol;
import jolie.net.ports.InputPort;

import jolie.js.JsUtils;
import jolie.xml.XmlUtils;

import jolie.runtime.ByteArray;
import jolie.runtime.typing.OperationTypeDescription;
import jolie.runtime.typing.Type;
import jolie.runtime.typing.TypeCheckingException;
import jolie.runtime.VariablePath;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.typing.OneWayTypeDescription;
import jolie.runtime.typing.RequestResponseTypeDescription;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
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
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;

import java.nio.charset.Charset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import jolie.runtime.FaultException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

public class MqttProtocol extends AsyncCommProtocol {

    private Set<String> aliasKeys;
    private Charset charset;
    private final AtomicInteger nextMessageId;

    /**
     * ****************************** PROTOCOL *****************************
     *
     * @param configurationPath
     */
    public MqttProtocol(VariablePath configurationPath) {

	super(configurationPath);
	this.nextMessageId = new AtomicInteger(1);
	this.charset = Charset.forName("UTF8");
	this.aliasKeys = new TreeSet<>();
    }

    @Override
    public void setupPipeline(ChannelPipeline p) {

	p.addLast("LOGGER", new LoggingHandler(LogLevel.INFO));
	p.addLast("ENCODER", MqttEncoder.INSTANCE);
	p.addLast("DECODER", new MqttDecoder());
	p.addLast("PING", new ChannelInboundHandlerAdapter() {
	    @Override
	    public void userEventTriggered(ChannelHandlerContext ctx,
		    Object evt) throws Exception {

		if (evt instanceof IdleStateEvent) {
		    IdleStateEvent event = (IdleStateEvent) evt;
		    switch (event.state()) {
			case READER_IDLE:
			    break;
			case WRITER_IDLE:
			    MqttFixedHeader fixedHeader = new MqttFixedHeader(
				    MqttMessageType.PINGREQ,
				    false,
				    MqttQoS.AT_MOST_ONCE,
				    false,
				    0);
			    ctx.channel().writeAndFlush(
				    new MqttMessage(fixedHeader));
		    }
		}
	    }
	});
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
     * Method handling the incoming publishes from the broker.
     *
     * @param ch - The channel where to write the eventual Comm Message
     * @param mpm - The publish message received.
     */
    public void recPub(Channel ch, MqttPublishMessage mpm) {

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
		    ch.writeAndFlush(new MqttPubAckMessage(fh, vh));
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
		    ch.writeAndFlush(new MqttMessage(fh, vh));
		}
		break;
	}
    }

    public void startPing(ChannelPipeline p) {
	p.addAfter("DECODER", "IDLE_STATE", new IdleStateHandler(0, 2, 0));
    }

    public void stopPing(ChannelPipeline p) {
	p.remove("IDLE_STATE");
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

    public MqttConnectMessage connectMsg() {

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
		false,
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

    /**
     *
     * @param channel
     * @param message
     * @return ChannelFuture
     */
    public ChannelFuture handlePubrec(Channel channel, MqttMessage message) {

	MqttFixedHeader fixedHeader = new MqttFixedHeader(
		MqttMessageType.PUBREL, false, MqttQoS.AT_LEAST_ONCE, false, 0);
	MqttMessageIdVariableHeader variableHeader
		= (MqttMessageIdVariableHeader) message.variableHeader();

	return channel.writeAndFlush(
		new MqttMessage(fixedHeader, variableHeader));
    }

    /**
     *
     * @param channel
     * @param message
     * @return ChannelFuture
     */
    public ChannelFuture handlePubrel(Channel channel, MqttMessage message) {

	MqttFixedHeader fixedHeader
		= new MqttFixedHeader(MqttMessageType.PUBCOMP,
			false, MqttQoS.AT_MOST_ONCE, false, 0);
	MqttMessageIdVariableHeader variableHeader
		= MqttMessageIdVariableHeader.from(
			((MqttMessageIdVariableHeader) message.variableHeader())
				.messageId());

	return channel.writeAndFlush(
		new MqttMessage(fixedHeader, variableHeader));
    }

    /**
     * Add the response topic to the value.
     *
     * @param cm CommMessage
     */
    public void addRespTopicToValue(CommMessage cm) {

	String a = cm.operationName() + "/response";

	if (hasOperationSpecificParameter(cm.operationName(),
		Parameters.ALIAS_RESPONSE)) {
	    a = getOperationSpecificStringParameter(cm.operationName(),
		    Parameters.ALIAS_RESPONSE);
	}

	StringBuilder sb = new StringBuilder();
	sb.append(Parameters.BOUNDARY);
	sb.append(topic(cm, a, false));
	sb.append(Parameters.BOUNDARY);

	cm.value().add(Value.create(sb.toString()));
    }

    /**
     *
     * @param in
     * @return
     * @throws java.lang.Exception
     */
    public MqttPublishMessage send_response(CommMessage in) throws Exception {

	String t = extractTopicResponseFromValue(in);
	ByteBuf bb = valueToByteBuf(in);
	MqttQoS q = qos(in.operationName());

	return publishMsg(t, bb, q);
    }

    /**
     *
     * @param in
     * @return
     * @throws java.lang.Exception
     */
    public Object rec_request(MqttPublishMessage in) throws Exception {

	String on = operation(in.variableHeader().topicName());
	Value v = ByteBufToValue(in);

	return CommMessage.createRequest(on, "/", v);
    }

    /**
     *
     * @param ch
     */
    public void send_subRequest(Channel ch) {

	startPing(ch.pipeline());
	ch.writeAndFlush(subscribeMsg(topics(), qos()));
    }

    public MqttPublishMessage pubOneWayRequest(CommMessage in) throws Exception {

	String a = in.operationName();

	if (hasOperationSpecificParameter(in.operationName(),
		Parameters.ALIAS)) {
	    a = getOperationSpecificStringParameter(in.operationName(),
		    Parameters.ALIAS);
	}

	return publishMsg(topic(in, a, true), valueToByteBuf(in),
		qos(in.operationName()));
    }

    public MqttSubscribeMessage subRequestResponseRequest(CommMessage in) {

	String a = in.operationName() + "/response";

	if (hasOperationSpecificParameter(in.operationName(),
		Parameters.ALIAS_RESPONSE)) {
	    a = getOperationSpecificStringParameter(in.operationName(),
		    Parameters.ALIAS_RESPONSE);
	}

	return subscribeMsg(Collections.singletonList(topic(in, a, false)), qos());
    }

    public MqttPublishMessage pubRequestResponseRequest(CommMessage in)
	    throws Exception {

	String a = in.operationName();

	if (hasOperationSpecificParameter(in.operationName(),
		Parameters.ALIAS)) {
	    a = getOperationSpecificStringParameter(in.operationName(),
		    Parameters.ALIAS);
	}

	addRespTopicToValue(in);

	return publishMsg(topic(in, a, true), valueToByteBuf(in),
		qos(in.operationName()));
    }

    /*
     * ******************************* PRIVATE *******************************
     */
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

    private MqttMessageIdVariableHeader getNewMessageId() {
	nextMessageId.compareAndSet(0xffff, 1);
	return MqttMessageIdVariableHeader.from(
		nextMessageId.getAndIncrement());
    }

    public CommMessage pubRequestResponseResponse(MqttPublishMessage mpm,
	    CommMessage req) throws Exception {
	return CommMessage.createResponse(req, ByteBufToValue(mpm));
    }

    /* 
     * ******************************* PARAM *******************************
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
	private static final String JSON_ENCODING = "json_encoding";
	private static final MqttVersion MQTT_VERSION = MqttVersion.MQTT_3_1_1;

    }

    /*
     * ******************************* PARAM *******************************
     *
     * ******************************* SEND *******************************
     */
    private String extractTopicResponseFromValue(CommMessage in)
	    throws Exception {

	String nn = "id";
	if (hasOperationSpecificParameter(in.operationName(),
		Parameters.ALIAS_RESPONSE)) {
	    nn = getOperationSpecificStringParameter(in.operationName(),
		    Parameters.ALIAS_RESPONSE);
	}
	if (in.value().hasChildren(nn)) {
	    String t = in.value().getFirstChild(nn).strValue();
	    in.value().children().remove(nn);
	    return t;
	} else {
	    throw new FaultException("Topic for response is not present "
		    + "in the value of the comm message with id: " + in.id());
	}
    }

    private ByteBuf valueToByteBuf(CommMessage in) throws Exception {
	/*
	Depending on the format the user specified in the operation parameter
	the value as to be read in order to produce a byte buffer in accordance.
	we focus on 3 cases: xml format, json format or raw format.
	This last kind of format could be a simple string or numeric field, or
	in addition, a more complex tree structure.
	 */
	ByteBuf bb = Unpooled.buffer();
	String format = format(in.operationName());
	if (format.equals("json")) {
	    StringBuilder jsonStringBuilder = new StringBuilder();
	    JsUtils.valueToJsonString(in.value(), true, getSendType(in),
		    jsonStringBuilder);
	    bb = Unpooled.wrappedBuffer(jsonStringBuilder.toString()
		    .getBytes(charset));
	} else {
	    if (format.equals("xml")) {
		DocumentBuilder db = DocumentBuilderFactory.newInstance()
			.newDocumentBuilder();
		Document doc = db.newDocument();
		Element root = doc.createElement(in.operationName());
		doc.appendChild(root);
		XmlUtils.valueToDocument(in.value(), root, doc);
		Source src = new DOMSource(doc);
		ByteArrayOutputStream strm = new ByteArrayOutputStream();
		Result dest = new StreamResult(strm);
		Transformer trf = TransformerFactory.newInstance()
			.newTransformer();
		trf.setOutputProperty(OutputKeys.ENCODING, charset.name());
		trf.transform(src, dest);
		bb = Unpooled.wrappedBuffer(strm.toByteArray());
	    } else {
		if (format.equals("raw")) {
		    writeValue(bb, in.value());
		} else {
		    throw new FaultException("Format " + format + " not "
			    + "supported for operation " + in.operationName());
		}
	    }
	}

	return bb;
    }

    private String format(String operationName) {
	/*
	We suppose in advance that this is a raw format if nothing else
	is specified.
	 */
	return hasOperationSpecificParameter(operationName,
		Parameters.FORMAT)
			? getOperationSpecificStringParameter(operationName,
				Parameters.FORMAT) : "raw";
    }

    private void writeByteArray(ByteBuf out, ByteArray byteArray) {
	out.writeBytes(byteArray.getBytes());
    }

    private void writeString(ByteBuf out, String str) {
	byte[] bytes = str.getBytes(charset);
	out.writeBytes(bytes);
    }

    private void writeValue(ByteBuf out, Value value) {
	Object valueObject = value.valueObject();
	if (valueObject instanceof String) {
	    writeString(out, (String) valueObject);
	} else if (valueObject instanceof Integer) {
	    out.writeInt((Integer) valueObject);
	} else if (valueObject instanceof Double) {
	    out.writeDouble((Double) valueObject);
	} else if (valueObject instanceof ByteArray) {
	    writeByteArray(out, (ByteArray) valueObject);
	} else if (valueObject instanceof Boolean) {
	    out.writeBoolean((Boolean) valueObject);
	} else if (valueObject instanceof Long) {
	    out.writeLong((Long) valueObject);
	}

	Map< String, ValueVector> children = value.children();
	List< Map.Entry< String, ValueVector>> entries
		= new LinkedList<>();
	for (Map.Entry< String, ValueVector> entry : children.entrySet()) {
	    entries.add(entry);
	}

	for (Map.Entry< String, ValueVector> entry : entries) {
	    writeString(out, entry.getKey());
	    for (Value v : entry.getValue()) {
		writeValue(out, v);
	    }
	}
    }

    /*
     * ******************************* SEND *******************************
     *
     * ******************************* REC *******************************
     */
    private Value ByteBufToValue(MqttPublishMessage in) throws Exception {
	String msg = Unpooled.copiedBuffer(in.payload()).toString(charset);
	String tr = null;
	boolean isResponse = false;

	try {
	    tr = msg.substring(msg.indexOf("$") + 1, msg.indexOf("$",
		    msg.indexOf("$") + 1));
	    StringBuilder sb = new StringBuilder();
	    sb.append(msg.substring(0, msg.indexOf("$")));
	    sb.append(msg.substring(msg.indexOf("$", msg.indexOf("$") + 1) + 1,
		    msg.length()));
	    msg = sb.toString();
	    isResponse = true;
	} catch (IndexOutOfBoundsException ex) {
	    // do nothing
	}

	Value v = Value.UNDEFINED_VALUE;
	String on = operation(in.variableHeader().topicName());
	Type type = operationType(on,
		channel().parentPort() instanceof InputPort);

	if (msg.length() > 0) {
	    String format = format(on);
	    if (format.equals("xml")) {
		DocumentBuilderFactory docBuilderFactory
			= DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = docBuilderFactory
			.newDocumentBuilder();
		InputSource src = new InputSource(new StringReader(msg));
		src.setEncoding(charset.name());
		Document doc = builder.parse(src);
		XmlUtils.documentToValue(doc, v);
	    } else {
		if (format.equals("json")) {
		    JsUtils.parseJsonIntoValue(new StringReader(msg), v,
			    checkStringParameter(
				    Parameters.JSON_ENCODING, "strict"));
		} else {
		    if (format.equals("raw")) {
			recv_parseMessage(msg, v, type);
		    } else {
			throw new FaultException("Format " + format
				+ "is not supported. Supported formats are: "
				+ "xml, json and raw");
		    }
		}
	    }
	} else {
	    v = Value.create();
	    try {
		type.check(v);
	    } catch (TypeCheckingException ex1) {
		v = Value.create("");
		try {
		    type.check(v);
		} catch (TypeCheckingException ex2) {
		    v = Value.create(new ByteArray(new byte[0]));
		    try {
			type.check(v);
		    } catch (TypeCheckingException ex3) {
			throw new FaultException("Empty message received but "
				+ "producing type mismatch, expected: "
				+ type);
		    }
		}
	    }
	}
	/*
	if is response that add the topic response in the final value
	at the alias response parameter value for the operation
	 */

	if (isResponse) {
	    if (hasOperationSpecificParameter(on, Parameters.ALIAS_RESPONSE)) {
		String ar = getOperationSpecificStringParameter(on,
			Parameters.ALIAS_RESPONSE);

		Value tmp1 = Value.create();
		tmp1.setValue(tr);
		Value tmp2 = Value.create();
		tmp2.setValue(ar);
		v.add(tmp2);
		v.getChildren(ar).add(tmp1);
	    } else {
		throw new FaultException("No .aliasResponse parameter added "
			+ "for operation " + on);
	    }
	}

	return v;
    }

    private Type operationType(String on, boolean isRequest) {

	OperationTypeDescription otd = channel().parentPort()
		.getOperationTypeDescription(on, "/");
	Type type = isOneWay(on) ? otd.asOneWayTypeDescription().requestType()
		: isRequest
			? otd.asRequestResponseTypeDescription().requestType()
			: otd.asRequestResponseTypeDescription().responseType();
	return type;
    }

    private void recv_parseMessage(String message, Value value, Type type)
	    throws TypeCheckingException {

	if (message.indexOf("\"") == 0 && message.lastIndexOf("\"")
		== message.length() - 1) {
	    value.setValue(message.substring(0, message.length() - 1)
		    .replaceAll("\\\"", "\""));
	} else {
	    if (isNumeric(message)) {
		try {
		    if (message.equals("0")) {
			type.check(Value.create(false));
			value.setValue(false);
		    } else {
			if (message.equals("1")) {
			    type.check(Value.create(true));
			    value.setValue(true);
			} else {
			    throw (new TypeCheckingException("Type "
				    + "checking exception"));
			}
		    }
		} catch (TypeCheckingException e) {
		    try {
			value.setValue(Integer.parseInt(message));
			try {
			    value.setValue(Long.parseLong(message));
			} catch (NumberFormatException nfe2) {
			    try {
				value.setValue(Double.parseDouble(message));
			    } catch (NumberFormatException nfe1) {
				nfe1.printStackTrace();
			    }
			}
		    } catch (NumberFormatException nfe) {
			e.printStackTrace();
		    }
		}
	    } else {
		try {
		    type.check(Value.create(new ByteArray(message.getBytes())));
		    value.setValue(new ByteArray(message.getBytes()));
		} catch (TypeCheckingException e) {
		    value.setValue(message);
		}
	    }
	}
    }

    private boolean isNumeric(final CharSequence cs) {

	if (cs.length() == 0) {
	    return false;
	}
	final int sz = cs.length();
	for (int i = 0; i < sz; i++) {
	    if (!Character.isDigit(cs.charAt(i))) {
		return false;
	    }
	}
	return true;
    }

    /*
     * ******************************* SEND *******************************
     *
     * ******************************* REC *******************************
     */

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


    private MqttQoS qos(String operationName) {

	return hasOperationSpecificParameter(operationName, Parameters.QOS)
		? MqttQoS.valueOf(getOperationSpecificParameterFirstValue(
			operationName, Parameters.QOS).intValue())
		: MqttQoS.AT_LEAST_ONCE;
    }

    private MqttQoS qos() {

	return hasParameter(Parameters.QOS) ? MqttQoS.valueOf(
		getIntParameter(Parameters.QOS)) : MqttQoS.AT_LEAST_ONCE;
    }

    private String topic(CommMessage cm, String alias, boolean removeKeys) {

	String pattern = "%(!)?\\{[^\\}]*\\}";

	// find pattern
	int offset = 0;
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

	if (removeKeys) {
	    for (String aliasKey : aliasKeys) {
		cm.value().children().remove(aliasKey);
	    }
	}

	return result.toString();
    }

    private MqttSubscribeMessage subscribeMsg(List<String> topics,
	    MqttQoS subQos) {

	List<MqttTopicSubscription> tmsL = new ArrayList<>();
	for (String t : topics) {
	    tmsL.add(new MqttTopicSubscription(t, MqttQoS.EXACTLY_ONCE));
	}
	MqttFixedHeader mfh = new MqttFixedHeader(
		MqttMessageType.SUBSCRIBE, false, subQos, false, 0);
	MqttMessageIdVariableHeader vh = getNewMessageId();
	MqttSubscribePayload p = new MqttSubscribePayload(tmsL);

	return new MqttSubscribeMessage(mfh, vh, p);
    }

    private MqttPublishMessage publishMsg(String topic, ByteBuf payload,
	    MqttQoS pubQos) {

	MqttFixedHeader mfh = new MqttFixedHeader(
		MqttMessageType.PUBLISH,
		false,
		pubQos,
		false,
		0);
	MqttPublishVariableHeader vh = new MqttPublishVariableHeader(topic,
		getNewMessageId().messageId());

	return new MqttPublishMessage(mfh, vh, payload);
    }

    private Type getSendType(CommMessage message)
	    throws IOException {
	Type ret = null;

	if (channel().parentPort() == null) {
	    throw new IOException("Could not retrieve communication "
		    + "port for MQTT protocol");
	}

	OperationTypeDescription opDesc = channel().parentPort()
		.getOperationTypeDescription(message.operationName(), "/");

	if (opDesc == null) {
	    return null;
	}

	if (opDesc.asOneWayTypeDescription() != null) {
	    if (message.isFault()) {
		ret = Type.UNDEFINED;
	    } else {
		OneWayTypeDescription ow = opDesc.asOneWayTypeDescription();
		ret = ow.requestType();
	    }
	} else if (opDesc.asRequestResponseTypeDescription() != null) {
	    RequestResponseTypeDescription rr
		    = opDesc.asRequestResponseTypeDescription();
	    if (message.isFault()) {
		ret = rr.getFaultType(message.fault().faultName());
		if (ret == null) {
		    ret = Type.UNDEFINED;
		}
	    } else {
		ret = (channel().parentPort() instanceof InputPort)
			? rr.responseType() : rr.requestType();
	    }
	}

	return ret;
    }
}
