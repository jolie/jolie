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
import jolie.runtime.FaultException;

public class ScopeProcess implements Process
{
	private class Execution
	{
		private ScopeProcess parent;
		private ExecutionThread ethread;
		private boolean shouldMerge = true;
		private FaultException fault = null;
		
		public Execution( ScopeProcess parent )
		{
			this.parent = parent;
			this.ethread = ExecutionThread.currentThread();
		}
		
		public void run()
			throws FaultException
		{
			ethread.pushScope( parent.id );
			runScope( parent.process );
			ethread.popScope( shouldMerge );
			if ( shouldMerge && fault != null )
				throw fault;
		}
		
		private void runScope( Process p )
		{
			try {
				p.run();
				if ( ethread.isKilled() ) {
					shouldMerge = false;
					p = ethread.getCompensation( id );
					if ( p != null ) {
						ethread.clearKill();
						this.runScope( p );
					}
				}
			} catch( FaultException f ) {
				p = ethread.getFaultHandler( f.fault(), true );
				if ( p != null ) {
					this.runScope( p );
				} else
					fault = f;
			}
		}
	}
	
	protected String id;
	protected Process process;
	
	public ScopeProcess( String id, Process process )
	{
		this.id = id;
		this.process = process;
	}
	
	public Process clone( TransformationReason reason )
	{
		return new ScopeProcess( id, process.clone( reason ) );
	}
	
	public void run()
		throws FaultException
	{
		(new Execution( this )).run();
	}
}
