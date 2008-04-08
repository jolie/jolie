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

import java.util.Vector;

import jolie.ExecutionThread;
import jolie.runtime.Condition;
import jolie.runtime.FaultException;


public class IfProcess implements Process
{
	private class CPPair
	{
		private Condition condition;
		private Process process;
		
		public CPPair( Condition condition, Process process )
		{
			this.condition = condition;
			this.process = process;
		}
		
		public Condition condition()
		{
			return condition;
		}
		
		public Process process()
		{
			return process;
		}
	}
	
	private Vector< CPPair > pairs;
	private Process elseProcess;
	
	public IfProcess()
	{
		pairs = new Vector< CPPair >();
		elseProcess = null;
	}
	
	public Process clone( TransformationReason reason )
	{
		IfProcess p = new IfProcess();
		for( CPPair pair : pairs )
			p.addPair( pair.condition.cloneCondition( reason ), pair.process.clone( reason ) );
		p.elseProcess = elseProcess.clone( reason );
		return p;
	}
	
	public void run()
		throws FaultException
	{
		if ( ExecutionThread.currentThread().isKilled() )
			return;

		boolean stop = false;
		int i = 0;
		int size = pairs.size();
		CPPair pair;
		
		while( !stop && i < size ) {
			pair = pairs.elementAt( i );
			if ( pair.condition().evaluate() ) {
				stop = true;
				pair.process().run();
			}
			i++;
		}

		// No valid condition found, run the else process
		if ( !stop && elseProcess != null )
			elseProcess.run();
	}
	
	/*
	 * @todo -- eliminate these methods by embedding them in the constructor
	 */
	public void addPair( Condition condition, Process process )
	{
		pairs.add( new CPPair( condition, process ) );
	}
	
	public void setElseProcess( Process process )
	{
		elseProcess = process;
	}
}