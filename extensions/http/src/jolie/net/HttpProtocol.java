 /**************************************************************************
 *   Copyright (C) 2008-2015 by Fabrizio Montesi <famontesi@gmail.com>     *
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
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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
import jolie.js.JsUtils;
import jolie.lang.Constants;
import jolie.lang.NativeType;
import jolie.net.http.HttpMessage;
import jolie.net.http.HttpParser;
import jolie.net.http.HttpUtils;
import jolie.net.http.Method;
import jolie.net.http.MultiPartFormDataParser;
import jolie.net.ports.Interface;
import jolie.net.protocols.CommProtocol;
import jolie.runtime.ByteArray;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.VariablePath;
import jolie.runtime.typing.OneWayTypeDescription;
import jolie.runtime.typing.OperationTypeDescription;
import jolie.runtime.typing.RequestResponseTypeDescription;
import jolie.runtime.typing.Type;
import jolie.runtime.typing.TypeCastingException;
import jolie.util.LocationParser;
import jolie.xml.XmlUtils;
import joliex.gwt.client.JolieService;
import joliex.gwt.server.JolieGWTConverter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * HTTP protocol implementation
 * @author Fabrizio Montesi
 * 14 Nov 2012 - Saverio Giallorenzo - Fabrizio Montesi: support for status codes
 */
public class HttpProtocol extends CommProtocol implements HttpUtils.HttpProtocol
{
	private static final int DEFAULT_STATUS_CODE = 200;
	private static final int DEFAULT_REDIRECTION_STATUS_CODE = 303;
	private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream"; // default content type per RFC 2616#7.2.1
	private static final String DEFAULT_FORMAT = "xml";
	private static final Map< Integer, String > statusCodeDescriptions = new HashMap<>();
	private static final Set< Integer > locationRequiredStatusCodes = new HashSet<>();

	static {
		locationRequiredStatusCodes.add( 301 );
		locationRequiredStatusCodes.add( 302 );
		locationRequiredStatusCodes.add( 303 );
		locationRequiredStatusCodes.add( 307 );
		locationRequiredStatusCodes.add( 308 );
	}

	static {
		// Initialise the HTTP Status code map.
		statusCodeDescriptions.put( 100,"Continue" );
		statusCodeDescriptions.put( 101,"Switching Protocols" );
		statusCodeDescriptions.put( 102,"Processing" );
		statusCodeDescriptions.put( 200,"OK" );
		statusCodeDescriptions.put( 201,"Created" );
		statusCodeDescriptions.put( 202,"Accepted" );
		statusCodeDescriptions.put( 203,"Non-Authoritative Information" );
		statusCodeDescriptions.put( 204,"No Content" );
		statusCodeDescriptions.put( 205,"Reset Content" );
		statusCodeDescriptions.put( 206,"Partial Content" );
		statusCodeDescriptions.put( 207,"Multi-Status" );
		statusCodeDescriptions.put( 208,"Already Reported" );
		statusCodeDescriptions.put( 226,"IM Used" );
		statusCodeDescriptions.put( 300,"Multiple Choices" );
		statusCodeDescriptions.put( 301,"Moved Permanently" );
		statusCodeDescriptions.put( 302,"Found" );
		statusCodeDescriptions.put( 303,"See Other" );
		statusCodeDescriptions.put( 304,"Not Modified" );
		statusCodeDescriptions.put( 305,"Use Proxy" );
		statusCodeDescriptions.put( 306,"Reserved" );
		statusCodeDescriptions.put( 307,"Temporary Redirect" );
		statusCodeDescriptions.put( 308,"Permanent Redirect" );
		statusCodeDescriptions.put( 400,"Bad Request" );
		statusCodeDescriptions.put( 401,"Unauthorized" );
		statusCodeDescriptions.put( 402,"Payment Required" );
		statusCodeDescriptions.put( 403,"Forbidden" );
		statusCodeDescriptions.put( 404,"Not Found" );
		statusCodeDescriptions.put( 405,"Method Not Allowed" );
		statusCodeDescriptions.put( 406,"Not Acceptable" );
		statusCodeDescriptions.put( 407,"Proxy Authentication Required" );
		statusCodeDescriptions.put( 408,"Request Timeout" );
		statusCodeDescriptions.put( 409,"Conflict" );
		statusCodeDescriptions.put( 410,"Gone" );
		statusCodeDescriptions.put( 411,"Length Required" );
		statusCodeDescriptions.put( 412,"Precondition Failed" );
		statusCodeDescriptions.put( 413,"Request Entity Too Large" );
		statusCodeDescriptions.put( 414,"Request-URI Too Long" );
		statusCodeDescriptions.put( 415,"Unsupported Media Type" );
		statusCodeDescriptions.put( 416,"Requested Range Not Satisfiable" );
		statusCodeDescriptions.put( 417,"Expectation Failed" );
		statusCodeDescriptions.put( 422,"Unprocessable Entity" );
		statusCodeDescriptions.put( 423,"Locked" );
		statusCodeDescriptions.put( 424,"Failed Dependency" );
		statusCodeDescriptions.put( 426,"Upgrade Required" );
		statusCodeDescriptions.put( 427,"Unassigned" );
		statusCodeDescriptions.put( 428,"Precondition Required" );
		statusCodeDescriptions.put( 429,"Too Many Requests" );
		statusCodeDescriptions.put( 430,"Unassigned" );
		statusCodeDescriptions.put( 431,"Request Header Fields Too Large" );
		statusCodeDescriptions.put( 500,"Internal Server Error" );
		statusCodeDescriptions.put( 501,"Not Implemented" );
		statusCodeDescriptions.put( 502,"Bad Gateway" );
		statusCodeDescriptions.put( 503,"Service Unavailable" );
		statusCodeDescriptions.put( 504,"Gateway Timeout" );
		statusCodeDescriptions.put( 505,"HTTP Version Not Supported" );
		statusCodeDescriptions.put( 507,"Insufficient Storage" );
		statusCodeDescriptions.put( 508,"Loop Detected" );
		statusCodeDescriptions.put( 509,"Unassigned" );
		statusCodeDescriptions.put( 510,"Not Extended" );
		statusCodeDescriptions.put( 511,"Network Authentication Required" );
	}

