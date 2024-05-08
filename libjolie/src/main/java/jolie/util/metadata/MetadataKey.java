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

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A metadata key.
 * 
 * @author Fabrizio Montesi
 * @param <T> the type of values associated to this metadata key
 */
public final class MetadataKey< T > {
	private final static Set< String > REGISTRY = new HashSet<>();

	private final String name;
	private final Class< T > typeClass;

	private MetadataKey( String name, Class< T > typeClass ) {
		this.name = name;
		this.typeClass = typeClass;
	}

	public String name() {
		return name;
	}

	public Class< T > typeClass() {
		return typeClass;
	}

	@Override
	public int hashCode() {
		return Objects.hash( name, typeClass );
	}

	@Override
	public boolean equals( Object obj ) {
		if( this == obj )
			return true;

		if( obj == null || getClass() != obj.getClass() )
			return false;

		MetadataKey< ? > other = (MetadataKey< ? >) obj;
		return Objects.equals( name, other.name ) && Objects.equals( typeClass, other.typeClass );
	}

	public static < T > MetadataKey< T > register( String name, Class< T > typeClass ) {
		synchronized( REGISTRY ) {
			if( REGISTRY.contains( name ) ) {
				throw new IllegalStateException( "Metadata key with name " + name + " has already been registered." );
			}

			REGISTRY.add( name );
		}
		return new MetadataKey<>( name, typeClass );
	}
}
