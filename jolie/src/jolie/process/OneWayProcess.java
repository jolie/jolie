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
import jolie.net.CommMessage;
import jolie.runtime.GlobalVariablePath;
import jolie.runtime.InputHandler;
import jolie.runtime.InputOperation;

public class OneWayProcess implements InputOperationProcess, CorrelatedInputProcess
{
	private InputOperation operation;
	private GlobalVariablePath varPath;
	private CorrelatedProcess correlatedProcess = null;

	public OneWayProcess( InputOperation operation, GlobalVariablePath varPath )
	{
		this.operation = operation;
		this.varPath = varPath;
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
	
	public GlobalVariablePath inputVarPath()
	{
		return varPath;
	}
	
	public boolean recvMessage( CommMessage message )
	{
		if ( correlatedProcess != null )
			correlatedProcess.inputReceived();
		if ( message.inputId().equals( operation.id() ) ) {
			varPath.getValue().deepCopy( message.value() );
		} else {
			Interpreter.logger().warning( "Rejecting malformed packet for operation " + operation.id() + ": wrong variables number" );
			return false;
		}

		return true;
	}
}