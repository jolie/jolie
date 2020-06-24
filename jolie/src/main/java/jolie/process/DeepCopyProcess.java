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
import jolie.runtime.VariablePath;
import jolie.runtime.expression.Expression;
import jolie.tracer.AssignmentTraceAction;
import jolie.tracer.Tracer;

public class DeepCopyProcess implements Process {
	private final VariablePath leftPath;
	private final Expression rightExpression;
	private final boolean copyLinks;
	private final ParsingContext context;

	public DeepCopyProcess( VariablePath leftPath, Expression rightExpression, boolean copyLinks,
		ParsingContext context ) {
		this.leftPath = leftPath;
		this.rightExpression = rightExpression;
		this.copyLinks = copyLinks;
		this.context = context;
	}


	@Override
	public Process copy( TransformationReason reason ) {
		return new DeepCopyProcess(
			(VariablePath) leftPath.cloneExpression( reason ),
			rightExpression.cloneExpression( reason ),
			copyLinks,
			context );
	}

	@Override
	public void run() {
		if( ExecutionThread.currentThread().isKilled() )
			return;

		if( rightExpression instanceof VariablePath ) {
			leftPath.deepCopy( (VariablePath) rightExpression );
		} else {
			if( copyLinks ) {
				leftPath.getValue().deepCopyWithLinks( rightExpression.evaluate() );
			} else {
				leftPath.getValue().deepCopy( rightExpression.evaluate() );
			}
		}
		final Tracer tracer = Interpreter.getInstance().tracer();

		tracer.trace( () -> new AssignmentTraceAction(
			AssignmentTraceAction.Type.DEEPCOPY,
			"COPIED",
			null,
			leftPath.getValue( ExecutionThread.currentThread().state().root().clone() ),
			context ) );

	}

	@Override
	public boolean isKillable() {
		return true;
	}
}
