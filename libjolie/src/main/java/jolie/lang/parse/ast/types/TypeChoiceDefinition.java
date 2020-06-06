/*
 * Copyright (C) 2015 Fabrizio Montesi <famontesi@gmail.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package jolie.lang.parse.ast.types;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.context.ParsingContext;
import jolie.util.Pair;
import jolie.util.Range;

public class TypeChoiceDefinition extends TypeDefinition {
	private final TypeDefinition left;
	private final TypeDefinition right;

	public TypeChoiceDefinition( ParsingContext context, String id, Range cardinality, TypeDefinition left,
		TypeDefinition right ) {
		super( context, id, cardinality );
		this.left = left;
		this.right = right;
	}

	@Override
	public void accept( OLVisitor visitor ) {
		visitor.visit( this );
	}

	public TypeDefinition left() {
		return left;
	}

	public TypeDefinition right() {
		return right;
	}

	@Override
	protected boolean containsPath( Iterator< Pair< OLSyntaxNode, OLSyntaxNode > > it ) {
		final List< Pair< OLSyntaxNode, OLSyntaxNode > > path = new LinkedList<>();
		it.forEachRemaining( pair -> path.add( pair ) );
		return left.containsPath( path.iterator() ) && right.containsPath( path.iterator() );
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((left == null) ? 0 : left.hashCode());
		result = prime * result + ((right == null) ? 0 : right.hashCode());
		return result;
	}

	@Override
	public boolean equals( Object obj ) {
		if( this == obj )
			return true;
		if( !super.equals( obj ) )
			return false;
		if( getClass() != obj.getClass() )
			return false;
		TypeChoiceDefinition other = (TypeChoiceDefinition) obj;
		if( left == null ) {
			if( other.left != null )
				return false;
		} else if( !left.equals( other.left ) )
			return false;
		if( right == null ) {
			if( other.right != null )
				return false;
		} else if( !right.equals( other.right ) )
			return false;
		return true;
	}

}
