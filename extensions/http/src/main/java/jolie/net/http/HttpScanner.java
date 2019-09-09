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
import java.net.URI;
import jolie.lang.parse.Scanner;
import jolie.lang.parse.Scanner.Token;
import jolie.lang.parse.Scanner.TokenType;

public class HttpScanner
{
	private final InputStream stream;
	private int state; // current state
	private int currInt;
	private char ch;
	private static final int OVERFLOW_NET = 8192;
	
	
	public HttpScanner( InputStream stream, URI source )
		throws IOException
	{
		this.stream = stream;
		readChar();
	}

	public String readLine()
		throws IOException
	{
		return readLine( true );
	}

	public String readLine( boolean readChar )
		throws IOException
	{
		if ( readChar ) {
			readChar();
		}
		
		resetTokenBuilder();
		
		while( !Scanner.isNewLineChar( ch ) ) {
			tokenBuilder.append( ch );
			readChar();
		}
		// okay, we have met \r now parse also \n (HTTP specification)
		if ( ch == '\r' ) {
			readChar();
			if ( ch != '\n' ) {
				throw new IOException( "malformed CR-LF sequence" );
			}
		}
		return tokenBuilder.toString();
	}
	
	/*
	 * TODO: remove code duplication from jolie.lang.Scanner
	 */
	public String readWord()
		throws IOException
	{
		return readWord( true );
	}
	
	public String readWord( boolean readChar )
		throws IOException
	{
		if ( readChar ) {
			readChar();
		}
		
		resetTokenBuilder();

		do {
			tokenBuilder.append( ch );
			readChar();
		} while( !Scanner.isSeparator( ch ) );
		return tokenBuilder.toString();
	}
	
	public void eatSeparators()
		throws IOException
	{
		while( Scanner.isNewLineChar( ch ) ) {
			readChar();
		}
	}
	
	public void eatSeparatorsUntilEOF()
		throws IOException
	{
		while( Scanner.isSeparator( ch ) && stream.available() > 0 ) {
			readChar();
		}
	}
	
	public char currentCharacter()
	{
		return ch;
	}
	
	public InputStream inputStream()
	{
		return stream;
	}
	
	public final void readChar()
		throws IOException
	{
		currInt = stream.read();
		ch = (char)currInt;            
	}
	
	private final StringBuilder tokenBuilder = new StringBuilder( 64 );

	private void resetTokenBuilder()
	{
		tokenBuilder.setLength( 0 );
	}
	
