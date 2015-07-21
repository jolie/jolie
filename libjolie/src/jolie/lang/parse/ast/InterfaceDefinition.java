/***************************************************************************
 *   Copyright (C) 2008 by Fabrizio Montesi                                *
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

import java.util.HashMap;
import java.util.Map;
import jolie.lang.parse.DocumentedNode;
import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.context.ParsingContext;

/**
 *
 * @author Fabrizio Montesi
 */
public class InterfaceDefinition extends OLSyntaxNode implements OperationCollector, DocumentedNode
{
	private final Map<String, OperationDeclaration> operationsMap =
		new HashMap<>();
	private final String name;
	private String documentation;

	public InterfaceDefinition( ParsingContext context, String name )
	{
		super( context );
		this.name = name;
	}

	@Override
	public Map< String, OperationDeclaration > operationsMap()
	{
		return operationsMap;
	}

	public String name()
	{
		return name;
	}

	@Override
	public void addOperation( OperationDeclaration decl )
	{
		operationsMap.put( decl.id(), decl );
	}

	public void copyTo( OperationCollector oc )
	{
		oc.operationsMap().putAll( operationsMap );
	}

	@Override
	public void accept( OLVisitor visitor )
	{
		visitor.visit( this );
	}

	@Override
	public void setDocumentation( String documentation )
	{
		this.documentation = documentation;
	}

	@Override
	public String getDocumentation()
	{
		return this.documentation;
	}
}
