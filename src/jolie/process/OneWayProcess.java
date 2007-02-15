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

import jolie.InputHandler;
import jolie.InputOperation;
import jolie.Variable;
import jolie.net.CommMessage;

import org.w3c.dom.Node;

public class OneWayProcess implements InputProcess
{
	private InputOperation operation;
	private Vector< Variable > varsVec;

	public OneWayProcess( InputOperation operation, Vector< Variable > varsVec )
	{
		this.operation = operation;
		this.varsVec = varsVec;
	}
	
	public InputHandler inputHandler()
	{
		return operation;
	}
	
	public void run()
	{
		operation.getMessage( this );
	}
	
	public synchronized boolean recvMessage( CommMessage message )
	{
		if ( message.inputId().equals( operation.id() ) &&
				varsVec.size() == message.size() ) {
			int i = 0;
			Vector< Variable.Type > varTypes = operation.inVarTypes();
			for( Variable var : message ) { // Check their types first!
				if ( varTypes.elementAt( i ) != Variable.Type.VARIANT &&
							var.type() != varTypes.elementAt( i ) ) {
					System.out.println( "Warning: rejecting wrong packet for operation " + 
							operation.id() + ". Wrong argument types received." );
					return false;
				}
				i++;
			}
			i = 0;
			for( Variable var : message )
				varsVec.elementAt( i++ ).assignValue( var );
		} else
			return false;

		return true;
	}
	
	public void translateToBPEL( Node parentNode )
	{
		
	}
}