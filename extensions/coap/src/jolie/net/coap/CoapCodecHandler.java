/*
 *   Copyright (C) 2017 by Stefano Pio Zingaro <stefanopio.zingaro@unibo.it>  
 *   Copyright (C) 2017 by Saverio Giallorenzo <saverio.giallorenzo@gmail.com>
 *                                                                             
 *   This program is free software; you can redistribute it and/or modify      
 *   it under the terms of the GNU Library General Public License as           
 *   published by the Free Software Foundation; either version 2 of the        
 *   License, or (at your option) any later version.                           
 *                                                                             
 *   This program is distributed in the hope that it will be useful,           
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of            
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             
 *   GNU General Public License for more details.                              
 *                                                                             
 *   You should have received a copy of the GNU Library General Public         
 *   License along with this program; if not, write to the                     
 *   Free Software Foundation, Inc.,                                           
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.                 
 *                                                                             
 *   For details about the authors of this software, see the AUTHORS file.     
 */
package jolie.net.coap;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.util.CharsetUtil;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import jolie.Interpreter;
import jolie.js.JsUtils;

import jolie.net.CoapProtocol;
import jolie.net.CommCore;
import jolie.net.CommMessage;
import jolie.net.NioDatagramCommChannel;
import jolie.net.coap.message.CoapMessage;
import jolie.net.coap.message.MessageCode;
import jolie.net.coap.message.MessageType;
import jolie.runtime.ByteArray;

import jolie.runtime.FaultException;
import jolie.runtime.Value;

import jolie.xml.XmlUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class CoapCodecHandler
	extends MessageToMessageCodec<CoapMessage, CommMessage> {

    private boolean input;
    private static final Charset charset = CharsetUtil.UTF_8;
    private final CoapProtocol protocol;
    private Channel cc;
    private Map<String, Integer> allowedMethods;
    private CommMessage commMessageRequest;

    public CoapCodecHandler(CoapProtocol prt) {
	this.allowedMethods = new HashMap<>();
	this.protocol = prt;
	this.input = prt.isInput;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
	cc = ctx.channel();
	((CommCore.ExecutionContextThread) Thread.currentThread())
		.executionThread(cc
			.attr(NioDatagramCommChannel.EXECUTION_CONTEXT).get());
    }

    @Override
    protected void encode(ChannelHandlerContext ctx,
	    CommMessage in, List<Object> out) throws Exception {

	String operationName = in.operationName();
	if (input) { // input port - RR

	} else { // output port - OW and RR

	    // CREATE COAP MESSAGE 
	    int messageType = getMessageType(operationName);
	    int messageCode = getMessageCode(operationName);
	    CoapMessage msg = new CoapMessage(messageType, messageCode) {
	    };
	    if (MessageCode.allowsContent(messageCode)) {
		ByteBuf payload = valueToByteBuf(in);
		msg.setContent(payload);
	    } else {
		Interpreter.getInstance().logSevere("Method do not "
			+ "allow content!");
	    }
	    out.add(msg);

	    if (messageType == MessageType.NON) {
		sendAck(ctx, in);
	    }

	    this.commMessageRequest = in;
	}
    }

    @Override
    protected void decode(ChannelHandlerContext ctx,
	    CoapMessage in, List<Object> out) throws Exception {
	if (input) { // input port - OW and RR

	    if (in.getMessageType() == MessageType.CON) { // send back ack
		ctx.channel().writeAndFlush(CoapMessage
			.createEmptyAcknowledgement(in.getMessageID()));
	    }

	    String operationName = getOperationName(in);
	    CommMessage msg = CommMessage.createRequest(operationName, "/",
		    byteBufToValue(in));
	    out.add(msg);

	} else { // output port - OW and RR (confirmable)

	    if (in.getMessageType() == MessageType.ACK) {
		if (this.commMessageRequest != null) {
		    out.add(CommMessage
			    .createEmptyResponse(commMessageRequest));
		} else { // should never happen
		    Interpreter.getInstance().logSevere("No Comm Message "
			    + "Waiting for Acknowledgement");
		}
	    }
	}
    }

    private ByteBuf valueToByteBuf(CommMessage in) throws Exception {
	ByteBuf bb = Unpooled.buffer();
	String format = format(in.operationName());
	String message;
	Value v = in.isFault() ? Value.create(in.fault().getMessage())
		: in.value();
	switch (format) {
	    case "json":
		StringBuilder jsonStringBuilder = new StringBuilder();
		JsUtils.valueToJsonString(v, true, protocol.getSendType(in),
			jsonStringBuilder);
		message = jsonStringBuilder.toString();
		break;
	    case "xml":
		DocumentBuilder db = DocumentBuilderFactory.newInstance()
			.newDocumentBuilder();
		Document doc = db.newDocument();
		Element root = doc.createElement(in.operationName());
		doc.appendChild(root);
		XmlUtils.valueToDocument(v, root, doc);
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
		message = valueToRaw(v);
		break;
	    default:
		throw new FaultException("Format " + format + " not "
			+ "supported for operation " + in.operationName());
	}

	if (protocol.checkBooleanParameter(Parameters.DEBUG)) {
	    Interpreter.getInstance().logInfo("Sending "
		    + format.toUpperCase() + " message: " + message);
	}
	bb.writeBytes(message.getBytes(charset));
	return bb;
    }

    private String format(String operationName) {
	return protocol.hasOperationSpecificParameter(operationName,
		Parameters.FORMAT)
			? protocol.getOperationSpecificStringParameter(
				operationName,
				Parameters.FORMAT) : "raw";
    }

    private String valueToRaw(Value value) {
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

    private void sendAck(ChannelHandlerContext ctx, CommMessage in) {
	ctx.pipeline().fireChannelRead(new CommMessage(
		in.id(), in.operationName(), "/", Value.create(), null));
    }

    private int getMessageType(String operationName) {
	if (protocol.hasOperationSpecificParameter(operationName,
		Parameters.CONFIRMABLE)) {
	    if (protocol.getOperationSpecificParameterFirstValue(operationName,
		    Parameters.CONFIRMABLE).boolValue()) {
		return MessageType.CON;
	    }
	}
	return MessageType.NON;
    }

    private int getMessageCode(String operationName) {
	if (protocol.hasOperationSpecificParameter(operationName,
		Parameters.METHOD)) {
	    String method = protocol
		    .getOperationSpecificParameterFirstValue(operationName,
			    Parameters.METHOD).strValue();
	    if (allowedMethods.containsKey(method)) {
		return allowedMethods.get(method);
	    }
	}
	return MessageCode.POST;
    }

    private Value byteBufToValue(CoapMessage in) {
	throw new UnsupportedOperationException("Not supported yet.");
    }

    private String getOperationName(CoapMessage in) {
	throw new UnsupportedOperationException("Not supported yet.");
    }

    private static class Parameters {

	private static final String DEBUG = "debug";
	private static final String FORMAT = "format";
	private static final String CONFIRMABLE = "confirmable";
	private static final String METHOD = "method";

    }
}
