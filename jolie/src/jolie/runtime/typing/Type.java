/***************************************************************************
 *   Copyright (C) 2009-2015 by Fabrizio Montesi <famontesi@gmail.com>     *
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

package jolie.runtime.typing;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import jolie.lang.NativeType;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.util.Range;

class TypeImpl extends Type
{
	private final Range cardinality;
	private final NativeType nativeType;
	private final Map< String, Type > subTypes;
	
	public TypeImpl(
		NativeType nativeType,
		Range cardinality,
		boolean undefinedSubTypes,
		Map< String, Type > subTypes
	) {
		this.nativeType = nativeType;
		this.cardinality = cardinality;
		if ( undefinedSubTypes ) {
			this.subTypes = null;
		} else {
			this.subTypes = Collections.unmodifiableMap( subTypes );
		}
	}
	
	@Override
	public Map< String, Type > subTypes()
	{
		return subTypes;
	}

	@Override
	public Range cardinality()
	{
		return cardinality;
	}
	
	@Override
	public void cutChildrenFromValue( Value value )
	{
		if ( subTypes != null ) {
			for( String childName : subTypes.keySet() ) {
				value.children().remove( childName );
			}
		}
	}

	@Override
	protected Value cast( Value value, StringBuilder pathBuilder )
		throws TypeCastingException
	{
		castNativeType( value, pathBuilder );
		if ( subTypes != null ) {
			for( Entry< String, Type > entry : subTypes.entrySet() ) {
				castSubType( entry.getKey(), entry.getValue(), value, new StringBuilder( pathBuilder ) );
			}
		}
		
		return value;
	}

	private void castSubType( String typeName, Type type, Value value, StringBuilder pathBuilder )
		throws TypeCastingException
	{
		pathBuilder.append( '.' );
		pathBuilder.append( typeName );

		final boolean hasChildren = value.hasChildren( typeName );
		if ( hasChildren == false && type.cardinality().min() > 0 ) {
			throw new TypeCastingException( "Undefined required child node: " + pathBuilder.toString() );
		} else if ( hasChildren ) {
			final ValueVector vector = value.getChildren( typeName );
			final int size = vector.size();
			if ( type.cardinality().min() > size || type.cardinality().max() < size ) {
				throw new TypeCastingException(
					"Child node " + pathBuilder.toString() + " has a wrong number of occurencies. Permitted range is [" +
					type.cardinality().min() + "," + type.cardinality().max() + "], found " + size
				);
			}

			for( Value v : vector ) {
				type.cast( v, pathBuilder );
			}
		}
	}

	@Override
	protected void check( Value value, StringBuilder pathBuilder )
		throws TypeCheckingException
	{
		if ( checkNativeType( value, nativeType ) == false ) {
			throw new TypeCheckingException( "Invalid native type for node " + pathBuilder.toString() + ": expected " + nativeType + ", found " + (( value.valueObject() == null ) ? "void" : value.valueObject().getClass().getName()) );
		}

		if ( subTypes != null ) {
			final int l = pathBuilder.length();
			for( Entry< String, Type > entry : subTypes.entrySet() ) {
				checkSubType( entry.getKey(), entry.getValue(), value, pathBuilder );
				pathBuilder.setLength( l );
			}
			
			// TODO make this more performant
			for( String childName : value.children().keySet() ) {
				if ( subTypes.containsKey( childName ) == false ) {
					throw new TypeCheckingException( "Unexpected child node: " + pathBuilder.toString() + "." + childName );
				}
			}
		}
	}

	private void checkSubType( String typeName, Type type, Value value, StringBuilder pathBuilder )
		throws TypeCheckingException
	{
		pathBuilder.append( '.' );
		pathBuilder.append( typeName );

		boolean hasChildren = value.hasChildren( typeName );
		if ( hasChildren == false && !(type instanceof TypeChoice)) {
			if (type.cardinality().min() > 0){
				throw new TypeCheckingException( "Undefined required child node: " + pathBuilder.toString() );
			}
		} else if ( hasChildren ) {
			final ValueVector vector = value.getChildren( typeName );
			final int size = vector.size();
			if (!(type instanceof TypeChoice) && checkCardinality(type, size)) {
				throw new TypeCheckingException(
					"Child node " + pathBuilder.toString() + " has a wrong number of occurencies. Permitted range is [" +
					type.cardinality().min() + "," + type.cardinality().max() + "], found " + size
				);
			}

			for( Value v : vector ) {
				type.check( v, pathBuilder );
			}
		}
	}

    private Boolean checkCardinality (Type type, int size){
        return ( type.cardinality().min() > size || type.cardinality().max() < size );
    }

	private void castNativeType( Value value, StringBuilder pathBuilder )
		throws TypeCastingException
	{
		if ( checkNativeType( value, nativeType ) == false ) {
			// ANY is not handled, because checkNativeType returns true for it anyway
			if ( nativeType == NativeType.DOUBLE ) {
				try {
					value.setValue( value.doubleValueStrict() );
				} catch( TypeCastingException e ) {
					throw new TypeCastingException( "Cannot cast node value to " + nativeType.id() + ": " + pathBuilder.toString() );
				}
			} else if ( nativeType == NativeType.INT ) {
				try {
					value.setValue( value.intValueStrict() );
				} catch( TypeCastingException e ) {
					throw new TypeCastingException( "Cannot cast node value to " + nativeType.id() + ": " + pathBuilder.toString() );
				}
			} else if ( nativeType == NativeType.LONG ) {
				try {
					value.setValue( value.longValueStrict() );
				} catch( TypeCastingException e ) {
					throw new TypeCastingException( "Cannot cast node value to " + nativeType.id() + ": " + pathBuilder.toString() );
				}
			} else if ( nativeType == NativeType.BOOL ) {
				try {
					value.setValue( value.boolValueStrict() );
				} catch( TypeCastingException e ) {
					throw new TypeCastingException( "Cannot cast node value to " + nativeType.id() + ": " + pathBuilder.toString() );
				}
			} else if ( nativeType == NativeType.STRING ) {
				try {
					value.setValue( value.strValueStrict() );
				} catch( TypeCastingException e ) {
					throw new TypeCastingException( "Cannot cast node value to " + nativeType.id() + ": " + pathBuilder.toString() );
				}
			} else if ( nativeType == NativeType.VOID ) {
				if ( value.valueObject() != null ) {
					throw new TypeCastingException(
						"Expected " + NativeType.VOID.id() + ", found " +
						value.valueObject().getClass().getSimpleName() +
						": " + pathBuilder.toString()
					);
				}
			} else if ( nativeType == NativeType.RAW ) {
				try {
					value.setValue( value.byteArrayValueStrict() );
				} catch( TypeCastingException e ) {
					throw new TypeCastingException( "Cannot cast node value to " + nativeType.id() + ": " + pathBuilder.toString() );
				}
			} else {
				throw new TypeCastingException(
					"Expected " + nativeType.id() + ", found " +
					value.valueObject().getClass().getSimpleName() +
					": " + pathBuilder.toString()
				);
			}
		}
	}

	private boolean checkNativeType( Value value, NativeType nativeType )
	{
		switch( nativeType ) {
		case ANY:
			return true;
		case DOUBLE:
			return value.isDouble() || value.isInt();
		case LONG:
			return value.isLong() || value.isInt();
		case BOOL:
			return value.isBool();
		case INT:
			return value.isInt();
		case STRING:
			return value.isString();
		case VOID:
			return value.valueObject() == null;
		case RAW:
			return value.isByteArray();
		}
		
		return false;
	}
	
	@Override
	public NativeType nativeType()
	{
		return nativeType;
	}
}

/**
 *
 * @author Fabrizio Montesi
 */
