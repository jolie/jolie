/***************************************************************************
 *   Copyright (C) 2006-2015 by Fabrizio Montesi <famontesi@gmail.com>     *
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

import java.util.LinkedList;
import java.util.List;

import jolie.runtime.expression.Expression;
import jolie.util.Pair;

/**
 *
 * @author Fabrizio Montesi
 */
public final class VariablePathBuilder {
	private final List< Pair< Expression, Expression > > list = new LinkedList<>();
	private final boolean global;

	public VariablePathBuilder( boolean global ) {
		this.global = global;
	}

	public VariablePathBuilder add( String id, int index ) {
		list.add( new Pair<>( Value.create( id ), Value.create( index ) ) );
		return this;
	}

	public VariablePathBuilder add( String id ) {
		list.add( new Pair<>( Value.create( id ), null ) );
		return this;
	}

	@SuppressWarnings( "unchecked" )
	public VariablePath toVariablePath() {
		if( global ) {
			return new GlobalVariablePath( list.toArray( new Pair[] {} ) );
		} else {
			return new VariablePath( list.toArray( new Pair[] {} ) );
		}
	}

	@SuppressWarnings( "unchecked" )
	public VariablePath toClosedVariablePath( Value rootValue ) {
		return new ClosedVariablePath( list.toArray( new Pair[ 0 ] ), rootValue );
	}
}
