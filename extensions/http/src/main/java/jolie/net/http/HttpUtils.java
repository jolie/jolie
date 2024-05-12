/***************************************************************************
 *   Copyright (C) 2008 by Fabrizio Montesi                                *
 *   Copyright (C) 2015-24 by Matthias Dieter Wallnöfer                    *
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

package jolie.net.http;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.StandardCharsets;

import java.util.AbstractMap;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringJoiner;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import jolie.Interpreter;
import jolie.js.JsUtils;
import jolie.net.CommChannel;
import jolie.net.CommMessage;
import jolie.runtime.ByteArray;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.typing.Type;
import jolie.xml.XmlUtils;

/**
 * Utilities for handling HTTP messages.
 * 
 * @author Fabrizio Montesi
 */
public class HttpUtils {
	public static final String CRLF = new String( new char[] { 13, 10 } );
	public static final String BOUNDARY = "----jol13h77p77bound4r155";
	public static final String URL_DECODER_ENC = StandardCharsets.UTF_8.toString();
	public static final String DEFAULT_CONTENT_CHARSET = StandardCharsets.UTF_8.toString();
	public static final String DEFAULT_CONTENT_TYPE = ContentTypes.APPLICATION_OCTET_STREAM; // default content type per
																								// RFC 2616#7.2.1
	public static final String DEFAULT_FORMAT = "xml";

	public static final int DEFAULT_STATUS_CODE = 200;
	public static final int DEFAULT_FAULT_STATUS_CODE = 500;
	public static final int DEFAULT_REDIRECTION_STATUS_CODE = 303;

	private static final Map< Integer, String > STATUS_CODE_DESCRIPTIONS = new HashMap<>();
	private static final Set< Integer > LOCATION_REQUIRED_STATUS_CODES = new HashSet<>();

	public static interface Protocol {
		CommMessage recv_internal( InputStream istream, OutputStream ostream ) throws IOException;

		void send_internal( OutputStream ostream, CommMessage message, InputStream istream ) throws IOException;
	}

	public static class EncodedContent {
		public ByteArray content = null;
		public String contentType = DEFAULT_CONTENT_TYPE;
		public String contentDisposition = "";
	}

	public static class DecodedMessage {
		public String operationName = null;
		public Value value = Value.create();
		public String resourcePath = "/";
		public long id = CommMessage.GENERIC_REQUEST_ID;
	}

	public static class Headers {
		public static final String CONTENT_TYPE = "Content-Type";
		public static final String JOLIE_MESSAGE_ID = "X-Jolie-MessageID";
		public static final String JOLIE_RESOURCE_PATH = "X-Jolie-ServicePath";
		public static final String JOLIE_OPERATION = "X-Jolie-Operation";
	}

	public static class ContentTypes {
		public static final String TEXT_XML = "text/xml";
		public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
		public static final String TEXT_HTML = "text/html";
		public static final String MULTIPART_FORM_DATA = "multipart/form-data";
		public static final String APPLICATION_X_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";
		public static final String APPLICATION_JSON = "application/json";
		public static final String APPLICATION_NDJSON = "application/x-ndjson";
		public static final String TEXT_PLAIN = "text/plain";
	}

	public static class Formats {
		public static final String XML = "xml";
		public static final String BINARY = "binary";
		public static final String HTML = "html";
		public static final String FORM_DATA = "multipart/form-data";
		public static final String FORM_URLENCODED = "x-www-form-urlencoded";
		public static final String JSON = "json";
		public static final String NDJSON = "ndjson";
		public static final String RAW = "raw";
	}

