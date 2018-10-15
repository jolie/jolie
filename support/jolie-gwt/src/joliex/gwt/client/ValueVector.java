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

package joliex.gwt.client;

import com.google.gwt.user.client.rpc.IsSerializable;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ValueVector implements Serializable, Iterable< Value >, IsSerializable
{
	private List< Value > values = new ArrayList< Value >();
	
	public ValueVector()
	{}
	
	public Value first()
	{
		return get( 0 );
	}
	
	public Iterator< Value > iterator()
	{
		return values.iterator();
	}
	
	public Value get( int i )
	{
		if ( i >= values.size() ) {
			for( int k = values.size(); k <= i; k++ )
				values.add( new Value() );
		}
		return values.get( i );
	}
	
	public int size()
	{
		return values.size();
	}
	
	public void set( int i, Value value )
	{
		if ( i >= values.size() ) {
			for( int k = values.size(); k < i; k++ )
				values.add( new Value() );
			values.add( value );
		} else {
			values.set( i, value);
		}
	}
	
	public void add( Value value )
	{
		values.add( value );
	}
}
