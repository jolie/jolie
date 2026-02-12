/*
 * Copyright (C) 2006-2020 Fabrizio Montesi <famontesi@gmail.com>
 * Copyright (C) 2021-2022 Vicki Mixen <vicki@mixen.dk>
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

package jolie.lang.parse;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import jolie.lang.Constants;
import jolie.lang.NativeType;
import jolie.lang.parse.module.ModuleSource;

/**
 * Scanner implementation for the Jolie language parser.
 *
 * @author Fabrizio Montesi
 *
 */
public class Scanner implements AutoCloseable {
	// @formatter:off
	/** Token types */
	public enum TokenType {
		EOF,				///< End Of File
		ID,					///< [a-zA-Z_][a-zA-Z0-9_]*
		COMMA,				///< ,
		DOT,				///< .
		DOTDOT,				///< ..
		INT,				///< [0-9]+
		TRUE,				///< true
		FALSE,				///< false
		LONG,				///< [0-9]+(l|L)
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
		DOCUMENTATION_FORWARD,
		DOCUMENTATION_BACKWARD,
		INSTALL,				///< install
		SCOPE,				///< scope
		SPAWN,				///< spawn
		THIS,				///< this
		COMPENSATE,			///< comp
		EXIT,				///< exit
		INCLUDE,			///< include
		CONSTANTS,			///< constants
		POINTS_TO,			///< ->
		QUESTION_MARK,		///< ?
		ARROW,				///< =>
		DEEP_COPY_LEFT,		///< <<
		DEEP_COPY_WITH_LINKS_LEFT,	///< <<-
		RUN,				///< run
		UNDEF,				///< undef
		HASH,				///< #
		DOLLAR,				///< $
		PERCENT_SIGN,		///< %
		APOSTROPHE,			///< '
		BACKTICK,			///< `
		UNDERSCORE,			///< _
		TILDE,				///< ~
		FOR,				///< for
		FOREACH,			///< foreach
		WITH,				///< with
		DECREMENT,			///< --
		IS_STRING,			///< is_string
		IS_INT,				///< is_int
		IS_DOUBLE,			///< is_double
		IS_BOOL,			///< is_bool
		IS_LONG,			///< is_long
		IS_DEFINED,			///< is_defined
		CAST_INT,			///< int
		CAST_STRING,		///< string
		CAST_DOUBLE,		///< double
		CAST_BOOL,			///< bool
		CAST_LONG,			///< long
		SYNCHRONIZED,		///< synchronized
		THROWS,				///< throws
		CURRENT_HANDLER,	///< cH
		INIT,				///< init
		PROVIDE,			///< provide
		IMPORT,				///< import
		AS,					///< as
		FROM,				///< from
		PRIVATE,			///< private
		PUBLIC,				///< public
		NEWLINE,			///< a newline token
		ERROR				///< Scanner error
	}
	// @formatter:off

	/*
	 * Map of unreserved keywords,
	 * which can be considered as IDs in certain places (e.g. variables).
	 */
	private static final Map< String, TokenType > UNRESERVED_KEYWORDS = new HashMap<>();

