/***************************************************************************
 *   Copyright (C) 2006-2015 by Fabrizio Montesi <famontesi@gmail.com>     *
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
import jolie.lang.parse.context.ParsingContext;
import jolie.net.ports.OutputPort;
import jolie.runtime.ExitingException;
import jolie.runtime.FaultException;
import jolie.runtime.VariablePath;
import jolie.runtime.expression.Expression;
import jolie.runtime.expression.SolicitResponseExpression;
import jolie.runtime.typing.RequestResponseTypeDescription;

public class SolicitResponseProcess implements Process {
	private final String operationId;
	private final OutputPort outputPort;
	private final VariablePath inputVarPath; // may be null
	private final Expression outputExpression; // may be null
	private final Process installProcess; // may be null
	private final RequestResponseTypeDescription types;
	private final ParsingContext context;

	public SolicitResponseProcess(
		String operationId,
		OutputPort outputPort,
		Expression outputExpression,
		VariablePath inputVarPath,
		Process installProcess,
		RequestResponseTypeDescription types,
		ParsingContext context ) {
		this.operationId = operationId;
		this.outputPort = outputPort;
		this.outputExpression = outputExpression;
		this.inputVarPath = inputVarPath;
		this.installProcess = installProcess;
		this.types = types;
		this.context = context;
	}

	@Override
	public Process copy( TransformationReason reason ) {
		return new SolicitResponseProcess(
			operationId,
			outputPort,
			(outputExpression == null) ? null : outputExpression.cloneExpression( reason ),
			(inputVarPath == null) ? null : (VariablePath) inputVarPath.cloneExpression( reason ),
			(installProcess == null) ? null : installProcess.copy( reason ),
			types,
			context );
	}

	@Override
	public void run()
		throws FaultException {
		if( ExecutionThread.currentThread().isKilled() ) {
			return;
		}

		SolicitResponseExpression solicitResponseExpression =
			new SolicitResponseExpression(
				operationId,
				outputPort,
				outputExpression,
				types,
				context,
				inputVarPath );

		try {
			solicitResponseExpression.evaluate();
		} catch( FaultException.RuntimeFaultException e ) {
			throw e.faultException();
		}

		try {
			installProcess.run();
		} catch( ExitingException e ) {
			assert false;
		}
	}

	@Override
	public boolean isKillable() {
		return true;
	}
}
