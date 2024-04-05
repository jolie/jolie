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

package jolie.process;

import jolie.ExecutionThread;
import jolie.Interpreter;
import jolie.lang.parse.context.ParsingContext;
import jolie.runtime.Value;
import jolie.runtime.VariablePath;
import jolie.runtime.expression.Expression;
import jolie.tracer.AssignmentTraceAction;
import jolie.tracer.Tracer;

/**
 * Assigns an expression value to a VariablePath.
 * 
 * @see Expression
 * @see VariablePath
 * @author Fabrizio Montesi
 */
public class AssignmentProcess implements Process, Expression {
	final private VariablePath varPath;
	final private Expression expression;
	final private ParsingContext context;

	/**
	 * Constructor.
	 * 
	 * @param varPath the variable which will receive the value
	 * @param expression the expression of which the evaluation will be stored in the variable
	 */
	public AssignmentProcess( VariablePath varPath, Expression expression, ParsingContext context ) {
		this.varPath = varPath;
		this.expression = expression;
		this.context = context;
	}

	@Override
	public Process copy( TransformationReason reason ) {
		return new AssignmentProcess(
			(VariablePath) varPath.cloneExpression( reason ),
			expression.cloneExpression( reason ),
			context );
	}

	@Override
	public Expression cloneExpression( TransformationReason reason ) {
		return new AssignmentProcess(
			(VariablePath) varPath.cloneExpression( reason ),
			expression.cloneExpression( reason ),
			context );
	}

	/** Evaluates the expression and stores its value in the variable. */
	@Override
	public void run() {
		if( ExecutionThread.currentThread().isKilled() )
			return;
		Value evaluationValue = expression.evaluate();
		varPath.getValue().assignValue( evaluationValue );
		final Tracer tracer = Interpreter.getInstance().tracer();
		tracer.trace( () -> new AssignmentTraceAction(
			AssignmentTraceAction.Type.ASSIGNMENT,
			"ASSIGN",
			null,
			evaluationValue.clone(),
			context ) );
	}

	@Override
	public Value evaluate() {
		Value val = varPath.getValue();
		val.assignValue( expression.evaluate() );
		return val;
	}

	@Override
	public boolean isKillable() {
		return true;
	}

}
