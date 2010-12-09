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

package jolie.lang.parse.ast;

import java.net.URI;
import java.util.Map;

import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.context.ParsingContext;

public class InputPortInfo extends PortInfo
{
	private final URI location;
	private final String protocolId;
	private final OLSyntaxNode protocolConfiguration;
	private final String[] aggregationList;
	private final Map< String, String > redirectionMap;

	public InputPortInfo(
		ParsingContext context,
		String id,
		URI location,
		String protocolId,
		OLSyntaxNode protocolConfiguration,
		String[] aggregationList,
		Map< String, String > redirectionMap
	) {
		super( context, id );
		this.location = location;
		this.protocolId = protocolId;
		this.protocolConfiguration = protocolConfiguration;
		this.aggregationList = aggregationList;
		this.redirectionMap = redirectionMap;
	}

	public String[] aggregationList()
	{
		return aggregationList;
	}

	public Map< String, String > redirectionMap()
	{
		return redirectionMap;
	}

	public OLSyntaxNode protocolConfiguration()
	{
		return protocolConfiguration;
	}

	public String protocolId()
	{
		return protocolId;
	}

	public URI location()
	{
		return location;
	}

	public void accept( OLVisitor visitor )
	{
		visitor.visit( this );
	}
}
