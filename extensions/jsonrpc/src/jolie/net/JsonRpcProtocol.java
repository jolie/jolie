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

import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
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
	
	public static EncodedJsonRpcContent notification = new EncodedJsonRpcContent(null, null, null);
	
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
	
	public class JsonRpcHttpCommMessageCodec extends MessageToMessageCodec<FullHttpMessage, EncodedJsonRpcContent>
	{

		@Override
		protected void encode( ChannelHandlerContext ctx, EncodedJsonRpcContent content, List<Object> out ) throws Exception
		{
			((CommCore.ExecutionContextThread) Thread.currentThread()).executionContext( content.context() );
			FullHttpMessage msg = buildHttpJsonRpcMessage( content );
			out.add( msg );
		}

		@Override
		protected void decode( ChannelHandlerContext ctx, FullHttpMessage msg, List<Object> out ) throws Exception
		{
			if ( msg instanceof FullHttpRequest ) {
				FullHttpRequest request = (FullHttpRequest) msg;
			} else if ( msg instanceof FullHttpResponse ) {
				FullHttpResponse response = (FullHttpResponse) msg;
			}
			EncodedJsonRpcContent content = recv_http_internal( msg );
			out.add( content );
		}

	}	
	
	public class JsonRpcCommMessageCodec extends MessageToMessageCodec<EncodedJsonRpcContent, StatefulMessage>
	{

		@Override
		protected void encode( ChannelHandlerContext ctx, StatefulMessage message, List<Object> out ) throws Exception
		{
			((CommCore.ExecutionContextThread) Thread.currentThread()).executionContext( message.context() );
			EncodedJsonRpcContent content = buildJsonRpcMessage(message );
			out.add( content );
		}

		@Override
		protected void decode( ChannelHandlerContext ctx, EncodedJsonRpcContent content, List<Object> out ) throws Exception
		{
			StatefulMessage message = recv_internal( content );
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
		pipeline.addLast( new JsonRpcHttpCommMessageCodec() );
		setupWrapablePipeline( pipeline );
	}

	@Override
	public void setupWrapablePipeline( ChannelPipeline pipeline )
	{
		pipeline.addLast( new JsonRpcCommMessageCodec() );
	}
	
	public FullHttpMessage buildHttpJsonRpcMessage( EncodedJsonRpcContent content )
		throws IOException
	{
		StatefulContext ctx = content.context();
		FullHttpMessage httpMessage;
				
		if (content == notification) {
			FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NO_CONTENT);
			response.headers().add( HttpHeaderNames.SERVER, "Jolie" );
			return response;
		}
		
		if (inInputPort) {
			// We're responding to a request
			httpMessage = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content.content() );
			httpMessage.headers().add( HttpHeaderNames.SERVER, "Jolie" );
		} else {
			// We're sending a request
			String path = uri.getRawPath(); // TODO: fix this to consider resourcePaths
			if (path == null || path.length() == 0) {
				path = "*";
			}
			httpMessage = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, path, content.content() );
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
		httpMessage.headers().add( HttpHeaderNames.CONTENT_LENGTH, httpMessage.content().readableBytes() );

		if (checkBooleanParameter(ctx, "debug", false)) {
			interpreter.logInfo("[JSON-RPC debug] Sending:\n" + httpMessage.toString() );
		}
		return httpMessage;
	}
	
	public EncodedJsonRpcContent buildJsonRpcMessage( StatefulMessage msg )
		throws IOException
	{
		CommMessage message = msg.message();
		StatefulContext ctx = msg.context();
		
		channel().setToBeClosed(!checkBooleanParameter(ctx, "keepAlive", true));

		if (!message.isFault() && message.hasGenericId() && inInputPort) {
			return notification;
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
			if ( !message.isRequest() ) {
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
		return new EncodedJsonRpcContent( Unpooled.wrappedBuffer( json.toString().getBytes( "utf-8" ) ), Charset.forName( "utf-8" ), ctx );
	}

	public EncodedJsonRpcContent recv_http_internal( FullHttpMessage message )
		throws IOException
	{
		Charset charset = Charset.forName( HttpUtils.getCharset( null, message ) );
		HttpUtils.recv_checkForChannelClosing( message, channel() );
		if ( (message instanceof FullHttpResponse) && ((FullHttpResponse)message).status().code() >= 400) {
			throw new IOException("HTTP error: " + message.content().toString( charset ));
		}
		if (inInputPort && ((FullHttpRequest)message).method() != HttpMethod.POST) {
			throw new UnsupportedMethodException("Only HTTP method POST allowed!", Method.POST);
		}

		encoding = message.headers().get( HttpHeaderNames.ACCEPT_ENCODING );
		
		if ( message.content().readableBytes() > 0 ) {
			return new EncodedJsonRpcContent( message.content().retain(), charset, null );
		}
		return null; // error situation
	}
	
	public StatefulMessage recv_internal(EncodedJsonRpcContent content)
		throws IOException
	{
		
		Value value = Value.create();
		
		JsUtils.parseJsonIntoValue(new InputStreamReader(new ByteBufInputStream(content.content()), content.charset()), value, false);
		boolean isRequest = value.hasChildren("method");
			
		if (!value.hasChildren("id")) {
			StatefulContext genericContext = channel().getContextFor( CommMessage.GENERIC_ID, isRequest );
			((CommCore.ExecutionContextThread) Thread.currentThread()).executionContext( genericContext );
			if ( checkBooleanParameter( genericContext , "debug", false ) ) { // TODO figure out what context touse here...
				interpreter.logInfo( "[JSON-RPC debug] Receiving:\n" + content.text() );
			}
			// JSON-RPC notification mechanism (method call with dropped result)
			return new StatefulMessage( new CommMessage(CommMessage.GENERIC_ID, value.getFirstChild("method").strValue(),
						"/", value.getFirstChild("params"), null, isRequest), genericContext);
		}
		
		String jsonRpcId = value.getFirstChild("id").strValue();
		StatefulContext ctx = channel().getContextFor( Long.valueOf(jsonRpcId), isRequest );
		
		((CommCore.ExecutionContextThread) Thread.currentThread()).executionContext( ctx );
		getParameterFirstValue(ctx, "channelLocation").setValue( channel() ); // TODO remove me for testing
		
		
		if ( isRequest ) {
			jsonRpcIdMap.put((long)jsonRpcId.hashCode(), jsonRpcId);
			return new StatefulMessage( new CommMessage(jsonRpcId.hashCode(), value.getFirstChild("method").strValue(),
						"/", value.getFirstChild("params"), null, isRequest), ctx);
		} else if (value.hasChildren("error")) {
			String operationName = jsonRpcOpMap.get(jsonRpcId);
			return new StatefulMessage( new CommMessage(Long.valueOf(jsonRpcId), operationName, "/", null,
				new FaultException(
					value.getFirstChild( "error" ).getFirstChild( "message" ).strValue(),
					value.getFirstChild( "error" ).getFirstChild( "data" )
				),  isRequest
			), ctx);
		} else {
			// Certain implementations do not provide a result if it is "void"
			String operationName = jsonRpcOpMap.get(jsonRpcId);
			return new StatefulMessage( new CommMessage(Long.valueOf(jsonRpcId), operationName, "/", value.getFirstChild("result"), null, isRequest), ctx);
		}
	}
	
	@Override
	public boolean isThreadSafe()
	{
		return false;
	}
}
