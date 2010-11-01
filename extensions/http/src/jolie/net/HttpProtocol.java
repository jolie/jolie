/***************************************************************************
 *   Copyright (C) 2008-2010 by Fabrizio Montesi <famontesi@gmail.com>     *
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

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.server.rpc.RPC;
import com.google.gwt.user.server.rpc.RPCRequest;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

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
import jolie.lang.NativeType;
import jolie.net.http.HttpMessage;
import jolie.net.http.HttpParser;
import jolie.net.http.HttpUtils;
import joliex.gwt.server.JolieGWTConverter;
import jolie.net.http.Method;
import jolie.net.http.MultiPartFormDataParser;
import jolie.net.protocols.CommProtocol;
import jolie.runtime.ByteArray;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.VariablePath;

import jolie.xml.XmlUtils;
import joliex.gwt.client.JolieService;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * HTTP protocol implementation
 * @author Fabrizio Montesi
 */
public class HttpProtocol extends CommProtocol
{
	private static final byte[] NOT_IMPLEMENTED_HEADER = "HTTP/1.1 501 Not Implemented".getBytes();
	//private static final byte[] INTERNAL_SERVER_ERROR_HEADER = "HTTP/1.1 500 Internal Server error".getBytes();


	private static class Parameters {
		private static String DEBUG = "debug";
		private static String COOKIES = "cookies";
		private static String METHOD = "method";
		private static String ALIAS = "alias";
		private static String MULTIPART_HEADERS = "multipartHeaders";
		private static String CONCURRENT = "concurrent";

		private static class MultiPartHeaders {
			private static String FILENAME = "filename";
		}
	}

	private static class Headers {
		private static String JOLIE_MESSAGE_ID = "X-Jolie-MessageID";
	}

	private String inputId = null;
	private final Transformer transformer;
	private final DocumentBuilderFactory docBuilderFactory;
	private final DocumentBuilder docBuilder;
	private final URI uri;
	private final boolean inInputPort;
	private MultiPartFormDataParser multiPartFormDataParser = null;
	
	public final static String CRLF = new String( new char[] { 13, 10 } );

	public String name()
	{
		return "http";
	}

	public boolean isThreadSafe()
	{
		return checkBooleanParameter( Parameters.CONCURRENT );
	}

	public HttpProtocol(
		VariablePath configurationPath,
		URI uri,
		boolean inInputPort,
		TransformerFactory transformerFactory,
		DocumentBuilderFactory docBuilderFactory,
		DocumentBuilder docBuilder
	)
		throws TransformerConfigurationException
	{
		super( configurationPath );
		this.uri = uri;
		this.inInputPort = inInputPort;
		this.transformer = transformerFactory.newTransformer();
		this.docBuilderFactory = docBuilderFactory;
		this.docBuilder = docBuilder;
		transformer.setOutputProperty( OutputKeys.OMIT_XML_DECLARATION, "yes" );
	}
		
	private void valueToDocument(
			Value value,
			Node node,
			Document doc
			)
	{
		node.appendChild( doc.createTextNode( value.strValue() ) );

		Element currentElement;
		for( Entry< String, ValueVector > entry : value.children().entrySet() ) {
			if ( !entry.getKey().startsWith( "@" ) ) {
				for( Value val : entry.getValue() ) {
					currentElement = doc.createElement( entry.getKey() );
					node.appendChild( currentElement );
					Map< String, ValueVector > attrs = jolie.xml.XmlUtils.getAttributesOrNull( val );
					if ( attrs != null ) {
						for( Entry< String, ValueVector > attrEntry : attrs.entrySet() ) {
							currentElement.setAttribute(
								attrEntry.getKey(),
								attrEntry.getValue().first().strValue()
								);
						}
					}
					valueToDocument( val, currentElement, doc );
				}
			}
		}
	}

