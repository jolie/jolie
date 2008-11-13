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
import jolie.Interpreter;
import jolie.net.CommChannel;
import jolie.net.CommMessage;
import jolie.runtime.FaultException;
import jolie.runtime.InputHandler;
import jolie.runtime.InputOperation;
import jolie.runtime.Value;
import jolie.runtime.VariablePath;

public class OneWayProcess implements CorrelatedInputProcess, InputOperationProcess
{
	private class Execution extends AbstractInputProcessExecution< OneWayProcess >
	{
		protected CommMessage message = null;
		protected CommChannel channel = null;
		
		public Execution( OneWayProcess parent )
		{
			super( parent );
		}
		
		public Process clone( TransformationReason reason )
		{
			return new Execution( parent );
		}

		public void run()
			throws FaultException
		{
			try {
				operation.signForMessage( this );
				synchronized( this ) {
					if( message == null ) {
						ExecutionThread ethread = ExecutionThread.currentThread();
						ethread.setCanBeInterrupted( true );
						this.wait();
						ethread.setCanBeInterrupted( false );
					}
				}
				parent.runBehaviour( null, message );
			} catch( InterruptedException ie ) {
				parent.operation.cancelWaiting( this );
			}
		}

		public synchronized boolean recvMessage( CommChannel channel, CommMessage message )
		{
			if ( parent.correlatedProcess != null ) {
				if ( Interpreter.getInstance().exiting() ) {
					// Do not trigger session spawning if we're exiting
					return false;
				}
				parent.correlatedProcess.inputReceived();
			}

			this.message = message;
			this.notify();
			return true;
		}
		
		public boolean isKillable()
		{
				return true;
		}
	}
	
	final protected InputOperation operation;
	final protected VariablePath varPath;
	protected CorrelatedProcess correlatedProcess = null;

	public OneWayProcess(
			InputOperation operation,
			VariablePath varPath
			)
	{
		this.operation = operation;
		this.varPath = varPath;
	}
	
	public Process clone( TransformationReason reason )
	{
		return new OneWayProcess( operation, varPath );
	}
	
	public void setCorrelatedProcess( CorrelatedProcess process )
	{
		this.correlatedProcess = process;
	}
	
	public VariablePath inputVarPath()
	{
		return varPath;
	}

	public void run()
		throws FaultException
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
		if ( varPath != null ) {
			Value val = varPath.getValue();
			val.erase();
			val.deepCopy( message.value() );
		}

		try {
			if ( channel != null )
				channel.disposeForInput();
		} catch( IOException ioe ) {}
	}
	
	public boolean isKillable()
	{
		return true;
	}
}
