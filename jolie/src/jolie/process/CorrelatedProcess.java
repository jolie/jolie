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

import jolie.lang.Constants;
import jolie.ExecutionThread;
import jolie.Interpreter;
import jolie.SessionListener;
import jolie.SessionThread;
import jolie.runtime.ExitingException;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.VariablePathBuilder;

public class CorrelatedProcess implements Process, SessionListener
{
	final private Interpreter interpreter;
	final private Process process;
	private boolean mustWait = false;
	private SessionThread spawnModel;
	private int activeSessions = 0;
	
	public CorrelatedProcess( Interpreter interpreter, Process process )
	{
		this.interpreter = interpreter;
		this.process = process;
		interpreter.registerSessionSpawner( this );
	}
	
	public Process clone( TransformationReason reason )
	{
		interpreter.unregisterSessionSpawner( this );
		return new CorrelatedProcess( interpreter, process.clone( reason ) );
	}
	
	private void startSession()
	{
		mustWait = true;
		SessionThread session = spawnModel.clone();
		session.addSessionListener( this );
		session.start();
	}
	
	public void run()
		throws FaultException
	{
		spawnModel = new SessionThread( process, ExecutionThread.currentThread() );
		if ( interpreter.executionMode() == Constants.ExecutionMode.SINGLE ) {
			try {
				process.run();
			} catch( ExitingException e ) {}
		} else {
			while( !interpreter.exiting() ) {
				startSession();
				synchronized( this ) {
					if ( mustWait && !interpreter.exiting() ) { // We are still waiting for an input
						try {
							wait();
						} catch( InterruptedException e ) {}
					}
				}
			}
			synchronized( this ) {
				while( activeSessions > 0 ) {
					try {
						wait();
					} catch( InterruptedException e ) {}
				}
			}
		}
	}
	
	public synchronized void interpreterExit()
	{
		if ( mustWait ) {
			notify();
		}
	}
	
	public synchronized void inputReceived()
	{
		activeSessions++;
		if ( interpreter.executionMode() == Constants.ExecutionMode.CONCURRENT ) {
			mustWait = false;
			notify();
		}
	}
	
	public synchronized void sessionExecuted( SessionThread session )
	{
		activeSessions--;
		if ( interpreter.executionMode() == Constants.ExecutionMode.SEQUENTIAL ) {
			mustWait = false;
			notify();
		} else if ( interpreter.exiting() ) {
			notify();
		}
	}
	
	public void sessionError( SessionThread ethread, FaultException f )
	{
		Process p = null;
		while( ethread.hasScope() && (p=ethread.getFaultHandler( f.faultName(), true )) == null ) {
			ethread.popScope();
		}
		
		try {
			if ( p == null ) {
				Interpreter.getInstance().logUnhandledFault( f );
			} else {
				Value scopeValue =
						new VariablePathBuilder( false )
						.add( ethread.currentScopeId(), 0 )
						.toVariablePath()
						.getValue();
				scopeValue.getChildren( f.faultName() ).set( 0, f.value() );
				try {
					p.run();
				} catch( ExitingException e ) {}
			}
		
			synchronized( this ) {
				if ( Interpreter.getInstance().executionMode() == Constants.ExecutionMode.SEQUENTIAL ) {
					mustWait = false;
					notify();
				}
			}
		} catch( FaultException fault ) {
			sessionError( ethread, fault );
		}
		sessionExecuted( ethread );
	}
	
	public boolean isKillable()
	{
		return true;
	}
}
