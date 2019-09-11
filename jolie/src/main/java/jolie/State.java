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

package jolie;

import java.util.concurrent.ConcurrentHashMap;
import jolie.runtime.InternalLink;
import jolie.runtime.Value;

/**
 * A variable and links state.
 * Variables are stored in a tree-like structure, of which the root Value element is stored.
 * @see Value
 * @author Fabrizio Montesi
 */
public final class State implements Cloneable
{
	private final Value root;
	private final ConcurrentHashMap< String, InternalLink > linksMap = new ConcurrentHashMap<>();
	
	private State( Value root )
	{
		this.root = root;
	}
	
	/**
	 * Returns the InternalLink identified by id in this State scope.
	 * @param id the identifier of the requested InternalLink
	 * @return the InternalLink identified by id
	 */
	public InternalLink getLink( String id )
	{
		return linksMap.computeIfAbsent( id, k -> new InternalLink( k ) );
	}
	
	/**
	 * Constructs a new State, using a fresh memory state.
	 */
	public State()
	{
		this.root = Value.createRootValue();
	}
	
	@Override
	public State clone()
	{
		return new State( Value.createClone( root ) );
	}
	
	/**
	 * Returns the root Value of this State.
	 * @return the root Value of this State
	 * @see Value
	 */
	public Value root()
	{
		return root;
	}
}
