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

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.ParsingContext;
import jolie.util.Pair;


public class VariablePathNode extends OLSyntaxNode implements Serializable
{
	final private List< Pair< OLSyntaxNode, OLSyntaxNode > > path;
	final private boolean global;

	public VariablePathNode( ParsingContext context, boolean global )
	{
		super( context );
		path = new LinkedList< Pair< OLSyntaxNode, OLSyntaxNode > >();
		this.global = global;
	}
	
	public boolean isGlobal()
	{
		return global;
	}

	public static void levelPaths( VariablePathNode leftPath, VariablePathNode rightPath )
	{
		int leftIndex = leftPath.path().size() - 1;
		int rightIndex = rightPath.path().size() - 1;

		Pair< OLSyntaxNode, OLSyntaxNode > left = leftPath.path().get( leftIndex );
		Pair< OLSyntaxNode, OLSyntaxNode > right = rightPath.path().get( rightIndex );

		if ( left.value() == null && right.value() != null ) {
			left = new Pair< OLSyntaxNode, OLSyntaxNode >(
					left.key(),
					new ConstantIntegerExpression( leftPath.context(), 0 )
				);
			leftPath.path().set( leftIndex, left );
		} else if ( left.value() != null && right.value() == null ) {
			right = new Pair< OLSyntaxNode, OLSyntaxNode >(
					right.key(),
					new ConstantIntegerExpression( rightPath.context(), 0 )
				);
			rightPath.path().set( rightIndex, right );
		}
	}
	
	public void append( Pair< OLSyntaxNode, OLSyntaxNode > node )
	{
		path.add( node );
	}
	
	public List< Pair< OLSyntaxNode, OLSyntaxNode > > path()
	{
		return path;
	}

	public void accept( OLVisitor visitor )
	{
		visitor.visit( this );
	}
}
