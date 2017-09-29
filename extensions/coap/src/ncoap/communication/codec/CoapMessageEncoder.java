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
/**
 * Copyright (c) 2012, Oliver Kleine, Institute of Telematics, University of Luebeck
 * All rights reserved
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice, this list of conditions and the following
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
/**
 * Copyright (c) 2012, Oliver Kleine, Institute of Telematics, University of Luebeck
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials provided with the distribution.
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote
 * products derived from this software without specific prior written permission.
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

import com.google.common.primitives.Ints;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import ncoap.communication.dispatching.Token;
import ncoap.communication.events.MiscellaneousErrorEvent;
import ncoap.message.CoapMessage;
import ncoap.message.MessageCode;
import ncoap.message.options.OptionValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * A {@link CoapMessageEncoder} serializes outgoing {@link CoapMessage}s. In the
 * (rather unlikely) case that there is an exception thrown during the encoding
 * process, an internal message is sent upstream, i.e. in the direction of the
 * application.
 *
 * @author Oliver Kleine
 */
public class CoapMessageEncoder extends MessageToMessageEncoder<CoapMessage> {

    private static Logger LOG = LoggerFactory.getLogger(CoapMessageEncoder.class.getName());

    /**
     * The maximum option delta (65804)
     */
    public static final int MAX_OPTION_DELTA = 65804;

    /**
     * The maximum option length (65804)
     */
    public static final int MAX_OPTION_LENGTH = 65804;

    protected ByteBuf encode(CoapMessage coapMessage) throws OptionCodecException {
	LOG.info("CoapMessage to be encoded: {}", coapMessage);

	// start encoding
	ByteBuf encodedMessage = Unpooled.buffer(0);

	// encode HEADER and TOKEN
	encodeHeader(encodedMessage, coapMessage);
	LOG.debug("Encoded length of message (after HEADER + TOKEN): {}", encodedMessage.readableBytes());

	if (coapMessage.getMessageCode() == MessageCode.EMPTY) {
	    encodedMessage = Unpooled.wrappedBuffer(Ints.toByteArray(encodedMessage.getInt(0) & 0xF0FFFFFF));
	    return encodedMessage;
	}

	if (coapMessage.getAllOptions().size() == 0 && coapMessage.getContent().readableBytes() == 0) {
	    return encodedMessage;
	}

	// encode OPTIONS (if any)
	encodeOptions(encodedMessage, coapMessage);
	LOG.debug("Encoded length of message (after OPTIONS): {}", encodedMessage.readableBytes());

	// encode payload (if any)
	if (coapMessage.getContent().readableBytes() > 0) {
	    // add END-OF-OPTIONS marker only if there is payload
	    encodedMessage.writeByte(255);

	    // add payload
	    encodedMessage = Unpooled.wrappedBuffer(encodedMessage, coapMessage.getContent());
	    LOG.debug("Encoded length of message (after CONTENT): {}", encodedMessage.readableBytes());
	}

	return encodedMessage;
    }

    protected void encodeHeader(ByteBuf buffer, CoapMessage coapMessage) {

	byte[] token = coapMessage.getToken().getBytes();

	int encodedHeader = ((coapMessage.getProtocolVersion() & 0x03) << 30)
		| ((coapMessage.getMessageType() & 0x03) << 28)
		| ((token.length & 0x0F) << 24)
		| ((coapMessage.getMessageCode() & 0xFF) << 16)
		| ((coapMessage.getMessageID() & 0xFFFF));

	buffer.writeInt(encodedHeader);

	if (LOG.isDebugEnabled()) {
	    String binary = Integer.toBinaryString(encodedHeader);
	    while (binary.length() < 32) {
		binary = "0" + binary;
	    }
	    LOG.debug("Encoded Header: {}", binary);
	}

	//Write token
	if (token.length > 0) {
	    buffer.writeBytes(token);
	}
    }

    protected void encodeOptions(ByteBuf buffer, CoapMessage coapMessage) throws OptionCodecException {

	//Encode options one after the other and append buf option to the buf
	int previousOptionNumber = 0;

	for (int optionNumber : coapMessage.getAllOptions().keySet()) {
	    for (OptionValue optionValue : coapMessage.getOptions(optionNumber)) {
		encodeOption(buffer, optionNumber, optionValue, previousOptionNumber);
		previousOptionNumber = optionNumber;
	    }
	}
    }