public abstract class Type implements Cloneable
{
	public static final Type UNDEFINED =
		Type.create( NativeType.ANY, new Range( 0, Integer.MAX_VALUE ), true, null );

	public static Type create(
		NativeType nativeType,
		Range cardinality,
		boolean undefinedSubTypes,
		Map< String, Type > subTypes
	) {
		return new TypeImpl( nativeType, cardinality, undefinedSubTypes, subTypes );
	}

	public static TypeLink createLink( String linkedTypeName, Range cardinality )
	{
		return new TypeLink( linkedTypeName, cardinality );
	}
	
	public static Type merge( Type t1, Type t2 )
	{
		final NativeType nativeType = t1.nativeType();
		final Range cardinality = t1.cardinality();
		final Map< String, Type > subTypes = new HashMap<>();
		for( Entry< String, Type > entry : t1.subTypes().entrySet() ) {
			subTypes.put( entry.getKey(), entry.getValue() );
		}
		if ( t2 != null ) {
			for( Entry< String, Type > entry : t2.subTypes().entrySet() ) {
				subTypes.put( entry.getKey(), entry.getValue() );
			}
		}
		return create( nativeType, cardinality, false, subTypes );
	}

	public void check( Value value )
		throws TypeCheckingException
	{
		check( value, new StringBuilder( "#Message" ) );
	}

	public Value cast( Value value )
		throws TypeCastingException
	{
		return cast( value, new StringBuilder( "#Message" ) );
	}

	public abstract void cutChildrenFromValue( Value value );
	public abstract NativeType nativeType();
	public abstract Range cardinality();
	public abstract Map< String, Type > subTypes();
	protected abstract void check( Value value, StringBuilder pathBuilder )
		throws TypeCheckingException;
	protected abstract Value cast( Value value, StringBuilder pathBuilder )
		throws TypeCastingException;

	public static class TypeLink extends Type
	{
		private final String linkedTypeName;
		private final Range cardinality;
		private Type linkedType;

		public TypeLink( String linkedTypeName, Range cardinality )
		{
			this.linkedTypeName = linkedTypeName;
			this.cardinality = cardinality;
		}
		
		@Override
		public Map< String, Type > subTypes()
		{
			return linkedType.subTypes();
		}
		
		@Override
		public NativeType nativeType()
		{
			return linkedType.nativeType();
		}

		public String linkedTypeName()
		{
			return linkedTypeName;
		}

		public void setLinkedType( Type linkedType )
		{
			this.linkedType = linkedType;
		}
		
		@Override
		public void cutChildrenFromValue( Value value )
		{
			linkedType.cutChildrenFromValue( value );
		}

		@Override
		public Range cardinality()
		{
			return cardinality;
		}

		@Override
		protected void check( Value value, StringBuilder pathBuilder )
			throws TypeCheckingException
		{
			linkedType.check( value, pathBuilder );
		}

		@Override
		protected Value cast( Value value, StringBuilder pathBuilder )
			throws TypeCastingException
		{
			return linkedType.cast( value, pathBuilder );
		}
	}
}
