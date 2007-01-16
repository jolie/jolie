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

import jolie.net.CommProtocol;
import jolie.net.SOAPProtocol;

/** Generic operation declaration
 * 
 * @author Fabrizio Montesi
 *
 */

abstract public class Operation extends AbstractMappedGlobalObject
{
	public Operation( String id )
	{
		super( id );
	}
	
	public CommProtocol getProtocol()
	{
		//return new SODEPProtocol();
		return new SOAPProtocol();
	}
	
	public String value()
	{
		return id();
	}

	public static Operation getById( String id )
		throws InvalidIdException
	{
		Object obj = Interpreter.getObjectById( id );
		if ( !( obj instanceof Operation ) )
			throw new InvalidIdException( id );
		return (Operation)obj;
	}
}