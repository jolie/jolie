/*
 * Copyright (C) 2015-2020 Fabrizio Montesi <famontesi@gmail.com>
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

import java.io.Serializable;

import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.ast.VariablePathNode;
import jolie.lang.parse.context.ParsingContext;


public class InlineTreeExpressionNode extends OLSyntaxNode {
	public static interface Operation {
	}

	public static class AssignmentOperation implements Operation, Serializable {
		private final VariablePathNode path;
		private final OLSyntaxNode expression;

		public AssignmentOperation( VariablePathNode path, OLSyntaxNode expression ) {
			this.path = path;
			this.expression = expression;
		}

		public VariablePathNode path() {
			return path;
		}

		public OLSyntaxNode expression() {
			return expression;
		}
	}

	public static class DeepCopyOperation implements Operation, Serializable {
		private final VariablePathNode path;
		private final OLSyntaxNode expression;

		public DeepCopyOperation( VariablePathNode path, OLSyntaxNode expression ) {
			this.path = path;
			this.expression = expression;
		}

		public VariablePathNode path() {
			return path;
		}

		public OLSyntaxNode expression() {
			return expression;
		}
	}

	public static class PointsToOperation implements Operation, Serializable {
		private final VariablePathNode path;
		private final VariablePathNode target;

		public PointsToOperation( VariablePathNode path, VariablePathNode target ) {
			this.path = path;
			this.target = target;
		}

		public VariablePathNode path() {
			return path;
		}

		public VariablePathNode target() {
			return target;
		}
	}

	private final OLSyntaxNode rootExpression;
	private final Operation[] operations;

	public InlineTreeExpressionNode(
		ParsingContext context,
		OLSyntaxNode rootExpression,
		Operation[] operations ) {
		super( context );
		this.rootExpression = rootExpression;
		this.operations = operations;
	}

	public OLSyntaxNode rootExpression() {
		return rootExpression;
	}

	public Operation[] operations() {
		return operations;
	}

	@Override
	public void accept( OLVisitor visitor ) {
		visitor.visit( this );
	}
}
