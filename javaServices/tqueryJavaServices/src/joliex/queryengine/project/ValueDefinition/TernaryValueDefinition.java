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

package joliex.queryengine.project.valuedefinition;

import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import joliex.queryengine.match.MatchExpression;

public class TernaryValueDefinition implements ValueDefinition {

	private final MatchExpression condition;
	private final ValueDefinition ifTrue, ifFalse;
	
	public TernaryValueDefinition( MatchExpression condition, ValueDefinition ifTrue, ValueDefinition ifFalse ) {
		this.condition = condition;
		this.ifTrue = ifTrue;
		this.ifFalse = ifFalse;
	}

	@Override
	public ValueVector evaluate( Value value ) {
		if ( condition.applyOn( value ) ){
			return ifTrue.evaluate( value );
		} else {
			return ifFalse.evaluate( value );
		}
	}

	@Override
	public boolean isDefined( Value value ) {
		if ( condition.applyOn( value ) ){
			return ifTrue.isDefined( value );
		} else {
			return ifFalse.isDefined( value );
		}
	}
	
}
