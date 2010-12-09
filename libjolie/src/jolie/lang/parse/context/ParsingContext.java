/***************************************************************************
 *   Copyright (C) by Fabrizio Montesi <famontesi@gmail.com>               *
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

import java.io.Serializable;
import java.net.URI;

/**
 * A {@code ParsingContext} allows for the retrieval of information
 * regarding the context in which an {@link jolie.lang.parse.ast.OLSyntaxNode}
 * was parsed.
 * @author Fabrizio Montesi
 * @see jolie.lang.parse.ast.OLSyntaxNode
 */
public interface ParsingContext extends Serializable
{
	/**
	 * Returns an URI for the source from which the node has been read.
	 * @return an URI for the source from which the node has been read.
	 */
	public URI source();

	/**
	 * Returns the simple name of the source from which the node has been read.
	 * This could be, e.g., the simple name of a file (instead of its complete
	 * absolute path).
	 * @return the simple name of the source from which the node has been read
	 */
	public String sourceName();

	/**
	 * Returns the line at which the node has been read.
	 * @return the line at which the node has been read
	 */
	public int line();
}
