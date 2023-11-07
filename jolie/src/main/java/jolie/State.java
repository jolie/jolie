/*
 * Copyright (C) 2006-2019 Fabrizio Montesi <famontesi@gmail.com>
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

package jolie;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jolie.runtime.InternalLink;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.VariablePath;

/**
 * The state of a process, containing the values of its variables (inside of the root tree), its
 * internal links, and data structures for detecting alias loops.
 *
 * @see Value
 * @see InternalLink
 * @author Fabrizio Montesi
 */
public final class State implements Cloneable {
	private final Value root;
	private final ConcurrentHashMap< String, InternalLink > linksMap = new ConcurrentHashMap<>();
	private final LoopDetectionMap< Value > valueLoopDetectionMap = new LoopDetectionMap<>();
	private final LoopDetectionMap< ValueVector > valueVectorLoopDetectionMap = new LoopDetectionMap<>();

	private State( Value root ) {
		this.root = root;
	}

	/**
	 * Returns the InternalLink identified by id in this State scope.
	 *
	 * @param id the identifier of the requested InternalLink
	 * @return the InternalLink identified by id
	 */
	public InternalLink getLink( String id ) {
		return linksMap.computeIfAbsent( id, InternalLink::new );
	}

	/**
	 * Constructs a new State, using a fresh memory state.
	 */
	public State() {
		this.root = Value.createRootValue();
	}

	@Override
	public State clone() {
		return new State( Value.createClone( root ) );
	}

	/**
	 * Returns the root Value of this State.
	 *
	 * @return the root Value of this State
	 * @see Value
	 */
	public Value root() {
		return root;
	}

	public void putAlias( VariablePath p, Value l ) {
		valueLoopDetectionMap.put( p, l );
	}

	public void putAlias( VariablePath p, ValueVector l ) {
		valueVectorLoopDetectionMap.put( p, l );
	}

	public void removeAlias( VariablePath p, Value l ) {
		valueLoopDetectionMap.remove( p, l );
	}

	public void removeAlias( VariablePath p, ValueVector l ) {
		valueVectorLoopDetectionMap.remove( p, l );
	}

	public boolean hasAlias( VariablePath p, Value l ) {
		return valueLoopDetectionMap.contains( p, l );
	}

	public boolean hasAlias( VariablePath p, ValueVector l ) {
		return valueVectorLoopDetectionMap.contains( p, l );
	}

	private static class LoopDetectionMap< V > {
		private final Map< VariablePath, Set< V > > m = new ConcurrentHashMap<>();

		public synchronized void put( VariablePath p, V v ) {
			m.compute( p, ( path, set ) -> {
				if( set == null ) {
					Set< V > s = new HashSet<>();
					s.add( v );
					return s;
				} else {
					set.add( v );
					return set;
				}
			} );
		}

		public synchronized boolean contains( VariablePath p, V v ) {
			return m.containsKey( p ) && m.get( p ).contains( v );
		}

		public synchronized void remove( VariablePath p, V v ) {
			m.computeIfPresent( p, ( path, set ) -> {
				set.remove( v );
				return set.isEmpty() ? null : set;
			} );
		}
	}
}