	static {
		// Initialise the unreserved keywords map.
		UNRESERVED_KEYWORDS.put( "OneWay", TokenType.OP_OW );
		UNRESERVED_KEYWORDS.put( "oneWay", TokenType.OP_OW );
		UNRESERVED_KEYWORDS.put( "RequestResponse", TokenType.OP_RR );
		UNRESERVED_KEYWORDS.put( "requestResponse", TokenType.OP_RR );
		UNRESERVED_KEYWORDS.put( "linkIn", TokenType.LINKIN );
		UNRESERVED_KEYWORDS.put( "linkOut", TokenType.LINKOUT );
		UNRESERVED_KEYWORDS.put( "if", TokenType.IF );
		UNRESERVED_KEYWORDS.put( "else", TokenType.ELSE );
		UNRESERVED_KEYWORDS.put( "include", TokenType.INCLUDE );
		UNRESERVED_KEYWORDS.put( "define", TokenType.DEFINE );
		UNRESERVED_KEYWORDS.put( "nullProcess", TokenType.NULL_PROCESS );
		UNRESERVED_KEYWORDS.put( "while", TokenType.WHILE );
		UNRESERVED_KEYWORDS.put( "execution", TokenType.EXECUTION );
		UNRESERVED_KEYWORDS.put( "install", TokenType.INSTALL );
		UNRESERVED_KEYWORDS.put( "this", TokenType.THIS );
		UNRESERVED_KEYWORDS.put( "synchronized", TokenType.SYNCHRONIZED );
		UNRESERVED_KEYWORDS.put( "throw", TokenType.THROW );
		UNRESERVED_KEYWORDS.put( "scope", TokenType.SCOPE );
		UNRESERVED_KEYWORDS.put( "spawn", TokenType.SPAWN );
		UNRESERVED_KEYWORDS.put( "comp", TokenType.COMPENSATE );
		UNRESERVED_KEYWORDS.put( "exit", TokenType.EXIT );
		UNRESERVED_KEYWORDS.put( "constants", TokenType.CONSTANTS );
		UNRESERVED_KEYWORDS.put( "undef", TokenType.UNDEF );
		UNRESERVED_KEYWORDS.put( "for", TokenType.FOR );
		UNRESERVED_KEYWORDS.put( "foreach", TokenType.FOREACH );
		UNRESERVED_KEYWORDS.put( "is_defined", TokenType.IS_DEFINED );
		UNRESERVED_KEYWORDS.put( "is_string", TokenType.IS_STRING );
		UNRESERVED_KEYWORDS.put( "is_int", TokenType.IS_INT );
		UNRESERVED_KEYWORDS.put( "is_bool", TokenType.IS_BOOL );
		UNRESERVED_KEYWORDS.put( "is_long", TokenType.IS_LONG );
		UNRESERVED_KEYWORDS.put( "is_double", TokenType.IS_DOUBLE );
		UNRESERVED_KEYWORDS.put( "instanceof", TokenType.INSTANCE_OF );
		UNRESERVED_KEYWORDS.put( NativeType.INT.id(), TokenType.CAST_INT );
		UNRESERVED_KEYWORDS.put( NativeType.STRING.id(), TokenType.CAST_STRING );
		UNRESERVED_KEYWORDS.put( NativeType.BOOL.id(), TokenType.CAST_BOOL );
		UNRESERVED_KEYWORDS.put( NativeType.DOUBLE.id(), TokenType.CAST_DOUBLE );
		UNRESERVED_KEYWORDS.put( NativeType.LONG.id(), TokenType.CAST_LONG );
		UNRESERVED_KEYWORDS.put( "throws", TokenType.THROWS );
		UNRESERVED_KEYWORDS.put( "cH", TokenType.CURRENT_HANDLER );
		UNRESERVED_KEYWORDS.put( "init", TokenType.INIT );
		UNRESERVED_KEYWORDS.put( "with", TokenType.WITH );
		UNRESERVED_KEYWORDS.put( "true", TokenType.TRUE );
		UNRESERVED_KEYWORDS.put( "false", TokenType.FALSE );
		UNRESERVED_KEYWORDS.put( "provide", TokenType.PROVIDE );
		UNRESERVED_KEYWORDS.put( "import", TokenType.IMPORT );
		UNRESERVED_KEYWORDS.put( "from", TokenType.FROM );
		UNRESERVED_KEYWORDS.put( "as", TokenType.AS );
		UNRESERVED_KEYWORDS.put( "private", TokenType.PRIVATE );
		UNRESERVED_KEYWORDS.put( "public", TokenType.PUBLIC );
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
		private final boolean isUnreservedKeyword;

		/**
		 * Constructor. The content of the token will be set to "".
		 *
		 * @param type the type of this token
		 */
		public Token( TokenType type )
		{
			this.type = type;
			this.content = "";
			this.isUnreservedKeyword = false;
		}

		/**
		 * Constructor.
		 *
		 * @param type the type of this token
		 * @param content the content of this token
		 */
		public Token( TokenType type, String content )
		{
			this.type = type;
			this.content = content;
			this.isUnreservedKeyword = false;
		}

		/**
		 * Constructor.
		 *
		 * @param type the type of this token
		 * @param content the content of this token
		 * @param isUnreservedKeyword specifies whether this token is an unreserved keyword
		 */
		public Token( TokenType type, String content, boolean isUnreservedKeyword )
		{
			this.type = type;
			this.content = content;
			this.isUnreservedKeyword = isUnreservedKeyword;
		}

		/**
		 * Returns the content of this token.
		 *
		 * @return the content of this token
		 */
		public String content()
		{
			return content;
		}

		/**
		 * Returns the type of this token.
		 *
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
			return type == TokenType.STRING
				|| type == TokenType.INT
				|| type == TokenType.ID
				|| type == TokenType.LONG
				|| type == TokenType.TRUE
				|| type == TokenType.FALSE
				|| type == TokenType.DOUBLE;
		}

		/**
		 * Equivalent to <code>is(TokenType.EOF)</code>
		 *
		 * @return <code>true</code> if this token has type <code>TokenType.EOF</code>, false otherwise
		 */
		public boolean isEOF()
		{
			return type == TokenType.EOF;
		}

		/**
		 * Returns <code>true</code> if this token has the passed type, <code>false</code> otherwise.
		 *
		 * @param compareType the type to compare the type of this token with
		 * @return <code>true</code> if this token has the passed type, <code>false</code> otherwise
		 */
		public boolean is( TokenType compareType )
		{
			return type == compareType;
		}

		/**
		 * Returns <code>true</code> if this token has a different type from the passed one, <code>false</code> otherwise.
		 *
		 * @param compareType the type to compare the type of this token with
		 * @return <code>true</code> if this token has a different type from the passed one, <code>false</code> otherwise
		 */
		public boolean isNot( TokenType compareType )
		{
			return type != compareType;
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
			return type == TokenType.ID && content.equals( keyword );
		}

		/**
		 * Returns <code>true</code> if this token has type <code>TokenType.ID</code>
		 * or is a token for an unreserved keyword, <code>false</code> otherwise.
		 * @return <code>true</code> if this token has type <code>TokenType.ID</code>
		 * or is a token for an unreserved keyword, <code>false</code> otherwise.
		 */
		public boolean isIdentifier()
		{
			return type == TokenType.ID || isUnreservedKeyword;
		}

		/**
		 * This method behaves as {@link #isKeyword(java.lang.String) isKeyword}, except that
		 * it is case insensitive.
		 * @param keyword the keyword to check the content of this token against
		 */
		public boolean isKeywordIgnoreCase( String keyword )
		{
			return type == TokenType.ID && content.equalsIgnoreCase( keyword );
		}
	}

