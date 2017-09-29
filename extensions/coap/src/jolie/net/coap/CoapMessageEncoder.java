package jolie.net.coap;

import com.google.common.primitives.Ints;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;
import jolie.Interpreter;

public class CoapMessageEncoder extends MessageToMessageEncoder<CoapMessage> {

    public static final int MAX_OPTION_DELTA = 65804;
    public static final int MAX_OPTION_LENGTH = 65804;

    @Override
    protected void encode(ChannelHandlerContext ctx, CoapMessage in,
	    List<Object> out) throws Exception {

	Interpreter.getInstance().logInfo("CoapMessage to be encoded: " + in);

	ByteBuf bb = encode(in);
	if (bb != null) {
	    out.add(bb);
	} else {
	    Interpreter.getInstance().logSevere("Error handling Encoding");
	}
    }

    protected ByteBuf encode(CoapMessage coapMessage) {

	// start encoding
	ByteBuf encodedMessage = Unpooled.buffer(0);

	// encode HEADER and TOKEN
	encodeHeader(encodedMessage, coapMessage);

	if (coapMessage.getMessageCode() == MessageCode.EMPTY) {
	    encodedMessage
		    = Unpooled.wrappedBuffer(
			    Ints.toByteArray(
				    encodedMessage.getInt(0) & 0xF0FFFFFF));
	    return encodedMessage;
	}

	if (coapMessage.getAllOptions().size()
		== 0 && coapMessage.getContent().readableBytes() == 0) {
	    return encodedMessage;
	}

	try {
	    encodeOptions(encodedMessage, coapMessage);
	} catch (Exception ex) {
	    Interpreter.getInstance().logSevere(ex.getMessage());
	    return null;
	}

	if (coapMessage.getContent().readableBytes() > 0) {
	    encodedMessage.writeByte(255);
	    encodedMessage = Unpooled.wrappedBuffer(encodedMessage,
		    coapMessage.getContent());
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

	if (token.length > 0) {
	    buffer.writeBytes(token);
	}
    }

    protected void encodeOptions(ByteBuf buffer, CoapMessage coapMessage)
	    throws Exception {

	//Encode options one after the other and append buf option to the buf
	int previousOptionNumber = 0;

	for (int optionNumber : coapMessage.getAllOptions().keySet()) {
	    for (OptionValue optionValue
		    : coapMessage.getOptions(optionNumber)) {
		encodeOption(buffer, optionNumber, optionValue,
			previousOptionNumber);
		previousOptionNumber = optionNumber;
	    }
	}
    }

    protected void encodeOption(ByteBuf buffer, int optionNumber,
	    OptionValue optionValue, int prevNumber) throws Exception {

	if (prevNumber > optionNumber) {
	    throw new Exception("The previous option number must be smaller "
		    + "or equal to the actual one!");
	}

	int optionDelta = optionNumber - prevNumber;
	int optionLength = optionValue.getValue().length;

	if (optionLength > MAX_OPTION_LENGTH) {
	    throw new Exception("Option length error!");
	}

	if (optionDelta > MAX_OPTION_DELTA) {
	    throw new Exception("option delta error!");
	}

	if (optionDelta < 13) {
	    //option delta < 13
	    if (optionLength < 13) {
		buffer.writeByte(((optionDelta & 0xFF) << 4)
			| (optionLength & 0xFF));
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
    }
}
