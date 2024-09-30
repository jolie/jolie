/***************************************************************************
 *   Copyright (C) 2011 by Fabrizio Montesi <famontesi@gmail.com>          *
 *   Copyright (C) 2011 by Károly Szántó                                   *
 *   Copyright (C) 2011 by Giannakis Manthios                              *
 *   Copyright (C) 2014 by Matthias Dieter Wallnöfer                       *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU Library General Public License as       *
 *   published by the Free Software Foundation; either version 2 of the    *
 *   License, or (at your option) any later version.                       *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU Library General Public     *
 *   License along with this program; if not, write to the                 *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 *                                                                         *
 *   For details about the authors of this software, see the AUTHORS file. *
 ***************************************************************************/

package jolie.net;

import jolie.Interpreter;
import jolie.js.JsUtils;
import jolie.lang.NativeType;
import jolie.lang.parse.ast.types.BasicTypeDefinition;
import jolie.net.http.*;
import jolie.net.ports.Interface;
import jolie.net.ports.OutputPort;
import jolie.net.protocols.SequentialCommProtocol;
import jolie.runtime.*;
import jolie.runtime.typing.BasicType;
import jolie.runtime.typing.RequestResponseTypeDescription;
import jolie.runtime.typing.Type;
import jolie.util.Range;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author Fabrizio Montesi
 * @author Károly Szántó
 * @author Giannakis Manthios
 * @author Eros Fabrici
 *
 *         2014 Matthias Dieter Wallnöfer: conversion to JSONRPC over HTTP
 */
public class JsonRpcProtocol extends SequentialCommProtocol implements HttpUtils.Protocol {
	private final URI uri;
	private final Interpreter interpreter;
	private final boolean inInputPort;
	private String encoding;
	private Interface channelInterface = null;

	private static class Parameters {
		private final static String TRANSPORT = "transport";
		private final static String ALIAS = "alias";
		private final static String OSC = "osc";
		private final static String CLIENT_LOCATION = "clientLocation";
		private final static String CLIENT_OUTPUTPORT = "clientOutputPort";
		private final static String IS_NULLABLE = "isNullable";
	}

	private static final Type JSON_RPC_TYPE =
		Type.create( BasicType.fromBasicTypeDefinition( BasicTypeDefinition.of( NativeType.STRING ) ),
			new Range( 1, 1 ), false, null );
	private static final Type ID_TYPE = Type.createChoice(
		new Range( 0, 1 ),
		Type.create(
			BasicType.fromBasicTypeDefinition(
				BasicTypeDefinition.of(
					NativeType.STRING ) ),
			new Range( 1, 1 ), false,
			null ),
		Type.createChoice(
			new Range( 1, 1 ),
			Type.create(
				BasicType.fromBasicTypeDefinition(
					BasicTypeDefinition.of(
						NativeType.LONG ) ),
				new Range( 1, 1 ), false,
				null ),
			Type.createChoice(
				new Range( 1, 1 ),
				Type.create(
					BasicType.fromBasicTypeDefinition(
						BasicTypeDefinition.of(
							NativeType.DOUBLE ) ),
					new Range( 1, 1 ), false,
					null ),
				Type.create(
					BasicType.fromBasicTypeDefinition(
						BasicTypeDefinition.of(
							NativeType.INT ) ),
					new Range( 1, 1 ), false, null ) ) ) );

