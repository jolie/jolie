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

import java.util.function.BiPredicate;
import jolie.process.TransformationReason;
import jolie.runtime.Value;


/**
 * @author Fabrizio Montesi
 * TODO Clean up code.
 *
 */
public class CompareCondition implements Expression
{
	private final Expression leftExpression, rightExpression;
	private final BiPredicate< Value, Value > compareOperator;
	
	public CompareCondition( Expression left, Expression right, BiPredicate< Value, Value > compareOperator )
	{
		this.leftExpression = left;
		this.rightExpression = right;
		this.compareOperator = compareOperator;
	}
	
	public Expression cloneExpression( TransformationReason reason )
	{
		return new CompareCondition(
					leftExpression.cloneExpression( reason ),
					rightExpression.cloneExpression( reason ),
					compareOperator
				);
	}
	
	public Value evaluate()
	{
		return Value.create( compareOperator.test( leftExpression.evaluate(), rightExpression.evaluate() ) );
	}
}
