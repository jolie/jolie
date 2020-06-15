/*
 * Copyright (C) 2008-2020 Fabrizio Montesi <famontesi@gmail.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package jolie.lang;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Fabrizio Montesi
 */
public enum NativeType {
	// UNDEFINED( "undefined" ),
	STRING( "string" ), INT( "int" ), LONG( "long" ), BOOL( "bool" ), DOUBLE( "double" ), VOID( "void" ), RAW(
		"raw" ), ANY( "any" );

	private final static Map< String, NativeType > idMap = new HashMap<>();

	static {
		for( NativeType type : NativeType.values() ) {
			idMap.put( type.id(), type );
		}
	}

	private final String id;


	private NativeType( String id ) {
		this.id = id;
	}

	public String id() {
		return id;
	}

	public static NativeType fromString( String id ) {
		return idMap.get( id );
	}

	public static boolean isNativeTypeKeyword( String id ) {
		return idMap.containsKey( id );
	}

}
