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
import java.util.ArrayList;
import java.util.List;

import jolie.lang.Keywords;
import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.ast.expression.ConstantIntegerExpression;
import jolie.lang.parse.ast.expression.ConstantStringExpression;
import jolie.lang.parse.context.ParsingContext;
import jolie.util.Pair;


public class VariablePathNode extends OLSyntaxNode implements Serializable {
	public enum Type {
		NORMAL, GLOBAL, CSET
	}

	private final List< Pair< OLSyntaxNode, OLSyntaxNode > > path;
	private final Type type;

	public VariablePathNode( ParsingContext context, Type type ) {
		super( context );
		path = new ArrayList<>();
		this.type = type;
	}

	public VariablePathNode( ParsingContext context, Type type, int size ) {
		super( context );
		path = new ArrayList<>( size );
		this.type = type;
	}

	public Type type() {
		return type;
	}

	public boolean isGlobal() {
		return type == Type.GLOBAL;
	}

	public boolean isCSet() {
		return type == Type.CSET;
	}

	public boolean isStatic() {
		for( Pair< OLSyntaxNode, OLSyntaxNode > node : path ) {
			if( node.key() instanceof ConstantStringExpression == false ) {
				return false;
			}
			if( node.value() != null && node.value() instanceof ConstantIntegerExpression == false ) {
				return false;
			}
		}
		return true;
	}

	public static void levelPaths( VariablePathNode leftPath, VariablePathNode rightPath ) {
		int leftIndex = leftPath.path().size() - 1;
		int rightIndex = rightPath.path().size() - 1;

		Pair< OLSyntaxNode, OLSyntaxNode > left = leftPath.path().get( leftIndex );
		Pair< OLSyntaxNode, OLSyntaxNode > right = rightPath.path().get( rightIndex );

		if( left.value() == null && right.value() != null ) {
			left = new Pair<>(
				left.key(),
				new ConstantIntegerExpression( leftPath.context(), 0 ) );
			leftPath.path().set( leftIndex, left );
		} else if( left.value() != null && right.value() == null ) {
			right = new Pair<>(
				right.key(),
				new ConstantIntegerExpression( rightPath.context(), 0 ) );
			rightPath.path().set( rightIndex, right );
		}
	}

	public void append( Pair< OLSyntaxNode, OLSyntaxNode > node ) {
		path.add( node );
	}

	public List< Pair< OLSyntaxNode, OLSyntaxNode > > path() {
		return path;
	}

	@Override
	public < C, R > R accept( OLVisitor< C, R > visitor, C ctx ) {
		return visitor.visit( this, ctx );
	}

	public boolean isEquivalentTo( VariablePathNode right ) {
		return checkVariablePathNodeEquivalence( this, right );
	}

	private static boolean checkVariablePathNodeEquivalence( VariablePathNode left, VariablePathNode right ) {
		if( left.path.size() != right.path.size() ) {
			return false;
		}

		if( left.type != right.type ) {
			return false;
		}

		Pair< OLSyntaxNode, OLSyntaxNode > leftPair, rightPair;
		for( int i = 0; i < left.path.size(); i++ ) {
			leftPair = left.path.get( i );
			rightPair = right.path.get( i );
			if( checkNodeKeyEquivalence( leftPair.key(), rightPair.key() ) == false ||
				checkNodeIndexEquivalence( leftPair.value(), rightPair.value() ) == false ) {
				return false;
			}
		}
		return true;
	}

	private static boolean checkNodeKeyEquivalence( OLSyntaxNode left, OLSyntaxNode right ) {
		if( left.equals( right ) ) {
			return true;
		}

		if( left instanceof ConstantStringExpression && right instanceof ConstantStringExpression ) {
			return ((ConstantStringExpression) left).value().equals( ((ConstantStringExpression) right).value() );
		}

		return false;
	}

	private static boolean checkNodeIndexEquivalence( OLSyntaxNode left, OLSyntaxNode right ) {
		if( left == right ) { // Used for null values and same objects
			return true;
		}

		if( left == null && right instanceof ConstantIntegerExpression ) {
			return ((ConstantIntegerExpression) right).value() == 0;
		}

		if( right == null && left instanceof ConstantIntegerExpression ) {
			return ((ConstantIntegerExpression) left).value() == 0;
		}

		if( left != null && right != null ) {
			if( left.equals( right ) ) {
				return true;
			}

			if( left instanceof ConstantIntegerExpression && right instanceof ConstantIntegerExpression ) {
				return ((ConstantIntegerExpression) left).value() == ((ConstantIntegerExpression) right).value();
			}
		}

		return false;
	}

	public String toPrettyString() {
		StringBuilder builder = new StringBuilder();
		Pair< OLSyntaxNode, OLSyntaxNode > node;
		if( isGlobal() ) {
			builder.append( Keywords.GLOBAL );
			if( path.size() > 1 ) {
				builder.append( '.' );
			}
		}
		for( int i = 0; i < path.size(); i++ ) {
			node = path.get( i );
			if( node.key() instanceof ConstantStringExpression ) {
				builder.append( ((ConstantStringExpression) node.key()).value() );
			} else {
				builder.append( "<Expression>" );
			}
			if( node.value() != null ) {
				if( node.value() instanceof ConstantIntegerExpression ) {
					ConstantIntegerExpression expr = (ConstantIntegerExpression) node.value();
					if( expr.value() != 0 ) {
						builder.append( '[' )
							.append( node.value() )
							.append( ']' );
					}
				} else {
					builder.append( '[' )
						.append( "<Expression>" )
						.append( ']' );
				}
			}

			if( i < path.size() - 1 ) {
				builder.append( '.' );
			}
		}
		return builder.toString();
	}
}
