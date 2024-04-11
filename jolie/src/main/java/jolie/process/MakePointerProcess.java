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
import jolie.tracer.AssignmentTraceAction;
import jolie.tracer.Tracer;

public class MakePointerProcess implements Process {
	final private VariablePath leftPath, rightPath;
	final private ParsingContext context;

	public MakePointerProcess( VariablePath leftPath, VariablePath rightPath, ParsingContext context ) {
		this.leftPath = leftPath;
		this.rightPath = rightPath;
		this.context = context;
	}

	// private void log( String description, Value value, String name ) {
	// final Tracer tracer = Interpreter.getInstance().tracer();
	// tracer.trace( () -> new AssignmentTraceAction(
	// AssignmentTraceAction.Type.POINTER,
	// name,
	// description,
	// value,
	// context ) );
	// }


	@Override
	public Process copy( TransformationReason reason ) {
		return new MakePointerProcess(
			(VariablePath) leftPath.cloneExpression( reason ),
			(VariablePath) rightPath.cloneExpression( reason ),
			context );
	}

	@Override
	public void run() {
		if( ExecutionThread.currentThread().isKilled() )
			return;

		leftPath.makePointer( rightPath );
		final Tracer tracer = Interpreter.getInstance().tracer();

		tracer.trace( () -> new AssignmentTraceAction(
			AssignmentTraceAction.Type.ASSIGNMENT,
			"POINTS",
			null,
			rightPath.getValue( ExecutionThread.currentThread().state().root().clone() ),
			context ) );

	}

	@Override
	public boolean isKillable() {
		return true;
	}


}
