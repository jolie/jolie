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
import jolie.Interpreter;
import jolie.net.CommChannel;
import jolie.net.CommMessage;
import jolie.runtime.ExitingException;
import jolie.runtime.FaultException;
import jolie.runtime.InputHandler;
import jolie.runtime.OneWayOperation;
import jolie.runtime.VariablePath;
import jolie.runtime.typing.TypeCheckingException;

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

		public void interpreterExit()
		{
			synchronized( this ) {
				this.notify();
			}
		}

		protected void runImpl()
			throws FaultException, ExitingException
		{
			try {
				operation.signForMessage( this );
				synchronized( this ) {
					if ( message == null && !Interpreter.getInstance().exiting() ) {
						ExecutionThread ethread = ExecutionThread.currentThread();
						ethread.setCanBeInterrupted( true );
						this.wait();
						ethread.setCanBeInterrupted( false );
					}
				}

				if ( message == null ) { // If message == null, we are exiting
					throw new ExitingException();
				} else {
					parent.runBehaviour( channel, message );
				}
			} catch( InterruptedException ie ) {
				parent.operation.cancelWaiting( this );
			}
		}

		public synchronized boolean recvMessage( CommChannel channel, CommMessage message )
		{
			try {
				checkMessageType( message );
			} catch( TypeCheckingException e ) {
				Interpreter.getInstance().logWarning( "Received message TypeMismatch (One-Way input operation " + operation.id() + "): " + e.getMessage() );
				return false;
			}
			
			if ( parent.correlatedProcess != null ) {
				if ( Interpreter.getInstance().exiting() ) {
					this.notify();
					// Do not trigger session spawning if we're exiting
					return false;
				}
				parent.correlatedProcess.inputReceived();
			}

			this.channel = channel;
			this.message = message;
			this.notify();
			return true;
		}
		
		public boolean isKillable()
		{
			return true;
		}
	}
	
	final protected OneWayOperation operation;
	final protected VariablePath varPath;
	protected CorrelatedProcess correlatedProcess = null;

	public OneWayProcess(
			OneWayOperation operation,
			VariablePath varPath
			)
	{
		this.operation = operation;
		this.varPath = varPath;
	}

	public void checkMessageType( CommMessage message )
		throws TypeCheckingException
	{
		if ( operation.requestType() != null ) {
			operation.requestType().check( message.value() );
		}
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
		throws FaultException, ExitingException
	{
		if ( ExecutionThread.currentThread().isKilled() ) {
			return;
		}

		(new Execution( this )).run();
	}
	
	public InputHandler getInputHandler()
	{
		return operation;
	}

	private void log( String message )
	{
		if ( Interpreter.getInstance().verbose() ) {
			Interpreter.getInstance().logInfo( "[OneWay operation " + operation.id() + "]: " + message );
		}
	}
	
	public void runBehaviour( CommChannel channel, CommMessage message )
	{
		log( "received message " + message.id() );

		if ( varPath != null ) {
			varPath.getValue().refCopy( message.value() );
		}

		/*try {
			if ( channel != null ) {
				channel.release();
			}
		} catch( IOException e ) {
			Interpreter.getInstance().logSevere( e );
		}*/
	}
	
	public boolean isKillable()
	{
		return true;
	}
}
