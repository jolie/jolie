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
import java.net.URISyntaxException;
import java.util.Vector;

import jolie.Location;
import jolie.SolicitResponseOperation;
import jolie.Variable;
import jolie.net.CommChannel;
import jolie.net.CommMessage;

import org.w3c.dom.Node;

public class SolicitResponseProcess implements Process
{
	private SolicitResponseOperation operation;
	private Vector< Variable > outVars, inVars;
	private Location location;

	public SolicitResponseProcess( SolicitResponseOperation operation, Location location, Vector< Variable > outVars, Vector< Variable > inVars )
	{
		this.operation = operation;
		this.location = location;
		this.outVars = outVars;
		this.inVars = inVars;
	}
	
	public void run()
	{
		Variable.castAll( outVars, operation.outVarTypes() );
		try {
			/*if ( wsdlInfo.outVarNames() == null ) {
				wsdlInfo = operation.wsdlInfo().clone();
				wsdlInfo.setOutVarNames( Variable.getNames( outVars ) );
			}
			
			if ( wsdlInfo.boundName() == null )
				wsdlInfo.setBoundName( operation.boundOperationId() );*/
			String boundName = operation.wsdlInfo().boundName();
			if ( boundName == null )
				boundName = operation.boundOperationId();
			
			CommChannel channel = new CommChannel( location, operation.getProtocol( location ) );
			CommMessage message = new CommMessage( boundName, outVars );
			channel.send( message );
			message = channel.recv();
			
			if ( message.size() == inVars.size() ) {
				int i = 0;
				boolean correctTypes = true;

				Vector< Variable.Type > varTypes = operation.inVarTypes();
				for( Variable var : message ) { // Check their types first!
					if ( varTypes.elementAt( i ) != Variable.Type.VARIANT &&
							var.type() != varTypes.elementAt( i ) ) {
						System.out.println( "Warning: rejecting wrong packet for operation " + 
							operation.id() + ". Wrong argument types received." );
						correctTypes = false;
					}
					i++;
				}
				i = 0;
				if ( correctTypes ) {
					for( Variable recvVar : message )
						inVars.elementAt( i++ ).assignValue( recvVar );
				}
			} // todo -- if else throw exception?
			
			channel.close();
		} catch( IOException ioe ) {
			ioe.printStackTrace();
		} catch( URISyntaxException ue ) {
			ue.printStackTrace();
		}
	}
	
	public void translateToBPEL( Node parentNode )
	{
		
	}
}