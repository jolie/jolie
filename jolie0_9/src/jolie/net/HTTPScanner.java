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

package jolie.net;

import java.io.IOException;
import java.io.InputStream;

import jolie.lang.parse.Scanner;

public class HTTPScanner
{
	public enum TokenType {
		EOF,		// End Of File
		ID,			// [a-z]([a-zA-Z0-9]|-)*
		COMMA,		// ,
		INT,		// [0-9]+
		LPAREN,		// (
		RPAREN,		// )
		LSQUARE,	// [
		RSQUARE,	// ]
		LCURLY,		// {
		RCURLY,		// }
		STRING,		// "[[:graph:]]*"
		CHOICE,		// ++
		ASTERISK,	// *
		DIVIDE,		// /
		ASSIGN,		// =
		PLUS,		// +
		IF,			// if
		ELSE,		// else
		LANGLE,		// <
		RANGLE,		// >
		AT,			// @
		LINKIN,		// linkIn
		LINKOUT,	// linkOut
		IN,			// in
		OUT,		// out
		EQUAL,		// ==
		AND,		// and
		OR,			// or
		PARALLEL,	// ||
		NOT,		// !
		COLON,		// :
		OP_OW,		// OneWay
		OP_RR,		// RequestResponse
		OP_N,		// Notification
		OP_SR,		// SolicitResponse
		LOCATIONS,	// locations
		OPERATIONS,	// operations
		VARIABLES,	// variables
		MAIN,		// main
		DEFINE, 	// define
		CALL,		// call
		MAJOR_OR_EQUAL,	// >=
		MINOR_OR_EQUAL,	// <=
		NOT_EQUAL,		// !=
		LINKS,			// links
		NULL_PROCESS,	// nullProcess
		WHILE,			// while
		SLEEP,			// sleep
		CONTENTLENGTH,
		CONTENTTYPE,
		DOT,
		SEMICOLON,
		POST,			// POST
		HTTP,			// HTTP
		ERROR
	}
	
	/**
	 * This class represents an input token read by the Scanner class.
	 * 
	 * @see Scanner
	 * @author Fabrizio Montesi
	 * @version 1.0
	 *
	 */
	public class Token
	{
		private TokenType type;
		private String content;
		
		public Token( TokenType type )
		{
			this.type = type;
			content = "";
		}
		
		public Token( TokenType type, String content )
		{
			this.type = type;
			this.content = content;
		}
		
		public String content()
		{
			return content;
		}
		
		public TokenType type()
		{
			return type;
		}
		
		public boolean isEOF()
		{
			return ( type == TokenType.EOF );
		}
		
		public boolean isA( TokenType compareType )
		{
			return ( type == compareType );
		}
	}
	

	private InputStream stream;				// input stream
	private char ch;						// current character
	private int state;						// current state
	private int line;						// current line
	private String sourceName;				// source name
	
	public HTTPScanner( InputStream stream, String sourceName )
		throws IOException
	{
		this.stream = stream;
		this.sourceName = sourceName;
		line = 1;
		readChar();
	}
	
	public static String addStringTerminator( String str )
	{
		StringBuffer buffer = new StringBuffer( str );
		buffer.append( 65535 );
		return buffer.toString();
	}
	
	public int line()
	{
		return line;
	}
	
	public String sourceName()
	{
		return sourceName;
	}
	
	private boolean isSeparator( char c )
	{
		if ( c == '\n' || c == '\r' || c == '\t' || c == ' ' )
			return true;
		
		return false;
	}
	
	private void readChar()
		throws IOException
	{
		ch = (char) stream.read();

		if ( ch == '\n' )
			line++;
	}
	
