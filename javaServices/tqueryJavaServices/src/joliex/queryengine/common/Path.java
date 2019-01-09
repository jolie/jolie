/*******************************************************************************
 *   Copyright (C) 2018 by Larisa Safina <safina@imada.sdu.dk>                 *
 *   Copyright (C) 2018 by Stefano Pio Zingaro <stefanopio.zingaro@unibo.it>   *
 *   Copyright (C) 2018 by Saverio Giallorenzo <saverio.giallorenzo@gmail.com> *
 *                                                                             *
 *   This program is free software; you can redistribute it and/or modify      *
 *   it under the terms of the GNU Library General Public License as           *
 *   published by the Free Software Foundation; either version 2 of the        *
 *   License, or (at your option) any later version.                           *
 *                                                                             *
 *   This program is distributed in the hope that it will be useful,           *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of            *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             *
 *   GNU General Public License for more details.                              *
 *                                                                             *
 *   You should have received a copy of the GNU Library General Public         * 
 *   License along with this program; if not, write to the                     *
 *   Free Software Foundation, Inc.,                                           *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.                 *
 *                                                                             *
 *   For details about the authors of this software, see the AUTHORS file.     *
 *******************************************************************************/

package joliex.queryengine.common;

import java.util.Optional;
import jolie.Jolie;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;

/**
 * This class implements {@link Path}s in {@link Jolie}. We use {@link Path}s in
 * the TQuery framework for ephemeral data handling over {@link Jolie} trees.
 *
 */
public class Path {

	private static final String PATH_SEPARATOR = ".";

	private final String node;
	private final Optional<Path> continuation;

	private Path( String node, Optional<Path> continuation ) {
		this.node = node;
		this.continuation = continuation;
	}
	
	public String getCurrentNode(){
		return node;
	}
	
	public Optional<Path> getContinuation(){
		return continuation;
	}

	/**
	 * It parses a
	 * <pre>String</pre>, e.g. a.b.c, and recursively build a
	 * {@link Path} with a node in the root and a {@link Path} as its
	 * continuation
	 *
	 * @param path the <pre>String</pre> representing the {@link Path}
	 * @return 
	 *
	 */
	public static Path parsePath( String path ) {
		int idx = path.indexOf( PATH_SEPARATOR );
		if(  idx > 0 ){
			return new Path( path.substring( 0, idx ), Optional.of( parsePath( path.substring( idx + 1 ) ) ) );
		} else if ( path.length() > 0 ) {
			return new Path( path, Optional.empty() );
		} else {
			throw new IllegalArgumentException( "Could not parse path \"" + path + "\"" );
		}
	}
	
	public Optional<ValueVector> apply( Value value ) {
		if ( value.hasChildren( node ) ){
			if( continuation.isPresent() ){
				Optional<ValueVector> childrenOptional = Optional.empty();
				ValueVector children = ValueVector.create();
				for ( Value child : value.getChildren( node ) ) {
					Optional<ValueVector> grandchildren = continuation.get().apply( child );
					if( grandchildren.isPresent() ){
						if( childrenOptional.isEmpty() ){ childrenOptional = Optional.of( children ); }
						for( Value grandchild : grandchildren.get() ){
							children.add( grandchild );
						}
					}
				}
				return childrenOptional;
			} else {
				return Optional.of( value.getChildren( node ) );
			}
		} else {
			return Optional.empty();
		}
	}
	
	public boolean exists( Value value ){
		if ( value.hasChildren( node ) ){
			if( continuation.isPresent() ) {
				for ( Value child : value.getChildren( node ) ) {
					if( continuation.get().exists( child ) ){
						return true;
					}
				}
				return false;
			} else {
				return true;
			}
		} else {
			return false;
		}
	}

	/**
	 * It gives a nice <pre>String</pre> representation of a {@link Path}
	 *
	 * @return the <pre>String</pre> representation of this {@link Path}
	 */
	public String toPrettyString() {
		return node + ( continuation.isPresent() ? PATH_SEPARATOR + continuation.get().toPrettyString() : "" );	
	}
	
}
