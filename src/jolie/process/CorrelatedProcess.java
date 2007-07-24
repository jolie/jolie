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
import jolie.StatefulThread;
import jolie.StatelessThread;
import jolie.runtime.FaultException;

public class CorrelatedProcess implements Process
{
	private SequentialProcess sequence;
	
	private class Fields {
		private ExecutionThread waitingThread = null;	
	}
	
	public CorrelatedProcess( SequentialProcess sequence )
	{
		this.sequence = sequence;
	}
	
	private void startSession( Fields fields )
	{
		if ( Interpreter.stateMode() == Constants.StateMode.PERSISTENT )
			fields.waitingThread = new StatelessThread( ExecutionThread.currentThread(), sequence, this );
		else
			fields.waitingThread = new StatefulThread( sequence, ExecutionThread.currentThread(), this );
		
		fields.waitingThread.start();
	}
	
	public void run()
		throws FaultException
	{
		Fields fields = ExecutionThread.getLocalObject( this, Fields.class );
		if ( Interpreter.executionMode() != Constants.ExecutionMode.SINGLE ) {
			while( !Interpreter.exiting() ) {
				startSession( fields );
				synchronized( this ) {
					if ( fields.waitingThread != null ) { // We are still waiting for an input
						try {
							wait();
						} catch( InterruptedException ie ) {}
					}
				}
			}
		} else
			sequence.run();
	}
	
	public synchronized void inputReceived()
	{
		Fields fields = ExecutionThread.getLocalObject( this, Fields.class );
		if ( Interpreter.executionMode() == Constants.ExecutionMode.CONCURRENT ) {
			fields.waitingThread = null;
			notify();
		}
	}
	
	public synchronized void sessionTerminated()
	{
		Fields fields = ExecutionThread.getLocalObject( this, Fields.class );
		if ( Interpreter.executionMode() == Constants.ExecutionMode.SEQUENTIAL ) {
			fields.waitingThread = null;
			notify();
		}
	}
	
	public synchronized void signalFault( FaultException f )
	{
		Fields fields = ExecutionThread.getLocalObject( this, Fields.class );
		Interpreter.logUnhandledFault( f );
		
		if ( Interpreter.executionMode() == Constants.ExecutionMode.SEQUENTIAL ) {
			fields.waitingThread = null;
			notify();
		}
	}
}
