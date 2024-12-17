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
