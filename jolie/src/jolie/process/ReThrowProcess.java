/*
 * Copyright (C) 2016 Martin Wolf.
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
import jolie.lang.Constants;
import jolie.runtime.ExitingException;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.VariablePathBuilder;
import jolie.runtime.expression.Expression;

public class ReThrowProcess implements Process
{
	private final Expression expression;

	public ReThrowProcess( Expression expression )
	{
		this.expression = expression;
	}

	public ReThrowProcess()
	{
		this.expression = null;
	}

	@Override
	public void run()
		throws FaultException, ExitingException
	{
		if ( ExecutionThread.currentThread().isKilled() ) {
			return;
		}

		ExecutionThread ethread = ExecutionThread.currentThread();

		Value scopeValue = new VariablePathBuilder( false )
			.add( ethread.currentScopeId(), 0 )
			.toVariablePath()
			.getValue();
		String faultName = scopeValue.getFirstChild( Constants.Keywords.DEFAULT_HANDLER_NAME ).strValue();

		Value faultValue = scopeValue.getChildren( faultName ).get( 0 );

		if ( expression == null ) {
			throw new FaultException( faultName, faultValue );
		} else {
			throw new FaultException( faultName, expression.evaluate() );
		}
	}

	@Override
	public Process clone( TransformationReason reason )
	{
		return new ReThrowProcess( this.expression );
	}

	@Override
	public boolean isKillable()
	{
		return true;
	}

}
