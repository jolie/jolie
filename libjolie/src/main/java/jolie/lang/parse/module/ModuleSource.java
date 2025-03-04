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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Optional;
import jolie.lang.parse.ast.ServiceNode;

/**
 * an Interface of Joile module Source
 */
public interface ModuleSource {

	/**
	 * @return URI location of the module
	 */
	URI uri();

	/**
	 * @return an optional include path for parsing this module
	 */
	Optional< URI > includePath();

	/**
	 * @return an InputStream of source
	 */
	InputStream openStream() throws IOException;

	/**
	 * @return name of module
	 */
	String name();

	/**
	 * @return name of module
	 */
	Optional< URI > parentURI();

	/**
	 * Creates a new ModuleSource instance from given ServiceNode. This creation method is used by the
	 * ServiceLoader
	 *
	 * @param node the ServiceNode from which the ModuleSource is created
	 * @return a new ModuleSource instance
	 */
	public static ModuleSource create( ServiceNode node ) {
		return new ProgramSource( node.program(), node.context().source(), node.name() );
	}

	/**
	 * Creates a new ModuleSource instance from given URI and InputStream. This creation method is used
	 * for parsing Jolie code string or parsing an include files
	 *
	 * @param uri the URI of the module
	 * @param is the InputStream of the module
	 * @param name Service name to execute
	 * @return a new ModuleSource instance
	 */
	public static ModuleSource create( URI uri, InputStream is, String name ) {
		return new InputStreamSource( is, uri, name );
	}

	/**
	 * Creates a new ModuleSource instance from given URI. This creation method is used when parsing
	 * from a file or a jap archive.
	 *
	 * @param uri the URI of the module
	 * @return a new ModuleSource instance
	 * @throws FileNotFoundException if the file does not exist
	 */
	public static ModuleSource create( URI uri ) throws FileNotFoundException {
		if( uri.getScheme() == null ) {
			return new ProgramSource( null, uri );
		}
		switch( uri.getScheme() ) {
		case "jap":
			return new JapSource( uri );
		case "file":
			return new PathSource( Paths.get( uri ) );
		default:
			return new ProgramSource( null, uri );
		}
	}
}
