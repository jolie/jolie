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
import jolie.lang.Constants;
import jolie.runtime.ExitingException;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.VariablePathBuilder;

public class ScopeProcess implements Process {
	private class Execution {
		final private ScopeProcess parent;
		final private ExecutionThread ethread;
		private boolean shouldMerge = true;
		private FaultException fault = null;

		public Execution( ScopeProcess parent ) {
			this.parent = parent;
			this.ethread = ExecutionThread.currentThread();
		}

		public void run()
			throws FaultException, ExitingException {
			ethread.pushScope( parent.id );
			runScope( parent.process );
			if( autoPop ) {
				ethread.popScope( shouldMerge );
			}
			if( shouldMerge && fault != null ) {
				throw fault;
			}
		}

		private void runScope( Process p )
			throws ExitingException {
			try {
				try {
					p.run();
					if( ethread.isKilled() ) {
						shouldMerge = false;
						p = ethread.getCompensation( id );
						if( p != null ) { // Termination handling
							FaultException f = ethread.killerFault();
							ethread.clearKill();
							this.runScope( p );
							ethread.kill( f );
						}
					}
				} catch( FaultException.RuntimeFaultException rf ) {
					throw rf.faultException();
				}
			} catch( FaultException f ) {
				p = ethread.getFaultHandler( f.faultName(), true );

				if( p != null ) {
					Value scopeValue =
						new VariablePathBuilder( false )
							.add( ethread.currentScopeId(), 0 )
							.toVariablePath()
							.getValue();
					scopeValue.getChildren( f.faultName() ).set( 0, f.value() );
					scopeValue.getFirstChild( Constants.DEFAULT_HANDLER_NAME ).setValue( f.faultName() );
					this.runScope( p );
				} else {
					fault = f;
				}
			} catch( ExitingException e ) {
				throw e;
			} catch( Exception e ) {
				fault = new FaultException( e ); // TODO find and add context?
			}
		}
	}

	private final String id;
	private final Process process;
	private final boolean autoPop;

	public ScopeProcess( String id, Process process, boolean autoPop ) {
		this.id = id;
		this.process = process;
		this.autoPop = autoPop;
	}

	public ScopeProcess( String id, Process process ) {
		this( id, process, true );
	}

	@Override
	public Process copy( TransformationReason reason ) {
		return new ScopeProcess( id, process.copy( reason ), autoPop );
	}

	@Override
	public void run()
		throws FaultException, ExitingException {
		(new Execution( this )).run();
	}

	@Override
	public boolean isKillable() {
		return process.isKillable();
	}
}
