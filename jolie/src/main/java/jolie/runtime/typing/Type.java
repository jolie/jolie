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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import jolie.lang.NativeType;
import jolie.lang.parse.ast.types.BasicTypeDefinition;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.util.Range;

class TypeImpl extends Type {
	private final Range cardinality;
	private final BasicType< ? > basicType;
	private final Map< String, Type > subTypes;

	public TypeImpl(
		BasicType< ? > basicType,
		Range cardinality,
		boolean undefinedSubTypes,
		Map< String, Type > subTypes ) {
		this.basicType = basicType;
		this.cardinality = cardinality;
		this.subTypes = undefinedSubTypes ? null : subTypes;
	}

	@Override
	public Type findSubType( String key ) {
		return (subTypes != null) ? subTypes.get( key ) : null;
	}

	@Override
	public Type findSubType( String key, Value value ) {

		Type checkSubType = findSubType( key );
		if( checkSubType == null ) {
			return null;
		}

		try {
			checkSubType.check( value );
			return checkSubType;
		} catch( TypeCheckingException e ) {
			return null;
		}
	}

	@Override
	public Range cardinality() {
		return cardinality;
	}

	protected Map< String, Type > subTypes() {
		return subTypes;
	}

	protected BasicType< ? > basicType() {
		return basicType;
	}

	@Override
	public void cutChildrenFromValue( Value value ) {
		if( subTypes != null ) {
			for( String childName : subTypes.keySet() ) {
				value.children().remove( childName );
			}
		}
	}

	@Override
	protected Value cast( Value value, StringBuilder pathBuilder )
		throws TypeCastingException {
		castNativeType( value, pathBuilder );
		if( subTypes != null ) {
			for( Entry< String, Type > entry : subTypes.entrySet() ) {
				castSubType( entry.getKey(), entry.getValue(), value, new StringBuilder( pathBuilder ) );
			}
		}

		return value;
	}

	private void castSubType( String typeName, Type type, Value value, StringBuilder pathBuilder )
		throws TypeCastingException {
		pathBuilder.append( '.' )
			.append( typeName );

		final boolean hasChildren = value.hasChildren( typeName );
		if( !hasChildren && type.cardinality().min() > 0 ) {
			throw new TypeCastingException( "Undefined required child node: " + pathBuilder.toString() );
		} else if( hasChildren ) {
			final ValueVector vector = value.getChildren( typeName );
			final int size = vector.size();
			if( type.cardinality().min() > size || type.cardinality().max() < size ) {
				throw new TypeCastingException(
					"Child node " + pathBuilder.toString() + " has a wrong number of occurencies. Permitted range is ["
						+
						type.cardinality().min() + "," + type.cardinality().max() + "], found " + size );
			}

			for( Value v : vector ) {
				type.cast( v, pathBuilder );
			}
		}
	}

	@Override
	public Optional< Type > getMinimalType( Value value ) {
		try {
			check( value );
			return Optional.of( this );
		} catch( TypeCheckingException ex ) {
			return Optional.empty();
		}
	}

	@Override
	protected void check( Value value, StringBuilder pathBuilder )
		throws TypeCheckingException {
		basicType.check( value, pathBuilder::toString );

		if( subTypes != null ) {
			final int l = pathBuilder.length();
			for( Entry< String, Type > entry : subTypes.entrySet() ) {
				checkSubType( entry.getKey(), entry.getValue(), value, pathBuilder );
				pathBuilder.setLength( l );
			}

			// TODO make this more performant
			for( String childName : value.children().keySet() ) {
				if( !subTypes.containsKey( childName ) ) {
					throw new TypeCheckingException(
						"Unexpected child node: " + pathBuilder.toString() + "." + childName );
				}
			}
		}
	}

	private void checkSubType( String typeName, Type type, Value value, StringBuilder pathBuilder )
		throws TypeCheckingException {
		pathBuilder.append( '.' )
			.append( typeName );

		final boolean hasChildren = value.hasChildren( typeName );
		if( !hasChildren && type.cardinality().min() > 0 ) {
			throw new TypeCheckingException( "Undefined required child node: " + pathBuilder.toString() );
		} else if( hasChildren ) {
			final ValueVector vector = value.getChildren( typeName );
			final int size = vector.size();
			if( type.cardinality().min() > size || type.cardinality().max() < size ) {
				throw new TypeCheckingException(
					"Child node " + pathBuilder.toString() + " has a wrong number of occurencies. Permitted range is ["
						+
						type.cardinality().min() + "," + type.cardinality().max() + "], found " + size );
			}

			for( Value v : vector ) {
				type.check( v, pathBuilder );
			}
		}
	}