	public static class Parameters {
		public static final String KEEP_ALIVE = "keepAlive";
		public static final String DEBUG = "debug";
		public static final String COOKIES = "cookies";
		public static final String METHOD = "method";
		public static final String ALIAS = "alias";
		public static final String MULTIPART_HEADERS = "multipartHeaders";
		public static final String CONCURRENT = "concurrent";
		public static final String USER_AGENT = "userAgent";
		public static final String HOST = "host";
		public static final String HEADERS = "headers";
		public static final String HEADERS_WILDCARD = "*";
		public static final String REQUEST_HEADERS = "requestHeaders";
		public static final String ADD_HEADERS = "addHeader";
		public static final String STATUS_CODE = "statusCode";
		public static final String REDIRECT = "redirect";
		public static final String DEFAULT_OPERATION = "default";
		public static final String COMPRESSION = "compression";
		public static final String COMPRESSION_TYPES = "compressionTypes";
		public static final String REQUEST_COMPRESSION = "requestCompression";
		public static final String FORMAT = "format";
		public static final String RESPONSE_HEADER = "responseHeaders";
		public static final String JSON_ENCODING = "json_encoding";
		public static final String REQUEST_USER = "request";
		public static final String RESPONSE_USER = "response";
		public static final String HEADER_USER = "headers";
		public static final String CHARSET = "charset";
		public static final String CONTENT_TYPE = "contentType";
		public static final String CONTENT_TRANSFER_ENCODING = "contentTransferEncoding";
		public static final String CONTENT_DISPOSITION = "contentDisposition";
		public static final String DROP_URI_PATH = "dropURIPath";
		public static final String CACHE_CONTROL = "cacheControl";
		public static final String FORCE_CONTENT_DECODING = "forceContentDecoding";
		public static final String TEMPLATE = "template";
		public static final String OUTGOING_HEADERS = "outHeaders";
		public static final String INCOMING_HEADERS = "inHeaders";
		public static final String STATUS_CODES = "statusCodes";
		public static final String FORCE_RECEIVING_CHARSET = "forceRecvCharset";

		public static class MultiPartHeaders {
			public static final String FILENAME = "filename";
		}
	}

	static {
		LOCATION_REQUIRED_STATUS_CODES.add( 301 );
		LOCATION_REQUIRED_STATUS_CODES.add( 302 );
		LOCATION_REQUIRED_STATUS_CODES.add( 303 );
		LOCATION_REQUIRED_STATUS_CODES.add( 307 );
		LOCATION_REQUIRED_STATUS_CODES.add( 308 );
	}