	private final InputStream stream;		// input stream
	private final InputStreamReader reader;	// data input
	protected char ch;						// current character
	protected int currInt;					// current stream int
    private int line;						// current line
	private int startLine;					// start line for eventual error (has to be set)
	private int endLine;					// end line, same as start line
	private final URI source;				// source name
	private final boolean includeDocumentation;	// include documentation tokens
	private final ArrayList<String> readCodeLines = new ArrayList<>();
	private int currColumn;					// column of the current character
	private int errorColumn;				// column of the error character (first character of the current token or line)
	private int tokenEndLine;				// Line the last returned token ended on
	private int tokenEndColumn; 			// Column the last returned token ended on

	/**
	 * Constructor for arbitrary streams, used for CommandLineParser parsing constants
	 *
	 * @param stream the <code>InputStream</code> to use for input reading
	 * @param source the source URI of the stream
	 * @throws java.io.IOException if the input reading initialization fails
	 */
	public Scanner( InputStream stream, URI source )
		throws IOException
	{
		this.stream = stream;
		this.reader = new InputStreamReader( stream );
		this.source = source;
		this.includeDocumentation = false;
		line = 0;
		startLine = 0;
		endLine = 0;
		currColumn = -1;
		readChar();
	}

	/**
	 * Constructor
	 *
	 * @param source the source of the stream
	 * @param charset the character encoding
	 * @param includeDocumentation if true, emit documentation tokens
	 * @throws java.io.IOException if the input reading initialization fails
	 */
	public Scanner( ModuleSource source, String charset, boolean includeDocumentation )
		throws IOException
	{
		this.stream = source.openStream();
		this.reader = charset != null ? new InputStreamReader( stream, charset ) : new InputStreamReader( stream );
		this.source = source.uri();
		this.includeDocumentation = includeDocumentation;
		line = 0;
		startLine = 0;
		endLine = 0;
		currColumn = -1;
		readChar();
	}

	/**
	 * Constructor for a scanner that does not return documentation tokens.
	 *
	 * @param source the source of the stream
	 * @param charset the character encoding
	 * @throws java.io.IOException if the input reading initialization fails
	 */
	public Scanner( ModuleSource source, String charset ) throws IOException
	{
		this( source, charset, false );
	}

	public boolean includeDocumentation()
	{
		return includeDocumentation;
	}

	private final StringBuilder tokenBuilder = new StringBuilder( 64 );

	private void resetTokenBuilder()
	{
		tokenBuilder.setLength( 0 );
	}

	public String readLine()
		throws IOException
	{
		resetTokenBuilder();
		readChar();
		while( !isNewLineChar( ch ) ) {
			tokenBuilder.append( ch );
			readChar();
		}
		return tokenBuilder.toString();
	}

	/**
	 * Extra method to read line. Used when exception happens and the rest of the line, where exception happened has to be read
	 * to create the error message and help message
	 * @return
	 * @throws IOException
	 */
	public String readLineAfterError()
		throws IOException
	{
		resetTokenBuilder();
		// TODO: would it make more sense to just have the while loop?
		if(currInt != -1){ // if currInt == -1, EOF was already found, and there is no more line to read after the error
			readCharAfterError();
			while( !isNewLineChar( ch ) && reader.ready() && currInt != -1) {
				tokenBuilder.append( ch );
				readCharAfterError();
			}
		}
		return tokenBuilder.toString();
	}

	public InputStream inputStream()
	{
		return stream;
	}

	/**
	 * Returns character encoding
	 *
	 * @return character encoding
	 */
	public String charset()
	{
		return reader.getEncoding();
	}

	/**
	 * Returns the current line the scanner is reading.
	 *
	 * @return the current line the scanner is reading.
	 */
	public int line()
	{
		return line;
	}

	/**
	 * Returns the source URI the scanner is reading.
	 *
	 * @return the source URI the scanner is reading
	 */
	public URI source()
	{
		return source;
	}

