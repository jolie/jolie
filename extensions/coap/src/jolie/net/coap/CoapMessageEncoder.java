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
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;
import jolie.net.coap.message.CoapMessage;
import jolie.net.coap.options.OptionValue;

public class CoapMessageEncoder extends MessageToMessageEncoder<CoapMessage> {

    public static final int MAX_OPTION_DELTA = 65804;
    public static final int MAX_OPTION_LENGTH = 65804;

    @Override
    protected void encode(ChannelHandlerContext ctx, CoapMessage in,
	    List<Object> out) throws Exception {

	ByteBuf msg = Unpooled.buffer();

	//write encoded header
	byte[] token = in.getToken().getBytes();
	int encodedHeader = ((in.getProtocolVersion() & 0x03) << 30)
		| ((in.getMessageType() & 0x03) << 28)
		| ((token.length & 0x0F) << 24)
		| ((in.getMessageCode() & 0xFF) << 16)
		| ((in.getMessageID() & 0xFFFF));
	msg.writeInt(encodedHeader);
	if (token.length > 0) {
	    msg.writeBytes(token);
	}

	if (in.getAllOptions().isEmpty()
		&& in.content().readableBytes() == 0) {

	    out.add(msg);
	}

	//write encoded options
	int previousOptionNumber = 0;

	for (int optionNumber : in.getAllOptions().keySet()) {
	    for (OptionValue optionValue : in.getOptions(optionNumber)) {
		encodeOption(msg, optionNumber, optionValue,
			previousOptionNumber);
		previousOptionNumber = optionNumber;
	    }
	}

	//write encoded content
	if (in.content().readableBytes() > 0) {
	    msg.writeByte(255);
	    msg.writeBytes(Unpooled.wrappedBuffer(in.content()));
	}

	out.add(msg);
    }

    private void encodeOption(ByteBuf buffer, int optionNumber,
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