	private static class Parameters {
		private static final String KEEP_ALIVE = "keepAlive";
		private static final String DEBUG = "debug";
		private static final String COOKIES = "cookies";
		private static final String METHOD = "method";
		private static final String ALIAS = "alias";
		private static final String MULTIPART_HEADERS = "multipartHeaders";
		private static final String CONCURRENT = "concurrent";
		private static final String USER_AGENT = "userAgent";
		private static final String HOST = "host";
		private static final String HEADERS = "headers";
		private static final String ADD_HEADERS = "addHeader";
		private static final String STATUS_CODE = "statusCode";
		private static final String REDIRECT = "redirect";
		private static final String DEFAULT_OPERATION = "default";
		private static final String COMPRESSION = "compression";
		private static final String COMPRESSION_TYPES = "compressionTypes";
		private static final String REQUEST_COMPRESSION = "requestCompression";
		private static final String FORMAT = "format";
		private static final String RESPONSE_HEADER = "responseHeaders";
		private static final String JSON_ENCODING = "json_encoding";
		private static final String REQUEST_USER = "request";
		private static final String RESPONSE_USER = "response";
		private static final String HEADER_USER = "headers";
		private static final String CHARSET = "charset";
		private static final String CONTENT_TYPE = "contentType";
		private static final String CONTENT_TRANSFER_ENCODING = "contentTransferEncoding";
		private static final String CONTENT_DISPOSITION = "contentDisposition";
		private static final String DROP_URI_PATH = "dropURIPath";
		private static final String CACHE_CONTROL = "cacheControl";
		private static final String FORCE_CONTENT_DECODING = "forceContentDecoding";

		private static class MultiPartHeaders {
			private static final String FILENAME = "filename";
		}
	}

	private static class Headers {
		private static final String JOLIE_MESSAGE_ID = "X-Jolie-MessageID";
	}

	private static class ContentTypes {
		private static final String APPLICATION_JSON = "application/json";
	}

	private String inputId = null;
	private final Transformer transformer;
	private final DocumentBuilderFactory docBuilderFactory;
	private final DocumentBuilder docBuilder;
	private final URI uri;
	private final boolean inInputPort;
	private MultiPartFormDataParser multiPartFormDataParser = null;

	@Override
	public String name()
	{
		return "http";
	}

	@Override
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
		transformer.setOutputProperty( OutputKeys.INDENT, "no" );
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

