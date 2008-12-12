/***************************************************************************
 *   Copyright (C) 2008 by Fabrizio Montesi                                *
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import jolie.Constants;
import jolie.Interpreter;
import jolie.net.http.HttpMessage;
import jolie.net.http.HttpParser;
import jolie.net.http.HttpUtils;
import jolie.net.http.JolieGWTConverter;
import jolie.net.http.MultiPartFormDataParser;
import jolie.net.protocols.SequentialCommProtocol;
import jolie.runtime.AndJarDeps;
import jolie.runtime.ByteArray;
import jolie.runtime.InputOperation;
import jolie.runtime.InvalidIdException;
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
@AndJarDeps({"jolie-xml.jar"})
public class HttpProtocol extends SequentialCommProtocol
{
	private static class Parameters {
		private static String DEBUG = "debug";
	}

	private String inputId = null;
	final private TransformerFactory transformerFactory;
	final private Transformer transformer;
	final private DocumentBuilderFactory docBuilderFactory;
	final private DocumentBuilder docBuilder;
	final private URI uri;
	private boolean received = false;
	
	final public static String CRLF = new String( new char[] { 13, 10 } );

	public String name()
	{
		return "http";
	}

	public HttpProtocol( VariablePath configurationPath, URI uri )
		throws ParserConfigurationException, TransformerConfigurationException
	{
		super( configurationPath );
		this.uri = uri;
		docBuilderFactory = DocumentBuilderFactory.newInstance();
		docBuilderFactory.setNamespaceAware( true );
		docBuilder = docBuilderFactory.newDocumentBuilder();
		transformerFactory = TransformerFactory.newInstance();
		transformer = transformerFactory.newTransformer();
	}
		
	private static Map< String, ValueVector > getAttributesOrNull( Value value )
	{
		Map< String, ValueVector > ret = null;
		ValueVector vec = value.children().get( Constants.Predefined.ATTRIBUTES.token().content() );
		if ( vec != null && vec.size() > 0 )
			ret = vec.first().children();
		
		if ( ret == null )
			ret = new HashMap< String, ValueVector >();
		
		return ret;
	}
	
	private void valueToDocument(
			Value value,
			Node node,
			Document doc
			)
		throws SOAPException
	{
		node.appendChild( doc.createTextNode( value.strValue() ) );

		Element currentElement;
		for( Entry< String, ValueVector > entry : value.children().entrySet() ) {
			if ( !entry.getKey().startsWith( "@" ) ) {
				for( Value val : entry.getValue() ) {
					currentElement = doc.createElement( entry.getKey() );
					node.appendChild( currentElement );
					Map< String, ValueVector > attrs = getAttributesOrNull( val );
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
	
	private final static String BOUNDARY = "----Jol13H77p$$Bound4r1$$";
	
	private static void send_appendCookies( CommMessage message, String hostname, StringBuilder headerBuilder )
	{
		ValueVector cookieVec = message.value().getChildren( Constants.Predefined.COOKIES.token().content() );
		StringBuilder cookieSB = new StringBuilder();
		String domain;
		// TODO check for cookie expiration
		for( Value v : cookieVec ) {
			domain = v.getChildren( "domain" ).first().strValue();
			if ( domain.isEmpty() ||
					(!domain.isEmpty() && hostname.endsWith( domain )) ) {
				cookieSB.append(
							v.getChildren( "name" ).first().strValue() + "=" +
							v.getChildren( "value" ).first().strValue() + "; "
				);
			}
		}
		if ( cookieSB.length() > 0 ) {
			headerBuilder.append( "Cookie: " );
			headerBuilder.append( cookieSB );
			headerBuilder.append( CRLF );
		}
	}
	
	private static void send_appendSetCookieHeader( CommMessage message, StringBuilder headerBuilder )
	{
		ValueVector cookieVec = message.value().getChildren( Constants.Predefined.COOKIES.token().content() );
		// TODO check for cookie expiration
		for( Value v : cookieVec ) {
			headerBuilder.append( "Set-Cookie: " +
					v.getFirstChild( "name" ).strValue() + "=" +
					v.getFirstChild( "value" ).strValue() + "; " +
					"expires=" + v.getFirstChild( "expires" ).strValue() + "; " +
					"path=" + v.getFirstChild( "path" ).strValue() + "; " +
					"domain=" + v.getFirstChild( "domain" ).strValue() +
					( (v.getFirstChild( "secure" ).intValue() > 0) ? "; secure" : "" ) +
					CRLF
			);
		}
	}
	
	private String requestFormat = null;
	
	private static CharSequence parseAlias( String alias, Value value )
	{
		int offset = 0;
		String currStrValue;
		String currKey;
		StringBuilder result = new StringBuilder( alias );
		Matcher m = Pattern.compile( "%\\{[^\\}]*\\}" ).matcher( alias );

		while( m.find() ) {
			currKey = alias.substring( m.start()+2, m.end()-1 );
			currStrValue = value.getFirstChild( currKey ).strValue();
			result.replace(
				m.start() + offset, m.end() + offset,
				currStrValue
			);
			offset += currStrValue.length() - 3 - currKey.length();
		}
		return result;
	}
	
	private String send_getCharset( CommMessage message )
	{
		String charset = null;
		if ( message.value().hasChildren( jolie.Constants.Predefined.CHARSET.token().content() ) ) {
			charset = message.value().getFirstChild( jolie.Constants.Predefined.CHARSET.token().content() ).strValue();
		} else if ( hasParameter( "charset" ) ) {
			charset = getStringParameter( "charset" );
		}
		return charset;
	}
	
	private String send_getFormat( CommMessage message )
	{
		String format = "xml";
		if ( received && requestFormat != null ) {
			format = requestFormat;
			requestFormat = null;
		} else if ( message.value().hasChildren( jolie.Constants.Predefined.FORMAT.token().content() ) ) {
			format = message.value().getFirstChild( jolie.Constants.Predefined.FORMAT.token().content() ).strValue();
		} else if ( hasParameter( "format" ) ) {
			format = getStringParameter( "format" );
		}
		return format;
	}
	
	private class EncodedContent {
		private ByteArray content = null;
		private String contentType = null;
	}
		
	private EncodedContent send_encodeContent( CommMessage message, String charset, String format )
		throws IOException
	{
		if ( charset == null ) {
			charset = "UTF8";
		}
		EncodedContent ret = new EncodedContent();
		if ( "xml".equals( format ) ) {
			Document doc = docBuilder.newDocument();
			Element root = doc.createElement( message.operationName() + (( received ) ? "Response" : "") );
			doc.appendChild( root );
			try {
				valueToDocument( message.value(), root, doc );
			} catch( SOAPException e ) {
				throw new IOException( e );
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
			ret.contentType = "x-www-form-urlencoded";
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
		String redirect = message.value().getFirstChild( Constants.Predefined.REDIRECT.token().content() ).strValue();
		if ( redirect.isEmpty() ) {
			headerBuilder.append( "HTTP/1.1 200 OK" + CRLF );
		} else {
			headerBuilder.append( "HTTP/1.1 303 See Other" + CRLF );
			headerBuilder.append( "Location: " + redirect + CRLF );
		}
		send_appendSetCookieHeader( message, headerBuilder );
	}
	
	private void send_appendRequestMethod( StringBuilder headerBuilder )
	{
		if ( hasParameter( "method" ) ) {
			headerBuilder.append( getStringParameter( "method" ).toUpperCase() );
		} else {
			headerBuilder.append( "GET" );
		}
	}
	
	private void send_appendRequestPath( CommMessage message, StringBuilder headerBuilder )
	{
		if ( uri.getPath().length() < 1 || uri.getPath().charAt( 0 ) != '/' ) {
			headerBuilder.append( '/' );
		}
		headerBuilder.append( uri.getPath() );
		if ( uri.toString().endsWith( "/" ) == false ) {
			headerBuilder.append( '/' );
		}
		if (
			hasParameter( "aliases" ) &&
			getParameterFirstValue( "aliases" ).hasChildren( message.operationName() )
		) {
			headerBuilder.append(
				parseAlias(
					getParameterFirstValue( "aliases" ).getFirstChild( message.operationName() ).strValue(),
					message.value()
				)
			);
		} else {
			headerBuilder.append( message.operationName() );
		}
	}
	
	private static void send_appendAuthorizationHeader( CommMessage message, StringBuilder headerBuilder )
	{
		if ( message.value().hasChildren( jolie.Constants.Predefined.HTTP_BASIC_AUTHENTICATION.token().content() ) ) {
			Value v = message.value().getFirstChild( jolie.Constants.Predefined.HTTP_BASIC_AUTHENTICATION.token().content() );
			//String realm = v.getFirstChild( "realm" ).strValue();
			String userpass =
				v.getFirstChild( "userid" ).strValue() + ":" +
				v.getFirstChild( "password" ).strValue();
			sun.misc.BASE64Encoder encoder = new sun.misc.BASE64Encoder();
			userpass = encoder.encode( userpass.getBytes() );
			headerBuilder.append( "Authorization: Basic " + userpass + CRLF );
		}
	}
	
	private void send_appendRequestHeaders( CommMessage message, StringBuilder headerBuilder )
	{
		send_appendRequestMethod( headerBuilder );
		headerBuilder.append( ' ' );
		send_appendRequestPath( message, headerBuilder );
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
		if ( checkBooleanParameter( "keepAlive" ) ) {
			channel().setToBeClosed( true );
			headerBuilder.append( "Connection: close" + CRLF );
		}
		
		if ( encodedContent.content != null ) {
			param = message.value().getFirstChild( jolie.Constants.Predefined.CONTENT_TYPE.token().content() ).strValue();
			if ( !param.isEmpty() ) {
				encodedContent.contentType = param;
			}

			headerBuilder.append( "Content-Type: " + encodedContent.contentType );
			if ( charset != null ) {
				headerBuilder.append( "; charset=" + charset.toLowerCase() );
			}
			headerBuilder.append( CRLF );

			param = message.value().getFirstChild( jolie.Constants.Predefined.CONTENT_TRANSFER_ENCODING.token().content() ).strValue();
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
			Interpreter.getInstance().logger().info( debugSB.toString() );
		}
	}
	
	public void send( OutputStream ostream, CommMessage message, InputStream istream )
		throws IOException
	{
		String charset = send_getCharset( message );
		String format = send_getFormat( message );
		EncodedContent encodedContent = send_encodeContent( message, charset, format );
		StringBuilder headerBuilder = new StringBuilder();
		if ( received ) {
			// We're responding to a request
			send_appendResponseHeaders( message, headerBuilder );
			received = false;
		} else {
			// We're sending a notification or a solicit
			send_appendRequestHeaders( message, headerBuilder );
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
		ostream.flush();
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
	
	private static void parseMultiPartFormData( HttpMessage message, Value value )
		throws IOException
	{
		MultiPartFormDataParser parser = new MultiPartFormDataParser( message, value );
		parser.parse();
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
	
	private static void recv_checkForSetCookie( HttpMessage message, Value value )
	{
		ValueVector cookieVec = value.getChildren( Constants.Predefined.COOKIES.token().content() );
		Value currValue;
		for( HttpMessage.Cookie cookie : message.setCookies() ) {
			currValue = Value.create();
			currValue.getNewChild( "expires" ).setValue( cookie.expirationDate() );
			currValue.getNewChild( "path" ).setValue( cookie.path() );
			currValue.getNewChild( "name" ).setValue( cookie.name() );
			currValue.getNewChild( "value" ).setValue( cookie.value() );
			currValue.getNewChild( "domain" ).setValue( cookie.domain() );
			currValue.getNewChild( "secure" ).setValue( (cookie.secure() ? 1 : 0) );
			cookieVec.add( currValue );
		}
	}
	
	private static void recv_checkForCookies( HttpMessage message, Value value )
	{
		ValueVector cookieVec = value.getChildren( Constants.Predefined.COOKIES.token().content() );
		Value v;
		for( Entry< String, String > entry : message.cookies().entrySet() ) {
			v = Value.create();
			v.getNewChild( "name" ).setValue( entry.getKey() );
			v.getNewChild( "value" ).setValue( entry.getValue() );
			cookieVec.add( v );
		}
	}
	
	private static void recv_parseQueryString( HttpMessage message, Value value )
	{
		if ( message.requestPath() != null ) {
			try {
				Value qsValue = value.getFirstChild( Constants.Predefined.QUERY_STRING.token().content() );
				String qs = message.requestPath().split( "\\?" )[1];
				String[] params = qs.split( "&" );
				for( String param : params ) {
					String[] kv = param.split( "=" );
					qsValue.getNewChild( kv[0] ).setValue( kv[1] );
				}
			} catch( ArrayIndexOutOfBoundsException e ) {}
		}
	}
	
	// Print debug information about a received message
	private static void recv_logDebugInfo( HttpMessage message )
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
		debugSB.append( "--> Message content\n" );
		if ( message.content() != null )
			debugSB.append( new String( message.content() ) );
		Interpreter.getInstance().logger().info( debugSB.toString() );
	}
	
	private void recv_parseMessage( HttpMessage message, DecodedMessage decodedMessage )
		throws IOException
	{
		String format = "xml";
		if ( hasParameter( "format" ) ) {
			format = getStringParameter( "format" );
		}
		String type = message.getProperty( "content-type" ).split( ";" )[0];
		if ( "application/x-www-form-urlencoded".equals( type ) ) {
			parseForm( message, decodedMessage.value );
		} else if ( "text/xml".equals( type ) || "rest".equals( format ) ) {
			parseXML( message, decodedMessage.value );
		} else if ( "text/x-gwt-rpc".equals( type ) ) {
			decodedMessage.operationName = parseGWTRPC( message, decodedMessage.value );
			requestFormat = "text/x-gwt-rpc";
		} else if ( "multipart/form-data".equals( type ) ) {
			parseMultiPartFormData( message, decodedMessage.value );
		} else {
			decodedMessage.value.setValue( new String( message.content() ) );
		}
	}
	
	private void recv_checkReceivingOperation( HttpMessage message, DecodedMessage decodedMessage )
	{
		if ( decodedMessage.operationName == null ) {
			decodedMessage.operationName = message.requestPath().split( "\\?" )[0];
		}

		InputOperation op = null;
		try {
			op = Interpreter.getInstance().getInputOperation( decodedMessage.operationName );
		} catch( InvalidIdException e ) {}
		if ( op == null || !channel().parentListener().canHandleInputOperation( op ) ) {
			String defaultOpId = getParameterVector( "default" ).first().strValue();
			if ( defaultOpId.length() > 0 ) {
				Value body = decodedMessage.value;
				decodedMessage.value = Value.create();
				decodedMessage.value.getChildren( "data" ).add( body );
				decodedMessage.value.getChildren( "operation" ).first().setValue( decodedMessage.operationName );
				decodedMessage.operationName = defaultOpId;
			}
		}
	}
	
	private static void recv_checkForMessageProperties( HttpMessage message, Value messageValue )
	{
		recv_checkForCookies( message, messageValue );
		String property;
		if ( (property=message.getProperty( "user-agent" )) != null ) {
			messageValue.getNewChild( Constants.Predefined.USER_AGENT.token().content() ).setValue( property );
		}
	}

	private class DecodedMessage {
		private String operationName = null;
		private Value value = Value.create();
	}
	
	public CommMessage recv( InputStream istream, OutputStream ostream )
		throws IOException
	{
		CommMessage retVal = null;
		DecodedMessage decodedMessage = new DecodedMessage();
		HttpMessage message = new HttpParser( istream ).parse();
		HttpUtils.recv_checkForChannelClosing( message, channel() );
		if ( checkBooleanParameter( Parameters.DEBUG ) ) {
			recv_logDebugInfo( message );
		}
		if ( message.size() > 0 ) {
			recv_parseMessage( message, decodedMessage );
		}
		
		if ( message.isResponse() ) {
			recv_checkForSetCookie( message, decodedMessage.value );
			retVal = new CommMessage( inputId, "/", decodedMessage.value );
			received = false;
		} else if ( !message.isError() ) {
			recv_parseQueryString( message, decodedMessage.value );
			recv_checkReceivingOperation( message, decodedMessage );
			recv_checkForMessageProperties( message, decodedMessage.value );
			//TODO support resourcePath
			retVal = new CommMessage( decodedMessage.operationName, "/", decodedMessage.value  );
			received = true;
		}

		return retVal;
	}
}