	public Token getToken()
		throws IOException
	{
		state = 1;

		while ( isSeparator( ch ) )
			readChar();
		
		if ( ch == 65535 )
			return new Token( TokenType.EOF );

		//boolean ret = false;
		boolean stopOneChar = false;
		Token retval = null;
		String str = new String();

		while ( ch < 65535 && retval == null ) {
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
					else if ( ch == '<' )
						state = 9;
					else if ( ch == '>' )
						state = 10;
					else if ( ch == '!' )
						state = 11;
					else if ( ch == '/' )
						state = 12;
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
							retval = new Token( TokenType.SEMICOLON );
						else if ( ch == '.' )
							retval = new Token( TokenType.DOT );
						
						readChar();
					}
					
					break;
				case 2:	// ID
					if ( !( Character.isLetterOrDigit( ch ) || ch == '-' ) ) {
						if ( "OneWay".equals( str ) )
							retval = new Token( TokenType.OP_OW );
						else if ( "RequestResponse".equals( str ) )
							retval = new Token( TokenType.OP_RR );
						else if ( "Notification".equals( str ) )
							retval = new Token( TokenType.OP_N );
						else if ( "SolicitResponse".equals( str ) )
							retval = new Token( TokenType.OP_SR );
						else if ( "linkIn".equals( str ) )
							retval = new Token( TokenType.LINKIN );
						else if ( "linkOut".equals( str ) )
							retval = new Token( TokenType.LINKOUT );
						else if ( "if".equals( str ) )
							retval = new Token( TokenType.IF );
						else if ( "else".equals( str ) )
							retval = new Token( TokenType.ELSE );
						else if ( "in".equals( str ) )
							retval = new Token( TokenType.IN );
						else if ( "out".equals( str ) )
							retval = new Token( TokenType.OUT );
						else if ( "and".equals( str ) )
							retval = new Token( TokenType.AND );
						else if ( "or".equals( str ) )
							retval = new Token( TokenType.OR );
						else if ( "locations".equals( str ) )
							retval = new Token( TokenType.LOCATIONS );
						else if ( "operations".equals( str ) )
							retval = new Token( TokenType.OPERATIONS );
						else if ( "variables".equals( str ) )
							retval = new Token( TokenType.VARIABLES );
						else if ( "main".equals( str ) )
							retval = new Token( TokenType.MAIN );
						else if ( "define".equals( str ) )
							retval = new Token( TokenType.DEFINE );
						else if ( "call".equals( str ) )
							retval = new Token( TokenType.CALL );
						else if ( "links".equals( str ) )
							retval = new Token( TokenType.LINKS );
						else if ( "nullProcess".equals( str ) )
							retval = new Token( TokenType.NULL_PROCESS );
						else if ( "while".equals( str ) )
							retval = new Token( TokenType.WHILE );
						else if ( "sleep".equals( str ) )
							retval = new Token( TokenType.SLEEP );
						else if ( "content-length".equalsIgnoreCase( str ) )
							retval = new Token( TokenType.CONTENTLENGTH );
						else if ( "content-type".equalsIgnoreCase( str ) )
							retval = new Token( TokenType.CONTENTTYPE );
						else if ( "POST".equalsIgnoreCase( str ) )
							retval = new Token( TokenType.POST );
						else if ( "HTTP".equalsIgnoreCase( str ) )
							retval = new Token( TokenType.HTTP );
						/*else if ( str.equals( "int" ) )
							retval = new Token( TokenType.INTKEYWORD );
						else if ( str.equals( "string" ) )
							retval = new Token( TokenType.STRINGKEYWORD );*/
						else
							retval = new Token( TokenType.ID, str );
					}
					break;	
				case 3: // INT
					if ( !Character.isDigit( ch ) )
						retval = new Token( TokenType.INT, str );
					break;
				case 4:	// STRING
					if ( ch == '"' ) {
						retval = new Token( TokenType.STRING, str.substring( 1 ) );
						readChar();
					} else if ( ch == '\\' ) { // Parse special characters
						readChar();
						if ( ch == '\\' )
							str += '\\';
						else if ( ch == 'n' )
							str += '\n';
						else if ( ch == 't' )
							str += '\t';
						else if ( ch == '"' )
							str += '"';
						else
							throw new IOException( "malformed string: bad \\ usage" );
						
						stopOneChar = true;
						readChar();
					}
					break;		
				case 5:	// PLUS OR CHOICE
					if ( ch == '+' ) {
						retval = new Token( TokenType.CHOICE );
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
				case 7:	// PARALLEL
					if ( ch == '|' ) {
						retval = new Token( TokenType.PARALLEL );
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
				case 11: // NOT_EQUAL
					if ( ch == '=' ) {
						retval = new Token( TokenType.NOT_EQUAL );
						readChar();
					} else
						retval = new Token( TokenType.NOT );
					break;
				case 12: // DIVIDE OR BEGIN_COMMENT OR LINE_COMMENT
					if ( ch == '*' ) {
						state = 13;
						readChar();
					} else if ( ch == '/' )  {
						state = 15;
						readChar();
					} else
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
				if ( stopOneChar )
					stopOneChar = false;
				else {
					str += ch;
					readChar();
				}
			}
		}
		
		//state = 1;
		
		if ( retval == null )
			retval = new Token( TokenType.ERROR );
		
		return retval;
	}
	
}