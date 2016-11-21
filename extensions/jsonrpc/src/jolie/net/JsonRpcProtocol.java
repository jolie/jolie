/***************************************************************************
 *   Copyright (C) 2011 by Fabrizio Montesi <famontesi@gmail.com>          *
 *   Copyright (C) 2011 by Károly Szántó                                   *
 *   Copyright (C) 2011 by Giannakis Manthios                              *
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

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpVersion;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jolie.Interpreter;
import jolie.StatefulContext;
import jolie.js.JsUtils;
import jolie.net.http.HttpUtils;
import jolie.net.http.Method;
import jolie.net.http.UnsupportedMethodException;
import jolie.net.protocols.AsyncCommProtocol;
import jolie.runtime.ByteArray;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.VariablePath;
import jolie.runtime.typing.Type;

/**
 * 
 * @author Fabrizio Montesi
 * @author Károly Szántó
 * @author Giannakis Manthios
 *
 * 2014 Matthias Dieter Wallnöfer: conversion to JSONRPC over HTTP
 */
public class JsonRpcProtocol extends AsyncCommProtocol
{
	private final URI uri;
	private final Interpreter interpreter;
	private final boolean inInputPort;
	private String encoding;

	private final static int INITIAL_CAPACITY = 8;
	private final static float LOAD_FACTOR = 0.75f;
	
	private final Map< Long, String > jsonRpcIdMap;
	private final Map< String, String > jsonRpcOpMap;
	
	public String name()
	{
		return "jsonrpc";
	}
	
	public JsonRpcProtocol( VariablePath configurationPath, URI uri,
				Interpreter interpreter, boolean inInputPort )
	{
		super( configurationPath );
		this.uri = uri;
		this.interpreter = interpreter;
		this.inInputPort = inInputPort;
		
		// prepare the two maps
		this.jsonRpcIdMap = new HashMap<Long, String>(INITIAL_CAPACITY, LOAD_FACTOR);
		this.jsonRpcOpMap = new HashMap<String, String>(INITIAL_CAPACITY, LOAD_FACTOR);
	}
	
	public class JsonRpcCommMessageCodec extends MessageToMessageCodec<FullHttpMessage, StatefulMessage>
	{

		@Override
		protected void encode( ChannelHandlerContext ctx, StatefulMessage message, List<Object> out ) throws Exception
		{
			((CommCore.ExecutionContextThread) Thread.currentThread()).executionContext( message.context() );
//			System.out.println( "Sending: " + message.toString() );
			FullHttpMessage msg = buildJsonRpcMessage(message );
			out.add( msg );
		}

		@Override
		protected void decode( ChannelHandlerContext ctx, FullHttpMessage msg, List<Object> out ) throws Exception
		{
			if ( msg instanceof FullHttpRequest ) {
				FullHttpRequest request = (FullHttpRequest) msg;
//				System.out.println( "HTTP request ! (" + request.uri() + ")" );
			} else if ( msg instanceof FullHttpResponse ) {
				FullHttpResponse response = (FullHttpResponse) msg;
//				System.out.println( "HTTP response !" );
			}
			StatefulMessage message = recv_internal( msg );
//			System.out.println( "Decoded JSON-RPC message for operation: " + message.message().operationName() );
			out.add( message );
		}

	}
	
	@Override
	public void setupPipeline( ChannelPipeline pipeline )
	{
		if (inInputPort) {
			pipeline.addLast( new HttpServerCodec() );
			pipeline.addLast( new HttpContentCompressor() );
		} else {
			pipeline.addLast( new HttpClientCodec() );
			pipeline.addLast( new HttpContentDecompressor() );
		}
		pipeline.addLast( new HttpObjectAggregator( 65536 ) );
		pipeline.addLast( new JsonRpcCommMessageCodec() );
	}

	public FullHttpMessage buildJsonRpcMessage( StatefulMessage msg )
		throws IOException
	{
		CommMessage message = msg.message();
		StatefulContext ctx = msg.context();
		FullHttpMessage httpMessage;
		
		channel().setToBeClosed(!checkBooleanParameter(ctx, "keepAlive", true));

		if (!message.isFault() && message.hasGenericId() && inInputPort) {
			// JSON-RPC notification mechanism (method call with dropped result)
			// we just send HTTP status code 204
			FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NO_CONTENT);
			response.headers().add( HttpHeaderNames.SERVER, "Jolie" );
			return response;
		}
				
		Value value = Value.create();
		value.getFirstChild( "jsonrpc" ).setValue( "2.0" );
		if ( message.isFault() ) {
			Value error = value.getFirstChild( "error" );
			error.getFirstChild( "code" ).setValue( -32000 );
			error.getFirstChild( "message" ).setValue( message.fault().faultName() );
			error.getChildren( "data" ).set( 0, message.fault().value() );
			String jsonRpcId = jsonRpcIdMap.get( message.id() );
			error.getFirstChild( "id" ).setValue( jsonRpcId );
		} else {
			if ( inInputPort ) {
				value.getChildren( "result" ).set( 0, message.value() );
				String jsonRpcId = jsonRpcIdMap.get( message.id() );
				value.getFirstChild( "id" ).setValue( jsonRpcId );
			} else {
				jsonRpcOpMap.put( message.id() + "", message.operationName() );
				value.getFirstChild( "method" ).setValue( message.operationName() );
				if ( message.value().isDefined() || message.value().hasChildren() ) {
					// some implementations need an array here
					value.getFirstChild( "params" ).getChildren( JsUtils.JSONARRAY_KEY ).set( 0, message.value() );
				}
				value.getFirstChild( "id" ).setValue( message.id() );
			}
		}
		StringBuilder json = new StringBuilder();
		JsUtils.valueToJsonString( value, true, Type.UNDEFINED, json );
		ByteArray content = new ByteArray( json.toString().getBytes( "utf-8" ) );
				
