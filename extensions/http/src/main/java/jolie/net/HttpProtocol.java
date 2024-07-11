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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import jolie.ExecutionThread;
import jolie.Interpreter;
import jolie.js.JsUtils;
import jolie.lang.Constants;
import jolie.lang.NativeType;
import jolie.monitoring.events.ProtocolMessageEvent;
import jolie.net.constants.HttpProtocolConstants;
import jolie.net.http.HttpMessage;
import jolie.net.http.HttpParser;
import jolie.net.http.HttpUtils;
import jolie.net.http.HttpUtils.ContentTypes;
import jolie.net.http.HttpUtils.Formats;
import jolie.net.http.Method;
import jolie.net.http.MultiPartFormDataParser;
import jolie.net.http.UnsupportedMethodException;
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
import jolie.util.metadata.MetadataKey;
import jolie.xml.XmlUtils;

/**
 * HTTP protocol implementation
 *
 * @author Fabrizio Montesi 14 Nov 2012 - Saverio Giallorenzo - Fabrizio Montesi: support for status
 *         codes
 */
public class HttpProtocol extends CommProtocol implements HttpUtils.Protocol {
	private static class ExchangeContext {
		private MultiPartFormDataParser multiPartFormDataParser = null;
		private String encoding = null;
		private String requestFormat = null;
		private String requestCharset = null;
		private boolean headRequest = false;
	}

	private static final MetadataKey< ExchangeContext > HTTP_METADATA_KEY =
		MetadataKey.of( "http", ExchangeContext.class );

	private final URI uri;
	private final boolean inInputPort;

	private final Transformer transformer;
	private final DocumentBuilder docBuilder;

	private String inputId = null;

	@Override
	public String name() {
		return "http";
	}

	@Override
	public boolean isThreadSafe() {
		return checkBooleanParameter( HttpUtils.Parameters.CONCURRENT );
	}

	public HttpProtocol(
		VariablePath configurationPath,
		URI uri,
		boolean inInputPort,
		TransformerFactory transformerFactory,
		DocumentBuilder docBuilder )
		throws TransformerConfigurationException {
		super( configurationPath );
		this.uri = uri;
		this.inInputPort = inInputPort;
		this.transformer = transformerFactory.newTransformer();
		this.docBuilder = docBuilder;

		transformer.setOutputProperty( OutputKeys.OMIT_XML_DECLARATION, "yes" );
		transformer.setOutputProperty( OutputKeys.INDENT, "no" );
	}

	public String getMultipartHeaderForPart( String operationName, String partName ) {
		if( hasOperationSpecificParameter( operationName, HttpUtils.Parameters.MULTIPART_HEADERS ) ) {
			Value v = getOperationSpecificParameterFirstValue( operationName, HttpUtils.Parameters.MULTIPART_HEADERS );
			if( v.hasChildren( partName ) ) {
				v = v.getFirstChild( partName );
				if( v.hasChildren( HttpUtils.Parameters.MultiPartHeaders.FILENAME ) ) {
					v = v.getFirstChild( HttpUtils.Parameters.MultiPartHeaders.FILENAME );
					return v.strValue();
				}
			}
		}
		return null;
	}

