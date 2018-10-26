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
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;
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
import jolie.lang.Constants;
import jolie.net.coap.communication.codec.CoapMessageDecoder;
import jolie.net.coap.communication.codec.CoapMessageEncoder;
import jolie.net.coap.correlator.CoapMessageCorrelator;
import jolie.net.coap.correlator.CommMessageCorrelator;
import jolie.net.coap.message.CoapMessage;
import jolie.net.coap.message.CoapRequest;
import jolie.net.coap.message.CoapResponse;
import jolie.net.coap.message.MessageCode;
import jolie.net.coap.message.MessageType;
import jolie.net.coap.message.Token;
import jolie.net.coap.message.options.ContentFormat;
import jolie.net.coap.message.options.Option;
import jolie.net.coap.message.options.OptionValue;
import jolie.net.protocols.AsyncCommProtocol;
import jolie.runtime.ByteArray;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.VariablePath;
import jolie.runtime.typing.Type;
import jolie.runtime.typing.TypeCastingException;
import jolie.runtime.typing.TypeCheckingException;
import jolie.xml.XmlUtils;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
Implementations of {@link AsyncCommProtocol} CoAP for Jolie.
-------------------------------------------------------------------------------------
1. COAP MESSAGE INBOUND				CoapDecoder
2. COAP MESSAGE OUTBOUND				CoapEncoder
3. COAP MESSAGE INBOUND/OUTBOUND		CoapToCommMessageCodec
4. COMM MESSAGE INBOUND				StreamingCommChannelHandler
-------------------------------------------------------------------------------------
@author stefanopiozingaro
 */
public class CoapProtocol extends AsyncCommProtocol
{
	private static final Charset DEFAULT_CHARSET = CharsetUtil.UTF_8;
	private boolean isInput;
	private CommMessageCorrelator commMessageCorrelator;
	private CoapMessageCorrelator coapMessageCorrelator;

	/**
	 *
	 * @param configurationPath
	 * @param isInput
	 */
	public CoapProtocol( VariablePath configurationPath, boolean isInput )
	{
		super( configurationPath );
		this.isInput = isInput;
		commMessageCorrelator = new CommMessageCorrelator();
		coapMessageCorrelator = new CoapMessageCorrelator();
	}

	@Override
	public void setupPipeline( ChannelPipeline pipeline )
	{
//		pipeline.addLast( "LOGGER", new LoggingHandler( LogLevel.INFO ) );
		pipeline.addLast( "COAP MESSAGE INBOUND", new CoapMessageDecoder() );
		pipeline.addLast( "COAP MESSAGE OUTBOUND", new CoapMessageEncoder() );
		pipeline.addLast( "COAP MESSAGE INBOUND/OUTBOUND", new CoapToCommMessageCodec() );
	}

	@Override
	public String name()
	{
		return "coap";
	}

	@Override
	public boolean isThreadSafe()
	{
		return true;
	}

	private class CoapToCommMessageCodec extends MessageToMessageCodec<CoapMessage, CommMessage>
	{
		private static final String TIMEOUT_HANLDER_NAME = "READ TIMEOUT INBOUND/OUTBOUND";
		private ChannelHandlerContext ctx;

		@Override
		public void exceptionCaught( ChannelHandlerContext ctx, Throwable cause ) throws Exception
		{
			if ( cause instanceof ReadTimeoutException ) {
				CommMessage msg = null;
				if ( CommMessageCorrelator.request != null ) {
					msg = CommMessageCorrelator.request;
					// fault message to comm core
					CommMessage fault = CommMessage.createFaultResponse( msg, new FaultException( cause ) );
					ctx.fireChannelRead( fault );
					// reset message to server
					CoapMessage reset = CoapMessage.createEmptyReset( (int) msg.id() );
					ctx.writeAndFlush( reset );
					CommMessageCorrelator.request = null;
				}
				ctx.pipeline().remove( TIMEOUT_HANLDER_NAME );
			} else {
				super.exceptionCaught( ctx, cause );
			}
		}

