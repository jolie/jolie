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

package jolie.behaviours;

import jolie.StatefulContext;
import jolie.runtime.Value;
import jolie.runtime.VariablePath;
import jolie.runtime.expression.Expression;

/** Assigns an expression value to a VariablePath.
 * @see Expression
 * @see VariablePath
 * @author Fabrizio Montesi
 */
public class AssignmentBehaviour implements Behaviour, Expression
{
	final private VariablePath varPath;
	final private Expression expression;

	/** Constructor.
	 * 
	 * @param varPath the variable which will receive the value
	 * @param expression the expression of which the evaluation will be stored in the variable
	 */
	public AssignmentBehaviour( VariablePath varPath, Expression expression )
	{
		this.varPath = varPath;
		this.expression = expression;
	}
	
	public Behaviour clone( TransformationReason reason )
	{
		return new AssignmentBehaviour(
					(VariablePath)varPath.cloneExpression( reason ),
					expression.cloneExpression( reason )
				);
	}
	
	public Expression cloneExpression( TransformationReason reason )
	{
		return new AssignmentBehaviour(
					(VariablePath)varPath.cloneExpression( reason ),
					expression.cloneExpression( reason )
				);
	}
	
	/** Evaluates the expression and stores its value in the variable. */
	public void run(StatefulContext ctx)
	{
		if ( ctx.isKilled() )
			return;
		varPath.getValue( ctx ).assignValue( expression.evaluate() );
	}
	
	public Value evaluate()
	{
		Value val = varPath.getValue();
		val.assignValue( expression.evaluate() );
		return val;
	}
	
	public boolean isKillable()
	{
		return true;
	}
}
