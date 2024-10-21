/*
 * Copyright (C) 2020 Claudio Guidi <cguidi@italianasoftware.com>
 * Copyright (C) 2020 Fabrizio Montesi <famontesi@gmail.com>
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

package jolie.lang.parse.ast.types;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jolie.lang.NativeType;
import jolie.lang.parse.ast.types.refinements.BasicTypeRefinement;

public class BasicTypeDefinition implements Serializable {
	private final NativeType nativeType;
	private final List< BasicTypeRefinement< ? extends Object > > refinements;
	private static final Map< NativeType, BasicTypeDefinition > PURE_BASIC_TYPE_DEFINITIONS = new HashMap<>();

	static {
		for( NativeType nativeType : NativeType.values() ) {
			PURE_BASIC_TYPE_DEFINITIONS.put( nativeType,
				new BasicTypeDefinition( nativeType, Collections.emptyList() ) );
		}
	}

	private BasicTypeDefinition( NativeType nativeType, List< BasicTypeRefinement< ? > > refinements ) {
		this.nativeType = nativeType;
		this.refinements = Collections.unmodifiableList( refinements );
	}

	public NativeType nativeType() {
		return nativeType;
	}

	public List< BasicTypeRefinement< ? extends Object > > refinements() {
		return refinements;
	}

	public boolean checkBasicTypeEqualness( BasicTypeDefinition basicTypeDefinition ) {
		if( refinements.size() > 1 || basicTypeDefinition.refinements.size() > 1 ) {
			throw new IllegalArgumentException(
				"Checking for equality of basic types with more than one refinement is unsupported" );
		}

		return nativeType.equals( basicTypeDefinition.nativeType ) &&
			refinements.size() == basicTypeDefinition.refinements.size() &&
			refinements.stream()
				.allMatch( refinement -> checkSingleTypeRefinement( refinement, basicTypeDefinition.refinements() ) );
	}

	// TODO: update this when we allow for multiple refinements. (It's broken when one uses the same
	// refinement twice.)
	private static boolean checkSingleTypeRefinement( BasicTypeRefinement< ? > basicTypeRefinement,
		List< BasicTypeRefinement< ? > > targetList ) {
		boolean returnValue = false;
		BasicTypeRefinement< ? > foundBasicTypeRefinement = targetList.stream()
			.filter( btr -> btr.getClass().equals( basicTypeRefinement.getClass() ) ).findFirst().get();
		if( foundBasicTypeRefinement != null ) {
			returnValue = basicTypeRefinement.checkEqualness( foundBasicTypeRefinement );
		}
		return returnValue;
	}

	public static BasicTypeDefinition of( NativeType nativeType ) {
		Objects.requireNonNull( nativeType, "native type in BasicType must not be null" );

		return PURE_BASIC_TYPE_DEFINITIONS.get( nativeType );
	}

	public static BasicTypeDefinition of( NativeType nativeType, List< BasicTypeRefinement< ? > > refinements ) {
		Objects.requireNonNull( nativeType, "native type in BasicType must not be null" );
		if( refinements.isEmpty() ) {
			return of( nativeType );
		} else {
			return new BasicTypeDefinition( nativeType, refinements );
		}
	}
}