	static {
		// Initialise the HTTP Status code map.
		STATUS_CODE_DESCRIPTIONS.put( 100, "Continue" );
		STATUS_CODE_DESCRIPTIONS.put( 101, "Switching Protocols" );
		STATUS_CODE_DESCRIPTIONS.put( 102, "Processing" );
		STATUS_CODE_DESCRIPTIONS.put( 200, "OK" );
		STATUS_CODE_DESCRIPTIONS.put( 201, "Created" );
		STATUS_CODE_DESCRIPTIONS.put( 202, "Accepted" );
		STATUS_CODE_DESCRIPTIONS.put( 203, "Non-Authoritative Information" );
		STATUS_CODE_DESCRIPTIONS.put( 204, "No Content" );
		STATUS_CODE_DESCRIPTIONS.put( 205, "Reset Content" );
		STATUS_CODE_DESCRIPTIONS.put( 206, "Partial Content" );
		STATUS_CODE_DESCRIPTIONS.put( 207, "Multi-Status" );
		STATUS_CODE_DESCRIPTIONS.put( 208, "Already Reported" );
		STATUS_CODE_DESCRIPTIONS.put( 226, "IM Used" );
		STATUS_CODE_DESCRIPTIONS.put( 300, "Multiple Choices" );
		STATUS_CODE_DESCRIPTIONS.put( 301, "Moved Permanently" );
		STATUS_CODE_DESCRIPTIONS.put( 302, "Found" );
		STATUS_CODE_DESCRIPTIONS.put( 303, "See Other" );
		STATUS_CODE_DESCRIPTIONS.put( 304, "Not Modified" );
		STATUS_CODE_DESCRIPTIONS.put( 305, "Use Proxy" );
		STATUS_CODE_DESCRIPTIONS.put( 306, "Reserved" );
		STATUS_CODE_DESCRIPTIONS.put( 307, "Temporary Redirect" );
		STATUS_CODE_DESCRIPTIONS.put( 308, "Permanent Redirect" );
		STATUS_CODE_DESCRIPTIONS.put( 400, "Bad Request" );
		STATUS_CODE_DESCRIPTIONS.put( 401, "Unauthorized" );
		STATUS_CODE_DESCRIPTIONS.put( 402, "Payment Required" );
		STATUS_CODE_DESCRIPTIONS.put( 403, "Forbidden" );
		STATUS_CODE_DESCRIPTIONS.put( 404, "Not Found" );
		STATUS_CODE_DESCRIPTIONS.put( 405, "Method Not Allowed" );
		STATUS_CODE_DESCRIPTIONS.put( 406, "Not Acceptable" );
		STATUS_CODE_DESCRIPTIONS.put( 407, "Proxy Authentication Required" );
		STATUS_CODE_DESCRIPTIONS.put( 408, "Request Timeout" );
		STATUS_CODE_DESCRIPTIONS.put( 409, "Conflict" );
		STATUS_CODE_DESCRIPTIONS.put( 410, "Gone" );
		STATUS_CODE_DESCRIPTIONS.put( 411, "Length Required" );
		STATUS_CODE_DESCRIPTIONS.put( 412, "Precondition Failed" );
		STATUS_CODE_DESCRIPTIONS.put( 413, "Request Entity Too Large" );
		STATUS_CODE_DESCRIPTIONS.put( 414, "Request-URI Too Long" );
		STATUS_CODE_DESCRIPTIONS.put( 415, "Unsupported Media Type" );
		STATUS_CODE_DESCRIPTIONS.put( 416, "Requested Range Not Satisfiable" );
		STATUS_CODE_DESCRIPTIONS.put( 417, "Expectation Failed" );
		STATUS_CODE_DESCRIPTIONS.put( 422, "Unprocessable Entity" );
		STATUS_CODE_DESCRIPTIONS.put( 423, "Locked" );
		STATUS_CODE_DESCRIPTIONS.put( 424, "Failed Dependency" );
		STATUS_CODE_DESCRIPTIONS.put( 426, "Upgrade Required" );
		STATUS_CODE_DESCRIPTIONS.put( 427, "Unassigned" );
		STATUS_CODE_DESCRIPTIONS.put( 428, "Precondition Required" );
		STATUS_CODE_DESCRIPTIONS.put( 429, "Too Many Requests" );
		STATUS_CODE_DESCRIPTIONS.put( 430, "Unassigned" );
		STATUS_CODE_DESCRIPTIONS.put( 431, "Request Header Fields Too Large" );
		STATUS_CODE_DESCRIPTIONS.put( 500, "Internal Server Error" );
		STATUS_CODE_DESCRIPTIONS.put( 501, "Not Implemented" );
		STATUS_CODE_DESCRIPTIONS.put( 502, "Bad Gateway" );
		STATUS_CODE_DESCRIPTIONS.put( 503, "Service Unavailable" );
		STATUS_CODE_DESCRIPTIONS.put( 504, "Gateway Timeout" );
		STATUS_CODE_DESCRIPTIONS.put( 505, "HTTP Version Not Supported" );
		STATUS_CODE_DESCRIPTIONS.put( 507, "Insufficient Storage" );
		STATUS_CODE_DESCRIPTIONS.put( 508, "Loop Detected" );
		STATUS_CODE_DESCRIPTIONS.put( 509, "Unassigned" );
		STATUS_CODE_DESCRIPTIONS.put( 510, "Not Extended" );
		STATUS_CODE_DESCRIPTIONS.put( 511, "Network Authentication Required" );
	}

	public static boolean isLocationNeeded( int statusCode ) {
		return LOCATION_REQUIRED_STATUS_CODES.contains( statusCode );
	}

