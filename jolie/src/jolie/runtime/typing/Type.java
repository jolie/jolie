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

package jolie.runtime.typing;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import jolie.lang.NativeType;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.util.Range;

/**
 *
 * @author Fabrizio Montesi
 */
public class Type
{
	private final Range cardinality;
	private final NativeType nativeType;
	private final Set< Entry< String, Type > > subTypeSet;
	private final Set< String > subTypeKeySet;

	public Type(
		NativeType nativeType,
		Range cardinality,
		boolean undefinedSubTypes,
		Map< String, Type > subTypes
	) {
		this.nativeType = nativeType;
		this.cardinality = cardinality;
		if ( undefinedSubTypes ) {
			subTypeSet = null;
			subTypeKeySet = null;
		} else {
			subTypeSet = subTypes.entrySet();
			subTypeKeySet = subTypes.keySet();
		}
	}

	public void check( Value value )
		throws TypeCheckingException
	{
		check( value, new StringBuilder( "#Message" ) );
	}

	private void check( Value value, StringBuilder pathBuilder )
		throws TypeCheckingException
	{
		if ( checkNativeType( value, nativeType ) == false ) {
			throw new TypeCheckingException( "Invalid native type for node " + pathBuilder.toString() + ": expected " + nativeType + ", found " + (( value.valueObject() == null ) ? "void" : value.valueObject().getClass().getName()) );
		}

		if ( subTypeSet != null ) {
			for( Entry< String, Type > entry : subTypeSet ) {
				checkSubType( entry.getKey(), entry.getValue(), value, new StringBuilder( pathBuilder ) );
			}
			// TODO make this more performant
			for( String childName : value.children().keySet() ) {
				if ( subTypeKeySet.contains( childName ) == false ) {
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
		if ( hasChildren == false && type.cardinality.min() > 0 ) {
			throw new TypeCheckingException( "Undefined required child node: " + pathBuilder.toString() );
		} else if ( hasChildren ) {
			ValueVector vector = value.getChildren( typeName );
			int size = vector.size();
			if ( type.cardinality.min() > size || type.cardinality.max() < size ) {
				throw new TypeCheckingException(
					"Child node " + pathBuilder.toString() + " has a wrong number of occurencies. Permitted range is [" +
					type.cardinality.min() + "," + type.cardinality.max() + "], found " + size
				);
			}

			for( Value v : vector ) {
				type.check( v, pathBuilder );
			}
		}
	}

	private boolean checkNativeType( Value value, NativeType nativeType )
	{
		if ( nativeType == NativeType.ANY ) {
			return true;
		} else if ( nativeType == NativeType.DOUBLE ) {
			return value.isDouble() || value.isInt();
		} else if ( nativeType == NativeType.INT ) {
			return value.isInt();
		} else if ( nativeType == NativeType.STRING ) {
			return value.isString();
		} else if ( nativeType == NativeType.VOID ) {
			return value.valueObject() == null;
		} else if ( nativeType == NativeType.RAW ) {
			return value.isByteArray();
		}

		return false;
	}
}
