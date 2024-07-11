/*
 * Copyright (C) 2022  Narongrit Unwerawattana <narongrit.kie@gmail.com>
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
package jolie.lang.parse.module;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ModuleRecordCache {

	/**
	 * Module record Cache
	 */
	private final Map< URI, ModuleRecord > cache = new ConcurrentHashMap<>();

	/*
	 * Dependencies loaded from parsing Module <key>
	 */
	private final Map< URI, Set< URI > > dependenciesLoadedFrom = new ConcurrentHashMap<>();

	/*
	 * Dependencies needed by the module <key>
	 */
	private final Map< URI, Set< URI > > dependenciesNeededBy = new ConcurrentHashMap<>();

	/**
	 * Register dependency graph of the module's URI and its dependencies
	 *
	 * @param moduleURI target module
	 * @param dependencies list of dependencies needed by moduleURI
	 */
	private void putDependencies( URI moduleURI, List< URI > dependencies ) {
		dependenciesLoadedFrom.putIfAbsent( moduleURI, new HashSet<>() );
		dependenciesLoadedFrom.get( moduleURI ).addAll( dependencies );
		dependencies.forEach( d -> {
			dependenciesNeededBy.putIfAbsent( d, new HashSet<>() );
			dependenciesNeededBy.get( d ).add( moduleURI );
		} );
	}

	protected void put( ModuleRecord mc, List< URI > dependencies ) {
		cache.put( mc.uri(), mc );
		this.putDependencies( mc.uri(), dependencies );
	}

	protected boolean contains( URI source ) {
		return cache.containsKey( source );
	}

	protected ModuleRecord get( URI source ) {
		return cache.get( source );
	}

	/**
	 * Handle dependency graph when removing a cache entry
	 *
	 * @param source target module to remove cache
	 */
	private void removeDependencies( URI source ) {
		Optional< Set< URI > > dependenciesSet = Optional.ofNullable( dependenciesLoadedFrom.remove( source ) );
		dependenciesSet.ifPresent( dependency -> {
			dependency.forEach( d -> {
				Optional< Set< URI > > neededBySet = Optional.ofNullable( dependenciesNeededBy.get( d ) );
				neededBySet.ifPresent( s -> s.remove( source ) );
				if( neededBySet.isEmpty() ) {
					this.remove( d );
				}
			} );
		} );
	}

	protected void remove( URI source ) {
		if( source != null ) {
			try {
				if( cache.remove( source ) != null ) {
					this.removeDependencies( source );
				}
			} catch( ClassCastException e ) {
			}
		}
	}

}
