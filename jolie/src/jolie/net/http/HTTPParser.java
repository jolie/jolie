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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Vector;

import jolie.lang.parse.Scanner;

public class HTTPParser
{
	private static final String HTTP = "HTTP";
	private static final String GET = "GET";
	private static final String POST = "POST";
	
	final private HTTPScanner scanner;
	private Scanner.Token token;
	
	private void getToken()
		throws IOException
	{
		token = scanner.getToken();
	}
	
	public HTTPParser( InputStream istream )
		throws IOException
	{
		scanner = new HTTPScanner( istream, "network" );
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

	private void parseHeaderProperties( HTTPMessage message )
		throws IOException
	{
		String name, value;
		getToken();
		while( token.is( Scanner.TokenType.ID ) ) {
			name = token.content();
			getToken();
			tokenAssert( Scanner.TokenType.COLON );
			value = scanner.readLine();
			message.setProperty( name.toLowerCase(), value );
			getToken();
		}
	}
	
	private HTTPMessage parseRequest()
		throws IOException
	{
		HTTPMessage message = null;
		if ( token.isKeyword( GET ) ) {
			message = new HTTPMessage( HTTPMessage.Type.GET );
		} else if ( token.isKeyword( POST ) ) {
			message = new HTTPMessage( HTTPMessage.Type.POST );
		} else
			throw new IOException( "Unknown HTTP request type: " + token.content() );
		
		message.setRequestPath( scanner.readWord().substring( 1 ) );
		
		getToken();
		if ( !token.isKeyword( HTTP ) )
			throw new IOException( "Invalid HTTP header: expected HTTP version" );
		
		if ( (char)scanner.currentByte() != '/' )
			throw new IOException( "Expected HTTP version" );

		String version = scanner.readWord();
		if ( "1.0".equals( version ) )
			message.setVersion( HTTPMessage.Version.HTTP_1_0 );
		else if ( "1.1".equals( version ) )
			message.setVersion( HTTPMessage.Version.HTTP_1_1 );
		else
			throw new IOException( "Unsupported HTTP version specified: " + version );
		
		return message;
	}
	
	private HTTPMessage parseMessageType()
		throws IOException
	{
		if ( token.isKeyword( HTTP ) ) {
			return parseResponse();
		} else {
			return parseRequest();
		}
	}
	
	private HTTPMessage parseResponse()
		throws IOException
	{
		HTTPMessage message = new HTTPMessage( HTTPMessage.Type.RESPONSE );
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
	
	private void readContent( HTTPMessage message )
		throws IOException
	{
		String p;
		int contentLength = 0;
		p = message.getProperty( "content-length" );
		if ( p != null && !p.isEmpty() ) {
			try {
				contentLength = Integer.parseInt( p );
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
			HTTPMessage.Version version = message.version();
			if ( // Will the connection be closed?
				// HTTP 1.1
				((version == null || version.equals( HTTPMessage.Version.HTTP_1_1 ))
				&&
				message.getPropertyOrEmptyString( "connection" ).equalsIgnoreCase( "close" ))
				||
				// HTTP 1.0
				(version.equals( HTTPMessage.Version.HTTP_1_0 )
				&&
				!message.getPropertyOrEmptyString( "connection" ).equalsIgnoreCase( "keep-alive" )
				)
			) {
				contentLength = scanner.inputStream().available();
				buffer = new byte[ contentLength ];
				InputStream stream = scanner.inputStream();
				blockingRead( stream, buffer, 0, contentLength );
			}
		}
		
		message.setContent( buffer );
	}

	public HTTPMessage parse()
		throws IOException
	{
		getToken();
		HTTPMessage message = parseMessageType();
		parseHeaderProperties( message );
		readContent( message );
		scanner.eatSeparatorsUntilEOF();
		return message;
	}
}