	/**
	 * Parses the given JSON-RPC `id` according to the JSON-RPC specification.
	 *
	 * <p>
	 * This method accepts a {@link Value} object representing the `id` field in a JSON-RPC request and
	 * returns the corresponding Java type based on the type of the `id`. The following conversions are
	 * supported:
	 * </p>
	 *
	 * <ul>
	 * <li>If the `id` is not defined, returns {@code null}.</li>
	 * <li>If the `id` is a double, returns its double value.</li>
	 * <li>If the `id` is an integer, returns its integer value.</li>
	 * <li>If the `id` is a long, returns its long value.</li>
	 * <li>If the `id` is a string, returns its string value.</li>
	 * </ul>
	 *
	 * <p>
	 * If the `id` is of an unknown type, an {@link IllegalArgumentException} is thrown.
	 * </p>
	 *
	 * @param id the {@link Value} object representing the JSON-RPC `id`
	 * @return the parsed `id` as an {@link Object}, which can be a {@code Double}, {@code Integer},
	 *         {@code Long}, {@code String}, or {@code null} if the `id` is not defined
	 * @throws IllegalArgumentException if the type of the `id` is not recognized
	 */
	private Object parseId( Value id ) {
		if( !id.isDefined() ) {
			return null;
		}
		if( id.isDouble() ) {
			return id.doubleValue();
		}
		if( id.isInt() ) {
			return id.intValue();
		}
		if( id.isLong() ) {
			return id.longValue();
		}
		if( id.isString() ) {
			return id.strValue();
		}
		throw new IllegalArgumentException( "Unknown id type" );
	}

	private final static String LSP = "lsp";
	private final static int INITIAL_CAPACITY = 8;
	private final static float LOAD_FACTOR = 0.75f;

	private final Map< Long, Object > jsonRpcIdMap;
	private final Map< Long, String > jsonRpcOpMap;

	@Override
	public String name() {
		return "jsonrpc";
	}

	public JsonRpcProtocol( VariablePath configurationPath, URI uri,
		Interpreter interpreter, boolean inInputPort ) {
		super( configurationPath );
		this.uri = uri;
		this.interpreter = interpreter;
		this.inInputPort = inInputPort;

		// prepare the two maps
		this.jsonRpcIdMap = new HashMap<>( INITIAL_CAPACITY, LOAD_FACTOR );
		this.jsonRpcOpMap = new HashMap<>( INITIAL_CAPACITY, LOAD_FACTOR );
	}

