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
import jolie.lang.parse.Scanner;


public class CompareConditionNode extends OLSyntaxNode
{
	private final OLSyntaxNode leftExpression, rightExpression;
	private final Scanner.TokenType opType;

	public CompareConditionNode( ParsingContext context, OLSyntaxNode leftExpr, OLSyntaxNode rightExpr, Scanner.TokenType opType )
	{
		super( context );
		this.leftExpression = leftExpr;
		this.rightExpression = rightExpr;
		this.opType = opType;
	}
	
	public OLSyntaxNode leftExpression()
	{
		return leftExpression;
	}
	
	public OLSyntaxNode rightExpression()
	{
		return rightExpression;
	}
	
	public Scanner.TokenType opType()
	{
		return opType;
	}
	
	@Override
	public void accept( OLVisitor visitor )
	{
		visitor.visit( this );
	}
}
