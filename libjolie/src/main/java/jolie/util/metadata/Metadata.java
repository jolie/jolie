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
 * The Metadata class represents a collection of key-value pairs, where the keys are instances of
 * MetadataKey and the values can be of any type. It provides methods to put, get, check for the
 * existence of, and remove key-value pairs. Metadata keys (see {@link MetadataKey}) are used to
 * ensure type safety.
 *
 * @author Fabrizio Montesi
 */
public final class Metadata {
	private final Map< MetadataKey< ? >, Object > data = new HashMap<>();

	/**
	 * Associates the specified value with the specified key in this metadata. If the key already
	 * exists, the previous value will be replaced.
	 *
	 * @param <T> the type of the value
	 * @param key the key with which the specified value is to be associated
	 * @param value the value to be associated with the specified key
	 * @throws IllegalArgumentException if the value is not of the expected type for the key
	 */
	public < T > void put( MetadataKey< T > key, T value ) {
		Objects.requireNonNull( value );
		if( !key.typeClass().isInstance( value ) ) {
			throw new IllegalArgumentException( "Value " + value + " is not of type " + key.typeClass() );
		}
		data.put( key, value );
	}

	/**
	 * Returns the value to which the specified key is mapped, or null if this metadata contains no
	 * mapping for the key.
	 *
	 * @param <T> the type of the value
	 * @param key the key whose associated value is to be returned
	 * @throws ClassCastException if the value is not of the expected type for the key
	 * @return the value to which the specified key is mapped, or null if this metadata contains no
	 *         mapping for the key
	 */
	public < T > T get( MetadataKey< T > key ) {
		return key.typeClass().cast( data.get( key ) );
	}

	/**
	 * Returns true if this metadata contains a mapping for the specified key.
	 *
	 * @param key the key whose presence in this metadata is to be tested
	 * @return true if this metadata contains a mapping for the specified key, false otherwise
	 */
	public boolean containsKey( MetadataKey< ? > key ) {
		return data.containsKey( key );
	}
}
