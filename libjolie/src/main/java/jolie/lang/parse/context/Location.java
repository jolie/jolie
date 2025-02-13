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
import java.net.URI;

/**
 * Implementation of the LSP specification of a Location.
 *
 * @see <a href=
 *      "https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/#location">LSP
 *      location specification</a>
 * @param documentUri The file of the Location.
 * @param range The Range in the document.
 */
public record Location(URI documentUri, Range range) implements Serializable {
	private static final long serialVersionUID = Constants.serialVersionUID();

	public Location( URI documentUri, int startLine, int endLine, int startCharacter, int endCharacter ) {
		this( documentUri,
			new Range( new Position( startLine, startCharacter ), new Position( endLine, endCharacter ) ) );
	}

	public Location {
		if( documentUri == null )
			throw new IllegalArgumentException( "documentUri cannot be null." );
		if( range == null )
			throw new IllegalArgumentException( "range cannot be null" );
	}

}
