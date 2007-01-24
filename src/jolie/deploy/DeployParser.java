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

package jolie.deploy;

import java.io.IOException;

import jolie.AbstractParser;
import jolie.GlobalLocation;
import jolie.InvalidIdException;
import jolie.ParserException;
import jolie.Scanner;

public class DeployParser extends AbstractParser
{
	public DeployParser( Scanner scanner )
	{
		super( scanner );
	}
	
	public void parse()
		throws IOException, ParserException
	{
		getToken();
		parseLocations();
	}
	
	private void parseLocations()
		throws IOException, ParserException
	{
		int checkedLocations = 0;
		boolean stop = false;
		GlobalLocation loc;

		if ( token.type() == Scanner.TokenType.LOCATIONS ) {
			getToken();
			eat( Scanner.TokenType.LCURLY, "{ expected" );
			while ( token.type() != Scanner.TokenType.RCURLY && !stop ) {
				tokenAssert( Scanner.TokenType.ID, "location id expected" );
				try {
					loc = GlobalLocation.getById( token.content() );
					getToken();
					eat( Scanner.TokenType.ASSIGN, "= expected" );
					tokenAssert( Scanner.TokenType.STRING, "location value expected" );
					loc.setValue( token.content() );
					checkedLocations++;
				} catch( InvalidIdException e ) {
					throwException( "invalid location identifier" );
				}
				getToken();
				if ( token.type() != Scanner.TokenType.COMMA )
					stop = true;
				else
					getToken();
			}
			eat( Scanner.TokenType.RCURLY, "} expected" );
		}
		
		if ( checkedLocations != GlobalLocation.getAll().size() )
			throwException( "locations deployment block is not complete" ); 
	}
}