		if (inInputPort) {
			// We're responding to a request
			httpMessage = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK );
			httpMessage.headers().add( HttpHeaderNames.SERVER, "Jolie" );
		} else {
			// We're sending a request
			String path = uri.getRawPath(); // TODO: fix this to consider resourcePaths
			if (path == null || path.length() == 0) {
				path = "*";
			}
			httpMessage = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, path);
			httpMessage.headers().add( HttpHeaderNames.USER_AGENT, "Jolie" );
			httpMessage.headers().add( HttpHeaderNames.HOST, uri.getHost() );

			if ( checkBooleanParameter( ctx, "compression", true ) ) {
				String requestCompression = getStringParameter( ctx, "requestCompression" );
				if ( requestCompression.equals( "gzip" ) || requestCompression.equals( "deflate" ) ) {
					encoding = requestCompression;
					httpMessage.headers().add( HttpHeaderNames.ACCEPT_ENCODING, encoding );
				} else {
					httpMessage.headers().add( HttpHeaderNames.ACCEPT_ENCODING, "gzip, deflate");
				}
			}
		}

		if (channel().toBeClosed()) {
			httpMessage.headers().add( HttpHeaderNames.CONNECTION, "close" );
		}

		//httpMessage.append( "Content-Type: application/json-rpc; charset=utf-8" + HttpUtils.CRLF );
		httpMessage.headers().add( HttpHeaderNames.CONTENT_TYPE, "application/json; charset=utf-8" );
		httpMessage.headers().add( HttpHeaderNames.CONTENT_LENGTH, content.size() );

		if (checkBooleanParameter(ctx, "debug", false)) {
			interpreter.logInfo("[JSON-RPC debug] Sending:\n" + httpMessage.toString() + content.toString( "utf-8" ));
		}
		httpMessage.content().writeBytes( content.getBytes() );
		return httpMessage;
	}

	public StatefulMessage recv_internal( FullHttpMessage message )
		throws IOException
	{
		String charset = HttpUtils.getCharset( null, message );
		HttpUtils.recv_checkForChannelClosing( message, channel() );

		if ( (message instanceof FullHttpResponse) && ((FullHttpResponse)message).status().code() >= 400) {
			throw new IOException("HTTP error: " + message.content().toString( Charset.forName( charset )) );
		}
		if (inInputPort && ((FullHttpRequest)message).method() != HttpMethod.POST) {
			throw new UnsupportedMethodException("Only HTTP method POST allowed!", Method.POST);
		}

		encoding = message.headers().get( HttpHeaderNames.ACCEPT_ENCODING );
		
		Value value = Value.create();
		if ( message.content().readableBytes() > 0 ) {
			
			if ( checkBooleanParameter( channel().getContextFor( CommMessage.GENERIC_ID ), "debug", false ) ) { // TODO figure out what context touse here...
				interpreter.logInfo( "[JSON-RPC debug] Receiving:\n" + message.content().toString( Charset.forName( charset) ));
			}
			
			byte[] content = new byte[message.content().readableBytes()];
			message.content().readBytes( content, 0, content.length);
			JsUtils.parseJsonIntoValue(new InputStreamReader(new ByteArrayInputStream(content), charset), value, false);

			if (!value.hasChildren("id")) {
				// JSON-RPC notification mechanism (method call with dropped result)
				if (!inInputPort) {
					throw new IOException("A JSON-RPC notification (message without \"id\") needs to be a request, not a response!");
				}
				return new StatefulMessage( new CommMessage(CommMessage.GENERIC_ID, value.getFirstChild("method").strValue(),
						    "/", value.getFirstChild("params"), null), channel().getContextFor( CommMessage.GENERIC_ID ));
			}
			String jsonRpcId = value.getFirstChild("id").strValue();
			StatefulContext ctx = channel().getContextFor( Long.valueOf(jsonRpcId) );
			if ( inInputPort ) {
				jsonRpcIdMap.put((long)jsonRpcId.hashCode(), jsonRpcId);
				return new StatefulMessage( new CommMessage(jsonRpcId.hashCode(), value.getFirstChild("method").strValue(),
						    "/", value.getFirstChild("params"), null), ctx);
			} else if (value.hasChildren("error")) {
				String operationName = jsonRpcOpMap.get(jsonRpcId);
				return new StatefulMessage( new CommMessage(Long.valueOf(jsonRpcId), operationName, "/", null,
					new FaultException(
						value.getFirstChild( "error" ).getFirstChild( "message" ).strValue(),
						value.getFirstChild( "error" ).getFirstChild( "data" )
					)
				), ctx);
			} else {
				// Certain implementations do not provide a result if it is "void"
				String operationName = jsonRpcOpMap.get(jsonRpcId);
				return new StatefulMessage( new CommMessage(Long.valueOf(jsonRpcId), operationName, "/", value.getFirstChild("result"), null), ctx);
			}
		}
		return null; // error situation
	}
	
	@Override
	public boolean isThreadSafe()
	{
		return false;
	}
}
