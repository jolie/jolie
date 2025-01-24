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
 * Implements the LSP specification of a Position.
 *
 * @param line The zero-based line of the Position.
 * @param character The zero-based character offset.
 * @see <a href=
 *      "https://microsoft.github.io/language-server-protocol/specifications/lsp/3.17/specification/#position">LSP
 *      position specification</a>
 */
public record Position(int line, int character) implements Serializable {
	private static final long serialVersionUID = Constants.serialVersionUID();

	// /** FIXME re-add this exception when use of -1 is eliminated
	// * @throws IllegalArgumentException If line or character are negative.
	// */
	// public Position {
	// if( line < 0 || character < 0 )
	// throw new IllegalArgumentException( "Position line and character must be non-negative." );
	// }
}