	@Override
	public void send_internal( OutputStream ostream, CommMessage message, InputStream istream )
		throws IOException {

		boolean isLsp = checkStringParameter( Parameters.TRANSPORT, LSP );

		if( !isLsp ) {
			if( !message.isFault() && message.hasGenericRequestId() && inInputPort ) {
				// JSON-RPC notification mechanism (method call with dropped result)
				// we just send HTTP status code 204
				StringBuilder httpMessage = new StringBuilder();
				httpMessage.append( "HTTP/1.1 204 No Content" ).append( HttpUtils.CRLF )
					.append( "Server: Jolie" ).append( HttpUtils.CRLF ).append( HttpUtils.CRLF );
				ostream.write( httpMessage.toString().getBytes( StandardCharsets.UTF_8 ) );
				return;
			}
		}
		Value value = Value.create();
		value.getFirstChild( "jsonrpc" ).setValue( "2.0" );

		/*
		 * If we are in LSP mode: 1. we do not want to close the connection (because at least VS Code does
		 * not reopen it correctly); 2. we do not want to send ACKs to the client.
		 */
		if( isLsp ) {
			// 1.
			channel().setToBeClosed( false );

			// 2.
			if( message.hasGenericRequestId() && message.value().getChildren( "result" ).isEmpty() ) {
				return;
			}
		}

		String operationNameAliased = message.operationName();
		// resolving aliases
		if( isLsp && hasParameter( Parameters.OSC ) ) {
			Value osc = getParameterFirstValue( Parameters.OSC );
			for( Entry< String, ValueVector > ev : osc.children().entrySet() ) {
				Value v = ev.getValue().get( 0 );
				if( v.hasChildren( Parameters.ALIAS ) ) {
					if( ev.getKey().equals( operationNameAliased ) ) {
						operationNameAliased = v.getFirstChild( Parameters.ALIAS ).strValue();
					}
				}
			}
		}

		/*
		 * While we build the full message we type the entire message, so JsUtils class will convert it
		 * properly
		 */
		Type operationType = Type.UNDEFINED;
		String originalOpName = message.operationName();
		Map< String, Type > subTypes = new HashMap<>();
		subTypes.put( "jsonrpc", JSON_RPC_TYPE );
		subTypes.put( "id", ID_TYPE );

		if( message.isFault() ) {
			Object jsonRpcId = jsonRpcIdMap.get( message.requestId() );
			value.setFirstChild( "id", jsonRpcId != null ? jsonRpcId : message.requestId() );
			Value error = value.getFirstChild( "error" );
			error.getFirstChild( "code" ).setValue( -32000 );
			error.getFirstChild( "message" ).setValue( message.fault().faultName() );
			error.getChildren( "data" ).set( 0, message.fault().value() );
		} else {
			boolean isRR = channel().parentPort().getOperationTypeDescription( message.operationName(),
				message.resourcePath() ) instanceof RequestResponseTypeDescription;
			// if we are in LSP, we want to be sure the message to be
			// a response to a request in order to send it with
			// the fields "results" and "id"
			// boolean check = isLsp ? isRR : true;
			if( inInputPort && (isRR || !isLsp) ) {
				value.getChildren( "result" ).set( 0, message.value() );
				Object jsonRpcId = jsonRpcIdMap.get( message.requestId() );
				value.getFirstChild( "id" )
					.setValue(
						jsonRpcId != null ? jsonRpcId : message.requestId() );

				if( channelInterface.requestResponseOperations().containsKey( originalOpName ) ) {
					operationType = channelInterface.requestResponseOperations().get( originalOpName ).responseType();
				} else if( channel().parentInputPort().getAggregatedOperation( originalOpName ) != null ) {
					operationType = channel().parentInputPort().getAggregatedOperation( originalOpName )
						.getOperationTypeDescription().asRequestResponseTypeDescription().responseType();
				}

				operationType = operationType.getMinimalType( message.value() ).orElse( Type.UNDEFINED );
				subTypes.put( "result", operationType );
			} else {
				jsonRpcOpMap.put( message.requestId(), operationNameAliased );
				value.getFirstChild( "method" ).setValue( operationNameAliased );

				if( isRR ) {
					if( channelInterface.requestResponseOperations().containsKey( originalOpName ) ) {
						operationType =
							channelInterface.requestResponseOperations().get( originalOpName ).requestType();
					}
				} else {
					if( channelInterface.oneWayOperations().containsKey( originalOpName ) ) {
						operationType = channelInterface.oneWayOperations().get( originalOpName ).requestType();
					}
				}

				operationType = operationType.getMinimalType( message.value() ).orElse( Type.UNDEFINED );

				if( message.value().isDefined() || message.value().hasChildren() ) {
					// some implementations need an array here
					// value.getFirstChild( "params" ).getChildren( JsUtils.JSONARRAY_KEY ).set( 0, message.value() );
					// paramsSubTypes.put( JsUtils.JSONARRAY_KEY, operationType );
					// subTypes.put( "params", Type.create( NativeType.VOID, new Range( 1, 1 ), false, paramsSubTypes )
					// );
					value.getChildren( "params" ).add( message.value() );
					subTypes.put( "params", operationType );
				}

				if( !message.hasGenericRequestId() && !isLsp ) {
					value.getFirstChild( "id" ).setValue( message.requestId() );
				}
			}
		}

		Type fullMessageType =
			Type.create( BasicType.fromBasicTypeDefinition( BasicTypeDefinition.of( NativeType.VOID ) ),
				new Range( 1, 1 ), false, subTypes );
		final StringBuilder json = new StringBuilder();
		JsUtils.valueToJsonString( value, true, fullMessageType, json );
		String jsonMessage = json.toString();

		/*
		 * LSP clients sometimes want a empty array for some fields, the only way to do in jolie is to have
		 * a type like the follwoing: t*: void then you assing t = void, resulting in t[0] = void the
		 * problem is that JsUtils will convert this in "t": [null] with this we remove manually null values
		 * iff there is the parameter osc."operationName".isNullable = true
		 */
		if( hasParameter( Parameters.OSC ) ) {
			Value osc = getParameterFirstValue( Parameters.OSC );
			String opName = message.operationName();

			if( osc.hasChildren( opName ) ) {
				Value childOp = osc.getFirstChild( opName );
				// if osc has a child with opName and grandChild isNullable
				if( childOp.hasChildren( Parameters.IS_NULLABLE ) ) {
					if( childOp.getFirstChild( Parameters.IS_NULLABLE ).boolValue() ) {
						// then we replace all null with and empty string
						// TODO use a regex
						jsonMessage = jsonMessage.replaceAll( "null", "" );
					}
				}
			}
		}

		ByteArray content = new ByteArray( jsonMessage.getBytes( StandardCharsets.UTF_8 ) );

		if( checkStringParameter( Parameters.TRANSPORT, LSP ) ) {
			String lspHeaders = "Content-Length: " + content.size() + HttpUtils.CRLF + HttpUtils.CRLF;

			if( checkBooleanParameter( HttpUtils.Parameters.DEBUG, false ) ) {
				interpreter.logInfo( "[JSON-RPC debug] Sending:\n" + lspHeaders + content.toString( "utf-8" ) );
			}

			ostream.write( lspHeaders.getBytes( HttpUtils.URL_DECODER_ENC ) );
			ostream.write( content.getBytes() );
		} else {
			StringBuilder httpMessage = new StringBuilder();
			if( inInputPort ) {
				// We're responding to a request
				httpMessage.append( "HTTP/1.1 200 OK" ).append( HttpUtils.CRLF )
					.append( "Server: Jolie" ).append( HttpUtils.CRLF );
			} else {
				// We're sending a request
				String path = uri.getRawPath(); // TODO: fix this to consider resourcePaths
				if( path == null || path.length() == 0 ) {
					path = "*";
				}
				httpMessage.append( "POST " ).append( path ).append( " HTTP/1.1" ).append( HttpUtils.CRLF )
					.append( "User-Agent: Jolie" ).append( HttpUtils.CRLF )
					.append( "Host: " ).append( uri.getHost() ).append( HttpUtils.CRLF );

				if( checkBooleanParameter( HttpUtils.Parameters.COMPRESSION, true ) ) {
					String requestCompression = getStringParameter( HttpUtils.Parameters.REQUEST_COMPRESSION );
					if( requestCompression.equals( "gzip" ) || requestCompression.equals( "deflate" ) ) {
						encoding = requestCompression;
						httpMessage.append( "Accept-Encoding: " ).append( encoding ).append( HttpUtils.CRLF );
					} else {
						httpMessage.append( "Accept-Encoding: gzip, deflate" ).append( HttpUtils.CRLF );
					}
				}
			}

			if( !checkBooleanParameter( HttpUtils.Parameters.KEEP_ALIVE, true ) ) {
				if( inInputPort ) // we may do this only in input (server) mode
					channel().setToBeClosed( true );
				httpMessage.append( "Connection: close" ).append( HttpUtils.CRLF );
			}

			if( encoding != null && checkBooleanParameter( HttpUtils.Parameters.COMPRESSION, true ) ) {
				content = HttpUtils.encode( encoding, content, httpMessage );
			}

			// httpMessage.append( "Content-Type: application/json-rpc; charset=utf-8" + HttpUtils.CRLF );
			httpMessage.append( "Content-Type: application/json; charset=utf-8" ).append( HttpUtils.CRLF )
				.append( "Content-Length: " ).append( content.size() ).append( HttpUtils.CRLF )
				.append( HttpUtils.CRLF );

			if( checkBooleanParameter( HttpUtils.Parameters.DEBUG ) ) {
				interpreter
					.logInfo( new StringBuilder( "[JSON-RPC debug] Sending:\n" ).append( httpMessage.toString() )
						.append( content.toString( "utf-8" ) ) );
			}

			ostream.write( httpMessage.toString().getBytes( HttpUtils.URL_DECODER_ENC ) );
			ostream.write( content.getBytes() );
		}
	}

