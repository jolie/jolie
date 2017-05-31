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
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * A very simple {@code ParsingContext} based upon an URI.
 * @author Fabrizio Montesi
 */
public class URIParsingContext extends AbstractParsingContext
{
	private final URI uri;

	public static final URIParsingContext DEFAULT = new URIParsingContext( URI.create( "urn:undefined" ), 0 );

	public URIParsingContext( URI uri, int line )
	{
		super( line );
		this.uri = uri;
	}

	@Override
	public URI source()
	{
		return uri;
	}

	@Override
	public String sourceName()
	{
		try {
			Path path = Paths.get( uri );
			return path.toString();
		} catch( IllegalArgumentException | FileSystemNotFoundException e ) {
			return uri.toString();
		}
	}
}
