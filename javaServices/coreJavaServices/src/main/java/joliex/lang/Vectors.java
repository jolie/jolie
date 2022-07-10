/*
 *   Copyright (C) 2022 by Fabrizio Montesi <famontesi@gmail.com>
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU Library General Public License as
 *   published by the Free Software Foundation; either version 2 of the
 *   License, or (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU Library General Public
 *   License along with this program; if not, write to the
 *   Free Software Foundation, Inc.,
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 *   For details about the authors of this software, see the AUTHORS file.
 */

package joliex.lang;

import jolie.runtime.JavaService;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;

public class Vectors extends JavaService {
	private final static String ITEMS = "items";
	private final static String VECTOR = "vector";
	private final static String ITEM = "item";

	public Value insert( Value request ) {
		ValueVector items = request.getFirstChild( VECTOR ).getChildren( ITEMS );
		items.add( request.getFirstChild( "index" ).intValue(), request.getFirstChild( ITEM ) );

		Value retVal = Value.create();
		retVal.children().put( ITEMS, items );
		return retVal;
	}

	public Value add( Value request ) {
		ValueVector items = request.getFirstChild( VECTOR ).getChildren( ITEMS );
		items.add( request.getFirstChild( ITEM ) );

		Value retVal = Value.create();
		retVal.children().put( ITEMS, items );
		return retVal;
	}

	public Value slice( Value request ) {
		ValueVector items = request.getFirstChild( VECTOR ).getChildren( ITEMS );
		int from = request.firstChildOrDefault( "from", Value::intValue, 0 );
		int to = request.firstChildOrDefault( "to", Value::intValue, items.size() );
		Value retVal = Value.create();
		ValueVector vec = retVal.getChildren( ITEMS );
		for( int i = from; i < to; i++ ) {
			vec.add( items.get( i ) );
		}
		return retVal;
	}

	public Boolean equals( Value request ) {
		return Values.checkVectorEquality(
			request.getFirstChild( "fst" ).getFirstChild( VECTOR ).getChildren( ITEMS ),
			request.getFirstChild( "snd" ).getFirstChild( VECTOR ).getChildren( ITEMS ) );
	}

	public Value concat( Value request ) {
		ValueVector vec = request.getFirstChild( "fst" ).getChildren( ITEMS );
		for( Value value : request.getFirstChild( "snd" ).getChildren( ITEMS ) ) {
			vec.add( value );
		}

		Value retVal = Value.create();
		retVal.children().put( ITEMS, vec );
		return retVal;
	}
}
