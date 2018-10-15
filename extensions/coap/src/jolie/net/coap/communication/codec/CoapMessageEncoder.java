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
package jolie.net.coap.communication.codec;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import java.util.List;
import jolie.Interpreter;
import jolie.net.coap.message.CoapMessage;
import jolie.net.coap.message.options.OptionValue;

/**
 * A {@link CoapMessageEncoder} serializes outgoing {@link CoapMessage}s. In the
 * (rather unlikely) case that there is an exception thrown during the encoding
 * process, an internal message is sent upstream, i.e. in the direction of the
 * application.
 *
 * @author Oliver Kleine
 */
public class CoapMessageEncoder extends MessageToMessageEncoder<CoapMessage>
{

	public static final int MAX_OPTION_DELTA = 65804;
	public static final int MAX_OPTION_LENGTH = 65804;

	@Override
	protected void encode( ChannelHandlerContext ctx, CoapMessage in,
		List<Object> out ) throws Exception
	{
		try {
			ByteBuf msg = this.internal_encode( in );
			out.add( msg );
		} catch( OptionCodecException ex ) {
			ctx.fireExceptionCaught( ex );
		}
	}

	private ByteBuf internal_encode( CoapMessage coapMessage )
		throws OptionCodecException
	{

		ByteBuf msg = Unpooled.buffer();

		//write encoded header
		byte[] token = coapMessage.token().getBytes();
		int encodedHeader = ((coapMessage.getProtocolVersion() & 0x03) << 30)
			| ((coapMessage.messageType() & 0x03) << 28)
			| ((token.length & 0x0F) << 24)
			| ((coapMessage.messageCode() & 0xFF) << 16)
			| ((coapMessage.id() & 0xFFFF));

		msg.writeInt( encodedHeader );
		if ( token.length > 0 ) {
			msg.writeBytes( token );
		}

		if ( coapMessage.getAllOptions().isEmpty()
			&& coapMessage.getContent().readableBytes() == 0 ) {
			return msg;
		}

		//write encoded options
		int previousOptionNumber = 0;

		for( int optionNumber : coapMessage.getAllOptions().keySet() ) {
			for( OptionValue optionValue : coapMessage.getOptions( optionNumber ) ) {
				this.encodeOption( msg, optionNumber, optionValue, previousOptionNumber );
				previousOptionNumber = optionNumber;
			}
		}

		//write encoded setContent
		if ( coapMessage.getContent().readableBytes() > 0 ) {
			msg.writeByte( 255 );
			msg.writeBytes( Unpooled.wrappedBuffer( coapMessage.getContent() ) );
		}

		return msg;
	}

	private void encodeOption( ByteBuf buffer, int optionNumber,
		OptionValue optionValue, int prevNumber ) throws OptionCodecException
	{

		//The previous option number must be smaller or equal to the actual one
		if ( prevNumber > optionNumber ) {
			Interpreter.getInstance().logSevere( "Previous option no. (" + prevNumber
				+ ") must not be larger then current option no (" + optionNumber
				+ ")" );
			throw new OptionCodecException( optionNumber );
		}

		int optionDelta = optionNumber - prevNumber;
		int optionLength = optionValue.getValue().length;

		if ( optionLength > MAX_OPTION_LENGTH ) {
			Interpreter.getInstance().logSevere( "Option no. " + optionNumber
				+ " exceeds maximum option length (actual: " + optionLength
				+ ", max: " + MAX_OPTION_LENGTH + ")." );
			throw new OptionCodecException( optionNumber );
		}

		if ( optionDelta > MAX_OPTION_DELTA ) {
			Interpreter.getInstance().logSevere( "Option delta exceeds maximum "
				+ "option delta (actual: " + optionDelta + ", max: "
				+ MAX_OPTION_DELTA + ")" );
			throw new OptionCodecException( optionNumber );
		}

		if ( optionDelta < 13 ) {
			//option delta < 13
			if ( optionLength < 13 ) {
				buffer.writeByte( ((optionDelta & 0xFF) << 4)
					| (optionLength & 0xFF) );
			} else if ( optionLength < 269 ) {
				buffer.writeByte( ((optionDelta << 4) & 0xFF) | (13 & 0xFF) );
				buffer.writeByte( (optionLength - 13) & 0xFF );
			} else {
				buffer.writeByte( ((optionDelta << 4) & 0xFF) | (14 & 0xFF) );
				buffer.writeByte( ((optionLength - 269) & 0xFF00) >>> 8 );
				buffer.writeByte( (optionLength - 269) & 0xFF );
			}
		} else if ( optionDelta
			< 269 ) {
			//13 <= option delta < 269
			if ( optionLength < 13 ) {
				buffer.writeByte( ((13 & 0xFF) << 4) | (optionLength & 0xFF) );
				buffer.writeByte( (optionDelta - 13) & 0xFF );
			} else if ( optionLength < 269 ) {
				buffer.writeByte( ((13 & 0xFF) << 4) | (13 & 0xFF) );
				buffer.writeByte( (optionDelta - 13) & 0xFF );
				buffer.writeByte( (optionLength - 13) & 0xFF );
			} else {
				buffer.writeByte( (13 & 0xFF) << 4 | (14 & 0xFF) );
				buffer.writeByte( (optionDelta - 13) & 0xFF );
				buffer.writeByte( ((optionLength - 269) & 0xFF00) >>> 8 );
				buffer.writeByte( (optionLength - 269) & 0xFF );
			}
		} else {
			//269 <= option delta < 65805
			if ( optionLength < 13 ) {
				buffer.writeByte( ((14 & 0xFF) << 4) | (optionLength & 0xFF) );
				buffer.writeByte( ((optionDelta - 269) & 0xFF00) >>> 8 );
				buffer.writeByte( (optionDelta - 269) & 0xFF );
			} else if ( optionLength < 269 ) {
				buffer.writeByte( ((14 & 0xFF) << 4) | (13 & 0xFF) );
				buffer.writeByte( ((optionDelta - 269) & 0xFF00) >>> 8 );
				buffer.writeByte( (optionDelta - 269) & 0xFF );
				buffer.writeByte( (optionLength - 13) & 0xFF );
			} else {
				buffer.writeByte( ((14 & 0xFF) << 4) | (14 & 0xFF) );
				buffer.writeByte( ((optionDelta - 269) & 0xFF00) >>> 8 );
				buffer.writeByte( (optionDelta - 269) & 0xFF );
				buffer.writeByte( ((optionLength - 269) & 0xFF00) >>> 8 );
				buffer.writeByte( (optionLength - 269) & 0xFF );
			}
		}

		//Write option value
		buffer.writeBytes( optionValue.getValue() );
	}
}
