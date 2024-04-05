/*
 * Copyright (C) 2022 Fabrizio Montesi <famontesi@gmail.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */


package jolie.runtime.expression;

import jolie.process.TransformationReason;
import jolie.runtime.Value;

public class IfExpression implements Expression {
	private final Expression guard, thenExpression, elseExpression;

	public IfExpression( Expression guard, Expression thenExpression, Expression elseExpression ) {
		this.guard = guard;
		this.thenExpression = thenExpression;
		this.elseExpression = elseExpression;
	}

	@Override
	public Expression cloneExpression( TransformationReason reason ) {
		return new IfExpression( guard.cloneExpression( reason ), thenExpression.cloneExpression( reason ),
			elseExpression.cloneExpression( reason ) );
	}

	@Override
	public Value evaluate() {
		return guard.evaluate().boolValue() ? thenExpression.evaluate() : elseExpression.evaluate();
	}
}