	public String getMultipartHeaderForPart( String operationName, String partName )
	{
		if ( hasOperationSpecificParameter( operationName, Parameters.MULTIPART_HEADERS ) ) {
			Value v = getOperationSpecificParameterFirstValue( operationName, Parameters.MULTIPART_HEADERS );
			if ( v.hasChildren( partName ) ) {
				v = v.getFirstChild( partName );
				if ( v.hasChildren( Parameters.MultiPartHeaders.FILENAME ) ) {
					v = v.getFirstChild( Parameters.MultiPartHeaders.FILENAME );
					return v.strValue();
				}
			}
		}
		return null;
	}
	
	private final static String BOUNDARY = "----Jol13H77p$$Bound4r1$$";
	
	private void send_appendCookies( CommMessage message, String hostname, StringBuilder headerBuilder )
	{
		Value cookieParam = null;
		if ( hasOperationSpecificParameter( message.operationName(), Parameters.COOKIES ) ) {
			cookieParam = getOperationSpecificParameterFirstValue( message.operationName(), Parameters.COOKIES );
		} else if ( hasParameter( Parameters.COOKIES ) ) {
			cookieParam = getParameterFirstValue( Parameters.COOKIES );
		}
		if ( cookieParam != null ) {
			Value cookieConfig;
			String domain;
			StringBuilder cookieSB = new StringBuilder();
			for( Entry< String, ValueVector > entry : cookieParam.children().entrySet() ) {
				cookieConfig = entry.getValue().first();
				if ( message.value().hasChildren( cookieConfig.strValue() ) ) {
					domain = cookieConfig.hasChildren( "domain" ) ? cookieConfig.getFirstChild( "domain" ).strValue() : "";
					if ( domain.isEmpty() || hostname.endsWith( domain ) ) {
						cookieSB
							.append( entry.getKey() )
							.append( '=' )
							.append( message.value().getFirstChild( cookieConfig.strValue() ).strValue() )
							.append( ";" );
					}
				}
			}
			if ( cookieSB.length() > 0 ) {
				headerBuilder
					.append( "Cookie: " )
					.append( cookieSB )
					.append( CRLF );
			}
		}
	}
	
	private void send_appendSetCookieHeader( CommMessage message, StringBuilder headerBuilder )
	{
		Value cookieParam = null;
		if ( hasOperationSpecificParameter( message.operationName(), Parameters.COOKIES ) ) {
			cookieParam = getOperationSpecificParameterFirstValue( message.operationName(), Parameters.COOKIES );
		} else if ( hasParameter( Parameters.COOKIES ) ) {
			cookieParam = getParameterFirstValue( Parameters.COOKIES );
		}
		if ( cookieParam != null ) {
			Value cookieConfig;
			for( Entry< String, ValueVector > entry : cookieParam.children().entrySet() ) {
				cookieConfig = entry.getValue().first();
				if ( message.value().hasChildren( cookieConfig.strValue() ) ) {
					headerBuilder
						.append( "Set-Cookie: " )
						.append( entry.getKey() ).append( '=' )
						.append( message.value().getFirstChild( cookieConfig.strValue() ).strValue() )
						.append( "; expires=" )
						.append( cookieConfig.hasChildren( "expires" ) ? cookieConfig.getFirstChild( "expires" ).strValue() : "" )
						.append( "; domain=" )
						.append( cookieConfig.hasChildren( "domain" ) ? cookieConfig.getFirstChild( "domain" ).strValue() : "" )
						.append( "; path=" )
						.append( cookieConfig.hasChildren( "path" ) ? cookieConfig.getFirstChild( "path" ).strValue() : "" );
					if ( cookieConfig.hasChildren( "secure" ) && cookieConfig.getFirstChild( "secure" ).intValue() > 0 ) {
						headerBuilder.append( "; secure" );
					}
					headerBuilder.append( CRLF );
				}
			}
		}
	}
	
	private String requestFormat = null;

	private void send_appendQuerystring( Value value, String charset, StringBuilder headerBuilder )
		throws IOException
	{
		if ( value.children().isEmpty() == false ) {
			headerBuilder.append( '?' );
			for( Entry< String, ValueVector > entry : value.children().entrySet() ) {
				headerBuilder
					.append( entry.getKey() )
					.append( '=' )
					.append( URLEncoder.encode( entry.getValue().first().strValue(), charset ) )
					.append( '&' );
			}
		}
	}
	
