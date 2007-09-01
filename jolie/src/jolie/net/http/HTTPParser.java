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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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
	
	private void eat( Scanner.TokenType type )
		throws IOException
	{
		tokenAssert( type );
		getToken();
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
	
	private void conditionAssert( boolean condition )
		throws IOException
	{
		if ( condition == false )
			throwException();
	}
	
	private void eatHeaderUntil( boolean condition )
		throws IOException
	{
		while(
				token.isNot( Scanner.TokenType.EOF ) &&
				token.isNot( Scanner.TokenType.ERROR ) &&
				condition == false
				) {
			getToken();
		}
	}
	
	public HTTPMessage parse()
		throws IOException
	{		
		getToken();
		int contentLength = 0;
		int httpCode = 0;
		String contentType = new String();
		HTTPMessage message = null;
		HTTPMessage.Type type = HTTPMessage.Type.ERROR;
		String requestPath = null;
		if ( token.isKeyword( HTTP ) ) { // It's an HTTP response
			getToken();
			eat( Scanner.TokenType.DIVIDE );
			conditionAssert(
					token.is( Scanner.TokenType.INT ) &&
					Integer.parseInt( token.content() ) == 1
					);
			getToken();
			eat( Scanner.TokenType.DOT );
			httpCode = Integer.parseInt( token.content() );
			conditionAssert(
					token.is( Scanner.TokenType.INT ) &&
					( httpCode == 1 || httpCode == 0 )
					);
			getToken();
			tokenAssert( Scanner.TokenType.INT );
			httpCode = Integer.parseInt( token.content() );
			getToken();
			eatHeaderUntil( token.isKeywordIgnoreCase( "Content-Length" ) );
			
			conditionAssert( token.isKeywordIgnoreCase( "Content-Length" ) );
			getToken();
			eat( Scanner.TokenType.COLON );
			tokenAssert( Scanner.TokenType.INT );
			contentLength = Integer.parseInt( token.content() );

			type = HTTPMessage.Type.RESPONSE;
		} else {
			if ( token.isKeyword( POST ) ) {
				type = HTTPMessage.Type.POST;
			} else if ( token.isKeyword( GET ) ) {
				type = HTTPMessage.Type.GET;
			} else {
				throw new IOException( "Unrecognized HTTP request type" );
			}

			requestPath = scanner.readWord().substring( 1 );
			
			while( token.isNot( Scanner.TokenType.EOF ) ) {
				eatHeaderUntil(
					token.isKeywordIgnoreCase( "Content-Length" ) ||
					token.isKeywordIgnoreCase( "Content-Type" )
					);
				if ( token.isKeywordIgnoreCase( "Content-Length" ) ) {
					getToken();
					eat( Scanner.TokenType.COLON );
					tokenAssert( Scanner.TokenType.INT );
					contentLength = Integer.parseInt( token.content() );
				} else if ( token.isKeywordIgnoreCase( "Content-Type" ) ) {
					getToken();
					eat( Scanner.TokenType.COLON );
					contentType = scanner.readWord();
				}
			}
		}

		char buffer[] = new char[ contentLength ];
		if ( contentLength != 0 ) {
			InputStream stream = scanner.inputStream();
			BufferedReader reader = new BufferedReader( new InputStreamReader( stream ) );

			buffer[0] = scanner.currentCharacter();
			reader.read( buffer, 0, contentLength );
		}
		
		/*if ( true ) {
			InputStream stream = scanner.inputStream();
			BufferedReader reader = new BufferedReader( new InputStreamReader( stream ) );

			// Eat the rest of the HTTP header
			String line;
			do {
				System.out.println( line = reader.readLine() );
			} while( line != null );
		}application/x-www-form-urlencoded*/
		
		if ( type == HTTPMessage.Type.RESPONSE ) {
			message = new HTTPMessage( httpCode, buffer );
		} else {
			message = new HTTPMessage( type, requestPath, contentType, buffer );
		}

		return message;
	}
}
