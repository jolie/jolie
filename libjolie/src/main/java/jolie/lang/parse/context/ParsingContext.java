/***************************************************************************
 *   Copyright 2010-2016 (C) by Fabrizio Montesi <famontesi@gmail.com>	   *
 *   Copyright (C) 2021-2022 Vicki Mixen <vicki@mixen.dk>                  *
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

import java.net.URI;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import jolie.lang.Constants;

/**
 * Allows for the retrieval of information regarding the context in which
 * an {@link jolie.lang.parse.ast.OLSyntaxNode} was parsed.
 * @param textLocation
 * @param enclosingCode
 */
public record ParsingContext(Location textLocation, List< String > enclosingCode) implements Serializable {
	private static final long serialVersionUID = Constants.serialVersionUID();

	public static final ParsingContext DEFAULT =
		new ParsingContext( URI.create( "urn:undefined" ), 0, 0, 0, 0, List.of() );

	public ParsingContext( URI uri, int startLine, int endLine, int startCharacter, int endCharacter,
		List< String > enclosingCode ) {
		this( new Location( uri, startLine, endLine, startCharacter, endCharacter ), enclosingCode );
	}

	/**
	 *
	 * @return The URI of the file containing the code
	 */
	public URI source() {
		return textLocation().documentUri();
	}

	/**
	 * @return The name of the file containing the code.
	 */
	public String sourceName() {
		try {
			Path path = Paths.get( source() );
			return path.toString();
		} catch( InvalidPathException | FileSystemNotFoundException e ) {
			return source().toString();
		}
	}

	/**
	 * @return The zero-based starting line of the code range.
	 */
	public int startLine() {
		return textLocation().range().start().line();
	}

	/**
	 * @return The zero-based ending line of the code range.
	 */
	public int endLine() {
		return textLocation().range().end().line();
	}

	/**
	 *
	 * @return The zero-based starting character column of the code range.
	 */
	public int startColumn() {
		return textLocation().range().start().character();
	}

	/**
	 *
	 * @return The zero-based starting character column of the code range.
	 */
	public int endColumn() {
		return textLocation().range().end().character();
	}

	/**
	 *
	 * @return the code which the ParsingContext points at as a List of strings, and has the correct
	 *         line numbers on each line as well
	 */
	public List< String > enclosingCodeWithLineNumbers() {
		int i = textLocation().range().start().line();
		List< String > linesWithNumbers = new ArrayList<>();
		for( String line : enclosingCode() ) {
			String newLine = i + ":" + line;
			linesWithNumbers.add( newLine );
			i++;
		}
		return linesWithNumbers;
	}
}