	/**
	 * Used for getting the string of the line, where the error occured, with line number.
	 *
	 * @return current line in file, with line number
	 */
	public List<String> codeLineWithLineNumber() {
		try{
			int lineNumber = line() + 1;
			String line = lineNumber + ":" + readCodeLines.get( line() );
			if(!line.endsWith("\n")){
				line += "\n";
			}
			return List.of(line);
		} catch (IndexOutOfBoundsException e){
			if(line()>0){
				int lineNumber = line();
				String line = lineNumber + ":" + readCodeLines.get( line() - 1 );
				if(!line.endsWith("\n")){
					line += "\n";
				}
				return List.of(line);
			} else{ // if line is 0, and indexoutofbound happened, then no line has been read yet
				return List.of();
			}
		}
	}

	/**
	 * Used for getting the erroneous line of code
	 * @return current line in file
	 */
	public List<String> codeLine(){
		try{
			String line = readCodeLines.get(line());
			if(!line.endsWith("\n")){
				line += "\n";
			}
			return List.of(line);
		} catch (IndexOutOfBoundsException e){
			if(line()>0){
				String line = readCodeLines.get(line()-1);
				if(!line.endsWith("\n")){
					line += "\n";
				}
				return List.of(line);
			} else { // no lines have been read yet
				return List.of();
			}
		}
	}

	/**
	 * Returns the current column, meaning the index of the current character in the line
	 * @return current column
	 */
	public int currentColumn() {
		return currColumn;
	}

	/**
	 * Returns The 0-indexed column of the error character (first character of the current token)
	 * @return the starting index of the token
	 */
	public int errorColumn() {
		return errorColumn;
	}

	/**
	 * Sets the errorColumn to the currentColumn
	 */
	public void setErrorColumn(){
		this.errorColumn = currentColumn();
	}

	/**
	 * Return all the lines of code that have been read during parsing, as a list of lines
	 * @return all read code lines
	 */
	public List<String> getAllCodeLines(){
		return readCodeLines;
	}

	/**
	 * Returns the startLine
	 * @return startLine
	 */
	public int startLine(){
		return startLine;
	}

	/**
	 * Sets the startLine to the given int
	 * @param startLine
	 */
	public void setStartLine(int startLine){
		this.startLine = startLine;
	}
	/**
	 * Returns the endLine
	 * @return endLine
	 */
	public int endLine(){
		return endLine;
	}

	/**
	 * Sets the endLine to the given int
	 * @param endLine
	 */
	public void setEndLine(int endLine){
		this.endLine = endLine;
	}

	/**
	 * The 0-indexed line the last returned token ended on
	 * @return Line the last returned token ended on
	 */
	public int tokenEndLine(){
		return tokenEndLine;
	}

	/**
	 * Returns the column the last returned token ended on +1 (for LSP specification compatibility)
	 * @return Column the last returned token ended on +1
	 */
	public int tokenEndColumn() {
		return tokenEndColumn;
	}

	/**
	 * Saves the end line and column of a token. Sets tokenEndColumn and tokenEndLine correctly if run before readChar()
	 * moves the Scanner past the last character of the Token.
	 */
	private void recordTokenEnd() {
		// +1 to make the tokenEndColumn exclusive
		tokenEndColumn = currColumn + 1;
		tokenEndLine = line;
	}



	/*
	 * used in AbstractParser getContextDuringError()
	 * returns 1 when startline == 0, endline == 0 and line >= 0
	 * returns 2 when endline > 0 and startline < endline
	 * returns 3 when endline > 0 and startline >= endline
	 * returns -1 if none of the others match
	 */
	public int lineState(){
		// start and end line might not have been set
		if(startLine() == 0 && endLine() == 0 && line() >= 0){
			return 0;
		}
		// end line has been set and is larger than start line
		if(endLine() > 0 && startLine() < endLine()){
			return 1;
		}
		// if end line has been set and start line is larger or equal to end line
		if(endLine() > 0 && startLine() >= endLine()){
			return 2;
		}
		return -1;
	}

	/**
	 * Eats all separators (whitespace) until the next input.
	 */
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

	/**
	 * Checks whether a character is a separator (whitespace).
	 *
	 * @param c the character to check as a whitespace
	 * @return <code>true</code> if <code>c</code> is a separator (whitespace)
	 */
	public static boolean isSeparator( char c )
	{
		return isNewLineChar( c ) || c == '\t' || c == ' ';
	}

	/**
	 * Checks whether a character is a horizontal separator (whitespace).
	 *
	 * @param c the character to check
	 * @return <code>true</code> if <code>c</code> is a horizontal separator (whitespace)
	 */
	public static boolean isHorizontalWhitespace( char c )
	{
		return c == '\t' || c == ' ';
	}

	/**
	 * Checks whether a character is an overflow character.
	 *
	 * @param c the character to check
	 * @return <code>true</code> if <code>c</code> is an overflow character
	 */
	private static boolean isOverflowChar( char c )
	{
		return (int) c >= Character.MAX_VALUE;
	}

	/**
	 * Checks whether a character is a newline character.
	 *
	 * @param c the character to check
	 * @return <code>true</code> if <code>c</code> is a newline character
	 */
	public static boolean isNewLineChar( char c )
	{
		return c == '\n' || c == '\r';
	}

