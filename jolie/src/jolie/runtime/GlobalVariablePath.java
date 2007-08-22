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

import java.util.List;
import java.util.Vector;

import jolie.util.Pair;

public class GlobalVariablePath implements Expression
{
	private GlobalVariable variable;
	private Expression varElement;
	private List< Pair< String, Expression > > path;
	private Expression attribute;
	
	public static GlobalVariablePath create(
			String varId,
			Expression varElement,
			List< Pair< String, Expression > > path,
			Expression attribute
			)
		throws InvalidIdException
	{
		GlobalVariablePath ret = new GlobalVariablePath( GlobalVariable.getById( varId ) );
		ret.varElement = varElement;
		ret.path = path;
		ret.attribute = attribute;
		return ret;
	}
	
	private GlobalVariablePath( GlobalVariable variable )
	{
		this.variable = variable;
	}
	
	/*
	 * @todo -- refine!
	 */
	public Value getValue()
	{
		int index = varElement.evaluate().intValue();
		Vector< Value > vals = variable.values();
		if ( index >= vals.size() ) {
			for( int i = vals.size(); i <= index; i++ )
				vals.add( new Value() );
		}
		Value ret = vals.elementAt( index );

		if ( !path.isEmpty() ) {
			Vector< Value > children;
			for( Pair< String, Expression > pair : path ) {
				children = ret.getChildren( pair.key() );
				index = pair.value().evaluate().intValue();
				if ( index >= children.size() ) {
					for( int i = children.size(); i <= index; i++ )
						children.add( new Value() );
				}
				ret = children.elementAt( index );
			}
		}
		
		if ( attribute != null )
			ret = ret.getAttribute( attribute.evaluate().strValue() );

		return ret;
	}
	
	public Value evaluate()
	{
		return getValue();
	}
}