	private void send_appendParsedAlias( String alias, Value value, String charset, StringBuilder headerBuilder )
		throws IOException
	{
		int offset = 0;
		String currStrValue;
		String currKey;
		StringBuilder result = new StringBuilder( alias );
		Matcher m = Pattern.compile( "%\\{[^\\}]*\\}" ).matcher( alias );

		while( m.find() ) {
			currKey = alias.substring( m.start() + 2, m.end() - 1 );
			currStrValue = URLEncoder.encode( value.getFirstChild( currKey ).strValue(), charset );
			result.replace(
				m.start() + offset, m.end() + offset,
				currStrValue
			);
			offset += currStrValue.length() - 3 - currKey.length();
		}
		headerBuilder.append( result );
	}
	
	private String send_getCharset()
	{
		String charset = "UTF-8";
		if ( hasParameter( "charset" ) ) {
			charset = getStringParameter( "charset" );
		}
		return charset;
	}
	
	private String send_getFormat()
	{
		String format = "xml";
		if ( inInputPort && requestFormat != null ) {
			format = requestFormat;
			requestFormat = null;
		} else if ( hasParameter( "format" ) ) {
			format = getStringParameter( "format" );
		}
		return format;
	}
	
	private static class EncodedContent {
		private ByteArray content = null;
		private String contentType = "";
	}
		
	private EncodedContent send_encodeContent( CommMessage message, Method method, String charset, String format )
		throws IOException
	{
		EncodedContent ret = new EncodedContent();
		if ( inInputPort == false && method == Method.GET ) {
			// We are building a GET request
			return ret;
		}

		if ( "xml".equals( format ) ) {
			Document doc = docBuilder.newDocument();
			Element root = doc.createElement( message.operationName() + (( inInputPort ) ? "Response" : "") );
			doc.appendChild( root );
			if ( message.isFault() ) {
				Element faultElement = doc.createElement( message.fault().faultName() );
				root.appendChild( faultElement );
				valueToDocument( message.fault().value(), faultElement, doc );
			} else {
				valueToDocument( message.value(), root, doc );
			}
			Source src = new DOMSource( doc );
			ByteArrayOutputStream tmpStream = new ByteArrayOutputStream();
			Result dest = new StreamResult( tmpStream );
			try {
				transformer.transform( src, dest );
			} catch( TransformerException e ) {
				throw new IOException( e );
			}
			ret.content = new ByteArray( tmpStream.toByteArray() );

			ret.contentType = "text/xml";
		} else if ( "binary".equals( format ) ) {
			if ( message.value().isByteArray() ) {
				ret.content = (ByteArray)message.value().valueObject();
				ret.contentType = "application/octet-stream";
			}
		} else if ( "html".equals( format ) ) {
			ret.content = new ByteArray( message.value().strValue().getBytes( charset ) );
			ret.contentType = "text/html";
		} else if ( "multipart/form-data".equals( format ) ) {
			ret.contentType = "multipart/form-data; boundary=" + BOUNDARY;
			StringBuilder builder = new StringBuilder();
			for( Entry< String, ValueVector > entry : message.value().children().entrySet() ) {
				if ( !entry.getKey().startsWith( "@" ) ) {
					builder.append( "--" + BOUNDARY + CRLF );
					builder.append( "Content-Disposition: form-data; name=\"" + entry.getKey() + '\"' + CRLF + CRLF );
					builder.append( entry.getValue().first().strValue() + CRLF );
				}
			}
			builder.append( "--" + BOUNDARY + "--" );
			ret.content = new ByteArray( builder.toString().getBytes( charset ) );
		} else if ( "x-www-form-urlencoded".equals( format ) ) {
			ret.contentType = "application/x-www-form-urlencoded";
			Iterator< Entry< String, ValueVector > > it =
				message.value().children().entrySet().iterator();
			Entry< String, ValueVector > entry;
			StringBuilder builder = new StringBuilder();
			while( it.hasNext() ) {
				entry = it.next();
				builder.append( entry.getKey() + "=" + URLEncoder.encode( entry.getValue().first().strValue(), "UTF-8" ) );
				if ( it.hasNext() ) {
					builder.append( '&' );
				}
			}
			ret.content = new ByteArray( builder.toString().getBytes( charset ) );
		} else if ( "text/x-gwt-rpc".equals( format ) ) {
			ret.contentType = "text/x-gwt-rpc";
			try {
				if ( message.isFault() ) {
					ret.content = new ByteArray(
						RPC.encodeResponseForFailure( JolieService.class.getMethods()[0], JolieGWTConverter.jolieToGwtFault( message.fault() ) ).getBytes( charset )
					);
				} else {
					joliex.gwt.client.Value v = new joliex.gwt.client.Value();
					JolieGWTConverter.jolieToGwtValue( message.value(), v );
					ret.content = new ByteArray(
						RPC.encodeResponseForSuccess( JolieService.class.getMethods()[0], v ).getBytes( charset )
					);
				}
			} catch( SerializationException e ) {
				throw new IOException( e );
			}
		}
		return ret;
	}
	
