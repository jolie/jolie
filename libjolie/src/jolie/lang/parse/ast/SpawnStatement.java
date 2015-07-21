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

import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.context.ParsingContext;


public class SpawnStatement extends OLSyntaxNode
{
	private final VariablePathNode indexVariablePath;
	private final VariablePathNode inVariablePath; // may be null
	private final OLSyntaxNode upperBoundExpression, body;

	public SpawnStatement(
			ParsingContext context,
			VariablePathNode indexVariablePath,
			OLSyntaxNode upperBoundExpression,
			VariablePathNode inVariablePath,
			OLSyntaxNode body
	)
	{
		super( context );
		this.indexVariablePath = indexVariablePath;
		this.inVariablePath = inVariablePath;
		this.upperBoundExpression = upperBoundExpression;
		this.body = body;
	}
	
	public OLSyntaxNode body()
	{
		return body;
	}

	public OLSyntaxNode upperBoundExpression()
	{
		return upperBoundExpression;
	}

	public VariablePathNode indexVariablePath()
	{
		return indexVariablePath;
	}
	
	public VariablePathNode inVariablePath()
	{
		return inVariablePath;
	}
	
	@Override
	public void accept( OLVisitor visitor )
	{
		visitor.visit( this );
	}
}
