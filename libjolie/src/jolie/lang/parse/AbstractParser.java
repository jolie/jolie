/***************************************************************************
 *   Copyright (C) 2006-2015 by Fabrizio Montesi <famontesi@gmail.com>     *
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

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import jolie.lang.parse.context.ParsingContext;
import jolie.lang.parse.context.URIParsingContext;


/**
 * Skeleton implementation of a parser based on {@link jolie.lang.parse.Scanner}.
 * Note that the parsing process is not re-entrant.
 * @author Fabrizio Montesi
 * @see Scanner
 */
public abstract class AbstractParser
{
	private Scanner scanner;		// Input scanner.
	protected Scanner.Token token;	///< The current token.
	private final List< Scanner.Token > tokens = new ArrayList<>();
	private final StringBuilder stringBuilder = new StringBuilder( 256 );
	private boolean backup = false;
	private final List< Scanner.Token > backupTokens = new ArrayList<>();
	
	protected final String build( String... args )
	{
		stringBuilder.setLength( 0 );
		for( String s : args ) {
			stringBuilder.append( s );
		}
		return stringBuilder.toString();
	}
	
	/** Constructor
	 * 
	 * @param scanner The scanner to use during the parsing procedure.
	 */
	public AbstractParser( Scanner scanner )
	{
		this.scanner = scanner;
	}
	
	protected final void addTokens( Collection< Scanner.Token > tokens )
	{
		this.tokens.addAll( tokens );
	}
	
	protected final void addToken( Scanner.Token token )
	{
		this.tokens.add( token );
	}
	
	/** Gets a new token.
	 * 
	 * @throws IOException If the internal scanner raises one.
	 */
	protected final void getToken()
		throws IOException
	{
		if ( tokens.size() > 0 ) {
			token = tokens.remove( 0 );
		} else {
			token = scanner.getToken();
		}
		
		if ( backup ) {
			backupTokens.add( token );
		}
	}
	
	/** Recovers the backed up tokens. */
	protected final void recoverBackup()
		throws IOException
	{
		backup = false;
		if ( !backupTokens.isEmpty() ) {
			addTokens( backupTokens );
			backupTokens.clear();
			getToken();
		}
	}
	
	/** Discards the backed up tokens. */
	protected final void discardBackup()
	{
		backup = false;
		backupTokens.clear();
	}
	
	protected void startBackup()
	{
		if ( token != null ) {
			backupTokens.add( token );
		}
		backup = true;
	}
	
	
	/** Gets a new token, and throws an {@link EOFException} if such token is of type {@code jolie.lang.parse.Scanner.TokenType.EOF}.
	 * 
	 * @throws IOException If the internal scanner raises one.
	 * @throws EOFException If the next token is of type {@code jolie.lang.parse.Scanner.Token.EOF}
	 */
	protected final void getTokenNotEOF()
		throws IOException, EOFException
	{
		getToken();
		if ( token.isEOF() ) {
			throw new EOFException();
		}
	}
	
	/**
	 * Returns the Scanner object used by this parser.
	 * @return The Scanner used by this parser.
	 */
	public final Scanner scanner()
	{
		return scanner;
	}
	
	protected final void setScanner( Scanner scanner )
	{
		this.scanner = scanner;
	}
	
	/**
	 * Returns the current {@link ParsingContext} from the underlying {@link Scanner}
	 * @return the current {@link ParsingContext} from the underlying {@link Scanner}
	 */
	public final ParsingContext getContext()
	{
		return new URIParsingContext( scanner.source(), scanner.line() );
	}

	/**
	 * Eats the current token, asserting its type.
	 * Calling eat( type, errorMessage ) is equivalent to call subsequently
	 * tokenAssert( type, errorMessage ) and getToken().
	 * @param type The type of the token to eat.
	 * @param errorMessage The error message to display in case of a wrong token type.
	 * @throws ParserException If the token type is wrong.
	 * @throws IOException If the internal scanner raises one.
	 */
	protected final void eat( Scanner.TokenType type, String errorMessage )
		throws ParserException, IOException
	{
		assertToken( type, errorMessage );
		getToken();
	}
	
	protected final void eatKeyword( String keyword, String errorMessage )
		throws ParserException, IOException
	{
		assertToken( Scanner.TokenType.ID, errorMessage );
		if ( !token.content().equals( keyword ) ) {
			throwException( errorMessage );
		}
		getToken();
	}
	
	/**
	 * Eats the current token, asserting that it is an identifier (or an unreserved keyword).
	 * Calling eatIdentifier( errorMessage ) is equivalent to call subsequently
	 * assertIdentifier( errorMessage ) and getToken().
	 * @param errorMessage The error message to throw as a {@link ParserException} in case the current token is not an identifier.
	 * @throws ParserException If the current token is not an identifier.
	 * @throws IOException If the internal scanner cannot read the next token.
	 */
	protected final void eatIdentifier( String errorMessage )
		throws ParserException, IOException
	{
		assertIdentifier( errorMessage );
		getToken();
	}
	
	/**
	 * Asserts that the current token is an identifier (or an unreserved keyword).
	 * @param errorMessage the error message to throw as a {@link ParserException}
	 * if the current token is not an identifier.
	 * @throws ParserException if the current token is not an identifier.
	 */
	protected final void assertIdentifier( String errorMessage )
		throws ParserException
	{
		if ( !token.isIdentifier() ) {
			throwException( errorMessage );
		}
	}
	
	/**
	 * Asserts the current token type.
	 * @param type The token type to assert.
	 * @param errorMessage The error message to display in case of a wrong token type.
	 * @throws ParserException If the token type is wrong.
	 */
	protected final void assertToken( Scanner.TokenType type, String errorMessage )
		throws ParserException
	{
		if ( token.isNot( type ) ) {
			throwException( errorMessage );
		}
	}
	
	/**
	 * Shortcut to throw a correctly formed ParserException.
	 * @param mesg The message to insert in the ParserException.
	 * @throws ParserException Everytime, as its the purpose of this method.
	 */
	protected final void throwException( String mesg )
		throws ParserException
	{
		String m = mesg + ". Found token type " + token.type().toString();
		if ( !token.content().equals( "" ) ) {
			m += ", token content " + token.content();
		}

		throw new ParserException( getContext(), m );
	}
	
	/**
	 * Shortcut to throw a correctly formed ParserException, getting the message from an existing exception.
	 * @param exception The exception to get the message from.
	 * @throws ParserException Everytime, as its the purpose of this method.
	 */
	protected final void throwException( Exception exception )
		throws ParserException
	{
		throw new ParserException( getContext(), exception.getMessage() );
	}
}
