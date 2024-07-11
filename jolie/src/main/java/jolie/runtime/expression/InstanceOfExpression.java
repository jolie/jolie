/***************************************************************************
 *   Copyright (C) 2012 by Fabrizio Montesi <famontesi@gmail.com>          *
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


package jolie.runtime.expression;

import jolie.process.TransformationReason;
import jolie.runtime.Value;
import jolie.runtime.typing.Type;
import jolie.runtime.typing.TypeCheckingException;

/**
 * Implements the instanceof operator.
 *
 * @author Fabrizio Montesi
 */
public class InstanceOfExpression implements Expression {
	private final Expression expression;
	private final Type type;

	public InstanceOfExpression( Expression expression, Type type ) {
		this.expression = expression;
		this.type = type;
	}

	@Override
	public Expression cloneExpression( TransformationReason reason ) {
		return new InstanceOfExpression( expression, type );
	}

	@Override
	public Value evaluate() {
		boolean ret = true;
		try {
			type.check( expression.evaluate() );
		} catch( TypeCheckingException e ) {
			ret = false;
		}
		return Value.create( ret );
	}
}