		@Override
		public void channelRead( ChannelHandlerContext ctx, Object msg ) throws Exception
		{
			if ( acceptInboundMessage( msg ) ) {
				if ( isInput && ((CoapMessage) msg).isEmptyAck() ) {
					CoapMessage request = coapMessageCorrelator.sendProtocolResponse( ((CoapMessage) msg).id() );
					if ( request.messageType() == MessageType.NON ) {
						// the special case: this is a comm message ack flowing from comm core for NON coap message and we shall release it
						ReferenceCountUtil.release( msg );
					}
					return;
				}
				super.channelRead( ctx, msg );
			} else {
				ctx.fireChannelRead( msg ); // maybe retain maybe not
			}
		}

		@Override
		protected void encode( ChannelHandlerContext ctx, CommMessage in, List<Object> out )
			throws Exception
		{
			setSendExecutionThread( in.id() );
			this.ctx = ctx;

			if ( isInput ) {
				out.add( encode_inbound( in ) );
			} else {
				out.add( encode_outbound( in ) );
			}
		}

		@Override
		protected void decode( ChannelHandlerContext ctx, CoapMessage in, List<Object> out )
			throws Exception
		{
			long id = (long) in.id();
			if ( in.token().getBytes().length != 0 ) {
				id = ByteBuffer.wrap( in.token().getBytes() ).getLong();
			}
			setReceiveExecutionThread( id );
			this.ctx = ctx;

			if ( isInput ) {
				out.add( decode_inbound( in ) );
			} else {
				out.add( decode_outbound( in ) );
			}
		}

		private CoapMessage encode_outbound( CommMessage in ) throws URISyntaxException
		{
			String operationName = in.operationName();

			CoapRequest out = new CoapRequest(
				messageTypeProtocolParameter( operationName ),
				messageCodeProtocolParameter( operationName, false ),
				targetURI( in )
			);

			if ( MessageCode.allowsContent( out.messageCode() ) ) {
				out.setContent(
					valueToByteBuf(
						in,
						ContentFormat.CONTENT_FORMAT.get( longContentFormatProtocolParameter( in.operationName() ) ),
						DEFAULT_CHARSET ),
					longContentFormatProtocolParameter( operationName )
				);
			}

			out.id( (int) in.id() );
			int timeout = Parameters.DEFAULT_TIMEOUT;
			if ( hasParameter( Parameters.TIMEOUT ) ) {
				timeout = getIntParameter( Parameters.TIMEOUT );
			}
			if ( isRequestResponse( operationName ) ) {
				if ( checkBooleanParameter( Parameters.DEBUG ) ) {
					Interpreter.getInstance().logInfo( "Receiving a Comm Message Request for a CoAP Solicit Response:\n"
						+ in.toPrettyString() );
				}
				commMessageCorrelator.sendProtocolRequest( in );
				out.token( new Token( ByteBuffer.allocate( 8 ).putLong( in.id() ).array() ) );
				if ( checkBooleanParameter( Parameters.DEBUG ) ) {
					Interpreter.getInstance().logInfo( "Sending the CoAP Solicit Response:\n"
						+ out );
				}
				CommMessageCorrelator.request = in;
				if ( !ctx.pipeline().names().contains( TIMEOUT_HANLDER_NAME ) ) {
					ctx.pipeline().addFirst( TIMEOUT_HANLDER_NAME, new ReadTimeoutHandler( timeout ) );
				}
			} else {
				if ( checkBooleanParameter( Parameters.DEBUG ) ) {
					Interpreter.getInstance().logInfo( "Receiving a Comm Message Request for a CoAP Notification:\n"
						+ in.toPrettyString() );
				}
				if ( out.messageType() == MessageType.CON ) {
					CommMessageCorrelator.request = in;
					if ( !ctx.pipeline().names().contains( TIMEOUT_HANLDER_NAME ) ) {
						ctx.pipeline().addFirst( TIMEOUT_HANLDER_NAME, new ReadTimeoutHandler( timeout ) );
					}
				}
				if ( out.messageType() == MessageType.NON ) {
					CommMessage ack = new CommMessage(
						in.id(),
						in.operationName(),
						Constants.ROOT_RESOURCE_PATH,
						Value.create(),
						null
					);
					ctx.fireChannelRead( ack );
					if ( checkBooleanParameter( Parameters.DEBUG ) ) {
						Interpreter.getInstance().logInfo( "NON confirmable CoAP Notifications!\n"
							+ "Sending ACK to the Comm Core:\n"
							+ ack.toPrettyString() );
					}
				}
				if ( checkBooleanParameter( Parameters.DEBUG ) ) {
					Interpreter.getInstance().logInfo( "Sending the CoAP Notification:\n"
						+ out );
				}
			}

			return out;
		}

