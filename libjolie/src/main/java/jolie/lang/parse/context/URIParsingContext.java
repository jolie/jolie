/***************************************************************************
 *   Copyright 2010-2016 (C) by Fabrizio Montesi <famontesi@gmail.com>     *
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
import java.util.ArrayList;
import java.util.List;
import jolie.lang.Constants;


/**
 * A very simple {@code ParsingContext} based upon an URI.
 * 
 * @author Fabrizio Montesi
 */
public class URIParsingContext implements ParsingContext {
	private static final long serialVersionUID = Constants.serialVersionUID();
	private final URI uri;
	private final List< String > code;
	private final int startColumn;
	private final int endColumn;
	private final int startLine;
	private final int endLine;

	public static final URIParsingContext DEFAULT =
		new URIParsingContext( URI.create( "urn:undefined" ), 1, 1, 0, 0, List.of() );

	public URIParsingContext( URI uri, int startLine, int endLine, int startColumn, int endColumn,
		List< String > code ) {
		this.uri = uri;
		this.startColumn = startColumn; // The number character in line, where error occured
		this.endColumn = endColumn;
		this.code = code; // this is the line(s) in the file where the error occured
		this.startLine = startLine;
		this.endLine = endLine;
	}

	@Override
	public URI source() {
		return uri;
	}

	@Override
	public String sourceName() {
		try {
			Path path = Paths.get( uri );
			return path.toString();
		} catch( InvalidPathException | FileSystemNotFoundException e ) {
			return uri.toString();
		}
	}

	@Override
	public int startLine() {
		return startLine;
	}

	@Override
	public int endLine() {
		return endLine;
	}

	@Override
	public int startColumn() {
		return startColumn;
	}

	@Override
	public int endColumn() {
		return endColumn;
	}

	@Override
	public List< String > enclosingCode() {
		return code;
	}

	@Override
	public List< String > enclosingCodeWithLineNumbers() {
		int i = startLine;
		List< String > linesWithNumbers = new ArrayList<>();
		for( String line : code ) {
			String newLine = i + ":" + line;
			linesWithNumbers.add( newLine );
			i++;
		}
		return linesWithNumbers;
	}
}
