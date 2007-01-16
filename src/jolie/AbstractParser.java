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

/** Skeleton implementation of a JOLIE scanner based parser.
 * 
 * @author Fabrizio Montesi
 * @see Scanner
 */
public abstract class AbstractParser
{
	private Scanner scanner;		// Input scanner
	protected Scanner.Token token;	// Current token
	
	abstract public void parse()
		throws IOException, ParserException;

	public AbstractParser( Scanner scanner )
	{
		this.scanner = scanner;
	}
	
	protected void getToken()
		throws IOException
	{
		token = scanner.getToken();
	}

	protected void eat( Scanner.TokenType type, String errorMessage )
		throws ParserException, IOException
	{
		tokenAssert( type, errorMessage );
		getToken();
	}
	
	protected void tokenAssert( Scanner.TokenType type, String errorMessage )
		throws ParserException
	{
		if ( token.type() != type )
			throwException( errorMessage );
	}
	
	protected void throwException( String mesg )
		throws ParserException
	{
		throw new ParserException( scanner.sourceName(), scanner.line(), mesg );
	}
}