	/**
	 * Reads the next character and loads it into the scanner local state.
	 *
	 * @throws IOException if the source cannot be read
	 */
	public final void readChar()
		throws IOException
	{
		currInt = reader.read();

		ch = (char) currInt;
		if( readCodeLines.isEmpty() ) { // initialize the list of code lines, if it is empty
			readCodeLines.add(0, "");
		}
		String temp;
		if(currInt != -1){ //Cannot just return, as this messes with codeckecking after parsing
			// The if statement makes sure no extra caracters are added when EOF is reached
			try { // Set the line of code to the line index in readCodeLines
				temp = readCodeLines.get( line() );
				if(ch == '\t'){
					temp += " ".repeat(Constants.TAB_SIZE);
				} else {
					temp += ch;
				}
				readCodeLines.set( line(), temp );
			} catch( IndexOutOfBoundsException e ) {
				// the index in readCodeLines has not been initialized, add it to the list
				temp = "";
				if(ch == '\t'){
					temp += " ".repeat(Constants.TAB_SIZE);
				} else {
					temp += ch;
				}
				readCodeLines.add( line(), temp );
			}
		}
		currColumn++;
		if ( ch == '\n' ) {
			line++;
			currColumn = -1;
		}
	}

	/**
	 * The same as readChar except it returns as soon as EOF is found, instead of avoiding adding -1 to the token
	 * @throws IOException
	 */
	public final void readCharAfterError()
		throws IOException
	{
		currInt = reader.read();
		if (currInt == -1){
			return;
		}

		ch = (char) currInt;
		if( readCodeLines.isEmpty() ) { // Initialize list of code lines, if it is empty
			readCodeLines.add(0, "");
		}
		String temp;
		try { // set the line in readCodeLines to the line with the new char
			temp = readCodeLines.get( line() );
			if(ch == '\t'){
				temp += " ".repeat(Constants.TAB_SIZE);
			} else {
				temp += ch;
			}
			readCodeLines.set( line(), temp );
		} catch( IndexOutOfBoundsException e ) {
			// line in readCodeLines does not exist yet, add it with the new char
			temp = "";
			if(ch == '\t'){
				temp += " ".repeat(Constants.TAB_SIZE);
			} else {
				temp += ch;
			}
			readCodeLines.add( line(), temp );
		}
		currColumn++;
		if ( ch == '\n' ) {
			line++;
			currColumn = -1;
		}
	}

	/**
	 * Returns the current character in the scanner local state.
	 *
	 * @return the current character in the scanner local state
	 */
	public char currentCharacter()
	{
		return ch;
	}

	// The lowercase _or_ names are intentional and help reading.
	@SuppressWarnings("PMD")
	private enum State
	{
		FIRST_CHARACTER,
		ID,
		INT_or_LONG_or_DOUBLE,
		STRING,
		PLUS_or_CHOICE,
		MULTIPLY_or_MULTIPLY_ASSIGN,
		ASSIGN_or_EQUAL,
		PARALLEL_or_LOGIC_OR,
		LOGIC_AND,
		LANGLE_or_MINOR_OR_EQUAL_or_DEEP_COPY_LEFT_or_DEEP_COPY_WITH_LINKS_LEFT,
		DEEP_COPY_WITH_LINKS_LEFT_or_DEEP_COPY_LEFT,
		RANGLE_or_MINOR_OR_EQUAL,
		NOT_or_NOT_EQUAL,
		DIVIDE_or_BEGIN_COMMENT_or_LINE_COMMENT,
		WAITING_FOR_END_COMMENT,
		MINUS_or_NUMBER_or_POINTS_TO,
		LINE_COMMENT,
		DOT,
		REAL,
		SCIENTIFIC_NOTATION_FIRST_AFTER_E,
		SCIENTIFIC_NOTATION_FIRST_EXP_DIGIT,
		SCIENTIFIC_NOTATION_SECOND_TO_END_DIGITS,
		DOCUMENTATION_FORWARD_BLOCK,
		DOCUMENTATION_FORWARD_INLINE,
		DOCUMENTATION_BACKWARD_BLOCK,
		DOCUMENTATION_BACKWARD_INLINE
	}

