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
import jolie.net.mqtt.OutputPortHandler;
import jolie.net.protocols.AsyncCommProtocol;
import jolie.net.ports.InputPort;
import jolie.net.ports.OutputPort;

import jolie.js.JsUtils;
import jolie.xml.XmlUtils;

import jolie.runtime.ByteArray;
import jolie.runtime.typing.OperationTypeDescription;
import jolie.runtime.typing.Type;
import jolie.runtime.typing.TypeCheckingException;
import jolie.runtime.VariablePath;
import jolie.runtime.Value;
import jolie.runtime.FaultException;
import jolie.runtime.ValueVector;
import jolie.runtime.typing.OneWayTypeDescription;
import jolie.runtime.typing.RequestResponseTypeDescription;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
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
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;

import java.nio.charset.Charset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
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
import org.xml.sax.InputSource;

import jolie.Interpreter;
import jolie.runtime.typing.TypeCastingException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class MqttProtocol extends AsyncCommProtocol {

    private final Set<String> aliasKeys;
    private final Charset charset;
    private final AtomicInteger nextMessageId;
    private String operationResponse;
    private String topicResponse;

    /**
     * ****************************** PROTOCOL *****************************
     *
     * @param configurationPath
     */
    public MqttProtocol(VariablePath configurationPath) {

	super(configurationPath);
	this.nextMessageId = new AtomicInteger(1);
	this.charset = CharsetUtil.UTF_8;
	this.aliasKeys = new TreeSet<>();
    }

    @Override
    public void setupPipeline(ChannelPipeline p) {

//	p.addLast("LOGGER", new LoggingHandler(LogLevel.INFO));
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
	    p.addLast("OUTPUT", new OutputPortHandler(this));
	}
    }

    public void checkDebug(ChannelPipeline p) {
	if (checkBooleanParameter(Parameters.DEBUG) && p.get("DBENCODE") == null) {
	    p.addAfter("DECODER", "DBENCODE", new MessageToMessageEncoder<MqttMessage>() {
		@Override
		protected void encode(ChannelHandlerContext chc, MqttMessage i, List list) throws Exception {
                    String logLine = "";
                    try {
                        logLine = "#" + ((MqttMessageIdVariableHeader) i.variableHeader()).messageId() + " ";
                    } catch (Exception e ){}
		    MqttMessageType t = i.fixedHeader().messageType();
		    logLine += t + " ->";
		    if (t.equals(MqttMessageType.PUBLISH)) {
			logLine += "\t topic: " + ((MqttPublishMessage) i).variableHeader().topicName();
		    }
		    if (t.equals(MqttMessageType.SUBSCRIBE)) {
			logLine += "\t topics: ";
			for (MqttTopicSubscription topic : ((MqttSubscribeMessage) i).payload().topicSubscriptions()) {
			    logLine += topic.topicName() + ", ";
			}
                        logLine = logLine.substring( 0, logLine.length()-2 ); // removes the trailing ", "
		    }

		    if (!(t.equals(MqttMessageType.PINGRESP) || t.equals(MqttMessageType.PINGREQ))) {
			Interpreter.getInstance().logInfo(logLine);
		    }

		    if (channel().parentPort() instanceof OutputPort && t.equals(MqttMessageType.PUBLISH)) {
			chc.write(i);
			chc.flush();
		    } else {
			if (channel().parentPort() instanceof InputPort && t.equals(MqttMessageType.PUBLISH)) {
			    ((MqttPublishMessage) i).retain();
			}
			list.add(i);
		    }
		}
	    });
	    p.addAfter("DBENCODE", "DBDECODE", new MessageToMessageDecoder<MqttMessage>() {
		@Override
		protected void decode(ChannelHandlerContext chc, MqttMessage i, List<Object> list) throws Exception {
		    String logLine = "";
                    try {
                        logLine = "#" + ((MqttMessageIdVariableHeader) i.variableHeader()).messageId() + " ";
                    } catch (Exception e ){}
                    MqttMessageType t = i.fixedHeader().messageType();
		    if (!(t.equals(MqttMessageType.PINGRESP) || t.equals(MqttMessageType.PINGREQ))) {
			logLine += " <- " + t;
			if (t.equals(MqttMessageType.PUBLISH)) {
			    logLine += "\t  " + ((MqttPublishMessage) i).variableHeader().topicName();
			}
			Interpreter.getInstance().logInfo(logLine);
		    }
		    if (t.equals(MqttMessageType.PUBLISH)) {
			((MqttPublishMessage) i).retain();
		    }
		    list.add(i);
		}
	    });
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
    public void recv_pub(Channel ch, MqttPublishMessage mpm) {

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
     * @param cm
     * @param toBeChecked MqttQoS
     * @return boolean - True if the QoS is the same, False otherwise.
     */
    public boolean checkQoS(CommMessage cm, MqttQoS toBeChecked) {

	return qos(cm.operationName()).equals(toBeChecked);

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
     * Returns the response topic.
     *
     * @param cm CommMessage
     * @return the topic response
     */
    public String getRespTopic(CommMessage cm) {

	// bookkeeping variables for topic-to-operation correlation
	operationResponse = cm.operationName();
	topicResponse = cm.operationName() + "/response";

	if (hasOperationSpecificParameter(cm.operationName(),
		Parameters.ALIAS_RESPONSE)) {
	    topicResponse = getOperationSpecificStringParameter(cm.operationName(),
		    Parameters.ALIAS_RESPONSE);
	}

	return Parameters.BOUNDARY + topic(cm, topicResponse, true) + Parameters.BOUNDARY;

    }

    /**
     *
     * @param in
     * @return
     * @throws java.lang.Exception
     */
    public MqttPublishMessage send_response(CommMessage in) throws Exception {

	String t = extractTopicResponseFromValue(in);
	ByteBuf bb = Unpooled.copiedBuffer(valueToByteBuf(in));
	MqttQoS q = qos(in.operationName());

	return publishMsg( t, bb, q, (int) in.id() );
    }

    /**
     *
     * @param in
     * @return
     * @throws java.lang.Exception
     */
    public CommMessage recv_request(MqttPublishMessage in) throws Exception {

	String on = operation(in.variableHeader().topicName());
	Value v = byteBufToValue(on, in);

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

	return publishMsg( topic(in, a, true), valueToByteBuf(in), qos( in.operationName() ), (int) in.id() );
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

	return publishMsg( topic( in, a, false ), valueToByteBuf( in ), qos( in.operationName() ), (int) in.id() );
    }

    /*
     * ******************************* PRIVATE *******************************
     */
    private String operation(String topic) {

	if (channel().parentPort() instanceof OutputPort) {
	    if (topic.equals(topicResponse)) {
		return operationResponse;
	    } else {
		return topic;
	    }
	} else {
	    if (configurationPath().getValue().hasChildren("osc")) {
		for (Map.Entry<String, ValueVector> i : configurationPath()
			.getValue()
			.getFirstChild("osc").children().entrySet()) {
		    for (Map.Entry<String, ValueVector> j : i.getValue().first()
			    .children().entrySet()) {
			if (j.getKey().equals("alias") && j.getValue().first()
				.strValue().equals(topic)) {
			    return i.getKey();
			}
		    }
		}
	    }
	    // else we return directly the topic
	    return topic;
	}
    }

    private MqttMessageIdVariableHeader getNewMessageId() {
	nextMessageId.compareAndSet(0xffff, 1);
	return MqttMessageIdVariableHeader.from(
		nextMessageId.getAndIncrement());
    }

    public CommMessage recv_pubReqResp(MqttPublishMessage mpm,
	    CommMessage req) throws Exception {

	return new CommMessage(CommMessage.GENERIC_ID,
		req.operationName(), "/", byteBufToValue(req.operationName(), mpm.retain()), null);
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
	private static final String DEBUG = "debug";
	private static final MqttVersion MQTT_VERSION = MqttVersion.MQTT_3_1_1;

    }

    /*
     * ******************************* PARAM *******************************
     *
     * ******************************* SEND *******************************
     */
    private String extractTopicResponseFromValue(CommMessage in)
	    throws Exception {

	String ar = "aliasResponse";
	if (hasOperationSpecificParameter(in.operationName(),
		Parameters.ALIAS_RESPONSE)) {
	    ar = getOperationSpecificStringParameter(in.operationName(),
		    Parameters.ALIAS_RESPONSE);
	}

	String t = in.value().getFirstChild(ar).strValue();
	in.value().children().remove(ar);

	return t;
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
	String message;
	String topicResponsePrefix = "";
	if (!isOneWay(operationResponse) && channel().parentPort() instanceof OutputPort) {
	    topicResponsePrefix = getRespTopic(in);
	}
	switch (format) {
	    case "json":
		StringBuilder jsonStringBuilder = new StringBuilder();
		JsUtils.valueToJsonString(in.value(), true, getSendType(in),
			jsonStringBuilder);
		message = jsonStringBuilder.toString();
		break;
	    case "xml":
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
		message = strm.toString();
		break;
	    case "raw":
		message = valueToRaw(in.value());
		break;
	    default:
		throw new FaultException("Format " + format + " not "
			+ "supported for operation " + in.operationName());
	}
	message = topicResponsePrefix + message;
	if (checkBooleanParameter(Parameters.DEBUG)) {
	    Interpreter.getInstance().logInfo("Sending " + format.toUpperCase() + " message: " + message);
	}
	bb.writeBytes(message.getBytes(charset));
	return bb;
    }

    private String format(String operationName) {
	/*
	We suppose in advance that raw format stands if nothing else
	is specified.
	 */
	return hasOperationSpecificParameter(operationName,
		Parameters.FORMAT)
			? getOperationSpecificStringParameter(operationName,
				Parameters.FORMAT) : "raw";
    }

    private String valueToRaw(Value value) {
	// TODO handle bytearray
	Object valueObject = value.valueObject();
	String str = "";
	if (valueObject instanceof String) {
	    str = ((String) valueObject);
	} else if (valueObject instanceof Integer) {
	    str = ((Integer) valueObject).toString();
	} else if (valueObject instanceof Double) {
	    str = ((Double) valueObject).toString();
	} else if (valueObject instanceof ByteArray) {
	    str = ((ByteArray) valueObject).toString();
	} else if (valueObject instanceof Boolean) {
	    str = ((Boolean) valueObject).toString();
	} else if (valueObject instanceof Long) {
	    str = ((Long) valueObject).toString();
	}

	return str;
    }

    /*
     * ******************************* SEND *******************************
     *
     * ******************************* REC *******************************
     */
    private Value byteBufToValue(String operationName, MqttPublishMessage in) throws Exception {
	String msg = Unpooled.wrappedBuffer(in.payload()).toString(charset);
	if (checkBooleanParameter(Parameters.DEBUG)) {
	    Interpreter.getInstance().logInfo("Received message: " + msg);
	}
	String topicResp = null;
	String aliasResp = "aliasResponse";

	if (channel().parentPort() instanceof InputPort && !isOneWay(operationName)) {
	    try {
		if (hasOperationSpecificParameter(operationName, Parameters.ALIAS_RESPONSE)) {
		    aliasResp = getOperationSpecificStringParameter(operationName,
			    Parameters.ALIAS_RESPONSE);
		}
		if (msg.indexOf(Parameters.BOUNDARY) == 0 && msg.indexOf(Parameters.BOUNDARY, 1) > 0) {
		    topicResp = msg.substring(1, msg.indexOf(Parameters.BOUNDARY, 1));
		}
		msg = msg.substring(msg.indexOf(Parameters.BOUNDARY, 1) + 1, msg.length());
	    } catch (IndexOutOfBoundsException ex) {
	    }
	}

	Value v = Value.UNDEFINED_VALUE;

	Type type = operationType(operationName, channel().parentPort() instanceof InputPort);

	if (msg.length() > 0) {
	    String format = format(operationName);
	    switch (format) {
		case "xml":
		    DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		    DocumentBuilder builder = docBuilderFactory
			    .newDocumentBuilder();
		    InputSource src = new InputSource(new ByteBufInputStream(Unpooled.wrappedBuffer(msg.getBytes())));
		    src.setEncoding(charset.name());
		    Document doc = builder.parse(src);
		    XmlUtils.documentToValue(doc, v);
		    break;
		case "json":
		    JsUtils.parseJsonIntoValue(new StringReader(msg), v,
			    checkStringParameter(Parameters.JSON_ENCODING, "strict"));
		    break;
		case "raw":
		    recv_parseMessage(msg, v, type);
		    break;
		default:
		    throw new FaultException("Format " + format
			    + "is not supported. Supported formats are: "
			    + "xml, json and raw");
	    }
	    addTopicResponse(v, aliasResp, topicResp);
	    // for XML format
	    try {
		v = type.cast(v);
	    } catch (TypeCastingException e) {
	    }
	} else {
	    v = addTopicResponse(Value.create(), aliasResp, topicResp);
	    try {
		type.check(v);
	    } catch (TypeCheckingException ex1) {
		v = addTopicResponse(Value.create(""), aliasResp, topicResp);
		try {
		    type.check(v);
		} catch (TypeCheckingException ex2) {
		    v = addTopicResponse(Value.create(new ByteArray(new byte[0])), aliasResp, topicResp);
		    try {
			type.check(v);
		    } catch (TypeCheckingException ex3) {
			v = addTopicResponse(Value.create(), aliasResp, topicResp);
		    }
		}
	    }
	}

	return v;
    }

    private Value addTopicResponse(Value v, String aliasResp, String topicResp) {
	if (topicResp != null) {
	    v.setFirstChild(aliasResp, topicResp);
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

	try {
	    type.check(Value.create(message));
	    value.setValue(message);
	} catch (TypeCheckingException e1) {
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
			    throw new TypeCheckingException("");
			}
		    }
		} catch (TypeCheckingException e) {
		    try {
			value.setValue(Integer.parseInt(message));
		    } catch (NumberFormatException nfe) {
			try {
			    value.setValue(Long.parseLong(message));
			} catch (NumberFormatException nfe1) {
			    try {
				value.setValue(Double.parseDouble(message));
			    } catch (NumberFormatException nfe2) {
			    }
			}
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
	    MqttQoS pubQos, int messageID) {

	MqttFixedHeader mfh = new MqttFixedHeader(
		MqttMessageType.PUBLISH,
		false,
		pubQos,
		false,
		0);
	MqttPublishVariableHeader vh = new MqttPublishVariableHeader(topic,
            //getNewMessageId().messageId()
            messageID
        );

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
