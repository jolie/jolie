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

import java.util.Collection;
import java.util.HashMap;

import jolie.Constants;
import jolie.runtime.AbstractMappedGlobalObject;
import jolie.runtime.InvalidIdException;

abstract public class Port extends AbstractMappedGlobalObject
{
	private static HashMap< String, Port > idMap = new HashMap< String, Port >();
	private Constants.ProtocolId protocolId;
	
	public Port( String id, Constants.ProtocolId protocolId )
	{
		super( id );
		this.protocolId = protocolId;
	}
	
	public Constants.ProtocolId protocolId()
	{
		return protocolId;
	}

	public void register()
	{
		idMap.put( id(), this );
	}
	
	public static Port getById( String id )
		throws InvalidIdException
	{
		Port port = idMap.get( id );
		if ( port == null )
			throw new InvalidIdException( id );

		return port;
	}
	
	public static Collection< Port > getAll()
	{
		return idMap.values();
	}
}