	private void send_appendResponseHeaders( CommMessage message, StringBuilder headerBuilder )
	{
		String redirect = getStringParameter( "redirect" );
		if ( redirect.isEmpty() ) {
			headerBuilder.append( "HTTP/1.1 200 OK" + CRLF );
		} else {
			headerBuilder.append( "HTTP/1.1 303 See Other" + CRLF );
			headerBuilder.append( "Location: " + redirect + CRLF );
		}
		send_appendSetCookieHeader( message, headerBuilder );
		headerBuilder.append( "Server: JOLIE" ).append( CRLF );
		StringBuilder cacheControlHeader = new StringBuilder();
		if ( hasParameter( "cacheControl" ) ) {
			Value cacheControl = getParameterFirstValue( "cacheControl" );
			if ( cacheControl.hasChildren( "maxAge" ) ) {
				cacheControlHeader.append( "max-age=" ).append( cacheControl.getFirstChild( "maxAge" ).intValue() );
			}
		}
		if ( cacheControlHeader.length() > 0 ) {
			headerBuilder.append( "Cache-Control: " ).append( cacheControlHeader ).append( CRLF );
		}
	}
	
	private void send_appendRequestMethod( Method method, StringBuilder headerBuilder )
	{
		headerBuilder.append( method.id() );
	}
	
	private void send_appendRequestPath( CommMessage message, Method method, StringBuilder headerBuilder, String charset )
		throws IOException
	{
		if ( uri.getPath().length() < 1 || uri.getPath().charAt( 0 ) != '/' ) {
			headerBuilder.append( '/' );
		}
		headerBuilder.append( uri.getPath() );

		String alias = getOperationSpecificStringParameter( message.operationName(), Parameters.ALIAS );
		if ( alias.isEmpty() ) {
			headerBuilder.append( message.operationName() );
		} else {
			send_appendParsedAlias( alias, message.value(), charset, headerBuilder );
		}

		if ( method == Method.GET ) {
			send_appendQuerystring( message.value(), charset, headerBuilder );
		}
	}
	
	private static void send_appendAuthorizationHeader( CommMessage message, StringBuilder headerBuilder )
	{
		if ( message.value().hasChildren( jolie.lang.Constants.Predefined.HTTP_BASIC_AUTHENTICATION.token().content() ) ) {
			Value v = message.value().getFirstChild( jolie.lang.Constants.Predefined.HTTP_BASIC_AUTHENTICATION.token().content() );
			//String realm = v.getFirstChild( "realm" ).strValue();
			String userpass =
				v.getFirstChild( "userid" ).strValue() + ":" +
				v.getFirstChild( "password" ).strValue();
			sun.misc.BASE64Encoder encoder = new sun.misc.BASE64Encoder();
			userpass = encoder.encode( userpass.getBytes() );
			headerBuilder.append( "Authorization: Basic " + userpass + CRLF );
		}
	}

