/***************************************************************************
 *   Copyright (C) 2011 by Fabrizio Montesi <famontesi@gmail.com>          *
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

package jolie.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Fabrizio Montesi
 */
public abstract class MultiMap< K, V >
{
	private final Map< K, Collection< V > > map = new HashMap<>();

	protected abstract Collection< V > createCollection();

	public boolean containsKey( K key )
	{
		return map.containsKey( key );
	}

	public Collection< V > get( K key )
	{
		Collection< V > r = map.get( key );
		if ( r == null ) {
			return Collections.emptyList();
		}
		return r;
	}

	public V put( K key, V value )
	{
		Collection< V > r = map.get( key );
		if ( r == null ) {
			r = createCollection();
			map.put( key, r );
		}
		r.add( value );
		return value;
	}

	public void clear()
	{
		map.clear();
	}

	public Set< K > keySet()
	{
		return map.keySet();
	}
}
