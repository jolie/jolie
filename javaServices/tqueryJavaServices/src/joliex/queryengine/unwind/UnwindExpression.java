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

package joliex.queryengine.unwind;

import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import joliex.queryengine.common.Path;
import joliex.queryengine.common.TQueryExpression;

class UnwindExpression implements TQueryExpression {

	private final Path path;

	UnwindExpression(Path p) {
		this.path = p;
	}

	@Override
	public Value applyOn(Value element) {
		throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
	}

	@Override
	public ValueVector applyOn(ValueVector elements) {
		
		ValueVector resultVector = ValueVector.create();
		elements.forEach( ( element ) -> {
			String node = path.getCurrentNode();
			ValueVector elementsContinuation 
					= Path.parsePath( node ).apply( element )
							.orElse( ValueVector.create() );
			if ( path.getContinuation().isPresent() ) {
				expand( element, 
						new UnwindExpression( 
								path.getContinuation()
									.get() )
									.applyOn( elementsContinuation ), 
						node )
							.forEach( ( elementContinuation ) -> 
									include( resultVector, elementContinuation ) 
							);
			} else {
				elementsContinuation.forEach( ( elementContinuation ) -> 
						include( resultVector, elementContinuation )
				);
			}
		});
		
		return resultVector;
	}

	private ValueVector expand( Value tree, ValueVector applyOn, String node ) {
		return ValueVector.create();
	}

	private void include( ValueVector valueVector, Value value ) {
		valueVector.add( value );
	}
}
