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
package jolie.net;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.CharsetUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
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
import jolie.ExecutionThread;

import jolie.Interpreter;
import jolie.net.coap.message.CoapMessage;
import jolie.net.coap.message.CoapRequest;
import jolie.net.coap.message.options.ContentFormat;
import jolie.net.coap.message.MessageCode;
import jolie.net.coap.message.MessageType;
import jolie.net.coap.message.options.Option;
import jolie.runtime.ByteArray;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.typing.Type;
import jolie.runtime.typing.TypeCastingException;
import jolie.runtime.typing.TypeCheckingException;
import jolie.js.JsUtils;
import jolie.net.coap.application.linkformat.LinkParam;
import jolie.net.coap.application.linkformat.LinkValueList;
import jolie.net.coap.message.CoapResponse;
import jolie.net.coap.communication.dispatching.Token;
import jolie.net.coap.message.options.OptionValue;
import jolie.runtime.ValuePrettyPrinter;
import jolie.runtime.ValueVector;
import jolie.xml.XmlUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class CoapToCommMessageCodec
	extends MessageToMessageCodec<CoapMessage, CommMessage> {

	private static final Charset charset = CharsetUtil.UTF_8;
	private static final String DEFAULT_CONTENT_FORMAT = "application/json";

	private final boolean isInput;
	private final CoapProtocol protocol;

	private int correlationID;
	private Token correlationToken;
	private CommMessage commMessageRequest;
	private ExecutionThread eThread;

	public CoapToCommMessageCodec( CoapProtocol protocol ) {
		this.correlationID = -1;
		this.protocol = protocol;
		this.isInput = protocol.isInput;
	}

	@Override
	public void exceptionCaught( ChannelHandlerContext ctx, Throwable cause ) throws Exception {
		if ( cause instanceof ReadTimeoutException ) {
			throw new IOException(
				"The set timeout for Acknowledgment message expired"
				+ " for channel on location: '" + this.protocol.channel().getLocation().toString()
				+ "' protocol: '" + this.protocol.name() + "'" );
		} else {
			Interpreter.getInstance().logSevere( cause.getMessage() );
		}
		ctx.close();
	}

	@Override
	protected void encode( ChannelHandlerContext ctx, CommMessage in,
		List<Object> out ) throws Exception {
		if ( in.executionThread() != null && !in.hasGenericId() ) {
			this.eThread = in.executionThread();
			this.protocol.setExecutionThread( this.eThread );
			if ( this.protocol.checkBooleanParameter( Parameters.DEBUG ) ) {
				Interpreter.getInstance().logInfo( "Setting the Thread to be the "
					+ "Execution Thread " + this.eThread );
			}
		}
		out.add( this.encode_internal( ctx, in ) );
	}

	@Override
	protected void decode( ChannelHandlerContext ctx, CoapMessage in,
		List<Object> out ) throws Exception {
		out.add( this.decode_internal( in ) );
	}

	private CoapMessage encode_internal( ChannelHandlerContext ctx,
		CommMessage commMessage ) {

		String operationName = commMessage.operationName();
		int messageType = this.messageType( operationName );

		if ( this.isInput ) {

			if ( this.isOneWay( operationName ) ) {

				if ( this.protocol.checkBooleanParameter( Parameters.DEBUG ) ) {
					Interpreter.getInstance().logInfo( "Receiving an ack from jolie "
						+ "--> forward it to the client" );
				}
				return CoapMessage.createEmptyAcknowledgement( this.correlationID );

			} else {

				if ( this.protocol.checkBooleanParameter( Parameters.DEBUG ) ) {
					Interpreter.getInstance().logInfo( "Receiving a response from jolie "
						+ "--> forwarding it to the client" );
				}

				int messageCode = this.getMessageCode( operationName, true );
				CoapMessage msg = new CoapResponse( MessageType.NON, messageCode );
				msg.setMessageId( this.correlationID );
				msg.setToken( this.correlationToken );

				if ( MessageCode.allowsContent( messageCode ) ) {
					ByteBuf content;
					try {
						content = this.valueToByteBuf( commMessage );
						msg.setContent( content, this.getContentFormat( operationName ) );
					} catch ( IOException | ParserConfigurationException | TransformerException ex ) {
						Interpreter.getInstance().logSevere( ex.getMessage() );
						ctx.close();
					}
				}
				if ( this.protocol.checkBooleanParameter( Parameters.DEBUG ) ) {
					Interpreter.getInstance().logInfo( "Sending CoapResponse\n" + msg );
				}

				return msg;
			}
		} else {

			this.commMessageRequest = commMessage;

			if ( this.protocol.checkBooleanParameter( Parameters.DEBUG ) ) {
				Interpreter.getInstance().logInfo( "Receiving a request from jolie "
					+ "--> forwarding it to the server" );
			}

			URI targetURI = null;
			try {
				targetURI = this.getTargetURI( commMessage );
				if ( this.protocol.checkBooleanParameter( Parameters.DEBUG ) ) {
					Interpreter.getInstance().logInfo( "Complete URI Target of the resource: "
						+ targetURI );
				}
			} catch ( URISyntaxException ex ) {
				Interpreter.getInstance().logSevere( ex.getMessage() );
			}

			int messageCode = this.getMessageCode( operationName, false );

			CoapRequest msg = null;
			try {
				msg = new CoapRequest(
					messageType,
					messageCode,
					targetURI
				);
			} catch ( IllegalArgumentException ex ) {
				Interpreter.getInstance().logSevere( ex.getMessage() );
				ctx.close();
			}

			msg.setRandomMessageID();
			this.protocol.addExecutionThread( new Long( msg.getMessageID() ), this.eThread );
			if ( this.protocol.checkBooleanParameter( Parameters.DEBUG ) ) {
				Interpreter.getInstance().logInfo( "Adding the ExecutionThread "
					+ this.eThread + " to the map, having key " + msg.getMessageID() );
			}

			this.correlationID = msg.getMessageID();

			if ( !this.isOneWay( operationName ) ) {
				msg.setMessageType( MessageType.CON );
				msg.setRandomToken();
				this.correlationToken = msg.getToken();
			}

			if ( MessageCode.allowsContent( messageCode ) ) {
				try {
					msg.setContent(
						this.valueToByteBuf( commMessage ),
						this.getContentFormat( operationName )
					);
				} catch ( IOException | ParserConfigurationException | TransformerException ex ) {
					Interpreter.getInstance().logSevere( ex.getMessage() );
				}
			}

			if ( messageType == MessageType.CON ) {
				if ( this.protocol.hasParameter( Parameters.TIMEOUT ) ) {
					int timeout = this.protocol.getIntParameter( Parameters.TIMEOUT );
					ctx.channel().pipeline().addLast( "readTimeoutHandler", new ReadTimeoutHandler( timeout ) );
				}
			}

			if ( this.protocol.checkBooleanParameter( Parameters.DEBUG ) ) {
				Interpreter.getInstance().logInfo( "Sending CoapRequest\n" + msg );
			}

			if ( this.isOneWay( operationName ) && messageType == MessageType.NON ) {
				CommMessage ack = new CommMessage(
					commMessage.id(),
					commMessage.operationName(),
					"/",
					Value.create(),
					null
				);
				ctx.fireChannelRead( ack );
			}

			return msg;
		}
	}

	private CommMessage decode_internal( CoapMessage coapMessage )
		throws IOException,
		ParserConfigurationException,
		SAXException,
		TypeCheckingException {

		if ( this.isInput ) {

			this.eThread = this.protocol.getInitExecutionThread();
			this.protocol.setExecutionThread( this.eThread );
			if ( this.protocol.checkBooleanParameter( Parameters.DEBUG ) ) {
				Interpreter.getInstance().logInfo( "Setting the Thread to be the "
					+ "Init Execution Thread: " + this.eThread );
			}

			if ( this.protocol.checkBooleanParameter( Parameters.DEBUG ) ) {
				Interpreter.getInstance().logInfo( "Receiving a request from a client coap "
					+ "--> forwarding it to the comm core" );
			}

			String operationName = this.getOperationName( coapMessage );
			Value v = Value.create();
			if ( MessageCode.allowsContent( coapMessage.getMessageCode() )
				&& !coapMessage.getContent().equals( Unpooled.EMPTY_BUFFER ) ) {
				String format
					= ContentFormat.CONTENT_FORMAT.get(
						( ( long ) coapMessage.getOptions( Option.CONTENT_FORMAT ).get( 0 ).getDecodedValue() )
					);
				if ( this.protocol.checkBooleanParameter( Parameters.DEBUG ) ) {
					Interpreter.getInstance().logInfo( "The Message has a content in the format: "
						+ format );
				}
				v = this.byteBufToValue( coapMessage.getContent(), this.protocol.getSendType( operationName ), format );
			}

			this.correlationID = coapMessage.getMessageID();
			this.protocol.addExecutionThread( new Long( this.correlationID ), this.eThread );
			if ( this.protocol.checkBooleanParameter( Parameters.DEBUG ) ) {
				Interpreter.getInstance().logInfo( "Adding the ExecutionThread "
					+ this.eThread + " to the map, having key " + this.correlationID );
			}
			if ( this.protocol.checkBooleanParameter( Parameters.DEBUG ) ) {
				Interpreter.getInstance().logInfo( "Storing id for later correlation: " + this.correlationID );
			}
			if ( !this.isOneWay( operationName ) ) {
				this.correlationToken = coapMessage.getToken();
				if ( this.protocol.checkBooleanParameter( Parameters.DEBUG ) ) {
					Interpreter.getInstance().logInfo( "Storing token for later correlation: " + this.correlationToken );
				}
			}

			return CommMessage.createRequest( operationName, "/", v );

		} else {

			if ( this.protocol.hasExecutionThread( new Long( coapMessage.getMessageID() ) ) ) {
				this.eThread = this.protocol.getExecutionThread( new Long( coapMessage.getMessageID() ) );
				this.protocol.setExecutionThread( this.eThread );
				if ( this.protocol.checkBooleanParameter( Parameters.DEBUG ) ) {
					Interpreter.getInstance().logInfo( "Setting the Thread to be the "
						+ " Execution Thread " + this.eThread + ", extraceted from the map with key "
						+ coapMessage.getMessageID() );
				}
			} else {
				throw new IOException(
					"Found no matching sender for message id: " + coapMessage.getMessageID()
					+ " for threadSafe channel on location: '" + this.protocol.channel().getLocation().toString()
					+ "' protocol: '" + this.protocol.name() + "'" );
			}

			if ( this.protocol.checkBooleanParameter( Parameters.DEBUG ) ) {
				Interpreter.getInstance().logInfo( "Receiving a response form a server coap "
					+ "--> forwarding it to the comm core" );
				Interpreter.getInstance().logInfo( coapMessage.toString() );
			}

			String operationName = this.commMessageRequest.operationName();
			Value v = Value.create();
			if ( MessageCode.allowsContent( coapMessage.getMessageCode() )
				&& !coapMessage.getContent().equals( Unpooled.EMPTY_BUFFER ) ) {
				String format
					= ContentFormat.CONTENT_FORMAT.get(
						( ( long ) coapMessage.getOptions( Option.CONTENT_FORMAT ).get( 0 ).getDecodedValue() )
					);
				if ( this.protocol.checkBooleanParameter( Parameters.DEBUG ) ) {
					Interpreter.getInstance().logInfo( "The Message has a content in the format: "
						+ format );
				}
				v = this.byteBufToValue( coapMessage.getContent(), this.protocol.getSendType( operationName ), format );
			}

			if ( coapMessage.isAck()
				&& this.commMessageRequest != null
				&& this.correlationID != -1
				&& this.correlationID == coapMessage.getMessageID()
				&& coapMessage.getToken().equals( this.correlationToken ) ) {

				// PIGGYBACK RESPONSE
				return CommMessage.createResponse( this.commMessageRequest, v );

			} else {

				if ( coapMessage.isResponse()
					&& coapMessage.getToken().equals( this.correlationToken ) ) {

					// SEPARATE RESPONSE
					return CommMessage.createResponse( this.commMessageRequest, v );

				} else {

					if ( coapMessage.isAck()
						&& this.commMessageRequest != null
						&& this.correlationID != -1
						&& this.correlationID == coapMessage.getMessageID() ) {

						if ( this.protocol.checkBooleanParameter( Parameters.DEBUG ) ) {
							Interpreter.getInstance().logInfo( "Coap Acknowledgement Message "
								+ "matching correlation with id: " + this.correlationID );
						}

						// ACK
						return new CommMessage(
							this.commMessageRequest.id(),
							operationName,
							"/",
							v,
							null
						);

					} else {

						return new CommMessage(
							this.correlationID,
							operationName,
							"/",
							v,
							new FaultException(
								new IOException( "Error in decoding a coap message response!" )
							)
						);
					}
				}
			}
		}
	}

	/**
	 * TODO : Promote for higher level of abstarction
	 *
	 * @param commMessage
	 * @return
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws TransformerConfigurationException
	 * @throws TransformerException
	 */
	private ByteBuf valueToByteBuf( CommMessage commMessage ) throws IOException,
		ParserConfigurationException, TransformerConfigurationException,
		TransformerException {

		ByteBuf byteBuf = Unpooled.buffer();
		String format = ContentFormat.CONTENT_FORMAT.get( this.getContentFormat( commMessage.operationName() ) );
		Value v = commMessage.isFault() ? Value.create( commMessage.fault().getMessage() ) : commMessage.value();
		switch ( format ) {
			case "application/link-format": // TODO support it!
				break;
			case "application/xml":
				DocumentBuilder db = DocumentBuilderFactory.newInstance()
					.newDocumentBuilder();
				Document doc = db.newDocument();
				Element root = doc.createElement( commMessage.operationName() );
				doc.appendChild( root );
				XmlUtils.valueToDocument( v, root, doc );
				Source src = new DOMSource( doc );
				ByteArrayOutputStream strm = new ByteArrayOutputStream();
				Result dest = new StreamResult( strm );
				Transformer trf = TransformerFactory.newInstance().newTransformer();
				trf.setOutputProperty( OutputKeys.ENCODING, charset.name() );
				trf.transform( src, dest );
				byteBuf.writeBytes( strm.toByteArray() );
				break;
			case "application/octet-stream":
			case "application/exi":
			case "text/plain":
				byteBuf.writeBytes( this.valueToPlainText( v ).getBytes( charset ) );
				break;
			case "application/json":
				StringBuilder jsonStringBuilder = new StringBuilder();
				JsUtils.valueToJsonString( v, true, this.protocol.getSendType( commMessage.operationName() ),
					jsonStringBuilder );
				byteBuf.writeBytes( jsonStringBuilder.toString().getBytes( charset ) );
				break;
		}
		if ( this.protocol.checkBooleanParameter( Parameters.DEBUG ) ) {
			Interpreter.getInstance().logInfo( "Sending " + format.toUpperCase()
				+ " message: " + Unpooled.wrappedBuffer( byteBuf ).toString( charset ) );
		}
		return byteBuf;
	}

	private String getFormat( String operationName ) {
		String format = DEFAULT_CONTENT_FORMAT;
		if ( this.protocol.hasOperationSpecificParameter( operationName, Parameters.CONTENT_FORMAT ) ) {
			format = this.protocol.getOperationSpecificStringParameter( operationName, Parameters.CONTENT_FORMAT ).toLowerCase();
			if ( ContentFormat.JOLIE_ALLOWED_CONTENT_FORMAT.containsKey( format ) ) {
				return format;
			} else {
				Interpreter.getInstance().logSevere( "The specified content format "
					+ format + " is not supported! \"text/plain\" format will be used instead."
					+ "Currently supported formats are: " );
				for ( Map.Entry<String, Long> f : ContentFormat.JOLIE_ALLOWED_CONTENT_FORMAT.entrySet() ) {
					String key = f.getKey();
					Interpreter.getInstance().logSevere( key );
				}
			}
		}
		return format;
	}

	private long getContentFormat( String operationName ) {
		String format = this.getFormat( operationName );
		Long contentFormat = ContentFormat.APP_JSON;

		if ( ContentFormat.JOLIE_ALLOWED_CONTENT_FORMAT.containsKey( format ) ) {
			contentFormat = ContentFormat.JOLIE_ALLOWED_CONTENT_FORMAT.get( format );
		} else {
			Interpreter.getInstance().logSevere( "The specified content format "
				+ format + " is not supported! \"application/json\" format will be used instead."
				+ "Currently supported formats are: " );
			for ( Map.Entry<String, Long> f : ContentFormat.JOLIE_ALLOWED_CONTENT_FORMAT.entrySet() ) {
				String key = f.getKey();
				Interpreter.getInstance().logSevere( key );
			}
		}

		return contentFormat;
	}

	private String valueToPlainText( Value value ) {
		Object valueObject = value.valueObject();
		String str = "";
		if ( valueObject instanceof String ) {
			str = ( ( String ) valueObject );
		} else if ( valueObject instanceof Integer ) {
			str = ( ( Integer ) valueObject ).toString();
		} else if ( valueObject instanceof Double ) {
			str = ( ( Double ) valueObject ).toString();
		} else if ( valueObject instanceof ByteArray ) {
			str = ( ( ByteArray ) valueObject ).toString();
		} else if ( valueObject instanceof Boolean ) {
			str = ( ( Boolean ) valueObject ).toString();
		} else if ( valueObject instanceof Long ) {
			str = ( ( Long ) valueObject ).toString();
		}

		return str;
	}

	private int messageType( String operationName ) {
		int messageType = MessageType.NON;
		if ( this.protocol.hasOperationSpecificParameter( operationName,
			Parameters.MESSAGE_TYPE ) ) {
			Value messageTypeValue = this.protocol.getOperationSpecificParameterFirstValue(
				operationName, Parameters.MESSAGE_TYPE );
			if ( messageTypeValue.isInt() ) {
				if ( MessageType.isMessageType( messageTypeValue.intValue() ) ) {
					messageType = messageTypeValue.intValue();
				} else {
					Interpreter.getInstance().logSevere( "Coap Message Type "
						+ messageTypeValue.intValue() + " is not allowed! "
						+ "Assuming default message type \"NON\"." );
				}
			} else {
				if ( messageTypeValue.isString() ) {
					String messageTypeString = messageTypeValue.strValue();
					switch ( messageTypeString ) {
						case "CON": {
							messageType = MessageType.CON;
							break;
						}
						case "NON": {
							messageType = MessageType.NON;
							break;
						}
						case "RST": {
							messageType = MessageType.RST;
							break;
						}
						case "ACK": {
							messageType = MessageType.ACK;
							break;
						}
						default: {
							Interpreter.getInstance().logSevere( "Coap Message Type "
								+ messageTypeString + " is not allowed! "
								+ "Assuming default message type \"NON\"." );
							break;
						}
					}
				} else {
					Interpreter.getInstance().logSevere( "Coap Message Type "
						+ "cannot  be read as an integer nor as a string! "
						+ "Check the message type." );
				}
			}
		}
		return messageType;
	}

	/**
	Default Code is Integer 	for POST in request and Integer for CONTENT for responses.
	@param operationName
	@param isResponse
	@return The Integer rapresentation of the Message Code as listed in {@link MessageCode}
	 */
	private int getMessageCode( String operationName, boolean isResponse ) {
		int messageCode = MessageCode.POST;
		if ( isResponse ) {
			messageCode = MessageCode.CONTENT_205;
		}
		if ( this.protocol.hasOperationSpecificParameter( operationName,
			Parameters.MESSAGE_CODE ) ) {
			Value messageCodeValue = this.protocol
				.getOperationSpecificParameterFirstValue( operationName,
					Parameters.MESSAGE_CODE );
			if ( messageCodeValue.isInt() ) {
				messageCode = messageCodeValue.intValue();
			} else {
				if ( messageCodeValue.isString() ) {
					String messageCodeValueString = messageCodeValue.strValue().toUpperCase();
					if ( MessageCode.JOLIE_ALLOWED_MESSAGE_CODE.containsKey( messageCodeValueString ) ) {
						messageCode = MessageCode.JOLIE_ALLOWED_MESSAGE_CODE.get( messageCodeValueString );
					} else {
						Interpreter.getInstance().logSevere( "Message Code "
							+ messageCodeValueString + " is not supported! "
							+ "Assuming default message code "
							+ MessageCode.asString( messageCode ) + " instead."
							+ "Supported message codes are:" );
						for ( Map.Entry<String, Integer> f : MessageCode.JOLIE_ALLOWED_MESSAGE_CODE.entrySet() ) {
							String key = f.getKey();
							Interpreter.getInstance().logSevere( key );
						}
					}
				}
			}
		}
		return messageCode;
	}

	private Value byteBufToValue( ByteBuf in, Type type, String format )
		throws IOException, ParserConfigurationException,
		SAXException, TypeCheckingException {

		ByteBuf byteBuf = Unpooled.copiedBuffer( in );
		Value value = Value.create();
		String message = Unpooled.copiedBuffer( byteBuf ).toString( charset );

		if ( message.length() > 0 ) {
			switch ( format ) {
				case "application/xml":
					DocumentBuilderFactory docBuilderFactory
						= DocumentBuilderFactory.newInstance();
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
					this.parsePlainText( message, value, type );
					break;
				case "application/json":
					JsUtils.parseJsonIntoValue( new InputStreamReader(
						new ByteBufInputStream( byteBuf ) ), value,
						this.protocol.checkStringParameter( Parameters.JSON_ENCODING,
							"strict" ) );
					break;
			}

			// for XML format
			try {
				value = type.cast( value );
			} catch ( TypeCastingException e ) {
				// do nothing
			}

		} else {

			value = Value.create();
			try {
				type.check( value );
			} catch ( TypeCheckingException ex1 ) {
				value = Value.create( "" );
				try {
					type.check( value );
				} catch ( TypeCheckingException ex2 ) {
					value = Value.create( new ByteArray( new byte[ 0 ] ) );
					try {
						type.check( value );
					} catch ( TypeCheckingException ex3 ) {
						value = Value.create();
					}
				}
			}
		}

		return value;
	}

	private void parsePlainText( String message, Value value, Type type )
		throws TypeCheckingException {

		try {
			type.check( Value.create( message ) );
			value.setValue( message );
		} catch ( TypeCheckingException e1 ) {
			if ( this.isNumeric( message ) ) {
				try {
					if ( message.equals( "0" ) ) {
						type.check( Value.create( false ) );
						value.setValue( false );
					} else {
						if ( message.equals( "1" ) ) {
							type.check( Value.create( true ) );
							value.setValue( true );
						} else {
							throw new TypeCheckingException( "" );
						}
					}
				} catch ( TypeCheckingException e ) {
					try {
						value.setValue( Integer.parseInt( message ) );
					} catch ( NumberFormatException nfe ) {
						try {
							value.setValue( Long.parseLong( message ) );
						} catch ( NumberFormatException nfe1 ) {
							try {
								value.setValue( Double.parseDouble( message ) );
							} catch ( NumberFormatException nfe2 ) {
							}
						}
					}
				}
			} else {
				try {
					type.check( Value.create( new ByteArray( message.getBytes() ) ) );
					value.setValue( new ByteArray( message.getBytes() ) );
				} catch ( TypeCheckingException e ) {
					value.setValue( message );
				}
			}
		}
	}

	private boolean isNumeric( final CharSequence cs ) {

		if ( cs.length() == 0 ) {
			return false;
		}
		final int sz = cs.length();
		for ( int i = 0; i < sz; i++ ) {
			if ( !Character.isDigit( cs.charAt( i ) ) ) {
				return false;
			}
		}
		return true;
	}

	/**
	 * TODO it can be upgrade to Streaming Comm Channel method
	 *
	 * @param operationName
	 * @return
	 */
	public boolean isOneWay( String operationName ) {
		return this.protocol.channel().parentPort().getInterface()
			.oneWayOperations().containsKey( operationName );
	}

	private URI getTargetURI( CommMessage in )
		throws URISyntaxException {

		URI location;
		if ( this.protocol.isInput ) {
			location = this.protocol.channel().parentInputPort().location();
		} else {
			location = new URI( this.protocol.channel().parentOutputPort()
				.locationVariablePath().evaluate().strValue() );
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
		if ( this.protocol.hasOperationSpecificParameter( in.operationName(),
			Parameters.ALIAS ) ) {

			for ( Value v : this.protocol.getOperationSpecificParameterVector( in.operationName(), Parameters.ALIAS ) ) {
				String path = this.getDynamicAlias( v.strValue(), in.value() );
				resource_name.append( path );
			}
		} else {
			resource_name.append( location.getPath() );
		}

		// 7. if resource name is empty append a single backslash and the operation name
		if ( resource_name.length() == 0 ) {
			resource_name.append( "/" );
			resource_name.append( in.operationName() );
		}

		if ( location.getQuery() != null ) {
			resource_name.append( "?" ).append( location.getQuery() );
		}

		// 9. append resource name to url
		url.append( resource_name );

		// set the string without percents or not allowed chars 
		String uri = new URI( url.toString() ).toASCIIString();

		return new URI( uri );
	}

	private String getDynamicAlias( String start, Value value ) {

		Set<String> aliasKeys = new TreeSet<>();
		String pattern = "%(!)?\\{[^\\}]*\\}";

		// find pattern
		int offset = 0;
		String currStrValue;
		String currKey;
		StringBuilder result = new StringBuilder( start );
		Matcher m = Pattern.compile( pattern ).matcher( start );

		// substitute in alias
		while ( m.find() ) {
			currKey = start.substring( m.start() + 3, m.end() - 1 );
			currStrValue = value.getFirstChild( currKey ).strValue();
			aliasKeys.add( currKey );
			result.replace(
				m.start() + offset, m.end() + offset,
				currStrValue
			);
			offset += currStrValue.length() - 3 - currKey.length();
		}

		// remove from the value
		for ( String aliasKey : aliasKeys ) {
			value.children().remove( aliasKey );
		}

		return result.toString();
	}

	private String getOperationName( CoapMessage in ) {

		StringBuilder sb = new StringBuilder();
		int i = 1;
		List<OptionValue> options = in.getOptions( Option.URI_PATH );
		sb.append( "/" );
		for ( OptionValue option : options ) {
//			StringOptionValue stringOption = ( StringOptionValue ) option;
			if ( i < options.size() ) {
				sb.append( option.getDecodedValue() ).append( "/" );
			} else {
				sb.append( option.getDecodedValue() );
			}
			i++;
		}
		String URIPath = sb.toString();
		String operationName = this.protocol.getOperationFromOperationSpecificStringParameter( Parameters.ALIAS, URIPath.substring( 1 ) );

		return operationName;
	}

	private String valueToPrettyString( Value request ) {
		Writer writer = new StringWriter();
		ValuePrettyPrinter printer = new ValuePrettyPrinter( request, writer, "" );
		try {
			printer.run();
		} catch ( IOException e ) {
		} // Should never happen
		return writer.toString();

	}

	private void parseLinkFormat( ByteBuf in, Value value ) {
		LinkValueList linkValueList = LinkValueList.decode( Unpooled.copiedBuffer( in ).toString( charset ) );
		for ( String ref : linkValueList.getUriReferences() ) {
			ValueVector key = ValueVector.create();
			Value v = Value.create();
			for ( LinkParam param : linkValueList.getLinkParams( ref ) ) {
				ValueVector par = ValueVector.create();
				par.add( Value.create( param.getValue().replaceAll( "\"", "" ) ) );
				v.children().put( param.getKey().toString(), par );
			}
			key.add( v );
			value.children().put( ref, key );
		}
		if ( this.protocol.checkBooleanParameter( Parameters.DEBUG ) ) {
			Interpreter.getInstance().logInfo( valueToPrettyString( value ) );
		}
	}

	private static class Parameters {

		private static final String DEBUG = "debug";
		private static final String CONTENT_FORMAT = "contentFormat";
		private static final String MESSAGE_TYPE = "messageType";
		private static final String MESSAGE_CODE = "messageCode";
		private static final String JSON_ENCODING = "json_encoding";
		private static final String ALIAS = "alias";
		private static final String TIMEOUT = "timeout";
	}
}
