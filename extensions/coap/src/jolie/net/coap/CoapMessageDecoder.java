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
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.util.CharsetUtil;

import java.util.List;
import jolie.Interpreter;
import jolie.net.coap.message.CoapMessage;
import jolie.net.coap.message.CoapRequest;
import jolie.net.coap.message.CoapResponse;
import jolie.net.coap.options.EmptyOptionValue;
import jolie.net.coap.message.MessageCode;
import jolie.net.coap.message.MessageType;
import jolie.net.coap.options.OpaqueOptionValue;
import jolie.net.coap.options.OptionValue;
import jolie.net.coap.options.StringOptionValue;
import jolie.net.coap.miscellaneous.Token;
import jolie.net.coap.options.UintOptionValue;

public class CoapMessageDecoder extends MessageToMessageDecoder<ByteBuf> {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in,
	    List<Object> out) throws Exception {

	String message = "";
	System.out.println(Unpooled.wrappedBuffer(in).toString(CharsetUtil.UTF_8));
	if (in.readableBytes() == 4) {
	    CoapMessage msg = recv(in);
	    if (msg != null) {
		out.add(msg);
	    } else {
		message = "Invalid CoapMessage!";
		Interpreter.getInstance().logSevere(message);
	    }
	}
    }

    private CoapMessage recv(ByteBuf in) {

	//Decode the header values
	int encodedHeader = in.readInt();
	int version = (encodedHeader >>> 30) & 0x03;
	int messageType = (encodedHeader >>> 28) & 0x03;
	int tokenLength = (encodedHeader >>> 24) & 0x0F;
	int messageCode = (encodedHeader >>> 16) & 0xFF;
	int messageID = (encodedHeader) & 0xFFFF;

	String errMsg = "";

	//Check whether the protocol version is supported (=1)
	if (version != CoapMessage.PROTOCOL_VERSION) {
	    errMsg = "CoAP version (" + version
		    + ") is other than \"1\"!";
	    Interpreter.getInstance().logSevere(errMsg);
	    return null;
	}

	//Check whether TKL indicates a not allowed token length
	if (tokenLength > CoapMessage.MAX_TOKEN_LENGTH) {
	    errMsg = "TKL value (" + tokenLength
		    + ") is larger than 8!";
	    Interpreter.getInstance().logSevere(errMsg);
	    return null;
	}

	//Check whether there are enough unread bytes left to read the token
	if (in.readableBytes() < tokenLength) {
	    errMsg = "TKL value is " + tokenLength + " but only "
		    + in.readableBytes() + " bytes left!";
	    Interpreter.getInstance().logSevere(errMsg);
	    return null;
	}

	//Handle empty message (ignore everything but the first 4 bytes)
	if (messageCode == MessageCode.EMPTY) {

	    if (messageType == MessageType.ACK) {
		return CoapMessage.createEmptyAcknowledgement(messageID);
	    } else if (messageType == MessageType.RST) {
		return CoapMessage.createEmptyReset(messageID);
	    } else if (messageType == MessageType.CON) {
		return CoapMessage.createPing(messageID);
	    } else {
		//There is no empty NON message defined, so send a RST
		errMsg = "Empty NON messages are invalid!";
		Interpreter.getInstance().logSevere(errMsg);
		return null;
	    }
	}

	//Read the token
	byte[] token = new byte[tokenLength];
	in.readBytes(token);

	//Handle non-empty messages (CON, NON or ACK)
	CoapMessage coapMessage;

	if (MessageCode.isRequest(messageCode)) {
	    coapMessage = new CoapRequest(messageType, messageCode);
	} else {
	    coapMessage = new CoapResponse(messageType, messageCode);
	    coapMessage.setMessageType(messageType);
	}

	coapMessage.setMessageID(messageID);
	coapMessage.setToken(new Token(token));

	//Decode and set the options
	if (in.readableBytes() > 0) {
	    try {
		setOptions(coapMessage, in);
	    } catch (Exception ex) {
		Interpreter.getInstance().logSevere(ex);
	    }
	}

	//The remaining bytes (if any) are the messages payload. 
	//If there is no payload, reader and writer index are
	//at the same position (buf.readableBytes() == 0).
	in.discardReadBytes();

	try {
	    coapMessage.content(in);
	} catch (IllegalArgumentException e) {
	    errMsg = "Message code {} does not "
		    + "allow content. Ignore {} bytes.";
	    Interpreter.getInstance().logSevere(errMsg);
	}

	Interpreter.getInstance().logInfo("Decoded Message: " + coapMessage);

	return coapMessage;
    }

    private void setOptions(CoapMessage coapMessage, ByteBuf bb) {

	//Decode the options
	int previousOptionNumber = 0;
	int firstByte = bb.readByte() & 0xFF;
	String errMsg = "";

	while (firstByte != 0xFF && bb.readableBytes() >= 0) {

	    int optionDelta = (firstByte & 0xF0) >>> 4;
	    int optionLength = firstByte & 0x0F;

	    if (optionDelta == 13) {
		optionDelta += bb.readByte() & 0xFF;
	    } else if (optionDelta == 14) {
		optionDelta = 269 + ((bb.readByte() & 0xFF) << 8)
			+ (bb.readByte() & 0xFF);
	    }

	    if (optionLength == 13) {
		optionLength += bb.readByte() & 0xFF;
	    } else if (optionLength == 14) {
		optionLength = 269 + ((bb.readByte() & 0xFF) << 8)
			+ (bb.readByte() & 0xFF);
	    }

	    int actualOptionNumber = previousOptionNumber + optionDelta;

	    try {
		byte[] optionValue = new byte[optionLength];
		bb.readBytes(optionValue);

		switch (OptionValue.getType(actualOptionNumber)) {
		    case EMPTY: {
			EmptyOptionValue value
				= new EmptyOptionValue(actualOptionNumber);
			coapMessage.addOption(actualOptionNumber, value);
			break;
		    }
		    case OPAQUE: {
			OpaqueOptionValue value
				= new OpaqueOptionValue(actualOptionNumber,
					optionValue);
			coapMessage.addOption(actualOptionNumber, value);
			break;
		    }
		    case STRING: {
			StringOptionValue value
				= new StringOptionValue(actualOptionNumber,
					optionValue, true);
			coapMessage.addOption(actualOptionNumber, value);
			break;
		    }
		    case UINT: {
			UintOptionValue value
				= new UintOptionValue(actualOptionNumber,
					optionValue, true);
			coapMessage.addOption(actualOptionNumber, value);
			break;
		    }
		    default: {
			errMsg = "This should never happen!";
			Interpreter.getInstance().logSevere(errMsg);
			throw new RuntimeException(errMsg);
		    }
		}
	    } catch (IllegalArgumentException e) {
		errMsg = "Exception while decoding option!" + e;
		Interpreter.getInstance().logSevere(errMsg);
	    }

	    previousOptionNumber = actualOptionNumber;

	    if (bb.readableBytes() > 0) {
		firstByte = bb.readByte() & 0xFF;
	    } else {
		firstByte = 0xFF;
	    }

	    Interpreter.getInstance().logInfo(bb.readableBytes()
		    + " readable bytes remaining.");
	}
    }
}
