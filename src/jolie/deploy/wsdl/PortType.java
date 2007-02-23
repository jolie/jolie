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


package jolie.deploy.wsdl;

import java.util.HashMap;

import jolie.AbstractMappedGlobalObject;
import jolie.InvalidIdException;
import jolie.net.CommProtocol;

abstract public class PortType extends AbstractMappedGlobalObject
{
	private static HashMap< String, PortType > idMap = new HashMap< String, PortType >();
	private PartnerLinkType partnerLinkType;
	
	public PortType( String id )
	{
		super( id );
		partnerLinkType = null;
	}
	
	public void register()
	{
		idMap.put( id(), this );
	}
	
	abstract public Port createPort( String portId, CommProtocol.Identifier protocolId )
		throws PortCreationException;
	
	public static PortType getById( String id )
		throws InvalidIdException
	{
		PortType pt = idMap.get( id );
		if ( pt == null )
			throw new InvalidIdException( id );

		return pt;
	}
	
	public PartnerLinkType partnerLinkType()
	{
		return partnerLinkType;
	}
	
	public void setPartnerLinkType( PartnerLinkType plk )
	{
		partnerLinkType = plk;
	}
}
