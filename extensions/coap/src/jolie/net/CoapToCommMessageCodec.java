/**********************************************************************************
 *   Copyright (C) 2017-18 by Stefano Pio Zingaro <stefanopio.zingaro@unibo.it>   *
 *   Copyright (C) 2017-18 by Saverio Giallorenzo <saverio.giallorenzo@gmail.com> *
 *                                                                                *
 *   This program is free software; you can redistribute it and/or modify         *
 *   it under the terms of the GNU Library General Public License as              *
 *   published by the Free Software Foundation; either version 2 of the           *
 *   License, or (at your option) any later version.                              *
 *                                                                                *
 *   This program is distributed in the hope that it will be useful,              *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of               *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                *
 *   GNU General Public License for more details.                                 *
 *                                                                                *
 *   You should have received a copy of the GNU Library General Public            *
 *   License along with this program; if not, write to the                        *
 *   Free Software Foundation, Inc.,                                              *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.                    *
 *                                                                                *
 *   For details about the authors of this software, see the AUTHORS file.        *
 **********************************************************************************/

package jolie.net;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.util.CharsetUtil;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import jolie.Interpreter;
import jolie.js.JsUtils;
import jolie.net.coap.message.CoapMessage;
import jolie.net.coap.message.CoapRequest;
import jolie.net.coap.message.CoapResponse;
import jolie.net.coap.message.MessageCode;
import jolie.net.coap.message.MessageType;
import jolie.net.coap.message.options.ContentFormat;
import jolie.net.coap.message.options.Option;
import jolie.net.coap.message.options.OptionValue;
import jolie.runtime.ByteArray;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.typing.Type;
import jolie.runtime.typing.TypeCastingException;
import jolie.runtime.typing.TypeCheckingException;
import jolie.xml.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class CoapToCommMessageCodec extends MessageToMessageCodec<CoapMessage, CommMessage>
{
	private static final Charset DEFAULT_CHARSET = CharsetUtil.UTF_8;

	private final CoapProtocol protocol;
	private final boolean isInput;

	public CoapToCommMessageCodec( CoapProtocol protocol )
	{
		this.protocol = protocol;
		this.isInput = protocol.isInput;
	}

	@Override
	protected void encode( ChannelHandlerContext ctx, CommMessage in, List<Object> out ) throws Exception
	{
		protocol.setSendExecutionThread( in.id() );

		if ( isInput ) {
			out.add( encode_inbound( in ) );
		} else {
			out.add( encode_outbound( ctx, in ) );
		}
	}

	@Override
	protected void decode( ChannelHandlerContext ctx, CoapMessage in, List<Object> out ) throws Exception
	{
		protocol.setReceiveExecutionThread( (long) in.id() );

		if ( isInput ) {
			out.add( decode_inbound( in ) );
		} else {
			out.add( decode_outbound( in ) );
		}
	}

	private static class Parameters
	{

		private static final String DEBUG = "debug";
		private static final String CONTENT_FORMAT = "contentFormat";
		private static final String DEFAULT_CONTENT_FORMAT = "application/json";
		private static final String MESSAGE_TYPE = "messageType";
		private static final int DEFAULT_MESSAGE_TYPE = MessageType.NON;
		private static final String MESSAGE_CODE = "messageCode";
		private static final String JSON_ENCODING = "json_encoding";
		private static final String ALIAS = "alias";
		private static final String TIMEOUT = "timeout";
		private static final int DEFAULT_TIMEOUT = 2;
	}

	private CoapMessage encode_outbound( ChannelHandlerContext ctx, CommMessage in )
	{
		String operationName = in.operationName();
		if ( isRequestResponse( operationName ) ) {
			if ( protocol.checkBooleanParameter( Parameters.DEBUG ) ) {
				Interpreter.getInstance().logInfo( "Receiving a Comm Message Request for a CoAP Solicit Response:\n"
					+ in.toString() );
			}
		} else {
			if ( protocol.checkBooleanParameter( Parameters.DEBUG ) ) {
				Interpreter.getInstance().logInfo( "Receiving a Comm Message Request for a CoAP Notification:\n"
					+ in.toString() );
			}
		}

		CoapRequest out = null;
		try {
			out = new CoapRequest(
				messageTypeProtocolParameter( operationName ),
				messageCodeProtocolParameter( operationName, false ),
				targetURI( in )
			);
		} catch( IllegalArgumentException | URISyntaxException ex ) {
			Interpreter.getInstance().logSevere( ex.getMessage() );
		}

		out.id( (int) in.id() );

		// separate response
		if ( isRequestResponse( operationName ) && out.messageType() == MessageType.NON ) {
			out.setRandomToken();
		}

		// content
		if ( MessageCode.allowsContent( out.messageCode() ) ) {
			try {
				out.setContent(
					valueToByteBuf( in, ContentFormat.CONTENT_FORMAT.get( getLongContentFormatProtocolParameter( in.operationName() ) ) ),
					getLongContentFormatProtocolParameter( operationName )
				);
			} catch( IOException | ParserConfigurationException | TransformerException ex ) {
				Interpreter.getInstance().logSevere( ex.getMessage() );
			}
		}

		if ( isOneWay( operationName ) && out.messageType() == MessageType.NON ) {
			CommMessage ack = new CommMessage(
				in.id(),
				in.operationName(),
				"/",
				Value.create(),
				null
			);
			ctx.fireChannelRead( ack );
			if ( protocol.checkBooleanParameter( Parameters.DEBUG ) ) {
				Interpreter.getInstance().logInfo( "NON confirmable CoAP Notifications!\n"
					+ "Sending ACK to the Comm Core:\n"
					+ ack.toString() );
			}
		}

//		if ( out.messageType() == MessageType.CON ) {
//			int timeout = Parameters.DEFAULT_TIMEOUT;
//			if ( protocol.hasParameter( Parameters.TIMEOUT ) ) {
//				timeout = protocol.getIntParameter( Parameters.TIMEOUT );
//			}
//			ctx.pipeline().addLast( "TIMEOUT INBOUND/OUTBOUND", new ReadTimeoutHandler( timeout ) );
//			ctx.pipeline().addLast( "ERROR INBOUND", new ChannelInboundHandlerAdapter()
//			{
//				@Override
//				public void exceptionCaught( ChannelHandlerContext ctx, Throwable cause ) throws Exception
//				{
//					CoapMessage out = CoapMessage.createEmptyReset( (int) in.id() );
//					if ( protocol.checkBooleanParameter( Parameters.DEBUG ) ) {
//						Interpreter.getInstance().logInfo( "Sending a CoAP RST to the Client:\n"
//							+ out );
//					}
//					ctx.fireChannelRead( out );
//				}
//			} );
//		}
		if ( isRequestResponse( operationName ) ) {
			if ( protocol.checkBooleanParameter( Parameters.DEBUG ) ) {
				Interpreter.getInstance().logInfo( "Sending a CoAP Solicit Response:\n"
					+ out );
			}
		} else {
			if ( protocol.checkBooleanParameter( Parameters.DEBUG ) ) {
				Interpreter.getInstance().logInfo( "Sending a CoAP Notification:\n"
					+ out );
			}
		}

		return out;
	}

	private CommMessage decode_inbound( CoapMessage in )
	{
		String operationName = operationName( in );
		if ( isRequestResponse( operationName ) ) {
			if ( protocol.checkBooleanParameter( Parameters.DEBUG ) ) {
				Interpreter.getInstance().logInfo( "Receiving a CoAP Solicit Response:\n"
					+ in.toString() );
			}
		} else {
			if ( protocol.checkBooleanParameter( Parameters.DEBUG ) ) {
				Interpreter.getInstance().logInfo( "Receiving a CoAP Notification:\n"
					+ in.toString() );
			}
		}

		Value v = Value.create();
		if ( MessageCode.allowsContent( in.messageCode() )
			&& !in.getContent().equals( Unpooled.EMPTY_BUFFER ) ) {

			String format = in.contentFormat();
			try {
				v = byteBufToValue( in.getContent(), protocol.getSendType( operationName ), format, DEFAULT_CHARSET, protocol.checkStringParameter( Parameters.JSON_ENCODING, "strict" ) );
			} catch( IOException | ParserConfigurationException | SAXException | TypeCheckingException ex ) {
				Interpreter.getInstance().logSevere( ex.getMessage() );
			}
		}

		CommMessage out = new CommMessage( in.id(), operationName, "/", v, null );

		if ( isRequestResponse( operationName ) ) {
			if ( protocol.checkBooleanParameter( Parameters.DEBUG ) ) {
				Interpreter.getInstance().logInfo( "Forwading the CoAP Solicit Response to Comm Core:\n"
					+ out );
			}
		} else {
			if ( protocol.checkBooleanParameter( Parameters.DEBUG ) ) {
				Interpreter.getInstance().logInfo( "Forwading the CoAP Notification to Comm Core:\n"
					+ out );
			}
		}

		return out;
	}

	private CoapMessage encode_inbound( CommMessage in )
	{
		String operationName = in.operationName();
		if ( isRequestResponse( operationName ) ) {
			if ( protocol.checkBooleanParameter( Parameters.DEBUG ) ) {
				Interpreter.getInstance().logInfo( "Receiving a Comm Message Response from Comm Core:\n"
					+ in );
			}

			// THIS MESSAGE HAS NO INFORMATION ABOUT THE OPERATION IT BELONGS !! NOT READABLE AT DESTINATION !!
			// IT IS NOT POSSIBLE TO SET THE URI PATH MANUALLY BECAUSE COAP RESPONSE DOES NOT PERMIT IT
			CoapMessage out = new CoapResponse(
				messageTypeProtocolParameter( operationName ),
				messageCodeProtocolParameter( operationName, true ) );

			out.id( (int) in.id() );

			if ( MessageCode.allowsContent( out.messageCode() ) ) {
				ByteBuf content;
				try {
					content = valueToByteBuf( in, ContentFormat.CONTENT_FORMAT.get( getLongContentFormatProtocolParameter( in.operationName() ) ) );
					out.setContent( content, getLongContentFormatProtocolParameter( operationName ) );
				} catch( IOException | ParserConfigurationException | TransformerException ex ) {
					Interpreter.getInstance().logSevere( ex.getMessage() );
				}
			}
			if ( protocol.checkBooleanParameter( Parameters.DEBUG ) ) {
				Interpreter.getInstance().logInfo( "Sending a CoAP Response to the Client:\n"
					+ out );
			}
			return out;
		} else {
			if ( protocol.checkBooleanParameter( Parameters.DEBUG ) ) {
				Interpreter.getInstance().logInfo( "Receiving a Comm Message Ack from Comm Core:\n"
					+ in );
			}

			CoapMessage out = CoapMessage.createEmptyAcknowledgement( (int) in.id() );

			if ( protocol.checkBooleanParameter( Parameters.DEBUG ) ) {
				Interpreter.getInstance().logInfo( "Sending a CoAP Empty Ack to the Client:\n"
					+ out );
			}
			return out;
		}
	}

	private CommMessage decode_outbound( CoapMessage in )
	{
		if ( in.isEmptyAck() ) {
			if ( protocol.checkBooleanParameter( Parameters.DEBUG ) ) {
				Interpreter.getInstance().logInfo( "Receiving CoAP Ack from the Server:\n" + in );
			}
			CommMessage ack = new CommMessage( in.id(), null, "/", Value.create(), null );
			if ( protocol.checkBooleanParameter( Parameters.DEBUG ) ) {
				Interpreter.getInstance().logInfo( "Sending a Comm Message Ack to Comm Core:\n" + ack.toString() );
			}
			return ack;
		}

		String operationName = operationName( in );

		if ( isRequestResponse( operationName ) ) {
			if ( protocol.checkBooleanParameter( Parameters.DEBUG ) ) {
				Interpreter.getInstance().logInfo( "Receiving a CoAP Response from the Server:\n"
					+ in );
			}
			Value v = Value.create();
			if ( MessageCode.allowsContent( in.messageCode() )
				&& !in.getContent().equals( Unpooled.EMPTY_BUFFER ) ) {

				String format = ContentFormat.CONTENT_FORMAT.get(
					((long) in.getOptions( Option.CONTENT_FORMAT ).get( 0 ).getDecodedValue())
				);

				try {
					v = byteBufToValue( in.getContent(), protocol.getSendType( operationName ), format, DEFAULT_CHARSET, protocol.checkStringParameter( Parameters.JSON_ENCODING, "strict" ) );
				} catch( IOException | ParserConfigurationException | SAXException | TypeCheckingException ex ) {
					Interpreter.getInstance().logSevere( ex );
				}
			}

			CommMessage out = new CommMessage( in.id(), "/", operationName, v, null );
			if ( protocol.checkBooleanParameter( Parameters.DEBUG ) ) {
				Interpreter.getInstance().logInfo( "Sending a Comm Message Response to Comm Core:\n"
					+ out.toString() );
			}
			return out;
		}

		if ( protocol.checkBooleanParameter( Parameters.DEBUG ) ) {
			Interpreter.getInstance().logInfo( "Receiving CoAP RST from the Server:\n" + in );
		}
		CommMessage fault
			= new CommMessage( in.id(), operationName, "/", Value.create(), new FaultException( "Received a RESET from the Client for Timeout exceeding!" ) );
		if ( protocol.checkBooleanParameter( Parameters.DEBUG ) ) {
			Interpreter.getInstance().logInfo( "Sending a Comm Message Fault to Comm Core:\n"
				+ fault.toString() );
		}
		return fault;
	}

	/**
	 * TODO : Promote to AsyncCommProtocol
	 *
	 * @param commMessage
	 * @return
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws TransformerConfigurationException
	 * @throws TransformerException
	 */
	private ByteBuf valueToByteBuf( CommMessage commMessage, String format )
		throws IOException, ParserConfigurationException, TransformerConfigurationException, TransformerException
	{
		ByteBuf byteBuf = Unpooled.buffer();
		Value v = commMessage.isFault() ? Value.create( commMessage.fault().getMessage() ) : commMessage.value();

		switch( format ) {
			case "application/link-format": // TODO support it!
				break;
			case "application/xml":
				DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				Document doc = db.newDocument();
				Element root = doc.createElement( commMessage.operationName() );
				doc.appendChild( root );
				XmlUtils.valueToDocument( v, root, doc );
				Source src = new DOMSource( doc );
				ByteArrayOutputStream strm = new ByteArrayOutputStream();
				Result dest = new StreamResult( strm );
				Transformer trf = TransformerFactory.newInstance().newTransformer();
				trf.setOutputProperty( OutputKeys.ENCODING, DEFAULT_CHARSET.name() );
				trf.transform( src, dest );
				byteBuf.writeBytes( strm.toByteArray() );
				break;
			case "application/octet-stream":
			case "application/exi":
			case "text/plain":
				byteBuf.writeBytes( valueToPlainText( v ).getBytes( DEFAULT_CHARSET ) );
				break;
			case "application/json":
				StringBuilder jsonStringBuilder = new StringBuilder();
				JsUtils.valueToJsonString( v, true, protocol.getSendType( commMessage.operationName() ), jsonStringBuilder );
				byteBuf.writeBytes( jsonStringBuilder.toString().getBytes( DEFAULT_CHARSET ) );
				break;
		}
		if ( protocol.checkBooleanParameter( Parameters.DEBUG ) ) {
			Interpreter.getInstance().logInfo(
				"Sending " + format.toUpperCase() + " message: " + Unpooled.wrappedBuffer( byteBuf ).toString( DEFAULT_CHARSET ) );
		}
		return byteBuf;

	}

	private Value byteBufToValue( ByteBuf in, Type type, String format, Charset charset, boolean jsonEncoding )
		throws IOException, ParserConfigurationException, SAXException, TypeCheckingException
	{
		ByteBuf byteBuf = Unpooled.copiedBuffer( in );
		Value value = Value.create();
		String message = Unpooled.copiedBuffer( byteBuf ).toString( charset );

		if ( message.length() > 0 ) {
			switch( format ) {
				case "application/xml":
					DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
					DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
					InputSource src = new InputSource( new ByteBufInputStream( byteBuf ) );
					src.setEncoding( charset.name() );
					Document doc = builder.parse( src );
					XmlUtils.documentToValue( doc, value );
					break;
				case "application/link-format":
				case "application/octet-stream":
				case "application/exi":
				case "text/plain":
					parsePlainText( message, value, type );
					break;
				case "application/json":
					JsUtils.parseJsonIntoValue( new InputStreamReader( new ByteBufInputStream( byteBuf ) ), value, jsonEncoding );
					break;
			}

			// for XML format
			try {
				value = type.cast( value );
			} catch( TypeCastingException e ) {
				// do nothing
			}

		} else {
			value = Value.create();
			try {
				type.check( value );
			} catch( TypeCheckingException ex1 ) {
				value = Value.create( "" );
				try {
					type.check( value );
				} catch( TypeCheckingException ex2 ) {
					value = Value.create( new ByteArray( new byte[ 0 ] ) );
					try {
						type.check( value );
					} catch( TypeCheckingException ex3 ) {
						value = Value.create();
					}
				}
			}
		}
		return value;
	}

	private void parsePlainText( String message, Value value, Type type ) throws TypeCheckingException
	{

		try {
			type
				.check( Value
					.create( message
					) );
			value
				.setValue( message
				);

		} catch( TypeCheckingException e1 ) {
			if ( isNumeric( message
			) ) {
				try {
					if ( message
						.equals( "0" ) ) {
						type
							.check( Value
								.create( false ) );
						value
							.setValue( false );

					} else {
						if ( message
							.equals( "1" ) ) {
							type
								.check( Value
									.create( true ) );
							value
								.setValue( true );

						} else {
							throw new TypeCheckingException( "" );

						}
					}
				} catch( TypeCheckingException e ) {
					try {
						value
							.setValue( Integer
								.parseInt( message
								) );

					} catch( NumberFormatException nfe ) {
						try {
							value
								.setValue( Long
									.parseLong( message
									) );

						} catch( NumberFormatException nfe1 ) {
							try {
								value
									.setValue( Double
										.parseDouble( message
										) );

							} catch( NumberFormatException nfe2 ) {
							}
						}
					}
				}
			} else {
				try {
					type
						.check( Value
							.create( new ByteArray( message
								.getBytes() ) ) );
					value
						.setValue( new ByteArray( message
							.getBytes() ) );

				} catch( TypeCheckingException e ) {
					value
						.setValue( message
						);

				}
			}
		}
	}

	private String valueToPlainText( Value value )
	{
		Object valueObject
			= value
				.valueObject();
		String str
			= "";

		if ( valueObject instanceof String ) {
			str
				= ((String) valueObject);

		} else if ( valueObject instanceof Integer ) {
			str
				= ((Integer) valueObject).toString();

		} else if ( valueObject instanceof Double ) {
			str
				= ((Double) valueObject).toString();

		} else if ( valueObject instanceof ByteArray ) {
			str
				= ((ByteArray) valueObject).toString();

		} else if ( valueObject instanceof Boolean ) {
			str
				= ((Boolean) valueObject).toString();

		} else if ( valueObject instanceof Long ) {
			str
				= ((Long) valueObject).toString();

		}

		return str;

	}

	/**
	This method is used to retrieve the content format of the current protocol parameter setting.
	If you mean to get the content format of the Coap Message, please use {@link CoapMessage} method
	@param operationName
	@return 
	 */
	private String getStringContentFormatProtocolParameter( String operationName )
	{
		String stringContentFormat
			= Parameters.DEFAULT_CONTENT_FORMAT;

		if ( protocol
			.hasOperationSpecificParameter( operationName,
				Parameters.CONTENT_FORMAT
			) ) {
			return protocol
				.getOperationSpecificStringParameter( operationName,
					Parameters.CONTENT_FORMAT
				).toLowerCase();

		}
		return stringContentFormat;

	}

	private long getLongContentFormatProtocolParameter( String operationName )
		throws IOException
	{
		String stringContentFormat
			= getStringContentFormatProtocolParameter( operationName
			);

		try {
			return ContentFormat.JOLIE_ALLOWED_CONTENT_FORMAT
				.get( stringContentFormat
				);

		} catch( NullPointerException ex ) {
			throw new IOException( "Content format " + stringContentFormat
				+ " is not allowed!" );

		}
	}

	private int messageTypeProtocolParameter( String operationName )
	{
		int messageType
			= Parameters.DEFAULT_MESSAGE_TYPE;

		if ( protocol
			.hasOperationSpecificParameter( operationName,
				Parameters.MESSAGE_TYPE
			) ) {
			Value messageTypeValue
				= protocol
					.getOperationSpecificParameterFirstValue( operationName,
						Parameters.MESSAGE_TYPE
					);

			if ( messageTypeValue
				.isInt() ) {
				if ( MessageType
					.isMessageType( messageTypeValue
						.intValue() ) ) {
					messageType
						= messageTypeValue
							.intValue();

				} else {
					Interpreter
						.getInstance().logSevere( "Coap Message Type "
							+ messageTypeValue
								.intValue() + " is not allowed! "
							+ "Assuming default message type \"NON\"." );

				}
			} else {
				if ( messageTypeValue
					.isString() ) {
					String messageTypeString
						= messageTypeValue
							.strValue();

					switch( messageTypeString ) {
						case "CON": {
							messageType
								= MessageType.CON;

							break;

						}
						case "NON": {
							messageType
								= MessageType.NON;

							break;

						}
						case "RST": {
							messageType
								= MessageType.RST;

							break;

						}
						case "ACK": {
							messageType
								= MessageType.ACK;

							break;

						}
						default: {
							Interpreter
								.getInstance().logSevere( "Coap Message Type "
									+ messageTypeString
									+ " is not allowed! "
									+ "Assuming default message type \"NON\"." );

							break;

						}
					}
				} else {
					Interpreter
						.getInstance().logSevere( "Coap Message Type "
							+ "cannot  be read as an integer nor as a string! "
							+ "Check the message type." );

				}
			}
		}
		return messageType;

	}

	/**
	Default Code is Integer for POST in request and Integer for CONTENT for responses.
	@param operationName
	@param isResponse
	@return The Integer rapresentation of the Message Code as listed in {@link MessageCode}
	 */
	private int messageCodeProtocolParameter( String operationName, boolean isResponse )
	{
		int messageCode
			= MessageCode.POST;

		if ( isResponse ) {
			messageCode
				= MessageCode.CONTENT_205;

		}
		if ( protocol
			.hasOperationSpecificParameter( operationName,
				Parameters.MESSAGE_CODE
			) ) {
			Value messageCodeValue
				= protocol
					.getOperationSpecificParameterFirstValue( operationName,
						Parameters.MESSAGE_CODE
					);

			if ( messageCodeValue
				.isInt() ) {
				messageCode
					= messageCodeValue
						.intValue();

			} else if ( messageCodeValue
				.isString() ) {
				String messageCodeValueString
					= messageCodeValue
						.strValue().toUpperCase();
				messageCode
					= MessageCode.JOLIE_ALLOWED_MESSAGE_CODE
						.get( messageCodeValueString
						);

			}
		}
		return messageCode;

	}

	private boolean isNumeric( final CharSequence cs )
	{
		if ( cs.length() == 0 ) {
			return false;
		}
		final int sz = cs.length();

		for( int i = 0; i < sz; i++ ) {
			if ( !Character.isDigit( cs.charAt( i ) ) ) {
				return false;
			}
		}
		return true;
	}

	/**
	 * TODO promote to Async Comm Protocol or Comm Channel
	 *
	 * @param operationName
	 * @return
	 */
	public boolean isOneWay( String operationName )
	{
		return protocol.channel().parentPort().getInterface().oneWayOperations().containsKey( operationName );
	}

	/**
	 * TODO promote to Async Comm Protocol or Comm Channel
	 *
	 * @param operationName
	 * @return
	 */
	public boolean isRequestResponse( String operationName )
	{
		return protocol.channel().parentPort().getInterface().requestResponseOperations().containsKey( operationName );
	}

	private URI targetURI( CommMessage in ) throws URISyntaxException
	{
		URI location;
		if ( protocol.isInput ) {
			location = protocol.channel().parentInputPort().location();
		} else {
			location = new URI( protocol.channel().parentOutputPort().locationVariablePath().evaluate().strValue() );
		}

		// 1. build the string for uri resource let url be coap://
		StringBuilder url
			= new StringBuilder( "coap://" );

		// 2. let host be the location host or the option uri-host specified
		String host
			= location
				.getHost();
		// 3. append url to host
		url
			.append( host
			);

		// 4. let port be the location port or the uri port option
		int port
			= location
				.getPort();
		// 5. append colon followed by the decimal representation of port
		url
			.append( ":" ).append( port
		);

		// 6. let resource name be empty, for each uri path option append / and the option
		StringBuilder resource_name
			= new StringBuilder();

		if ( protocol
			.hasOperationSpecificParameter( in
				.operationName(),
				Parameters.ALIAS
			) ) {

			for( Value v
				: protocol
					.getOperationSpecificParameterVector( in
						.operationName(), Parameters.ALIAS
					) ) {
				String path
					= getDynamicAlias( v
						.strValue(), in
							.value() );
				resource_name
					.append( path
					);

			}
		} else {
			resource_name
				.append( location
					.getPath() );

		}

		// 7. if resource name is empty append a single backslash and the operation name
		if ( resource_name
			.length() == 0 ) {
			resource_name
				.append( "/" );
			resource_name
				.append( in
					.operationName() );

		}

		if ( location
			.getQuery() != null ) {
			resource_name
				.append( "?" ).append( location
				.getQuery() );

		}

		// 9. append resource name to url
		url
			.append( resource_name
			);

		// set the string without percents or not allowed chars 
		String uri
			= new URI( url
				.toString() ).toASCIIString();

		return new URI( uri
		);

	}

	private String getDynamicAlias( Value value )
	{
		return getDynamicAlias( "", value );
	}

	private String getDynamicAlias( String start, Value value )
	{
		Set<String> aliasKeys = new TreeSet<>();
		String pattern = "%(!)?\\{[^\\}]*\\}";

		// find pattern
		int offset = 0;
		String currStrValue;

		String currKey;

		StringBuilder result = new StringBuilder( start );
		Matcher m = Pattern.compile( pattern ).matcher( start );

		// substitute in alias
		while( m.find() ) {
			currKey = start.substring( m.start() + 3, m.end() - 1 );
			currStrValue = value.getFirstChild( currKey ).strValue();
			aliasKeys.add( currKey );
			result.replace( m.start() + offset, m.end() + offset, currStrValue );
			offset += currStrValue.length() - 3 - currKey.length();
		}

		// remove from the value
		for( String aliasKey : aliasKeys ) {
			value.children().remove( aliasKey );
		}

		return result.toString();
	}

	private String operationName( CoapMessage in )
	{
		StringBuilder sb = new StringBuilder();

		int i = 1;
		List<OptionValue> options = in.getOptions( Option.URI_PATH );
		sb.append( "/" );

		for( OptionValue option : options ) {
			if ( i < options.size() ) {
				sb.append( option.getDecodedValue() ).append( "/" );
			} else {
				sb.append( option.getDecodedValue() );
			}
			i++;
		}
		String URIPath = sb.toString();
		String operationName = protocol.getOperationFromOperationSpecificStringParameter( Parameters.ALIAS,
			URIPath.substring( 1 ) );

		return operationName;

	}
}
