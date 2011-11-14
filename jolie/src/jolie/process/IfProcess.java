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
import jolie.runtime.ExitingException;
import jolie.runtime.FaultException;
import jolie.runtime.expression.Expression;


public class IfProcess implements Process
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
	
	public void run()
		throws FaultException, ExitingException
	{
		if ( ExecutionThread.currentThread().isKilled() ) {
			return;
		}

		boolean keepRun = true;
		int i = 0;
		CPPair pair;
		
		while( keepRun && i < pairs.length ) {
			pair = pairs[ i ];
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
	
	public boolean isKillable()
	{
		return true;
	}
}