	private Method send_getRequestMethod( CommMessage message )
		throws IOException
	{
		try {
			Method method;
			if ( hasOperationSpecificParameter( message.operationName(), Parameters.METHOD ) ) {
				method = Method.fromString( getOperationSpecificStringParameter( message.operationName(), Parameters.METHOD ).toUpperCase() );
			} else if ( hasParameter( Parameters.METHOD ) ) {
				method = Method.fromString( getStringParameter( Parameters.METHOD ).toUpperCase() );
			} else {
				method = Method.POST;
			}
			return method;
		} catch( Method.UnsupportedMethodException e ) {
			throw new IOException( e );
		}
	}
	
	private void send_appendRequestHeaders( CommMessage message, Method method, StringBuilder headerBuilder, String charset )
		throws IOException
	{
		send_appendRequestMethod( method, headerBuilder );
		headerBuilder.append( ' ' );
		send_appendRequestPath( message, method, headerBuilder, charset );
		headerBuilder.append( " HTTP/1.1" + CRLF );
		headerBuilder.append( "Host: " + uri.getHost() + CRLF );
		send_appendCookies( message, uri.getHost(), headerBuilder );
		send_appendAuthorizationHeader( message, headerBuilder );
	}
	
	private void send_appendGenericHeaders(
		CommMessage message,
		EncodedContent encodedContent,
		String charset,
		StringBuilder headerBuilder
	)
	{
		String param;
		if ( checkBooleanParameter( "keepAlive" ) == false || channel().toBeClosed() ) {
			channel().setToBeClosed( true );
			headerBuilder.append( "Connection: close" + CRLF );
		}
		if ( checkBooleanParameter( Parameters.CONCURRENT ) ) {
			headerBuilder.append( Headers.JOLIE_MESSAGE_ID ).append( ": " ).append( message.id() ).append( CRLF );
		}
		
		if ( encodedContent.content != null ) {
			String contentType = getStringParameter( "contentType" );
			if ( contentType.length() > 0 ) {
				encodedContent.contentType = contentType;
			}

			headerBuilder.append( "Content-Type: " + encodedContent.contentType );
			if ( charset != null ) {
				headerBuilder.append( "; charset=" + charset.toLowerCase() );
			}
			headerBuilder.append( CRLF );

			param = getStringParameter( "contentTransferEncoding" );
			if ( !param.isEmpty() ) {
				headerBuilder.append( "Content-Transfer-Encoding: " + param + CRLF );
			}
			headerBuilder.append( "Content-Length: " + (encodedContent.content.size() + 2) + CRLF );
		} else {
			headerBuilder.append( "Content-Length: 0" + CRLF );
		}
	}
	
	private void send_logDebugInfo( CharSequence header, EncodedContent encodedContent )
	{
		if ( checkBooleanParameter( "debug" ) ) {
			StringBuilder debugSB = new StringBuilder();
			debugSB.append( "[HTTP debug] Sending:\n" );
			debugSB.append( header );
			if (
				getParameterVector( "debug" ).first().getFirstChild( "showContent" ).intValue() > 0
				&& encodedContent.content != null
				) {
				debugSB.append( encodedContent.content.toString() );
			}
			Interpreter.getInstance().logInfo( debugSB.toString() );
		}
	}
	
	public void send( OutputStream ostream, CommMessage message, InputStream istream )
		throws IOException
	{
		Method method = send_getRequestMethod( message );
		String charset = send_getCharset();
		String format = send_getFormat();
		EncodedContent encodedContent = send_encodeContent( message, method, charset, format );
		StringBuilder headerBuilder = new StringBuilder();
		if ( inInputPort ) {
			// We're responding to a request
			send_appendResponseHeaders( message, headerBuilder );
		} else {
			// We're sending a notification or a solicit
			send_appendRequestHeaders( message, method, headerBuilder, charset );
		}
		send_appendGenericHeaders( message, encodedContent, charset, headerBuilder );
		headerBuilder.append( CRLF );
		
		send_logDebugInfo( headerBuilder, encodedContent );
		inputId = message.operationName();
		
		if ( charset == null ) {
			charset = "UTF8";
		}
		ostream.write( headerBuilder.toString().getBytes( charset ) );
		if ( encodedContent.content != null ) {
			ostream.write( encodedContent.content.getBytes() );
			ostream.write( CRLF.getBytes( charset ) );
		}
	}

