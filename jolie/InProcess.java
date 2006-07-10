/***************************************************************************
 *   Copyright (C) by Fabrizio Montesi <famontesi@gmail.com>               *
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
 ***************************************************************************/

package jolie;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

public class InProcess implements Process
{
	private GlobalVariable var;
	
	public InProcess( String varId )
		throws InvalidIdException
	{
		GlobalVariable findVar = GlobalVariable.getById( varId );
		if ( findVar == null )
			throw new InvalidIdException( varId );
		
		this.var = findVar;
	}
	
	public void run()
	{
		BufferedReader stdin = new BufferedReader( new InputStreamReader( System.in ) );
		try {
			// todo: prevedere un prefisso per il tipo di dato?
			// o tentare direttamente un parseInt?
			var.setStrValue( stdin.readLine() );
		} catch ( IOException e ) {
		}
	}
}
