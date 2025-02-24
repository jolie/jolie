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


import java.util.Collection;
import java.util.HashSet;

import jolie.ExecutionThread;
import jolie.TransparentExecutionThread;
import jolie.process.Process;

public class ParallelExecution {
	private class ParallelThread extends TransparentExecutionThread {
		public ParallelThread( Process process ) {
			super( process, ExecutionThread.currentThread() );
		}

		@Override
		public void runProcess() {
			try {
				try {
					process().run();
					terminationNotify( this );
				} catch( FaultException.RuntimeFaultException rf ) {
					throw rf.faultException();
				}
			} catch( FaultException f ) {
				signalFault( this, f );
			} catch( ExitingException f ) {
				terminationNotify( this );
			}
		}
	}

	final private Collection< ParallelThread > threads = new HashSet<>();
	private FaultException fault = null;
	private boolean isKilled = false;

	public ParallelExecution( Process[] procs ) {
		for( Process proc : procs ) {
			threads.add( new ParallelThread( proc ) );
		}
	}

	public void run()
		throws FaultException {
		synchronized( this ) {
			for( ParallelThread t : threads ) {
				t.start();
			}

			ExecutionThread ethread;
			while( fault == null && !threads.isEmpty() ) {
				ethread = ExecutionThread.currentThread();
				try {
					ethread.setCanBeInterrupted( true );
					wait();
					ethread.setCanBeInterrupted( false );
				} catch( InterruptedException e ) {
					synchronized( this ) {
						if( ethread.isKilled() && !threads.isEmpty() ) {
							isKilled = true;
							for( ParallelThread t : threads ) {
								t.kill( ethread.killerFault() );
							}
							try {
								wait();
							} catch( InterruptedException ie ) {
							}
						}
					}
				}
			}

			if( fault != null ) {
				for( ParallelThread t : threads ) {
					t.kill( fault );
				}
				while( !threads.isEmpty() ) {
					try {
						wait();
					} catch( InterruptedException e ) {
					}
				}
				throw fault;
			}
		}
	}

	private void terminationNotify( ParallelThread thread ) {
		synchronized( this ) {
			threads.remove( thread );

			if( threads.isEmpty() ) {
				notify();
			}
		}
	}


	private void signalFault( ParallelThread thread, FaultException f ) {
		synchronized( this ) {
			threads.remove( thread );

			if( !isKilled && fault == null ) {
				fault = f;
				notify();
			} else if( threads.isEmpty() ) {
				notify();
			}
		}
	}

}
