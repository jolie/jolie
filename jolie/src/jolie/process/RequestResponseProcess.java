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
import jolie.lang.Constants;
import jolie.net.CommChannel;
import jolie.net.CommMessage;
import jolie.runtime.ExitingException;
import jolie.runtime.Expression;
import jolie.runtime.FaultException;
import jolie.runtime.InputHandler;
import jolie.runtime.RequestResponseOperation;
import jolie.runtime.Value;
import jolie.runtime.VariablePath;
import jolie.runtime.typing.Type;
import jolie.runtime.typing.TypeCheckingException;

public class RequestResponseProcess implements CorrelatedInputProcess, InputOperationProcess
{
	private class Execution  extends AbstractInputProcessExecution< RequestResponseProcess >
	{
		private CommMessage message;
		private CommChannel channel;
		
		public Execution( RequestResponseProcess parent )
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
				parent.operation.signForMessage( this );
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
			} catch( InterruptedException e ) {
				parent.operation.cancelWaiting( this );
			}
		}
		
		public boolean isKillable()
		{
			return true;
		}
		
		public VariablePath inputVarPath()
		{
			return parent.inputVarPath;
		}
		
		// TODO: is synchronized really needed here?
		public synchronized boolean recvMessage( CommChannel channel, CommMessage message )
			throws TypeCheckingException
		{
			try {
				checkMessageType( message );
			} catch( TypeCheckingException e ) {
				Interpreter.getInstance().logWarning( "Received message TypeMismatch (Request-Response input operation " + operation.id() + "): " + e.getMessage() );
				try {
					channel.send( CommMessage.createFaultResponse( message, new FaultException( Constants.TYPE_MISMATCH_FAULT_NAME, e.getMessage() ) ) );
					//channel.release();
				} catch( IOException ioe ) {
					Interpreter.getInstance().logSevere( ioe );
				}
				throw e;
			}
			
			if ( parent.correlatedProcess != null ) {
				if ( Interpreter.getInstance().exiting() ) {
					this.notify();
					// Do not trigger session spawning if we're exiting
					return false;
				} else {
					parent.correlatedProcess.inputReceived();
				}
			}

			this.channel = channel;
			this.message = message;
			this.notify();

			return true;
		}	
	}

	
	final protected RequestResponseOperation operation;
	final protected VariablePath inputVarPath; // may be null
	final protected Expression outputExpression; // may be null
	final protected Process process;
	protected CorrelatedProcess correlatedProcess = null;
	
	public RequestResponseProcess(
			RequestResponseOperation operation,
			VariablePath inputVarPath,
			Expression outputExpression,
			Process process )
	{
		this.operation = operation;
		this.inputVarPath = inputVarPath;
		this.process = process;
		this.outputExpression = outputExpression;
	}

	private void log( String message )
	{
		if ( Interpreter.getInstance().verbose() ) {
			Interpreter.getInstance().logInfo( "[RequestResponse operation " + operation.id() + "]: " + message );
		}
	}
	
	public boolean isKillable()
	{
		return true;
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
		return new RequestResponseProcess(
					operation,
					( inputVarPath == null ) ? null : (VariablePath)inputVarPath.cloneExpression( reason ),
					( outputExpression == null ) ? null : (VariablePath)outputExpression.cloneExpression( reason ),
					process.clone( reason )
				);
	}
	
	public void setCorrelatedProcess( CorrelatedProcess process )
	{
		this.correlatedProcess = process;
	}
	
	public void run()
		throws FaultException, ExitingException
	{
		if ( ExecutionThread.currentThread().isKilled() ) {
			return;
		}

		(new Execution( this )).run();
	}
	
	public VariablePath inputVarPath()
	{
		return inputVarPath;
	}
	
	public InputHandler getInputHandler()
	{
		return operation;
	}
	
	private CommMessage createFaultMessage( CommMessage request, FaultException f )
		throws TypeCheckingException
	{
		if ( operation.faults().containsKey( f.faultName() ) ) {
			Type faultType = operation.faults().get( f.faultName() );
			if ( faultType != null ) {
				faultType.check( f.value() );
			}
		} else {
			Interpreter.getInstance().logSevere(
				"Request-Response process for " + operation.id() +
				" threw an undeclared fault for that operation (" + f.faultName() + "), throwing TypeMismatch" );
			f = new FaultException( Constants.TYPE_MISMATCH_FAULT_NAME, "Internal server error" );
		}
		return CommMessage.createFaultResponse( request, f );
	}
	
	public void runBehaviour( CommChannel channel, CommMessage message )
		throws FaultException
	{
		log( "received message " + message.id() );

		if ( inputVarPath != null ) {
			inputVarPath.setValue( message.value() );
		}

		FaultException typeMismatch = null;

		FaultException fault = null;
		CommMessage response = null;
		try {
			try {
				process.run();
			} catch( ExitingException e ) {}
			ExecutionThread ethread = ExecutionThread.currentThread();
			if ( ethread.isKilled() ) {
				try {
					response = createFaultMessage( message, ethread.killerFault() );
				} catch( TypeCheckingException e ) {
					typeMismatch = new FaultException( Constants.TYPE_MISMATCH_FAULT_NAME, "Request-Response process TypeMismatch for fault " + ethread.killerFault().faultName() + " (operation " + operation.id() + "): " + e.getMessage() );
					response = CommMessage.createFaultResponse( message, typeMismatch );
				}
			} else {
				response =
					CommMessage.createResponse(
						message,
						( outputExpression == null ) ? Value.UNDEFINED_VALUE : outputExpression.evaluate()
					);
				if ( operation.responseType() != null ) {
					try {
						operation.responseType().check( response.value() );
					} catch( TypeCheckingException e ) {
						typeMismatch = new FaultException( Constants.TYPE_MISMATCH_FAULT_NAME, "Request-Response input operation output value TypeMismatch (operation " + operation.id() + "): " + e.getMessage() );
						response = CommMessage.createFaultResponse( message, new FaultException( Constants.TYPE_MISMATCH_FAULT_NAME, "Internal server error (TypeMismatch)" ) );
					}
				}
			}
		} catch( FaultException f ) {
			try {
				response = createFaultMessage( message, f );
			} catch( TypeCheckingException e ) {
				typeMismatch = new FaultException( Constants.TYPE_MISMATCH_FAULT_NAME, "Request-Response process TypeMismatch for fault " + f.faultName() + " (operation " + operation.id() + "): " + e.getMessage() );
				response = CommMessage.createFaultResponse( message, typeMismatch );
			}
			fault = f;
		}

		try {
			channel.send( response );
			log( "sent response for message " + message.id() );
		} catch( IOException e ) {
			//Interpreter.getInstance().logSevere( e );
			throw new FaultException( Constants.IO_EXCEPTION_FAULT_NAME, e );
		} finally {
			try {
				channel.release(); // TODO: what if the channel is in disposeForInput?
			} catch( IOException e ) {
				Interpreter.getInstance().logSevere( e );
			}
		}

		if ( fault != null ) {
			if ( typeMismatch != null ) {
				Interpreter.getInstance().logWarning( typeMismatch.value().strValue() );
			}
			throw fault;
		} else if ( typeMismatch != null ) {
			throw typeMismatch;
		}
	}
}
