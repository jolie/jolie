/*
 * Copyright (C) 2016 Fabrizio Montesi <famontesi@gmail.com>
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

package jolie.lang.parse.ast;

import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.ast.expression.ConstantIntegerExpression;
import jolie.lang.parse.context.ParsingContext;
import jolie.util.Pair;

public class ForEachArrayItemStatement extends OLSyntaxNode
{
	private final VariablePathNode keyPath, targetPath;
	private final OLSyntaxNode body;

	public ForEachArrayItemStatement(
		ParsingContext context,
		VariablePathNode keyPath,
		VariablePathNode targetPath,
		OLSyntaxNode body
	) {

		super( context );

		final int index = keyPath.path().size() - 1;
		Pair< OLSyntaxNode, OLSyntaxNode > pair = keyPath.path().get( index );
		if ( pair.value() == null ) {
			pair = new Pair<>(
				pair.key(),
				new ConstantIntegerExpression( keyPath.context(), 0 )
			);
			keyPath.path().set( index, pair );
		}

		this.keyPath = keyPath;
		this.targetPath = targetPath;
		this.body = body;
	}

	public OLSyntaxNode body()
	{
		return body;
	}

	public VariablePathNode keyPath()
	{
		return keyPath;
	}

	public VariablePathNode targetPath()
	{
		return targetPath;
	}

	@Override
	public void accept( OLVisitor visitor )
	{
		visitor.visit( this );
	}
}
