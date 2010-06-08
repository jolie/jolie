/***************************************************************************
 *   Copyright (C) by Fabrizio Montesi                                     *
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.util.Vector;
import java.util.regex.Pattern;

import jolie.lang.parse.Scanner;

public class HttpParser
{
	private final static String URL_DECODER_ENC = "UTF-8";
	private static final String HTTP = "HTTP";
	private static final String GET = "GET";
	private static final String POST = "POST";
	private static final String PUT = "PUT";
	private static final String HEAD = "HEAD";
	private static final String DELETE = "DELETE";
	private static final String TRACE = "TRACE";
	private static final String CONNECT = "CONNECT";
	private static final String OPTIONS = "OPTIONS";

	private static final Pattern cookiesSplitPattern = Pattern.compile( ";" );
	private static final Pattern cookieNameValueSplitPattern = Pattern.compile( "=" );
	
	private final HttpScanner scanner;
	private Scanner.Token token;
	
	private void getToken()
		throws IOException
	{
		token = scanner.getToken();
	}
	
	public HttpParser( InputStream istream )
		throws IOException
	{
		scanner = new HttpScanner( istream, "network" );
	}
	
	private void tokenAssert( Scanner.TokenType type )
		throws IOException
	{
		if ( token.isNot( type ) )
			throwException();
	}
	
	private void throwException()
		throws IOException
	{
		throw new IOException( "Malformed HTTP header" );
	}

	private void parseHeaderProperties( HttpMessage message )
		throws IOException
	{
		String name, value;
		getToken();
		HttpMessage.Cookie cookie;
		while( token.is( Scanner.TokenType.ID ) ) {
			name = token.content().toLowerCase();
			getToken();
			tokenAssert( Scanner.TokenType.COLON );
			value = scanner.readLine();
			if ( "set-cookie".equals( name ) ) {
				//cookie = parseSetCookie( value );
				if ( (cookie=parseSetCookie( value )) != null ) {
					message.addSetCookie( cookie );
				}
			} else if ( "cookie".equals( name ) ) {
				String ss[] = value.split(  ";" );
				for( String s : ss ) {
					String nv[] = s.trim().split( "=" );
					if ( nv.length > 1 ) {
						message.addCookie( nv[0], nv[1] );
					}
				}
			} else {
				message.setProperty( name, value );
			}
			getToken();
		}
	}
	
	private HttpMessage.Cookie parseSetCookie( String cookieString )
	{
		String ss[] = cookiesSplitPattern.split( cookieString );
		if ( cookieString.isEmpty() == false && ss.length > 0 ) {
			boolean secure = false;
			String domain = "";
			String path = "";
			String expires = "";
			String nameValue[] = cookieNameValueSplitPattern.split( ss[ 0 ], 2 );
			if ( ss.length > 1 ) {
				String kv[];
				for( int i = 1; i < ss.length; i++ ) {
					if ( "secure".equals( ss[ i ] ) ) {
						secure = true;
					} else {
						kv = cookieNameValueSplitPattern.split( ss[ i ], 2 );
						if ( kv.length > 1 ) {
							kv[ 0 ] = kv[ 0 ].trim();
							if ( "expires".equalsIgnoreCase( kv[ 0 ] ) ) {
								expires = kv[ 1 ];
							} else if ( "path".equalsIgnoreCase( kv[ 0 ] ) ) {
								path = kv[ 1 ];
							} else if ( "domain".equalsIgnoreCase( kv[ 0 ] ) ) {
								domain = kv[ 1 ];
							}
						}
					}
				}
			}
			return new HttpMessage.Cookie(
					nameValue[0],
					nameValue[1],
					domain,
					path,
					expires,
					secure
				);
		}
		return null;
	}
	
	private HttpMessage parseRequest()
		throws IOException
	{
		HttpMessage message = null;
		if ( token.isKeyword( GET ) ) {
			message = new HttpMessage( HttpMessage.Type.GET );
		} else if ( token.isKeyword( POST ) ) {
			message = new HttpMessage( HttpMessage.Type.POST );
		} else if (
			token.isKeyword( OPTIONS ) || token.isKeyword( CONNECT ) ||
			token.isKeyword( HEAD ) || token.isKeyword( PUT ) ||
			token.isKeyword( DELETE ) || token.isKeyword( TRACE )
		) {
			message = new HttpMessage( HttpMessage.Type.UNSUPPORTED );
		} else {
			throw new IOException( "Unknown HTTP request type: " + token.content() + "(" + token.type() + ")" );
		}

		message.setRequestPath( URLDecoder.decode( scanner.readWord().substring( 1 ), URL_DECODER_ENC ) );

		getToken();
		if ( !token.isKeyword( HTTP ) )
			throw new IOException( "Invalid HTTP header: expected HTTP version" );
		
		if ( (char)scanner.currentByte() != '/' )
			throw new IOException( "Expected HTTP version" );

		String version = scanner.readWord();
		if ( "1.0".equals( version ) )
			message.setVersion( HttpMessage.Version.HTTP_1_0 );
		else if ( "1.1".equals( version ) )
			message.setVersion( HttpMessage.Version.HTTP_1_1 );
		else
			throw new IOException( "Unsupported HTTP version specified: " + version );
		
		return message;
	}
	
	private HttpMessage parseMessageType()
		throws IOException
	{
		if ( token.isKeyword( HTTP ) ) {
			return parseResponse();
		} else {
			return parseRequest();
		}
	}
	
	private HttpMessage parseResponse()
		throws IOException
	{
		HttpMessage message = new HttpMessage( HttpMessage.Type.RESPONSE );
		if ( (char)scanner.currentByte() != '/' )
			throw new IOException( "Expected HTTP version" );

		String version = scanner.readWord();
		if ( !( "1.1".equals( version ) || "1.0".equals( version ) ) )
			throw new IOException( "Unsupported HTTP version specified: " + version );

		getToken();
		tokenAssert( Scanner.TokenType.INT );
		message.setHttpCode( Integer.parseInt( token.content() ) );
		message.setReason( scanner.readLine() );

		return message;
	}
	
	@SuppressWarnings( "empty-statement" )
	public static void blockingRead( InputStream stream, byte[] buffer, int offset, int length )
		throws IOException
	{
		int r = 0;
		while( (r+=stream.read( buffer, offset+r, length-r )) < length ); 
	}
	
	private static final int BLOCK_SIZE = 1024;
	
	public static byte[] readAll( InputStream stream )
		throws IOException
	{
		int r = 0;
		ByteArrayOutputStream c = new ByteArrayOutputStream();
		byte[] tmp = new byte[ BLOCK_SIZE ];
		while( (r=stream.read( tmp, 0, BLOCK_SIZE )) != -1 ) {
			c.write( tmp, 0, r );
			tmp = new byte[ BLOCK_SIZE ];
		}
		return c.toByteArray();
	}
	
	private void readContent( HttpMessage message )
		throws IOException
	{
		String p;
		int contentLength = 0;
		p = message.getProperty( "content-length" );
		if ( p != null && !p.isEmpty() ) {
			try {
				contentLength = Integer.parseInt( p );
				if ( contentLength == 0 ) {
					message.setContent( new byte[0] );
					return;
				}
			} catch( NumberFormatException e ) {
				contentLength = 0;
			}
		}
		
		boolean chunked = false;
		p = message.getProperty( "transfer-encoding" );
		if ( p != null && p.equals( "chunked" ) )
			chunked = true;
		
		byte buffer[] = null;
		if ( chunked ) {
			InputStream stream = scanner.inputStream();
			Vector< byte[] > chunks = new Vector< byte[] > ();
			byte[] chunk;
			
			int l;
			int total = 0;
			boolean keepRun = true;
			String lStr = scanner.readWord();
			while( keepRun ) {
				l = Integer.parseInt( lStr, 16 );
				if ( l > 0 ) {
					scanner.eatSeparators();
					total += l;
					chunk = new byte[ l ];
					chunk[0] = scanner.currentByte();
					blockingRead( stream, chunk, 1, l - 1 );
					chunks.add( chunk );
					scanner.readChar();
					scanner.eatSeparators();
					lStr = scanner.readWord( false );
				} else
					keepRun = false;
			}
			ByteBuffer b = ByteBuffer.allocate( total );
			for( byte[] c : chunks )
				b.put( c );
			buffer = b.array();
		} else if ( contentLength > 0 ) {
			buffer = new byte[ contentLength ];
			InputStream stream = scanner.inputStream();
			blockingRead( stream, buffer, 0, contentLength );
		} else {
			HttpMessage.Version version = message.version();
			if ( // Will the connection be closed?
				// HTTP 1.1
				((version == null || version.equals( HttpMessage.Version.HTTP_1_1 ))
				&&
				message.getPropertyOrEmptyString( "connection" ).equalsIgnoreCase( "close" ))
				||
				// HTTP 1.0
				(version.equals( HttpMessage.Version.HTTP_1_0 )
				&&
				!message.getPropertyOrEmptyString( "connection" ).equalsIgnoreCase( "keep-alive" )
				)
			) {
				buffer = readAll( scanner.inputStream() );
			}
		}
		
		message.setContent( buffer );
	}

	public HttpMessage parse()
		throws IOException
	{
		getToken();
		HttpMessage message = parseMessageType();
		parseHeaderProperties( message );
		readContent( message );
		scanner.eatSeparatorsUntilEOF();
		return message;
	}
}