	public static boolean isStatusCodeValid( int statusCode ) {
		return STATUS_CODE_DESCRIPTIONS.containsKey( statusCode );
	}

	public static String getStatusCodeDescription( int statusCode ) {
		return String.format( "%d %s", statusCode, STATUS_CODE_DESCRIPTIONS.get( statusCode ) );
	}

	// Checks if the message requests the channel to be closed or kept open
	public static void recv_checkForChannelClosing( HttpMessage message, CommChannel channel ) {
		if( channel != null ) {
			if( message.isResponse() ) { // https://tools.ietf.org/html/rfc7230#section-6.6
				HttpMessage.Version version = message.version();
				if( version == null || version.equals( HttpMessage.Version.HTTP_1_1 ) ) {
					// The default is to keep the connection open, unless Connection: close is specified
					channel
						.setToBeClosed( message.getPropertyOrEmptyString( "connection" ).equalsIgnoreCase( "close" ) );
				} else if( version.equals( HttpMessage.Version.HTTP_1_0 ) ) {
					// The default is to close the connection, unless Connection: Keep-Alive is specified
					channel.setToBeClosed(
						!message.getPropertyOrEmptyString( "connection" ).equalsIgnoreCase( "keep-alive" ) );
				}
			} else {
				// IMPORTANT: in input mode we need to deliver the response, so no immediate close!
				channel.setToBeClosed( false );
			}
		}
	}

	private static void errorGenerator( OutputStream ostream, IOException e ) throws IOException {
		StringBuilder httpMessage = new StringBuilder( "HTTP/1.1 " );
		if( e instanceof UnsupportedEncodingException ) { // 415 Unsupported Media Type
			httpMessage.append( getStatusCodeDescription( 415 ) ).append( CRLF );
		} else if( e instanceof UnsupportedMethodException ) {
			UnsupportedMethodException ex = (UnsupportedMethodException) e;
			if( ex.allowedMethods() == null ) { // 501 Not Implemented
				httpMessage.append( getStatusCodeDescription( 501 ) ).append( CRLF );
			} else { // 405 Method Not Allowed
				httpMessage.append( getStatusCodeDescription( 405 ) ).append( CRLF );
				httpMessage.append( "Allowed: " );
				Method[] methods = ex.allowedMethods();
				for( int i = 0; i < methods.length; i++ ) {
					httpMessage.append( methods[ i ].id() ).append( i + 1 < methods.length ? ", " : "" );
				}
				httpMessage.append( CRLF );
			}
		} else if( e instanceof UnsupportedHttpVersionException ) { // 505 HTTP Version Not Supported
			httpMessage.append( getStatusCodeDescription( 505 ) ).append( CRLF );
		} else { // 500 Internal Server Error
			httpMessage.append( getStatusCodeDescription( 500 ) ).append( CRLF );
		}
		String message = e.getMessage() != null ? e.getMessage() : e.toString();
		ByteArray content = new ByteArray( message.getBytes( StandardCharsets.UTF_8 ) );
		httpMessage.append( "Server: Jolie" ).append( CRLF )
			.append( "Content-Type: text/plain; charset=utf-8" ).append( CRLF )
			.append( "Content-Length: " ).append( content.size() ).append( CRLF ).append( CRLF );
		ostream.write( httpMessage.toString().getBytes( StandardCharsets.UTF_8 ) );
		ostream.write( content.getBytes() );
		ostream.flush();
	}

	public static CommMessage recv( InputStream istream, OutputStream ostream, boolean inInputPort, CommChannel channel,
		Protocol protocol )
		throws IOException {
		try {
			return protocol.recv_internal( istream, ostream );
		} catch( IOException e ) {
			if( inInputPort && channel.isOpen() ) {
				HttpUtils.errorGenerator( ostream, e );
			}
			throw e;
		}
	}

