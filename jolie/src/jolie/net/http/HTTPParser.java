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
	
	public HTTPMessage parse()
		throws IOException
	{
		getToken();
		int contentLength = 0;
		HTTPMessage message = null;
		if ( token.isKeyword( HTTP ) ) { // It's an HTTP response
			int httpCode;
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
			while(
					token.isNot( Scanner.TokenType.EOF ) &&
					token.isNot( Scanner.TokenType.ERROR ) &&
					!token.isKeywordIgnoreCase( "Content-Length" )
					) {
				getToken();
			}
			conditionAssert( token.isKeywordIgnoreCase( "Content-Length" ) );
			getToken();
			eat( Scanner.TokenType.COLON );
			tokenAssert( Scanner.TokenType.INT );
			contentLength = Integer.parseInt( token.content() );
			//int ch = scanner.currentCharacter();
			InputStream stream = scanner.inputStream();
			BufferedReader reader = new BufferedReader( new InputStreamReader( stream ) );
			String line = reader.readLine();
			do {
				line = reader.readLine();
			} while( line != null && !"".equals( line ) );
			char buffer[] = new char[ contentLength ];
			reader.read( buffer );
			message = new HTTPMessage( httpCode, buffer );
		} else {	// @todo Handle get and post messages here
			
		}
		return message;
	}
}
