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


package jolie.runtime;

import java.util.Vector;

import jolie.process.TransformationReason;

/** Provides the support for a "logical and" chain of other conditions. 
 * 
 * @author Fabrizio Montesi
 * @see Condition
 */
public class AndCondition implements Condition
{
	final private Vector< Condition > children;
	
	/** Constructor */
	public AndCondition()
	{
		children = new Vector< Condition >();
	}
	
	public Condition cloneCondition( TransformationReason reason )
	{
		AndCondition ret = new AndCondition();
		for( Condition c : children )
			ret.addChild( c.cloneCondition( reason ) );
		return ret;
	}
	
	/** Applies the "logical and" rule.
	 * Implemented as short and: starting from left, the first condition which
	 * evaluates as false makes this "logical and" condition 
	 * evaluation returning false, without checking the other conditions.  
	 * @return true if every condition is satisfied, false otherwise.
	 */
	public boolean evaluate()
	{
		for( Condition condition : children )
			if ( condition.evaluate() == false )
				return false;

		return true;
	}
	
	/** Adds a condition to the "logical and" group.
	 * The condition will be checked by the evaluate method. 
	 * @param condition The condition to add.
	 */
	public void addChild( Condition condition )
	{
		children.add( condition );
	}
}
