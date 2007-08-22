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

import jolie.net.CommChannel;
import jolie.net.CommMessage;
import jolie.runtime.FaultException;
import jolie.runtime.GlobalVariable;
import jolie.runtime.Location;
import jolie.runtime.SolicitResponseOperation;
import jolie.runtime.Value;
import jolie.runtime.Variable;

public class SolicitResponseProcess implements Process
{
	private SolicitResponseOperation operation;
	private Vector< GlobalVariable > outVars, inVars;
	private Location location;

	public SolicitResponseProcess( SolicitResponseOperation operation, Location location, Vector< GlobalVariable > outVars, Vector< GlobalVariable > inVars )
	{
		this.operation = operation;
		this.location = location;
		this.outVars = outVars;
		this.inVars = inVars;
	}
	
	public void run()
		throws FaultException
	{
		CommChannel channel = null;
		//Variable.castAll( outVars, operation.outVarTypes() );
		try {
			/*if ( wsdlInfo.outVarNames() == null ) {
				wsdlInfo = operation.wsdlInfo().clone();
				wsdlInfo.setOutVarNames( Variable.getNames( outVars ) );
			}
			
			if ( wsdlInfo.boundName() == null )
				wsdlInfo.setBoundName( operation.boundOperationId() );*/
			String boundName = operation.deployInfo().boundName();
			if ( boundName == null )
				boundName = operation.boundOperationId();
			
			channel = new CommChannel( location, operation.getOutputProtocol( location ) );
			Vector< Value > valsVec = new Vector< Value >();
			for( Variable var : outVars )
				valsVec.add( var.value() );
			CommMessage message = new CommMessage( boundName, valsVec );
			channel.send( message );
			message = channel.recv();
			if ( message.isFault() )
				throw new FaultException( message.faultName() );
			
			if ( message.size() == inVars.size() ) {
				int i = 0;
				//boolean correctTypes = true;

				/*Vector< Constants.VariableType > varTypes = operation.inVarTypes();
				for( Value val : message ) { // Check their types first!
					if ( varTypes.elementAt( i ) != Constants.VariableType.VARIANT &&
							val.type() != varTypes.elementAt( i ) ) {
						Interpreter.logger().warning( "Rejecting wrong packet for operation " + 
							operation.id() + ". Wrong argument types received." );
						correctTypes = false;
					}
					i++;
				}
				i = 0;*/
				//if ( correctTypes ) {
					for( Value recvVal : message )
						inVars.elementAt( i++ ).value().assignValue( recvVal );
				//}
			} // todo -- if else throw exception?
		} catch( IOException ioe ) {
			ioe.printStackTrace();
		} catch( URISyntaxException ue ) {
			ue.printStackTrace();
		} finally {
			if ( channel != null ) {
				try {
					channel.close();
				} catch( IOException ioe ) {
					ioe.printStackTrace();
				}
			}
		}
	}
}