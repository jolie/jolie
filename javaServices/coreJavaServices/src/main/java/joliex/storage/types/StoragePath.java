/***************************************************************************
 *   Copyright (C) 2010 by Fabrizio Montesi <famontesi@gmail.com>          *
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

package joliex.storage.types;

import java.util.Iterator;
import java.util.NoSuchElementException;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import joliex.storage.types.StoragePath.Node;

/**
 *
 * @author Fabrizio Montesi
 */
public class StoragePath implements Iterable< Node > {
	public static class Node {
		private final String name;
		private final int index;

		private Node( String name, int index ) {
			this.name = name;
			this.index = index;
		}

		public String name() {
			return name;
		}

		public int index() {
			return index;
		}
	}

	private static class ArrayIterator< T > implements Iterator< T > {
		private final T[] array;
		private int index = 0;

		public ArrayIterator( T[] array ) {
			this.array = array;
		}

		@Override
		public T next() {
			if( index + 1 >= array.length ) {
				throw new NoSuchElementException();
			}
			return array[ index++ ];
		}

		@Override
		public boolean hasNext() {
			return index + 1 < array.length;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	private final Node[] nodes;

	private StoragePath( Node[] nodes ) {
		this.nodes = nodes;
	}

	public Node[] nodes() {
		return nodes;
	}

	@Override
	public Iterator< Node > iterator() {
		return new ArrayIterator<>( nodes );
	}

	public static StoragePath fromValue( Value value ) {
		ValueVector nodesVector = value.getChildren( "node" );
		Node[] nodes = new Node[ nodesVector.size() ];
		int i = 0;
		for( Value nodeValue : nodesVector ) {
			nodes[ i++ ] = new Node(
				nodeValue.getFirstChild( "name" ).strValue(),
				nodeValue.getFirstChild( "index" ).intValue() );
		}
		return new StoragePath( nodes );
	}

	public static Value toValue( StoragePath path ) {
		ValueVector nodesVector = ValueVector.create();
		Value nodeValue;
		for( Node node : path.nodes ) {
			nodeValue = Value.create();
			nodeValue.getFirstChild( "name" ).setValue( node.name() );
			nodeValue.getFirstChild( "index" ).setValue( node.index() );
			nodesVector.add( nodeValue );
		}
		nodeValue = Value.create();
		nodeValue.children().put( "node", nodesVector );
		return nodeValue;
	}
}