		private CommMessage decode_inbound( CoapMessage in )
			throws TypeCastingException, IOException
		{
			String operationName = operationName( in );
			long id = (long) in.id();
			if ( isRequestResponse( operationName ) ) {
				if ( checkBooleanParameter( Parameters.DEBUG ) ) {
					Interpreter.getInstance().logInfo( "Receiving a CoAP Solicit Response:\n"
						+ in );
				}
				if ( in.token().getBytes().length != 0 ) {
					id = ByteBuffer.wrap( in.token().getBytes() ).getLong();
				}
			} else {
				if ( checkBooleanParameter( Parameters.DEBUG ) ) {
					Interpreter.getInstance().logInfo( "Receiving a CoAP Notification:\n"
						+ in );
				}
			}

			Value v = Value.create();
			if ( MessageCode.allowsContent( in.messageCode() ) && !in.getContent().equals( Unpooled.EMPTY_BUFFER ) ) {
				String format = in.contentFormat();
				v = byteBufToValue(
					in.getContent(),
					getSendType( operationName ),
					format,
					DEFAULT_CHARSET,
					checkStringParameter( Parameters.JSON_ENCODING, "strict" )
				);
			}

			coapMessageCorrelator.receiveProtocolRequest( (int) id, in );
			CommMessage out = new CommMessage( id, operationName, Constants.ROOT_RESOURCE_PATH, v, null );

			if ( isRequestResponse( operationName ) ) {
				if ( checkBooleanParameter( Parameters.DEBUG ) ) {
					Interpreter.getInstance().logInfo( "Forwading the CoAP Solicit Response to Comm Core:\n"
						+ out.toPrettyString() );
				}
			} else {
				if ( checkBooleanParameter( Parameters.DEBUG ) ) {
					Interpreter.getInstance().logInfo( "Forwading the CoAP Notification to Comm Core:\n"
						+ out.toPrettyString() );
				}
			}

			return out;
		}

		private CoapMessage encode_inbound( CommMessage in )
			throws IOException
		{
			CoapMessage out = null;
			String operationName = in.operationName();

			if ( isRequestResponse( operationName ) ) {
				if ( checkBooleanParameter( Parameters.DEBUG ) ) {
					Interpreter.getInstance().logInfo( "Receiving a Comm Message Response from Comm Core:\n"
						+ in.toPrettyString() );
				}

				out = new CoapResponse(
					MessageType.ACK,
					messageCodeProtocolParameter( operationName, true ) );
				out.id( (int) in.id() );
				if ( getOperationSpecificBooleanParameter( operationName, Parameters.SEPARATE_RESPONSE ) ) {
					out.messageType( messageTypeProtocolParameter( operationName ) );
					out.randomId();
					out.token( new Token( ByteBuffer.allocate( 8 ).putLong( in.id() ).array() ) ); // id of the message ;-)
				}

				// content
				if ( MessageCode.allowsContent( out.messageCode() ) ) {
					ByteBuf content = valueToByteBuf( in,
						ContentFormat.CONTENT_FORMAT.get(
							longContentFormatProtocolParameter( in.operationName() ) ), DEFAULT_CHARSET );
					out.setContent( content, longContentFormatProtocolParameter( operationName ) );
				}
				if ( checkBooleanParameter( Parameters.DEBUG ) ) {
					Interpreter.getInstance().logInfo( "Sending the CoAP Response to the Client:\n"
						+ out );
				}

			} else {
				if ( checkBooleanParameter( Parameters.DEBUG ) ) {
					Interpreter.getInstance().logInfo( "Receiving a Comm Message Ack from Comm Core:\n"
						+ in.toPrettyString() );
				}
				out = CoapMessage.createEmptyAcknowledgement( (int) in.id() );
				if ( checkBooleanParameter( Parameters.DEBUG ) ) {
					Interpreter.getInstance().logInfo( "Sending a CoAP Empty Ack, "
						+ "in case of a NON request, this message will not be delivered to the Client:\n"
						+ out );
				}

			}
			return out;
		}

