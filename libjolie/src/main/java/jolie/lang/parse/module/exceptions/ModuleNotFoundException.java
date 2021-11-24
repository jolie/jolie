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

package jolie.lang.parse.module.exceptions;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.Path;

import jolie.lang.Constants;
import jolie.lang.parse.module.ImportPath;

public class ModuleNotFoundException extends FileNotFoundException {
	private static final long serialVersionUID = Constants.serialVersionUID();

	private final List< Path > lookedPaths;
	private final ImportPath importPath;

	public ModuleNotFoundException( ImportPath importPath, List< Path > lookedPaths ) {
		super();
		this.importPath = importPath;
		this.lookedPaths = lookedPaths;
	}

	public ModuleNotFoundException( ImportPath importPath, Path lookedPath ) {
		super();
		this.importPath = importPath;
		this.lookedPaths = new ArrayList<>();
		this.lookedPaths.add( lookedPath );
	}

	public final List< Path > lookedPaths() {
		return this.lookedPaths;
	}

	public final ImportPath importPath() {
		return this.importPath;
	}

	@Override
	public String getMessage() {
		StringBuilder message =
			new StringBuilder().append( "Module " ).append( '\"' ).append( this.importPath )
				.append( "\" not found from lookup paths.\n" );
		return message.toString();
	}
}
