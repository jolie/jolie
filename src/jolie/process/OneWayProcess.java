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

import java.util.List;
import java.util.Vector;

import jolie.Constants;
import jolie.ExecutionThread;
import jolie.Interpreter;
import jolie.net.CommMessage;
import jolie.runtime.GlobalVariable;
import jolie.runtime.InputHandler;
import jolie.runtime.InputOperation;
import jolie.runtime.Variable;

public class OneWayProcess implements InputOperationProcess, CorrelatedInputProcess
{
	private InputOperation operation;
	private Vector< GlobalVariable > varsVec;
	private CorrelatedProcess correlatedProcess = null;

	public OneWayProcess( InputOperation operation, Vector< GlobalVariable > varsVec )
	{
		this.operation = operation;
		this.varsVec = varsVec;
	}
	
	public void setCorrelatedProcess( CorrelatedProcess process )
	{
		this.correlatedProcess = process;
	}
	
	public InputHandler inputHandler()
	{
		return operation;
	}
	
	public void run()
	{
		if ( ExecutionThread.killed() )
			return;
		operation.getMessage( this );
	}
	
	public List< GlobalVariable > inputVars()
	{
		return varsVec;
	}
	
	/**
	 * @todo transform the type check in a shared procedure
	 */
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
			if ( correlatedProcess != null )
				correlatedProcess.inputReceived();
			i = 0;
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

		return true;
	}
}