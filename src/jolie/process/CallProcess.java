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

package jolie.process;
import jolie.Definition;
import jolie.InvalidIdException;


/** Makes a Definition object (a sub-routine) executing its content. 
 * 
 * @see Definition
 * @author Fabrizio Montesi
 */
public class CallProcess implements Process, Optimizable
{
	private Definition definition;
	
	/** Constructor
	 * 
	 * @param defId the identifier of the definition to execute.
	 * @throws InvalidIdException if defId does not identify a Definition object.
	 */
	public CallProcess( String defId )
		throws InvalidIdException
	{
		Definition def = Definition.getById( defId );
		if ( def == null )
			throw new InvalidIdException( defId );
		
		this.definition = def;
	}
	
	/** Executes the definition. */
	public void run()
	{
		definition.run();
	}
	
	public Process optimize()
	{
		return definition.optimize();
	}
}