	private void castNativeType( Value value, StringBuilder pathBuilder )
		throws TypeCastingException {
		try {
			basicType.check( value, pathBuilder::toString );
		} catch( TypeCheckingException ex ) {
			// ANY is not handled, because checkNativeType returns true for it anyway
			switch( basicType.nativeType() ) {
			case DOUBLE:
				try {
					value.setValue( value.doubleValueStrict() );
				} catch( TypeCastingException e ) {
					throw new TypeCastingException(
						"Cannot cast node value to " + basicType.nativeType().id() + ": " + pathBuilder.toString() );
				}
				break;
			case INT:
				try {
					value.setValue( value.intValueStrict() );
				} catch( TypeCastingException e ) {
					throw new TypeCastingException(
						"Cannot cast node value to " + basicType.nativeType().id() + ": " + pathBuilder.toString() );
				}
				break;
			case LONG:
				try {
					value.setValue( value.longValueStrict() );
				} catch( TypeCastingException e ) {
					throw new TypeCastingException(
						"Cannot cast node value to " + basicType.nativeType().id() + ": " + pathBuilder.toString() );
				}
				break;
			case BOOL:
				try {
					value.setValue( value.boolValueStrict() );
				} catch( TypeCastingException e ) {
					throw new TypeCastingException(
						"Cannot cast node value to " + basicType.nativeType().id() + ": " + pathBuilder.toString() );
				}
				break;
			case STRING:
				try {
					value.setValue( value.strValueStrict() );
				} catch( TypeCastingException e ) {
					throw new TypeCastingException(
						"Cannot cast node value to " + basicType.nativeType().id() + ": " + pathBuilder.toString() );
				}
				break;
			case VOID:
				if( value.valueObject() != null ) {
					throw new TypeCastingException(
						"Expected " + NativeType.VOID.id() + ", found " +
							value.valueObject().getClass().getSimpleName() +
							": " + pathBuilder.toString() );
				}
				break;
			case RAW:
				try {
					value.setValue( value.byteArrayValueStrict() );
				} catch( TypeCastingException e ) {
					throw new TypeCastingException(
						"Cannot cast node value to " + basicType.nativeType().id() + ": " + pathBuilder.toString() );
				}
				break;
			default:
				throw new TypeCastingException(
					"Expected " + basicType.nativeType().id() + ", found " +
						value.valueObject().getClass().getSimpleName() +
						": " + pathBuilder.toString() );
			}
		}
	}
}


class TypeChoice extends Type {
	private final Range cardinality;
	private final Type left;
	private final Type right;

	public TypeChoice( Range cardinality, Type left, Type right ) {
		this.cardinality = cardinality;
		this.left = left;
		this.right = right;
	}

	@Override
	public Type findSubType( String key ) {
		Type ret = left.findSubType( key );
		return (ret != null) ? ret : right.findSubType( key );
	}

	@Override
	public Type findSubType( String key, Value value ) {
		Type ret = left.findSubType( key, value );
		return (ret != null) ? ret : right.findSubType( key, value );
	}

	@Override
	public void cutChildrenFromValue( Value value ) {
		left.cutChildrenFromValue( value );
		right.cutChildrenFromValue( value );
	}

	@Override
	public Range cardinality() {
		return cardinality;
	}

	@Override
	protected void check( Value value, StringBuilder pathBuilder )
		throws TypeCheckingException {
		final int l = pathBuilder.length();
		try {
			left.check( value, pathBuilder );
		} catch( TypeCheckingException e ) {
			pathBuilder.setLength( l );
			right.check( value, pathBuilder );
		}
	}

	@Override
	public Optional< Type > getMinimalType( Value value ) {
		Optional< Type > leftType = left().getMinimalType( value );
		if( leftType.isPresent() ) {
			return leftType;
		} else {
			return right().getMinimalType( value );
		}
	}

	@Override
	protected Value cast( Value value, StringBuilder pathBuilder )
		throws TypeCastingException {
		// TODO: The performance of this is not great, but does the job for now
		final Value copy = Value.createDeepCopy( value );
		try {
			left.cast( copy );
			return left.cast( value );
		} catch( TypeCastingException e ) {
			return right.cast( value );
		}
	}

	protected Type left() {
		return left;
	}

	protected Type right() {
		return right;
	}
}


/**
 *
 * @author Fabrizio Montesi
 */
public abstract class Type {
	public static final Type UNDEFINED =
		Type.create( BasicType.fromBasicTypeDefinition( BasicTypeDefinition.of( NativeType.ANY ) ),
			new Range( 0, Integer.MAX_VALUE ), true, null );
	public static final Type VOID =
		Type.create( BasicType.fromBasicTypeDefinition( BasicTypeDefinition.of( NativeType.VOID ) ),
			new Range( 1, 1 ), false, null );

	public static Type create(
		BasicType< ? > basicType,
		Range cardinality,
		boolean undefinedSubTypes,
		Map< String, Type > subTypes ) {
		return new TypeImpl( basicType, cardinality, undefinedSubTypes, subTypes );
	}

	public static TypeLink createLink( String linkedTypeName, Range cardinality ) {
		return new TypeLink( linkedTypeName, cardinality );
	}

	public static Type createChoice( Range cardinality, Type left, Type right ) {
		return new TypeChoice( cardinality, left, right );
	}

