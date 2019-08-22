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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import jolie.Interpreter;
import jolie.net.http.HttpMessage;
import jolie.net.http.HttpParser;
import jolie.net.http.HttpUtils;
import jolie.net.http.Method;
import jolie.net.http.UnsupportedMethodException;
import jolie.net.protocols.SequentialCommProtocol;
import jolie.runtime.ByteArray;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.VariablePath;
import jolie.runtime.typing.Type;
import jolie.js.JsUtils;

/**
 * 
 * @author Fabrizio Montesi
 * @author Károly Szántó
 * @author Giannakis Manthios
 *
 * 2014 Matthias Dieter Wallnöfer: conversion to JSONRPC over HTTP
 */
public class JsonRpcProtocol extends SequentialCommProtocol implements HttpUtils.HttpProtocol
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

	public void send_internal( OutputStream ostream, CommMessage message, InputStream istream )
		throws IOException
	{
		channel().setToBeClosed(!checkBooleanParameter("keepAlive", true));

		if (!message.isFault() && message.hasGenericId() && inInputPort) {
			// JSON-RPC notification mechanism (method call with dropped result)
			// we just send HTTP status code 204
			StringBuilder httpMessage = new StringBuilder();
			httpMessage.append( "HTTP/1.1 204 No Content" + HttpUtils.CRLF );
			httpMessage.append( "Server: Jolie" + HttpUtils.CRLF + HttpUtils.CRLF );
			ostream.write( httpMessage.toString().getBytes( "utf-8" ) );
			return;
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
				
		StringBuilder httpMessage = new StringBuilder();
		if (inInputPort) {
			// We're responding to a request
			httpMessage.append( "HTTP/1.1 200 OK" + HttpUtils.CRLF );
			httpMessage.append( "Server: Jolie" + HttpUtils.CRLF );
		} else {
			// We're sending a request
			String path = uri.getRawPath(); // TODO: fix this to consider resourcePaths
			if (path == null || path.length() == 0) {
				path = "*";
			}
			httpMessage.append( "POST " + path + " HTTP/1.1" + HttpUtils.CRLF );
			httpMessage.append( "User-Agent: Jolie" + HttpUtils.CRLF );
			httpMessage.append( "Host: " + uri.getHost() + HttpUtils.CRLF );

			if ( checkBooleanParameter( "compression", true ) ) {
				String requestCompression = getStringParameter( "requestCompression" );
				if ( requestCompression.equals( "gzip" ) || requestCompression.equals( "deflate" ) ) {
					encoding = requestCompression;
					httpMessage.append( "Accept-Encoding: " + encoding + HttpUtils.CRLF );
				} else {
					httpMessage.append( "Accept-Encoding: gzip, deflate" + HttpUtils.CRLF );
				}
			}
		}

		if (channel().toBeClosed()) {
			httpMessage.append( "Connection: close" + HttpUtils.CRLF );
		}

		if ( encoding != null && checkBooleanParameter( "compression", true ) ) {
			content = HttpUtils.encode( encoding, content, httpMessage );
		}

		//httpMessage.append( "Content-Type: application/json-rpc; charset=utf-8" + HttpUtils.CRLF );
		httpMessage.append( "Content-Type: application/json; charset=utf-8" + HttpUtils.CRLF );
		httpMessage.append( "Content-Length: " + content.size() + HttpUtils.CRLF + HttpUtils.CRLF );

		if (checkBooleanParameter("debug", false)) {
			interpreter.logInfo("[JSON-RPC debug] Sending:\n" + httpMessage.toString() + content.toString( "utf-8" ));
		}

		ostream.write( httpMessage.toString().getBytes( HttpUtils.URL_DECODER_ENC ) );
		ostream.write( content.getBytes() );
	}

	public void send( OutputStream ostream, CommMessage message, InputStream istream )
		throws IOException
	{
		HttpUtils.send( ostream, message, istream, inInputPort, channel(), this );
	}

	public CommMessage recv_internal( InputStream istream, OutputStream ostream )
		throws IOException
	{
		HttpParser parser = new HttpParser( istream );
		HttpMessage message = parser.parse();
		String charset = HttpUtils.getCharset( null, message );
		HttpUtils.recv_checkForChannelClosing( message, channel() );

		if (message.isError()) {
			throw new IOException("HTTP error: " + new String(message.content(), charset));
		}
		if (inInputPort && message.type() != HttpMessage.Type.POST) {
			throw new UnsupportedMethodException("Only HTTP method POST allowed!", Method.POST);
		}

		encoding = message.getProperty( "accept-encoding" );

		Value value = Value.create();
		if ( message.size() > 0 ) {
			if ( checkBooleanParameter( "debug", false ) ) {
				interpreter.logInfo( "[JSON-RPC debug] Receiving:\n" + new String( message.content(), charset ) );
			}

			JsUtils.parseJsonIntoValue(new InputStreamReader(new ByteArrayInputStream(message.content()), charset), value, false);

			if (!value.hasChildren("id")) {
				// JSON-RPC notification mechanism (method call with dropped result)
				if (!inInputPort) {
					throw new IOException("A JSON-RPC notification (message without \"id\") needs to be a request, not a response!");
				}
				return new CommMessage(CommMessage.GENERIC_ID, value.getFirstChild("method").strValue(),
						    "/", value.getFirstChild("params"), null);
			}
			String jsonRpcId = value.getFirstChild("id").strValue();
			if ( inInputPort ) {
				jsonRpcIdMap.put((long)jsonRpcId.hashCode(), jsonRpcId);
				return new CommMessage(jsonRpcId.hashCode(), value.getFirstChild("method").strValue(),
						    "/", value.getFirstChild("params"), null);
			} else if (value.hasChildren("error")) {
				String operationName = jsonRpcOpMap.get(jsonRpcId);
				return new CommMessage(Long.valueOf(jsonRpcId), operationName, "/", null,
					new FaultException(
						value.getFirstChild( "error" ).getFirstChild( "message" ).strValue(),
						value.getFirstChild( "error" ).getFirstChild( "data" )
					)
				);
			} else {
				// Certain implementations do not provide a result if it is "void"
				String operationName = jsonRpcOpMap.get(jsonRpcId);
				return new CommMessage(Long.valueOf(jsonRpcId), operationName, "/", value.getFirstChild("result"), null);
			}
		}
		return null; // error situation
	}

	public CommMessage recv( InputStream istream, OutputStream ostream )
		throws IOException
	{
		return HttpUtils.recv( istream, ostream, inInputPort, channel(), this );
	}
}
