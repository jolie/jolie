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


package jolie.runtime.expression;


import java.util.concurrent.locks.Condition;

import jolie.process.TransformationReason;
import jolie.runtime.Value;

/**
 * Provides the support for a "logical and" chain of other conditions.
 *
 * @author Fabrizio Montesi
 * @see Condition
 */
public class AndCondition implements Expression {
	private final Expression[] children;

	/** Constructor */
	public AndCondition( Expression[] children ) {
		this.children = children;
	}

	@Override
	public Expression cloneExpression( TransformationReason reason ) {
		return new AndCondition( children );
	}

	/**
	 * Applies the "logical and" rule. Implemented as short and: starting from left, the first condition
	 * that evaluates as false makes this "logical and" condition evaluation returning false, without
	 * checking the other conditions.
	 *
	 * @return true if every condition is satisfied, false otherwise.
	 */
	@Override
	public Value evaluate() {
		for( Expression condition : children ) {
			if( condition.evaluate().boolValue() == false ) {
				return Value.create( false );
			}
		}

		return Value.create( true );
	}
}
