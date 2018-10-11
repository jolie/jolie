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
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import jolie.net.Token;
import jolie.net.coap.message.CoapMessage;
import jolie.net.coap.message.CoapRequest;
import jolie.net.coap.message.CoapResponse;
import jolie.net.coap.message.MessageCode;
import jolie.net.coap.message.MessageType;
import jolie.net.coap.message.options.EmptyOptionValue;
import jolie.net.coap.message.options.OpaqueOptionValue;
import jolie.net.coap.message.options.OptionValue;
import jolie.net.coap.message.options.StringOptionValue;
import jolie.net.coap.message.options.UintOptionValue;

public class CoapMessageDecoder extends MessageToMessageDecoder<ByteBuf>
{

	private static final int RST_MSG_ID = Integer.MAX_VALUE;
	private SocketAddress remoteSocket;

	@Override
	protected void decode( ChannelHandlerContext ctx, ByteBuf in,
		List<Object> out ) throws Exception
	{

		this.remoteSocket = ctx.channel().remoteAddress();

		try {
			CoapMessage msg = this.decode_internal( in );
			out.add( msg );
		} catch( HeaderDecodingException ex ) {
			ctx.fireExceptionCaught( ex );
		}
	}

	private CoapMessage decode_internal( ByteBuf in ) throws HeaderDecodingException
	{

		if ( in.readableBytes() < 4 ) {
			String message = "Encoded CoAP messages MUST have "
				+ "min. 4 bytes. This has " + in.readableBytes() + "!";
			throw new HeaderDecodingException( RST_MSG_ID, (InetSocketAddress) this.remoteSocket, message );
		}

		//Decode the header values
		int encodedHeader = in.readInt();
		int version = (encodedHeader >>> 30) & 0x03;
		int messageType = (encodedHeader >>> 28) & 0x03;
		int tokenLength = (encodedHeader >>> 24) & 0x0F;
		int messageCode = (encodedHeader >>> 16) & 0xFF;
		int messageId = (encodedHeader) & 0xFFFF;

		if ( version != CoapMessage.PROTOCOL_VERSION ) {
			String message = "Coap Message Decoder >>> "
				+ "CoAP Message with \"version\": " + version
				+ " other than \"1\", is invalid!";
			throw new HeaderDecodingException( RST_MSG_ID, (InetSocketAddress) this.remoteSocket, message );
		}

		if ( tokenLength > CoapMessage.MAX_TOKEN_LENGTH ) {
			String message = "Coap Message Decoder >>> "
				+ "Coap Message with Token Length: " + tokenLength
				+ " larger than 8, is invalid!";
			throw new HeaderDecodingException( RST_MSG_ID, (InetSocketAddress) this.remoteSocket, message );
		}

		if ( in.readableBytes() < tokenLength ) {
			String message = "Coap Message Decoder >>> "
				+ "Coap Message with Token Length: " + tokenLength
				+ " and readable " + in.readableBytes() + " bytes left, is invalid!";
			throw new HeaderDecodingException( RST_MSG_ID, (InetSocketAddress) this.remoteSocket, message );
		}

		if ( messageCode == MessageCode.EMPTY ) {

			switch( messageType ) {
				case MessageType.ACK:
					return CoapMessage.createEmptyAcknowledgement( messageId );
				case MessageType.RST:
					return CoapMessage.createEmptyReset( messageId );
				case MessageType.CON:
					return CoapMessage.createPing( messageId );
				default:
					String message = "Coap Message Decoder >>> "
						+ "Coap Message with Message Code: \"" + messageCode + "\" and "
						+ "Message Type: \"" + messageCode + "\" is invalid!";
					throw new HeaderDecodingException( RST_MSG_ID, (InetSocketAddress) this.remoteSocket, message );
			}
		}

		byte[] token = new byte[ tokenLength ];
		in.readBytes( token );

		CoapMessage coapMessage;
		if ( MessageCode.isRequest( messageCode ) ) {
			coapMessage = new CoapRequest( messageType, messageCode );
		} else {
			coapMessage = new CoapResponse( messageType, messageCode );
			coapMessage.setMessageType( messageType );
		}

		coapMessage.messageID( messageId );
		coapMessage.token( new Token( token ) );

		if ( in.readableBytes() > 0 ) {
			this.setOptions( coapMessage, in );
		}

		in.discardReadBytes();
		coapMessage.setContent( in.retain() );

		return coapMessage;
	}

	private void setOptions( CoapMessage coapMessage, ByteBuf bb )
		throws HeaderDecodingException
	{

		int previousOptionNumber = 0;
		int firstByte = bb.readByte() & 0xFF;

		while( firstByte != 0xFF && bb.readableBytes() >= 0 ) {

			int optionDelta = (firstByte & 0xF0) >>> 4;
			int optionLength = firstByte & 0x0F;

			if ( optionDelta == 13 ) {
				optionDelta += bb.readByte() & 0xFF;
			} else if ( optionDelta == 14 ) {
				optionDelta = 269 + ((bb.readByte() & 0xFF) << 8)
					+ (bb.readByte() & 0xFF);
			}

			if ( optionLength == 13 ) {
				optionLength += bb.readByte() & 0xFF;
			} else if ( optionLength == 14 ) {
				optionLength = 269 + ((bb.readByte() & 0xFF) << 8)
					+ (bb.readByte() & 0xFF);
			}

			int actualOptionNumber = previousOptionNumber + optionDelta;

			byte[] optionValue = new byte[ optionLength ];
			bb.readBytes( optionValue );

			switch( OptionValue.getType( actualOptionNumber ) ) {
				case EMPTY: {
					EmptyOptionValue value
						= new EmptyOptionValue( actualOptionNumber );
					coapMessage.addOption( actualOptionNumber, value );
					break;
				}
				case OPAQUE: {
					OpaqueOptionValue value
						= new OpaqueOptionValue( actualOptionNumber,
							optionValue );
					coapMessage.addOption( actualOptionNumber, value );
					break;
				}
				case STRING: {
					StringOptionValue value
						= new StringOptionValue( actualOptionNumber,
							optionValue, true );
					coapMessage.addOption( actualOptionNumber, value );
					break;
				}
				case UINT: {
					UintOptionValue value
						= new UintOptionValue( actualOptionNumber,
							optionValue, true );
					coapMessage.addOption( actualOptionNumber, value );
					break;
				}
				default: {
					String message = "Coap Message Decoder >>> "
						+ "Coap Message Type: " + OptionValue.getType( actualOptionNumber )
						+ ", is invalid!";
					throw new HeaderDecodingException( RST_MSG_ID, (InetSocketAddress) this.remoteSocket, message );
				}
			}

			previousOptionNumber = actualOptionNumber;

			if ( bb.readableBytes() > 0 ) {
				firstByte = bb.readByte() & 0xFF;
			} else {
				firstByte = 0xFF;
			}
		}
	}

	private static String toBinaryString( int byteValue )
	{
		StringBuilder buffer = new StringBuilder( 8 );

		for( int i = 7; i >= 0; i-- ) {
			if ( (byteValue & (int) Math.pow( 2, i )) > 0 ) {
				buffer.append( "1" );
			} else {
				buffer.append( "0" );
			}
		}

		return buffer.toString();
	}
}
