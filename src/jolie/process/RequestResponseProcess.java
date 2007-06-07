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
import java.util.List;
import java.util.Vector;

import jolie.Constants;
import jolie.CorrelatedThread;
import jolie.Interpreter;
import jolie.net.CommChannel;
import jolie.net.CommCore;
import jolie.net.CommMessage;
import jolie.runtime.FaultException;
import jolie.runtime.GlobalVariable;
import jolie.runtime.InputHandler;
import jolie.runtime.InputOperation;
import jolie.runtime.Variable;

public class RequestResponseProcess implements InputOperationProcess, CorrelatedInputProcess
{
	private InputOperation operation;
	private Vector< GlobalVariable > varsVec;
	private Process process;
	private Vector< GlobalVariable > outVars;
	private CorrelatedProcess correlatedProcess = null;

	public RequestResponseProcess( InputOperation operation, Vector< GlobalVariable > varsVec, Vector< GlobalVariable > outVars, Process process )
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
		if ( CorrelatedThread.killed() )
			return;
		operation.getMessage( this );
		CorrelatedThread.currentThread().throwPendingFault();
	}
	
	public InputHandler inputHandler()
	{
		return operation;
	}
	
	public boolean recvMessage( CommMessage message )
	{
		if ( message.inputId().equals( operation.id() ) &&
				varsVec.size() == message.size() ) {
			int i = 0;
			Vector< Constants.VariableType > varTypes = operation.inVarTypes();
			for( Variable var : message ) { // Check their types first!
				if ( varTypes.elementAt( i ) != Constants.VariableType.VARIANT &&
							var.type() != varTypes.elementAt( i ) ) {
					Interpreter.logger().warning( "Rejecting wrong packet for operation " + 
							operation.id() + ". Wrong argument types received." );
					return false;
				}
				i++;
			}
			i = 0;
			if ( correlatedProcess != null )
				correlatedProcess.inputReceived();

			for( Variable var : message ) {
				/*if (	Interpreter.correlationSet().contains( varsVec.elementAt( i ) ) &&
						!varsVec.elementAt( i ).equals( var )
						)
					correlatedProcess.inputReceived();*/
				varsVec.elementAt( i++ ).assignValue( var );
			}
		} else {
			if ( correlatedProcess != null )
				correlatedProcess.inputReceived();
			Interpreter.logger().warning( "Rejecting wrong packet for operation " + operation.id() + ": wrong variables number" );
			return false;
		}
		
		CommMessage response = null;
		try {
			process.run();
			response = new CommMessage( operation.id(), outVars );
		} catch( FaultException f ) {
			CorrelatedThread.currentThread().setPendingFault( f );
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