	private void parseXML( HttpMessage message, Value value )
		throws IOException
	{
		try {
			if ( message.size() > 0 ) {
				DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
				InputSource src = new InputSource( new ByteArrayInputStream( message.content() ) );
				Document doc = builder.parse( src );
				XmlUtils.documentToValue( doc, value );
			}
		} catch( ParserConfigurationException pce ) {
			throw new IOException( pce );
		} catch( SAXException saxe ) {
			throw new IOException( saxe );
		}
	}
	
	private static void parseForm( HttpMessage message, Value value )
		throws IOException
	{
		String line = new String( message.content(), "UTF8" );
		String[] s, pair;
		s = line.split( "&" );
		for( int i = 0; i < s.length; i++ ) {
			pair = s[i].split( "=", 2 );
			value.getChildren( pair[0] ).first().setValue( pair[1] );
		}		
	}
	
	private void parseMultiPartFormData( HttpMessage message, Value value )
		throws IOException
	{
		multiPartFormDataParser = new MultiPartFormDataParser( message, value );
		multiPartFormDataParser.parse();
	}
	
	private static String parseGWTRPC( HttpMessage message, Value value )
		throws IOException
	{
		RPCRequest request = RPC.decodeRequest( new String( message.content(), "UTF8" ) );
		String operationName = (String)request.getParameters()[0];
		joliex.gwt.client.Value requestValue = (joliex.gwt.client.Value)request.getParameters()[1];
		JolieGWTConverter.gwtToJolieValue( requestValue, value );
		return operationName;
	}
	
	private void recv_checkForSetCookie( HttpMessage message, Value value )
		throws IOException
	{
		if ( hasParameter( Parameters.COOKIES ) ) {
			String type;
			Value cookies = getParameterFirstValue( Parameters.COOKIES );
			Value cookieConfig;
			Value v;
			for( HttpMessage.Cookie cookie : message.setCookies() ) {
				if ( cookies.hasChildren( cookie.name() ) ) {
					cookieConfig = cookies.getFirstChild( cookie.name() );
					if ( cookieConfig.isString() ) {
						v = value.getFirstChild( cookieConfig.strValue() );
						if ( cookieConfig.hasChildren( "type" ) ) {
							type = cookieConfig.getFirstChild( "type" ).strValue();
						} else {
							type = "string";
						}
						recv_assignCookieValue( cookie.value(), v, type );
					}
				}

				/*currValue = Value.create();
				currValue.getNewChild( "expires" ).setValue( cookie.expirationDate() );
				currValue.getNewChild( "path" ).setValue( cookie.path() );
				currValue.getNewChild( "name" ).setValue( cookie.name() );
				currValue.getNewChild( "value" ).setValue( cookie.value() );
				currValue.getNewChild( "domain" ).setValue( cookie.domain() );
				currValue.getNewChild( "secure" ).setValue( (cookie.secure() ? 1 : 0) );
				cookieVec.add( currValue );*/
			}
		}
	}

	private void recv_assignCookieValue( String cookieValue, Value value, String typeKeyword )
		throws IOException
	{
		NativeType type = NativeType.fromString( typeKeyword );
		if ( NativeType.INT == type ) {
			try {
				value.setValue( new Integer( cookieValue ) );
			} catch( NumberFormatException e ) {
				throw new IOException( e );
			}
		} else if ( NativeType.STRING == type ) {
			value.setValue( cookieValue );
		} else if ( NativeType.DOUBLE == type ) {
			try {
				value.setValue( new Double( cookieValue ) );
			} catch( NumberFormatException e ) {
				throw new IOException( e );
			}
		} else {
			value.setValue( cookieValue );
		}
	}

