/*
 * Copyright (C) 2006-2015 Fabrizio Montesi <famontesi@gmail.com>
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
import jolie.runtime.ExitingException;
import jolie.runtime.FaultException;
import jolie.runtime.expression.Expression;

public final class IfProcess implements Process
{
	public static class CPPair
	{
		private final Expression condition;
		private final Process process;
		
		public CPPair( Expression condition, Process process )
		{
			this.condition = condition;
			this.process = process;
		}
		
		public Expression condition()
		{
			return condition;
		}
		
		public Process process()
		{
			return process;
		}
	}
	
	private final CPPair[] pairs;
	private final Process elseProcess;
	
	public IfProcess( CPPair[] pairs, Process elseProcess )
	{
		this.pairs = pairs;
		this.elseProcess = elseProcess;
	}
	
	@Override
	public Process clone( TransformationReason reason )
	{
		CPPair[] pairsCopy = new CPPair[ pairs.length ];
		for( int i = 0; i < pairs.length; i++ ) {
			pairsCopy[ i ] = new CPPair(
				pairs[ i ].condition.cloneExpression( reason ),
				pairs[ i ].process.clone( reason )
			);
		}
		return new IfProcess(
			pairsCopy,
			( elseProcess == null ) ? null : elseProcess.clone( reason )
		);
	}
	
	@Override
	public void run()
		throws FaultException, ExitingException
	{
		if ( ExecutionThread.currentThread().isKilled() ) {
			return;
		}

		boolean keepRun = true;
		int i = 0;
	
		while( keepRun && i < pairs.length ) {
			final CPPair pair = pairs[ i ];
			if ( pair.condition().evaluate().boolValue() ) {
				keepRun = false;
				pair.process().run();
			}
			i++;
		}

		// No valid condition found, run the else process
		if ( keepRun && elseProcess != null ) {
			elseProcess.run();
		}
	}
	
	@Override
	public boolean isKillable()
	{
		return true;
	}
}