		private CommMessage decode_outbound( CoapMessage in )
			throws IOException, TypeCastingException
		{
			long key = (long) in.id();
			CommMessage commMessageRequest = commMessageCorrelator.receiveProtocolResponse( key, false );
			if ( commMessageRequest == null ) {
				try {
					key = ByteBuffer.wrap( in.token().getBytes() ).getLong();
				} catch( Exception ex ) {

				}
				commMessageRequest = commMessageCorrelator.receiveProtocolResponse( key );
				if ( commMessageCorrelator == null ) {
					throw new IOException( "Non correlating comm message with any requests" );
				}
			}

			String operationName = commMessageRequest.operationName();
			CommMessage out = null;

			if ( isRequestResponse( operationName ) ) {
				if ( in.isEmptyAck() ) {
					if ( checkBooleanParameter( Parameters.DEBUG ) ) {
						Interpreter.getInstance().logInfo( "Receiving the CoAP Empty Ack of a Solicit Reponse from the Server:\n" + in );
					}
					throw new IOException( "this is an ack message that Jolie does not have to handle!" );
				} else {
					if ( in.isAck() ) {
						if ( checkBooleanParameter( Parameters.DEBUG ) ) {
							Interpreter.getInstance().logInfo( "Receiving the CoAP Piggyback Response from the Server:\n" + in );
						}
						// so the comm message matches with the id
					} else {
						if ( checkBooleanParameter( Parameters.DEBUG ) ) {
							Interpreter.getInstance().logInfo( "Receiving the CoAP Separate Response from the Server:\n" + in );
						}
						// so the comm message matches with the token
					}
					Value v = Value.create();
					if ( MessageCode.allowsContent( in.messageCode() ) && !in.getContent().equals( Unpooled.EMPTY_BUFFER ) ) {

						String format = ContentFormat.CONTENT_FORMAT.get(
							((long) in.getOptions( Option.CONTENT_FORMAT ).get( 0 ).getDecodedValue())
						);

						v = byteBufToValue(
							in.getContent(),
							getSendType( operationName ),
							format,
							DEFAULT_CHARSET,
							checkStringParameter( Parameters.JSON_ENCODING, "strict" )
						);
					}
					out = CommMessage.createResponse( commMessageRequest, v );
					if ( checkBooleanParameter( Parameters.DEBUG ) ) {
						Interpreter.getInstance().logInfo( "Sending a Comm Message Response to Comm Core:\n"
							+ out.toPrettyString() );
					}
				}
			} else {
				if ( in.isEmptyAck() ) {
					if ( checkBooleanParameter( Parameters.DEBUG ) ) {
						Interpreter.getInstance().logInfo( "Receiving the CoAP Empty Ack of a Notification from the Server:\n" + in );
					}
					out = CommMessage.createEmptyResponse( commMessageRequest );
					if ( checkBooleanParameter( Parameters.DEBUG ) ) {
						Interpreter.getInstance().logInfo( "Sending a Comm Message Ack to Comm Core:\n" + out.toPrettyString() );
					}
				}
			}

			if ( in.messageType() == MessageType.RST ) {
				out = CommMessage.createFaultResponse( commMessageRequest, new FaultException( "Received a RESET from a CoAP Client!" ) );
			}

			return out;
		}

		private String operationName( CoapMessage in )
		{
			StringBuilder sb = new StringBuilder();

			int i = 1;
			List<OptionValue> options = in.getOptions( Option.URI_PATH );

			for( OptionValue option : options ) {
				if ( i < options.size() ) {
					sb.append( option.getDecodedValue() ).append( '/' );
				} else {
					sb.append( option.getDecodedValue() );
				}
				i++;
			}
			String URIPath = sb.toString();
			String operationName = getOperationFromOperationSpecificStringParameter( Parameters.ALIAS,
				URIPath );

			return operationName;

		}

