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

import java.util.Vector;
import java.io.IOException;

import jolie.InputHandler;
import jolie.InputOperation;
import jolie.Variable;
import jolie.net.CommChannel;
import jolie.net.CommCore;
import jolie.net.CommMessage;

public class RequestResponseProcess implements InputProcess, Optimizable
{
	private InputOperation operation;
	private Vector< Variable > varsVec;
	private Process process;
	private Vector< Variable > outVars;

	public RequestResponseProcess( InputOperation operation, Vector< Variable > varsVec, Vector< Variable > outVars, Process process )
	{
		this.operation = operation;
		this.varsVec = varsVec;
		this.process = process;
		this.outVars = outVars;
	}
	
	public void run()
	{
		operation.getMessage( this );
	}
	
	public InputHandler inputHandler()
	{
		return operation;
	}
	
	public synchronized boolean recvMessage( CommMessage message )
	{
		if ( message.inputId().equals( operation.id() ) &&
				varsVec.size() == message.size() ) {
			int i = 0;
			Vector< Variable.Type > varTypes = operation.inVarTypes();
			for( Variable var : message ) { // Check their types first!
				if ( var.type() != varTypes.elementAt( i ) ) {
					System.out.println( "Warning: rejecting wrong packet for operation " + 
							operation.id() + ". Wrong argument types received." );
					return false;
				}
			}
			i = 0;
			for( Variable var : message )
				varsVec.elementAt( i++ ).assignValue( var );
		} else
			return false;
		

		process.run();
		
		CommMessage response = new CommMessage( operation.id(), outVars );

		CommChannel channel = CommCore.currentCommChannel();
		if ( channel != null ) {
			try {
				channel.send( response );
			} catch( IOException ioe ) {
				ioe.printStackTrace();
			}
		} // todo -- else throw exception?
		
		return true;
	}
	
	public Process optimize()
	{
		if ( process instanceof Optimizable )
			((Optimizable)process).optimize();

		return this;
	}
}