	public static void send( OutputStream ostream, CommMessage message, InputStream istream, boolean inInputPort,
		CommChannel channel, Protocol protocol )
		throws IOException {
		try {
			protocol.send_internal( ostream, message, istream );
		} catch( IOException e ) {
			if( inInputPort && channel.isOpen() ) {
				HttpUtils.errorGenerator( ostream, e );
				// Do not re-throw an I/O exception on a working server when the client could be notified.
				// E.g. it could have requested an invalid operation or passed an unsupported media format. In this
				// case the server informs the client about its mistake and then it may continue to run.
				// If this has not been possible (another I/O exception thrown) then we definitely need to bail out.
				Interpreter.getInstance().logInfo( e.getMessage() );
				return;
			}
			throw e;
		}
	}

	public static String getResponseCharset( HttpMessage message ) {
		if( message != null && message.getProperty( "content-type" ) != null ) {
			String[] contentType = message.getProperty( "content-type" ).split( ";" );
			for( int i = 1; i < contentType.length; i++ ) {
				if( contentType[ i ].toLowerCase().contains( "charset" ) ) {
					String[] pair = contentType[ i ].split( "=", 2 );
					if( pair.length == 2 ) {
						return pair[ 1 ];
					}
				}
			}
		}
		return DEFAULT_CONTENT_CHARSET; // if no charset has been passed to us stick to the default one
	}

	private static PriorityQueue< Map.Entry< Double, String > > parseAcceptHeaders( String headerProperty ) {
		PriorityQueue< Map.Entry< Double, String > > values = new PriorityQueue<>( new Comparator<>() {
			@Override
			public int compare( Entry< Double, String > arg0, Entry< Double, String > arg1 ) {
				// inverse order
				return -Double.compare( arg0.getKey(), arg1.getKey() );
			}
		} );

		if( headerProperty == null )
			return values;

		for( String token : headerProperty.split( "," ) ) {
			double weight = 1.;

			String[] params = token.split( ";" );
			if( params.length < 1 )
				continue; // illegal token
			String value = params[ 0 ].trim().toLowerCase();
			if( value.isEmpty() )
				continue; // illegal token

			for( int i = 1; i < params.length; i++ ) {
				String[] pair = params[ i ].trim().split( "=", 2 );
				if( pair.length == 2 && pair[ 0 ].equals( "q" ) ) {
					try {
						// assume weight 1. on parsing errors
						double parsedWeight = Double.parseDouble( pair[ 1 ] );
						if( parsedWeight >= 0. && parsedWeight <= 1. )
							weight = parsedWeight;
					} catch( NumberFormatException ex ) {
					}
				}
			}

			values.add( new AbstractMap.SimpleEntry<>( weight, value ) );
		}
		return values;
	}

	public static String getRequestFormat( HttpMessage message ) {
		for( Map.Entry< Double, String > type : parseAcceptHeaders( message.getProperty( "accept" ) ) ) {
			if( !type.getValue().contains( "/" ) || type.getValue().contains( "*" ) )
				continue; // we don't consider wildcard types

			switch( type.getValue() ) {
			case ContentTypes.TEXT_XML:
				return Formats.XML;
			case ContentTypes.APPLICATION_OCTET_STREAM:
				return Formats.BINARY;
			case ContentTypes.TEXT_HTML:
				return Formats.HTML;
			case ContentTypes.MULTIPART_FORM_DATA:
				return Formats.FORM_DATA;
			case ContentTypes.APPLICATION_X_WWW_FORM_URLENCODED:
				return Formats.FORM_URLENCODED;
			case ContentTypes.APPLICATION_JSON:
				return Formats.JSON;
			case ContentTypes.APPLICATION_NDJSON:
				return Formats.NDJSON;
			case ContentTypes.TEXT_PLAIN:
				return Formats.RAW;
			}
		}
		return null;
	}

	public static String getRequestCharset( HttpMessage message ) {
		for( Map.Entry< Double, String > charset : parseAcceptHeaders( message.getProperty( "accept-charset" ) ) ) {
			try {
				if( Charset.isSupported( charset.getValue() ) )
					return charset.getValue();
			} catch( IllegalCharsetNameException ex ) {
			}
		}
		return null;
	}

