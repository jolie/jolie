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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import jolie.lang.parse.Scanner;

public class HTTPParser
{
	private static final String HTTP = "HTTP";
	private static final String GET = "GET";
	private static final String POST = "POST";
	
	private HTTPScanner scanner;
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
	
	/*private void eat( Scanner.TokenType type )
		throws IOException
	{
		tokenAssert( type );
		getToken();
	}*/
	
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
	
	/*private void assertCondition( boolean condition )
		throws IOException
	{
		if ( condition == false )
			throwException();
	}
	
	private void eatHeaderUntil( String[] conds )
		throws IOException
	{
		int i;
		while(
				token.isNot( Scanner.TokenType.EOF ) &&
				token.isNot( Scanner.TokenType.ERROR ) 
			) {
			for( i = 0; i < conds.length; i++ ) {
				if ( token.isKeywordIgnoreCase( conds[i] ) )
						return;
			}
			getToken();
		}
	}*/
	
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
		if ( !( "1.1".equals( version ) || "1.0".equals( version ) ) )
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
	
	private void readContent( HTTPMessage message )
		throws IOException
	{
		String p;
		int contentLength = 0;
		p = message.getProperty( "content-length" );
		if ( p != null )
			contentLength = Integer.parseInt( p );
		
		boolean chunked = false;
		p = message.getProperty( "transfer-encoding" );
		if ( p != null && p.equals( "chunked" ) )
			chunked = true;
		
		byte buffer[] = new byte[ contentLength ];
		if ( contentLength > 0 ) {
			InputStream stream = scanner.inputStream();
			BufferedInputStream reader = new BufferedInputStream( stream );

			buffer[0] = scanner.currentByte();
			reader.read( buffer, 0, contentLength );
		} else if ( chunked ) {
			byte tmp[] = new byte[ 1024*64 ];
			//int total = 0;
			int l = 0;
			String lStr;
			
			InputStream stream = scanner.inputStream();
			BufferedInputStream reader = new BufferedInputStream( stream );
			
			//do {
				lStr = scanner.readWord();
				l = Integer.parseInt( lStr, 16 );
				scanner.eatSeparators();
				tmp[0] = scanner.currentByte();
				reader.read( tmp, 1, l - 1 );
				while( !token.isEOF() && token.isNot( Scanner.TokenType.ERROR ) )
					getToken();
			//} while( l > 0 );
			buffer = new byte[ l ];
			for( int i = 0; i < l; i++ )
				buffer[i] = tmp[i];
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
		return message;
	}
}
