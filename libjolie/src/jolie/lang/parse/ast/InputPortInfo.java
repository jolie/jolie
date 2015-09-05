/***************************************************************************
 *   Copyright (C) 2007-2011 by Fabrizio Montesi <famontesi@gmail.com>     *
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

import java.io.Serializable;
import java.net.URI;
import java.util.Map;

import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.context.ParsingContext;

public class InputPortInfo extends PortInfo
{
	public static class AggregationItemInfo implements Serializable {
		private final String[] outputPortList;
		private final InterfaceExtenderDefinition interfaceExtender;
		
		public AggregationItemInfo( String[] outputPortList, InterfaceExtenderDefinition extender )
		{
			this.outputPortList = outputPortList;
			this.interfaceExtender = extender;
		}
		
		public String[] outputPortList()
		{
			 return outputPortList;
		}
		
		public InterfaceExtenderDefinition interfaceExtender()
		{
			return interfaceExtender;
		}
	}
	
	private final URI location;
	private final String protocolId;
	private final OLSyntaxNode protocolConfiguration;
	private final AggregationItemInfo[] aggregationList;
	private final Map< String, String > redirectionMap;

	public InputPortInfo(
		ParsingContext context,
		String id,
		URI location,
		String protocolId,
		OLSyntaxNode protocolConfiguration,
		AggregationItemInfo[] aggregationList,
		Map< String, String > redirectionMap
	) {
		super( context, id );
		this.location = location;
		this.protocolId = protocolId;
		this.protocolConfiguration = protocolConfiguration;
		this.aggregationList = aggregationList;
		this.redirectionMap = redirectionMap;
	}

	public AggregationItemInfo[] aggregationList()
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

	@Override
	public void accept( OLVisitor visitor )
	{
		visitor.visit( this );
	}
}
