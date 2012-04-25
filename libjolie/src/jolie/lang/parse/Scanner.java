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

package jolie.lang.parse;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import jolie.lang.NativeType;

/**
 * Scanner implementation for the JOLIE language parser.
 * 
 * @author Fabrizio Montesi
 *
 */
public class Scanner
{
	public enum TokenType {
		EOF,				///< End Of File
		ID,					///< [a-z][a-zA-Z0-9]*
		COMMA,				///< ,
		DOT,				///< .
		INT,				///< [0-9]+
		TRUE,				///< true
		FALSE,				///< false
		LONG,				///< [0-9]+L
		DOUBLE,				///< [0-9]*"."[0-9]+(e|E)[0-9]+
		LPAREN,				///< (
		RPAREN,				///< )
		LSQUARE,			///< [
		RSQUARE,			///< ]
		LCURLY,				///< {
		RCURLY,				///< }
		//DOLLAR,			///< $
		STRING,				///< "[[:graph:]]*"
		INCREMENT,			///< ++
		MINUS,				///< The minus sign -
		ASTERISK,			///< *
		DIVIDE,				///< /
		ASSIGN,				///< =
		PLUS,				///< +
		ADD_ASSIGN,			///< +=
		MINUS_ASSIGN,		///< -=
		MULTIPLY_ASSIGN,	///< *=
		DIVIDE_ASSIGN,		///< %=
		SEQUENCE,			///< ;
		IF,					///< if
		ELSE,				///< else
		LANGLE,				///< <
		RANGLE,				///< >
		AT,					///< @
		LINKIN,				///< linkIn
		LINKOUT,			///< linkOut
		INSTANCE_OF,		///< instanceof
		EQUAL,				///< ==
		AND,				///< &&
		OR,					///< ||
		PARALLEL,			///< |
		NOT,				///< !
		CARET,				///< ^
		COLON,				///< :
		OP_OW,				///< OneWay
		OP_RR,				///< RequestResponse
		DEFINE, 			///< define
		MAJOR_OR_EQUAL,		///< >=
		MINOR_OR_EQUAL,		///< <=
		NOT_EQUAL,			///< !=
		NULL_PROCESS,		///< nullProcess
		WHILE,				///< while
		EXECUTION,			///< execution
		THROW,				///< throw
		DOCUMENTATION_COMMENT,
		INSTALL,				///< install
		SCOPE,					///< scope
		SPAWN,					///< spawn
		THIS,					///< this
		COMPENSATE,				///< comp
		EXIT,					///< exit
		INCLUDE,				///< include
		CONSTANTS,				///< constants
		POINTS_TO,				///< ->
		QUESTION_MARK,			///< ?
		ARROW,					///< =>
		DEEP_COPY_LEFT,			///< <<
		RUN,					///< run
		UNDEF,					///< undef
		HASH,					///< #
		PERCENT_SIGN,			///< %
		FOR,					///< for
		FOREACH,				///< foreach
		WITH,					///< with
		DECREMENT,				///< --
		IS_STRING,				///< is_string
		IS_INT,					///< is_int
		IS_DOUBLE,				///< is_double
		IS_BOOL,				///< is_bool
		IS_LONG,				///< is_long
		IS_DEFINED,				///< is_defined
		CAST_INT,				///< int
		CAST_STRING,			///< string
		CAST_DOUBLE,				///< double
		CAST_BOOL,				///< bool
		CAST_LONG,				///< long
		SYNCHRONIZED,			///< synchronized
		THROWS,					///< throws
		CURRENT_HANDLER,		///< cH
		INIT,					///< init
		ERROR					///< Scanner error
	}
	
	/**
	 * This class represents an input token read by the Scanner class.
	 * 
	 * @see Scanner
	 * @author Fabrizio Montesi
	 * @version 1.0
	 *
	 */
	public static class Token
	{
		private final TokenType type;
		private final String content;

		/**
		 * Constructor. The content of the token will be set to "".
		 * @param type the type of this token
		 */
		public Token( TokenType type )
		{
			this.type = type;
			content = "";
		}

		/**
		 * Constructor.
		 * @param type the type of this token
		 * @param content the content of this token
		 */
		public Token( TokenType type, String content )
		{
			this.type = type;
			this.content = content;
		}

		/**
		 * Returns the content of this token.
		 * @return the content of this token
		 */
		public String content()
		{
			return content;
		}

		/**
		 * Returns the type of this token.
		 * @return the type of this token
		 */
		public TokenType type()
		{
			return type;
		}

