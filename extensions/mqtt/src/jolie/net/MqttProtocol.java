/*
 *   Copyright (C) 2017 by Stefano Pio Zingaro <stefanopio.zingaro@unibo.it>  
 *   Copyright (C) 2017 by Saverio Giallorenzo <saverio.giallorenzo@gmail.com>
 *   Copyright (C) 2017 by Ivan Lanese <ivan.lanese@unibo.com>                 
 *   Copyright (C) 2017 by Maurizio Gabbrielli <maurizio.gabbrielli@unibo.com> 
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

import jolie.net.mqtt.InputPortHandler;
import jolie.net.mqtt.OutputPortHandler;
import jolie.net.ports.InputPort;
import jolie.net.ports.OutputPort;
import jolie.net.protocols.PubSubCommProtocol;

import jolie.js.JsUtils;
import jolie.xml.XmlUtils;

import jolie.runtime.ByteArray;
import jolie.runtime.typing.Type;
import jolie.runtime.typing.TypeCheckingException;
import jolie.runtime.VariablePath;
import jolie.runtime.Value;
import jolie.runtime.FaultException;
import jolie.runtime.ValueVector;
import jolie.runtime.typing.OneWayTypeDescription;
import jolie.runtime.typing.RequestResponseTypeDescription;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectPayload;
import io.netty.handler.codec.mqtt.MqttConnectVariableHeader;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttSubscribePayload;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import io.netty.handler.codec.mqtt.MqttVersion;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;

import java.nio.charset.Charset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import jolie.ExecutionThread;
import org.xml.sax.InputSource;

import jolie.Interpreter;
import jolie.runtime.typing.TypeCastingException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class MqttProtocol extends PubSubCommProtocol {

	private final Set<String> aliasKeys;
	private final Charset charset;
	private final AtomicInteger nextMessageId;
	private String operationResponse;
	private String topicResponse;

	/**
	 *
	 * @param configurationPath
	 */
	public MqttProtocol( VariablePath configurationPath ) {

		super( configurationPath );
		this.nextMessageId = new AtomicInteger( 1 );
		this.charset = CharsetUtil.UTF_8;
		this.aliasKeys = new TreeSet<>();
	}

	public static MqttMessage getPingMessage() {
		return new MqttMessage(
			new MqttFixedHeader(
				MqttMessageType.PINGREQ,
				false,
				MqttQoS.AT_MOST_ONCE,
				false,
				0 )
		);
	}

	@Override
	public void setupPipeline( ChannelPipeline p ) {

		//p.addLast("LOGGER", new LoggingHandler(LogLevel.INFO));
		p.addLast( "ENCODER", MqttEncoder.INSTANCE );
		p.addLast( "DECODER", new MqttDecoder() );

		p.addLast( "PING", new ChannelInboundHandlerAdapter() {
			@Override
			public void userEventTriggered( ChannelHandlerContext ctx,
				Object evt ) throws Exception {

				if ( evt instanceof IdleStateEvent ) {
					IdleStateEvent event = ( IdleStateEvent ) evt;
					switch ( event.state() ) {
						case READER_IDLE:
							break;
						case WRITER_IDLE:
							ctx.channel().writeAndFlush( MqttProtocol.getPingMessage() );
							break;
					}
				}
			}
		} );
		if ( channel().parentPort() instanceof InputPort ) {
			p.addLast( "INPUT", new InputPortHandler( this, channel() ) );
		}
		if ( channel().parentPort() instanceof OutputPort ) {
			p.addLast( "OUTPUT", new OutputPortHandler( this ) );
		}
		// else we do not add a specific handler, we will add it after channel registation (see InputResponseHandler)
	}

	public void checkDebug( ChannelPipeline p ) {
		if ( checkBooleanParameter( Parameters.DEBUG ) ) {
			p.addAfter( "DECODER", "DBDecode",
				new MessageToMessageDecoder<MqttMessage>() {
				@Override
				protected void decode( ChannelHandlerContext chc, MqttMessage i,
					List<Object> list ) throws Exception {
					String logLine = "";
					try {
						logLine = "#" + getMessageID( i ) + " ";
					} catch ( Exception e ) {
					}
					MqttMessageType t = i.fixedHeader().messageType();
					if ( !( t.equals( MqttMessageType.PINGRESP )
						|| t.equals( MqttMessageType.PINGREQ ) ) ) {
						logLine += " <- " + t;
						if ( t.equals( MqttMessageType.PUBLISH ) ) {
							logLine += "\t  " + ( ( MqttPublishMessage ) i )
								.variableHeader().topicName();
						}
						Interpreter.getInstance().logInfo( logLine );
					}
					if ( t.equals( MqttMessageType.PUBLISH ) ) {
						( ( MqttPublishMessage ) i ).retain();
					}
					list.add( i );
				}
			} );
			p.addAfter( "DECODER", "DBEncode",
				new MessageToMessageEncoder<MqttMessage>() {
				@Override
				protected void encode( ChannelHandlerContext chc, MqttMessage i,
					List list ) throws Exception {
					String logLine = "";
					try {
						logLine = "#" + getMessageID( i ) + " ";
					} catch ( Exception e ) {
					}
					MqttMessageType t = i.fixedHeader().messageType();
					logLine += t + " ->";
					if ( t.equals( MqttMessageType.PUBLISH ) ) {
						logLine += "\t topic: " + ( ( MqttPublishMessage ) i )
							.variableHeader().topicName();
					}
					if ( t.equals( MqttMessageType.SUBSCRIBE ) ) {
						logLine += "\t topics: ";
						for ( MqttTopicSubscription topic : ( ( MqttSubscribeMessage ) i )
							.payload().topicSubscriptions() ) {
							logLine += topic.topicName() + ", ";
						}
						logLine = logLine.substring( 0, logLine.length() - 2 ); // removes the trailing ", "
					}

					if ( !( t.equals( MqttMessageType.PINGRESP )
						|| t.equals( MqttMessageType.PINGREQ ) ) ) {
						Interpreter.getInstance().logInfo( logLine );
					}

					if ( channel().parentPort() instanceof OutputPort
						&& t.equals( MqttMessageType.PUBLISH ) ) {
						chc.write( i );
						chc.flush();
					} else {
						if ( channel().parentPort() instanceof InputPort
							&& t.equals( MqttMessageType.PUBLISH ) ) {
							( ( MqttPublishMessage ) i ).retain();
						}
						list.add( i );
					}
				}
			} );
		}
	}

	@Override
	public String name() {
		return "mqtt";
	}

	@Override
	public boolean isThreadSafe() {
		return true;
	}

	/**
	 *
	 * @param operationName
	 * @return boolean
	 */
	public boolean isOneWay( String operationName ) {
		return channel().parentPort().getInterface()
			.oneWayOperations().containsKey( operationName );
	}

	/**
	 * Method handling the incoming publishes from the broker.
	 *
	 * @param ch - The channel where to write the eventual Comm Message
	 * @param mpm - The publish message received.
	 */
	public void recv_pub( Channel ch, MqttPublishMessage mpm ) {
		if ( !( mpm.variableHeader().messageId() == -1
			// we just manage QoS 1 and 2 as QoS 0 needs no response back
			|| getQoS( mpm ).equals( MqttQoS.AT_MOST_ONCE ) ) ) {
			MqttMessage respMessage = null;
			// WE OBTAIN THE APPROPRIATE HEADER ...
			switch ( getQoS( mpm ) ) {
				case AT_LEAST_ONCE:
					// ... A PUBACK
					MqttFixedHeader f = new MqttFixedHeader(
						MqttMessageType.PUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0 );
					respMessage = new MqttPubAckMessage(
						f, MqttMessageIdVariableHeader.from( getMessageID( mpm ) )
					);
					break;
				case EXACTLY_ONCE:
					// ... OR A PUBREC
					f = new MqttFixedHeader(
						MqttMessageType.PUBREC, false, MqttQoS.AT_MOST_ONCE, false, 0 );
					respMessage = new MqttMessage(
						f, MqttMessageIdVariableHeader.from( getMessageID( mpm ) )
					);
					break;
			}
			// AND WE SEND BACK THE APPROPRIATE RESPONSE
			ch.writeAndFlush( respMessage );
		}
	}

	public void startPing( ChannelPipeline p ) {
		p.addAfter( "DECODER", "IDLE_STATE", new IdleStateHandler( 0, 2, 0 ) );
	}

	public void stopPing( ChannelPipeline p ) {
		if ( p.get( "IDLE_STATE" ) != null ) {
			p.remove( "IDLE_STATE" );
		}
	}

	public static int getMessageID( MqttMessage m ) {
		if ( m instanceof MqttPublishMessage ) {
			return ( ( MqttPublishMessage ) m ).variableHeader().messageId();
		} else {
			return ( ( MqttMessageIdVariableHeader ) m.variableHeader() ).messageId();
		}
	}

	public static MqttQoS getQoS( MqttMessage m ) {
		return m.fixedHeader().qosLevel();
	}

	public void releaseMessage( int messageID ) throws IOException {
		( ( StreamingCommChannel ) ( ( NioSocketCommChannel ) channel() )
			.getChannelHandler().getInChannel() ).sendRelease( ( long ) messageID );
	}

	public void markAsSentAndStopPing( Channel cc, int messageID )
		throws IOException {
		releaseMessage( messageID );
		stopPing( cc.pipeline() );
	}

	/**
	 * Check if the given incoming Mqtt Message has the related requested QoS.
	 *
	 * @param cm
	 * @param toBeChecked MqttQoS
	 * @return boolean - True if the QoS is the same, False otherwise.
	 */
	public boolean checkQoS( CommMessage cm, MqttQoS toBeChecked ) {
		return getOperationQoS( cm.operationName() ).equals( toBeChecked );
	}

	/**
	 *
	 * @return List of String - The list of topics for port interface
	 */
	public List<String> topics() {

		List<String> opL = new ArrayList<>();
		for ( Map.Entry<String, OneWayTypeDescription> owon
			: channel().parentPort().getInterface().oneWayOperations()
				.entrySet() ) {
			opL.add( alias( owon.getKey() ) );
		}
		for ( Map.Entry<String, RequestResponseTypeDescription> rron
			: channel().parentPort().getInterface()
				.requestResponseOperations()
				.entrySet() ) {
			opL.add( alias( rron.getKey() ) );
		}
		return opL;
	}

	public MqttConnectMessage connectMsg() {

		Random random = new Random();
		String clientId = "jolie/";
		String[] options
			= ( "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456"
				+ "789" ).split( "" );
		for ( int i = 0; i < 4; i++ ) {
			clientId += options[ random.nextInt( options.length ) ];
		}

		MqttFixedHeader mfh = new MqttFixedHeader(
			MqttMessageType.CONNECT,
			false,
			MqttQoS.AT_MOST_ONCE,
			false,
			0 );
		MqttConnectVariableHeader vh = new MqttConnectVariableHeader(
			Parameters.MQTT_VERSION.protocolName(),
			Parameters.MQTT_VERSION.protocolLevel(),
			checkBooleanParameter( Parameters.USERNAME ),
			checkBooleanParameter( Parameters.PASSWORD ),
			false,
			MqttQoS.AT_MOST_ONCE.value(),
			checkBooleanParameter( Parameters.WILL_TOPIC ),
			true,
			2 );
		MqttConnectPayload p = new MqttConnectPayload(
			clientId,
			getStringParameter( Parameters.WILL_TOPIC ),
			getStringParameter( Parameters.WILL_MESSAGE ),
			getStringParameter( Parameters.USERNAME ),
			getStringParameter( Parameters.PASSWORD ) );

		return new MqttConnectMessage( mfh, vh, p );
	}

	/**
	 *
	 * @param channel
	 * @param message
	 * @return ChannelFuture
	 */
	public ChannelFuture handlePubrec( Channel channel, MqttMessage message ) {

		MqttFixedHeader fixedHeader = new MqttFixedHeader(
			MqttMessageType.PUBREL, false, MqttQoS.AT_LEAST_ONCE, false, 0 );
		MqttMessageIdVariableHeader variableHeader
			= ( MqttMessageIdVariableHeader ) message.variableHeader();

		return channel.writeAndFlush(
			new MqttMessage( fixedHeader, variableHeader ) );
	}

	/**
	 *
	 * @param channel
	 * @param message
	 * @return ChannelFuture
	 */
	public ChannelFuture handlePubrel( Channel channel, MqttMessage message ) {

		MqttFixedHeader fixedHeader
			= new MqttFixedHeader( MqttMessageType.PUBCOMP,
				false, MqttQoS.AT_MOST_ONCE, false, 0 );
		MqttMessageIdVariableHeader variableHeader
			= MqttMessageIdVariableHeader.from(
				( ( MqttMessageIdVariableHeader ) message.variableHeader() )
					.messageId() );

		return channel.writeAndFlush(
			new MqttMessage( fixedHeader, variableHeader ) );
	}

	/**
	 * Returns the response topic.
	 *
	 * @param cm CommMessage
	 * @return the topic response
	 */
	public String getRespTopic( CommMessage cm ) {

		// bookkeeping variables for topic-to-operation correlation
		operationResponse = cm.operationName();
		topicResponse = cm.operationName() + "/response";

		if ( hasOperationSpecificParameter( cm.operationName(),
			Parameters.ALIAS_RESPONSE ) ) {
			topicResponse = getOperationSpecificStringParameter( cm.operationName(),
				Parameters.ALIAS_RESPONSE );
		}

		return Parameters.BOUNDARY + topic( cm, topicResponse, true )
			+ Parameters.BOUNDARY;

	}

	/**
	 *
	 * @param in
	 * @param t
	 * @return
	 * @throws java.lang.Exception
	 */
	public MqttPublishMessage send_response( CommMessage in, String t )
		throws Exception {

		ByteBuf bb = Unpooled.copiedBuffer( valueToByteBuf( in ) );
		MqttQoS q = getOperationQoS( in.operationName() );

		return publishMsg( t, bb, q, ( int ) in.id() );
	}

	/**
	 *
	 * @param in
	 * @return
	 * @throws java.lang.Exception
	 */
	public CommMessage recv_request( MqttPublishMessage in ) throws Exception {

		String on = operation( in.variableHeader().topicName() );
		Value v = byteBufToValue( on, in.payload() );

		return CommMessage.createRequest( on, "/", v );
	}

	/**
	 *
	 * @param ch
	 */
	public void send_subRequest( Channel ch ) {
		startPing( ch.pipeline() );
		ch.writeAndFlush( subscribeMsg( topics(), qos() ) );
	}

	/**
	 *
	 * @param in
	 * @return
	 * @throws Exception
	 */
	public MqttPublishMessage pubOneWayRequest( CommMessage in ) throws Exception {

		String a = in.operationName();

		if ( hasOperationSpecificParameter( in.operationName(),
			Parameters.ALIAS ) ) {
			a = getOperationSpecificStringParameter( in.operationName(),
				Parameters.ALIAS );
		}

		return publishMsg( topic( in, a, true ), valueToByteBuf( in ),
			getOperationQoS( in.operationName() ), ( int ) in.id() );
	}

	/**
	 *
	 * @param in
	 * @return
	 */
	public MqttSubscribeMessage subRequestResponseRequest( CommMessage in ) {

		String a = in.operationName() + "/response";

		if ( hasOperationSpecificParameter( in.operationName(),
			Parameters.ALIAS_RESPONSE ) ) {
			a = getOperationSpecificStringParameter( in.operationName(),
				Parameters.ALIAS_RESPONSE );
		}

		return subscribeMsg( Collections.singletonList( topic( in, a, false ) ), qos() );
	}

	/**
	 *
	 * @param in
	 * @return
	 * @throws Exception
	 */
	public MqttPublishMessage pubRequestResponseRequest( CommMessage in )
		throws Exception {

		String a = in.operationName();

		if ( hasOperationSpecificParameter( in.operationName(),
			Parameters.ALIAS ) ) {
			a = getOperationSpecificStringParameter( in.operationName(),
				Parameters.ALIAS );
		}

		return publishMsg( topic( in, a, false ), valueToByteBuf( in ),
			getOperationQoS( in.operationName() ), ( int ) in.id() );
	}

	private String operation( String topic ) {

		if ( channel().parentPort() instanceof OutputPort ) {
			if ( topic.equals( topicResponse ) ) {
				return operationResponse;
			} else {
				return topic;
			}
		} else {
			if ( configurationPath().getValue().hasChildren( "osc" ) ) {
				for ( Map.Entry<String, ValueVector> i : configurationPath()
					.getValue()
					.getFirstChild( "osc" ).children().entrySet() ) {
					for ( Map.Entry<String, ValueVector> j : i.getValue().first()
						.children().entrySet() ) {
						if ( j.getKey().equals( "alias" ) && j.getValue().first()
							.strValue().equals( topic ) ) {
							return i.getKey();
						}
					}
				}
			}
			// else we return directly the topic
			return topic;
		}
	}

	private MqttMessageIdVariableHeader getNewMessageId() {
		nextMessageId.compareAndSet( 0xffff, 1 );
		return MqttMessageIdVariableHeader.from(
			nextMessageId.getAndIncrement() );
	}

	/**
	 *
	 * @param mpm
	 * @param req
	 * @return
	 * @throws Exception
	 */
	public CommMessage recv_pubReqResp( MqttPublishMessage mpm,
		CommMessage req ) throws Exception {
		return new CommMessage( req.id(),//CommMessage.GENERIC_ID,
			req.operationName(), "/", byteBufToValue( req.operationName(),
			mpm.retain().payload() ), null );
	}

	/**
	 *
	 * @param m
	 * @return
	 */
	public String extractTopicResponse( MqttPublishMessage m ) {
		String msg = Unpooled.wrappedBuffer( m.payload() ).toString( charset );

		if ( msg.indexOf( Parameters.BOUNDARY )
			== 0 && msg.indexOf( Parameters.BOUNDARY, 1 ) > 0 ) {
			return msg.substring( 1, msg.indexOf( Parameters.BOUNDARY, 1 ) );
		} else {
			return null;
		}
	}

	@Override
	public void setExecutionThread( ExecutionThread t ) {
		super.setExecutionThread( t ); //To change body of generated methods, choose Tools | Templates.
	}

	private static class Parameters {

		private static final String BROKER = "broker";
		private static final String ALIAS = "alias";
		private static final String QOS = "QoS";
		private static final String WILL_TOPIC = "willTopic";
		private static final String WILL_MESSAGE = "willMessage";
		private static final String USERNAME = "username";
		private static final String PASSWORD = "password";
		private static final String FORMAT = "format";
		private static final String ALIAS_RESPONSE = "aliasResponse";
		private static final String BOUNDARY = "$";
		private static final String JSON_ENCODING = "json_encoding";
		private static final String DEBUG = "debug";
		private static final MqttVersion MQTT_VERSION = MqttVersion.MQTT_3_1_1;

	}

	private ByteBuf valueToByteBuf( CommMessage in ) throws Exception {

		ByteBuf bb = Unpooled.buffer();
		String format = format( in.operationName() );
		String message;
		String topicResponsePrefix = "";
		Value v = in.isFault() ? Value.create( in.fault().getMessage() ) : in.value();
		if ( !isOneWay( in.operationName() )
			&& channel().parentPort() instanceof OutputPort ) {
			topicResponsePrefix = getRespTopic( in );
		}
		switch ( format ) {
			case "json":
				StringBuilder jsonStringBuilder = new StringBuilder();
				JsUtils.valueToJsonString( v, true, getSendType( in ),
					jsonStringBuilder );
				message = jsonStringBuilder.toString();
				break;
			case "xml":
				DocumentBuilder db = DocumentBuilderFactory.newInstance()
					.newDocumentBuilder();
				Document doc = db.newDocument();
				Element root = doc.createElement( in.operationName() );
				doc.appendChild( root );
				XmlUtils.valueToDocument( v, root, doc );
				Source src = new DOMSource( doc );
				ByteArrayOutputStream strm = new ByteArrayOutputStream();
				Result dest = new StreamResult( strm );
				Transformer trf = TransformerFactory.newInstance()
					.newTransformer();
				trf.setOutputProperty( OutputKeys.ENCODING, charset.name() );
				trf.transform( src, dest );
				message = strm.toString();
				break;
			case "raw":
				message = valueToRaw( v );
				break;
			default:
				throw new FaultException( "Format " + format + " not "
					+ "supported for operation " + in.operationName() );
		}
		message = topicResponsePrefix + message;
		if ( checkBooleanParameter( Parameters.DEBUG ) ) {
			Interpreter.getInstance().logInfo( "Sending " + format.toUpperCase()
				+ " message: " + message );
		}
		bb.writeBytes( message.getBytes( charset ) );
		return bb;
	}

	private String format( String operationName ) {
		/*
	We suppose in advance that raw format stands if nothing else
	is specified.
		 */
		return hasOperationSpecificParameter( operationName,
			Parameters.FORMAT )
				? getOperationSpecificStringParameter( operationName,
					Parameters.FORMAT ) : "raw";
	}

	private String valueToRaw( Value value ) {
		// TODO handle bytearray
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

	private Value byteBufToValue( String operationName, ByteBuf payload )
		throws Exception {

		String msg = Unpooled.wrappedBuffer( payload ).toString( charset );
		if ( checkBooleanParameter( Parameters.DEBUG ) ) {
			Interpreter.getInstance().logInfo( "Received message: " + msg );
		}
		if ( channel().parentPort() instanceof InputPort
			&& !isOneWay( operationName ) ) {
			try {
				msg = msg.substring( msg.indexOf( Parameters.BOUNDARY, 1 ) + 1,
					msg.length() );
			} catch ( IndexOutOfBoundsException ex ) {
			}
		}

		Value v = Value.create();

		Type type = operationType( operationName,
			channel().parentPort() instanceof InputPort );

		if ( msg.length() > 0 ) {
			String format = format( operationName );
			switch ( format ) {
				case "xml":
					DocumentBuilderFactory docBuilderFactory
						= DocumentBuilderFactory.newInstance();
					DocumentBuilder builder = docBuilderFactory
						.newDocumentBuilder();
					InputSource src = new InputSource(
						new ByteBufInputStream(
							Unpooled.wrappedBuffer( msg.getBytes() ) ) );
					src.setEncoding( charset.name() );
					Document doc = builder.parse( src );
					XmlUtils.documentToValue( doc, v );
					break;
				case "json":
					JsUtils.parseJsonIntoValue( new StringReader( msg ), v,
						checkStringParameter(
							Parameters.JSON_ENCODING, "strict" ) );
					break;
				case "raw":
					parseRaw( msg, v, type );
					break;
				default:
					throw new FaultException( "Format " + format
						+ "is not supported. Supported formats are: "
						+ "xml, json and raw" );
			}
			// for XML format
			try {
				v = type.cast( v );
			} catch ( TypeCastingException e ) {
			}
		} else {
			v = Value.create();
			try {
				type.check( v );
			} catch ( TypeCheckingException ex1 ) {
				v = Value.create( "" );
				try {
					type.check( v );
				} catch ( TypeCheckingException ex2 ) {
					v = Value.create( new ByteArray( new byte[ 0 ] ) );
					try {
						type.check( v );
					} catch ( TypeCheckingException ex3 ) {
						v = Value.create();
					}
				}
			}
		}

		return v;
	}

	private void parseRaw( String message, Value value, Type type )
		throws TypeCheckingException {

		try {
			type.check( Value.create( message ) );
			value.setValue( message );
		} catch ( TypeCheckingException e1 ) {
			if ( isNumeric( message ) ) {
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

	private String alias( String operationName ) {

		for ( Iterator<Map.Entry<String, ValueVector>> it = configurationPath()
			.getValue().getFirstChild( "osc" ).children().entrySet()
			.iterator();
			it.hasNext(); ) {
			Map.Entry<String, ValueVector> i = it.next();
			if ( operationName.equals( i.getKey() ) ) {
				return i.getValue().first().getFirstChild( "alias" ).strValue();
			}
		}
		return operationName;
	}

	public MqttQoS getOperationQoS( String operationName ) {

		return hasOperationSpecificParameter( operationName, Parameters.QOS )
			? MqttQoS.valueOf( getOperationSpecificParameterFirstValue(
				operationName, Parameters.QOS ).intValue() )
			: MqttQoS.AT_LEAST_ONCE;
	}

	private MqttQoS qos() {

		return hasParameter( Parameters.QOS ) ? MqttQoS.valueOf(
			getIntParameter( Parameters.QOS ) ) : MqttQoS.AT_LEAST_ONCE;
	}

	private String topic( CommMessage cm, String alias, boolean removeKeys ) {

		String pattern = "%(!)?\\{[^\\}]*\\}";

		// find pattern
		int offset = 0;
		String currStrValue;
		String currKey;
		StringBuilder result = new StringBuilder( alias );
		Matcher m = Pattern.compile( pattern ).matcher( alias );

		// substitute in alias
		while ( m.find() ) {
			currKey = alias.substring( m.start() + 3, m.end() - 1 );
			currStrValue = cm.value().getFirstChild( currKey ).strValue();
			aliasKeys.add( currKey );
			result.replace(
				m.start() + offset, m.end() + offset,
				currStrValue
			);
			offset += currStrValue.length() - 3 - currKey.length();
		}

		if ( removeKeys ) {
			for ( String aliasKey : aliasKeys ) {
				cm.value().children().remove( aliasKey );
			}
		}

		return result.toString();
	}

	private MqttSubscribeMessage subscribeMsg( List<String> topics,
		MqttQoS subQos ) {

		List<MqttTopicSubscription> tmsL = new ArrayList<>();
		for ( String t : topics ) {
			tmsL.add( new MqttTopicSubscription( t, MqttQoS.EXACTLY_ONCE ) );
		}
		MqttFixedHeader mfh = new MqttFixedHeader(
			MqttMessageType.SUBSCRIBE, false, subQos, false, 0 );
		MqttMessageIdVariableHeader vh = getNewMessageId();
		MqttSubscribePayload p = new MqttSubscribePayload( tmsL );

		return new MqttSubscribeMessage( mfh, vh, p );
	}

	private MqttPublishMessage publishMsg( String topic, ByteBuf payload,
		MqttQoS pubQos, int messageID ) {

		MqttFixedHeader mfh = new MqttFixedHeader(
			MqttMessageType.PUBLISH,
			false,
			pubQos,
			false,
			0 );
		MqttPublishVariableHeader vh = new MqttPublishVariableHeader( topic,
			//getNewMessageId().messageId()
			messageID
		);

		return new MqttPublishMessage( mfh, vh, payload );
	}
}
