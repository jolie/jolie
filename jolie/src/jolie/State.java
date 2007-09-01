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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Vector;

import jolie.net.CommMessage;
import jolie.runtime.GlobalVariable;
import jolie.runtime.Value;

public class State implements Cloneable
{
	private HashMap< GlobalVariable, Vector< Value > > stateMap;
	
	private State( HashMap< GlobalVariable, Vector< Value > > map )
	{
		stateMap = map;
	}
	
	public State()
	{
		stateMap = new HashMap< GlobalVariable, Vector< Value > > ();
	}
	
	private Vector< Value > deepCopyValues( Vector< Value > oldValues )
	{
		Vector< Value > vec = new Vector< Value >();
		Value newVal;
		for( Value v : oldValues ) {
			newVal = new Value();
			newVal.deepCopy( v );
			vec.add( newVal );
		}

		return vec;
	}
	
	public State clone()
	{
		HashMap< GlobalVariable, Vector< Value > > map =
				new HashMap< GlobalVariable, Vector< Value > > ();
		
		for( GlobalVariable var : stateMap.keySet() )
			map.put( var, deepCopyValues( getValues( var ) ) );
		
		return new State( map );
	}
	
	public void setValues( GlobalVariable var, Vector< Value > newValues )
	{
		stateMap.put( var, newValues );
	}

	public Vector< Value > getValues( GlobalVariable var )
	{
		Vector< Value > values;
		if ( (values=stateMap.get( var )) == null ) {
			values = new Vector< Value >();
			stateMap.put( var, values );
		}
		return values;
	}
	
	public boolean checkCorrelation( List< GlobalVariable > vars, CommMessage message )
	{
		Iterator< Value > it = message.iterator();
		Value val;
		Value varValue;
		for( GlobalVariable var : vars ) {
			val = it.next();
			if ( Interpreter.correlationSet().contains( var ) && getValues( var ).size() > 0 ) {
				varValue = getValues( var ).elementAt( 0 ); // TODO - this does not work anymore with structured data!
				try {
					if ( varValue.isDefined() && (
								varValue.type() != val.type() ||
								(varValue.isInt() && varValue.intValue() != val.intValue()) ||
								(varValue.isString() && !varValue.strValue().equals( val.strValue() ))
							)
						)
						return false;
				} catch( NoSuchElementException e ) {
					return false;
				}
			}
		}
		
		return true;
	}
}
