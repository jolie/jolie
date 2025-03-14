/***************************************************************************
 *   Copyright (C) by Fabrizio Montesi                                     *
 *   Copyright (C) 2015 by Matthias Dieter Wallnöfer                       *
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

import jolie.lang.parse.Scanner;
import jolie.net.ChannelClosingException;

public class HttpParser {
	private static final String HTTP = "HTTP";
	private static final String GET = "GET";
	private static final String POST = "POST";
	private static final String PUT = "PUT";
	private static final String HEAD = "HEAD";
	private static final String DELETE = "DELETE";
	// private static final String TRACE = "TRACE";
	// private static final String CONNECT = "CONNECT";
	private static final String OPTIONS = "OPTIONS";
	private static final String PATCH = "PATCH";

	private static final Pattern COOKIES_SPLIT_PATTERN = Pattern.compile( ";" );
	private static final Pattern COOKIE_NAME_VALUE_SPLIT_PATTERN = Pattern.compile( "=" );

	private final HttpScanner scanner;
	private Scanner.Token token;

	private void nextToken()
		throws IOException {
		token = scanner.getToken();
	}

	public HttpParser( InputStream istream )
		throws IOException {
		scanner = new HttpScanner( istream );
		// , URI.create( "urn:network" ) );
	}

	private void tokenAssert( Scanner.TokenType type )
		throws IOException {
		if( token.isNot( type ) )
			throwException();
	}

	private void throwException()
		throws IOException {
		throw new IOException( "Malformed HTTP header" );
	}

	private void parseHeaderProperties( HttpMessage message )
		throws IOException {
		String name, value;
		nextToken();
		HttpMessage.Cookie cookie;
		while( token.is( Scanner.TokenType.ID ) ) {
			name = token.content().toLowerCase();
			nextToken();
			tokenAssert( Scanner.TokenType.COLON );
			value = scanner.readLine();
			switch( name ) {
			case "set-cookie":
				// cookie = parseSetCookie( value );
				if( (cookie = parseSetCookie( value )) != null ) {
					message.addSetCookie( cookie );
				}
				break;
			case "cookie":
				String[] ss = value.split( ";" );
				for( String s : ss ) {
					String[] nv = s.trim().split( "=", 2 );
					if( nv.length > 1 ) {
						message.addCookie( nv[ 0 ], nv[ 1 ] );
					}
				}
				break;
			case "user-agent":
				message.setUserAgent( value );
				message.setProperty( name, value );
				break;
			default:
				message.setProperty( name, value );
				break;
			}
			nextToken();
		}
	}

	private HttpMessage.Cookie parseSetCookie( String cookieString ) {
		String[] ss = COOKIES_SPLIT_PATTERN.split( cookieString );
		if( cookieString.isEmpty() == false && ss.length > 0 ) {
			boolean secure = false;
			String domain = "";
			String path = "";
			String expires = "";
			String[] nameValue = COOKIE_NAME_VALUE_SPLIT_PATTERN.split( ss[ 0 ], 2 );
			if( ss.length > 1 ) {
				String[] kv;
				for( int i = 1; i < ss.length; i++ ) {
					if( "secure".equals( ss[ i ] ) ) {
						secure = true;
					} else {
						kv = COOKIE_NAME_VALUE_SPLIT_PATTERN.split( ss[ i ], 2 );
						if( kv.length > 1 ) {
							kv[ 0 ] = kv[ 0 ].trim();
							if( "expires".equalsIgnoreCase( kv[ 0 ] ) ) {
								expires = kv[ 1 ];
							} else if( "path".equalsIgnoreCase( kv[ 0 ] ) ) {
								path = kv[ 1 ];
							} else if( "domain".equalsIgnoreCase( kv[ 0 ] ) ) {
								domain = kv[ 1 ];
							}
						}
					}
				}
			}
			return new HttpMessage.Cookie(
				nameValue[ 0 ],
				nameValue[ 1 ],
				domain,
				path,
				expires,
				secure );
		}
		return null;
	}

	private HttpMessage parseRequest()
		throws IOException {
		HttpMessage message = null;
		if( token.isKeyword( GET ) ) {
			message = new HttpMessage( HttpMessage.Type.GET );
		} else if( token.isKeyword( POST ) ) {
			message = new HttpMessage( HttpMessage.Type.POST );
		} else if( token.isKeyword( HEAD ) ) {
			message = new HttpMessage( HttpMessage.Type.HEAD );
		} else if( token.isKeyword( DELETE ) ) {
			message = new HttpMessage( HttpMessage.Type.DELETE );
		} else if( token.isKeyword( PUT ) ) {
			message = new HttpMessage( HttpMessage.Type.PUT );
		} else if( token.isKeyword( PATCH ) ) {
			message = new HttpMessage( HttpMessage.Type.PATCH );
		} else if( token.isKeyword( OPTIONS ) ) {
			message = new HttpMessage( HttpMessage.Type.OPTIONS );
		} else if( token.is( Scanner.TokenType.EOF ) ) {
			// It's not a real message, the client is just closing a connection.
			throw new ChannelClosingException( "[http] Remote host closed connection." );
		} else {
			throw new UnsupportedMethodException( "Unknown/Unsupported HTTP request type: "
				+ token.content() + "(" + token.type() + ")" );
		}

		message.setRequestPath( URLDecoder.decode( scanner.readWord(), HttpUtils.URL_DECODER_ENC ) );

		nextToken();
		if( !token.isKeywordIgnoreCase( HTTP ) )
			throw new UnsupportedHttpVersionException( "Expected HTTP version" );

		if( scanner.currentCharacter() != '/' )
			throw new UnsupportedHttpVersionException( "Expected HTTP version" );

		String version = scanner.readWord();
		if( "1.0".equals( version ) )
			message.setVersion( HttpMessage.Version.HTTP_1_0 );
		else if( "1.1".equals( version ) )
			message.setVersion( HttpMessage.Version.HTTP_1_1 );
		else
			throw new UnsupportedHttpVersionException( "Unsupported HTTP version specified: " + version );

		return message;
	}

	private HttpMessage parseMessageType()
		throws IOException {
		if( token.isKeywordIgnoreCase( HTTP ) ) {
			return parseResponse();
		} else {
			return parseRequest();
		}
	}

	private HttpMessage parseResponse()
		throws IOException {
		HttpMessage message = new HttpMessage( HttpMessage.Type.RESPONSE );
		if( scanner.currentCharacter() != '/' )
			throw new UnsupportedHttpVersionException( "Expected HTTP version" );

		String version = scanner.readWord();
		if( !("1.1".equals( version ) || "1.0".equals( version )) )
			throw new UnsupportedHttpVersionException( "Unsupported HTTP version specified: " + version );

		nextToken();
		tokenAssert( Scanner.TokenType.INT );
		message.setStatusCode( Integer.parseInt( token.content() ) );
		message.setReason( scanner.readLine() );

		return message;
	}

	private static void blockingRead( InputStream stream, byte[] buffer, int offset, int length )
		throws IOException {
		int s = 0;
		do {
			int r = stream.read( buffer, offset + s, length - s );
			if( r == -1 ) {
				throw new EOFException();
			}
			s += r;
		} while( s < length );
	}

	private static final int BLOCK_SIZE = 0x1000; // 4K

	private static byte[] readAll( InputStream stream )
		throws IOException {
		int r;
		ByteArrayOutputStream c = new ByteArrayOutputStream();
		byte[] tmp = new byte[ BLOCK_SIZE ];
		while( (r = stream.read( tmp, 0, BLOCK_SIZE )) != -1 ) {
			c.write( tmp, 0, r );
			tmp = new byte[ BLOCK_SIZE ];
		}
		return c.toByteArray();
	}

	private void readContent( HttpMessage message )
		throws IOException {
		boolean chunked = false;
		int contentLength = -1;

		String p = message.getProperty( "transfer-encoding" );

		if( p != null && p.trim().startsWith( "chunked" ) ) {
			// Transfer-encoding has the precedence over Content-Length
			chunked = true;
		} else {
			p = message.getProperty( "content-length" );
			if( p != null && !p.isEmpty() ) {
				try {
					contentLength = Integer.parseInt( p );
					if( contentLength == 0 ) {
						message.setContent( new byte[ 0 ] );
						return;
					}
				} catch( NumberFormatException e ) {
					throw new IOException( "Illegal Content-Length value " + p );
				}
			}
		}

		byte[] buffer = null;
		InputStream stream = scanner.inputStream();
		if( chunked ) {
			// Link: http://tools.ietf.org/html/rfc2616#section-3.6.1
			List< byte[] > chunks = new ArrayList<>();
			int l = -1, totalLen = 0;
			scanner.readChar();
			do {
				// the chunk header contains the size in hex format
				// and could contain additional parameters which we ignore atm
				String chunkHeader = scanner.readLine( false );
				String chunkSize = chunkHeader.split( ";", 2 )[ 0 ];
				try {
					l = Integer.parseInt( chunkSize, 16 );
				} catch( NumberFormatException e ) {
					throw new IOException( "Illegal chunk size " + chunkSize );
				}
				// parses the real chunk with the specified size, follwed by CR-LF
				if( l > 0 ) {
					totalLen += l;
					byte[] chunk = new byte[ l ];
					blockingRead( stream, chunk, 0, l );
					chunks.add( chunk );
					scanner.readChar();
					scanner.eatSeparators();
				}
			} while( l > 0 );
			// parse optional trailer (additional HTTP headers)
			parseHeaderProperties( message );
			ByteBuffer b = ByteBuffer.allocate( totalLen );
			chunks.forEach( b::put );
			buffer = b.array();
		} else if( contentLength > 0 ) {
			buffer = new byte[ contentLength ];
			blockingRead( stream, buffer, 0, contentLength );
		} else if( message.isResponse() ) {
			// Per https://tools.ietf.org/html/rfc7230#section-3.3.3 payload may only be sent on *responses*
			// (including the HTTP version header) when there is NO transfer encoding and NO content length
			// indication.
			HttpMessage.Version version =
				(message.version() == null ? HttpMessage.Version.HTTP_1_1 : message.version());

			if( // Will the connection be closed?
				// HTTP 1.1
			(version.equals( HttpMessage.Version.HTTP_1_1 )
				&&
				message.getPropertyOrEmptyString( "connection" ).equalsIgnoreCase( "close" ))
				||
				// HTTP 1.0
				(version.equals( HttpMessage.Version.HTTP_1_0 )
					&&
					!message.getPropertyOrEmptyString( "connection" ).equalsIgnoreCase( "keep-alive" )) ) {
				buffer = readAll( scanner.inputStream() );
			}
		}

		if( buffer != null ) {
			p = message.getProperty( "content-encoding" );
			if( p != null ) {
				if( p.contains( "deflate" ) ) {
					buffer = readAll( new InflaterInputStream( new ByteArrayInputStream( buffer ) ) );
				} else if( p.contains( "gzip" ) ) {
					buffer = readAll( new GZIPInputStream( new ByteArrayInputStream( buffer ) ) );
				} else if( !p.equals( "identity" ) ) {
					throw new UnsupportedEncodingException( "Unrecognized Content-Encoding: " + p );
				}
			}

			message.setContent( buffer );
		} else {
			message.setContent( new byte[ 0 ] );
		}
	}

	public HttpMessage parse()
		throws IOException {
		nextToken();
		HttpMessage message = parseMessageType();
		parseHeaderProperties( message );
		readContent( message );
		scanner.eatSeparatorsUntilEOF();
		return message;
	}
}
