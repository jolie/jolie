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
import java.util.HashMap;

import jolie.Constants;
import jolie.Interpreter;
import jolie.deploy.OutputPort;
import jolie.deploy.OutputPortType;
import jolie.deploy.PortType;
import jolie.net.CommProtocol;
import jolie.net.HTTPProtocol;
import jolie.net.SOAPProtocol;
import jolie.net.SODEPProtocol;

public class OutputOperation extends Operation
{
	private static HashMap< String, OutputOperation > idMap = 
		new HashMap< String, OutputOperation >();

	public OutputOperation( String id )
	{
		super( id );
	}

	public static OutputOperation getById( String id )
		throws InvalidIdException
	{
		OutputOperation retVal = idMap.get( id );
		if ( retVal == null )
			throw new InvalidIdException( id + " (undefined output operation)" );

		return retVal;
	}
	
	public void register()
	{
		idMap.put( id(), this );
	}
	
	public CommProtocol getOutputProtocol( Location location )
		throws URISyntaxException
	{
		PortType pt = deployInfo().portType();
		if ( pt != null ) {
			assert( pt instanceof OutputPortType );
			OutputPort port = ((OutputPortType)pt).outputPort();
			if ( port != null ) {
				Constants.ProtocolId pId = port.protocolId();
				if ( pId == Constants.ProtocolId.SODEP ) {
					return new SODEPProtocol();
				} else if ( pId == Constants.ProtocolId.SOAP ) {
					return new SOAPProtocol(
							location.getURI(),
							((OutputPortType)deployInfo().portType()).namespace()
							);
				} else if ( pId == Constants.ProtocolId.HTTP ) {
					return new HTTPProtocol( location.getURI() );
				}
			} else
				Interpreter.logger().warning( "Unspecified output port for operation " + id() );
		}
		return new SODEPProtocol();
	}
}