	/**
	 * Returns a type that extends t1 with t2.
	 *
	 * @param t1
	 * @param t2
	 * @return a new type that extends t1 with t2.
	 * @throws UnsupportedOperationException
	 */
	public static Type extend( Type t1, Type t2 )
		throws UnsupportedOperationException {
		final Type returnType;
		if( t2 instanceof TypeImpl ) {
			returnType = extend( t1, (TypeImpl) t2 );
		} else if( t2 instanceof TypeChoice ) {
			returnType = extend( t1, (TypeChoice) t2 );
		} else if( t2 instanceof TypeLink ) {
			returnType = extend( t1, (TypeLink) t2 );
		} else {
			String t1Name = t1 != null ? t1.getClass().getSimpleName() : "(null error)";
			String t2Name = t2 != null ? t2.getClass().getSimpleName() : "(null error)";
			throw new UnsupportedOperationException(
				String.format( "extension not supported between %s and %s", t1Name, t2Name ) );
		}
		return returnType;
	}

	private static Type extend( Type t1, TypeImpl t2 ) {
		final Type returnType;
		if( t1 instanceof TypeImpl ) {
			returnType = extend( (TypeImpl) t1, t2 );
		} else if( t1 instanceof TypeChoice ) {
			returnType = extend( (TypeChoice) t1, t2 );
		} else if( t1 instanceof TypeLink ) {
			returnType = extend( (TypeLink) t1, t2 );
		} else {
			String t1Name = t1 != null ? t1.getClass().getSimpleName() : "(null error)";
			String t2Name = t2 != null ? t2.getClass().getSimpleName() : "(null error)";
			throw new UnsupportedOperationException(
				String.format( "extension not supported between %s and %s", t1Name, t2Name ) );
		}
		return returnType;
	}

	private static Type extend( TypeLink t1, TypeImpl t2 ) {
		assert t1.linkedType != null;
		return extend( t1.linkedType, t2 );
	}

	private static Type extend( TypeChoice t1, TypeImpl t2 ) {
		return new TypeChoice( t1.cardinality(), extend( t1.left(), t2 ), extend( t1.right(), t2 ) );
	}

	private static Type extend( Type t1, TypeLink t2 ) {
		assert t2.linkedType != null;
		return extend( t1, t2.linkedType );
	}

	private static Type extend( Type t1, TypeChoice t2 ) {
		return new TypeChoice( t1.cardinality(), extend( t1, t2.left() ), extend( t1, t2.right() ) );
	}

	private static Type extend( TypeImpl t1, TypeImpl t2 ) {
		BasicType< ? > basicType = t1.basicType();
		Range cardinality = t1.cardinality();
		Map< String, Type > subTypes = new HashMap<>();
		t1.subTypes().entrySet().forEach( entry -> subTypes.put( entry.getKey(), entry.getValue() ) );
		if( t2 != null ) {
			t2.subTypes().entrySet().forEach( entry -> subTypes.put( entry.getKey(), entry.getValue() ) );
		}
		return create( basicType, cardinality, false, subTypes );
	}

	public void check( Value value )
		throws TypeCheckingException {
		check( value, new StringBuilder( "#Message" ) );
	}

	public Value cast( Value value )
		throws TypeCastingException {
		return cast( value, new StringBuilder( "#Message" ) );
	}

	public boolean isVoid() {
		if( !(this instanceof TypeImpl) )
			return false;
		TypeImpl t = (TypeImpl) this;
		return t.basicType().equals( BasicType.fromBasicTypeDefinition( BasicTypeDefinition.of( NativeType.VOID ) ) )
			&& t.cardinality().equals( new Range( 1, 1 ) )
			&& (t.subTypes() == null || t.subTypes().isEmpty());
	}

	public abstract Optional< Type > getMinimalType( Value value );

	public abstract void cutChildrenFromValue( Value value );

	public abstract Range cardinality();

	public abstract Type findSubType( String key );

	public abstract Type findSubType( String key, Value value );

	protected abstract void check( Value value, StringBuilder pathBuilder )
		throws TypeCheckingException;

	protected abstract Value cast( Value value, StringBuilder pathBuilder )
		throws TypeCastingException;

	public static class TypeLink extends Type {
		private final String linkedTypeName;
		private final Range cardinality;
		private Type linkedType;

		public TypeLink( String linkedTypeName, Range cardinality ) {
			this.linkedTypeName = linkedTypeName;
			this.cardinality = cardinality;
		}

		@Override
		public Type findSubType( String key ) {
			return linkedType.findSubType( key );
		}

		@Override
		public Type findSubType( String key, Value value ) {
			return linkedType.findSubType( key, value );
		}

		public String linkedTypeName() {
			return linkedTypeName;
		}

		public void setLinkedType( Type linkedType ) {
			this.linkedType = linkedType;
		}

		@Override
		public void cutChildrenFromValue( Value value ) {
			linkedType.cutChildrenFromValue( value );
		}

		@Override
		public Range cardinality() {
			return cardinality;
		}

		@Override
		protected void check( Value value, StringBuilder pathBuilder )
			throws TypeCheckingException {
			linkedType.check( value, pathBuilder );
		}

		@Override
		public Optional< Type > getMinimalType( Value value ) {
			return linkedType.getMinimalType( value );
		}

		@Override
		protected Value cast( Value value, StringBuilder pathBuilder )
			throws TypeCastingException {
			return linkedType.cast( value, pathBuilder );
		}
	}
}
