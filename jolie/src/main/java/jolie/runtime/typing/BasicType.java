/*
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

package jolie.runtime.typing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import jolie.lang.NativeType;
import jolie.lang.parse.ast.types.BasicTypeDefinition;
import jolie.lang.parse.ast.types.refinements.BasicTypeRefinement;
import jolie.runtime.Value;

public class BasicType< T > {
	private static final Map< NativeType, BasicType< ? > > PURE_BASIC_TYPES = new EnumMap<>( NativeType.class );
	private static final Predicate< Value > ANY_PREDICATE = v -> true;
	private static final Predicate< Value > INT_PREDICATE = Value::isInt;
	private static final Predicate< Value > LONG_PREDICATE = v -> v.isLong() || v.isInt();
	private static final Predicate< Value > DOUBLE_PREDICATE = v -> v.isDouble() || v.isInt();
	private static final Predicate< Value > STRING_PREDICATE = Value::isString;
	private static final Predicate< Value > BOOL_PREDICATE = Value::isBool;
	private static final Predicate< Value > VOID_PREDICATE = v -> v.valueObject() == null;
	private static final Predicate< Value > RAW_PREDICATE = Value::isByteArray;

	static {
		PURE_BASIC_TYPES.put( NativeType.ANY,
			new BasicType<>( NativeType.ANY, ANY_PREDICATE, Collections.emptyList(), Value::valueObject ) );
		PURE_BASIC_TYPES.put( NativeType.INT,
			new BasicType<>( NativeType.INT, INT_PREDICATE, Collections.emptyList(), Value::intValue ) );
		PURE_BASIC_TYPES.put( NativeType.LONG,
			new BasicType<>( NativeType.LONG, LONG_PREDICATE, Collections.emptyList(), Value::longValue ) );
		PURE_BASIC_TYPES.put( NativeType.DOUBLE,
			new BasicType<>( NativeType.DOUBLE, DOUBLE_PREDICATE, Collections.emptyList(), Value::doubleValue ) );
		PURE_BASIC_TYPES.put( NativeType.STRING,
			new BasicType<>( NativeType.STRING, STRING_PREDICATE, Collections.emptyList(), Value::strValue ) );
		PURE_BASIC_TYPES.put( NativeType.BOOL,
			new BasicType<>( NativeType.BOOL, BOOL_PREDICATE, Collections.emptyList(), Value::boolValue ) );
		PURE_BASIC_TYPES.put( NativeType.RAW,
			new BasicType<>( NativeType.RAW, RAW_PREDICATE, Collections.emptyList(), Value::byteArrayValue ) );
		PURE_BASIC_TYPES.put( NativeType.VOID,
			new BasicType<>( NativeType.VOID, VOID_PREDICATE, Collections.emptyList(), v -> {
				throw new IllegalStateException( "void values cannot be refined" );
			} ) );
	}

	private final NativeType nativeType;
	private final Predicate< Value > nativeTypePredicate;
	private final List< BasicTypeRefinement< ? super T > > refinements;
	private final Function< Value, T > mapper;

	private BasicType( NativeType nativeType, Predicate< Value > nativeTypePredicate,
		List< BasicTypeRefinement< ? super T > > refinements, Function< Value, T > mapper ) {
		this.nativeType = nativeType;
		this.nativeTypePredicate = nativeTypePredicate;
		this.refinements = refinements;
		this.mapper = mapper;
	}

	public NativeType nativeType() {
		return nativeType;
	}

	private static < U, T extends U > void checkRefinement( T nativeValue, BasicTypeRefinement< U > refinement,
		Supplier< String > pathSupplier ) throws TypeCheckingException {
		if( !refinement.checkValue( nativeValue ) ) {
			throw new TypeCheckingException( "Invalid basic value for node " + pathSupplier.get()
				+ ": does not respect the refinement " + refinement.getDocumentation() );
		}
	}

	public void check( Value value, Supplier< String > pathSupplier )
		throws TypeCheckingException {
		if( !nativeTypePredicate.test( value ) ) {
			throw new TypeCheckingException(
				"Invalid native type for node " + pathSupplier.get() + ": expected " + nativeType + ", found "
					+ ((value.valueObject() == null) ? "void"
						: value.valueObject().getClass().getName() + "(" + value.strValue() + ")") );
		}
		if( !refinements.isEmpty() ) {
			T nativeValue = mapper.apply( value );
			for( BasicTypeRefinement< ? super T > refinement : refinements )
				checkRefinement( nativeValue, refinement, pathSupplier );
		}
	}

	@SuppressWarnings( "unchecked" )
	private static < T > List< BasicTypeRefinement< ? super T > > fromBasicTypeDefinitionRefinements(
		final List< BasicTypeRefinement< ? > > refinements ) {
		List< BasicTypeRefinement< ? super T > > ret = new ArrayList<>();
		for( BasicTypeRefinement< ? > refinement : refinements )
			ret.add( (BasicTypeRefinement< ? super T >) refinement );
		return ret;
	}

	private static < T > BasicType< T > fromBasicTypeDefinitionInternal( BasicType< T > pureBasicType,
		List< BasicTypeRefinement< ? > > refinements ) {
		if( refinements.isEmpty() ) {
			return pureBasicType;
		} else {
			return new BasicType<>(
				pureBasicType.nativeType,
				pureBasicType.nativeTypePredicate,
				fromBasicTypeDefinitionRefinements( refinements ),
				pureBasicType.mapper );
		}
	}

	public static BasicType< ? > fromBasicTypeDefinition( BasicTypeDefinition basicTypeDefinition ) {
		return fromBasicTypeDefinitionInternal( PURE_BASIC_TYPES.get( basicTypeDefinition.nativeType() ),
			basicTypeDefinition.refinements() );
	}
}
