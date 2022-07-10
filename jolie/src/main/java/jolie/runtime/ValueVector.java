/***************************************************************************
 *   Copyright (C) by Fabrizio Montesi                                     *
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

package jolie.runtime;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import jolie.lang.Constants;

class ValueVectorLink extends ValueVector implements Cloneable {
	private final VariablePath linkPath;

	@Override
	public ValueVectorLink clone() {
		return new ValueVectorLink( linkPath );
	}

	@Override
	public Value get( int i ) {
		return getLinkedValueVector().get( i );
	}

	@Override
	public void set( int i, Value value ) {
		getLinkedValueVector().set( i, value );
	}

	public ValueVectorLink( VariablePath path ) {
		linkPath = path;
	}

	@Override
	public boolean isLink() {
		return true;
	}

	private ValueVector getLinkedValueVector() {
		return linkPath.getValueVector( this );
	}

	@Override
	protected List< Value > values() {
		return getLinkedValueVector().values();
	}

	@Override
	public List< Value > valuesCopy() {
		return getLinkedValueVector().valuesCopy();
	}

	@Override
	public int size() {
		ValueVector vector = linkPath.getValueVectorOrNull();
		return (vector == null) ? 0 : vector.size();
	}
}


class ValueVectorImpl extends ValueVector implements Serializable {
	private static final long serialVersionUID = Constants.serialVersionUID();
	private final ArrayList< Value > values;

	@Override
	protected List< Value > values() {
		return values;
	}

	@Override
	public synchronized int size() {
		return values().size();
	}

	@Override
	public Value get( int i ) {
		if( i >= values.size() ) {
			synchronized( this ) {
				if( i >= values.size() ) {
					values.ensureCapacity( i + 1 );
					for( int k = values.size(); k <= i; k++ ) {
						values.add( Value.create() );
					}
				}
			}
		}
		return values.get( i );
	}

	@Override
	public synchronized void set( int i, Value value ) {
		if( i >= values.size() ) {
			values.ensureCapacity( i + 1 );
			for( int k = values.size(); k < i; k++ ) {
				values.add( Value.create() );
			}
			values.add( value );
		} else {
			values.set( i, value );
		}
	}

	@Override
	public boolean isLink() {
		return false;
	}

	@Override
	public synchronized List< Value > valuesCopy() {
		return new ArrayList<>( values );
	}

	public ValueVectorImpl() {
		values = new ArrayList<>( 1 );
	}
}


public abstract class ValueVector implements Iterable< Value > {
	public static ValueVector create() {
		return new ValueVectorImpl();
	}

	public synchronized Value remove( int i ) {
		return values().remove( i );
	}

	public static ValueVector createLink( VariablePath path ) {
		return new ValueVectorLink( path );
	}

	public static ValueVector createClone( ValueVector vec ) {
		ValueVector retVec;

		if( vec.isLink() ) {
			retVec = ((ValueVectorLink) vec).clone();
		} else {
			retVec = create();
			for( Value v : vec ) {
				retVec.add( Value.createClone( v ) );
			}
		}

		return retVec;
	}

	public synchronized Value first() {
		return get( 0 );
	}

	public synchronized boolean isEmpty() {
		return values().isEmpty();
	}

	@Override
	public synchronized Iterator< Value > iterator() {
		return values().iterator();
	}

	public abstract Value get( int i );

	public abstract void set( int i, Value value );

	public abstract int size();

	public abstract List< Value > valuesCopy();


	public synchronized void add( Value value ) {
		values().add( value );
	}

	public synchronized void add( int index, Value value ) {
		values().add( index, value );
	}

	// TODO: improve performance
	public synchronized void deepCopy( ValueVector vec ) {
		for( int i = 0; i < vec.size(); i++ ) {
			get( i ).deepCopy( vec.get( i ) );
		}
	}

	public synchronized void deepCopyWithLinks( ValueVector vec ) {
		for( int i = 0; i < vec.size(); i++ ) {
			get( i ).deepCopyWithLinks( vec.get( i ) );
		}
	}

	protected abstract List< Value > values();

	public abstract boolean isLink();

	public final Stream< Value > stream() {
		return StreamSupport.stream( spliterator(), false );
	}
}
