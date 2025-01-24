/***************************************************************************
 *   Copyright (C) 2024 Kasper Okumu <kaspokumu@gmail.com>                 *
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

package jolie.lang.parse.context;

import jolie.lang.Constants;

import java.io.Serializable;
import java.net.URI;

/**
 * Implements of the LSP specification of a Location.
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