	public static ByteArray encode( String encodingHeader, ByteArray content, StringBuilder headerBuilder )
		throws IOException {
		for( Map.Entry< Double, String > encoding : parseAcceptHeaders( encodingHeader ) ) { // accept-encoding
			// RFC 7231 section-5.3.4 introduced the "*" (any) option, we opt for gzip as a sane default
			if( encoding.getValue().equals( "gzip" ) || encoding.getValue().equals( "*" ) ) {
				ByteArrayOutputStream baOutStream = new ByteArrayOutputStream();
				GZIPOutputStream outStream = new GZIPOutputStream( baOutStream );
				outStream.write( content.getBytes() );
				outStream.close();
				content = new ByteArray( baOutStream.toByteArray() );
				headerBuilder.append( "Content-Encoding: gzip" ).append( HttpUtils.CRLF );
				return content;
			} else if( encoding.getValue().equals( "deflate" ) ) {
				ByteArrayOutputStream baOutStream = new ByteArrayOutputStream();
				DeflaterOutputStream outStream = new DeflaterOutputStream( baOutStream );
				outStream.write( content.getBytes() );
				outStream.close();
				content = new ByteArray( baOutStream.toByteArray() );
				headerBuilder.append( "Content-Encoding: deflate" ).append( HttpUtils.CRLF );
				return content;
			}
		}
		return content;
	}

	public static String prepareSendDebugString( CharSequence header, EncodedContent encodedContent, String charset,
		boolean showContent )
		throws UnsupportedEncodingException {
		final StringBuilder debugSB = new StringBuilder( "[HTTP debug] Sending:\n" )
			.append( header );
		if( showContent && encodedContent != null && encodedContent.content != null ) {
			debugSB.append( encodedContent.content.toString( charset ) );
		}
		return debugSB.toString();
	}

	/*
	 * Prints debug information about a received message
	 */
	public static String getDebugMessage( HttpMessage message, String charset, boolean showContent )
		throws IOException {
		final StringBuilder debugSB =
			new StringBuilder( "\n[HTTP debug] Receiving:\n" ).append( getHttpHeader( message ) );
		if( showContent ) {
			debugSB.append( "--> Message content\n" )
				.append( getHttpBody( message, charset ) );
		}
		return debugSB.toString();
	}

	/*
	 * return the received message's header
	 */
	public static String getHttpHeader( HttpMessage message )
		throws IOException {
		final StringBuilder headerStr = new StringBuilder( "HTTP Code: " ).append( message.statusCode() )
			.append( "\n" ).append( "HTTP Method: " ).append( message.type().name() ).append( "\n" )
			.append( "Resource: " ).append( message.requestPath() ).append( "\n" )
			.append( "--> Header properties\n" );
		for( Entry< String, String > entry : message.properties() ) {
			headerStr.append( '\t' ).append( entry.getKey() ).append( ": " ).append( entry.getValue() ).append( '\n' );
		}
		for( HttpMessage.Cookie cookie : message.setCookies() ) {
			headerStr.append( "\tset-cookie: " ).append( cookie.toString() ).append( '\n' );
		}
		for( Entry< String, String > entry : message.cookies().entrySet() ) {
			headerStr.append( "\tcookie: " ).append( entry.getKey() ).append( '=' ).append( entry.getValue() )
				.append( '\n' );
		}
		return headerStr.toString();
	}

	/*
	 * Prints debug information about a received message
	 */
	public static String getHttpBody( HttpMessage message, String charset )
		throws IOException {
		return new String( message.content(), charset );
	}

