/***************************************************************************
 *   Copyright (C) by Fabrizio Montesi <famontesi@gmail.com>			   *
 *   Copyright (C) 2021-2022 Vicki Mixen <vicki@mixen.dk>	               *
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
import java.util.List;

/**
 * A {@code ParsingContext} allows for the retrieval of information regarding the context in which
 * an {@link jolie.lang.parse.ast.OLSyntaxNode} was parsed.
 *
 * @author Fabrizio Montesi
 * @see jolie.lang.parse.ast.OLSyntaxNode
 */
public interface ParsingContext extends Serializable {
	/**
	 * Returns an URI for the source from which the node has been read.
	 *
	 * @return an URI for the source from which the node has been read.
	 */
	URI source();

	/**
	 * Returns the simple name of the source from which the node has been read. This could be, e.g., the
	 * simple name of a file (instead of its complete absolute path).
	 *
	 * @return the simple name of the source from which the node has been read
	 */
	String sourceName();

	/**
	 * Returns the startLine at which the node has been read.
	 *
	 * @return the startLine at which the node has been read
	 */
	int startLine();

	/**
	 * Returns the endLine at which the node has been read.
	 *
	 * @return the endLine at which the node has been read
	 */
	int endLine();

	/**
	 * Returns the startColumn at which the node has been read.
	 *
	 * @return the startColumn at which the node has been read
	 */
	int startColumn();

	/**
	 * Returns the endColumn at which the node has been read.
	 *
	 * @return the endColumn at which the node has been read
	 */
	int endColumn();

	/**
	 * Returns the code as a List of strings, which were read
	 *
	 * @return the code as a List of strings, which were read
	 */
	List< String > enclosingCode();

	/**
	 * Returns the code as a List of strings, which were read, with line numbers
	 *
	 * @return the code as a List of strings, which were read, with line numbers
	 */
	List< String > enclosingCodeWithLineNumbers();

	/**
	 * Returns a Location representation of the ParsingContext.
	 *
	 * @return The Location of the ParsingContext.
	 */
	default Location location() {
		return new Location( source(), startLine(), endLine(), startColumn(), endColumn() );
	}

	/**
	 * Returns a string interpretations of the ParsingContext
	 *
	 * @return a string interpretations of the ParsingContext
	 */
	@Override
	String toString();
}
