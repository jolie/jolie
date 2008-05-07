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

import jolie.Constants;
import jolie.ExecutionThread;
import jolie.Interpreter;
import jolie.SessionThread;
import jolie.runtime.FaultException;

public class CorrelatedProcess implements Process
{
	private Process process;
	private boolean waiting = false;
	private SessionThread spawnModel;
	
	public CorrelatedProcess( Process process )
	{
		this.process = process;
	}
	
	public Process clone( TransformationReason reason )
	{
		return new CorrelatedProcess( process.clone( reason ) );
	}
	
	private void startSession()
	{
		waiting = true;
		spawnModel.clone().start();
	}
	
	public void run()
		throws FaultException
	{
		spawnModel = new SessionThread( process, ExecutionThread.currentThread(), this );
		Interpreter interpreter = Interpreter.getInstance();
		if ( interpreter.executionMode() != Constants.ExecutionMode.SINGLE ) {
			while( !interpreter.exiting() ) {
				startSession();
				synchronized( this ) {
					if ( waiting ) { // We are still waiting for an input
						try {
							wait();
						} catch( InterruptedException ie ) {}
					}
				}
			}
		} else
			process.run();
	}
	
	public synchronized void inputReceived()
	{
		if ( Interpreter.getInstance().executionMode() == Constants.ExecutionMode.CONCURRENT ) {
			waiting = false;
			notify();
		}
	}
	
	public synchronized void sessionTerminated()
	{
		if ( Interpreter.getInstance().executionMode() == Constants.ExecutionMode.SEQUENTIAL ) {
			waiting = false;
			notify();
		}
	}
	
	public void signalFault( FaultException f )
	{
		ExecutionThread ethread = ExecutionThread.currentThread();
		Process p = null;
		while( ethread.hasScope() && (p=ethread.getFaultHandler( f.faultName(), true )) == null )
			ethread.popScope();
		
		try {
			if ( p == null )
				Interpreter.getInstance().logUnhandledFault( f );
			else
				p.run();
		
			synchronized( this ) {
				if ( Interpreter.getInstance().executionMode() == Constants.ExecutionMode.SEQUENTIAL ) {
					waiting = false;
					notify();
				}
			}
		} catch( FaultException fault ) {
			signalFault( fault );
		}
	}
}
