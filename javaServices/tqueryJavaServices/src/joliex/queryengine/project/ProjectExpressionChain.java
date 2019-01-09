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

package joliex.queryengine.project;

import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import joliex.queryengine.common.TQueryExpression;
import joliex.queryengine.common.Utils;

public class ProjectExpressionChain implements TQueryExpression {

	private final LinkedList<TQueryExpression> expressions = new LinkedList<>();

	public ProjectExpressionChain addExpression( TQueryExpression expression ){
		expressions.add( expression );
		return this;
	}
	
	@Override
	public ValueVector applyOn( ValueVector elements ){
		ValueVector returnVector = ValueVector.create();
		for ( Value element : elements ) {
			returnVector.add( this.applyOn( element ) );
		}
		return returnVector;
	};	
	
	@Override
	public Value applyOn( Value element ){
		if( expressions.isEmpty() ){
			return element;
		} else {
			return applyOn( element, 0 );
		}
	}
	
	public Value applyOn( Value element, int index ) {
		if( expressions.size() > index ){
			try {
				return Utils.merge( expressions.get( index ).applyOn( element ), applyOn(element, index++) );
			} catch ( FaultException ex ) {
				Logger.getLogger( ProjectExpressionChain.class.getName() ).log( Level.SEVERE, null, ex );
				return Value.create();
			}
		} else {
			return expressions.get( index ).applyOn( element );
		}
 	}
}