	@Override
	public void send( OutputStream ostream, CommMessage message, InputStream istream )
		throws IOException {
		setChannelInterface();
		HttpUtils.send( ostream, message, istream, inInputPort, channel(), this );
	}

	@Override
	public CommMessage recv_internal( InputStream istream, OutputStream ostream )
		throws IOException {
		if( checkStringParameter( Parameters.TRANSPORT, LSP ) ) {
			if( inInputPort && configurationPath().getValue().hasChildren( Parameters.CLIENT_LOCATION ) ) {
				try {
					OutputPort op = interpreter.getOutputPort(
						getParameterFirstValue( Parameters.CLIENT_OUTPUTPORT ).strValue() );
					if( op != null ) {
						channelInterface.merge( op.getInterface() );
					}
				} catch( InvalidIdException ex ) {
				}
				if( !getParameterFirstValue( Parameters.CLIENT_LOCATION ).isDefined() ) {
					// Setting the outport to the channel
					getParameterFirstValue( Parameters.CLIENT_LOCATION ).setValue( channel() );
				}
			}
			channel().setToBeClosed( false );
			LSPParser parser = new LSPParser( istream );
			LSPMessage message = parser.parse();
			// LSP supports only utf-8 encoding
			String charset = "utf-8";
			return recv_createCommMessage( message.content(), charset );
		} else {
			HttpParser parser = new HttpParser( istream );
			HttpMessage message = parser.parse();
			String charset = HttpUtils.getResponseCharset( message );
			HttpUtils.recv_checkForChannelClosing( message, channel() );

			if( inInputPort && message.type() != HttpMessage.Type.POST ) {
				throw new UnsupportedMethodException( "Only HTTP method POST allowed", Method.POST );
			}

			encoding = message.getProperty( "accept-encoding" );

			return recv_createCommMessage( message.content(), charset );
		}
	}

