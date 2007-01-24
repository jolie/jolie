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

package jolie;

import java.io.IOException;

/** Skeleton implementation of a JOLIE Scanner based parser.
 * 
 * @author Fabrizio Montesi
 * @see Scanner
 */
public abstract class AbstractParser
{
	private Scanner scanner;		// Input scanner.
	protected Scanner.Token token;	///< The current token.
	
	/** Starts the parsing procedure.
	 * 
	 * @throws IOException If a scanner level error occurs.
	 * @throws ParserException If a parser level error occurs.
	 */
	abstract public void parse()
		throws IOException, ParserException;

	/** Constructor
	 * 
	 * @param scanner The scanner to use during the parsing procedure.
	 */
	public AbstractParser( Scanner scanner )
	{
		this.scanner = scanner;
	}
	
	/** Gets a new token and puts
	 * 
	 * @throws IOException If the internal scanner raises one.
	 */
	protected void getToken()
		throws IOException
	{
		token = scanner.getToken();
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
	protected void eat( Scanner.TokenType type, String errorMessage )
		throws ParserException, IOException
	{
		tokenAssert( type, errorMessage );
		getToken();
	}
	
	/**
	 * Asserts the current token type.
	 * @param type The token type to assert.
	 * @param errorMessage The error message to display in case of a wrong token type.
	 * @throws ParserException If the token type is wrong.
	 */
	protected void tokenAssert( Scanner.TokenType type, String errorMessage )
		throws ParserException
	{
		if ( !token.isA( type ) )
			throwException( errorMessage );
	}
	
	/**
	 * Shortcut to throw a correctly formed ParserException.
	 * @param mesg The message to insert in the ParserException.
	 * @throws ParserException Everytime, as its the function of this method.
	 */
	protected void throwException( String mesg )
		throws ParserException
	{
		throw new ParserException( scanner.sourceName(), scanner.line(), mesg );
	}
}
