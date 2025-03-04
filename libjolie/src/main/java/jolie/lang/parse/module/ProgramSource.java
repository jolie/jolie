/*
 * Copyright (C) 2020 Narongrit Unwerawattana <narongrit.kie@gmail.com>
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

package jolie.lang.parse.module;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Optional;
import jolie.lang.parse.ast.Program;

/**
 * An implementation of Source for internal Jolie
 */
class ProgramSource implements ModuleSource {

	@SuppressWarnings( "PMD" )
	private final Program program; // TODO move program reference on other classes to this field
	private final URI uri;
	private final String name;

	public ProgramSource( Program program, URI uri, String name ) {
		this.program = program;
		this.uri = uri;
		this.name = name;
	}

	public ProgramSource( Program program, URI uri ) {
		this.program = program;
		this.uri = uri;
		this.name = null;
	}


	@Override
	public URI uri() {
		return this.uri;
	}

	/**
	 * the include path of ol file should be empty
	 */
	@Override
	public Optional< URI > includePath() {
		return Optional.empty();
	}

	@Override
	public InputStream openStream() {
		throw new UnsupportedOperationException( "This method should not be called on ProgramSource" );
	}

	@Override
	public String name() {
		return this.name == null
			? this.uri.getSchemeSpecificPart().substring( this.uri.getSchemeSpecificPart().lastIndexOf( "/" ) + 1 )
			: this.name;
	}

	@Override
	public Optional< URI > parentURI() {
		if( this.uri.getScheme() != null && this.uri.getScheme().equals( "jap" ) ) {
			return Optional
				.of( URI.create( this.uri.toString().substring( 0, this.uri.toString().lastIndexOf( "/" ) ) ) );
		}
		if( Paths.get( this.uri ).toFile().exists() ) {
			return Optional.of( Paths.get( this.uri ).getParent().toUri() );
		}
		return Optional.empty();
	}
}