	private void recv_checkForCookies( HttpMessage message, DecodedMessage decodedMessage )
		throws IOException
	{
		Value cookies = null;
		if ( hasOperationSpecificParameter( decodedMessage.operationName, Parameters.COOKIES ) ) {
			cookies = getOperationSpecificParameterFirstValue( decodedMessage.operationName, Parameters.COOKIES );
		} else if ( hasParameter( Parameters.COOKIES ) ) {
			cookies = getParameterFirstValue( Parameters.COOKIES );
		}
		if ( cookies != null ) {
			Value v;
			String type;
			for( Entry< String, String > entry : message.cookies().entrySet() ) {
				if ( cookies.hasChildren( entry.getKey() ) ) {
					Value cookieConfig = cookies.getFirstChild( entry.getKey() );
					if ( cookieConfig.isString() ) {
						v = decodedMessage.value.getFirstChild( cookieConfig.strValue() );
						if ( cookieConfig.hasChildren( "type" ) ) {
							type = cookieConfig.getFirstChild( "type" ).strValue();
						} else {
							type = "string";
						}
						recv_assignCookieValue( entry.getValue(), v, type );
					}
				}
			}
		}
	}

	private static void recv_parseQueryString( HttpMessage message, Value value )
	{
		String queryString = message.requestPath() == null ? "" : message.requestPath();
		String[] kv = queryString.split( "\\?" );
		if ( kv.length > 1 ) {
			queryString = kv[1];
			String[] params = queryString.split( "&" );
			for( String param : params ) {
				kv = param.split( "=" );
				if ( kv.length > 1 ) {
					value.getFirstChild( kv[0] ).setValue( kv[1] );
				}
			}
		}
	}
	
	/*
	 * Prints debug information about a received message
	 */
	private void recv_logDebugInfo( HttpMessage message )
	{
		StringBuilder debugSB = new StringBuilder();
		debugSB.append( "[HTTP debug] Receiving:\n" );
		debugSB.append( "HTTP Code: " + message.httpCode() + "\n" );
		debugSB.append( "Resource: " + message.requestPath() + "\n" );
		debugSB.append( "--> Header properties\n" );
		for( Entry< String, String > entry : message.properties() ) {
			debugSB.append( '\t' + entry.getKey() + ": " + entry.getValue() + '\n' );
		}
		for( HttpMessage.Cookie cookie : message.setCookies() ) {
			debugSB.append( "\tset-cookie: " + cookie.toString() + '\n' );
		}
		for( Entry< String, String > entry : message.cookies().entrySet() ) {
			debugSB.append( "\tcookie: " + entry.getKey() + '=' + entry.getValue() + '\n' );
		}
		if (
			getParameterFirstValue( "debug" ).getFirstChild( "showContent" ).intValue() > 0
			&& message.content() != null
		) {
			debugSB.append( "--> Message content\n" );
			debugSB.append( new String( message.content() ) );
		}
		Interpreter.getInstance().logInfo( debugSB.toString() );
	}
	
	private void recv_parseMessage( HttpMessage message, DecodedMessage decodedMessage )
		throws IOException
	{
		requestFormat = null;
		String format = "xml";
		if ( hasParameter( "format" ) ) {
			format = getStringParameter( "format" );
		}
		String type = message.getProperty( "content-type" ).split( ";" )[0];
		if ( "text/html".equals( type ) ) {
			decodedMessage.value.setValue( new String( message.content() ) );
		} else if ( "application/x-www-form-urlencoded".equals( type ) ) {
			parseForm( message, decodedMessage.value );
		} else if ( "text/xml".equals( type ) ) {
			parseXML( message, decodedMessage.value );
		} else if ( "text/x-gwt-rpc".equals( type ) ) {
			decodedMessage.operationName = parseGWTRPC( message, decodedMessage.value );
			requestFormat = "text/x-gwt-rpc";
		} else if ( "multipart/form-data".equals( type ) ) {
			parseMultiPartFormData( message, decodedMessage.value );
		} else if ( "application/octet-stream".equals( type ) ) {
			decodedMessage.value.setValue( new ByteArray( message.content() ) );
		} else if ( "xml".equals( format ) || "rest".equals( format ) ) {
			parseXML( message, decodedMessage.value );
		} else {
			decodedMessage.value.setValue( new String( message.content() ) );
		}
	}

