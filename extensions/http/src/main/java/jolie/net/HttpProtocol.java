/*
 * Copyright (C) 2008-2018 Fabrizio Montesi <famontesi@gmail.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package jolie.net;

import jolie.ExecutionThread;
import jolie.Interpreter;
import jolie.js.JsUtils;
import jolie.lang.Constants;
import jolie.lang.NativeType;
import jolie.monitoring.events.ProtocolMessageEvent;
import jolie.net.http.HttpMessage;
import jolie.net.http.HttpParser;
import jolie.net.http.HttpUtils;
import jolie.net.http.Method;
import jolie.net.http.MultiPartFormDataParser;
import jolie.net.ports.Interface;
import jolie.net.protocols.CommProtocol;
import jolie.runtime.ByteArray;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.VariablePath;
import jolie.runtime.typing.OneWayTypeDescription;
import jolie.runtime.typing.OperationTypeDescription;
import jolie.runtime.typing.RequestResponseTypeDescription;
import jolie.runtime.typing.Type;
import jolie.runtime.typing.TypeCastingException;
import jolie.tracer.ProtocolTraceAction;
import jolie.uri.UriUtils;
import jolie.util.LocationParser;
import jolie.xml.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

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
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
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

/**
 * HTTP protocol implementation
 * 
 * @author Fabrizio Montesi 14 Nov 2012 - Saverio Giallorenzo - Fabrizio Montesi: support for status
 *         codes
 */
public class HttpProtocol extends CommProtocol implements HttpUtils.HttpProtocol {
	private static final int DEFAULT_STATUS_CODE = 200;
	private static final int DEFAULT_REDIRECTION_STATUS_CODE = 303;
	private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream"; // default content type per RFC
																					// 2616#7.2.1
	private static final String DEFAULT_FORMAT = "xml";
	private static final Map< Integer, String > STATUS_CODE_DESCRIPTIONS = new HashMap<>();
	private static final Set< Integer > LOCATION_REQUIRED_STATUS_CODES = new HashSet<>();

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
		private static final String HEADERS_WILDCARD = "*";
		private static final String REQUEST_HEADERS = "requestHeaders";
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
		private static final String TEMPLATE = "template";
		private static final String OUTGOING_HEADERS = "outHeaders";
		private static final String INCOMING_HEADERS = "inHeaders";
		private static final String STATUS_CODES = "statusCodes";

