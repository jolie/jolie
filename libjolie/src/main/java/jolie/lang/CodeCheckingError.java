/*
 * Copyright (C) 2020 Fabrizio Montesi <famontesi@gmail.com>
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
 * MA 02110-1301 USA
 */

package jolie.lang;

import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.context.ParsingContext;
import jolie.lang.parse.context.URIParsingContext;

public class CodeCheckingError {
	private final ParsingContext context;
	private final String message;

	private CodeCheckingError( ParsingContext context, String message ) {
		this.context = context;
		this.message = message;
	}

	public ParsingContext context() {
		return context;
	}

	@Override
	public String toString() {
		return context.sourceName() + ":" + context.line() + ": error: " + message;
	}

	public static CodeCheckingError build( OLSyntaxNode node, String message ) {
		return new CodeCheckingError(
			(node != null) ? node.context() : URIParsingContext.DEFAULT,
			message );
	}
}
