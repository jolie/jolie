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

package jolie.deploy;

import java.util.Vector;

import jolie.Constants;
import jolie.runtime.InputOperation;
import jolie.runtime.InvalidIdException;

public class InputPortType extends PortType
{
	private Vector< InputOperation > operations;
	
	public InputPortType( String id )
	{
		super( id );
		this.operations = new Vector< InputOperation >();
	}
	
	public Port createPort( String portId, Constants.ProtocolId protocolId )
		throws PortCreationException
	{
		return new InputPort( portId, this, protocolId );
	}
	
	public Vector< InputOperation > operations()
	{
		return operations;
	}
	
	/**
	 * Adds an InputOperation to this InputPortType, and sets
	 * the PortType of the InputOperation to this InputPortType by calling
	 * its OperationWSDLInfo setPortType method.
	 * @param operation The InputOperation to add to this InputPortType 
	 */
	public void addOperation( InputOperation operation )
	{
		operations.add( operation );
		operation.deployInfo().setPortType( this );
	}
	
	public static InputPortType getById( String id )
		throws InvalidIdException
	{
		PortType pt = PortType.getById( id );
		if ( !( pt instanceof InputPortType ) )
			throw new InvalidIdException( id );
		return (InputPortType)pt;
	}
}
