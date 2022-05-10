/***************************************************************************
 *   Copyright 2010-2016 (C) by Fabrizio Montesi <famontesi@gmail.com>	   *
 *   Copyright (C) 2021-2022 Vicki Mixen <vicki@mixen.dk>			       *
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
		new URIParsingContext( URI.create( "urn:undefined" ), 0, 0, 0, 0, List.of() );

	/**
	 * URIParsingContext contructor. startLine, endLine, startColumn, endColumn indicate which part of
	 * the source code in the source of the uri we are looking at, and the list code may contain the
	 * indicated code
	 * 
	 * @param uri
	 * @param startLine
	 * @param endLine
	 * @param startColumn
	 * @param endColumn
	 * @param code
	 */
	public URIParsingContext( URI uri, int startLine, int endLine, int startColumn, int endColumn,
		List< String > code ) {
		this.uri = uri;
		this.startColumn = startColumn; // The number character in line, where error occured
		this.endColumn = endColumn;
		this.code = code; // this is the line(s) in the file where the error occured
		this.startLine = startLine;
		this.endLine = endLine;
	}

	/**
	 * returns the uri
	 */
	@Override
	public URI source() {
		return uri;
	}

	/**
	 * returns the file name of the uri
	 */
	@Override
	public String sourceName() {
		try {
			Path path = Paths.get( uri );
			return path.toString();
		} catch( InvalidPathException | FileSystemNotFoundException e ) {
			return uri.toString();
		}
	}

	/**
	 * returns the startLine
	 */
	@Override
	public int startLine() {
		return startLine;
	}

	/**
	 * returns the endLine
	 */
	@Override
	public int endLine() {
		return endLine;
	}

	/**
	 * returns the startColumn
	 */
	@Override
	public int startColumn() {
		return startColumn;
	}

	/**
	 * returns the endColumn
	 */
	@Override
	public int endColumn() {
		return endColumn;
	}

	/**
	 * returns the code which the URIParsingContext points at as a List of strings
	 */
	@Override
	public List< String > enclosingCode() {
		return code;
	}

	/**
	 * returns the code which the URIParsingContext points at as a List of strings, and has the correct
	 * line numbers on each line as well
	 */
	@Override
	public List< String > enclosingCodeWithLineNumbers() {
		int i = startLine;
		List< String > linesWithNumbers = new ArrayList<>();
		for( String line : code ) {
			String newLine = i + 1 + ":" + line;
			linesWithNumbers.add( newLine );
			i++;
		}
		return linesWithNumbers;
	}

	/**
	 * toString method, for easy printing
	 */
	public String toString() {
		String contextString = "";
		contextString += "source: " + uri + "\n";
		contextString += "startLine: " + startLine + "\n";
		contextString += "endLine: " + endLine + "\n";
		contextString += "startColumn: " + startColumn + "\n";
		contextString += "endColumn: " + endColumn + "\n";
		contextString += "enclosingCode:\n" + enclosingCode() + "\n";
		contextString += "enclosingCodeWithLineNumer:\n" + enclosingCodeWithLineNumbers() + "\n";
		return contextString;
	}
}