		/**
	This method is used to retrieve the content format of the current protocol parameter setting.
	If you mean to get the content format of the Coap Message, please use {@link CoapMessage} method
	@param operationName
	@return 
		 */
		private String stringContentFormatProtocolParameter( String operationName )
		{
			String stringContentFormat
				= Parameters.DEFAULT_CONTENT_FORMAT;

			if ( hasOperationSpecificParameter( operationName,
				Parameters.CONTENT_FORMAT
			) ) {
				return getOperationSpecificStringParameter( operationName,
					Parameters.CONTENT_FORMAT
				).toLowerCase();

			}
			return stringContentFormat;

		}

		private long longContentFormatProtocolParameter( String operationName )
		{
			String stringContentFormat = stringContentFormatProtocolParameter( operationName );

			try {
				return ContentFormat.JOLIE_ALLOWED_CONTENT_FORMAT.get( stringContentFormat );

			} catch( NullPointerException ex ) {
				Interpreter.getInstance().logSevere( "Content format " + stringContentFormat
					+ " is not allowed! JSON will be used instead." );
			}
			return ContentFormat.APP_JSON;
		}

		private int messageTypeProtocolParameter( String operationName )
		{
			int messageType
				= Parameters.DEFAULT_MESSAGE_TYPE;

			if ( hasOperationSpecificParameter( operationName,
				Parameters.MESSAGE_TYPE
			) ) {
				Value messageTypeValue
					= getOperationSpecificParameterFirstValue( operationName,
						Parameters.MESSAGE_TYPE
					);

				if ( messageTypeValue
					.isInt() ) {
					if ( MessageType
						.isValidMessageType( messageTypeValue
							.intValue() ) ) {
						messageType
							= messageTypeValue
								.intValue();

					} else {
						throw new IllegalArgumentException( "Coap Message Type "
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
								throw new IllegalArgumentException( "Coap Message Type "
									+ messageTypeString
									+ " is not allowed! "
									+ "Assuming default message type \"NON\"." );
							}
						}
					} else {
						throw new IllegalArgumentException( "Coap Message Type "
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
			if ( hasOperationSpecificParameter( operationName,
				Parameters.MESSAGE_CODE
			) ) {
				Value messageCodeValue
					= getOperationSpecificParameterFirstValue( operationName,
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

		private URI targetURI( CommMessage in ) throws URISyntaxException
		{
			URI location = null;
			if ( isInput ) {
				location = channel().parentInputPort().location();
			} else {
				location = new URI( channel().parentOutputPort().locationVariablePath().evaluate().strValue() );
			}

			// 1. build the string for uri resource let url be coap://
			StringBuilder url = new StringBuilder( "coap://" );
			// 2. let host be the location host or the option uri-host specified
			String host = location.getHost();
			// 3. append url to host
			url.append( host );
			// 4. let port be the location port or the uri port option
			int port = location.getPort();
			// 5. append colon followed by the decimal representation of port
			url.append( ":" ).append( port );
			// 6. let resource name be empty, for each uri path option append / and the option
			StringBuilder resource_name = new StringBuilder();
			if ( hasOperationSpecificParameter( in.operationName(), Parameters.ALIAS ) ) {
				resource_name.append( '/' );
				for( Value v : getOperationSpecificParameterVector( in.operationName(), Parameters.ALIAS ) ) {
					String path = dynamicAlias( v.strValue(), in.value() );
					resource_name.append( path );
				}
			} else {
				resource_name.append( location.getPath() );
			}
			// 7. if resource name is empty append a single backslash and the operation name
			if ( resource_name.length() == 0 ) {
				resource_name.append( '/' );
				resource_name.append( in.operationName() );
			}
			if ( location.getQuery() != null ) {
				resource_name.append( "?" ).append( location.getQuery() );
			}
			// 8. append resource name to url
			url.append( resource_name );

			// 9. set the string without percents or not allowed chars 
			String uri = null;
			try {
				uri = new URI( url.toString() ).toASCIIString();
			} catch( URISyntaxException ex ) {
				// do nothing
			}

			try {
				return new URI( uri );
			} catch( URISyntaxException ex ) {
				// do nothing
			}
			return null;
		}
	}

	/**
	TODO Promote to {@link AsyncCommProtocol}
	@param commMessage
	@param format
	@return
	@throws IOException
	@throws ParserConfigurationException
	@throws TransformerConfigurationException
	@throws TransformerException 
	 */
	private ByteBuf valueToByteBuf( CommMessage commMessage, String format, Charset charset )
	{
		ByteBuf byteBuf
			= Unpooled
				.buffer();
		Value v
			= commMessage
				.isFault() ? Value
					.create( commMessage
						.fault().getMessage() ) : commMessage
					.value();

		try {
			switch( format ) {
				case "application/link-format": // TODO support it!
					break;

				case "application/xml":
					DocumentBuilder db
						= DocumentBuilderFactory
							.newInstance().newDocumentBuilder();
					Document doc
						= db
							.newDocument();
					Element root
						= doc
							.createElement( commMessage
								.operationName() );
					doc
						.appendChild( root
						);
					XmlUtils
						.valueToDocument( v,
							root,
							doc
						);
					Source src
						= new DOMSource( doc
						);
					ByteArrayOutputStream strm
						= new ByteArrayOutputStream();
					Result dest
						= new StreamResult( strm
						);
					Transformer trf
						= TransformerFactory
							.newInstance().newTransformer();
					trf
						.setOutputProperty( OutputKeys.ENCODING,
							charset
								.name() );
					trf
						.transform( src,
							dest
						);
					byteBuf
						.writeBytes( strm
							.toByteArray() );

					break;

				case "application/octet-stream":
				case "application/exi":
				case "text/plain":
					byteBuf
						.writeBytes( valueToPlainText( v
						).getBytes( charset
						) );

					break;

				case "application/json":
					StringBuilder jsonStringBuilder
						= new StringBuilder();
					JsUtils
						.valueToJsonString( v,
							true, getSendType( commMessage
								.operationName() ), jsonStringBuilder
						);
					byteBuf
						.writeBytes( jsonStringBuilder
							.toString().getBytes( charset
							) );

					break;

			}
		} catch( IOException
			| IllegalArgumentException
			| ParserConfigurationException
			| TransformerException
			| DOMException ex ) {

		}
		return byteBuf;

	}

	/**
	TODO Promote to {@link AsyncCommProtocol}
	@param in
	@param type
	@param format
	@param charset
	@param jsonEncoding
	@return
	@throws IOException
	@throws ParserConfigurationException
	@throws SAXException
	@throws TypeCheckingException 
	 */
	private Value byteBufToValue( ByteBuf in, Type type, String format, Charset charset, boolean jsonEncoding )
		throws TypeCastingException
	{
		ByteBuf byteBuf = Unpooled.copiedBuffer( in );
		Value value = Value.create();
		String message = Unpooled.copiedBuffer( byteBuf ).toString( charset );

		try {
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
					case "text/plain": {
						parsePlainText( message, value, type );
					}
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
		} catch( IOException | ParserConfigurationException | TypeCheckingException | SAXException ex ) {
			// do nothing
		}
		return value;
	}

	/**
	TODO Promote to {@link AsyncCommProtocol}
	@param message
	@param value
	@param type
	@throws TypeCheckingException 
	 */
	private void parsePlainText( String message, Value value, Type type )
		throws TypeCheckingException
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

	/**
	TODO Promote to {@link AsyncCommProtocol}
	@param value
	@return 
	 */
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
	TODO Promote to {@link AsyncCommProtocol}
	@param cs
	@return 
	 */
	private boolean isNumeric( final CharSequence cs )
	{
		if ( cs
			.length() == 0 ) {
			return false;

		}
		final int sz
			= cs
				.length();

		for( int i
			= 0; i
			< sz;
			i++ ) {
			if ( !Character
				.isDigit( cs
					.charAt( i
					) ) ) {
				return false;

			}
		}
		return true;

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
		private static final String SEPARATE_RESPONSE = "separateResponse";
	}
}
