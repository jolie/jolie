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
	private static final Map< URI, ModuleRecord > CACHE = new ConcurrentHashMap<>();

	/*
	 * Dependencies loaded from parsing Module <key>
	 */
	private static final Map< URI, Set< URI > > DEPENDENCIES_LOADED_FROM = new ConcurrentHashMap<>();

	/*
	 * Dependencies needed by the module <key>
	 */
	private static final Map< URI, Set< URI > > DEPENDENCIES_NEEDED_BY = new ConcurrentHashMap<>();

	/**
	 * Register dependency graph of the module's URI and its dependencies
	 * 
	 * @param moduleURI target module
	 * @param dependencies list of dependencies needed by moduleURI
	 */
	private static void putDependencies( URI moduleURI, List< URI > dependencies ) {
		DEPENDENCIES_LOADED_FROM.putIfAbsent( moduleURI, new HashSet<>() );
		DEPENDENCIES_LOADED_FROM.get( moduleURI ).addAll( dependencies );
		dependencies.forEach( d -> {
			DEPENDENCIES_NEEDED_BY.putIfAbsent( d, new HashSet<>() );
			DEPENDENCIES_NEEDED_BY.get( d ).add( moduleURI );
		} );
	}

	protected static void put( ModuleRecord mc, List< URI > dependencies ) {
		CACHE.put( mc.uri(), mc );
		ModuleRecordCache.putDependencies( mc.uri(), dependencies );
	}

	protected static boolean contains( URI source ) {
		return CACHE.containsKey( source );
	}

	protected static ModuleRecord get( URI source ) {
		return CACHE.get( source );
	}

	/**
	 * Handle dependency graph when removing a cache entry
	 * 
	 * @param source target module to remove cache
	 */
	private static void removeDependencies( URI source ) {
		Optional< Set< URI > > dependenciesSet = Optional.ofNullable( DEPENDENCIES_LOADED_FROM.remove( source ) );
		dependenciesSet.ifPresent( dependency -> {
			dependency.forEach( d -> {
				Optional< Set< URI > > neededBySet = Optional.ofNullable( DEPENDENCIES_NEEDED_BY.get( d ) );
				neededBySet.ifPresent( s -> s.remove( source ) );
				if( neededBySet.isEmpty() ) {
					ModuleRecordCache.remove( d );
				}
			} );
		} );
	}

	protected static void remove( URI source ) {
		if( source != null ) {
			try {
				if( CACHE.remove( source ) != null ) {
					ModuleRecordCache.removeDependencies( source );
				}
			} catch( ClassCastException e ) {
			}
		}
	}

}
