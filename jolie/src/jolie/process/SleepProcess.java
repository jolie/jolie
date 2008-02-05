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

import java.util.HashMap;
import java.util.Map.Entry;

import jolie.ExecutionThread;
import jolie.ProcessThread;
import jolie.net.CommChannel;
import jolie.net.CommMessage;
import jolie.runtime.Expression;
import jolie.runtime.InputHandler;

public class SleepProcess implements InputProcess
{
	public class SleepInputHandler extends ProcessThread implements InputHandler
	{
		private InputProcessExecution inputProcess;
		private Expression expression;
		private ExecutionThread ethread;
		private SleepProcess sleepProcess;

		public SleepInputHandler( SleepProcess sleepProcess, Expression expression )
		{
			this.sleepProcess = sleepProcess;
			this.expression = expression;
		}

		public ExecutionThread executionThread()
		{
			return ethread;
		}

		public void run()
		{
			int i = expression.evaluate().intValue();
			try {
				if ( i > 0 )
					Thread.sleep( i );
				if ( inputProcess != null ) {
					inputProcess.recvMessage( null, new CommMessage( id() ) );
					synchronized( ethread ) {
						ethread.notify();
					}
				}
			} catch ( InterruptedException e ) {
			} finally {
				SleepProcess.removeHandler( this );
			}
		}

		public String id()
		{
			return sleepProcess.toString();
		}

		public synchronized void signForMessage( InputProcessExecution process )
		{
			inputProcess = process;
			ethread = ExecutionThread.currentThread();
			if ( this.getState() == Thread.State.NEW )
				this.start();
		}

		public synchronized void cancelWaiting( InputProcessExecution process )
		{
			if ( inputProcess == process )
				inputProcess = null;

			this.interrupt();
		}
	}
	
	private Expression expression;
	
	private static HashMap< ExecutionThread, SleepInputHandler > map =
		new HashMap< ExecutionThread, SleepInputHandler >(); 
	
	public SleepProcess( Expression expression )
	{
		this.expression = expression;
	}
	
	public Process clone( TransformationReason reason )
	{
		return new SleepProcess( expression );
	}
	
	/**
	 * @todo Improve performance
	 */
	public synchronized static void removeHandler( SleepInputHandler h )
	{
		for( Entry< ExecutionThread, SleepInputHandler > entry : map.entrySet() ) {
			if( entry.getValue() == h ) {
				map.entrySet().remove( entry );
				break;
			}
		}
	}
	
	public void run()
	{
		int i = expression.evaluate().intValue();
		try {
			if ( i > 0 )
				Thread.sleep( i );
		} catch( InterruptedException e ) {}
	}
	
	public synchronized InputHandler getInputHandler()
	{
		ExecutionThread ethread = ExecutionThread.currentThread();

		SleepInputHandler h = map.get( ethread );
		if ( h == null ) {
			h = new SleepInputHandler( this, expression );
			map.put( ethread, h );
		}
		return h;
	}
	
	public void runBehaviour( CommChannel channel, CommMessage message )
	{}
}
