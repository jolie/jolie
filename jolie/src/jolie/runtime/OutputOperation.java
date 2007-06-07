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

package jolie.runtime;

import java.net.URISyntaxException;
import java.util.Vector;

import jolie.Constants;
import jolie.Interpreter;
import jolie.deploy.OutputPort;
import jolie.deploy.OutputPortType;
import jolie.deploy.PortType;
import jolie.net.CommProtocol;
import jolie.net.SOAPProtocol;
import jolie.net.SODEPProtocol;

public class OutputOperation extends Operation
{
	private String boundOperationId;
	private Vector< Constants.VariableType > outVarTypes;

	public OutputOperation( String id,
			String boundOperationId,
			Vector< Constants.VariableType > outVarTypes
			)
	{
		super( id );
		this.boundOperationId = boundOperationId;
		this.outVarTypes = outVarTypes;
	}
	
	public Vector< Constants.VariableType > outVarTypes()
	{
		return outVarTypes;
	}
	
	public String boundOperationId()
	{
		return boundOperationId;
	}
	
	public static OutputOperation getById( String id )
		throws InvalidIdException
	{
		Operation obj = Operation.getById( id );
		if ( !( obj instanceof OutputOperation ) )
			throw new InvalidIdException( id );
		return (OutputOperation)obj;
	}
	
	public CommProtocol getOutputProtocol( Location location )
		throws URISyntaxException
	{
		PortType pt = wsdlInfo().portType();
		if ( pt != null ) {
			assert( pt instanceof OutputPortType );
			OutputPort port = ((OutputPortType)pt).outputPort();
			if ( port != null ) {
				Constants.ProtocolId pId = port.protocolId();
				if ( pId == Constants.ProtocolId.SODEP )
					return new SODEPProtocol();
				else if ( pId == Constants.ProtocolId.SOAP )
					return new SOAPProtocol( location.getURI(), wsdlInfo() );
			} else
				Interpreter.logger().warning( "Unspecified output port for operation " + id() );
		}
		return new SODEPProtocol();
	}
}
