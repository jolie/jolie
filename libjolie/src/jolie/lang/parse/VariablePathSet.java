/***************************************************************************
 *   Copyright (C) 2011 by Fabrizio Montesi <famontesi@gmail.com>          *
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

package jolie.lang.parse;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import jolie.lang.parse.ast.VariablePathNode;

/**
 *
 * @author Fabrizio Montesi
 */
public class VariablePathSet <T extends VariablePathNode> implements Set< T >
{
	private final List< T > elements = new ArrayList< T >();

	public int size()
	{
		return elements.size();
	}

	public boolean isEmpty()
	{
		return elements.isEmpty();
	}

	public boolean contains( Object o )
	{
		if ( o instanceof VariablePathNode ) {
			VariablePathNode path = (VariablePathNode) o;
			for( VariablePathNode element : elements ) {
				if ( path.isEquivalentTo( element ) ) {
					return true;
				}
			}
		}

		return false;
	}

	public Iterator< T > iterator()
	{
		return elements.iterator();
	}

	public Object[] toArray()
	{
		return elements.toArray();
	}

	public <T> T[] toArray( T[] a )
	{
		return elements.toArray( a );
	}

	public boolean add( T e )
	{
		for( VariablePathNode element : elements ) {
			if ( e.isEquivalentTo( element ) ) {
				return false;
			}
		}
		elements.add( e );
		return true;
	}

	public T getContained( VariablePathNode e )
	{
		for( T element : elements ) {
			if ( e.isEquivalentTo( element ) ) {
				return element;
			}
		}
		return null;
	}

	public boolean remove( Object o )
	{
		if ( o instanceof VariablePathNode ) {
			VariablePathNode path = (VariablePathNode) o;

			for( int i = 0; i < elements.size(); i++ ) {
				if ( path.isEquivalentTo( elements.get( i ) ) ) {
					elements.remove( i );
					return true;
				}
			}
		}

		return false;
	}

	public boolean containsAll( Collection<?> c )
	{
		for( Object o : c ) {
			if ( !contains( o ) ) {
				return false;
			}
		}

		return true;
	}

	public boolean addAll( Collection<? extends T> c )
	{
		for( T n : c ) {
			add( n );
		}
		return true;
	}

	public boolean retainAll( Collection<?> c )
	{
		throw new UnsupportedOperationException( "Not supported yet." );
	}

	public boolean removeAll( Collection<?> c )
	{
		throw new UnsupportedOperationException( "Not supported yet." );
	}

	public void clear()
	{
		elements.clear();
	}
}