	public Token getToken()
		throws IOException
	{
		state = 1;
		
		tokenBuilder.append( ch );
		while ( currInt != -1 && Scanner.isSeparator( ch ) ) {
			readChar();
			tokenBuilder.append( ch );

			if ( tokenBuilder.indexOf( "\n" ) < tokenBuilder.lastIndexOf( "\n" ) ) {
				return new Token( TokenType.EOF );
			}
		}

		if ( currInt == -1 )
			return new Token( TokenType.EOF );

		boolean stopOneChar = false;
		Token retval = null;
		resetTokenBuilder();

		while ( currInt != -1 && retval == null ) {
			switch( state ) {
				/* When considering multi-characters tokens (states > 1),
				 * remember to read another character in case of a
				 * specific character (==) check.
				 */

				case 1:	// First character
					if ( Character.isLetter( ch ) )
						state = 2;
					else if ( Character.isDigit( ch ) )
						state = 3;
					else if ( ch == '"' )
						state = 4;
					else if ( ch == '+' )
						state = 5;
					else if ( ch == '=' )
						state = 6;
					else if ( ch == '|' )
						state = 7;
					else if ( ch == '&' )
						state = 8;
					else if ( ch == '<' )
						state = 9;
					else if ( ch == '>' )
						state = 10;
					else if ( ch == '!' )
						state = 11;
					else if ( ch == '/' )
						state = 12;
					else if ( ch == '-' )
						state = 14;
					else {	// ONE CHARACTER TOKEN
						if ( ch == '(' )							
							retval = new Token( TokenType.LPAREN );
						else if ( ch == ')' )
							retval = new Token( TokenType.RPAREN );
						else if ( ch == '[' )
							retval = new Token( TokenType.LSQUARE );
						else if ( ch == ']' )
							retval = new Token( TokenType.RSQUARE );
						else if ( ch == '{' )
							retval = new Token( TokenType.LCURLY );
						else if ( ch == '}' )
							retval = new Token( TokenType.RCURLY );
						else if ( ch == '*' )
							retval = new Token( TokenType.ASTERISK );
						else if ( ch == '@' )
							retval = new Token( TokenType.AT );
						else if ( ch == ':' )
							retval = new Token( TokenType.COLON );
						else if ( ch == ',' )
							retval = new Token( TokenType.COMMA );
						else if ( ch == ';' )
							retval = new Token( TokenType.SEQUENCE );
						else if ( ch == '.' )
							retval = new Token( TokenType.DOT );
						else if ( ch == '/' )
							retval = new Token( TokenType.DIVIDE );
						
						readChar();
					}
					
					break;
				case 2:	// ID
					if ( !Character.isLetterOrDigit( ch ) &&
							ch != '_' &&
							ch != '-' &&
							ch != '+' ) {
						retval = new Token( TokenType.ID, tokenBuilder.toString() );
					}
					break;	
				case 3: // INT
					if ( !Character.isDigit( ch ) )
						retval = new Token( TokenType.INT, tokenBuilder.toString() );
					break;
				case 4:	// STRING
					if ( ch == '"' ) {
						retval = new Token( TokenType.STRING, tokenBuilder.substring( 1 ) );
						readChar();
					} else if ( ch == '\\' ) { // Parse special characters
						readChar();
						if ( ch == '\\' )
							tokenBuilder.append( '\\' );
						else if ( ch == 'n' )
							tokenBuilder.append( '\n' );
						else if ( ch == 't' )
							tokenBuilder.append( '\t' );
						else if ( ch == '"' )
							tokenBuilder.append( '"' );
						else
							throw new IOException( "malformed string: bad \\ usage" );
						
						stopOneChar = true;
						readChar();
					}
					break;		
				case 5:	// PLUS OR CHOICE
					if ( ch == '+' ) {
						retval = new Token( TokenType.INCREMENT );
						readChar();
					} else
						retval = new Token( TokenType.PLUS );
					break;
				case 6:	// ASSIGN OR EQUAL
					if ( ch == '=' ) {
						retval = new Token( TokenType.EQUAL );
						readChar();
					} else
						retval = new Token( TokenType.ASSIGN );
					break;
				case 7:	// PARALLEL OR LOGICAL OR
					if ( ch == '|' ) {
						retval = new Token( TokenType.OR );
						readChar();
					} else
						retval = new Token( TokenType.PARALLEL );
					break;
				case 8:	// LOGICAL AND
					if ( ch == '&' ) {
						retval = new Token( TokenType.AND );
						readChar();
					}
					break;
				case 9: // LANGLE OR MINOR_OR_EQUAL
					if ( ch == '=' ) {
						retval = new Token( TokenType.MINOR_OR_EQUAL );
						readChar();
					} else
						retval = new Token( TokenType.LANGLE );
					break;
				case 10: // RANGLE OR MINOR_OR_EQUAL
					if ( ch == '=' ) {
						retval = new Token( TokenType.MAJOR_OR_EQUAL );
						readChar();
					} else
						retval = new Token( TokenType.RANGLE );
					break;
				case 11: // NOT OR NOT_EQUAL
					if ( ch == '=' ) {
						retval = new Token( TokenType.NOT_EQUAL );
						readChar();
					} else
						retval = new Token( TokenType.NOT );
					break;
				case 12: // DIVIDE OR BEGIN_COMMENT OR LINE_COMMENT
					/*if ( ch == '*' ) {
						state = 13;
						readChar();
					} else if ( ch == '/' )  {
						state = 15;
						readChar();
					} else*/
						retval = new Token( TokenType.DIVIDE );
					break;
				case 13: // WAITING FOR END_COMMENT
					if ( ch == '*' ) {
						readChar();
						stopOneChar = true;
						if ( ch == '/' ) {
							readChar();
							retval = getToken();
						}
					}
					break;
				case 14: // MINUS OR (negative) INT
					if ( Character.isDigit( ch ) )
						state = 3;
					else
						retval = new Token( TokenType.MINUS );
					break;
				case 15: // LINE_COMMENT: waiting for end of line
					if ( ch == '\n' ) {
						readChar();
						retval = getToken();
					}
					break;
				default:
					retval = new Token( TokenType.ERROR );
					break;
			}
			
			if ( retval == null ) {
				if ( stopOneChar ) {
					stopOneChar = false;
				} else {
					if ( tokenBuilder.length() > OVERFLOW_NET ) {
						throw new IOException(
							"Token length exceeds maximum allowed limit ("
							+ OVERFLOW_NET +
							" bytes). First 10 characters: "
							+ tokenBuilder.toString().substring( 0, 10 )
							+ " Last 10 characters: " + tokenBuilder.toString().substring( tokenBuilder.length() - 10, tokenBuilder.length() )
						);
					}
					tokenBuilder.append( ch );
					readChar();
				}
			}
		}

		if ( retval == null )
			retval = new Token( TokenType.ERROR );

		return retval;
	}	
}

