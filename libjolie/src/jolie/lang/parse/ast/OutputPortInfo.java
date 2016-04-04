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

import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.context.ParsingContext;

public class OutputPortInfo extends PortInfo
{	
	private String protocolId = null;
	private OLSyntaxNode protocolConfiguration = null;
	private URI location = null;
	
	public OutputPortInfo( ParsingContext context, String id )
	{
		super( context, id );
	}
	
	public void setProtocolId( String protocolId )
	{
		this.protocolId = protocolId;
	}
	
	public void setProtocolConfiguration( OLSyntaxNode protocolConfiguration )
	{
		this.protocolConfiguration = protocolConfiguration;
	}
	
	public void setLocation( URI location )
	{
		this.location = location;
	}
	
	@Override
	public void accept( OLVisitor visitor )
	{
		visitor.visit( this );
	}
	
	public String protocolId()
	{
		return protocolId;
	}
	
	public OLSyntaxNode protocolConfiguration()
	{
		return protocolConfiguration;
	}
	
	public URI location()
	{
		return location;
	}
}
