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
import java.util.Iterator;

import jolie.ExecutionThread;
import jolie.Interpreter;
import jolie.net.CommChannel;
import jolie.net.CommMessage;
import jolie.runtime.Expression;
import jolie.runtime.FaultException;
import jolie.runtime.InputHandler;
import jolie.runtime.RequestResponseOperation;
import jolie.runtime.Value;
import jolie.runtime.VariablePath;

public class RequestResponseProcess implements CorrelatedInputProcess, InputOperationProcess
{
	private class Execution implements InputProcessExecution
	{
		private CommMessage message;
		private CommChannel channel;
		private RequestResponseProcess parent;
		
		public Execution( RequestResponseProcess parent )
		{
			this.parent = parent;
		}
		
		public Process clone( TransformationReason reason )
		{
			return new Execution( parent );
		}
		
		public Process parent()
		{
			return parent;
		}
		
		public void run()
			throws FaultException
		{
			try {
				parent.operation.signForMessage( this );
				synchronized( this ) {
					if( message == null ) {
						ExecutionThread ethread = ExecutionThread.currentThread();
						ethread.setCanBeInterrupted( true );
						this.wait();
						ethread.setCanBeInterrupted( false );
					}
				}
				parent.runBehaviour( channel, message );
			} catch( InterruptedException ie ) {
				parent.operation.cancelWaiting( this );
			}
		}
		
		public VariablePath inputVarPath()
		{
			return parent.inputVarPath;
		}
		
		public synchronized boolean recvMessage( CommChannel channel, CommMessage message )
		{
			if ( parent.correlatedProcess != null )
				parent.correlatedProcess.inputReceived();
			
			this.channel = channel;
			this.message = message;
			this.notify();

			return true;
		}	
	}

	
	protected RequestResponseOperation operation;
	protected VariablePath inputVarPath;
	protected Expression outputExpression;
	protected Process process;
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
	
	public Process clone( TransformationReason reason )
	{
		return new RequestResponseProcess(
					operation,
					(VariablePath)inputVarPath.cloneExpression( reason ),
					(VariablePath)outputExpression.cloneExpression( reason ),
					process.clone( reason )
				);
	}
	
	public void setCorrelatedProcess( CorrelatedProcess process )
	{
		this.correlatedProcess = process;
	}
	
	public void run()
		throws FaultException
	{
		if ( ExecutionThread.currentThread().isKilled() )
			return;
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
	
	private CommMessage createFaultMessage( FaultException f )
	{
		if ( !operation.faultNames().contains( f.faultName() ) ) {
			Interpreter.getInstance().logger().severe(
				"Request-Response process for " + operation.id() +
				" threw an undeclared fault for that operation" );
			Iterator< String > it = operation.faultNames().iterator();
			if ( it.hasNext() ) {
				String newFault = it.next();
				Interpreter.getInstance().logger().warning(
					"Converting Request-Response fault " + f.faultName() +
					" to " + newFault );
				f = new FaultException( newFault );
			} else
				Interpreter.getInstance().logger().severe( "Could not find a fault to convert the undeclared fault to." );
		}
		//TODO support resourcePath
		return new CommMessage( operation.id(), "/", f );
	}
	
	public void runBehaviour( CommChannel channel, CommMessage message )
		throws FaultException
	{
		if ( inputVarPath != null ) {
			Value val = inputVarPath.getValue();
			val.erase();
			val.deepCopy( message.value() );
		}
		
		FaultException fault = null;
		
		CommMessage response = null;
		try {
			process.run();
			ExecutionThread ethread = ExecutionThread.currentThread();
			if ( ethread.isKilled() ) {
				response = createFaultMessage( ethread.killerFault() );
			} else {
				//TODO support resourcePath
				response =
				( outputExpression == null ) ?
						new CommMessage( operation.id(), "/" ) :
						new CommMessage( operation.id(), "/", outputExpression.evaluate() );
			}
		} catch( FaultException f ) {
			response = createFaultMessage( f );
			fault = f;
		}

		try {
			channel.send( response );
			channel.disposeForInput();
		} catch( IOException ioe ) {
			ioe.printStackTrace();
		}
		
		if ( fault != null )
			throw fault;
	}
}