	private final static String BOUNDARY = "----jol13h77p77bound4r155";

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
					.append( HttpUtils.CRLF );
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
					headerBuilder.append( HttpUtils.CRLF );
				}
			}
		}
	}

	private String encoding = null;
	private String responseFormat = null;
	private boolean headRequest = false;

	private static void send_appendQuerystring( Value value, StringBuilder headerBuilder )
		throws IOException
	{
		if ( !value.children().isEmpty() ) {
			headerBuilder.append( '?' );
			for( Entry< String, ValueVector > entry : value.children().entrySet() ) {
				for( Value v : entry.getValue() ) {
					headerBuilder
						.append( URLEncoder.encode( entry.getKey(), HttpUtils.URL_DECODER_ENC ) )
						.append( '=' )
						.append( URLEncoder.encode( v.strValue(), HttpUtils.URL_DECODER_ENC ) )
						.append( '&' );
				}
			}
		}
	}

	private void send_appendJsonQueryString( CommMessage message, StringBuilder headerBuilder )
		throws IOException
	{
		if ( message.value().isDefined() || message.value().hasChildren() ) {
			headerBuilder.append( "?" );
			StringBuilder builder = new StringBuilder();
			JsUtils.valueToJsonString( message.value(), true, getSendType( message ), builder );
			headerBuilder.append( URLEncoder.encode( builder.toString(), HttpUtils.URL_DECODER_ENC ) );
		}
	}

	private static void send_appendParsedAlias( String alias, Value value, StringBuilder headerBuilder )
		throws IOException
	{
		int offset = 0;
		List< String > aliasKeys = new ArrayList<>();
		String currStrValue;
		String currKey;
		StringBuilder result = new StringBuilder( alias );
		Matcher m = Pattern.compile( "%(!)?\\{[^\\}]*\\}" ).matcher( alias );

		while( m.find() ) {
			int displacement = 2;
			if ( m.group( 1 ) == null ) { // ! is missing after %: We have to use URLEncoder
				currKey = alias.substring( m.start() + displacement, m.end() - 1 );
				if ( "$".equals( currKey ) ) {
					currStrValue = URLEncoder.encode( value.strValue(), HttpUtils.URL_DECODER_ENC );
				} else {
					currStrValue = URLEncoder.encode( value.getFirstChild( currKey ).strValue(), HttpUtils.URL_DECODER_ENC );
					aliasKeys.add( currKey );
				}
			} else { // ! is given after %: We have to insert the string raw
				displacement = 3;
				currKey = alias.substring( m.start() + displacement, m.end() - 1 );
				if ( "$".equals( currKey ) ) {
					currStrValue = value.strValue();
				} else {
					currStrValue = value.getFirstChild( currKey ).strValue();
					aliasKeys.add( currKey );
				}
			}

			result.replace(
				m.start() + offset, m.end() + offset,
				currStrValue
			);
			displacement++; //considering also }
			offset += currStrValue.length() - displacement - currKey.length();
		}
		// removing used keys
		for( String aliasKey : aliasKeys ) {
			value.children().remove( aliasKey );
		}
		headerBuilder.append( result );
	}

	private String send_getFormat()
	{
		String format = DEFAULT_FORMAT;
		if ( inInputPort && responseFormat != null ) {
			format = responseFormat;
			responseFormat = null;
		} else if ( hasParameter( Parameters.FORMAT ) ) {
			format = getStringParameter( Parameters.FORMAT );
		}
		return format;
	}

	private static class EncodedContent {
		private ByteArray content = null;
		private String contentType = DEFAULT_CONTENT_TYPE;
		private String contentDisposition = "";
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
			ret.contentType = "text/xml";
			Document doc = docBuilder.newDocument();
			Element root = doc.createElement( message.operationName() + (( inInputPort ) ? "Response" : "") );
			doc.appendChild( root );
			if ( message.isFault() ) {
				Element faultElement = doc.createElement( message.fault().faultName() );
				root.appendChild( faultElement );
				XmlUtils.valueToDocument( message.fault().value(), faultElement, doc );
			} else {
				XmlUtils.valueToDocument( message.value(), root, doc );
			}
			Source src = new DOMSource( doc );
			ByteArrayOutputStream tmpStream = new ByteArrayOutputStream();
			Result dest = new StreamResult( tmpStream );
			transformer.setOutputProperty( OutputKeys.ENCODING, charset );
			try {
				transformer.transform( src, dest );
			} catch( TransformerException e ) {
				throw new IOException( e );
			}
			ret.content = new ByteArray( tmpStream.toByteArray() );
		} else if ( "binary".equals( format ) ) {
			ret.contentType = "application/octet-stream";
			ret.content = message.value().byteArrayValue();
		} else if ( "html".equals( format ) ) {
			ret.contentType = "text/html";
			if ( message.isFault() ) {
				StringBuilder builder = new StringBuilder();
				builder.append( "<html><head><title>" );
				builder.append( message.fault().faultName() );
				builder.append( "</title></head><body>" );
				builder.append( message.fault().value().strValue() );
				builder.append( "</body></html>" );
				ret.content = new ByteArray( builder.toString().getBytes( charset ) );
			} else {
				ret.content = new ByteArray( message.value().strValue().getBytes( charset ) );
			}
		} else if ( "multipart/form-data".equals( format ) ) {
			ret.contentType = "multipart/form-data; boundary=" + BOUNDARY;
			ByteArrayOutputStream bStream = new ByteArrayOutputStream();
			StringBuilder builder = new StringBuilder();
			for( Entry< String, ValueVector > entry : message.value().children().entrySet() ) {
				if ( !entry.getKey().startsWith( "@" ) ) {
					builder.append( "--" ).append( BOUNDARY ).append( HttpUtils.CRLF );
					builder.append( "Content-Disposition: form-data; name=\"" ).append( entry.getKey() ).append( '\"' );
					boolean isBinary = false;
					if ( hasOperationSpecificParameter( message.operationName(), Parameters.MULTIPART_HEADERS ) ) {
						Value specOpParam = getOperationSpecificParameterFirstValue( message.operationName(), Parameters.MULTIPART_HEADERS );
						if ( specOpParam.hasChildren( "partName" ) ) {
							ValueVector partNames = specOpParam.getChildren( "partName" );
							for( int p = 0; p < partNames.size(); p++ ) {
								if ( partNames.get( p ).hasChildren( "part" ) ) {
									if ( partNames.get( p ).getFirstChild( "part" ).strValue().equals( entry.getKey() ) ) {
										isBinary = true;
										if ( partNames.get( p ).hasChildren( "filename" ) ) {
											builder.append( "; filename=\"" ).append( partNames.get( p ).getFirstChild( "filename" ).strValue() ).append( "\"" );
										}
										if ( partNames.get( p ).hasChildren( "contentType" ) ) {
											builder.append( HttpUtils.CRLF ).append( "Content-Type:" ).append( partNames.get( p ).getFirstChild( "contentType" ).strValue() );
										}
									}
								}
							}
						}
					}

					builder.append( HttpUtils.CRLF ).append( HttpUtils.CRLF );
					if ( isBinary ) {
						bStream.write( builder.toString().getBytes( charset ) );
						bStream.write( entry.getValue().first().byteArrayValue().getBytes() );
						builder.delete( 0, builder.length() - 1 );
						builder.append( HttpUtils.CRLF );
					} else {
						builder.append( entry.getValue().first().strValue() ).append( HttpUtils.CRLF );
					}
				}
			}
			builder.append( "--" + BOUNDARY + "--" );
			bStream.write( builder.toString().getBytes( charset ));
			ret.content = new ByteArray( bStream.toByteArray() );
		} else if ( "x-www-form-urlencoded".equals( format ) ) {
			ret.contentType = "application/x-www-form-urlencoded";
			Iterator< Entry< String, ValueVector > > it =
				message.value().children().entrySet().iterator();
			StringBuilder builder = new StringBuilder();
			if ( message.isFault() ) {
				builder.append( "faultName=" );
				builder.append( URLEncoder.encode( message.fault().faultName(), HttpUtils.URL_DECODER_ENC ) );
				builder.append( "&data=" );
				builder.append( URLEncoder.encode( message.fault().value().strValue(), HttpUtils.URL_DECODER_ENC ) );
			} else {
				Entry< String, ValueVector > entry;
				while( it.hasNext() ) {
					entry = it.next();
					builder.append( URLEncoder.encode( entry.getKey(), HttpUtils.URL_DECODER_ENC ) )
						.append( "=" )
						.append( URLEncoder.encode( entry.getValue().first().strValue(), HttpUtils.URL_DECODER_ENC ) );
					if ( it.hasNext() ) {
						builder.append( '&' );
					}
				}
			}
			ret.content = new ByteArray( builder.toString().getBytes( charset ) );
		} else if ( "text/x-gwt-rpc".equals( format ) ) {
			ret.contentType = "text/x-gwt-rpc";
			try {
				if ( inInputPort ) { // It's a response
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
				} else { // It's a request
					throw new IOException( "Sending requests to a GWT server is currently unsupported." );
				}
			} catch( SerializationException e ) {
				throw new IOException( e );
			}
		} else if ( "json".equals( format ) ) {
			ret.contentType = ContentTypes.APPLICATION_JSON;
			StringBuilder jsonStringBuilder = new StringBuilder();
			if ( message.isFault() ) {
				Value error = message.value().getFirstChild( "error" );
				error.getFirstChild( "code" ).setValue( -32000 );
				error.getFirstChild( "message" ).setValue( message.fault().faultName() );
				error.getChildren( "data" ).set( 0, message.fault().value() );
				JsUtils.faultValueToJsonString( message.value(), getSendType( message ), jsonStringBuilder );
			} else {
				JsUtils.valueToJsonString( message.value(), true, getSendType( message ), jsonStringBuilder );
			}
			ret.content = new ByteArray( jsonStringBuilder.toString().getBytes( charset ) );
		} else if ( "raw".equals( format ) ) {
			ret.contentType = "text/plain";
			if ( message.isFault() ) {
				ret.content = new ByteArray( message.fault().value().strValue().getBytes( charset ) );
			} else {
				ret.content = new ByteArray( message.value().strValue().getBytes( charset ) );
			}
		}
		return ret;
	}

	private static boolean isLocationNeeded( int statusCode )
	{
		return locationRequiredStatusCodes.contains( statusCode );
	}

	private void send_appendResponseUserHeader( CommMessage message, StringBuilder headerBuilder )
	{
		Value responseHeaderParameters = null;
		if ( hasOperationSpecificParameter(message.operationName(), Parameters.RESPONSE_USER) ) {
			responseHeaderParameters = getOperationSpecificParameterFirstValue(message.operationName(), Parameters.RESPONSE_USER);
			if ( ( responseHeaderParameters != null ) && ( responseHeaderParameters.hasChildren(Parameters.HEADER_USER) ) ) {
				for ( Entry< String, ValueVector > entry : responseHeaderParameters.getFirstChild(Parameters.HEADER_USER).children().entrySet() )
					headerBuilder.append( entry.getKey() ).append(": ").append( entry.getValue().first().strValue() ).append( HttpUtils.CRLF );
			}
		}

		responseHeaderParameters = null;
		if ( hasParameter(Parameters.RESPONSE_USER) ) {
			responseHeaderParameters  = getParameterFirstValue(Parameters.RESPONSE_USER);

			if ( ( responseHeaderParameters != null ) && ( responseHeaderParameters.hasChildren(Parameters.HEADER_USER) ) ) {
				for ( Entry< String, ValueVector > entry : responseHeaderParameters.getFirstChild(Parameters.HEADER_USER).children().entrySet() )
					headerBuilder.append( entry.getKey() ).append(": ").append( entry.getValue().first().strValue() ).append( HttpUtils.CRLF );
			}
		}
	}

	private void send_appendResponseHeaders( CommMessage message, StringBuilder headerBuilder )
	{
		int statusCode = DEFAULT_STATUS_CODE;
		String statusDescription = null;

		if( hasParameter( Parameters.STATUS_CODE ) ) {
			statusCode = getIntParameter( Parameters.STATUS_CODE );
			if ( !statusCodeDescriptions.containsKey( statusCode ) ) {
				Interpreter.getInstance().logWarning( "HTTP protocol for operation " +
					message.operationName() +
					" is sending a message with status code " +
					statusCode +
					", which is not in the HTTP specifications."
				);
				statusDescription = "Internal Server Error";
			} else if ( isLocationNeeded( statusCode ) && !hasParameter( Parameters.REDIRECT ) ) {
				// if statusCode is a redirection code, location parameter is needed
				Interpreter.getInstance().logWarning( "HTTP protocol for operation " +
					message.operationName() +
					" is sending a message with status code " +
					statusCode +
					", which expects a redirect parameter but the latter is not set."
				);
			}
		} else if ( hasParameter( Parameters.REDIRECT ) ) {
			statusCode = DEFAULT_REDIRECTION_STATUS_CODE;
		}

		if ( statusDescription == null ) {
			statusDescription = statusCodeDescriptions.get( statusCode );
		}
		headerBuilder.append( "HTTP/1.1 " + statusCode + " " + statusDescription + HttpUtils.CRLF );

		// if redirect has been set, the redirect location parameter is set
		if ( hasParameter( Parameters.REDIRECT ) ) {
			headerBuilder.append( "Location: " + getStringParameter( Parameters.REDIRECT ) + HttpUtils.CRLF );
		}

		send_appendSetCookieHeader( message, headerBuilder );
		headerBuilder.append( "Server: Jolie" ).append( HttpUtils.CRLF );
		StringBuilder cacheControlHeader = new StringBuilder();
		if ( hasParameter( Parameters.CACHE_CONTROL ) ) {
			Value cacheControl = getParameterFirstValue( Parameters.CACHE_CONTROL );
			if ( cacheControl.hasChildren( "maxAge" ) ) {
				cacheControlHeader.append( "max-age=" ).append( cacheControl.getFirstChild( "maxAge" ).intValue() );
			}
		}
		if ( cacheControlHeader.length() > 0 ) {
			headerBuilder.append( "Cache-Control: " ).append( cacheControlHeader ).append( HttpUtils.CRLF );
		}
	}

	private static void send_appendRequestMethod( Method method, StringBuilder headerBuilder )
	{
		headerBuilder.append( method.id() );
	}

	private void send_appendRequestPath( CommMessage message, Method method, String qsFormat, StringBuilder headerBuilder )
		throws IOException
	{
		String path = uri.getRawPath();
		if ( uri.getScheme().equals( "localsocket") || path == null || path.isEmpty() || checkBooleanParameter( Parameters.DROP_URI_PATH, false ) ) {
			headerBuilder.append( '/' );
		} else {
			if ( path.charAt( 0 ) != '/' ) {
				headerBuilder.append( '/' );
			}
			headerBuilder.append( path );
		}

		if ( hasOperationSpecificParameter( message.operationName(), Parameters.ALIAS ) ) {
			String alias = getOperationSpecificStringParameter( message.operationName(), Parameters.ALIAS );
			send_appendParsedAlias( alias, message.value(), headerBuilder );
		} else {
			headerBuilder.append( message.operationName() );
		}

		if ( method == Method.GET ) {
			if ( qsFormat.equals( "json" ) ) {
				send_appendJsonQueryString( message, headerBuilder );
			} else {
				send_appendQuerystring( message.value(), headerBuilder );
			}
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
			Base64.Encoder encoder = Base64.getEncoder();
			userpass = encoder.encodeToString( userpass.getBytes() );
			headerBuilder.append( "Authorization: Basic " ).append( userpass ).append( HttpUtils.CRLF );
		}
	}

	private void send_appendRequestUserHeader( CommMessage message, StringBuilder headerBuilder )
	{
		Value responseHeaderParameters = null;
		if ( hasOperationSpecificParameter(message.operationName(), Parameters.REQUEST_USER ) ) {
			responseHeaderParameters = getOperationSpecificParameterFirstValue(message.operationName(), Parameters.RESPONSE_USER);
			if ( ( responseHeaderParameters != null ) && ( responseHeaderParameters.hasChildren(Parameters.HEADER_USER) ) ) {
				for ( Entry< String, ValueVector > entry : responseHeaderParameters.getFirstChild(Parameters.HEADER_USER).children().entrySet() )
					headerBuilder.append( entry.getKey() ).append(": ").append( entry.getValue().first().strValue() ).append( HttpUtils.CRLF );
			}
		}

		responseHeaderParameters = null;
		if ( hasParameter(Parameters.RESPONSE_USER) ){
			responseHeaderParameters  = getParameterFirstValue(Parameters.REQUEST_USER);
			if ( ( responseHeaderParameters != null ) && ( responseHeaderParameters.hasChildren(Parameters.HEADER_USER) ) ) {
				for ( Entry< String, ValueVector > entry : responseHeaderParameters.getFirstChild(Parameters.HEADER_USER).children().entrySet() )
					headerBuilder.append( entry.getKey() ).append(": ").append( entry.getValue().first().strValue() ).append( HttpUtils.CRLF );
			}
		}
	}

	private void send_appendHeader( StringBuilder headerBuilder )
	{
		Value v = getParameterFirstValue( Parameters.ADD_HEADERS );
		if ( v != null ) {
			if ( v.hasChildren( "header" ) ) {
				for( Value head : v.getChildren( "header" ) ) {
					String header
						= head.strValue() + ": "
						+ head.getFirstChild( "value" ).strValue();
					headerBuilder.append( header ).append( HttpUtils.CRLF );
				}
			}
		}
	}

	private Method send_getRequestMethod( CommMessage message )
		throws IOException
	{
		Method method =
			hasOperationSpecificParameter( message.operationName(), Parameters.METHOD ) ?
				Method.fromString( getOperationSpecificStringParameter( message.operationName(), Parameters.METHOD ) )
			: hasParameterValue( Parameters.METHOD ) ?
				Method.fromString( getStringParameter( Parameters.METHOD ) )
			:
				Method.POST;
		return method;
	}

	private void send_appendRequestHeaders( CommMessage message, Method method, String qsFormat, StringBuilder headerBuilder )
		throws IOException
	{
		send_appendRequestMethod( method, headerBuilder );
		headerBuilder.append( ' ' );
		send_appendRequestPath( message, method, qsFormat, headerBuilder );
		headerBuilder.append( " HTTP/1.1" + HttpUtils.CRLF );
		String host = uri.getHost();
		if ( uri.getScheme().equals( "localsocket" ) ) {
			/* in this case we need to replace the localsocket path with a host, that is the default one localhost */
			host = "localhost";
		}
		headerBuilder.append( "Host: " + host + HttpUtils.CRLF );
		send_appendCookies( message, uri.getHost(), headerBuilder );
		send_appendAuthorizationHeader( message, headerBuilder );
		if ( checkBooleanParameter( Parameters.COMPRESSION, true ) ) {
			String requestCompression = getStringParameter( Parameters.REQUEST_COMPRESSION );
			if ( requestCompression.equals( "gzip" ) || requestCompression.equals( "deflate" ) ) {
				encoding = requestCompression;
				headerBuilder.append( "Accept-Encoding: " + encoding + HttpUtils.CRLF );
			} else {
				headerBuilder.append( "Accept-Encoding: gzip, deflate" + HttpUtils.CRLF );
			}
		}
		send_appendHeader( headerBuilder );
	}

	private void send_appendGenericHeaders(
		CommMessage message,
		EncodedContent encodedContent,
		String charset,
		StringBuilder headerBuilder
	)
		throws IOException
	{
		if ( checkBooleanParameter( Parameters.KEEP_ALIVE, true ) == false || channel().toBeClosed() ) {
			channel().setToBeClosed( true );
			headerBuilder.append( "Connection: close" + HttpUtils.CRLF );
		}
		if ( checkBooleanParameter( Parameters.CONCURRENT, true ) ) {
			headerBuilder.append( Headers.JOLIE_MESSAGE_ID ).append( ": " ).append( message.id() ).append( HttpUtils.CRLF );
		}

		String contentType = getStringParameter( Parameters.CONTENT_TYPE );
		if ( contentType.length() > 0 ) {
			encodedContent.contentType = contentType;
		}
		encodedContent.contentType = encodedContent.contentType.toLowerCase();

		headerBuilder.append( "Content-Type: " + encodedContent.contentType );
		if ( charset != null ) {
			headerBuilder.append( "; charset=" + charset.toLowerCase() );
		}
		headerBuilder.append( HttpUtils.CRLF );

		if ( encodedContent.content != null ) {
			String transferEncoding = getStringParameter( Parameters.CONTENT_TRANSFER_ENCODING );
			if ( transferEncoding.length() > 0 ) {
				headerBuilder.append( "Content-Transfer-Encoding: " + transferEncoding + HttpUtils.CRLF );
			}

			String contentDisposition = getStringParameter( Parameters.CONTENT_DISPOSITION );
			if ( contentDisposition.length() > 0 ) {
				encodedContent.contentDisposition = contentDisposition;
				headerBuilder.append( "Content-Disposition: " + encodedContent.contentDisposition + HttpUtils.CRLF );
			}

			boolean compression = encoding != null && checkBooleanParameter( Parameters.COMPRESSION, true );
			String compressionTypes = getStringParameter(
				Parameters.COMPRESSION_TYPES,
				"text/html text/css text/plain text/xml text/x-js text/x-gwt-rpc application/json application/javascript application/x-www-form-urlencoded application/xhtml+xml application/xml"
			).toLowerCase();
			if ( compression && !compressionTypes.equals( "*" ) && !compressionTypes.contains( encodedContent.contentType ) ) {
				compression = false;
			}
			if ( compression ) {
				encodedContent.content = HttpUtils.encode( encoding, encodedContent.content, headerBuilder );
			}

			headerBuilder.append( "Content-Length: " + encodedContent.content.size() + HttpUtils.CRLF );
		} else {
			headerBuilder.append( "Content-Length: 0" + HttpUtils.CRLF );
		}
	}

	private void send_logDebugInfo( CharSequence header, EncodedContent encodedContent, String charset )
		throws IOException
	{
		if ( checkBooleanParameter( Parameters.DEBUG ) ) {
			StringBuilder debugSB = new StringBuilder();
			debugSB.append( "[HTTP debug] Sending:\n" );
			debugSB.append( header );
			if (
				getParameterVector( Parameters.DEBUG ).first().getFirstChild( "showContent" ).intValue() > 0
				&& encodedContent.content != null
				) {
				debugSB.append( encodedContent.content.toString( charset ) );
			}
			Interpreter.getInstance().logInfo( debugSB.toString() );
		}
	}

	public void send_internal( OutputStream ostream, CommMessage message, InputStream istream )
		throws IOException
	{
		Method method = send_getRequestMethod( message );
		String charset = HttpUtils.getCharset( getStringParameter( Parameters.CHARSET, "utf-8" ), null );
		String format = send_getFormat();
		String contentType = null;
		StringBuilder headerBuilder = new StringBuilder();

		if ( inInputPort ) {
			// We're responding to a request
			send_appendResponseHeaders( message, headerBuilder );
			send_appendResponseUserHeader( message, headerBuilder );
		} else {
			// We're sending a notification or a solicit
			String qsFormat = "";
			if ( method == Method.GET && getParameterFirstValue( Parameters.METHOD ).hasChildren( "queryFormat" ) ) {
				if ( getParameterFirstValue( Parameters.METHOD ).getFirstChild( "queryFormat" ).strValue().equals( "json" ) ) {
					qsFormat = format = "json";
					contentType = ContentTypes.APPLICATION_JSON;
				}
			}
			send_appendRequestUserHeader( message, headerBuilder );
			send_appendRequestHeaders( message, method, qsFormat, headerBuilder );
		}
		EncodedContent encodedContent = send_encodeContent( message, method, charset, format );
		if ( contentType != null ) {
			encodedContent.contentType = contentType;
		}
		send_appendGenericHeaders( message, encodedContent, charset, headerBuilder );
		headerBuilder.append( HttpUtils.CRLF );

		send_logDebugInfo( headerBuilder, encodedContent, charset );
		inputId = message.operationName();

		ostream.write( headerBuilder.toString().getBytes( HttpUtils.URL_DECODER_ENC ) );
		if ( encodedContent.content != null && !headRequest ) {
			ostream.write( encodedContent.content.getBytes() );
		}
		headRequest = false;
	}

	@Override
	public void send( OutputStream ostream, CommMessage message, InputStream istream )
		throws IOException
	{
		HttpUtils.send( ostream, message, istream, inInputPort, channel(), this );
	}

	private void parseXML( HttpMessage message, Value value, String charset )
		throws IOException
	{
		try {
			if ( message.size() > 0 ) {
				DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
				InputSource src = new InputSource( new ByteArrayInputStream( message.content() ) );
				src.setEncoding( charset );
				Document doc = builder.parse( src );
				XmlUtils.documentToValue( doc, value );
			}
		} catch( ParserConfigurationException pce ) {
			throw new IOException( pce );
		} catch( SAXException saxe ) {
			throw new IOException( saxe );
		}
	}

	private static void parseJson( HttpMessage message, Value value, boolean strictEncoding, String charset )
		throws IOException
	{
		JsUtils.parseJsonIntoValue( new InputStreamReader( new ByteArrayInputStream( message.content() ), charset ), value, strictEncoding );
	}

	private static void parseForm( HttpMessage message, Value value, String charset )
		throws IOException
	{
		String line = new String( message.content(), charset );
		String[] pair;
		for( String item : line.split( "&" ) ) {
			pair = item.split( "=", 2 );
			if ( pair.length > 1 ) {
				value.getChildren( URLDecoder.decode( pair[0], HttpUtils.URL_DECODER_ENC ) ).first().setValue( URLDecoder.decode( pair[1], HttpUtils.URL_DECODER_ENC ) );
			}
		}
	}

	private void parseMultiPartFormData( HttpMessage message, Value value, String charset )
		throws IOException
	{
		multiPartFormDataParser = new MultiPartFormDataParser( message, value );
		multiPartFormDataParser.parse();
	}

	private static String parseGWTRPC( HttpMessage message, Value value, String charset )
		throws IOException
	{
		RPCRequest request = RPC.decodeRequest( new String( message.content(), charset ) );
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
						type =
							cookieConfig.hasChildren( "type" ) ?
								cookieConfig.getFirstChild( "type" ).strValue()
							:
								"string";
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

	private static void recv_assignCookieValue( String cookieValue, Value value, String typeKeyword )
		throws IOException
	{
		NativeType type = NativeType.fromString( typeKeyword );
		if ( NativeType.INT == type ) {
			try {
				value.setValue( new Integer( cookieValue ) );
			} catch( NumberFormatException e ) {
				throw new IOException( e );
			}
		} else if ( NativeType.LONG == type ) {
			try {
				value.setValue( new Long( cookieValue ) );
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
		} else if ( NativeType.BOOL == type ) {
			value.setValue( Boolean.valueOf( cookieValue ) );
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

	private void recv_checkForGenericHeader( HttpMessage message, DecodedMessage decodedMessage )
		throws IOException
	{
		Value headers = null;
		if ( hasOperationSpecificParameter( decodedMessage.operationName, Parameters.HEADERS ) ) {
			headers = getOperationSpecificParameterFirstValue( decodedMessage.operationName, Parameters.HEADERS );
		} else if ( hasParameter( Parameters.HEADERS ) ) {
			headers = getParameterFirstValue( Parameters.HEADERS );
		}
		if ( headers != null ) {
			for( String headerName : headers.children().keySet() ) {
				String headerAlias = headers.getFirstChild( headerName ).strValue();
				headerName = headerName.replace( "_", "-" );
				decodedMessage.value.getFirstChild( headerAlias ).setValue( message.getPropertyOrEmptyString( headerName ) );
			}
		}
	}

	private static void recv_parseQueryString( HttpMessage message, Value value, String contentType, boolean strictEncoding )
		throws IOException
	{
		if ( message.isGet() && contentType.equals( ContentTypes.APPLICATION_JSON ) ) {
			recv_parseJsonQueryString( message, value, strictEncoding );
		} else {
			Map< String, Integer > indexes = new HashMap<>();
			String queryString = message.requestPath();
			String[] kv = queryString.split( "\\?", 2 );
			Integer index;
			if ( kv.length > 1 ) {
				queryString = kv[1];
				String[] params = queryString.split( "&" );
				for( String param : params ) {
					String[] ikv = param.split( "=", 2 );
					if ( ikv.length > 1 ) {
						index = indexes.get( ikv[0] );
						if ( index == null ) {
							index = 0;
							indexes.put( ikv[0], index );
						}
						// the query string was already URL decoded by the HttpParser
						value.getChildren( ikv[0] ).get( index ).setValue( ikv[1] );
						indexes.put( ikv[0], index + 1 );
					}
				}
			}
		}
	}

	private static void recv_parseJsonQueryString( HttpMessage message, Value value, boolean strictEncoding )
		throws IOException
	{
		String queryString = message.requestPath();
		String[] kv = queryString.split( "\\?", 2 );
		if ( kv.length > 1 ) {
			// the query string was already URL decoded by the HttpParser
			JsUtils.parseJsonIntoValue( new StringReader( kv[1] ), value, strictEncoding );
		}
	}

	/*
	 * Prints debug information about a received message
	 */
	private void recv_logDebugInfo( HttpMessage message, String charset )
		throws IOException
	{
		StringBuilder debugSB = new StringBuilder();
		debugSB.append( "[HTTP debug] Receiving:\n" );
		debugSB.append( "HTTP Code: " + message.statusCode() + "\n" );
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
			getParameterFirstValue( Parameters.DEBUG ).getFirstChild( "showContent" ).intValue() > 0
			&& message.size() > 0
		) {
			debugSB.append( "--> Message content\n" );
			debugSB.append( new String( message.content(), charset ) );
		}
		Interpreter.getInstance().logInfo( debugSB.toString() );
	}

	private void recv_parseRequestFormat( String type )
		throws IOException
	{
		responseFormat = null;

		if ( "text/xml".equals( type ) ) {
			responseFormat = "xml";
		} else if ( "text/x-gwt-rpc".equals( type ) ) {
			responseFormat = "text/x-gwt-rpc";
		} else if ( ContentTypes.APPLICATION_JSON.equals( type ) ) {
			responseFormat = "json";
		}
	}

	private void recv_parseMessage( HttpMessage message, DecodedMessage decodedMessage, String type, String charset )
		throws IOException
	{
		if ( getOperationSpecificStringParameter(inputId, Parameters.FORCE_CONTENT_DECODING ).equals("string") ) {
			decodedMessage.value.setValue( new String( message.content(), charset ) );
		} else if ( "text/html".equals( type ) ) {
			decodedMessage.value.setValue( new String( message.content(), charset ) );
		} else if ( "application/x-www-form-urlencoded".equals( type ) ) {
			parseForm( message, decodedMessage.value, charset );
		} else if ( "text/xml".equals( type ) || type.contains( "xml" ) ) {
			parseXML( message, decodedMessage.value, charset );
		} else if ( "text/x-gwt-rpc".equals( type ) ) {
			decodedMessage.operationName = parseGWTRPC( message, decodedMessage.value, charset );
		} else if ( "multipart/form-data".equals( type ) ) {
			parseMultiPartFormData( message, decodedMessage.value, charset );
		} else if (
			"application/octet-stream".equals( type ) || type.startsWith( "image/" )
			|| "application/zip".equals( type )
		) {
			decodedMessage.value.setValue( new ByteArray( message.content() ) );
		} else if ( ContentTypes.APPLICATION_JSON.equals( type ) || type.contains( "json" ) ) {
			boolean strictEncoding = checkStringParameter( Parameters.JSON_ENCODING, "strict" );
			parseJson( message, decodedMessage.value, strictEncoding, charset );
		} else {
			decodedMessage.value.setValue( new String( message.content(), charset ) );
		}
	}

	private String getDefaultOperation( HttpMessage.Type t )
	{
		if ( hasParameter( Parameters.DEFAULT_OPERATION ) ) {
			Value dParam = getParameterFirstValue( Parameters.DEFAULT_OPERATION );
			String method = HttpUtils.httpMessageTypeToString( t );
			if ( method == null || dParam.hasChildren( method ) == false ) {
				return dParam.strValue();
			} else {
				return dParam.getFirstChild( method ).strValue();
			}
		}

		return null;
	}

	private void recv_checkReceivingOperation( HttpMessage message, DecodedMessage decodedMessage )
	{
		if ( decodedMessage.operationName == null ) {
			String requestPath = message.requestPath().split( "\\?", 2 )[0];
			decodedMessage.operationName = requestPath.substring( 1 );
			Matcher m = LocationParser.RESOURCE_SEPARATOR_PATTERN.matcher( decodedMessage.operationName );
			if ( m.find() ) {
				int resourceStart = m.end();
				if ( m.find() ) {
					decodedMessage.resourcePath = requestPath.substring( resourceStart - 1, m.start() );
					decodedMessage.operationName = requestPath.substring( m.end(), requestPath.length() );
				}
			}
		}

		if ( decodedMessage.resourcePath.equals( "/" ) && !channel().parentInputPort().canHandleInputOperation( decodedMessage.operationName ) ) {
			String defaultOpId = getDefaultOperation( message.type() );
			if ( defaultOpId != null ) {
				Value body = decodedMessage.value;
				decodedMessage.value = Value.create();
				decodedMessage.value.getChildren( "data" ).add( body );
				decodedMessage.value.getFirstChild( "operation" ).setValue( decodedMessage.operationName );
				decodedMessage.value.setFirstChild( "requestUri", message.requestPath() );
				if ( message.userAgent() != null ) {
					decodedMessage.value.getFirstChild( Parameters.USER_AGENT ).setValue( message.userAgent() );
				}
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
		recv_checkForGenericHeader( message, decodedMessage );
		recv_checkForMultiPartHeaders( decodedMessage );
		if (
			message.userAgent() != null &&
			hasParameter( Parameters.USER_AGENT )
		) {
			getParameterFirstValue( Parameters.USER_AGENT ).setValue( message.userAgent() );
		}

		if ( getParameterVector( Parameters.HOST ) != null ) {
			getParameterFirstValue( Parameters.HOST ).setValue( message.getPropertyOrEmptyString( Parameters.HOST ) );
		}
	}

	private static class DecodedMessage {
		private String operationName = null;
		private Value value = Value.create();
		private String resourcePath = "/";
		private long id = CommMessage.GENERIC_ID;
	}

	private void recv_checkForStatusCode( HttpMessage message )
	{
		if ( hasParameter( Parameters.STATUS_CODE ) ) {
			getParameterFirstValue( Parameters.STATUS_CODE ).setValue( message.statusCode() );
		}
	}

	public CommMessage recv_internal( InputStream istream, OutputStream ostream )
		throws IOException
	{
		HttpMessage message = new HttpParser( istream ).parse();
		String charset = HttpUtils.getCharset( null, message );
		CommMessage retVal = null;
		DecodedMessage decodedMessage = new DecodedMessage();

		HttpUtils.recv_checkForChannelClosing( message, channel() );

		if ( checkBooleanParameter( Parameters.DEBUG ) ) {
			recv_logDebugInfo( message, charset );
		}

		recv_checkForStatusCode( message );

		encoding = message.getProperty( "accept-encoding" );
		headRequest = inInputPort && message.isHead();

		String contentType = DEFAULT_CONTENT_TYPE;
		if ( message.getProperty( "content-type" ) != null ) {
			contentType = message.getProperty( "content-type" ).split( ";", 2 )[0].toLowerCase();
		}

		// URI parameter parsing
		if ( message.requestPath() != null ) {
			boolean strictEncoding = checkStringParameter( Parameters.JSON_ENCODING, "strict" );
			recv_parseQueryString( message, decodedMessage.value, contentType, strictEncoding );
		}

		recv_parseRequestFormat( contentType );

		/* https://tools.ietf.org/html/rfc7231#section-4.3 */
		if ( !message.isGet() && !message.isHead() && !message.isDelete() ) {
			// body parsing
			if ( message.size() > 0 ) {
				recv_parseMessage( message, decodedMessage, contentType, charset );
			}
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
			String responseHeader = "";
			if ( hasParameter( Parameters.RESPONSE_HEADER ) || hasOperationSpecificParameter( inputId, Parameters.RESPONSE_HEADER ) ) {
				if ( hasOperationSpecificParameter( inputId, Parameters.RESPONSE_HEADER ) ) {
					responseHeader = getOperationSpecificStringParameter( inputId, Parameters.RESPONSE_HEADER );
				} else {
					responseHeader = getStringParameter( Parameters.RESPONSE_HEADER );
				}
				for( Entry<String, String> param : message.properties() ) {
					decodedMessage.value.getFirstChild( responseHeader ).getFirstChild( param.getKey() ).setValue( param.getValue() );
				}
				decodedMessage.value.getFirstChild( responseHeader ).getFirstChild( Parameters.STATUS_CODE ).setValue( message.statusCode() );
			}
			
			recv_checkForSetCookie( message, decodedMessage.value );
			retVal = new CommMessage( decodedMessage.id, inputId, decodedMessage.resourcePath, decodedMessage.value, null );
		} else if ( message.isError() == false ) {
			recv_checkReceivingOperation( message, decodedMessage );
			recv_checkForMessageProperties( message, decodedMessage );
			retVal = new CommMessage( decodedMessage.id, decodedMessage.operationName, decodedMessage.resourcePath, decodedMessage.value, null );
		}

		if ( retVal != null && "/".equals( retVal.resourcePath() ) && channel().parentPort() != null
			&& (channel().parentPort().getInterface().containsOperation( retVal.operationName() )
			|| channel().parentInputPort().getAggregatedOperation( retVal.operationName() ) != null) ) {
			try {
				// The message is for this service
				boolean hasInput = false;
				OneWayTypeDescription oneWayTypeDescription = null;
				if ( channel().parentInputPort() != null ) {
					if ( channel().parentInputPort().getAggregatedOperation( retVal.operationName() ) != null ) {
						oneWayTypeDescription = channel().parentInputPort().getAggregatedOperation( retVal.operationName() ).getOperationTypeDescription().asOneWayTypeDescription();
						hasInput = true;
					}
				}
				if ( !hasInput ) {
					Interface iface = channel().parentPort().getInterface();
					oneWayTypeDescription = iface.oneWayOperations().get( retVal.operationName() );
				}

				if ( oneWayTypeDescription != null ) {
					// We are receiving a One-Way message
					oneWayTypeDescription.requestType().cast( retVal.value() );
				} else {
					hasInput = false;
					RequestResponseTypeDescription rrTypeDescription = null;
					if ( channel().parentInputPort() != null ) {
						if ( channel().parentInputPort().getAggregatedOperation( retVal.operationName() ) != null ) {
							rrTypeDescription = channel().parentInputPort().getAggregatedOperation( retVal.operationName() ).getOperationTypeDescription().asRequestResponseTypeDescription();
							hasInput = true;
						}
					}

					if ( !hasInput ) {
						Interface iface = channel().parentPort().getInterface();
						rrTypeDescription = iface.requestResponseOperations().get( retVal.operationName() );
					}

					if ( retVal.isFault() ) {
						Type faultType = rrTypeDescription.faults().get( retVal.fault().faultName() );
						if ( faultType != null ) {
							faultType.cast( retVal.value() );
						}
					} else {
						if ( message.isResponse() ) {
							rrTypeDescription.responseType().cast( retVal.value() );
						} else {
							rrTypeDescription.requestType().cast( retVal.value() );
						}
					}
				}
			} catch( TypeCastingException e ) {
				// TODO: do something here?
			}
		}

		return retVal;
	}

	@Override
	public CommMessage recv( InputStream istream, OutputStream ostream )
		throws IOException
	{
		return HttpUtils.recv( istream, ostream, inInputPort, channel(), this );
	}

	private Type getSendType( CommMessage message )
		throws IOException
	{
		Type ret = null;

		if ( channel().parentPort() == null ) {
			throw new IOException( "Could not retrieve communication port for HTTP protocol" );
		}

		OperationTypeDescription opDesc = channel().parentPort().getOperationTypeDescription( message.operationName(), Constants.ROOT_RESOURCE_PATH );

		if ( opDesc == null ) {
			return null;
		}

		if ( opDesc.asOneWayTypeDescription() != null ) {
			if ( message.isFault() ) {
				ret = Type.UNDEFINED;
			} else {
				OneWayTypeDescription ow = opDesc.asOneWayTypeDescription();
				ret = ow.requestType();
			}
		} else if ( opDesc.asRequestResponseTypeDescription() != null ) {
			RequestResponseTypeDescription rr = opDesc.asRequestResponseTypeDescription();
			if ( message.isFault() ) {
				ret = rr.getFaultType( message.fault().faultName() );
				if ( ret == null ) {
					ret = Type.UNDEFINED;
				}
			} else {
				ret = ( inInputPort ) ? rr.responseType() : rr.requestType();
			}
		}

		return ret;
	}
}
