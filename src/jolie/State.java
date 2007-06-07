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

import jolie.net.CommMessage;
import jolie.runtime.GlobalVariable;
import jolie.runtime.Value;
import jolie.runtime.Variable;

public class State implements Cloneable
{
	private HashMap< GlobalVariable, Value > stateMap;
	
	private State( HashMap< GlobalVariable, Value > map )
	{
		stateMap = map;
	}
	
	public State()
	{
		stateMap = new HashMap< GlobalVariable, Value > ();
	}
	
	public State clone()
	{
		HashMap< GlobalVariable, Value > map =
				new HashMap< GlobalVariable, Value > ();
		
		for( GlobalVariable var : stateMap.keySet() )
			map.put( var, new Value( getValue( var  ) ) );
		
		return new State( map );
	}

	public Value getValue( GlobalVariable var )
	{
		Value value;
		if ( (value=stateMap.get( var )) == null ) {
			value = new Value();
			stateMap.put( var, value );
		}
		return value;
	}
	
	public boolean checkCorrelation( List< GlobalVariable > vars, CommMessage message )
	{
		Iterator< Variable > it = message.iterator();
		Variable val;
		Value varValue;
		for( GlobalVariable var : vars ) {
			if ( Interpreter.correlationSet().contains( var ) ) {
				varValue = getValue( var );
				try {
					val = it.next();
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
