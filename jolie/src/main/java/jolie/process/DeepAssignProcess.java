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

package jolie.process;

import jolie.ExecutionThread;
import jolie.Interpreter;
import jolie.lang.parse.context.ParsingContext;
import jolie.runtime.VariablePath;
import jolie.runtime.expression.Expression;
import jolie.tracer.AssignmentTraceAction;

public class DeepAssignProcess implements Process {
	private final VariablePath leftPath;
	private final Expression rightExpression;
	private final ParsingContext context;

	public DeepAssignProcess( VariablePath leftPath, Expression rightExpression,
		ParsingContext context ) {
		this.leftPath = leftPath;
		this.rightExpression = rightExpression;
		this.context = context;
	}

	@Override
	public Process copy( TransformationReason reason ) {
		return new DeepAssignProcess(
			(VariablePath) leftPath.cloneExpression( reason ),
			rightExpression.cloneExpression( reason ),
			context );
	}

	@Override
	public void run() {
		if( ExecutionThread.currentThread().isKilled() )
			return;

		leftPath.getValue().deepAssign( rightExpression.evaluate() );

		Interpreter.getInstance().tracer().trace( () -> new AssignmentTraceAction(
			AssignmentTraceAction.Type.DEEP_ASSIGN,
			"DEEP ASSIGN",
			null,
			leftPath.getValue( ExecutionThread.currentThread().state().root().clone() ),
			context ) );
	}

	@Override
	public boolean isKillable() {
		return true;
	}
}
