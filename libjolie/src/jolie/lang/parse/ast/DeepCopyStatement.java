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
import jolie.lang.parse.ast.expression.VariableExpressionNode;
import jolie.lang.parse.context.ParsingContext;


public class DeepCopyStatement extends OLSyntaxNode
{
	private final VariablePathNode leftPath;
	private final OLSyntaxNode rightExpression;

	public DeepCopyStatement( ParsingContext context, VariablePathNode leftPath, OLSyntaxNode rightExpression )
	{
		super( context );
		if ( rightExpression instanceof VariableExpressionNode ) {
			VariablePathNode.levelPaths( leftPath, ((VariableExpressionNode) rightExpression).variablePath() );
		}
		this.leftPath = leftPath;
		this.rightExpression = rightExpression;
	}
	
	public VariablePathNode leftPath()
	{
		return leftPath;
	}
	
	public OLSyntaxNode rightExpression()
	{
		return rightExpression;
	}
	
	@Override
	public void accept( OLVisitor visitor )
	{
		visitor.visit( this );
	}
}
