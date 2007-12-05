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

package jolie.runtime;

import java.util.Vector;

import jolie.ExecutionThread;
import jolie.StatelessThread;
import jolie.process.Process;

class ParallelThread extends StatelessThread
{
	private ParallelExecution parent;
	
	public ParallelThread( Process process, ParallelExecution parent )
	{
		super( ExecutionThread.currentThread(), process );
		this.parent = parent;
	}

	public void run()
	{
		try {
			process().run();
		} catch( FaultException f ) {
			parent.signalFault( this, f );
		}
		parent.terminationNotify( this );
	}
}

public class ParallelExecution
{
	private Vector< ParallelThread > threads = new Vector< ParallelThread >();
	private FaultException fault = null;
	//private ExecutionThread ethread;

	public ParallelExecution( Vector< Process > procs )
	{
		for( Process proc : procs )
			threads.add( new ParallelThread( proc, this ) );
		
		//ethread = ExecutionThread.currentThread();
	}
	
	public void run()
		throws FaultException
	{
		synchronized( this ) {
			for( ParallelThread t : threads )
				t.start();

			try {
				wait();
			} catch( InterruptedException e ) {}
		
			if ( fault != null ) {
				for( ParallelThread t : threads )
					t.kill();
			}
		
			while ( threads.size() > 0 ) {
				try {
					//threads.firstElement().join();
					wait();
				} catch( InterruptedException e ) {}
			}
		
			if ( fault != null )
				throw fault;
		}
	}
	
	public synchronized void terminationNotify( ParallelThread thread )
	{
		threads.remove( thread );
				
		if ( threads.size() < 1 )
			notify();
	}
	
		
	public synchronized void signalFault( ParallelThread thread, FaultException f )
	{
		threads.remove( thread );
		fault = f;
		notify();
	}
}