		private static class MultiPartHeaders {
			private static final String FILENAME = "filename";
		}
	}

	private static class Headers {
		private static final String CONTENT_TYPE = "Content-Type";
		private static final String JOLIE_MESSAGE_ID = "X-Jolie-MessageID";
		private static final String JOLIE_RESOURCE_PATH = "X-Jolie-ServicePath";
	}

	private static class ContentTypes {
		private static final String APPLICATION_JSON = "application/json";
		private static final String APPLICATION_NDJSON = "application/x-ndjson";
	}

	private String inputId = null;
	private final Transformer transformer;
	private final DocumentBuilderFactory docBuilderFactory;
	private final DocumentBuilder docBuilder;
	private final URI uri;
	private final boolean inInputPort;
	private MultiPartFormDataParser multiPartFormDataParser = null;

	@Override
	public String name() {
		return "http";
	}

	@Override
	public boolean isThreadSafe() {
		return checkBooleanParameter( Parameters.CONCURRENT );
	}

	public HttpProtocol(
		VariablePath configurationPath,
		URI uri,
		boolean inInputPort,
		TransformerFactory transformerFactory,
		DocumentBuilderFactory docBuilderFactory,
		DocumentBuilder docBuilder )
		throws TransformerConfigurationException {
		super( configurationPath );
		this.uri = uri;
		this.inInputPort = inInputPort;
		this.transformer = transformerFactory.newTransformer();
		this.docBuilderFactory = docBuilderFactory;
		this.docBuilder = docBuilder;

		transformer.setOutputProperty( OutputKeys.OMIT_XML_DECLARATION, "yes" );
		transformer.setOutputProperty( OutputKeys.INDENT, "no" );
	}

	public String getMultipartHeaderForPart( String operationName, String partName ) {
		if( hasOperationSpecificParameter( operationName, Parameters.MULTIPART_HEADERS ) ) {
			Value v = getOperationSpecificParameterFirstValue( operationName, Parameters.MULTIPART_HEADERS );
			if( v.hasChildren( partName ) ) {
				v = v.getFirstChild( partName );
				if( v.hasChildren( Parameters.MultiPartHeaders.FILENAME ) ) {
					v = v.getFirstChild( Parameters.MultiPartHeaders.FILENAME );
					return v.strValue();
				}
			}
		}
		return null;
	}

	private final static String BOUNDARY = "----jol13h77p77bound4r155";

	private void send_appendCookies( CommMessage message, String hostname, StringBuilder headerBuilder ) {
		Value cookieParam = null;
		if( hasOperationSpecificParameter( message.operationName(), Parameters.COOKIES ) ) {
			cookieParam = getOperationSpecificParameterFirstValue( message.operationName(), Parameters.COOKIES );
		} else if( hasParameter( Parameters.COOKIES ) ) {
			cookieParam = getParameterFirstValue( Parameters.COOKIES );
		}
		if( cookieParam != null ) {
			Value cookieConfig;
			String domain;
			StringBuilder cookieSB = new StringBuilder();
			for( Entry< String, ValueVector > entry : cookieParam.children().entrySet() ) {
				cookieConfig = entry.getValue().first();
				if( message.value().hasChildren( cookieConfig.strValue() ) ) {
					domain =
						cookieConfig.hasChildren( "domain" ) ? cookieConfig.getFirstChild( "domain" ).strValue() : "";
					if( domain.isEmpty() || hostname.endsWith( domain ) ) {
						cookieSB
							.append( entry.getKey() )
							.append( '=' )
							.append( message.value().getFirstChild( cookieConfig.strValue() ).strValue() )
							.append( ";" );
					}
				}
			}
			if( cookieSB.length() > 0 ) {
				headerBuilder
					.append( "Cookie: " )
					.append( cookieSB )
					.append( HttpUtils.CRLF );
			}
		}
	}

	private void send_appendSetCookieHeader( CommMessage message, StringBuilder headerBuilder ) {
		Value cookieParam = null;
		if( hasOperationSpecificParameter( message.operationName(), Parameters.COOKIES ) ) {
			cookieParam = getOperationSpecificParameterFirstValue( message.operationName(), Parameters.COOKIES );
		} else if( hasParameter( Parameters.COOKIES ) ) {
			cookieParam = getParameterFirstValue( Parameters.COOKIES );
		}
		if( cookieParam != null ) {
			Value cookieConfig;
			for( Entry< String, ValueVector > entry : cookieParam.children().entrySet() ) {
				cookieConfig = entry.getValue().first();
				if( message.value().hasChildren( cookieConfig.strValue() ) ) {
					headerBuilder
						.append( "Set-Cookie: " )
						.append( entry.getKey() ).append( '=' )
						.append( message.value().getFirstChild( cookieConfig.strValue() ).strValue() )
						.append( "; expires=" )
						.append(
							cookieConfig.hasChildren( "expires" ) ? cookieConfig.getFirstChild( "expires" ).strValue()
								: "" )
						.append( "; domain=" )
						.append(
							cookieConfig.hasChildren( "domain" ) ? cookieConfig.getFirstChild( "domain" ).strValue()
								: "" )
						.append( "; path=" )
						.append(
							cookieConfig.hasChildren( "path" ) ? cookieConfig.getFirstChild( "path" ).strValue() : "" );
					if( cookieConfig.hasChildren( "secure" )
						&& cookieConfig.getFirstChild( "secure" ).intValue() > 0 ) {
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

	private void send_appendQuerystring( Value value, StringBuilder headerBuilder, CommMessage message )
		throws IOException {
		getOperationSpecificParameterFirstValue( message.operationName(),
			Parameters.OUTGOING_HEADERS ).children()
				.forEach( ( headerName, headerValues ) -> value.children().remove( headerValues.get( 0 ).strValue() ) );

		if( value.hasChildren() ) {
			headerBuilder.append( '?' );
			Iterator< Entry< String, ValueVector > > nodesIt = value.children().entrySet().iterator();
			while( nodesIt.hasNext() ) {
				Entry< String, ValueVector > entry = nodesIt.next();
				Iterator< Value > vecIt = entry.getValue().iterator();
				while( vecIt.hasNext() ) {
					Value v = vecIt.next();
					headerBuilder
						.append( URLEncoder.encode( entry.getKey(), HttpUtils.URL_DECODER_ENC ) )
						.append( '=' )
						.append( URLEncoder.encode( v.strValue(), HttpUtils.URL_DECODER_ENC ) );

					if( vecIt.hasNext() || nodesIt.hasNext() ) {
						headerBuilder.append( '&' );
					}
				}
			}
		}
	}

	private void send_appendJsonQueryString( CommMessage message, StringBuilder headerBuilder )
		throws IOException {
		getOperationSpecificParameterFirstValue( message.operationName(),
			Parameters.OUTGOING_HEADERS ).children().forEach(
				( headerName, headerValues ) -> message.value().children().remove( headerValues.get( 0 ).strValue() ) );
		if( message.value().isDefined() || message.value().hasChildren() ) {
			headerBuilder.append( "?" );
			StringBuilder builder = new StringBuilder();
			JsUtils.valueToJsonString( message.value(), true, getSendType( message ), builder );
			headerBuilder.append( URLEncoder.encode( builder.toString(), HttpUtils.URL_DECODER_ENC ) );
		}
	}

	private static void send_appendParsedAlias( String alias, Value value, StringBuilder headerBuilder )
		throws IOException {
		int offset = 0;
		List< String > aliasKeys = new ArrayList<>();
		String currStrValue;
		String currKey;
		StringBuilder result = new StringBuilder( alias );
		Matcher m = Pattern.compile( "%(!)?\\{[^\\}]*\\}" ).matcher( alias );

		while( m.find() ) {
			int displacement = 2;
			if( m.group( 1 ) == null ) { // ! is missing after %: We have to use URLEncoder
				currKey = alias.substring( m.start() + displacement, m.end() - 1 );
				if( "$".equals( currKey ) ) {
					currStrValue = URLEncoder.encode( value.strValue(), HttpUtils.URL_DECODER_ENC );
				} else {
					currStrValue =
						URLEncoder.encode( value.getFirstChild( currKey ).strValue(), HttpUtils.URL_DECODER_ENC );
					aliasKeys.add( currKey );
				}
			} else { // ! is given after %: We have to insert the string raw
				displacement = 3;
				currKey = alias.substring( m.start() + displacement, m.end() - 1 );
				if( "$".equals( currKey ) ) {
					currStrValue = value.strValue();
				} else {
					currStrValue = value.getFirstChild( currKey ).strValue();
					aliasKeys.add( currKey );
				}
			}

			result.replace(
				m.start() + offset, m.end() + offset,
				currStrValue );
			displacement++; // considering also }
			offset += currStrValue.length() - displacement - currKey.length();
		}
		// removing used keys
		aliasKeys.forEach( value.children()::remove );
		headerBuilder.append( result );
	}

	private String send_getFormat() {
		String format = DEFAULT_FORMAT;
		if( inInputPort && responseFormat != null ) {
			format = responseFormat;
			responseFormat = null;
		} else if( hasParameter( Parameters.FORMAT ) ) {
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
		throws IOException {
		EncodedContent ret = new EncodedContent();
		if( inInputPort == false && (method == Method.GET || method == Method.DELETE) ) {
			// We are building a GET or DELETE request
			return ret;
		}

		if( "xml".equals( format ) ) {
			ret.contentType = "text/xml";
			Document doc = docBuilder.newDocument();
			Element root = doc.createElement( message.operationName() + ((inInputPort) ? "Response" : "") );
			doc.appendChild( root );
			if( message.isFault() ) {
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
		} else if( "binary".equals( format ) ) {
			ret.contentType = "application/octet-stream";
			ret.content = message.value().byteArrayValue();
		} else if( "html".equals( format ) ) {
			ret.contentType = "text/html";
			if( message.isFault() ) {
				StringBuilder builder = new StringBuilder();
				builder.append( "<html><head><title>" )
					.append( message.fault().faultName() )
					.append( "</title></head><body>" )
					.append( message.fault().value().strValue() )
					.append( "</body></html>" );
				ret.content = new ByteArray( builder.toString().getBytes( charset ) );
			} else {
				ret.content = new ByteArray( message.value().strValue().getBytes( charset ) );
			}
		} else if( "multipart/form-data".equals( format ) ) {
			ret.contentType = "multipart/form-data; boundary=" + BOUNDARY;
			ByteArrayOutputStream bStream = new ByteArrayOutputStream();
			StringBuilder builder = new StringBuilder();
			for( Entry< String, ValueVector > entry : message.value().children().entrySet() ) {
				if( !entry.getKey().startsWith( "@" ) ) {
					builder.append( "--" ).append( BOUNDARY ).append( HttpUtils.CRLF )
						.append( "Content-Disposition: form-data; name=\"" ).append( entry.getKey() ).append( '\"' );
					boolean isBinary = false;
					if( hasOperationSpecificParameter( message.operationName(), Parameters.MULTIPART_HEADERS ) ) {
						Value specOpParam = getOperationSpecificParameterFirstValue( message.operationName(),
							Parameters.MULTIPART_HEADERS );
						if( specOpParam.hasChildren( "partName" ) ) {
							ValueVector partNames = specOpParam.getChildren( "partName" );
							for( int p = 0; p < partNames.size(); p++ ) {
								if( partNames.get( p ).hasChildren( "part" ) ) {
									if( partNames.get( p ).getFirstChild( "part" ).strValue()
										.equals( entry.getKey() ) ) {
										isBinary = true;
										if( partNames.get( p ).hasChildren( "filename" ) ) {
											builder.append( "; filename=\"" )
												.append( partNames.get( p ).getFirstChild( "filename" ).strValue() )
												.append( "\"" );
										}
										if( partNames.get( p ).hasChildren( "contentType" ) ) {
											builder.append( HttpUtils.CRLF ).append( "Content-Type:" )
												.append( partNames.get( p ).getFirstChild( "contentType" ).strValue() );
										}
									}
								}
							}
						}
					}

					builder.append( HttpUtils.CRLF ).append( HttpUtils.CRLF );
					if( isBinary ) {
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
			bStream.write( builder.toString().getBytes( charset ) );
			ret.content = new ByteArray( bStream.toByteArray() );
		} else if( "x-www-form-urlencoded".equals( format ) ) {
			ret.contentType = "application/x-www-form-urlencoded";
			Iterator< Entry< String, ValueVector > > it =
				message.value().children().entrySet().iterator();
			StringBuilder builder = new StringBuilder();
			if( message.isFault() ) {
				builder.append( "faultName=" )
					.append( URLEncoder.encode( message.fault().faultName(), HttpUtils.URL_DECODER_ENC ) )
					.append( "&data=" )
					.append( URLEncoder.encode( message.fault().value().strValue(), HttpUtils.URL_DECODER_ENC ) );
			} else {
				Entry< String, ValueVector > entry;
				while( it.hasNext() ) {
					entry = it.next();
					builder.append( URLEncoder.encode( entry.getKey(), HttpUtils.URL_DECODER_ENC ) )
						.append( "=" )
						.append( URLEncoder.encode( entry.getValue().first().strValue(), HttpUtils.URL_DECODER_ENC ) );
					if( it.hasNext() ) {
						builder.append( '&' );
					}
				}
			}
			ret.content = new ByteArray( builder.toString().getBytes( charset ) );
		} else if( "json".equals( format ) ) {
			ret.contentType = ContentTypes.APPLICATION_JSON;
			StringBuilder jsonStringBuilder = new StringBuilder();
			if( message.isFault() ) {
				Value error = message.value().getFirstChild( "error" );
				error.getFirstChild( "code" ).setValue( -32000 );
				error.getFirstChild( "message" ).setValue( message.fault().faultName() );
				error.getChildren( "data" ).set( 0, message.fault().value() );
				JsUtils.faultValueToJsonString( message.value(), getSendType( message ), jsonStringBuilder );
			} else {
				JsUtils.valueToJsonString( message.value(), true, getSendType( message ), jsonStringBuilder );
			}
			ret.content = new ByteArray( jsonStringBuilder.toString().getBytes( charset ) );
		} else if( "ndjson".equals( format ) ) {
			ret.contentType = ContentTypes.APPLICATION_NDJSON;
			StringBuilder ndJsonStringBuilder = new StringBuilder();
			if( message.isFault() ) {
				Value error = message.value().getFirstChild( "error" );
				error.getFirstChild( "code" ).setValue( -32000 );
				error.getFirstChild( "message" ).setValue( message.fault().faultName() );
				error.getChildren( "data" ).set( 0, message.fault().value() );
				JsUtils.faultValueToJsonString( message.value(), getSendType( message ), ndJsonStringBuilder );
			} else {
				if( !message.value().hasChildren( "item" ) ) {
					Interpreter.getInstance().logWarning( "ndJson requires at least one child node 'item'" );
				}
				JsUtils.valueToNdJsonString( message.value(), true, getSendType( message ), ndJsonStringBuilder );
			}
			ret.content = new ByteArray( ndJsonStringBuilder.toString().getBytes( charset ) );
		} else if( "raw".equals( format ) ) {
			ret.contentType = "text/plain";
			if( message.isFault() ) {
				ret.content = new ByteArray( message.fault().value().strValue().getBytes( charset ) );
			} else {
				ret.content = new ByteArray( message.value().strValue().getBytes( charset ) );
			}
		}
		return ret;
	}

	private static boolean isLocationNeeded( int statusCode ) {
		return LOCATION_REQUIRED_STATUS_CODES.contains( statusCode );
	}

	private void send_appendResponseUserHeader( CommMessage message, StringBuilder headerBuilder ) {
		Value responseHeaderParameters = null;
		if( hasOperationSpecificParameter( message.operationName(), Parameters.RESPONSE_USER ) ) {
			responseHeaderParameters =
				getOperationSpecificParameterFirstValue( message.operationName(), Parameters.RESPONSE_USER );
			if( (responseHeaderParameters != null)
				&& (responseHeaderParameters.hasChildren( Parameters.HEADER_USER )) ) {
				for( Entry< String, ValueVector > entry : responseHeaderParameters
					.getFirstChild( Parameters.HEADER_USER ).children().entrySet() )
					for( int counter = 0; counter < entry.getValue().size(); counter++ ) {
						headerBuilder.append( entry.getKey() ).append( ": " )
							.append( entry.getValue().get( counter ).strValue() )
							.append( HttpUtils.CRLF );
					}

			}
		}

		responseHeaderParameters = null;
		if( hasParameter( Parameters.RESPONSE_USER ) ) {
			responseHeaderParameters = getParameterFirstValue( Parameters.RESPONSE_USER );

			if( (responseHeaderParameters != null)
				&& (responseHeaderParameters.hasChildren( Parameters.HEADER_USER )) ) {
				for( Entry< String, ValueVector > entry : responseHeaderParameters
					.getFirstChild( Parameters.HEADER_USER ).children().entrySet() )
					for( int counter = 0; counter < entry.getValue().size(); counter++ ) {
						headerBuilder.append( entry.getKey() ).append( ": " )
							.append( entry.getValue().get( counter ).strValue() )
							.append( HttpUtils.CRLF );
					}
			}
		}
	}

	private void send_appendResponseHeaders( CommMessage message, StringBuilder headerBuilder ) {
		int statusCode = DEFAULT_STATUS_CODE;
		String statusDescription = null;

		if( message.isFault()
			&& hasOperationSpecificParameter( message.operationName(), Parameters.STATUS_CODES ) ) {
			Value exceptionsValue = getOperationSpecificParameterFirstValue( message.operationName(),
				Parameters.STATUS_CODES );
			if( exceptionsValue.hasChildren( message.fault().faultName() ) ) {
				statusCode = exceptionsValue.getFirstChild( message.fault().faultName() ).intValue();
			}
		} else if( hasParameter( Parameters.STATUS_CODE ) ) {
			statusCode = getIntParameter( Parameters.STATUS_CODE );
			if( !STATUS_CODE_DESCRIPTIONS.containsKey( statusCode ) ) {
				Interpreter.getInstance().logWarning( "HTTP protocol for operation " +
					message.operationName() +
					" is sending a message with status code " +
					statusCode +
					", which is not in the HTTP specifications." );
				statusDescription = "Internal Server Error";
			} else if( isLocationNeeded( statusCode ) && !hasParameter( Parameters.REDIRECT ) ) {
				// if statusCode is a redirection code, location parameter is needed
				Interpreter.getInstance().logWarning( "HTTP protocol for operation " +
					message.operationName() +
					" is sending a message with status code " +
					statusCode +
					", which expects a redirect parameter but the latter is not set." );
			}
		} else if( hasParameter( Parameters.REDIRECT ) ) {
			statusCode = DEFAULT_REDIRECTION_STATUS_CODE;
		} else if( message.isFault() ) {
			statusCode = 500;
		}

		if( statusDescription == null ) {
			statusDescription = STATUS_CODE_DESCRIPTIONS.get( statusCode );
		}
		headerBuilder.append( "HTTP/1.1 " ).append( statusCode ).append( " " ).append( statusDescription )
			.append( HttpUtils.CRLF );

		// if redirect has been set, the redirect location parameter is set
		if( hasParameter( Parameters.REDIRECT ) ) {
			headerBuilder.append( "Location: " ).append( getStringParameter( Parameters.REDIRECT ) )
				.append( HttpUtils.CRLF );
		}

		send_appendSetCookieHeader( message, headerBuilder );
		headerBuilder.append( "Server: Jolie" ).append( HttpUtils.CRLF );
		StringBuilder cacheControlHeader = new StringBuilder();
		if( hasParameter( Parameters.CACHE_CONTROL ) ) {
			Value cacheControl = getParameterFirstValue( Parameters.CACHE_CONTROL );
			if( cacheControl.hasChildren( "maxAge" ) ) {
				cacheControlHeader.append( "max-age=" ).append( cacheControl.getFirstChild( "maxAge" ).intValue() );
			}
		}
		if( cacheControlHeader.length() > 0 ) {
			headerBuilder.append( "Cache-Control: " ).append( cacheControlHeader ).append( HttpUtils.CRLF );
		}
	}

	private static void send_appendRequestMethod( Method method, StringBuilder headerBuilder ) {
		headerBuilder.append( method.id() );
	}

	private void send_appendRequestPath( CommMessage message, Method method, String qsFormat,
		StringBuilder headerBuilder )
		throws IOException {
		String path = uri.getRawPath();
		if( uri.getScheme().equals( "localsocket" ) || path == null || path.isEmpty()
			|| checkBooleanParameter( Parameters.DROP_URI_PATH, false ) ) {
			headerBuilder.append( '/' );
		} else {
			if( path.charAt( 0 ) != '/' ) {
				headerBuilder.append( '/' );
			}
			headerBuilder.append( path );
			final Matcher m = LocationParser.RESOURCE_SEPARATOR_PATTERN.matcher( path );
			if( m.find() ) {
				if( !m.find() ) {
					headerBuilder.append( LocationParser.RESOURCE_SEPARATOR );
				}
			}
		}

		if( hasOperationSpecificParameter( message.operationName(), Parameters.ALIAS ) ) {
			String alias = getOperationSpecificStringParameter( message.operationName(), Parameters.ALIAS );
			send_appendParsedAlias( alias, message.value(), headerBuilder );
		} else if( hasOperationSpecificParameter( message.operationName(), Parameters.TEMPLATE ) ) {
			String template = getOperationSpecificStringParameter( message.operationName(), Parameters.TEMPLATE );
			send_appendParsedTemplate( template, message.value(), headerBuilder );
		} else {
			headerBuilder.append( message.operationName() );
		}

		if( method == Method.GET ) {
			if( qsFormat.equals( "json" ) ) {
				send_appendJsonQueryString( message, headerBuilder );
			} else {
				send_appendQuerystring( message.value(), headerBuilder, message );
			}
		}
	}

	private void send_appendParsedTemplate( String template, Value value, StringBuilder headerBuilder ) {
		List< String > templateKeys = new ArrayList<>();
		UriUtils uriUtils = new UriUtils();
		Map< String, Object > params = new HashMap<>();

		for( final Map.Entry< String, ValueVector > entry : value.children()
			.entrySet() ) {
			params.put( entry.getKey(), entry.getValue().first().valueObject() );
		}

		String uri = uriUtils.expand( template, params );

		/* cleaning value from used keys */


		uriUtils.match( template, uri ).children().forEach( ( s, values ) -> {
			templateKeys.add( s );
		} );

		templateKeys.forEach( value.children()::remove );
		headerBuilder.append( uri );

	}

	private static void send_appendAuthorizationHeader( CommMessage message, StringBuilder headerBuilder ) {
		if( message.value()
			.hasChildren( jolie.lang.Constants.Predefined.HTTP_BASIC_AUTHENTICATION.token().content() ) ) {
			Value v = message.value()
				.getFirstChild( jolie.lang.Constants.Predefined.HTTP_BASIC_AUTHENTICATION.token().content() );
			// String realm = v.getFirstChild( "realm" ).strValue();
			String userpass =
				v.getFirstChild( "userid" ).strValue() + ":" +
					v.getFirstChild( "password" ).strValue();
			Base64.Encoder encoder = Base64.getEncoder();
			userpass = encoder.encodeToString( userpass.getBytes() );
			headerBuilder.append( "Authorization: Basic " ).append( userpass ).append( HttpUtils.CRLF );
			message.value().children()
				.remove( jolie.lang.Constants.Predefined.HTTP_BASIC_AUTHENTICATION.token().content() );
		}
	}

	private void send_appendRequestUserHeader( CommMessage message, StringBuilder headerBuilder ) {
		Value responseHeaderParameters = null;
		if( hasOperationSpecificParameter( message.operationName(), Parameters.REQUEST_USER ) ) {
			responseHeaderParameters =
				getOperationSpecificParameterFirstValue( message.operationName(), Parameters.RESPONSE_USER );
			if( (responseHeaderParameters != null)
				&& (responseHeaderParameters.hasChildren( Parameters.HEADER_USER )) ) {
				for( Entry< String, ValueVector > entry : responseHeaderParameters
					.getFirstChild( Parameters.HEADER_USER ).children().entrySet() )
					headerBuilder.append( entry.getKey() ).append( ": " ).append( entry.getValue().first().strValue() )
						.append( HttpUtils.CRLF );
			}
		}

		responseHeaderParameters = null;
		if( hasParameter( Parameters.RESPONSE_USER ) ) {
			responseHeaderParameters = getParameterFirstValue( Parameters.REQUEST_USER );
			if( (responseHeaderParameters != null)
				&& (responseHeaderParameters.hasChildren( Parameters.HEADER_USER )) ) {
				for( Entry< String, ValueVector > entry : responseHeaderParameters
					.getFirstChild( Parameters.HEADER_USER ).children().entrySet() )
					headerBuilder.append( entry.getKey() ).append( ": " ).append( entry.getValue().first().strValue() )
						.append( HttpUtils.CRLF );
			}
		}
	}

	private void send_appendHeader( StringBuilder headerBuilder ) {
		if( hasParameter( Parameters.ADD_HEADERS ) ) {
			Value v = getParameterFirstValue( Parameters.ADD_HEADERS );
			if( v.hasChildren( "header" ) ) {
				for( Value head : v.getChildren( "header" ) ) {
					String header = head.strValue() + ": "
						+ head.getFirstChild( "value" ).strValue();
					headerBuilder.append( header ).append( HttpUtils.CRLF );
				}
			}
		}
		if( !inInputPort && hasParameter( Parameters.REQUEST_HEADERS ) ) {
			getParameterFirstValue( Parameters.REQUEST_HEADERS )
				.children().forEach( ( header, vector ) -> {
					headerBuilder.append( header )
						.append( ": " )
						.append( vector.first().strValue() )
						.append( HttpUtils.CRLF );
				} );
		}
	}

	private Method send_getRequestMethod( CommMessage message )
		throws IOException {
		return hasOperationSpecificParameter( message.operationName(), Parameters.METHOD )
			? Method.fromString( getOperationSpecificStringParameter( message.operationName(), Parameters.METHOD ) )
			: hasParameterValue( Parameters.METHOD ) ? Method.fromString( getStringParameter( Parameters.METHOD ) )
				: Method.POST;
	}

	private void send_appendRequestHeaders( CommMessage message, Method method, String qsFormat,
		StringBuilder headerBuilder )
		throws IOException {
		send_appendRequestMethod( method, headerBuilder );
		headerBuilder.append( ' ' );
		send_appendRequestPath( message, method, qsFormat, headerBuilder );
		headerBuilder.append( " HTTP/1.1" ).append( HttpUtils.CRLF );
		String host = uri.getHost();
		if( uri.getScheme().equals( "localsocket" ) ) {
			/*
			 * in this case we need to replace the localsocket path with a host, that is the default one
			 * localhost
			 */
			host = "localhost";
		}
		headerBuilder.append( "Host: " ).append( host ).append( HttpUtils.CRLF );
		send_appendCookies( message, uri.getHost(), headerBuilder );
		send_appendAuthorizationHeader( message, headerBuilder );
		if( checkBooleanParameter( Parameters.COMPRESSION, true ) ) {
			String requestCompression = getStringParameter( Parameters.REQUEST_COMPRESSION );
			if( requestCompression.equals( "gzip" ) || requestCompression.equals( "deflate" ) ) {
				encoding = requestCompression;
				headerBuilder.append( "Accept-Encoding: " ).append( encoding ).append( HttpUtils.CRLF );
			} else {
				headerBuilder.append( "Accept-Encoding: gzip, deflate" ).append( HttpUtils.CRLF );
			}
		}
		send_appendHeader( headerBuilder );

		if( hasOperationSpecificParameter( message.operationName(), Parameters.OUTGOING_HEADERS ) ) {
			send_operationSpecificHeader( message.value(),
				getOperationSpecificParameterFirstValue( message.operationName(), Parameters.OUTGOING_HEADERS ),
				headerBuilder );
		}
	}

	private void send_operationSpecificHeader( Value value, Value outboundHeaders, StringBuilder headerBuilder ) {
		List< String > headersKeys = new ArrayList<>();
		outboundHeaders.children().forEach( ( headerName, headerValues ) -> {
			headerBuilder.append( headerName ).append( ": " )
				.append( value.getFirstChild( headerValues.get( 0 ).strValue() ).strValue() )
				.append( HttpUtils.CRLF );
			headersKeys.add( headerValues.get( 0 ).strValue() );
		} );
		headersKeys.forEach( value.children()::remove );
	}

	private void send_appendGenericHeaders(
		CommMessage message,
		EncodedContent encodedContent,
		String charset,
		StringBuilder headerBuilder )
		throws IOException {
		if( !checkBooleanParameter( Parameters.KEEP_ALIVE, true ) ) {
			if( inInputPort ) // we may do this only in input (server) mode
				channel().setToBeClosed( true );
			headerBuilder.append( "Connection: close" ).append( HttpUtils.CRLF );
		}
		if( checkBooleanParameter( Parameters.CONCURRENT, true ) ) {
			headerBuilder.append( Headers.JOLIE_MESSAGE_ID ).append( ": " ).append( message.requestId() )
				.append( HttpUtils.CRLF );
		}

		headerBuilder.append( Headers.JOLIE_RESOURCE_PATH ).append( ": " ).append( message.resourcePath() )
			.append( HttpUtils.CRLF );

		String contentType = getStringParameter( Parameters.CONTENT_TYPE );
		if( contentType.length() > 0 ) {
			encodedContent.contentType = contentType;
		}
		encodedContent.contentType = encodedContent.contentType.toLowerCase();

		headerBuilder.append( "Content-Type: " ).append( encodedContent.contentType );
		if( charset != null ) {
			headerBuilder.append( "; charset=" ).append( charset.toLowerCase() );
		}
		headerBuilder.append( HttpUtils.CRLF );

		if( encodedContent.content != null ) {
			String transferEncoding = getStringParameter( Parameters.CONTENT_TRANSFER_ENCODING );
			if( transferEncoding.length() > 0 ) {
				headerBuilder.append( "Content-Transfer-Encoding: " ).append( transferEncoding )
					.append( HttpUtils.CRLF );
			}

			String contentDisposition = getStringParameter( Parameters.CONTENT_DISPOSITION );
			if( contentDisposition.length() > 0 ) {
				encodedContent.contentDisposition = contentDisposition;
				headerBuilder.append( "Content-Disposition: " ).append( encodedContent.contentDisposition )
					.append( HttpUtils.CRLF );
			}

			boolean compression = encoding != null && checkBooleanParameter( Parameters.COMPRESSION, true );
			String compressionTypes = getStringParameter(
				Parameters.COMPRESSION_TYPES,
				"text/html text/css text/plain text/xml text/x-js application/json application/javascript application/x-www-form-urlencoded application/xhtml+xml application/xml x-font/otf x-font/ttf application/x-font-ttf" )
					.toLowerCase();
			if( compression && !compressionTypes.equals( "*" )
				&& !compressionTypes.contains( encodedContent.contentType ) ) {
				compression = false;
			}
			if( compression ) {
				Interpreter.getInstance().tracer().trace( () -> {
					try {
						final String traceMessage = encodedContent.content.toString( charset );
						return new ProtocolTraceAction( ProtocolTraceAction.Type.HTTP, "HTTP COMPRESSING MESSAGE",
							message.resourcePath(), traceMessage, null );
					} catch( UnsupportedEncodingException e ) {
						return new ProtocolTraceAction( ProtocolTraceAction.Type.HTTP, "HTTP COMPRESSING MESSAGE",
							message.resourcePath(), e.getMessage(), null );
					}

				} );
				encodedContent.content = HttpUtils.encode( encoding, encodedContent.content, headerBuilder );
			}

			headerBuilder.append( "Content-Length: " ).append( encodedContent.content.size() ).append( HttpUtils.CRLF );
		} else {
			headerBuilder.append( "Content-Length: 0" ).append( HttpUtils.CRLF );
		}
	}

	private String prepareSendDebugString( CharSequence header, EncodedContent encodedContent, String charset,
		boolean showContent )
		throws UnsupportedEncodingException {
		StringBuilder debugSB = new StringBuilder();
		debugSB.append( "[HTTP debug] Sending:\n" )
			.append( header );
		if( showContent && encodedContent != null && encodedContent.content != null ) {
			debugSB.append( encodedContent.content.toString( charset ) );
		}
		return debugSB.toString();
	}

	private void send_logDebugInfo( CharSequence header, EncodedContent encodedContent, String charset )
		throws IOException {
		if( checkBooleanParameter( Parameters.DEBUG ) ) {
			boolean showContent = false;
			if( getParameterVector( Parameters.DEBUG ).first().getFirstChild( "showContent" ).intValue() > 0
				&& encodedContent.content != null ) {
				showContent = true;
			}
			Interpreter.getInstance().logInfo( prepareSendDebugString( header, encodedContent, charset, showContent ) );
		}
	}

	@Override
	public void send_internal( OutputStream ostream, CommMessage message, InputStream istream )
		throws IOException {
		Method method = send_getRequestMethod( message );
		String charset = HttpUtils.getCharset( getStringParameter( Parameters.CHARSET, "utf-8" ), null );
		String format = send_getFormat();
		String contentType = null;
		StringBuilder headerBuilder = new StringBuilder();

		if( inInputPort ) {
			// We're responding to a request
			send_appendResponseHeaders( message, headerBuilder );
			send_appendResponseUserHeader( message, headerBuilder );
			send_appendHeader( headerBuilder );

		} else {
			// We're sending a notification or a solicit
			String qsFormat = "";
			if( method == Method.GET && getParameterFirstValue( Parameters.METHOD ).hasChildren( "queryFormat" ) ) {
				if( getParameterFirstValue( Parameters.METHOD ).getFirstChild( "queryFormat" ).strValue()
					.equals( "json" ) ) {
					qsFormat = format = "json";
					contentType = ContentTypes.APPLICATION_JSON;
				}
			}
			send_appendRequestUserHeader( message, headerBuilder );
			send_appendRequestHeaders( message, method, qsFormat, headerBuilder );
		}

		EncodedContent encodedContent = send_encodeContent( message, method, charset, format );
		if( contentType != null ) {
			encodedContent.contentType = contentType;
		}

		// message's body in string format needed for the monitoring
		String bodyMessageString =
			encodedContent != null && encodedContent.content != null ? encodedContent.content.toString( charset ) : "";

		if( Interpreter.getInstance().isMonitoring() ) {
			Interpreter.getInstance().fireMonitorEvent(
				new ProtocolMessageEvent(
					bodyMessageString,
					headerBuilder.toString(),
					ExecutionThread.currentThread().getSessionId(),
					Long.toString( message.id() ),
					ProtocolMessageEvent.Protocol.HTTP ) );
		}

		send_appendGenericHeaders( message, encodedContent, charset, headerBuilder );
		headerBuilder.append( HttpUtils.CRLF );

		send_logDebugInfo( headerBuilder, encodedContent, charset );

		Interpreter.getInstance().tracer().trace( () -> {
			try {
				final String traceMessage = prepareSendDebugString( headerBuilder, encodedContent, charset, true );
				return new ProtocolTraceAction( ProtocolTraceAction.Type.HTTP, "HTTP MESSAGE SENT",
					message.resourcePath(), traceMessage, null );
			} catch( UnsupportedEncodingException e ) {
				return new ProtocolTraceAction( ProtocolTraceAction.Type.HTTP, "HTTP MESSAGE SENT",
					message.resourcePath(), e.getMessage(), null );
			}
		} );

		inputId = message.operationName();

		ostream.write( headerBuilder.toString().getBytes( HttpUtils.URL_DECODER_ENC ) );
		if( encodedContent.content != null && !headRequest ) {
			ostream.write( encodedContent.content.getBytes() );
		}
		headRequest = false;
	}

	@Override
	public void send( OutputStream ostream, CommMessage message, InputStream istream )
		throws IOException {
		HttpUtils.send( ostream, message, istream, inInputPort, channel(), this );
	}

	private void parseXML( HttpMessage message, Value value, String charset )
		throws IOException {
		try {
			if( message.size() > 0 ) {
				DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
				InputSource src = new InputSource( new ByteArrayInputStream( message.content() ) );
				src.setEncoding( charset );
				Document doc = builder.parse( src );
				XmlUtils.documentToValue( doc, value, false );
			}
		} catch( ParserConfigurationException | SAXException pce ) {
			throw new IOException( pce );
		}
	}

	private static void parseJson( HttpMessage message, Value value, boolean strictEncoding, String charset )
		throws IOException {
		JsUtils.parseJsonIntoValue( new InputStreamReader( new ByteArrayInputStream( message.content() ), charset ),
			value, strictEncoding );
	}

	private static void parseNdJson( HttpMessage message, Value value, boolean strictEncoding, String charset )
		throws IOException {
		JsUtils.parseNdJsonIntoValue(
			new BufferedReader( new InputStreamReader( new ByteArrayInputStream( message.content() ), charset ) ),
			value, strictEncoding );
	}

	private static void parseForm( HttpMessage message, Value value, String charset )
		throws IOException {
		String line = new String( message.content(), charset );
		String[] pair;
		for( String item : line.split( "&" ) ) {
			pair = item.split( "=", 2 );
			if( pair.length > 1 ) {
				value.getChildren( URLDecoder.decode( pair[ 0 ], HttpUtils.URL_DECODER_ENC ) ).first()
					.setValue( URLDecoder.decode( pair[ 1 ], HttpUtils.URL_DECODER_ENC ) );
			}
		}
	}

	private void parseMultiPartFormData( HttpMessage message, Value value )
		// , String charset )
		throws IOException {
		multiPartFormDataParser = new MultiPartFormDataParser( message, value );
		multiPartFormDataParser.parse();
	}

	private void recv_checkForSetCookie( HttpMessage message, Value value )
		throws IOException {
		if( hasParameter( Parameters.COOKIES ) ) {
			String type;
			Value cookies = getParameterFirstValue( Parameters.COOKIES );
			Value cookieConfig;
			Value v;
			for( HttpMessage.Cookie cookie : message.setCookies() ) {
				if( cookies.hasChildren( cookie.name() ) ) {
					cookieConfig = cookies.getFirstChild( cookie.name() );
					if( cookieConfig.isString() ) {
						v = value.getFirstChild( cookieConfig.strValue() );
						type =
							cookieConfig.hasChildren( "type" ) ? cookieConfig.getFirstChild( "type" ).strValue()
								: "string";
						recv_assignCookieValue( cookie.value(), v, type );
					}
				}

				/*
				 * currValue = Value.create(); currValue.getNewChild( "expires" ).setValue( cookie.expirationDate()
				 * ); currValue.getNewChild( "path" ).setValue( cookie.path() ); currValue.getNewChild( "name"
				 * ).setValue( cookie.name() ); currValue.getNewChild( "value" ).setValue( cookie.value() );
				 * currValue.getNewChild( "domain" ).setValue( cookie.domain() ); currValue.getNewChild( "secure"
				 * ).setValue( (cookie.secure() ? 1 : 0) ); cookieVec.add( currValue );
				 */
			}
		}
	}

	private static void recv_assignCookieValue( String cookieValue, Value value, String typeKeyword )
		throws IOException {
		NativeType type = NativeType.fromString( typeKeyword );
		if( NativeType.INT == type ) {
			try {
				value.setValue( Integer.valueOf( cookieValue ) );
			} catch( NumberFormatException e ) {
				throw new IOException( e );
			}
		} else if( NativeType.LONG == type ) {
			try {
				value.setValue( Long.valueOf( cookieValue ) );
			} catch( NumberFormatException e ) {
				throw new IOException( e );
			}
		} else if( NativeType.STRING == type ) {
			value.setValue( cookieValue );
		} else if( NativeType.DOUBLE == type ) {
			try {
				value.setValue( new Double( cookieValue ) );
			} catch( NumberFormatException e ) {
				throw new IOException( e );
			}
		} else if( NativeType.BOOL == type ) {
			value.setValue( Boolean.valueOf( cookieValue ) );
		} else {
			value.setValue( cookieValue );
		}
	}

	private void recv_checkForCookies( HttpMessage message, DecodedMessage decodedMessage )
		throws IOException {
		Value cookies = null;
		if( hasOperationSpecificParameter( decodedMessage.operationName, Parameters.COOKIES ) ) {
			cookies = getOperationSpecificParameterFirstValue( decodedMessage.operationName, Parameters.COOKIES );
		} else if( hasParameter( Parameters.COOKIES ) ) {
			cookies = getParameterFirstValue( Parameters.COOKIES );
		}
		if( cookies != null ) {
			Value v;
			String type;
			for( Entry< String, String > entry : message.cookies().entrySet() ) {
				if( cookies.hasChildren( entry.getKey() ) ) {
					Value cookieConfig = cookies.getFirstChild( entry.getKey() );
					if( cookieConfig.isString() ) {
						v = decodedMessage.value.getFirstChild( cookieConfig.strValue() );
						if( cookieConfig.hasChildren( "type" ) ) {
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
		throws IOException {
		Value headers = null;
		if( hasOperationSpecificParameter( decodedMessage.operationName, Parameters.INCOMING_HEADERS ) ) {
			headers = getOperationSpecificParameterFirstValue( decodedMessage.operationName,
				Parameters.INCOMING_HEADERS );
		} else if( hasOperationSpecificParameter( decodedMessage.operationName, Parameters.HEADERS ) ) {
			headers = getOperationSpecificParameterFirstValue( decodedMessage.operationName, Parameters.HEADERS );
		} else if( hasParameter( Parameters.HEADERS ) ) {
			headers = getParameterFirstValue( Parameters.HEADERS );
		}
		if( headers != null ) {
			if( headers.hasChildren( Parameters.HEADERS_WILDCARD ) ) {
				String headerAlias = headers.getFirstChild( Parameters.HEADERS_WILDCARD ).strValue();
				message.properties().forEach( propertyEntry -> {
					decodedMessage.value.getFirstChild( headerAlias )
						.getFirstChild( propertyEntry.getKey() )
						.setValue( propertyEntry.getValue() );
				} );
			} else {
				for( String headerName : headers.children().keySet() ) {
					String headerAlias = headers.getFirstChild( headerName ).strValue();
					decodedMessage.value.getFirstChild( headerAlias )
						.setValue( message.getPropertyOrEmptyString( headerName.replace( "_", "-" ) ) );
				}
			}
		}
	}

	private static void recv_parseQueryString( HttpMessage message, Value value, String contentType,
		boolean strictEncoding )
		throws IOException {
		if( message.isGet() && contentType.equals( ContentTypes.APPLICATION_JSON ) ) {
			recv_parseJsonQueryString( message, value, strictEncoding );
		} else {
			Map< String, Integer > indexes = new HashMap<>();
			String queryString = message.requestPath();
			String[] kv = queryString.split( "\\?", 2 );
			Integer index;
			if( kv.length > 1 ) {
				queryString = kv[ 1 ];
				String[] params = queryString.split( "&" );
				for( String param : params ) {
					String[] ikv = param.split( "=", 2 );
					if( ikv.length > 1 ) {
						index = indexes.computeIfAbsent( ikv[ 0 ], k -> 0 );
						// the query string was already URL decoded by the HttpParser
						value.getChildren( ikv[ 0 ] ).get( index ).setValue( ikv[ 1 ] );
						indexes.put( ikv[ 0 ], index + 1 );
					}
				}
			}
		}
	}

	private static void recv_parseJsonQueryString( HttpMessage message, Value value, boolean strictEncoding )
		throws IOException {
		String queryString = message.requestPath();
		String[] kv = queryString.split( "\\?", 2 );
		if( kv.length > 1 ) {
			// the query string was already URL decoded by the HttpParser
			JsUtils.parseJsonIntoValue( new StringReader( kv[ 1 ] ), value, strictEncoding );
		}
	}

	/*
	 * Prints debug information about a received message
	 */
	private String getDebugMessage( HttpMessage message, String charset, boolean showContent )
		throws IOException {
		StringBuilder debugSB = new StringBuilder();
		debugSB.append( "\n[HTTP debug] Receiving:\n" ).append( getHttpHeader( message ) );
		if( showContent ) {
			debugSB.append( "--> Message content\n" )
				.append( getHttpBody( message, charset ) );
		}
		return debugSB.toString();
	}

	/*
	 * return the received message's header
	 */
	private static String getHttpHeader( HttpMessage message )
		throws IOException {
		StringBuilder headerStr = new StringBuilder();
		headerStr.append( "HTTP Code: " ).append( message.statusCode() )
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
	private static String getHttpBody( HttpMessage message, String charset )
		throws IOException {
		StringBuilder bodyStr = new StringBuilder();
		bodyStr.append( new String( message.content(), charset ) );
		return bodyStr.toString();
	}



	private void recv_parseRequestFormat( String type )
		throws IOException {
		responseFormat = null;

		if( "text/xml".equals( type ) ) {
			responseFormat = "xml";
		} else if( ContentTypes.APPLICATION_JSON.equals( type ) ) {
			responseFormat = "json";
		}
	}

	private void recv_parseMessage( HttpMessage message, DecodedMessage decodedMessage, String type, String charset )
		throws IOException {
		final String operationName = message.isResponse() ? inputId : decodedMessage.operationName;
		if( getOperationSpecificStringParameter( operationName, Parameters.FORCE_CONTENT_DECODING )
			.equals( NativeType.STRING.id() ) ) {
			decodedMessage.value.setValue( new String( message.content(), charset ) );
		} else if( getOperationSpecificStringParameter( operationName, Parameters.FORCE_CONTENT_DECODING )
			.equals( NativeType.RAW.id() ) ) {
			decodedMessage.value.setValue( new ByteArray( message.content() ) );
		} else if( "text/html".equals( type ) ) {
			decodedMessage.value.setValue( new String( message.content(), charset ) );
		} else if( "application/x-www-form-urlencoded".equals( type ) ) {
			parseForm( message, decodedMessage.value, charset );
		} else if( "text/xml".equals( type ) || type.contains( "xml" ) ) {
			parseXML( message, decodedMessage.value, charset );
		} else if( "multipart/form-data".equals( type ) ) {
			parseMultiPartFormData( message, decodedMessage.value );
		} else if( "application/octet-stream".equals( type ) || type.startsWith( "image/" )
			|| "application/zip".equals( type ) ) {
			decodedMessage.value.setValue( new ByteArray( message.content() ) );
		} else if( ContentTypes.APPLICATION_NDJSON.equals( type ) || type.contains( "ndjson" ) ) {
			boolean strictEncoding = checkStringParameter( Parameters.JSON_ENCODING, "strict" );
			parseNdJson( message, decodedMessage.value, strictEncoding, charset );
		} else if( ContentTypes.APPLICATION_JSON.equals( type ) || type.contains( "json" ) ) {
			boolean strictEncoding = checkStringParameter( Parameters.JSON_ENCODING, "strict" );
			parseJson( message, decodedMessage.value, strictEncoding, charset );
		} else {
			decodedMessage.value.setValue( new String( message.content(), charset ) );
		}
	}

	private String getDefaultOperation( HttpMessage.Type t ) {
		if( hasParameter( Parameters.DEFAULT_OPERATION ) ) {
			Value dParam = getParameterFirstValue( Parameters.DEFAULT_OPERATION );
			String method = HttpUtils.httpMessageTypeToString( t );
			if( method == null || dParam.hasChildren( method ) == false ) {
				return dParam.strValue();
			} else {
				return dParam.getFirstChild( method ).strValue();
			}
		}

		return null;
	}

	private void recv_checkReceivingOperation( HttpMessage message, DecodedMessage decodedMessage ) {
		if( decodedMessage.operationName == null ) {
			final String requestPath = message.requestPath().split( "\\?", 2 )[ 0 ].substring( 1 );
			if( requestPath.startsWith( LocationParser.RESOURCE_SEPARATOR ) ) {
				final String compositePath = requestPath.substring( LocationParser.RESOURCE_SEPARATOR.length() - 1 );
				final Matcher m = LocationParser.RESOURCE_SEPARATOR_PATTERN.matcher( compositePath );
				if( m.find() ) {
					decodedMessage.resourcePath = compositePath.substring( 0, m.start() );
					decodedMessage.operationName = compositePath.substring( m.end() );
				} else {
					decodedMessage.resourcePath = compositePath;
				}
			} else {
				decodedMessage.operationName = requestPath;
				decodedMessage.resourcePath = message.getProperty( Headers.JOLIE_RESOURCE_PATH );
				if( decodedMessage.resourcePath == null ) {
					decodedMessage.resourcePath = "/";
				}
			}
		}

	}

	private void recv_extractReceivingOperation( HttpMessage message, DecodedMessage decodedMessage ) {
		if( message.getMethod().isEmpty() )
			return;

		Value configurationValue = getParameterFirstValue( CommProtocol.Parameters.OPERATION_SPECIFIC_CONFIGURATION );
		Iterator< Entry< String, ValueVector > > configurationIterator = configurationValue.children().entrySet()
			.iterator();
		boolean foundMatch = false;
		while( configurationIterator.hasNext() & !foundMatch ) {
			Entry< String, ValueVector > configEntry = configurationIterator.next();

			Value opConfig = configEntry.getValue().get( 0 );
			Value uriTemplateResult = Value.create();
			String uri = message.requestPath().substring( 0, 2 ).equals( "//" ) // FIXME, TODO: strange jolie
				// double slash
				? message.requestPath().substring( 1 )
				: message.requestPath();
			if( opConfig.hasChildren( Parameters.TEMPLATE ) ) {
				uriTemplateResult = UriUtils.match( opConfig.getFirstChild( Parameters.TEMPLATE ).strValue(), uri );
			}
			String opConfigMethod = opConfig.getFirstChild( Parameters.METHOD ).strValue();

			if( uriTemplateResult.boolValue() && message.getMethod().equalsIgnoreCase( opConfigMethod ) ) {
				foundMatch = true;
				decodedMessage.operationName = configEntry.getKey();
				decodedMessage.resourcePath = "/";

				String messagePath = "/".concat( configEntry.getKey() );
				StringBuilder paramStringBuilder = new StringBuilder();
				Iterator< Entry< String, ValueVector > > uriTemplateIterator = uriTemplateResult.children().entrySet()
					.iterator();

				while( uriTemplateIterator.hasNext() ) {
					Entry< String, ValueVector > entry = uriTemplateIterator.next();

					if( paramStringBuilder.length() == 0 ) {
						paramStringBuilder.append( "?" );

					} else {
						paramStringBuilder.append( "&" );
					}

					paramStringBuilder.append( entry.getKey() )
						.append( "=" )
						.append( entry.getValue().get( 0 ).strValue() );
				}
				if( opConfig.hasChildren( Parameters.INCOMING_HEADERS ) ) {

					Iterator< Entry< String, ValueVector > > inHeadersIterator = opConfig
						.getFirstChild( Parameters.INCOMING_HEADERS ).children().entrySet().iterator();
					while( inHeadersIterator.hasNext() ) {
						Entry< String, ValueVector > entry = inHeadersIterator.next();
						if( paramStringBuilder.length() == 0 ) {
							paramStringBuilder.append( "?" ).append( entry.getValue().get( 0 ).strValue() )
								.append( "=" )
								.append( message.getProperty( entry.getKey() ) );
						} else {
							paramStringBuilder.append( "&" )
								.append( entry.getValue().get( 0 ).strValue() )
								.append( "=" )
								.append( message.getProperty( entry.getKey() ) );
						}

					}
				}
				messagePath += paramStringBuilder.toString();

				message.setRequestPath( messagePath );

			}
		}
	}

	private void recv_checkDefaultOp( HttpMessage message, DecodedMessage decodedMessage ) {
		if( decodedMessage.resourcePath.equals( "/" )
			&& !channel().parentInputPort().canHandleInputOperation( decodedMessage.operationName ) ) {
			String defaultOpId = getDefaultOperation( message.type() );
			if( defaultOpId != null ) {
				Value body = decodedMessage.value;
				decodedMessage.value = Value.create();
				decodedMessage.value.getChildren( "data" ).add( body );
				decodedMessage.value.getFirstChild( "operation" ).setValue( decodedMessage.operationName );
				decodedMessage.value.setFirstChild( "requestUri", message.requestPath() );
				if( message.userAgent() != null ) {
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

	private void recv_checkForMultiPartHeaders( DecodedMessage decodedMessage ) {
		if( multiPartFormDataParser != null ) {
			String target;
			for( Entry< String, MultiPartFormDataParser.PartProperties > entry : multiPartFormDataParser
				.getPartPropertiesSet() ) {
				if( entry.getValue().filename() != null ) {
					target = getMultipartHeaderForPart( decodedMessage.operationName, entry.getKey() );
					if( target != null ) {
						decodedMessage.value.getFirstChild( target ).setValue( entry.getValue().filename() );
					}
				}
			}
			multiPartFormDataParser = null;
		}
	}

	private void recv_checkForMessageProperties( HttpMessage message, DecodedMessage decodedMessage )
		throws IOException {
		recv_checkForCookies( message, decodedMessage );
		recv_checkForGenericHeader( message, decodedMessage );
		recv_checkForMultiPartHeaders( decodedMessage );
		if( message.userAgent() != null &&
			hasParameter( Parameters.USER_AGENT ) ) {
			getParameterFirstValue( Parameters.USER_AGENT ).setValue( message.userAgent() );
		}

		if( getParameterVector( Parameters.HOST ) != null ) {
			getParameterFirstValue( Parameters.HOST ).setValue( message.getPropertyOrEmptyString( Parameters.HOST ) );
		}
	}

	private static class DecodedMessage {
		private String operationName = null;
		private Value value = Value.create();
		private String resourcePath = "/";
		private long id = CommMessage.GENERIC_REQUEST_ID;
	}

	private void recv_checkForStatusCode( HttpMessage message ) {
		if( hasParameter( Parameters.STATUS_CODE ) ) {
			getParameterFirstValue( Parameters.STATUS_CODE ).setValue( message.statusCode() );
		}
	}

	@Override
	public CommMessage recv_internal( InputStream istream, OutputStream ostream )
		throws IOException {
		HttpMessage message = new HttpParser( istream ).parse();
		String charset = HttpUtils.getCharset( null, message );
		CommMessage retVal = null;
		DecodedMessage decodedMessage = new DecodedMessage();

		HttpUtils.recv_checkForChannelClosing( message, channel() );

		if( checkBooleanParameter( Parameters.DEBUG ) ) {
			boolean showContent = false;
			if( getParameterFirstValue( Parameters.DEBUG ).getFirstChild( "showContent" ).intValue() > 0
				&& message.size() > 0 ) {
				showContent = true;
			}
			Interpreter.getInstance().logInfo( getDebugMessage( message, charset, showContent ) );
		}

		// tracer
		Interpreter.getInstance().tracer().trace( () -> {
			try {
				final String traceMessage = getDebugMessage( message, charset, message.size() > 0 );
				return new ProtocolTraceAction( ProtocolTraceAction.Type.HTTP, "HTTP MESSAGE RECEIVED",
					message.requestPath(), traceMessage, null );
			} catch( IOException e ) {
				return new ProtocolTraceAction( ProtocolTraceAction.Type.HTTP, "HTTP MESSAGE RECEIVED",
					message.requestPath(), e.getMessage(), null );

			}

		} );

		recv_checkForStatusCode( message );

		encoding = message.getProperty( "accept-encoding" );
		headRequest = inInputPort && message.isHead();

		String contentType = DEFAULT_CONTENT_TYPE;
		if( message.getProperty( "content-type" ) != null ) {
			contentType = message.getProperty( "content-type" ).split( ";", 2 )[ 0 ].toLowerCase();
		}


		recv_parseRequestFormat( contentType );
		if( !message.isResponse() ) {
			if( hasParameter( CommProtocol.Parameters.OPERATION_SPECIFIC_CONFIGURATION ) ) {
				recv_extractReceivingOperation( message, decodedMessage );
			}
			recv_checkReceivingOperation( message, decodedMessage );
		}


		// URI parameter parsing
		if( message.requestPath() != null ) {
			boolean strictEncoding = checkStringParameter( Parameters.JSON_ENCODING, "strict" );
			recv_parseQueryString( message, decodedMessage.value, contentType, strictEncoding );
		}

		/* https://tools.ietf.org/html/rfc7231#section-4.3 */
		if( !message.isGet() && !message.isHead() ) {
			// body parsing
			if( message.size() > 0 ) {
				recv_parseMessage( message, decodedMessage, contentType, charset );
			}
		}

		if( !message.isResponse() ) {
			recv_checkDefaultOp( message, decodedMessage );
		}

		if( checkBooleanParameter( Parameters.CONCURRENT ) ) {
			String messageId = message.getProperty( Headers.JOLIE_MESSAGE_ID );
			if( messageId != null ) {
				try {
					decodedMessage.id = Long.parseLong( messageId );
				} catch( NumberFormatException e ) {
				}
			}
		}

		if( message.isResponse() ) {
			FaultException faultException = null;
			if( hasOperationSpecificParameter( inputId, Parameters.STATUS_CODES ) ) {
				faultException = recv_mapHttpStatusCodeFault( message,
					getOperationSpecificParameterFirstValue( inputId, Parameters.STATUS_CODES ),
					decodedMessage.value );
			}
			String responseHeader = "";
			if( hasParameter( Parameters.RESPONSE_HEADER )
				|| hasOperationSpecificParameter( inputId, Parameters.RESPONSE_HEADER ) ) {
				if( hasOperationSpecificParameter( inputId, Parameters.RESPONSE_HEADER ) ) {
					responseHeader = getOperationSpecificStringParameter( inputId, Parameters.RESPONSE_HEADER );
				} else {
					responseHeader = getStringParameter( Parameters.RESPONSE_HEADER );
				}
				for( Entry< String, String > param : message.properties() ) {
					decodedMessage.value.getFirstChild( responseHeader ).getFirstChild( param.getKey() )
						.setValue( param.getValue() );
				}
				decodedMessage.value.getFirstChild( responseHeader ).getFirstChild( Parameters.STATUS_CODE )
					.setValue( message.statusCode() );
			}

			recv_checkForSetCookie( message, decodedMessage.value );
			retVal =
				new CommMessage( decodedMessage.id, inputId, decodedMessage.resourcePath, decodedMessage.value,
					faultException );
		} else if( message.isError() == false ) {
			recv_checkForMessageProperties( message, decodedMessage );
			retVal = new CommMessage( decodedMessage.id, decodedMessage.operationName, decodedMessage.resourcePath,
				decodedMessage.value, null );
		}

		if( Interpreter.getInstance().isMonitoring() ) {
			Interpreter.getInstance().fireMonitorEvent(
				new ProtocolMessageEvent(
					getHttpBody( message, charset ),
					getHttpHeader( message ),
					"",
					Long.toString( retVal.id() ),
					ProtocolMessageEvent.Protocol.HTTP ) );
		}

		if( retVal != null && "/".equals( retVal.resourcePath() ) && channel().parentPort() != null
			&& (channel().parentPort().getInterface().containsOperation( retVal.operationName() )
				|| (channel().parentInputPort() != null
					&& channel().parentInputPort().getAggregatedOperation( retVal.operationName() ) != null)) ) {
			try {
				// The message is for this service
				boolean hasInput = false;
				OneWayTypeDescription oneWayTypeDescription = null;
				if( channel().parentInputPort() != null ) {
					if( channel().parentInputPort().getAggregatedOperation( retVal.operationName() ) != null ) {
						oneWayTypeDescription =
							channel().parentInputPort().getAggregatedOperation( retVal.operationName() )
								.getOperationTypeDescription().asOneWayTypeDescription();
						hasInput = true;
					}
				}
				if( !hasInput ) {
					Interface iface = channel().parentPort().getInterface();
					oneWayTypeDescription = iface.oneWayOperations().get( retVal.operationName() );
				}

				if( oneWayTypeDescription != null ) {
					// We are receiving a One-Way message
					oneWayTypeDescription.requestType().cast( retVal.value() );
				} else {
					hasInput = false;
					RequestResponseTypeDescription rrTypeDescription = null;
					if( channel().parentInputPort() != null ) {
						if( channel().parentInputPort().getAggregatedOperation( retVal.operationName() ) != null ) {
							rrTypeDescription =
								channel().parentInputPort().getAggregatedOperation( retVal.operationName() )
									.getOperationTypeDescription().asRequestResponseTypeDescription();
							hasInput = true;
						}
					}

					if( !hasInput ) {
						Interface iface = channel().parentPort().getInterface();
						rrTypeDescription = iface.requestResponseOperations().get( retVal.operationName() );
					}

					if( retVal.isFault() ) {
						Type faultType = rrTypeDescription.faults().get( retVal.fault().faultName() );
						if( faultType != null ) {
							faultType.cast( retVal.value() );
						}
					} else {
						if( message.isResponse() ) {
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

	private FaultException recv_mapHttpStatusCodeFault( HttpMessage message, Value httpStatusValue,
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


	@Override
	public CommMessage recv( InputStream istream, OutputStream ostream )
		throws IOException {
		return HttpUtils.recv( istream, ostream, inInputPort, channel(), this );
	}

	private Type getSendType( CommMessage message )
		throws IOException {
		Type ret = null;

		if( channel().parentPort() == null ) {
			throw new IOException( "Could not retrieve communication port for HTTP protocol" );
		}

		OperationTypeDescription opDesc =
			channel().parentPort().getOperationTypeDescription( message.operationName(), Constants.ROOT_RESOURCE_PATH );

		if( opDesc == null ) {
			return null;
		}

		if( opDesc.asOneWayTypeDescription() != null ) {
			if( message.isFault() ) {
				ret = Type.UNDEFINED;
			} else {
				OneWayTypeDescription ow = opDesc.asOneWayTypeDescription();
				ret = ow.requestType();
			}
		} else if( opDesc.asRequestResponseTypeDescription() != null ) {
			RequestResponseTypeDescription rr = opDesc.asRequestResponseTypeDescription();
			if( message.isFault() ) {
				ret = rr.getFaultType( message.fault().faultName() );
				if( ret == null ) {
					ret = Type.UNDEFINED;
				}
			} else {
				ret = (inInputPort) ? rr.responseType() : rr.requestType();
			}
		}

		return ret;
	}
}
