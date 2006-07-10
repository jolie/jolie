/***************************************************************************
 *   Copyright (C) by Fabrizio Montesi <famontesi@gmail.com>               *
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
 ***************************************************************************/

package jolie;

import java.util.Vector;

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
	
	public void run()
	{
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
	
	public void addPair( Condition condition, Process process )
	{
		pairs.add( new CPPair( condition, process ) );
	}
	
	public void setElseProcess( Process process )
	{
		elseProcess = process;
	}
}