		/**
		 * Returns <code>true</code> if this token can be considered as a valid
		 * value for a constant, <code>false</code> otherwise.
		 * @return <code>true</code> if this token can be considered as a valid
		 * value for a constant, <code>false</code> otherwise
		 */
		public boolean isValidConstant()
		{
			return	type == TokenType.STRING ||
					type == TokenType.INT ||
					type == TokenType.ID ||
					type == TokenType.LONG ||
					type == TokenType.TRUE ||
					type == TokenType.FALSE ||
					type == TokenType.DOUBLE;
		}

		/**
		 * Equivalent to <code>is(TokenType.EOF)</code>
		 * @return <code>true</code> if this token has type <code>TokenType.EOF</code>, false otherwise
		 */
		public boolean isEOF()
		{
			return ( type == TokenType.EOF );
		}

		/**
		 * Returns <code>true</code> if this token has the passed type, <code>false</code> otherwise.
		 * @param compareType the type to compare the type of this token with
		 * @return <code>true</code> if this token has the passed type, <code>false</code> otherwise
		 */
		public boolean is( TokenType compareType )
		{
			return ( type == compareType );
		}

		/**
		 * Returns <code>true</code> if this token has a different type from the passed one, <code>false</code> otherwise.
		 * @param compareType the type to compare the type of this token with
		 * @return <code>true</code> if this token has a different type from the passed one, <code>false</code> otherwise
		 */
		public boolean isNot( TokenType compareType )
		{
			return ( type != compareType );
		}

		/**
		 * Returns <code>true</code> if this token has type <code>TokenType.ID</code>
		 * and its content is equal to the passed parameter, <code>false</code> otherwise.
		 * @param keyword the keyword to check the content of this token against
		 * @return <code>true</code> if this token has type <code>TokenType.ID</code>
		 * and its content is equal to the passed parameter, <code>false</code> otherwise
		 */
		public boolean isKeyword( String keyword )
		{
			return( type == TokenType.ID && content.equals( keyword ) ); 
		}

		/**
		 * This method behaves as {@link #isKeyword(java.lang.String) isKeyword}, except that
		 * it is case insensitive.
		 * @param keyword the keyword to check the content of this token against
		 * @return
		 */
		public boolean isKeywordIgnoreCase( String keyword )
		{
			return( type == TokenType.ID && content.equalsIgnoreCase( keyword ) );
		}
	}
	

	private final InputStream stream;		// input stream
	private final Reader reader;		// data input
	protected char ch;						// current character
	protected int currInt;					// current stream int
	protected int state;					// current state
	private int line;						// current line
	private final URI source;				// source name

	/**
	 * Constructor
	 * @param stream the <code>InputStream</code> to use for input reading
	 * @param sourceName an arbitrary name
	 * @throws java.io.IOException if the input reading initialization fails
	 */
	public Scanner( InputStream stream, URI source )
		throws IOException
	{
		this.stream = stream;
		this.reader = new InputStreamReader( stream );
		this.source = source;
		line = 1;
		readChar();
	}

	public String readWord()
		throws IOException
	{
		return readWord( true );
	}
	
	public String readWord( boolean readChar )
		throws IOException
	{
		StringBuilder buffer = new StringBuilder();
		if ( readChar ) {
			readChar();
		}

		do {
			buffer.append( ch );
			readChar();
		} while( !isSeparator( ch ) );
		return buffer.toString();
	}
	
	public String readLine()
		throws IOException
	{
		StringBuilder buffer = new StringBuilder();
		readChar();
		while( !isNewLineChar( ch ) ) {
			buffer.append( ch );
			readChar();
		}
		return buffer.toString();
	}
	
	public static String addStringTerminator( String str )
	{
		return str + -1;
	}
	
	public InputStream inputStream()
	{
		return stream;
	}
	
	public int line()
	{
		return line;
	}
	
	public URI source()
	{
		return source;
	}

	public String sourceName()
	{
		return source.getSchemeSpecificPart();
	}
	
	public void eatSeparators()
		throws IOException
	{
		while( isSeparator( ch ) ) {
			readChar();
		}
	}
	
	public void eatSeparatorsUntilEOF()
		throws IOException
	{
		while( isSeparator( ch ) && stream.available() > 0 ) {
			readChar();
		}
	}
	
	public static boolean isSeparator( int c )
	{
		return isNewLineChar( c ) || c == '\t' || c == ' ';
	}
	
	public static boolean isNewLineChar( int c )
	{
		return ( c == '\n' || c == '\r' );
	}
	
	public final void readChar()
		throws IOException
	{
		currInt = reader.read();

		ch = (char)currInt;

		if ( ch == '\n' ) {
			line++;
		}
	}
	
