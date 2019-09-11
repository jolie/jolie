/***************************************************************************
 *   Copyright (C) 2009 by Fabrizio Montesi <famontesi@gmail.com>          *
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

package jolie.net.http;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Fabrizio Montesi
 */
public enum Method
{
	POST( "POST" ),
	GET( "GET" ),
	PUT( "PUT" ),
	DELETE( "DELETE" );

	private final static Map< String, Method > idMap = new ConcurrentHashMap<>();

	static {
		for( Method type : Method.values() ) {
			idMap.put( type.id(), type );
		}
	}

	private final String id;
	
	private Method( String id )
	{
		this.id = id;
	}

	public String id()
	{
		return id;
	}

	public static Method fromString( String id )
		throws UnsupportedMethodException
	{
		Method m = idMap.get( id.toUpperCase() );
		if ( m == null ) {
			throw new UnsupportedMethodException( id );
		}
		return m;
	}
}
