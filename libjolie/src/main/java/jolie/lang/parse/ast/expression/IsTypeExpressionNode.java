/*
 * Copyright (C) Fabrizio Montesi <famontesi@gmail.com>
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

package jolie.lang.parse.ast.expression;

import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.ast.VariablePathNode;
import jolie.lang.parse.context.ParsingContext;


public class IsTypeExpressionNode extends OLSyntaxNode {
	public enum CheckType {
		DEFINED, INT, STRING, DOUBLE, LONG, BOOL
	}

	private final VariablePathNode variablePath;
	private final CheckType type;

	public IsTypeExpressionNode( ParsingContext context, CheckType type, VariablePathNode variablePath ) {
		super( context );
		this.type = type;
		this.variablePath = variablePath;
	}

	public CheckType type() {
		return type;
	}

	public VariablePathNode variablePath() {
		return variablePath;
	}

	@Override
	public void accept( OLVisitor visitor ) {
		visitor.visit( this );
	}
}
