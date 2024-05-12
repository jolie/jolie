/*
 * Copyright (C) 2024 Fabrizio Montesi <famontesi@gmail.com>
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

package jolie.util.metadata;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Utility class for storing metadata across extensions.
 * 
 * @author Fabrizio Montesi
 */
public final class Metadata {
	private final Map< MetadataKey< ? >, Object > data = new HashMap<>();

	public < T > void put( MetadataKey< T > key, T value ) {
		Objects.requireNonNull( value );
		if( !key.typeClass().isInstance( value ) ) {
			throw new IllegalArgumentException( "Value " + value + " is not of type " + key.typeClass() );
		}
		data.put( key, value );
	}

	public < T > T get( MetadataKey< T > key ) {
		return key.typeClass().cast( data.get( key ) );
	}

	public boolean containsKey( MetadataKey< ? > key ) {
		return data.containsKey( key );
	}

	public < T > T remove( MetadataKey< T > key ) {
		return key.typeClass().cast( data.remove( key ) );
	}
}
