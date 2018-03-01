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

package jolie.lang.parse.ast.expression;


import java.util.ArrayList;
import java.util.List;
import jolie.lang.Constants;
import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.context.ParsingContext;
import jolie.util.Pair;



public class ProductExpressionNode extends OLSyntaxNode
{
	private final List< Pair< Constants.OperandType, OLSyntaxNode > > operands = new ArrayList<>();

	public ProductExpressionNode( ParsingContext context )
	{
		super( context );
	}
	
	public void multiply( OLSyntaxNode expression )
	{
		operands.add( new Pair<>( Constants.OperandType.MULTIPLY, expression ) );
	}
	
	public void divide( OLSyntaxNode expression )
	{
		operands.add( new Pair<>( Constants.OperandType.DIVIDE, expression ) );
	}
	
	public void modulo( OLSyntaxNode expression )
	{
		operands.add( new Pair<>( Constants.OperandType.MODULUS, expression ) );
	}
	
	public List< Pair< Constants.OperandType, OLSyntaxNode > > operands()
	{
		return operands;
	}
	
	@Override
	public void accept( OLVisitor visitor )
	{
		visitor.visit( this );
	}
}