	public static FaultException recv_mapHttpStatusCodeFault( HttpMessage message, Value httpStatusValue,
		Value decodedMessageValue ) {
		FaultException faultException = null;
		Iterator< Entry< String, ValueVector > > statusCodeIterator = httpStatusValue.children().entrySet().iterator();
		while( statusCodeIterator.hasNext() && faultException == null ) {
			Entry< String, ValueVector > entry = statusCodeIterator.next();
			int configuredStatusCode = entry.getValue().get( 0 ).intValue();
			if( configuredStatusCode == message.statusCode() ) {
				if( message.getPropertyOrEmptyString( Headers.CONTENT_TYPE )
					.equals( ContentTypes.APPLICATION_JSON ) ) {
					faultException = new FaultException( entry.getKey(),
						decodedMessageValue.getFirstChild( "error" ).getFirstChild( "data" ) );
				} else {
					faultException = new FaultException( entry.getKey() );
				}
			}
		}
		return faultException;
	}

	public static String cutBeforeQuerystring( String requestPath ) {
		return requestPath.split( "\\?", 2 )[ 0 ];
	}

	public static MultiPartFormDataParser parseMultiPartFormData( HttpMessage message, Value value )
		// , String charset )
		throws IOException {
		MultiPartFormDataParser multiPartFormDataParser = new MultiPartFormDataParser( message, value );
		multiPartFormDataParser.parse();
		return multiPartFormDataParser;
	}

	public static void parseForm( HttpMessage message, Value value, String charset )
		throws IOException {
		String line = new String( message.content(), charset );
		String[] pair;
		for( String item : line.split( "&" ) ) {
			pair = item.split( "=", 2 );
			if( pair.length > 1 ) {
				value.getChildren( URLDecoder.decode( pair[ 0 ], URL_DECODER_ENC ) ).first()
					.setValue( URLDecoder.decode( pair[ 1 ], URL_DECODER_ENC ) );
			}
		}
	}

	public static void parseNdJson( HttpMessage message, Value value, boolean strictEncoding, String charset )
		throws IOException {
		JsUtils.parseNdJsonIntoValue(
			new BufferedReader( new InputStreamReader( new ByteArrayInputStream( message.content() ), charset ) ),
			value, strictEncoding );
	}

	public static void parseJson( HttpMessage message, Value value, boolean strictEncoding, String charset )
		throws IOException {
		JsUtils.parseJsonIntoValue( new InputStreamReader( new ByteArrayInputStream( message.content() ), charset ),
			value, strictEncoding );
	}

	public static void parseXML( DocumentBuilder docBuilder, HttpMessage message, Value value, String charset )
		throws IOException {
		try {
			if( message.size() > 0 ) {
				InputSource src = new InputSource( new ByteArrayInputStream( message.content() ) );
				src.setEncoding( charset );
				Document doc = docBuilder.parse( src );
				XmlUtils.documentToValue( doc, value, false );
			}
		} catch( SAXException pce ) {
			throw new IOException( pce );
		}
	}

	public static void send_appendQuerystring( Value value, StringBuilder headerBuilder )
		throws IOException {
		if( value.hasChildren() ) {
			StringJoiner sj = new StringJoiner( "&" );

			headerBuilder.append( '?' );
			Iterator< Entry< String, ValueVector > > nodesIt = value.children().entrySet().iterator();
			while( nodesIt.hasNext() ) {
				Entry< String, ValueVector > entry = nodesIt.next();
				Iterator< Value > vecIt = entry.getValue().iterator();
				while( vecIt.hasNext() ) {
					Value v = vecIt.next();
					sj.add( URLEncoder.encode( entry.getKey(), URL_DECODER_ENC ) + "="
						+ URLEncoder.encode( v.strValue(), URL_DECODER_ENC ) );
				}
			}
			headerBuilder.append( sj.toString() );
		}
	}

	public static void send_appendJsonQueryString( Value value, Type sendType, StringBuilder headerBuilder )
		throws IOException {
		if( value.isDefined() || value.hasChildren() ) {
			headerBuilder.append( "?" );
			StringBuilder builder = new StringBuilder();
			JsUtils.valueToJsonString( value, true, sendType, builder );
			headerBuilder.append( URLEncoder.encode( builder.toString(), URL_DECODER_ENC ) );
		}
	}
}
