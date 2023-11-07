/*
 * Copyright (C) 2011-2020 Fabrizio Montesi <famontesi@gmail.com>
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

package jolie.lang.parse.ast.courier;

import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.ast.VariablePathNode;
import jolie.lang.parse.context.ParsingContext;

/**
 *
 * @author Fabrizio Montesi
 */
public class NotificationForwardStatement extends OLSyntaxNode {
	private final String outputPortName;
	private final VariablePathNode outputVariablePath;

	public NotificationForwardStatement(
		ParsingContext context,
		String outputPortName,
		VariablePathNode outputVariablePath ) {
		super( context );
		this.outputPortName = outputPortName;
		this.outputVariablePath = outputVariablePath;
	}

	public String outputPortName() {
		return outputPortName;
	}

	public VariablePathNode outputVariablePath() {
		return outputVariablePath;
	}

	@Override
	public < C, R > R accept( OLVisitor< C, R > visitor, C ctx ) {
		return visitor.visit( this, ctx );
	}
}
