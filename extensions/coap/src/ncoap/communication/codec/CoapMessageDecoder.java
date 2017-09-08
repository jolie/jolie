/**
 * Copyright (c) 2016, Oliver Kleine, Institute of Telematics, University of Luebeck
 * All rights reserved
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *  - Redistributions of source messageCode must retain the above copyright notice, this list of conditions and the following
 *    disclaimer.
 *
 *  - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *    following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 *  - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote
 *    products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ncoap.communication.codec;

import io.netty.channel.ChannelOutboundHandlerAdapter;
import ncoap.communication.dispatching.Token;
import ncoap.message.*;
import ncoap.message.options.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

import static ncoap.message.MessageType.*;
import static ncoap.message.MessageCode.*;

/**
 * The {@link CoapMessageDecoder} deserializes inbound messages. Please note the
 * following:
 * <ul>
 * <li>
 * If the inbound message is a {@link de.uzl.itm.ncoap.message.CoapResponse}
 * then malformed or unknown options are silently ignored and the
 * {@link de.uzl.itm.ncoap.message.CoapResponse} is further processed without
 * these options.
 * </li>
 * <li>
 * If the inbound message is a {@link de.uzl.itm.ncoap.message.CoapRequest},
 * then malformed or unsupported, i.e. unknown non-critical options are silently
 * ignored but critical options lead to an immediate
 * {@link de.uzl.itm.ncoap.message.CoapResponse} with
 * {@link de.uzl.itm.ncoap.message.MessageCode#BAD_OPTION_402} being sent to the
 * remote CoAP endpoints.
 * </li>
 * <li>
 * Malformed inbound {@link de.uzl.itm.ncoap.message.CoapMessage}s with
 * malformed header, e.g. a TKL field that does not correspond to the actual
 * tokens length, lead to an immediate
 * {@link de.uzl.itm.ncoap.message.CoapMessage} with
 * {@link de.uzl.itm.ncoap.message.MessageType#RST} being sent to the remote
 * CoAP endpoints.
 * </li>
 * <li>
 * For inbound {@link de.uzl.itm.ncoap.message.CoapMessage}s with
 * {@link de.uzl.itm.ncoap.message.MessageCode#EMPTY} only the header, i.e. the
 * first 4 bytes are decoded and further processed. Any following bytes
 * contained in the same encoded message are ignored.
 * </li>
 * </ul>
 *
 * @author Oliver Kleine
 */
