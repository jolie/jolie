/*
 * Copyright (C) 2025 Narongrit Unwerawattana <narongrit.kie@gmail.com>
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

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;

/**
 * A module source that located on the file system.
 *
 */
class PathSource implements ModuleSource {

	private final Path path;

	/**
	 * Construct a new PathSource from given path.
	 *
	 * @param p a path to the source file
	 * @throws FileNotFoundException if the given path does not exist.
	 */
	public PathSource( Path p ) throws FileNotFoundException {
		if( !p.toFile().exists() ) {
			throw new FileNotFoundException( p.toString() );
		}
		this.path = p;
	}

	@Override
	public URI uri() {
		return this.path.toUri();
	}

	/**
	 * the include path of ol file should be empty
	 */
	@Override
	public Optional< URI > includePath() {
		return Optional.of( this.path.getParent().toUri() );
	}

	@Override
	public InputStream openStream() throws FileNotFoundException {
		InputStream is = new FileInputStream( this.path.toFile() );
		// wrap with BufferInputStream for improve performance
		return new BufferedInputStream( is );
	}

	@Override
	public String name() {
		return this.path.getFileName().toString();
	}

	@Override
	public Optional< URI > parentURI() {
		return Optional.of( this.path.getParent().toUri() );
	}

}
