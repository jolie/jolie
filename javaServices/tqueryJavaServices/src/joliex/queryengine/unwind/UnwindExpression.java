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

class UnwindExpression {

	private final Path path;

	UnwindExpression(Path p) {
		this.path = p;
	}

	public ValueVector applyOn(ValueVector elements) {
		
		ValueVector resultElements = ValueVector.create();
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
									include( resultElements, elementContinuation ) 
							);
			} else {
				elementsContinuation.forEach( ( elementContinuation ) -> 
						include( resultElements, elementContinuation )
				);
			}
		});
		
		return resultElements;
	}

	private ValueVector expand( Value element, ValueVector elements, String node ) {
		
		ValueVector resultElements = ValueVector.create();
		resultElements.forEach( (elementContinuation) -> {
			Value tmpElement = Value.createClone( elementContinuation );
			tmpElement.children().put( node, getValueVector( element ) );
			resultElements.add( tmpElement );
		});
		
		return resultElements;
	}

	private void include( ValueVector valueVector, Value value ) {
		valueVector.add( value );
	}

	private ValueVector getValueVector( Value element ) {
		
		ValueVector result = ValueVector.create();
		result.add( element );
		
		return result;
	}
}
