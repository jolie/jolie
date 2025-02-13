/*
 * Copyright (C) 2025 Kasper Okumu <kaspokumu@gmail.com>
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

package jolie.lang.parse.context;

import jolie.lang.Constants;

import java.io.Serializable;

/**
 * A text document range with zero-based start and end positions. Implements the LSP specification
 * of a Range.
 *
 * @see <a href=
 *      "https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/#range">LSP
 *      range specification</a>
 * @param start The start Position of the Range.
 * @param end The end Position of the Range.
 */
public record Range(Position start, Position end) implements Serializable {
	private static final long serialVersionUID = Constants.serialVersionUID();

	public Range {
		if( start == null )
			throw new IllegalArgumentException( "Range cannot have a null start." );
		if( end == null )
			throw new IllegalArgumentException( "Range cannot have a null end." );
	}

}
