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

import java.io.IOException;

import jolie.ExecutionThread;
import jolie.net.CommChannel;
import jolie.net.CommMessage;
import jolie.runtime.GlobalVariablePath;
import jolie.runtime.InputHandler;
import jolie.runtime.InputOperation;

public class OneWayProcess implements CorrelatedInputProcess, InputOperationProcess
{
	private class Execution implements InputProcessExecution
	{
		private CommMessage message = null;
		private OneWayProcess parent;
		
		public Execution( OneWayProcess parent )
		{
			this.parent = parent;
		}
		
		public Process parent()
		{
			return parent;
		}

		public void run()
		{
			try {
				parent.operation.signForMessage( this );
				synchronized( this ) {
					if( message == null )
						this.wait();
				}
				parent.runBehaviour( null, message );
			} catch( InterruptedException ie ) {
				parent.operation.cancelWaiting( this );
			}
		}

		public synchronized void recvMessage( CommChannel channel, CommMessage message )
		{
			if ( parent.correlatedProcess != null )
				parent.correlatedProcess.inputReceived();

			this.message = message;
			this.notify();
			try {
				channel.close();
			} catch( IOException ioe ) {}
		}
	}
	
	protected InputOperation operation;
	protected GlobalVariablePath varPath;
	protected CorrelatedProcess correlatedProcess = null;

	public OneWayProcess( InputOperation operation, GlobalVariablePath varPath )
	{
		this.operation = operation;
		this.varPath = varPath;
	}
	
	public void setCorrelatedProcess( CorrelatedProcess process )
	{
		this.correlatedProcess = process;
	}
	
	public GlobalVariablePath inputVarPath()
	{
		return varPath;
	}

	public void run()
	{
		if ( ExecutionThread.currentThread().isKilled() )
			return;
		(new Execution( this )).run();
	}
	
	public InputHandler getInputHandler()
	{
		return operation;
	}
	
	public void runBehaviour( CommChannel channel, CommMessage message )
	{
		if ( varPath != null )
			varPath.getValue().deepCopy( message.value() );
	}
}
