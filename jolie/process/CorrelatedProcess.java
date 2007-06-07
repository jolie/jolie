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
import jolie.CorrelatedThread;
import jolie.Interpreter;
import jolie.StatefulThread;
import jolie.StatelessThread;
import jolie.runtime.FaultException;

public class CorrelatedProcess implements Process
{
	private SequentialProcess sequence;
	private boolean keepRun = false;
	private int runningSessions = 0;
	
	public CorrelatedProcess( SequentialProcess sequence )
	{
		this.sequence = sequence;
	}
	
	private void startSession( boolean async )
	{
		CorrelatedThread thread;
		if ( Interpreter.stateMode() == Constants.StateMode.PERSISTENT )
			thread = new StatelessThread( CorrelatedThread.currentThread(), sequence, this );
		else
			thread = new StatefulThread( sequence, CorrelatedThread.currentThread(), this );
		
		runningSessions++;
		thread.start();
		if ( async == false ) {
			try {
				synchronized( this ) {
					wait();
				}
			} catch( InterruptedException e ) {}
		}
	}
	
	public void run()
	{
		//startSession( false );
		do {
			keepRun = false;
			startSession( false );
		} while( keepRun );
		
		while( runningSessions > 0 ) {
			try {
				synchronized( this ) {
					wait();
				}
			} catch( InterruptedException e ) {}
		}
	}
	
	public void inputReceived()
	{
		if ( Interpreter.executionMode() == Constants.ExecutionMode.CONCURRENT ) {
			startSession( true );
		} else
			keepRun = true;
	}
	
	public synchronized void sessionTerminated()
	{
		runningSessions--;
		notify();
	}
	
	public synchronized void signalFault( FaultException f )
	{
		Interpreter.logger().severe(
					"Uncaught fault( " + f.fault() +
					" )\nJava stack trace follows..." );
		f.printStackTrace();
		runningSessions--;
		notify();
	}
}