	private void send_appendCookies( CommMessage message, String hostname, StringBuilder headerBuilder ) {
		Value cookieParam = null;
		if( hasOperationSpecificParameter( message.operationName(), HttpUtils.Parameters.COOKIES ) ) {
			cookieParam =
				getOperationSpecificParameterFirstValue( message.operationName(), HttpUtils.Parameters.COOKIES );
		} else if( hasParameter( HttpUtils.Parameters.COOKIES ) ) {
			cookieParam = getParameterFirstValue( HttpUtils.Parameters.COOKIES );
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
		if( hasOperationSpecificParameter( message.operationName(), HttpUtils.Parameters.COOKIES ) ) {
			cookieParam =
				getOperationSpecificParameterFirstValue( message.operationName(), HttpUtils.Parameters.COOKIES );
		} else if( hasParameter( HttpUtils.Parameters.COOKIES ) ) {
			cookieParam = getParameterFirstValue( HttpUtils.Parameters.COOKIES );
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

	private static ExchangeContext getHttpMetadata( CommMessage message ) {
		ExchangeContext context = message.metadata().get( HTTP_METADATA_KEY );
		if( context == null ) {
			context = new ExchangeContext();
			message.metadata().put( HTTP_METADATA_KEY, context );
		}
		return context;
	}

	private String send_getFormat( CommMessage message ) {
		String operationName = message.operationName();
		String format = null;
		if( hasOperationSpecificParameter( operationName, HttpUtils.Parameters.FORMAT ) ) {
			format = getOperationSpecificStringParameter( operationName, HttpUtils.Parameters.FORMAT );
		} else if( hasParameter( HttpUtils.Parameters.FORMAT ) ) {
			format = getStringParameter( HttpUtils.Parameters.FORMAT );
		} else if( inInputPort && message.originalRequest().isPresent() ) {
			// if we are on the server side and have no "format" parameter specified we apply content type
			// negotiation according to RFC9110 section 12.5ff
			format = getHttpMetadata( message.originalRequest().get() ).requestFormat;
		}
		if( format == null || format.isEmpty() )
			format = HttpUtils.DEFAULT_FORMAT; // fall back to the default
		return format;
	}

	private String send_getCharset( CommMessage message ) {
		String operationName = message.operationName();
		String charset = null;
		if( hasOperationSpecificParameter( operationName, HttpUtils.Parameters.CHARSET ) ) {
			charset = getOperationSpecificStringParameter( operationName, HttpUtils.Parameters.CHARSET );
		} else if( hasParameter( HttpUtils.Parameters.CHARSET ) ) {
			charset = getStringParameter( HttpUtils.Parameters.CHARSET );
		} else if( inInputPort && message.originalRequest().isPresent() ) {
			// if we are on the server side and have no "charset" parameter specified we apply content type
			// negotiation according to RFC9110 section 12.5ff
			charset = getHttpMetadata( message.originalRequest().get() ).requestCharset;
		}
		if( charset == null || charset.isEmpty() )
			charset = HttpUtils.DEFAULT_CONTENT_CHARSET; // fall back to the default
		return charset;
	}

	private HttpUtils.EncodedContent send_encodeContent( CommMessage message, Method method, String charset,
		String format,
		Type sendType )
		throws IOException {
		HttpUtils.EncodedContent ret = new HttpUtils.EncodedContent();

		if( !inInputPort && method == Method.GET
			|| inInputPort && sendType.isVoid() && !message.isFault() ) {
			// no payload if we are building a GET request (client) or an one-way respectively
			// void-typed non-fault response (server)
			return ret;
		}

		if( Formats.XML.equals( format ) ) {
			ret.contentType = ContentTypes.TEXT_XML;
			Document doc = docBuilder.newDocument();
			Element root = null;
			try {
				root = doc.createElement( message.operationName() + ((inInputPort) ? "Response" : "") );
			} catch( DOMException ex ) { // invalid operation (the operation name just contains the URI path)
				root = doc.createElement( inInputPort ? "Response" : "Request" );
			}
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
		} else if( Formats.BINARY.equals( format ) ) {
			ret.contentType = ContentTypes.APPLICATION_OCTET_STREAM;
			ret.content = message.value().byteArrayValue();
		} else if( Formats.HTML.equals( format ) ) {
			ret.contentType = ContentTypes.TEXT_HTML;
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
		} else if( Formats.FORM_DATA.equals( format ) ) {
			ret.contentType = String.format( "%s; boundary=%s", ContentTypes.MULTIPART_FORM_DATA, HttpUtils.BOUNDARY );
			ByteArrayOutputStream bStream = new ByteArrayOutputStream();
			StringBuilder builder = new StringBuilder();
			for( Entry< String, ValueVector > entry : message.value().children().entrySet() ) {
				if( !entry.getKey().startsWith( "@" ) ) {
					builder.append( "--" ).append( HttpUtils.BOUNDARY ).append( HttpUtils.CRLF )
						.append( "Content-Disposition: form-data; name=\"" ).append( entry.getKey() ).append( '\"' );
					boolean isBinary = false;
					if( hasOperationSpecificParameter( message.operationName(),
						HttpUtils.Parameters.MULTIPART_HEADERS ) ) {
						Value specOpParam = getOperationSpecificParameterFirstValue( message.operationName(),
							HttpUtils.Parameters.MULTIPART_HEADERS );
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
			builder.append( "--" + HttpUtils.BOUNDARY + "--" );
			bStream.write( builder.toString().getBytes( charset ) );
			ret.content = new ByteArray( bStream.toByteArray() );
		} else if( Formats.FORM_URLENCODED.equals( format ) ) {
			ret.contentType = ContentTypes.APPLICATION_X_WWW_FORM_URLENCODED;
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
		} else if( Formats.JSON.equals( format ) ) {
			ret.contentType = HttpUtils.ContentTypes.APPLICATION_JSON;
			StringBuilder jsonStringBuilder = new StringBuilder();
			if( message.isFault() ) {
				Value error = message.value().getFirstChild( "error" );
				error.getFirstChild( "code" ).setValue( -32000 );
				error.getFirstChild( "message" ).setValue( message.fault().faultName() );
				error.getChildren( "data" ).set( 0, message.fault().value() );
				JsUtils.faultValueToJsonString( message.value(), sendType, jsonStringBuilder );
			} else {
				JsUtils.valueToJsonString( message.value(), true, sendType, jsonStringBuilder );
			}
			ret.content = new ByteArray( jsonStringBuilder.toString().getBytes( charset ) );
		} else if( Formats.NDJSON.equals( format ) ) {
			ret.contentType = HttpUtils.ContentTypes.APPLICATION_NDJSON;
			StringBuilder ndJsonStringBuilder = new StringBuilder();
			if( message.isFault() ) {
				Value error = message.value().getFirstChild( "error" );
				error.getFirstChild( "code" ).setValue( -32000 );
				error.getFirstChild( "message" ).setValue( message.fault().faultName() );
				error.getChildren( "data" ).set( 0, message.fault().value() );
				JsUtils.faultValueToJsonString( message.value(), sendType, ndJsonStringBuilder );
			} else {
				JsUtils.valueToNdJsonString( message.value(), true, sendType, ndJsonStringBuilder );
			}
			ret.content = new ByteArray( ndJsonStringBuilder.toString().getBytes( charset ) );
		} else if( Formats.RAW.equals( format ) ) {
			ret.contentType = HttpUtils.ContentTypes.TEXT_PLAIN;
			if( message.isFault() ) {
				ret.content = new ByteArray( message.fault().value().strValue().getBytes( charset ) );
			} else {
				ret.content = new ByteArray( message.value().strValue().getBytes( charset ) );
			}
		} else {
			throw new IOException( String.format( "Unrecognized transport format '%s'.", format ) );
		}
		return ret;
	}

	private void send_appendResponseUserHeader( CommMessage message, StringBuilder headerBuilder ) {
		Value responseHeaderParameters;
		if( hasOperationSpecificParameter( message.operationName(), HttpUtils.Parameters.RESPONSE_USER ) ) {
			responseHeaderParameters =
				getOperationSpecificParameterFirstValue( message.operationName(), HttpUtils.Parameters.RESPONSE_USER );
			if( (responseHeaderParameters != null)
				&& (responseHeaderParameters.hasChildren( HttpUtils.Parameters.HEADER_USER )) ) {
				for( Entry< String, ValueVector > entry : responseHeaderParameters
					.getFirstChild( HttpUtils.Parameters.HEADER_USER ).children().entrySet() )
					for( int counter = 0; counter < entry.getValue().size(); counter++ ) {
						headerBuilder.append( entry.getKey() ).append( ": " )
							.append( entry.getValue().get( counter ).strValue() )
							.append( HttpUtils.CRLF );
					}

			}
		}

		responseHeaderParameters = null;
		if( hasParameter( HttpUtils.Parameters.RESPONSE_USER ) ) {
			responseHeaderParameters = getParameterFirstValue( HttpUtils.Parameters.RESPONSE_USER );

			if( (responseHeaderParameters != null)
				&& (responseHeaderParameters.hasChildren( HttpUtils.Parameters.HEADER_USER )) ) {
				for( Entry< String, ValueVector > entry : responseHeaderParameters
					.getFirstChild( HttpUtils.Parameters.HEADER_USER ).children().entrySet() )
					for( int counter = 0; counter < entry.getValue().size(); counter++ ) {
						headerBuilder.append( entry.getKey() ).append( ": " )
							.append( entry.getValue().get( counter ).strValue() )
							.append( HttpUtils.CRLF );
					}
			}
		}
	}

	private void send_appendResponseHeaders( CommMessage message, Type sendType, StringBuilder headerBuilder )
		throws IOException {
		int statusCode = !message.isFault() // HTTP 200 OK on success and HTTP 500 Internal Server Error on faults
			? HttpUtils.DEFAULT_STATUS_CODE
			: HttpUtils.DEFAULT_FAULT_STATUS_CODE;

		if( hasOperationSpecificParameter( message.operationName(), HttpUtils.Parameters.STATUS_CODES ) ) {
			Value statusCodesValue = getOperationSpecificParameterFirstValue( message.operationName(),
				HttpUtils.Parameters.STATUS_CODES );
			if( message.isFault() && statusCodesValue.hasChildren( message.fault().faultName() ) ) {
				statusCode = statusCodesValue.getFirstChild( message.fault().faultName() ).intValue(); // exception
																										// status code
			} else if( !message.isFault() && statusCodesValue.isInt() ) {
				statusCode = statusCodesValue.intValue(); // custom success status code
			}
		} else if( hasParameter( HttpUtils.Parameters.STATUS_CODE ) ) {
			statusCode = getIntParameter( HttpUtils.Parameters.STATUS_CODE );
		} else if( hasParameter( HttpUtils.Parameters.REDIRECT ) ) {
			statusCode = HttpUtils.DEFAULT_REDIRECTION_STATUS_CODE;
		}

		if( !HttpUtils.isStatusCodeValid( statusCode ) ) {
			throw new IOException( String.format(
				"HTTP protocol for operation %s is sending a message with status code %d, which is not in the HTTP specifications.",
				message.operationName(), statusCode ) );
		} else if( HttpUtils.isLocationNeeded( statusCode ) && !hasParameter( HttpUtils.Parameters.REDIRECT ) ) {
			// if statusCode is a redirection code, location parameter is needed
			throw new IOException( String.format(
				"HTTP protocol for operation %s is sending a message with status code %d, which expects a redirect parameter but the latter is not set.",
				message.operationName(), statusCode ) );
		}

		// https://datatracker.ietf.org/doc/html/rfc9110#name-200-ok
		// we should not be sending a HTTP status code 200 with a null/void content (not empty one)
		if( statusCode == 200 && sendType.isVoid() )
			statusCode = 204;

		headerBuilder.append( "HTTP/1.1 " ).append( HttpUtils.getStatusCodeDescription( statusCode ) )
			.append( HttpUtils.CRLF );

		// if redirect has been set, the redirect location parameter is set
		if( hasParameter( HttpUtils.Parameters.REDIRECT ) ) {
			headerBuilder.append( "Location: " ).append( getStringParameter( HttpUtils.Parameters.REDIRECT ) )
				.append( HttpUtils.CRLF );
		}

		send_appendSetCookieHeader( message, headerBuilder );
		headerBuilder.append( "Server: Jolie" ).append( HttpUtils.CRLF );
		StringBuilder cacheControlHeader = new StringBuilder();
		if( hasParameter( HttpUtils.Parameters.CACHE_CONTROL ) ) {
			Value cacheControl = getParameterFirstValue( HttpUtils.Parameters.CACHE_CONTROL );
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

	private void send_appendRequestPath( CommMessage message, Method method, String qsFormat, Type sendType,
		StringBuilder headerBuilder )
		throws IOException {
		String path = uri.getRawPath();
		if( uri.getScheme().equals( "localsocket" ) || path == null || path.isEmpty()
			|| checkBooleanParameter( HttpUtils.Parameters.DROP_URI_PATH, false ) ) {
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

		if( hasOperationSpecificParameter( message.operationName(), HttpUtils.Parameters.ALIAS ) ) {
			String alias = getOperationSpecificStringParameter( message.operationName(), HttpUtils.Parameters.ALIAS );
			send_appendParsedAlias( alias, message.value(), headerBuilder );
		} else if( hasOperationSpecificParameter( message.operationName(), HttpUtils.Parameters.TEMPLATE ) ) {
			String template =
				getOperationSpecificStringParameter( message.operationName(), HttpUtils.Parameters.TEMPLATE );
			send_appendParsedTemplate( template, message.value(), headerBuilder );
		} else {
			headerBuilder.append( message.operationName() );
		}

		if( method == Method.GET ) {
			// we need to drop header parameters from the query value
			Value queryValue = message.value().clone();
			getOperationSpecificParameterFirstValue( message.operationName(),
				HttpUtils.Parameters.OUTGOING_HEADERS ).children().forEach(
					( headerName, headerValues ) -> queryValue.children().remove( headerValues.get( 0 ).strValue() ) );

			if( qsFormat.equals( "json" ) ) {
				HttpUtils.send_appendJsonQueryString( queryValue, sendType, headerBuilder );
			} else {
				HttpUtils.send_appendQuerystring( queryValue, headerBuilder );
			}
		}
	}

	private void send_appendParsedTemplate( String template, Value value, StringBuilder headerBuilder )
		throws MalformedURLException {
		List< String > templateKeys = new ArrayList<>();
		Map< String, Object > params = new HashMap<>();

		for( final Map.Entry< String, ValueVector > entry : value.children()
			.entrySet() ) {
			params.put( entry.getKey(), entry.getValue().first().valueObject() );
		}

		String uri = UriUtils.expand( template, params );

		/* cleaning value from used keys */


		UriUtils.match( template, uri ).children().forEach( ( s, values ) -> {
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
		if( hasOperationSpecificParameter( message.operationName(), HttpUtils.Parameters.REQUEST_USER ) ) {
			responseHeaderParameters =
				getOperationSpecificParameterFirstValue( message.operationName(), HttpUtils.Parameters.RESPONSE_USER );
			if( (responseHeaderParameters != null)
				&& (responseHeaderParameters.hasChildren( HttpUtils.Parameters.HEADER_USER )) ) {
				for( Entry< String, ValueVector > entry : responseHeaderParameters
					.getFirstChild( HttpUtils.Parameters.HEADER_USER ).children().entrySet() )
					headerBuilder.append( entry.getKey() ).append( ": " ).append( entry.getValue().first().strValue() )
						.append( HttpUtils.CRLF );
			}
		}

		responseHeaderParameters = null;
		if( hasParameter( HttpUtils.Parameters.RESPONSE_USER ) ) {
			responseHeaderParameters = getParameterFirstValue( HttpUtils.Parameters.REQUEST_USER );
			if( (responseHeaderParameters != null)
				&& (responseHeaderParameters.hasChildren( HttpUtils.Parameters.HEADER_USER )) ) {
				for( Entry< String, ValueVector > entry : responseHeaderParameters
					.getFirstChild( HttpUtils.Parameters.HEADER_USER ).children().entrySet() )
					headerBuilder.append( entry.getKey() ).append( ": " ).append( entry.getValue().first().strValue() )
						.append( HttpUtils.CRLF );
			}
		}
	}

	private void send_appendHeader( StringBuilder headerBuilder ) {
		if( hasParameter( HttpUtils.Parameters.ADD_HEADERS ) ) {
			Value v = getParameterFirstValue( HttpUtils.Parameters.ADD_HEADERS );
			if( v.hasChildren( "header" ) ) {
				for( Value head : v.getChildren( "header" ) ) {
					String header = head.strValue() + ": "
						+ head.getFirstChild( "value" ).strValue();
					headerBuilder.append( header ).append( HttpUtils.CRLF );
				}
			}
		}
		if( !inInputPort && hasParameter( HttpUtils.Parameters.REQUEST_HEADERS ) ) {
			getParameterFirstValue( HttpUtils.Parameters.REQUEST_HEADERS )
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
		return hasOperationSpecificParameter( message.operationName(), HttpUtils.Parameters.METHOD )
			? Method.fromString(
				getOperationSpecificStringParameter( message.operationName(), HttpUtils.Parameters.METHOD ) )
			: hasParameterValue( HttpUtils.Parameters.METHOD )
				? Method.fromString( getStringParameter( HttpUtils.Parameters.METHOD ) )
				: Method.POST;
	}

	private void send_appendRequestHeaders( CommMessage message, Method method, String qsFormat, Type sendType,
		StringBuilder headerBuilder )
		throws IOException {
		send_appendRequestMethod( method, headerBuilder );
		headerBuilder.append( ' ' );
		send_appendRequestPath( message, method, qsFormat, sendType, headerBuilder );
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
		if( checkBooleanParameter( HttpUtils.Parameters.COMPRESSION, true ) ) {
			String requestCompression = getStringParameter( HttpUtils.Parameters.REQUEST_COMPRESSION );
			if( requestCompression.equals( "gzip" ) || requestCompression.equals( "deflate" ) ) {
				final ExchangeContext messageMetadata = getHttpMetadata( message );
				messageMetadata.encoding = requestCompression;
				headerBuilder.append( "Accept-Encoding: " ).append( messageMetadata.encoding ).append( HttpUtils.CRLF );
			} else {
				headerBuilder.append( "Accept-Encoding: gzip, deflate" ).append( HttpUtils.CRLF );
			}
		}
		send_appendHeader( headerBuilder );

		if( hasOperationSpecificParameter( message.operationName(), HttpUtils.Parameters.OUTGOING_HEADERS ) ) {
			send_operationSpecificHeader( message.value(),
				getOperationSpecificParameterFirstValue( message.operationName(),
					HttpUtils.Parameters.OUTGOING_HEADERS ),
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
		HttpUtils.EncodedContent encodedContent,
		String charset,
		String requestEncoding,
		StringBuilder headerBuilder )
		throws IOException {
		if( !checkBooleanParameter( HttpUtils.Parameters.KEEP_ALIVE, true ) ) {
			if( inInputPort ) // we may do this only in input (server) mode
				channel().setToBeClosed( true );
			headerBuilder.append( "Connection: close" ).append( HttpUtils.CRLF );
		}
		if( checkBooleanParameter( HttpUtils.Parameters.CONCURRENT ) ) {
			headerBuilder.append( HttpUtils.Headers.JOLIE_MESSAGE_ID ).append( ": " ).append( message.requestId() )
				.append( HttpUtils.CRLF );
		} else {
			// Remember the operation name for later
			inputId = message.operationName();
		}

		headerBuilder.append( HttpUtils.Headers.JOLIE_OPERATION ).append( ": " )
			.append( message.operationName() ).append( HttpUtils.CRLF )
			.append( HttpUtils.Headers.JOLIE_RESOURCE_PATH ).append( ": " ).append( message.resourcePath() )
			.append( HttpUtils.CRLF );

		String contentType = getStringParameter( HttpUtils.Parameters.CONTENT_TYPE );
		if( !contentType.isEmpty() ) {
			encodedContent.contentType = contentType;
		}
		encodedContent.contentType = encodedContent.contentType.toLowerCase();

		headerBuilder.append( "Content-Type: " ).append( encodedContent.contentType );
		if( charset != null
			&& (encodedContent.contentType.startsWith( "text/" )
				|| (HttpProtocolConstants.shouldHaveCharset( encodedContent.contentType ))) ) {
			headerBuilder.append( "; charset=" ).append( charset.toLowerCase() );
		}
		headerBuilder.append( HttpUtils.CRLF );

		if( encodedContent.content != null ) {
			String transferEncoding = getStringParameter( HttpUtils.Parameters.CONTENT_TRANSFER_ENCODING );
			if( !transferEncoding.isEmpty() ) {
				headerBuilder.append( "Content-Transfer-Encoding: " ).append( transferEncoding )
					.append( HttpUtils.CRLF );
			}

			String contentDisposition = getStringParameter( HttpUtils.Parameters.CONTENT_DISPOSITION );
			if( !contentDisposition.isEmpty() ) {
				encodedContent.contentDisposition = contentDisposition;
				headerBuilder.append( "Content-Disposition: " ).append( encodedContent.contentDisposition )
					.append( HttpUtils.CRLF );
			}

			boolean compression =
				requestEncoding != null && checkBooleanParameter( HttpUtils.Parameters.COMPRESSION, true );
			String compressionTypes = getStringParameter(
				HttpUtils.Parameters.COMPRESSION_TYPES,
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
				encodedContent.content =
					HttpUtils.encode( requestEncoding, encodedContent.content, headerBuilder );
			}

			headerBuilder.append( "Content-Length: " ).append( encodedContent.content.size() ).append( HttpUtils.CRLF );
			// https://datatracker.ietf.org/doc/html/rfc9110#name-content-length
		} else if( !inInputPort ) {
			// a server response with no payload should not send back a zero-size content length header,
			// contrarily to client requests where this behaviour is perfectly fine
			headerBuilder.append( "Content-Length: 0" ).append( HttpUtils.CRLF );
		}
	}

	private void send_logDebugInfo( CharSequence header, HttpUtils.EncodedContent encodedContent, String charset )
		throws IOException {
		if( checkBooleanParameter( HttpUtils.Parameters.DEBUG ) ) {
			boolean showContent = false;
			if( getParameterVector( HttpUtils.Parameters.DEBUG ).first().getFirstChild( "showContent" ).intValue() > 0
				&& encodedContent.content != null ) {
				showContent = true;
			}
			Interpreter.getInstance()
				.logInfo( HttpUtils.prepareSendDebugString( header, encodedContent, charset, showContent ) );
		}
	}

	@Override
	public void send_internal( OutputStream ostream, CommMessage message, InputStream istream )
		throws IOException {
		Method method = send_getRequestMethod( message );
		String charset = send_getCharset( message );
		String format = send_getFormat( message );
		String contentType = null;
		Type sendType = getSendType( message );
		boolean headRequestResponse = false;
		String requestEncoding = null;

		StringBuilder headerBuilder = new StringBuilder();

		if( inInputPort ) {
			// We're responding to a request
			send_appendResponseHeaders( message, sendType, headerBuilder );
			send_appendResponseUserHeader( message, headerBuilder );
			send_appendHeader( headerBuilder );

			if( message.originalRequest().isPresent() ) {
				final ExchangeContext requestMessageMetadata = getHttpMetadata( message.originalRequest().get() );
				headRequestResponse = requestMessageMetadata.headRequest;
				requestEncoding = requestMessageMetadata.encoding;
			}
		} else {
			// We're sending a notification or a solicit
			String qsFormat = "";
			if( method == Method.GET
				&& getParameterFirstValue( HttpUtils.Parameters.METHOD ).hasChildren( "queryFormat" ) ) {
				if( getParameterFirstValue( HttpUtils.Parameters.METHOD ).getFirstChild( "queryFormat" ).strValue()
					.equals( "json" ) ) {
					qsFormat = format = "json";
					contentType = HttpUtils.ContentTypes.APPLICATION_JSON;
				}
			}
			send_appendRequestUserHeader( message, headerBuilder );
			send_appendRequestHeaders( message, method, qsFormat, sendType, headerBuilder );
		}

		HttpUtils.EncodedContent encodedContent = send_encodeContent( message, method, charset, format, sendType );
		if( contentType != null ) {
			encodedContent.contentType = contentType;
		}

		send_appendGenericHeaders( message, encodedContent, charset, requestEncoding, headerBuilder );
		headerBuilder.append( HttpUtils.CRLF );

		if( Interpreter.getInstance().isMonitoring() ) {
			// message's body in string format needed for the monitoring
			String bodyMessageString =
				encodedContent.content != null ? encodedContent.content.toString( charset ) : "";

			Interpreter.getInstance().fireMonitorEvent(
				new ProtocolMessageEvent(
					bodyMessageString,
					headerBuilder.toString(),
					ExecutionThread.currentThread().getSessionId(),
					Long.toString( message.id() ),
					ProtocolMessageEvent.Protocol.HTTP ) );
		}

		send_logDebugInfo( headerBuilder, encodedContent, charset );

		Interpreter.getInstance().tracer().trace( () -> {
			try {
				final String traceMessage =
					HttpUtils.prepareSendDebugString( headerBuilder, encodedContent, charset, true );
				return new ProtocolTraceAction( ProtocolTraceAction.Type.HTTP, "HTTP MESSAGE SENT",
					message.resourcePath(), traceMessage, null );
			} catch( UnsupportedEncodingException e ) {
				return new ProtocolTraceAction( ProtocolTraceAction.Type.HTTP, "HTTP MESSAGE SENT",
					message.resourcePath(), e.getMessage(), null );
			}
		} );

		ostream.write( headerBuilder.toString().getBytes( HttpUtils.URL_DECODER_ENC ) );
		if( encodedContent.content != null && !headRequestResponse ) {
			ostream.write( encodedContent.content.getBytes() );
		}
	}

	@Override
	public void send( OutputStream ostream, CommMessage message, InputStream istream )
		throws IOException {
		HttpUtils.send( ostream, message, istream, inInputPort, channel(), this );
	}

	private void recv_checkForSetCookie( HttpMessage message, Value value )
		throws IOException {
		if( hasParameter( HttpUtils.Parameters.COOKIES ) ) {
			String type;
			Value cookies = getParameterFirstValue( HttpUtils.Parameters.COOKIES );
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
				value.setValue( Double.parseDouble( cookieValue ) );
			} catch( NumberFormatException e ) {
				throw new IOException( e );
			}
		} else if( NativeType.BOOL == type ) {
			value.setValue( Boolean.valueOf( cookieValue ) );
		} else {
			value.setValue( cookieValue );
		}
	}

	private void recv_checkForCookies( HttpMessage message, HttpUtils.DecodedMessage decodedMessage )
		throws IOException {
		Value cookies = null;
		if( hasOperationSpecificParameter( decodedMessage.operationName, HttpUtils.Parameters.COOKIES ) ) {
			cookies =
				getOperationSpecificParameterFirstValue( decodedMessage.operationName, HttpUtils.Parameters.COOKIES );
		} else if( hasParameter( HttpUtils.Parameters.COOKIES ) ) {
			cookies = getParameterFirstValue( HttpUtils.Parameters.COOKIES );
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

	private void recv_checkForGenericHeader( HttpMessage message, HttpUtils.DecodedMessage decodedMessage )
		throws IOException {
		Value headers = null;
		if( hasOperationSpecificParameter( decodedMessage.operationName, HttpUtils.Parameters.INCOMING_HEADERS ) ) {
			headers = getOperationSpecificParameterFirstValue( decodedMessage.operationName,
				HttpUtils.Parameters.INCOMING_HEADERS );
		} else if( hasOperationSpecificParameter( decodedMessage.operationName, HttpUtils.Parameters.HEADERS ) ) {
			headers =
				getOperationSpecificParameterFirstValue( decodedMessage.operationName, HttpUtils.Parameters.HEADERS );
		} else if( hasParameter( HttpUtils.Parameters.HEADERS ) ) {
			headers = getParameterFirstValue( HttpUtils.Parameters.HEADERS );
		}
		if( headers != null ) {
			if( headers.hasChildren( HttpUtils.Parameters.HEADERS_WILDCARD ) ) {
				String headerAlias = headers.getFirstChild( HttpUtils.Parameters.HEADERS_WILDCARD ).strValue();
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
		if( message.isGet() && contentType.equals( HttpUtils.ContentTypes.APPLICATION_JSON ) ) {
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

	private void recv_parseMessage( HttpMessage message, HttpUtils.DecodedMessage decodedMessage, String type,
		String charset, ExchangeContext messageMetadata )
		throws IOException {
		final String operationName;
		if( decodedMessage.operationName != null ) {
			operationName = decodedMessage.operationName;
		} else if( message.isResponse() ) {
			operationName = inputId;
		} else {
			operationName = null;
		}

		if( getOperationSpecificStringParameter( operationName, HttpUtils.Parameters.FORCE_CONTENT_DECODING )
			.equals( NativeType.STRING.id() ) ) {
			decodedMessage.value.setValue( new String( message.content(), charset ) );
		} else if( getOperationSpecificStringParameter( operationName, HttpUtils.Parameters.FORCE_CONTENT_DECODING )
			.equals( NativeType.RAW.id() ) ) {
			decodedMessage.value.setValue( new ByteArray( message.content() ) );
		} else if( ContentTypes.TEXT_HTML.equals( type ) ) {
			decodedMessage.value.setValue( new String( message.content(), charset ) );
		} else if( ContentTypes.APPLICATION_X_WWW_FORM_URLENCODED.equals( type ) ) {
			HttpUtils.parseForm( message, decodedMessage.value, charset );
		} else if( ContentTypes.TEXT_XML.equals( type ) || type.contains( "xml" ) ) {
			HttpUtils.parseXML( docBuilder, message, decodedMessage.value, charset );
		} else if( ContentTypes.MULTIPART_FORM_DATA.equals( type ) ) {
			messageMetadata.multiPartFormDataParser = HttpUtils.parseMultiPartFormData( message, decodedMessage.value );
		} else if( ContentTypes.APPLICATION_OCTET_STREAM.equals( type ) || type.startsWith( "image/" )
			|| "application/zip".equals( type ) ) {
			decodedMessage.value.setValue( new ByteArray( message.content() ) );
		} else if( ContentTypes.APPLICATION_NDJSON.equals( type ) || type.contains( "ndjson" ) ) {
			boolean strictEncoding = checkStringParameter( HttpUtils.Parameters.JSON_ENCODING, "strict" );
			HttpUtils.parseNdJson( message, decodedMessage.value, strictEncoding, charset );
		} else if( ContentTypes.APPLICATION_JSON.equals( type ) || type.contains( "json" ) ) {
			boolean strictEncoding = checkStringParameter( HttpUtils.Parameters.JSON_ENCODING, "strict" );
			HttpUtils.parseJson( message, decodedMessage.value, strictEncoding, charset );
		} else {
			decodedMessage.value.setValue( new String( message.content(), charset ) );
		}
	}

	private String getDefaultOperation( String method ) {
		method = method.toLowerCase();
		if( hasParameter( HttpUtils.Parameters.DEFAULT_OPERATION ) ) {
			Value dParam = getParameterFirstValue( HttpUtils.Parameters.DEFAULT_OPERATION );
			if( method.isEmpty() || !dParam.hasChildren( method ) ) {
				return dParam.strValue();
			} else {
				return dParam.getFirstChild( method ).strValue();
			}
		}

		return null;
	}

	private Optional< RequestErrorCommMessage > recv_checkReceivingOperation( HttpMessage message,
		HttpUtils.DecodedMessage decodedMessage ) {
		if( decodedMessage.operationName == null ) {
			final String requestPath = HttpUtils.cutBeforeQuerystring( message.requestPath() ).substring( 1 );
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
				decodedMessage.resourcePath = message.getProperty( HttpUtils.Headers.JOLIE_RESOURCE_PATH );
				if( decodedMessage.resourcePath == null ) {
					decodedMessage.resourcePath = "/";
				}
			}
			if( hasOperationSpecificParameter( decodedMessage.operationName, HttpUtils.Parameters.TEMPLATE ) ) {
				return Optional.of( new RequestErrorCommMessage( decodedMessage.id, decodedMessage.operationName,
					decodedMessage.resourcePath, decodedMessage.value, null,
					getOperationSpecificStringParameter( decodedMessage.operationName,
						HttpUtils.Parameters.TEMPLATE ) ) );
			}
		}

		return Optional.empty();
	}

	private void recv_templatedOperation( HttpMessage message, HttpUtils.DecodedMessage decodedMessage )
		throws UnsupportedMethodException {
		// FIXME, TODO: strange jolie double slash
		String uri = HttpUtils.cutBeforeQuerystring( message.requestPath().startsWith( "//" )
			? message.requestPath().substring( 1 )
			: message.requestPath() );
		Value configurationValue = getParameterFirstValue( CommProtocol.Parameters.OPERATION_SPECIFIC_CONFIGURATION );
		Iterator< Entry< String, ValueVector > > configurationIterator = configurationValue.children().entrySet()
			.iterator();
		boolean foundMatch = false;
		String method = message.getMethod().name();
		while( configurationIterator.hasNext() & !foundMatch ) {
			Entry< String, ValueVector > configEntry = configurationIterator.next();

			Value opConfig = configEntry.getValue().get( 0 );
			if( !opConfig.hasChildren( HttpUtils.Parameters.TEMPLATE ) )
				continue;

			Value uriTemplateResult =
				UriUtils.match( opConfig.getFirstChild( HttpUtils.Parameters.TEMPLATE ).strValue(), uri );
			String opConfigMethod = opConfig.firstChildOrDefault( HttpUtils.Parameters.METHOD, Value::strValue, "" );

			if( uriTemplateResult.boolValue() && method.equalsIgnoreCase( opConfigMethod ) ) {
				foundMatch = true;
				decodedMessage.operationName = configEntry.getKey();
				decodedMessage.resourcePath = "/";

				Iterator< Entry< String, ValueVector > > uriTemplateIterator = uriTemplateResult.children().entrySet()
					.iterator();
				while( uriTemplateIterator.hasNext() ) {
					Entry< String, ValueVector > entry = uriTemplateIterator.next();
					decodedMessage.value.getFirstChild( entry.getKey() )
						.setValue( entry.getValue().get( 0 ).strValue() );
				}
				if( opConfig.hasChildren( HttpUtils.Parameters.INCOMING_HEADERS ) ) {
					Iterator< Entry< String, ValueVector > > inHeadersIterator = opConfig
						.getFirstChild( HttpUtils.Parameters.INCOMING_HEADERS ).children().entrySet().iterator();
					while( inHeadersIterator.hasNext() ) {
						Entry< String, ValueVector > entry = inHeadersIterator.next();
						decodedMessage.value.getFirstChild( entry.getValue().get( 0 ).strValue() )
							.setValue( message.getProperty( entry.getKey() ) );
					}
				}
			}
		}
	}

	private void recv_checkDefaultOp( HttpMessage message, HttpUtils.DecodedMessage decodedMessage )
		throws UnsupportedMethodException {
		if( "/".equals( decodedMessage.resourcePath )
			&& !channel().parentInputPort().canHandleInputOperation( decodedMessage.operationName ) ) {
			String defaultOpId = getDefaultOperation( message.getMethod().name() );
			if( defaultOpId != null ) {
				Value body = decodedMessage.value;
				decodedMessage.value = Value.create();
				decodedMessage.value.getChildren( "data" ).add( body );
				decodedMessage.value.getFirstChild( "operation" ).setValue( decodedMessage.operationName );
				decodedMessage.value.setFirstChild( "requestUri", message.requestPath() );
				if( message.userAgent() != null ) {
					decodedMessage.value.getFirstChild( HttpUtils.Parameters.USER_AGENT )
						.setValue( message.userAgent() );
				}
				Value cookies = decodedMessage.value.getFirstChild( "cookies" );
				for( Entry< String, String > cookie : message.cookies().entrySet() ) {
					cookies.getFirstChild( cookie.getKey() ).setValue( cookie.getValue() );
				}
				decodedMessage.operationName = defaultOpId;
			}
		}
	}

	private void recv_checkForMultiPartHeaders( ExchangeContext messageMetadata,
		HttpUtils.DecodedMessage decodedMessage ) {
		if( messageMetadata.multiPartFormDataParser != null ) {
			String target;
			for( Entry< String, MultiPartFormDataParser.PartProperties > entry : messageMetadata.multiPartFormDataParser
				.getPartPropertiesSet() ) {
				if( entry.getValue().filename() != null ) {
					target = getMultipartHeaderForPart( decodedMessage.operationName, entry.getKey() );
					if( target != null ) {
						decodedMessage.value.getFirstChild( target ).setValue( entry.getValue().filename() );
					}
				}
			}
		}
	}

	private void recv_checkForMessageProperties( HttpMessage message, ExchangeContext messageMetadata,
		HttpUtils.DecodedMessage decodedMessage )
		throws IOException {
		recv_checkForCookies( message, decodedMessage );
		recv_checkForGenericHeader( message, decodedMessage );
		recv_checkForMultiPartHeaders( messageMetadata, decodedMessage );
		if( message.userAgent() != null &&
			hasParameter( HttpUtils.Parameters.USER_AGENT ) ) {
			getParameterFirstValue( HttpUtils.Parameters.USER_AGENT ).setValue( message.userAgent() );
		}

		if( getParameterVector( HttpUtils.Parameters.HOST ) != null ) {
			getParameterFirstValue( HttpUtils.Parameters.HOST )
				.setValue( message.getPropertyOrEmptyString( HttpUtils.Parameters.HOST ) );
		}
	}

	private void recv_checkForStatusCode( HttpMessage message ) {
		if( hasParameter( HttpUtils.Parameters.STATUS_CODE ) ) {
			getParameterFirstValue( HttpUtils.Parameters.STATUS_CODE ).setValue( message.statusCode() );
		}
	}

	private void recv_readJolieHeaders( HttpMessage message, HttpUtils.DecodedMessage decodedMessage ) {
		String messageId = message.getProperty( HttpUtils.Headers.JOLIE_MESSAGE_ID );
		if( messageId != null ) {
			try {
				decodedMessage.id = Long.parseLong( messageId );
			} catch( NumberFormatException e ) {
			}
		}

		if( message.isResponse() ) {
			String operationName = message.getProperty( HttpUtils.Headers.JOLIE_OPERATION );
			if( operationName != null ) {
				decodedMessage.operationName = operationName;
			}
		}
	}

	@Override
	public CommMessage recv_internal( InputStream istream, OutputStream ostream )
		throws IOException {
		HttpMessage message = new HttpParser( istream ).parse();
		CommMessage retVal;
		final ExchangeContext messageMetadata = new ExchangeContext();
		HttpUtils.DecodedMessage decodedMessage = new HttpUtils.DecodedMessage();

		final String charset =
			(message.isResponse() && hasParameter( HttpUtils.Parameters.FORCE_RECEIVING_CHARSET ))
				? getStringParameter( HttpUtils.Parameters.FORCE_RECEIVING_CHARSET )
				: HttpUtils.getResponseCharset( message );

		HttpUtils.recv_checkForChannelClosing( message, channel() );

		recv_readJolieHeaders( message, decodedMessage );

		if( checkBooleanParameter( HttpUtils.Parameters.DEBUG ) ) {
			boolean showContent = false;
			if( getParameterFirstValue( HttpUtils.Parameters.DEBUG ).getFirstChild( "showContent" ).intValue() > 0
				&& message.size() > 0 ) {
				showContent = true;
			}
			Interpreter.getInstance().logInfo( HttpUtils.getDebugMessage( message, charset, showContent ) );
		}

		// tracer
		Interpreter.getInstance().tracer().trace( () -> {
			try {
				final String traceMessage = HttpUtils.getDebugMessage( message, charset, message.size() > 0 );
				return new ProtocolTraceAction( ProtocolTraceAction.Type.HTTP, "HTTP MESSAGE RECEIVED",
					message.requestPath(), traceMessage, null );
			} catch( IOException e ) {
				return new ProtocolTraceAction( ProtocolTraceAction.Type.HTTP, "HTTP MESSAGE RECEIVED",
					message.requestPath(), e.getMessage(), null );

			}

		} );

		recv_checkForStatusCode( message );

		messageMetadata.encoding = message.getProperty( "accept-encoding" );
		messageMetadata.headRequest = inInputPort && message.isHead();

		String contentType = HttpUtils.DEFAULT_CONTENT_TYPE;
		if( message.getProperty( "content-type" ) != null ) {
			contentType = message.getProperty( "content-type" ).split( ";", 2 )[ 0 ].toLowerCase();
		}

		messageMetadata.requestFormat = HttpUtils.getRequestFormat( message );
		messageMetadata.requestCharset = HttpUtils.getRequestCharset( message );
		if( !message.isResponse() ) {
			if( hasParameter( CommProtocol.Parameters.OPERATION_SPECIFIC_CONFIGURATION ) ) {
				recv_templatedOperation( message, decodedMessage );
			}
			Optional< RequestErrorCommMessage > requestErrorCommMessage =
				recv_checkReceivingOperation( message, decodedMessage );

			if( requestErrorCommMessage.isPresent() )
				return requestErrorCommMessage.get();
		}

		// URI parameter parsing
		if( message.requestPath() != null ) {
			boolean strictEncoding = checkStringParameter( HttpUtils.Parameters.JSON_ENCODING, "strict" );
			recv_parseQueryString( message, decodedMessage.value, contentType, strictEncoding );
		}

		/* https://tools.ietf.org/html/rfc7231#section-4.3 */
		if( !message.isGet() && !message.isHead() ) {
			// body parsing
			if( message.size() > 0 ) {
				recv_parseMessage( message, decodedMessage, contentType, charset, messageMetadata );
			}
		}

		if( !message.isResponse() ) {
			recv_checkDefaultOp( message, decodedMessage );
		}

		if( message.isResponse() ) {
			final String operationName = decodedMessage.operationName != null ? decodedMessage.operationName : inputId;
			FaultException faultException = null;
			if( hasOperationSpecificParameter( operationName, HttpUtils.Parameters.STATUS_CODES ) ) {
				faultException = HttpUtils.recv_mapHttpStatusCodeFault( message,
					getOperationSpecificParameterFirstValue( operationName,
						HttpUtils.Parameters.STATUS_CODES ),
					decodedMessage.value );
			}
			String responseHeader;
			if( hasParameter( HttpUtils.Parameters.RESPONSE_HEADER )
				|| hasOperationSpecificParameter( operationName, HttpUtils.Parameters.RESPONSE_HEADER ) ) {
				if( hasOperationSpecificParameter( operationName, HttpUtils.Parameters.RESPONSE_HEADER ) ) {
					responseHeader =
						getOperationSpecificStringParameter( operationName,
							HttpUtils.Parameters.RESPONSE_HEADER );
				} else {
					responseHeader = getStringParameter( HttpUtils.Parameters.RESPONSE_HEADER );
				}
				for( Entry< String, String > param : message.properties() ) {
					decodedMessage.value.getFirstChild( responseHeader ).getFirstChild( param.getKey() )
						.setValue( param.getValue() );
				}
				decodedMessage.value.getFirstChild( responseHeader ).getFirstChild( HttpUtils.Parameters.STATUS_CODE )
					.setValue( message.statusCode() );
			}

			recv_checkForSetCookie( message, decodedMessage.value );
			retVal =
				new CommMessage( decodedMessage.id, operationName, decodedMessage.resourcePath,
					decodedMessage.value,
					faultException, null );
		} else {
			recv_checkForMessageProperties( message, messageMetadata, decodedMessage );
			retVal = new CommMessage( decodedMessage.id, decodedMessage.operationName, decodedMessage.resourcePath,
				decodedMessage.value, null, null );
		}

		if( Interpreter.getInstance().isMonitoring() ) {
			Interpreter.getInstance().fireMonitorEvent(
				new ProtocolMessageEvent(
					HttpUtils.getHttpBody( message, charset ),
					HttpUtils.getHttpHeader( message ),
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

		retVal.metadata().put( HTTP_METADATA_KEY, messageMetadata );
		return retVal;
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
			return Type.UNDEFINED;
		}

		if( opDesc.asOneWayTypeDescription() != null ) {
			if( message.isFault() ) {
				ret = Type.UNDEFINED;
			} else {
				OneWayTypeDescription ow = opDesc.asOneWayTypeDescription();
				ret = (inInputPort) ? Type.VOID : ow.requestType();
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
