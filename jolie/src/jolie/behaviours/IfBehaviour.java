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

package jolie.behaviours;

import jolie.StatefulContext;
import jolie.runtime.ExitingException;
import jolie.runtime.FaultException;
import jolie.runtime.expression.Expression;

public final class IfBehaviour implements Behaviour
{
	public static class CPPair
	{
		private final Expression condition;
		private final Behaviour process;
		
		public CPPair( Expression condition, Behaviour process )
		{
			this.condition = condition;
			this.process = process;
		}
		
		public Expression condition()
		{
			return condition;
		}
		
		public Behaviour process()
		{
			return process;
		}
	}
	
	private final CPPair[] pairs;
	private final Behaviour elseProcess;
	
	public IfBehaviour( CPPair[] pairs, Behaviour elseProcess )
	{
		this.pairs = pairs;
		this.elseProcess = elseProcess;
	}
	
	@Override
	public Behaviour clone( TransformationReason reason )
	{
		CPPair[] pairsCopy = new CPPair[ pairs.length ];
		for( int i = 0; i < pairs.length; i++ ) {
			pairsCopy[ i ] = new CPPair(
				pairs[ i ].condition.cloneExpression( reason ),
				pairs[ i ].process.clone( reason )
			);
		}
		return new IfBehaviour(
			pairsCopy,
			( elseProcess == null ) ? null : elseProcess.clone( reason )
		);
	}
	
	@Override
	public void run(StatefulContext ctx)
		throws FaultException, ExitingException
	{
		if ( ctx.isKilled() ) {
			return;
		}

		boolean keepRun = true;
		int i = 0;
	
		while( keepRun && i < pairs.length ) {
			final CPPair pair = pairs[ i ];
			if ( pair.condition().evaluate().boolValue() ) {
				ctx.executeNext( pair.process() );
				return;
			}
			i++;
		}

		// No valid condition found, run the else process
		if ( elseProcess != null ) {
			ctx.executeNext( elseProcess );
		}
	}
	
	@Override
	public boolean isKillable()
	{
		return true;
	}
}