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
import jolie.net.CommCore;
import jolie.net.CommMessage;
import jolie.runtime.FaultException;
import jolie.runtime.GlobalVariablePath;
import jolie.runtime.InputHandler;
import jolie.runtime.RequestResponseOperation;

public class RequestResponseProcess implements InputOperationProcess, CorrelatedInputProcess
{
	private RequestResponseOperation operation;
	private GlobalVariablePath inputVarPath, outputVarPath;
	private Process process;
	private CorrelatedProcess correlatedProcess = null;

	public static class Fields {
		private FaultException pendingFault = null;
	}
	
	public RequestResponseProcess(
			RequestResponseOperation operation,
			GlobalVariablePath inputVarPath,
			GlobalVariablePath outputVarPath,
			Process process )
	{
		this.operation = operation;
		this.inputVarPath = inputVarPath;
		this.process = process;
		this.outputVarPath = outputVarPath;
	}
	
	public void setCorrelatedProcess( CorrelatedProcess process )
	{
		this.correlatedProcess = process;
	}
	
	public GlobalVariablePath inputVarPath()
	{
		return inputVarPath;
	}
	
	public void run()
		throws FaultException
	{
		if ( ExecutionThread.killed() )
			return;
		operation.getMessage( this );

		Fields fields = ExecutionThread.getLocalObject( this, Fields.class );
		if ( fields.pendingFault != null )
			throw fields.pendingFault;
	}
	
	public InputHandler inputHandler()
	{
		return operation;
	}
	
	public boolean recvMessage( CommMessage message )
	{
		if ( correlatedProcess != null )
			correlatedProcess.inputReceived();
		
		// @todo -- 
		// Do we still need this at this point? The CommCore should have handled this.
		if ( message.inputId().equals( operation.id() ) ) {
			if ( inputVarPath != null )
				inputVarPath.getValue().deepCopy( message.value() );
		} else {
			Interpreter.logger().warning( "Rejecting malformed packet for operation " + operation.id() + ": wrong variables number" );
			return false;
		}
		
		CommMessage response = null;
		try {
			process.run();
			response =
				( outputVarPath == null ) ?
						new CommMessage( operation.id() ) :
						new CommMessage( operation.id(), outputVarPath.getValue() );
		} catch( FaultException f ) {
			Fields fields = ExecutionThread.getLocalObject( this, Fields.class );
			fields.pendingFault = f;
			if ( !operation.faultNames().contains( f.fault() ) ) {
				Interpreter.logger().severe(
					"Request-Response process for " + operation.id() +
					"threw an undeclared fault for that operation" );
				Iterator< String > it = operation.faultNames().iterator();
				if ( it.hasNext() ) {
					String newFault = it.next();
					Interpreter.logger().warning(
						"Converting Request-Response fault " + f.fault() +
						" to " + newFault );
					f = new FaultException( newFault );
				} else
					Interpreter.logger().severe( "Could not find a fault to convert the undeclared fault to." );
			}
			response = new CommMessage( operation.id(), f );
		}

		CommChannel channel = CommCore.currentCommChannel();
		if ( channel != null ) {
			try {
				channel.send( response );
				channel.close();
			} catch( IOException ioe ) {
				ioe.printStackTrace();
			}
		} // todo -- else throw exception?

		return true;
	}
}
