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
import java.util.List;
import java.util.Vector;

import jolie.ExecutionThread;
import jolie.Interpreter;
import jolie.net.CommChannel;
import jolie.net.CommCore;
import jolie.net.CommMessage;
import jolie.runtime.FaultException;
import jolie.runtime.GlobalVariable;
import jolie.runtime.InputHandler;
import jolie.runtime.RequestResponseOperation;
import jolie.runtime.Value;
import jolie.runtime.Variable;

public class RequestResponseProcess implements InputOperationProcess, CorrelatedInputProcess
{
	private RequestResponseOperation operation;
	private Vector< GlobalVariable > varsVec;
	private Process process;
	private Vector< GlobalVariable > outVars;
	private CorrelatedProcess correlatedProcess = null;

	public static class Fields {
		private FaultException pendingFault = null;
	}

	
	public RequestResponseProcess( RequestResponseOperation operation, Vector< GlobalVariable > varsVec, Vector< GlobalVariable > outVars, Process process )
	{
		this.operation = operation;
		this.varsVec = varsVec;
		this.process = process;
		this.outVars = outVars;
	}
	
	public void setCorrelatedProcess( CorrelatedProcess process )
	{
		this.correlatedProcess = process;
	}
	
	public List< GlobalVariable > inputVars()
	{
		return varsVec;
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
		if ( message.inputId().equals( operation.id() ) &&
				varsVec.size() == message.size() ) {
			int i = 0;
			for( Value recvVal : message )
				varsVec.elementAt( i++ ).value().deepCopy( recvVal );
		} else {
			Interpreter.logger().warning( "Rejecting malformed packet for operation " + operation.id() + ": wrong variables number" );
			return false;
		}
		
		CommMessage response = null;
		try {
			process.run();
			Vector< Value > valsVec = new Vector< Value >();
			for( Variable var : outVars ) 
				valsVec.add( var.value() );

			response = new CommMessage( operation.id(), valsVec );
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
