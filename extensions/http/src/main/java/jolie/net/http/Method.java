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
public enum Method {
	POST( "POST" ), GET( "GET" ), PUT( "PUT" ), DELETE( "DELETE" ), OPTIONS( "OPTIONS" ), PATCH( "PATCH" );

	private final static Map< String, Method > ID_MAP = new ConcurrentHashMap<>();

	static {
		for( Method type : Method.values() ) {
			ID_MAP.put( type.id(), type );
		}
		ID_MAP.put( "HEAD", GET ); // RFC 9110 section 9.3.2
	}

	private final String id;

	Method( String id ) {
		this.id = id;
	}

	public String id() {
		return id;
	}

	public static Method fromString( String id )
		throws UnsupportedMethodException {
		Method m = ID_MAP.get( id.toUpperCase() );
		if( m == null ) {
			throw new UnsupportedMethodException( id );
		}
		return m;
	}
}