public class CoapMessageDecoder extends ChannelOutboundHandlerAdapter {

    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    @Override
    public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent evt) throws Exception {

	if (evt instanceof ExceptionEvent) {
	    exceptionCaught(ctx, (ExceptionEvent) evt);
	    return;
	}

	if (!(evt instanceof MessageEvent) || !(((MessageEvent) evt).getMessage() instanceof ChannelBuffer)) {
	    ctx.sendUpstream(evt);
	    return;
	}

	final MessageEvent messageEvent = (MessageEvent) evt;
	messageEvent.getFuture().setSuccess();

	InetSocketAddress remoteSocket = (InetSocketAddress) messageEvent.getRemoteAddress();
	CoapMessage coapMessage = decode(remoteSocket, (ChannelBuffer) messageEvent.getMessage());

	if (coapMessage != null) {
	    Channels.fireMessageReceived(ctx, coapMessage, remoteSocket);
	}
    }

    protected CoapMessage decode(InetSocketAddress remoteSocket, ChannelBuffer buffer)
	    throws HeaderDecodingException, OptionCodecException {

	log.debug("Incoming message to be decoded (length: {})", buffer.readableBytes());

	//Decode the Message Header which must have a length of exactly 4 bytes
	if (buffer.readableBytes() < 4) {
	    String message = "Encoded CoAP messages MUST have min. 4 bytes. This has " + buffer.readableBytes() + "!";
	    throw new HeaderDecodingException(CoapMessage.UNDEFINED_MESSAGE_ID, remoteSocket, message);
	}

	//Decode the header values
	int encodedHeader = buffer.readInt();
	int version = (encodedHeader >>> 30) & 0x03;
	int messageType = (encodedHeader >>> 28) & 0x03;
	int tokenLength = (encodedHeader >>> 24) & 0x0F;
	int messageCode = (encodedHeader >>> 16) & 0xFF;
	int messageID = (encodedHeader) & 0xFFFF;

	log.debug("Decoded Header: (T) {}, (TKL) {}, (C) {}, (ID) {}",
		new Object[]{messageType, tokenLength, messageCode, messageID});

	//Check whether the protocol version is supported (=1)
	if (version != CoapMessage.PROTOCOL_VERSION) {
	    String message = "CoAP version (" + version + ") is other than \"1\"!";
	    throw new HeaderDecodingException(messageID, remoteSocket, message);
	}

	//Check whether TKL indicates a not allowed token length
	if (tokenLength > CoapMessage.MAX_TOKEN_LENGTH) {
	    String message = "TKL value (" + tokenLength + ") is larger than 8!";
	    throw new HeaderDecodingException(messageID, remoteSocket, message);
	}

	//Check whether there are enough unread bytes left to read the token
	if (buffer.readableBytes() < tokenLength) {
	    String message = "TKL value is " + tokenLength + " but only " + buffer.readableBytes() + " bytes left!";
	    throw new HeaderDecodingException(messageID, remoteSocket, message);
	}

	//Handle empty message (ignore everything but the first 4 bytes)
	if (messageCode == EMPTY) {

	    if (messageType == ACK) {
		return CoapMessage.createEmptyAcknowledgement(messageID);
	    } else if (messageType == RST) {
		return CoapMessage.createEmptyReset(messageID);
	    } else if (messageType == CON) {
		return CoapMessage.createPing(messageID);
	    } else {
		//There is no empty NON message defined, so send a RST
		throw new HeaderDecodingException(messageID, remoteSocket, "Empty NON messages are invalid!");
	    }
	}

	//Read the token
	byte[] token = new byte[tokenLength];
	buffer.readBytes(token);

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
	if (buffer.readableBytes() > 0) {
	    try {
		setOptions(coapMessage, buffer);
	    } catch (OptionCodecException ex) {
		ex.setMessageID(messageID);
		ex.setToken(new Token(token));
		ex.setremoteSocket(remoteSocket);
		ex.setMessageType(messageType);
		throw ex;
	    }
	}

	//The remaining bytes (if any) are the messages payload. If there is no payload, reader and writer index are
	//at the same position (buf.readableBytes() == 0).
	buffer.discardReadBytes();

	try {
	    coapMessage.setContent(buffer);
	} catch (IllegalArgumentException e) {
	    String warning = "Message code {} does not allow content. Ignore {} bytes.";
	    log.warn(warning, coapMessage.getMessageCode(), buffer.readableBytes());
	}

	log.info("Decoded Message: {}", coapMessage);

	return coapMessage;
    }

    private void setOptions(CoapMessage coapMessage, ChannelBuffer buffer) throws OptionCodecException {

	//Decode the options
	int previousOptionNumber = 0;
	int firstByte = buffer.readByte() & 0xFF;

	while (firstByte != 0xFF && buffer.readableBytes() >= 0) {
	    log.debug("First byte: {} ({})", toBinaryString(firstByte), firstByte);
	    int optionDelta = (firstByte & 0xF0) >>> 4;
	    int optionLength = firstByte & 0x0F;
	    log.debug("temp. delta: {}, temp. length {}", optionDelta, optionLength);

	    if (optionDelta == 13) {
		optionDelta += buffer.readByte() & 0xFF;
	    } else if (optionDelta == 14) {
		optionDelta = 269 + ((buffer.readByte() & 0xFF) << 8) + (buffer.readByte() & 0xFF);
	    }

	    if (optionLength == 13) {
		optionLength += buffer.readByte() & 0xFF;
	    } else if (optionLength == 14) {
		optionLength = 269 + ((buffer.readByte() & 0xFF) << 8) + (buffer.readByte() & 0xFF);
	    }

	    log.info("Previous option: {}, Option delta: {}", previousOptionNumber, optionDelta);

	    int actualOptionNumber = previousOptionNumber + optionDelta;
	    log.info("Decode option no. {} with length of {} bytes.", actualOptionNumber, optionLength);

	    try {
		byte[] optionValue = new byte[optionLength];
		buffer.readBytes(optionValue);

		switch (OptionValue.getType(actualOptionNumber)) {
		    case EMPTY: {
			EmptyOptionValue value = new EmptyOptionValue(actualOptionNumber);
			coapMessage.addOption(actualOptionNumber, value);
			break;
		    }
		    case OPAQUE: {
			OpaqueOptionValue value = new OpaqueOptionValue(actualOptionNumber, optionValue);
			coapMessage.addOption(actualOptionNumber, value);
			break;
		    }
		    case STRING: {
			StringOptionValue value = new StringOptionValue(actualOptionNumber, optionValue, true);
			coapMessage.addOption(actualOptionNumber, value);
			break;
		    }
		    case UINT: {
			UintOptionValue value = new UintOptionValue(actualOptionNumber, optionValue, true);
			coapMessage.addOption(actualOptionNumber, value);
			break;
		    }
		    default: {
			log.error("This should never happen!");
			throw new RuntimeException("This should never happen!");
		    }
		}
	    } catch (IllegalArgumentException e) {
		//failed option creation leads to an illegal argument exception
		log.warn("Exception while decoding option!", e);

		if (MessageCode.isResponse(coapMessage.getMessageCode())) {
		    //Malformed options in responses are silently ignored...
		    log.warn("Silently ignore malformed option no. {} in inbound response.", actualOptionNumber);
		} else if (Option.isCritical(actualOptionNumber)) {
		    //Critical malformed options in requests cause an exception
		    throw new OptionCodecException(actualOptionNumber);
		} else {
		    //Not critical malformed options in requests are silently ignored...
		    log.warn("Silently ignore elective option no. {} in inbound request.", actualOptionNumber);
		}
	    }

	    previousOptionNumber = actualOptionNumber;

	    if (buffer.readableBytes() > 0) {
		firstByte = buffer.readByte() & 0xFF;
	    } else {
		// this is necessary if there is no payload and the last option is empty (e.g. UintOption with value 0)
		firstByte = 0xFF;
	    }

	    log.debug("{} readable bytes remaining.", buffer.readableBytes());
	}
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent exceptionEvent) {
	Throwable cause = exceptionEvent.getCause();

	//Invalid Header Exceptions cause a RST
	if (cause instanceof HeaderDecodingException) {
	    HeaderDecodingException ex = (HeaderDecodingException) cause;

	    if (ex.getMessageID() != CoapMessage.UNDEFINED_MESSAGE_ID) {
		writeReset(ctx, ex.getMessageID(), ex.getremoteSocket());
	    } else {
		log.warn("Ignore inbound message with malformed header...");
	    }
	} else if (cause instanceof OptionCodecException) {
	    OptionCodecException ex = (OptionCodecException) cause;
	    int messageType = ex.getMessageType() == CON ? ACK : NON;

	    writeBadOptionResponse(ctx, messageType, ex.getMessageID(), ex.getToken(), ex.getremoteSocket(),
		    ex.getMessage());
	} else {
	    ctx.sendUpstream(exceptionEvent);
	}

    }

    private void writeReset(ChannelHandlerContext ctx, int messageID, InetSocketAddress remoteSocket) {
	CoapMessage resetMessage = CoapMessage.createEmptyReset(messageID);
	Channels.write(ctx, Channels.future(ctx.getChannel()), resetMessage, remoteSocket);
    }

    private void writeBadOptionResponse(ChannelHandlerContext ctx, int messageType, int messageID,
	    Token token, InetSocketAddress remoteSocket, String content) {

	CoapResponse errorResponse = CoapResponse.createErrorResponse(messageType, BAD_OPTION_402, content);
	errorResponse.setMessageID(messageID);
	errorResponse.setToken(token);

	Channels.write(ctx, Channels.future(ctx.getChannel()), errorResponse, remoteSocket);
    }

    private static String toBinaryString(int byteValue) {
	StringBuilder buffer = new StringBuilder(8);

	for (int i = 7; i >= 0; i--) {
	    if ((byteValue & (int) Math.pow(2, i)) > 0) {
		buffer.append("1");
	    } else {
		buffer.append("0");
	    }
	}

	return buffer.toString();
    }
}