	public char currentCharacter()
	{
		return ch;
	}
	
	public Token getToken()
		throws IOException
	{
		boolean keepRun = true;
		state = 1;
		
		while ( currInt != -1 && isSeparator( ch ) ) {
			readChar();
		}
		
		if ( currInt == -1 ) {
			return new Token( TokenType.EOF );
		}

		boolean stopOneChar = false;
		Token retval = null;
		StringBuilder builder = new StringBuilder();

		while ( keepRun ) {
			if ( currInt == -1 && retval == null ) {
				keepRun = false; // We *need* a token at this point
			}
			switch( state ) {
				/* When considering multi-characters tokens (states > 1),
				 * remember to read another character in case of a
				 * specific character (==) check.
				 */

				case 1:	// First character
					if ( Character.isLetter( ch ) || ch == '_' ) {
						state = 2;
					} else if ( Character.isDigit( ch ) ) {
						state = 3;
					} else if ( ch == '"' ) {
						state = 4;
					} else if ( ch == '+' ) {
						state = 5;
					} else if ( ch == '*' ) {
						state = 23;
					} else if ( ch == '=' ) {
						state = 6;
					} else if ( ch == '|' ) {
						state = 7;
					} else if ( ch == '&' ) {
						state = 8;
					} else if ( ch == '<' ) {
						state = 9;
					} else if ( ch == '>' ) {
						state = 10;
					} else if ( ch == '!' ) {
						state = 11;
					} else if ( ch == '/' ) {
						state = 12;
					} else if ( ch == '-' ) {
						state = 14;
					} else if ( ch == '.') { // DOT or REAL
						state = 16;
					} else {	// ONE CHARACTER TOKEN
						if ( ch == '(' ) {
							retval = new Token( TokenType.LPAREN );
						} else if ( ch == ')' ) {
							retval = new Token( TokenType.RPAREN );
						} else if ( ch == '[' ) {
							retval = new Token( TokenType.LSQUARE );
						} else if ( ch == ']' ) {
							retval = new Token( TokenType.RSQUARE );
						} else if ( ch == '{' ) {
							retval = new Token( TokenType.LCURLY );
						} else if ( ch == '}' ) {
							retval = new Token( TokenType.RCURLY );
						} else if ( ch == '@' ) {
							retval = new Token( TokenType.AT );
						} else if ( ch == ':' ) {
							retval = new Token( TokenType.COLON );
						} else if ( ch == ',' ) {
							retval = new Token( TokenType.COMMA );
						} else if ( ch == ';' ) {
							retval = new Token( TokenType.SEQUENCE );
						} else if ( ch == '%' ) {
							retval = new Token( TokenType.PERCENT_SIGN );
						//else if ( ch == '.' )
							//retval = new Token( TokenType.DOT );
						} else if ( ch == '#' ) {
							retval = new Token( TokenType.HASH );
						} else if ( ch == '^' ) {
							retval = new Token( TokenType.CARET );
						} else if ( ch == '?' ) {
							retval = new Token( TokenType.QUESTION_MARK );
						}
						/*else if ( ch == '$' )
							retval = new Token( TokenType.DOLLAR );*/
						
						readChar();
					}
					
					break;
				case 2:	// ID
					if ( !Character.isLetterOrDigit( ch ) && ch != '_' ) {
						String str = builder.toString();
						if ( "OneWay".equals( str ) ) {
							retval = new Token( TokenType.OP_OW );
						} else if ( "RequestResponse".equals( str ) ) {
							retval = new Token( TokenType.OP_RR );
						} else if ( "linkIn".equals( str ) ) {
							retval = new Token( TokenType.LINKIN );
						} else if ( "linkOut".equals( str ) ) {
							retval = new Token( TokenType.LINKOUT );
						} else if ( "if".equals( str ) ) {
							retval = new Token( TokenType.IF );
						} else if ( "else".equals( str ) ) {
							retval = new Token( TokenType.ELSE );
						} else if ( "and".equals( str ) ) {
							retval = new Token( TokenType.AND );
						} else if ( "or".equals( str ) ) {
							retval = new Token( TokenType.OR );
						} else if ( "include".equals( str ) ) {
							retval = new Token( TokenType.INCLUDE );
						} else if ( "define".equals( str ) ) {
							retval = new Token( TokenType.DEFINE );
						} else if ( "nullProcess".equals( str ) ) {
							retval = new Token( TokenType.NULL_PROCESS );
						} else if ( "while".equals( str ) ) {
							retval = new Token( TokenType.WHILE );
						} else if ( "execution".equals( str ) ) {
							retval = new Token( TokenType.EXECUTION );
						} else if ( "install".equals( str ) ) {
							retval = new Token( TokenType.INSTALL );
						} else if ( "this".equals( str ) ) {
							retval = new Token( TokenType.THIS );
						} else if ( "synchronized".equals( str ) ) {
							retval = new Token( TokenType.SYNCHRONIZED );
						} else if ( "throw".equals( str ) ) {
							retval = new Token( TokenType.THROW );
						} else if ( "scope".equals( str ) ) {
							retval = new Token( TokenType.SCOPE );
						} else if ( "spawn".equals( str ) ) {
							retval = new Token( TokenType.SPAWN );
						} else if ( "comp".equals( str ) ) {
							retval = new Token( TokenType.COMPENSATE );
						} else if ( "exit".equals( str ) ) {
							retval = new Token( TokenType.EXIT );
						} else if ( "constants".equals( str ) ) {
							retval = new Token( TokenType.CONSTANTS );
						} else if ( "undef".equals( str ) ) {
							retval = new Token( TokenType.UNDEF );
						} else if ( "for".equals( str ) ) {
							retval = new Token( TokenType.FOR );
						} else if ( "foreach".equals( str ) ) {
							retval = new Token( TokenType.FOREACH );
						} else if ( "is_defined".equals( str ) ) {
							retval = new Token( TokenType.IS_DEFINED );
						} else if ( "is_string".equals( str ) ) {
							retval = new Token( TokenType.IS_STRING );
						} else if ( "is_int".equals( str ) ) {
							retval = new Token( TokenType.IS_INT );
						} else if ( "is_bool".equals( str ) ) {
							retval = new Token( TokenType.IS_BOOL );
						} else if ( "is_long".equals( str ) ) {
							retval = new Token( TokenType.IS_LONG );
						} else if ( "is_double".equals( str ) ) {
							retval = new Token( TokenType.IS_DOUBLE );
						} else if ( "instanceof".equals( str ) ) {
							retval = new Token( TokenType.INSTANCE_OF );
						} else if ( NativeType.INT.id().equals( str ) ) {
							retval = new Token( TokenType.CAST_INT, str );
						} else if ( NativeType.STRING.id().equals( str ) ) {
							retval = new Token( TokenType.CAST_STRING, str );
						} else if ( NativeType.BOOL.id().equals( str ) ) {
							retval = new Token( TokenType.CAST_BOOL, str );
						} else if ( NativeType.LONG.id().equals( str ) ) {
							retval = new Token( TokenType.CAST_LONG, str );
						} else if ( NativeType.DOUBLE.id().equals( str ) ) {
							retval = new Token( TokenType.CAST_DOUBLE, str );
						} else if ( "throws".equals( str ) ) {
							retval = new Token( TokenType.THROWS );
						} else if ( "cH".equals( str ) ) {
							retval = new Token( TokenType.CURRENT_HANDLER );
						} else if ( "init".equals( str ) ) {
							retval = new Token( TokenType.INIT );
						} else if ( "with".equals( str ) ) {
							retval = new Token( TokenType.WITH );
						} else if ( "true".equals( str ) ) {
							retval = new Token( TokenType.TRUE );
						} else if ( "false".equals( str ) ) {
							retval = new Token( TokenType.FALSE );
						} else {
							retval = new Token( TokenType.ID, str );
						}
					}
					break;	
				case 3: // INT (or LONG, or DOUBLE)
					if ( ch == 'e'|| ch == 'E' ){
						state = 19;
					} else if ( !Character.isDigit( ch ) && ch != '.' ) {
						if ( ch == 'L' ) {
							retval = new Token( TokenType.LONG, builder.toString() );
							readChar();
						} else {
							retval = new Token( TokenType.INT, builder.toString() );
						}
					} else if ( ch == '.' ) {
						builder.append( ch );
						readChar();
						if ( !Character.isDigit( ch ) ) {
							retval =  new Token( TokenType.ERROR, builder.toString() );
						}
						else state = 17; // recognized a DOUBLE
					}
					break;
				case 4:	// STRING
					if ( ch == '"' ) {
						retval = new Token( TokenType.STRING, builder.toString().substring( 1 ) );
						readChar();
					} else if ( ch == '\\' ) { // Parse special characters
						readChar();
						if ( ch == '\\' )
							builder.append( '\\' );
						else if ( ch == 'n' )
							builder.append( '\n' );
						else if ( ch == 't' )
							builder.append( '\t' );
						else if ( ch == 'r' )
							builder.append( '\r' );
						else if ( ch == '"' )
							builder.append( '"' );
						else if ( ch == 'u' )
							builder.append( 'u' );
						else
							throw new IOException( "malformed string: bad \\ usage" );
						
						stopOneChar = true;
						readChar();
					}
					break;		
				case 5:	// PLUS OR CHOICE
					if ( ch == '=' ) {
						retval = new Token( TokenType.ADD_ASSIGN );
						readChar();
					} else if ( ch == '+' ) {
						retval = new Token( TokenType.INCREMENT );
						readChar();
					} else {
						retval = new Token( TokenType.PLUS );
					}
					break;
				case 23:	// MULTIPLY or MULTIPLY_ASSIGN
					if ( ch == '=' ) {
						retval = new Token( TokenType.MULTIPLY_ASSIGN );
						readChar();
					} else {
						retval = new Token( TokenType.ASTERISK, "*" );
					}
					break;
				case 6:	// ASSIGN OR EQUAL
					if ( ch == '=' ) {
						retval = new Token( TokenType.EQUAL );
						readChar();
					} else if ( ch == '>' ) {
						retval = new Token( TokenType.ARROW );
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
				case 9: // LANGLE OR MINOR_OR_EQUAL OR DEEP_COPY_LEFT
					if ( ch == '=' ) {
						retval = new Token( TokenType.MINOR_OR_EQUAL );
						readChar();
					} else if ( ch == '<' ) {
						retval = new Token( TokenType.DEEP_COPY_LEFT );
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
					if ( ch == '*' ) {
						state = 13;
					} else if ( ch == '/' )  {
						state = 15;
					} else if( ch == '=' )  {
						retval = new Token( TokenType.DIVIDE_ASSIGN );
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
						} else if ( ch == '!' ) {
							builder = new StringBuilder();
							readChar();
							state = 21;
						}
					}
					break;
				case 14: // MINUS OR (negative) NUMBER OR POINTS_TO
					if ( Character.isDigit( ch ) )
						state = 3;
					else if ( ch == '-' ) {
						retval = new Token( TokenType.DECREMENT );
						readChar();
					} else if ( ch == '>' ) {
						retval = new Token( TokenType.POINTS_TO );
						readChar();
					} else if ( ch == '=' ) {
						retval = new Token( TokenType.MINUS_ASSIGN );
						readChar();
					} else if ( ch == '.' ) {
						builder.append( ch );
						readChar();
						if ( !Character.isDigit( ch ) )
							retval = new Token( TokenType.ERROR, "-." );
						else 
							state = 17;
					} else
						retval = new Token( TokenType.MINUS );
					break;
				case 15: // LINE_COMMENT: waiting for end of line
					if ( isNewLineChar( ch ) ) {
						readChar();
						retval = getToken();
					}
					break;
				case 16: // DOT
					if ( !Character.isDigit( ch ) )
						retval = new Token( TokenType.DOT );
					else
						state = 17; // It's a REAL
					break;
				case 17: // REAL "."[0-9]+ 
					if ( ch == 'E' || ch == 'e' )
						state = 18;
					else if ( !Character.isDigit( ch ) )
						retval = new Token( TokenType.DOUBLE, builder.toString() );
					break;
				case 18: // Scientific notation, first char after 'E'
					if ( ch == '-' || ch == '+' )
						state = 19;
					else if ( Character.isDigit( ch ) )
						state = 20;
					else
						retval = new Token( TokenType.ERROR ); 
					break;
				case 19: // Scientific notation, first exp. digit
					if ( !Character.isDigit( ch ) )
						retval = new Token( TokenType.ERROR );
					else
						state = 20;
					break;
				case 20: // Scientific notation: from second digit to end
					if ( !Character.isDigit( ch ) )
						retval = new Token( TokenType.DOUBLE, builder.toString() );
					break;
				case 21: // Documentation comment
					if ( ch == '*' ) {
						readChar();
						stopOneChar = true;
						if ( ch == '/' ) {
							readChar();
							retval = new Token( TokenType.DOCUMENTATION_COMMENT, builder.toString() );
						}
					}
					break;
				default:
					retval = new Token( TokenType.ERROR, builder.toString() );
					break;
			}
			
			if ( retval == null ) {
				if ( stopOneChar )
					stopOneChar = false;
				else {
					builder.append( ch );
					readChar();
				}
			} else {
				keepRun = false; // Ok, we are done.
			}
		}

		if ( retval == null ) {
			retval = new Token( TokenType.ERROR );
		}

		return retval;
	}
}