	/**
	 * Consumes characters from the source text and returns its corresponding token.
	 *
	 * @return the token corresponding to the consumed characters
	 * @throws IOException if not enough characters can be read from the source
	 */
	public Token getToken()
		throws IOException
	{
		boolean keepRun = true;
        // current state
        State state = State.FIRST_CHARACTER;
		while ( currInt != -1 && isHorizontalWhitespace( ch ) ) {
			readChar();
		}

		if ( currInt == -1 ) {
			errorColumn = currColumn-1;
			return new Token( TokenType.EOF );
		}

		errorColumn = currColumn;

		boolean stopOneChar = false;
		Token retval = null;
		resetTokenBuilder();

		while( keepRun ) {
			if ( currInt == -1 && retval == null ) {
				keepRun = false; // We *need* a token at this point
			}
			switch(state) {
				/* When considering multi-characters tokens (states > 1),
				 * remember to read another character in case of a
				 * specific character (==) check.
				 */

				case FIRST_CHARACTER: // First character
					if ( Character.isLetter( ch ) || ch == '_' ) {
						state = State.ID;
					} else if ( Character.isDigit( ch ) ) {
						state = State.INT_or_LONG_or_DOUBLE;
					} else if ( ch == '"' ) {
						state = State.STRING;
					} else if ( ch == '+' ) {
						state = State.PLUS_or_CHOICE;
					} else if ( ch == '*' ) {
						state = State.MULTIPLY_or_MULTIPLY_ASSIGN;
					} else if ( ch == '=' ) {
						state = State.ASSIGN_or_EQUAL;
					} else if ( ch == '|' ) {
						state = State.PARALLEL_or_LOGIC_OR;
					} else if ( ch == '&' ) {
						state = State.LOGIC_AND;
					} else if ( ch == '<' ) {
						state = State.LANGLE_or_MINOR_OR_EQUAL_or_DEEP_COPY_LEFT_or_DEEP_COPY_WITH_LINKS_LEFT;
					} else if ( ch == '>' ) {
						state = State.RANGLE_or_MINOR_OR_EQUAL;
					} else if ( ch == '!' ) {
						state = State.NOT_or_NOT_EQUAL;
					} else if ( ch == '/' ) {
						state = State.DIVIDE_or_BEGIN_COMMENT_or_LINE_COMMENT;
					} else if ( ch == '-' ) {
						state = State.MINUS_or_NUMBER_or_POINTS_TO;
					} else if ( ch == '.' ) { // DOT, DOTDOT or REAL
						state = State.DOT;
					} else { // ONE CHARACTER TOKEN

						//The ending column of One Character Tokens is the position of the character the Token is made of.
						recordTokenEnd();

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
							retval = new Token( TokenType.AT, "@" );
						} else if ( ch == ':' ) {
							retval = new Token( TokenType.COLON );
						} else if ( ch == ',' ) {
							retval = new Token( TokenType.COMMA );
						} else if ( ch == ';' ) {
							retval = new Token( TokenType.SEQUENCE );
						} else if ( ch == '%' ) {
							retval = new Token( TokenType.PERCENT_SIGN );
						} else if ( ch == '#' ) {
							retval = new Token( TokenType.HASH );
						} else if ( ch == '^' ) {
							retval = new Token( TokenType.CARET );
						} else if ( ch == '?' ) {
							retval = new Token( TokenType.QUESTION_MARK );
						} else if ( isNewLineChar( ch ) ) {
							retval = new Token( TokenType.NEWLINE );
						}

						readChar();
					}

					break;
				case ID:  // ID (or unreserved keyword)
					if ( !Character.isLetterOrDigit( ch ) && ch != '_' ) {
						String str = tokenBuilder.toString();
						TokenType tt = UNRESERVED_KEYWORDS.get( str );
						if ( tt != null ) {
							// It is an unreserved keyword
							retval = new Token( tt, str, true );
						} else {
							// It is a normal ID, not corresponding to any keyword
							retval = new Token( TokenType.ID, str );
						}
					}
					break;
				case INT_or_LONG_or_DOUBLE: // INT (or LONG, or DOUBLE)
					if ( ch == 'e' || ch == 'E' ) {
						state = State.SCIENTIFIC_NOTATION_FIRST_EXP_DIGIT;
					} else if ( !Character.isDigit( ch ) && ch != '.' ) {
						if ( ch == 'l' || ch == 'L' ) {
							retval = new Token( TokenType.LONG, tokenBuilder.toString() );
							readChar();
						} else {
							retval = new Token( TokenType.INT, tokenBuilder.toString() );
						}
					} else if ( ch == '.' ) {
						tokenBuilder.append( ch );
						readChar();
						if ( !Character.isDigit( ch ) ) {
							retval = new Token( TokenType.ERROR, tokenBuilder.toString() );
						} else {
							state = State.REAL; // recognized a DOUBLE
						}
					}
					break;
				case STRING:  // STRING
					if ( ch == '"' ) {
						retval = new Token( TokenType.STRING, tokenBuilder.toString().substring( 1 ) );
						readChar();
					} else if ( ch == '\\' ) { // Parse special characters
						readChar();
						if ( ch == '\\' ) {
							tokenBuilder.append( '\\' );
						} else if ( ch == 'n' ) {
							tokenBuilder.append( '\n' );
						} else if ( ch == 't' ) {
							tokenBuilder.append( '\t' );
						} else if ( ch == 'r' ) {
							tokenBuilder.append( '\r' );
						} else if ( ch == '"' ) {
							tokenBuilder.append( '"' );
						} else if ( ch == 'u' ) {
							tokenBuilder.append( 'u' );
						} else {
							throw new IOException( "malformed string: bad \\ usage" );
						}

						stopOneChar = true;
						readChar();
					}
					break;
				case PLUS_or_CHOICE:  // PLUS OR CHOICE
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
				case MULTIPLY_or_MULTIPLY_ASSIGN: // MULTIPLY or MULTIPLY_ASSIGN
					if ( ch == '=' ) {
						retval = new Token( TokenType.MULTIPLY_ASSIGN );
						readChar();
					} else {
						retval = new Token( TokenType.ASTERISK, "*" );
					}
					break;
				case ASSIGN_or_EQUAL: // ASSIGN OR EQUAL
					if ( ch == '=' ) {
						retval = new Token( TokenType.EQUAL );
						readChar();
					} else if ( ch == '>' ) {
						retval = new Token( TokenType.ARROW );
						readChar();
					} else {
						retval = new Token( TokenType.ASSIGN );
					}
					break;
				case PARALLEL_or_LOGIC_OR:  // PARALLEL OR LOGICAL OR
					if ( ch == '|' ) {
						retval = new Token( TokenType.OR );
						readChar();
					} else {
						retval = new Token( TokenType.PARALLEL );
					}
					break;
				case LOGIC_AND: // LOGICAL AND
					if ( ch == '&' ) {
						retval = new Token( TokenType.AND );
						readChar();
					}
					break;
				case LANGLE_or_MINOR_OR_EQUAL_or_DEEP_COPY_LEFT_or_DEEP_COPY_WITH_LINKS_LEFT: // LANGLE OR MINOR_OR_EQUAL OR DEEP_COPY_LEFT
					if ( ch == '=' ) {
						retval = new Token( TokenType.MINOR_OR_EQUAL );
						readChar();
					} else if ( ch == '<' ) {
						state = State.DEEP_COPY_WITH_LINKS_LEFT_or_DEEP_COPY_LEFT;
					} else {
						retval = new Token( TokenType.LANGLE );
					}
					break;
				case RANGLE_or_MINOR_OR_EQUAL: // RANGLE OR MINOR_OR_EQUAL
					if ( ch == '=' ) {
						retval = new Token( TokenType.MAJOR_OR_EQUAL );
						readChar();
					} else {
						retval = new Token( TokenType.RANGLE );
					}
					break;
				case NOT_or_NOT_EQUAL: // NOT OR NOT_EQUAL
					if ( ch == '=' ) {
						retval = new Token( TokenType.NOT_EQUAL );
						readChar();
					} else {
						retval = new Token( TokenType.NOT );
					}
					break;
				case DIVIDE_or_BEGIN_COMMENT_or_LINE_COMMENT: // DIVIDE OR BEGIN_COMMENT OR LINE_COMMENT
					if ( ch == '*' ) { // BEGIN COMMENT
						readChar();
						stopOneChar = true;

						if ( includeDocumentation && ch == '*' ) { // BEGIN DOCUMENTATION
							readChar();

							if ( ch == '!' ) { // Ignore old documentation symbol
								readChar();
							}

							resetTokenBuilder();
							state = State.DOCUMENTATION_FORWARD_BLOCK;
						} else if ( includeDocumentation && ch == '<' ) {
							readChar();
							resetTokenBuilder();
							state = State.DOCUMENTATION_BACKWARD_BLOCK;
						} else {
							state = State.WAITING_FOR_END_COMMENT;
						}
					} else if ( ch == '/' ) {
						readChar();
						stopOneChar = true;
						if ( includeDocumentation && ch == '/' ) {
							readChar();
							resetTokenBuilder();
							state = State.DOCUMENTATION_FORWARD_INLINE;
						} else if ( includeDocumentation && ch == '<' ) {
							readChar();
							resetTokenBuilder();
							state = State.DOCUMENTATION_BACKWARD_INLINE; // BACKWARD DOCUMENTATION COMMENT
						} else {
							state = State.LINE_COMMENT; // NORMAL LINE COMMENT
						}
					} else if ( ch == '=' ) {
						retval = new Token( TokenType.DIVIDE_ASSIGN );
						readChar();
					} else {
						retval = new Token( TokenType.DIVIDE );
					}
					break;
				case WAITING_FOR_END_COMMENT: // WAITING FOR END_COMMENT
					if ( ch == '*' ) {
						readChar();
						stopOneChar = true;
						if ( ch == '/' ) {
							readChar();
							retval = new Token( TokenType.NEWLINE );
						}
					}
					break;
				case MINUS_or_NUMBER_or_POINTS_TO: // MINUS OR (negative) NUMBER OR POINTS_TO
					if ( Character.isDigit( ch ) ) {
						state = State.INT_or_LONG_or_DOUBLE;
					} else if ( ch == '-' ) {
						retval = new Token( TokenType.DECREMENT );
						readChar();
					} else if ( ch == '>' ) {
						retval = new Token( TokenType.POINTS_TO );
						readChar();
					} else if ( ch == '=' ) {
						retval = new Token( TokenType.MINUS_ASSIGN );
						readChar();
					} else if ( ch == '.' ) {
						tokenBuilder.append( ch );
						readChar();
						if ( !Character.isDigit( ch ) ) {
							retval = new Token( TokenType.ERROR, "-." );
						} else {
							state = State.REAL;
						}
					} else {
						retval = new Token( TokenType.MINUS, "-" );
					}
					break;
				case LINE_COMMENT: // LINE_COMMENT: waiting for end of line
					if ( isNewLineChar( ch ) || isOverflowChar( ch ) ) {
						readChar();
						retval = new Token( TokenType.NEWLINE );
					}
					break;
				case DOT: // DOT
					if (ch == '.') {
						retval = new Token( TokenType.DOTDOT );
						readChar();
					} else if ( !Character.isDigit( ch ) ) {
						retval = new Token( TokenType.DOT );
					} else {
						state = State.REAL; // It's a REAL
					}
					break;
				case REAL: // REAL "."[0-9]+
					if ( ch == 'E' || ch == 'e' ) {
						state = State.SCIENTIFIC_NOTATION_FIRST_AFTER_E;
					} else if ( !Character.isDigit( ch ) ) {
						retval = new Token( TokenType.DOUBLE, tokenBuilder.toString() );
					}
					break;
				case SCIENTIFIC_NOTATION_FIRST_AFTER_E: // Scientific notation, first char after 'E'
					if ( ch == '-' || ch == '+' ) {
						state = State.SCIENTIFIC_NOTATION_FIRST_EXP_DIGIT;
					} else if ( Character.isDigit( ch ) ) {
						state = State.SCIENTIFIC_NOTATION_SECOND_TO_END_DIGITS;
					} else {
						retval = new Token( TokenType.ERROR );
					}
					break;
				case SCIENTIFIC_NOTATION_FIRST_EXP_DIGIT: // Scientific notation, first exp. digit
					if ( !Character.isDigit( ch ) ) {
						retval = new Token( TokenType.ERROR );
					} else {
						state = State.SCIENTIFIC_NOTATION_SECOND_TO_END_DIGITS;
					}
					break;
				case SCIENTIFIC_NOTATION_SECOND_TO_END_DIGITS: // Scientific notation: from second digit to end
					if ( !Character.isDigit( ch ) ) {
						retval = new Token( TokenType.DOUBLE, tokenBuilder.toString() );
					}
					break;
				case DOCUMENTATION_FORWARD_BLOCK: // Documentation comment
					if ( ch == '*' ) {
						readChar();
						stopOneChar = true;
						if ( ch == '/' ) {
							readChar();
							if ( !includeDocumentation ) {
								resetTokenBuilder();
								state = State.FIRST_CHARACTER;
							} else {
								retval = new Token( TokenType.DOCUMENTATION_FORWARD, tokenBuilder.toString() );
							}
						}
					}
					break;
				case DOCUMENTATION_FORWARD_INLINE:
					if ( ch == '\r' ) { // IGNORE CARRIGE RETURN (WINDOWS) NEWLINES
						readChar();
					}
					if ( ch == '\n' || isOverflowChar( ch ) ) {
						if ( !includeDocumentation ) {
							readChar();
							resetTokenBuilder();
							state = State.FIRST_CHARACTER;
						} else { // RETURN THE DOCUMENTATION COMMENT
							retval = new Token( TokenType.DOCUMENTATION_FORWARD, tokenBuilder.toString() );
						}
					}
					break;
				case DOCUMENTATION_BACKWARD_BLOCK:
					if ( ch == '*' ) {
						readChar();
						stopOneChar = true;
						if ( ch == '/' ) {
							readChar();
							if ( includeDocumentation ) {
								retval = new Token( TokenType.DOCUMENTATION_BACKWARD, tokenBuilder.toString() );
							} else {
								resetTokenBuilder();
								state = State.FIRST_CHARACTER;
							}
						}
					}
					break;
				case DOCUMENTATION_BACKWARD_INLINE:
					if ( isNewLineChar( ch ) || isOverflowChar( ch ) ) {
						if ( includeDocumentation ) {
							retval = new Token( TokenType.DOCUMENTATION_BACKWARD, tokenBuilder.toString() );
						} else {
							resetTokenBuilder();
							state = State.FIRST_CHARACTER;
						}
					}
					break;
				case DEEP_COPY_WITH_LINKS_LEFT_or_DEEP_COPY_LEFT: // << or <<-
					readChar();
					if ( ch == '-' ) {
						retval = new Token( TokenType.DEEP_COPY_WITH_LINKS_LEFT );
						readChar();
					} else {
						retval = new Token( TokenType.DEEP_COPY_LEFT );
					}
					break;
				// default:
				// 	retval = new Token( TokenType.ERROR, tokenBuilder.toString() );
				// 	break;
			}

			if ( retval == null ) {
				if ( stopOneChar ) {
					stopOneChar = false;
				} else {
					tokenBuilder.append( ch );
					recordTokenEnd();
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

	@Override
	public void close() throws IOException {
		stream.close();
		reader.close();
	}
}
