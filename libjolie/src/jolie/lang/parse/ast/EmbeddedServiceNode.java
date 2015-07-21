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

import jolie.lang.Constants;
import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.context.ParsingContext;

public class EmbeddedServiceNode extends OLSyntaxNode
{
	private final String servicePath;
	private final String portId;
	private final Constants.EmbeddedServiceType type;

	private Program program = null;

	public EmbeddedServiceNode(
			ParsingContext context,
			Constants.EmbeddedServiceType type,
			String servicePath,
			String portId )
	{
		super( context );
		this.type = type;
		this.servicePath = servicePath;
		this.portId = portId;
	}
	
	public Constants.EmbeddedServiceType type()
	{
		return type;
	}
	
	public String servicePath()
	{
		return servicePath;
	}
	
	public String portId()
	{
		return portId;
	}

	public void setProgram( Program program )
	{
		this.program = program;
	}

	public Program program()
	{
		return program;
	}

	@Override
	public void accept( OLVisitor visitor )
	{
		visitor.visit( this );
	}
}
