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


package jolie;

import java.util.Collection;
import java.util.HashMap;

/**
 * @author Fabrizio Montesi
 * @todo Refine the internal idMap methods.
 */
public class GlobalLocation extends Location implements MappedGlobalObject
{
	private static HashMap< String, GlobalLocation > idMap = 
		new HashMap< String, GlobalLocation >();

	private String id;
	private String value = "";
	
	public GlobalLocation( String id )
	{
		this.id = id;
	}
	
	public void setValue( String value )
	{
		this.value = value;
	}
	
	public String value()
	{
		return value;
	}
	
	public String id()
	{
		return id;
	}
	
	public static GlobalLocation getById( String id )
		throws InvalidIdException
	{
		GlobalLocation loc = idMap.get( id );
		if ( loc == null )
			throw new InvalidIdException( id );

		return loc;
	}
	
	public final void register()
	{
		idMap.put( id, this );
	}
	
	public static Collection< GlobalLocation > getAll()
	{
		return idMap.values();
	}
}