	private CommMessage recv_createCommMessage( byte[] messageContent, String charset )
		throws IOException {
		if( messageContent.length == 0 )
			return null; // error situation

		if( checkBooleanParameter( HttpUtils.Parameters.DEBUG ) ) {
			interpreter.logInfo(
				new StringBuilder( "[JSON-RPC debug] Receiving:\n" ).append( new String( messageContent, charset ) ) );
		}

		Value value = Value.create();
		JsUtils.parseJsonIntoValue( new InputStreamReader( new ByteArrayInputStream( messageContent ), charset ),
			value, false );

		String operation = value.getFirstChild( "method" ).strValue();

		// Resolving aliases
		if( hasParameter( Parameters.OSC ) ) {
			Value osc = getParameterFirstValue( Parameters.OSC );
			for( Entry< String, ValueVector > ev : osc.children().entrySet() ) {
				Value v = ev.getValue().get( 0 );
				if( v.hasChildren( Parameters.ALIAS ) ) {
					if( v.getFirstChild( Parameters.ALIAS ).strValue().equals( operation ) ) {
						operation = ev.getKey();
					}
				}
			}
		}

		if( !value.hasChildren( "id" ) ) {
			// JSON-RPC notification mechanism (method call with dropped result)
			if( !inInputPort ) {
				throw new IOException(
					"A JSON-RPC notification (message without \"id\") needs to be a request, not a response!" );
			}
			return new CommMessage( CommMessage.GENERIC_REQUEST_ID, operation,
				"/", value.getFirstChild( "params" ), null );
		}
		Object jsonRpcId = parseId( value.getFirstChild( "id" ) );
		if( inInputPort ) {
			jsonRpcIdMap.put( (long) jsonRpcId.hashCode(), jsonRpcId );
			return new CommMessage(
				jsonRpcId.hashCode(), operation,
				"/", value.getFirstChild( "params" ), null );
		} else if( value.hasChildren( "error" ) ) {
			long id = value.getFirstChild( "id" ).longValue();
			String operationName = jsonRpcOpMap.get( jsonRpcId );
			return new CommMessage( id, operationName, "/", null,
				new FaultException(
					value.getFirstChild( "error" ).getFirstChild( "message" ).strValue(),
					value.getFirstChild( "error" ).getFirstChild( "data" ) ) );
		} else {
			// Certain implementations do not provide a result if it is "void"
			long id = value.getFirstChild( "id" ).longValue();
			String operationName = jsonRpcOpMap.get( jsonRpcId );
			return new CommMessage( id, operationName, "/", value.getFirstChild( "result" ), null );
		}
	}

	private void setChannelInterface() {
		channelInterface = channel().parentPort().getInterface();
	}

	@Override
	public CommMessage recv( InputStream istream, OutputStream ostream )
		throws IOException {
		setChannelInterface();
		return HttpUtils.recv( istream, ostream, inInputPort, channel(), this );
	}
}
