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
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class FinderCreator {

	private final Path workingDirectory;
	private final Path[] packagesDirectories;

	public FinderCreator( String[] packagesDirectoriesStr )
		throws FileNotFoundException {
		this.workingDirectory = Paths.get( "" );
		this.packagesDirectories = Arrays.stream( packagesDirectoriesStr )
			.map( ( packageDirectoryStr ) -> Paths.get( packageDirectoryStr ) )
			.toArray( Path[]::new );
	}

	public FinderCreator( Path workingDirectory, String[] packagesDirectoriesStr )
		throws FileNotFoundException {
		this.workingDirectory = workingDirectory;
		this.packagesDirectories = Arrays.stream( packagesDirectoriesStr )
			.map( ( packageDirectoryStr ) -> Paths.get( packageDirectoryStr ) )
			.toArray( Path[]::new );
	}

	/**
	 * returns a Finder object corresponding to target either it is a relative import(starts with .) or
	 * absolute one
	 */
	public Finder getFinderForTarget( URI source, String[] target ) {
		final boolean isRelativeImport = target[ 0 ].isEmpty() ? true : false;

		if( isRelativeImport ) {
			return new RelativePathFinder( target, source );
		} else {
			return new AbsolutePathFinder( target, this.workingDirectory,
				this.packagesDirectories );
		}
	}
}
