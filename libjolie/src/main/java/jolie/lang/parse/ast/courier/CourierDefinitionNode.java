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
import jolie.lang.parse.context.ParsingContext;


public class CourierDefinitionNode extends OLSyntaxNode {
	private final String inputPortName;
	private final OLSyntaxNode body;

	public CourierDefinitionNode( ParsingContext context, String inputPortName, OLSyntaxNode body ) {
		super( context );
		this.inputPortName = inputPortName;
		this.body = body;
	}

	public String inputPortName() {
		return inputPortName;
	}

	public OLSyntaxNode body() {
		return body;
	}

	@Override
	public void accept( OLVisitor visitor ) {
		visitor.visit( this );
	}
}
