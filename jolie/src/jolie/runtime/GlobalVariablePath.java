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

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import jolie.Interpreter;
import jolie.util.Pair;

public class GlobalVariablePath implements Expression
{
	private GlobalVariable variable;
	private Expression varElement; // may be null
	private List< Pair< String, Expression > > path; // Expression may be null
	private Expression attribute; // may be null
	
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
	
	@SuppressWarnings("unchecked")
	private Object followPath( boolean forceValue, GlobalVariablePath pathToPoint )
	{
		int index = 0;
		
		if ( varElement == null ) {
			if ( path.isEmpty() ) {
				if ( pathToPoint != null ) {
					Interpreter.setValues(
							variable,
							(Vector< Value >) pathToPoint.followPath( false, null )
							);
					return null;
				} else if ( !forceValue ) {
					return variable.values();
				}
			}
		} else {
			index = varElement.evaluate().intValue();
		}		

		Vector< Value > vals = variable.values();
		if ( index >= vals.size() ) {
			for( int i = vals.size(); i <= index; i++ )
				vals.add( Value.createValue() );
		}
		if ( path.isEmpty() && pathToPoint != null ) {
			vals.setElementAt( Value.createLink( pathToPoint ), index );
			return null;
		}
		Value currVal = vals.elementAt( index );

		Vector< Value > children;
		Iterator< Pair< String, Expression > > it = path.iterator();
		Pair< String, Expression > pair;
		index = 0;
		while( it.hasNext() ) {
			pair = it.next();
			children = currVal.getChildren( pair.key() );
			if ( pair.value() == null ) {
				if ( it.hasNext() || attribute != null || forceValue ) {
					index = 0;
				} else {
					if ( pathToPoint != null ) {
						currVal.children().put(
								pair.key(),
								(Vector< Value >) pathToPoint.followPath( false, null )
								);
						return null;
					}
					return children; 
				}
			} else {
				index = pair.value().evaluate().intValue();
			}
			if ( index >= children.size() ) {
				for( int i = children.size(); i <= index; i++ )
					children.add( Value.createValue() );
			}
			if ( !it.hasNext() && pathToPoint != null && attribute == null ) {
				children.setElementAt(
						Value.createLink( pathToPoint ),
						index
						);
				return null;
			}
			currVal = children.elementAt( index );
		}
		
		if ( attribute != null ) {
			if ( pathToPoint == null ) {
				currVal = currVal.getAttribute( attribute.evaluate().strValue() );
			} else {
				currVal.attributes().put(
						attribute.evaluate().strValue(),
						Value.createLink( pathToPoint )
						);
				return null;
			}
		}

		return currVal;
	}
	
	public Value getValue()
	{
		return (Value) followPath( true, null );
	}
	
	/**
	 * @todo This can cast a ClassCastException. Handle that.
	 */
	public void makePointer( GlobalVariablePath rightPath )
	{
		followPath( false, rightPath ); 
	}
	
	@SuppressWarnings("unchecked")
	public void deepCopy( GlobalVariablePath rightPath )
	{
		Object myObj = followPath( false, null );
		if ( myObj instanceof Value ) {
			Value myVal = (Value) myObj;
			myVal.deepCopy( (Value)rightPath.followPath( true, null ) );
		} else {
			Vector< Value > myVec = (Vector< Value >) myObj;
			Vector< Value > rightVec = (Vector< Value >) rightPath.followPath( false, null );
			myVec.clear();
			Value myVal;
			for( Value val : rightVec ) {
				myVal = Value.createValue();
				myVal.deepCopy( val );
				myVec.add( myVal );
			}
		}
		followPath( false, rightPath );
	}
	
	public Value evaluate()
	{
		return getValue();
	}
}
