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

import jolie.runtime.GlobalVariable;
import jolie.runtime.ValueVector;

public class State implements Cloneable
{
	private HashMap< GlobalVariable, ValueVector > stateMap;
	
	private State( HashMap< GlobalVariable, ValueVector > map )
	{
		stateMap = map;
	}
	
	public State()
	{
		stateMap = new HashMap< GlobalVariable, ValueVector > ();
	}
	
	public State clone()
	{
		HashMap< GlobalVariable, ValueVector > map =
				new HashMap< GlobalVariable, ValueVector > ();
		
		synchronized( this ) {
			for( GlobalVariable var : stateMap.keySet() )
				map.put( var, ValueVector.createClone( getValues( var ) ) );
		}
		
		return new State( map );
	}
	
	public void setValues( GlobalVariable var, ValueVector newValues )
	{
		synchronized( this ) {
			stateMap.put( var, newValues );
		}
	}

	public ValueVector getValues( GlobalVariable var )
	{
		ValueVector values = null;
		synchronized( this ) {
			values = stateMap.get( var );
			if ( values == null ) {
				values = ValueVector.create();
				stateMap.put( var, values );
			}
		}
		return values;
	}
}