	private void recv_checkReceivingOperation( HttpMessage message, DecodedMessage decodedMessage )
	{
		if ( decodedMessage.operationName == null ) {
			decodedMessage.operationName = message.requestPath().split( "\\?" )[0];
		}

		if ( !channel().parentListener().canHandleInputOperation( decodedMessage.operationName ) ) {
			String defaultOpId = getStringParameter( "default" );
			if ( defaultOpId.length() > 0 ) {
				Value body = decodedMessage.value;
				decodedMessage.value = Value.create();
				decodedMessage.value.getChildren( "data" ).add( body );
				decodedMessage.value.getFirstChild( "operation" ).setValue( decodedMessage.operationName );
				Value cookies = decodedMessage.value.getFirstChild( "cookies" );
				for( Entry< String, String > cookie : message.cookies().entrySet() ) {
					cookies.getFirstChild( cookie.getKey() ).setValue( cookie.getValue() );
				}
				decodedMessage.operationName = defaultOpId;
			}
		}
	}

	private void recv_checkForMultiPartHeaders( DecodedMessage decodedMessage )
	{
		if ( multiPartFormDataParser != null ) {
			String target;
			for( Entry< String, MultiPartFormDataParser.PartProperties > entry : multiPartFormDataParser.getPartPropertiesSet() ) {
				if ( entry.getValue().filename() != null ) {
					target = getMultipartHeaderForPart( decodedMessage.operationName, entry.getKey() );
					if ( target != null ) {
						decodedMessage.value.getFirstChild( target ).setValue( entry.getValue().filename() );
					}
				}
			}
			multiPartFormDataParser = null;
		}
	}
	
	private void recv_checkForMessageProperties( HttpMessage message, DecodedMessage decodedMessage )
		throws IOException
	{
		recv_checkForCookies( message, decodedMessage );
		recv_checkForMultiPartHeaders( decodedMessage );// message, decodedMessage );
		String property;
		if (
			(property=message.getProperty( "user-agent" )) != null &&
			hasParameter( "userAgent" )
		) {
			getParameterFirstValue( "userAgent" ).setValue( property );
		}
	}

	private static class DecodedMessage {
		private String operationName = null;
		private Value value = Value.create();
		private long id = CommMessage.GENERIC_ID;
	}
	
	public CommMessage recv( InputStream istream, OutputStream ostream )
		throws IOException
	{
		CommMessage retVal = null;
		DecodedMessage decodedMessage = new DecodedMessage();
		HttpMessage message = new HttpParser( istream ).parse();

		if ( message.isSupported() == false ) {
			ostream.write( NOT_IMPLEMENTED_HEADER );
			ostream.write( CRLF.getBytes() );
			ostream.write( CRLF.getBytes() );
			ostream.flush();
			return null;
		}

		if ( hasParameter( "keepAlive" ) ) {
			channel().setToBeClosed( checkBooleanParameter( "keepAlive" ) == false );
		} else {
			HttpUtils.recv_checkForChannelClosing( message, channel() );
		}
		if ( checkBooleanParameter( Parameters.DEBUG ) ) {
			recv_logDebugInfo( message );
		}
		if ( message.size() > 0 ) {
			recv_parseMessage( message, decodedMessage );
		}

		if ( checkBooleanParameter( Parameters.CONCURRENT ) ) {
			String messageId = message.getProperty( Headers.JOLIE_MESSAGE_ID );
			if ( messageId != null ) {
				try {
					decodedMessage.id = Long.parseLong( messageId );
				} catch( NumberFormatException e ) {}
			}
		}
		
		if ( message.isResponse() ) {
			recv_checkForSetCookie( message, decodedMessage.value );
			retVal = new CommMessage( decodedMessage.id, inputId, "/", decodedMessage.value, null );
		} else if ( message.isError() == false ) {
			if ( message.isGet() ) {
				recv_parseQueryString( message, decodedMessage.value );
			}
			recv_checkReceivingOperation( message, decodedMessage );
			recv_checkForMessageProperties( message, decodedMessage );
			//TODO support resourcePath
			retVal = new CommMessage( decodedMessage.id, decodedMessage.operationName, "/", decodedMessage.value, null );
		}
		return retVal;
	}
}
