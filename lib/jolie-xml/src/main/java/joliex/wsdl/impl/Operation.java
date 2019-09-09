/***************************************************************************
 *   Copyright (C) 2010 by Fabrizio Montesi                                *
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

package joliex.wsdl.impl;

import java.util.List;
import jolie.util.Pair;

/**
 *
 * @author Fabrizio Montesi
 */
public class Operation
{
	private final String name;
	private final String comment;
	private final List< Pair< String, String > > faults;
	private final String requestTypeName;
	private final String responseTypeName;

	public Operation( String name, String requestTypeName, String responseTypeName, List< Pair< String, String > > faults, String comment )
	{
		this.name = name;
		this.faults = faults;
		this.comment = comment;
		this.requestTypeName = requestTypeName;
		this.responseTypeName = responseTypeName;
	}

	public String requestTypeName()
	{
		return requestTypeName;
	}

	public String responseTypeName()
	{
		return responseTypeName;
	}

	public List< Pair< String, String > > faults()
	{
		return faults;
	}

	public String name()
	{
		return name;
	}

	public String comment()
	{
		return comment;
	}
}