    protected void encodeOption(ByteBuf buffer, int optionNumber, OptionValue optionValue, int prevNumber) throws OptionCodecException {

	//The previous option number must be smaller or equal to the actual one
	if (prevNumber > optionNumber) {
	    LOG.error("Previous option no. ({}) must not be larger then current option no ({})",
		    prevNumber, optionNumber);
	    throw new OptionCodecException(optionNumber);
	}

	int optionDelta = optionNumber - prevNumber;
	int optionLength = optionValue.getValue().length;

	if (optionLength > MAX_OPTION_LENGTH) {
	    LOG.error("Option no. {} exceeds maximum option length (actual: {}, max: {}).",
		    new Object[]{optionNumber, optionLength, MAX_OPTION_LENGTH});

	    throw new OptionCodecException(optionNumber);
	}

	if (optionDelta > MAX_OPTION_DELTA) {
	    LOG.error("Option delta exceeds maximum option delta (actual: {}, max: {})", optionDelta, MAX_OPTION_DELTA);
	    throw new OptionCodecException(optionNumber);
	}

	if (optionDelta < 13) {
	    //option delta < 13
	    if (optionLength < 13) {
		buffer.writeByte(((optionDelta & 0xFF) << 4) | (optionLength & 0xFF));
	    } else if (optionLength < 269) {
		buffer.writeByte(((optionDelta << 4) & 0xFF) | (13 & 0xFF));
		buffer.writeByte((optionLength - 13) & 0xFF);
	    } else {
		buffer.writeByte(((optionDelta << 4) & 0xFF) | (14 & 0xFF));
		buffer.writeByte(((optionLength - 269) & 0xFF00) >>> 8);
		buffer.writeByte((optionLength - 269) & 0xFF);
	    }
	} else if (optionDelta < 269) {
	    //13 <= option delta < 269
	    if (optionLength < 13) {
		buffer.writeByte(((13 & 0xFF) << 4) | (optionLength & 0xFF));
		buffer.writeByte((optionDelta - 13) & 0xFF);
	    } else if (optionLength < 269) {
		buffer.writeByte(((13 & 0xFF) << 4) | (13 & 0xFF));
		buffer.writeByte((optionDelta - 13) & 0xFF);
		buffer.writeByte((optionLength - 13) & 0xFF);
	    } else {
		buffer.writeByte((13 & 0xFF) << 4 | (14 & 0xFF));
		buffer.writeByte((optionDelta - 13) & 0xFF);
		buffer.writeByte(((optionLength - 269) & 0xFF00) >>> 8);
		buffer.writeByte((optionLength - 269) & 0xFF);
	    }
	} else {
	    //269 <= option delta < 65805
	    if (optionLength < 13) {
		buffer.writeByte(((14 & 0xFF) << 4) | (optionLength & 0xFF));
		buffer.writeByte(((optionDelta - 269) & 0xFF00) >>> 8);
		buffer.writeByte((optionDelta - 269) & 0xFF);
	    } else if (optionLength < 269) {
		buffer.writeByte(((14 & 0xFF) << 4) | (13 & 0xFF));
		buffer.writeByte(((optionDelta - 269) & 0xFF00) >>> 8);
		buffer.writeByte((optionDelta - 269) & 0xFF);
		buffer.writeByte((optionLength - 13) & 0xFF);
	    } else {
		buffer.writeByte(((14 & 0xFF) << 4) | (14 & 0xFF));
		buffer.writeByte(((optionDelta - 269) & 0xFF00) >>> 8);
		buffer.writeByte((optionDelta - 269) & 0xFF);
		buffer.writeByte(((optionLength - 269) & 0xFF00) >>> 8);
		buffer.writeByte((optionLength - 269) & 0xFF);
	    }
	}

	//Write option value
	buffer.writeBytes(optionValue.getValue());
	LOG.debug("Encoded option no {} with value {}", optionNumber, optionValue.getDecodedValue());
	LOG.debug("Encoded message length is now: {}", buffer.readableBytes());
    }

    private void sendInternalEncodingFailedMessage(ChannelHandlerContext ctx, InetSocketAddress remoteSocket,
	    int messageID, Token token, Throwable cause) {

	String desc = cause.getMessage() == null ? "Encoder (" + cause.getClass().getName() + ")" : cause.getMessage();
	MiscellaneousErrorEvent event = new MiscellaneousErrorEvent(remoteSocket, messageID, token, desc);
	ctx.fireChannelRead(event);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, CoapMessage msg, List<Object> out) throws Exception {
	out.add(encode(msg));
    }
}
