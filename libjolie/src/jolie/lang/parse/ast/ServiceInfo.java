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
import java.util.Collection;
import java.util.Map;

import jolie.Constants;
import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.ParsingContext;

public class ServiceInfo extends OLSyntaxNode
{
	final private String id;
	final private URI location;
	final private Collection< String > inputPorts;
	final private Constants.ProtocolId protocolId;
	final private OLSyntaxNode protocolConfiguration;
	final private Map< String, String > redirectionMap;
	
	public ServiceInfo(
			ParsingContext context,
			String id,
			URI location,
			Collection< String > inputPorts,
			Constants.ProtocolId protocolId,
			OLSyntaxNode protocolConfiguration,
			Map< String, String > redirectionMap
			)
	{
		super( context );
		this.id = id;
		this.location = location;
		this.inputPorts = inputPorts;
		this.protocolId = protocolId;
		this.protocolConfiguration = protocolConfiguration;
		this.redirectionMap = redirectionMap;
	}
	
	public Map< String, String > redirectionMap()
	{
		return redirectionMap;
	}
	
	public String id()
	{
		return id;
	}
	
	public OLSyntaxNode protocolConfiguration()
	{
		return protocolConfiguration;
	}
	
	public Constants.ProtocolId protocolId()
	{
		return protocolId;
	}
	
	public Collection< String > inputPorts()
	{
		return inputPorts;
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
