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
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import jolie.lang.parse.module.exceptions.ModuleNotFoundException;

public class ModuleFinderImpl implements ModuleFinder {

	private class ModuleLookUpTarget {
		private final Path basePath;
		private final ImportPath importPath;

		private ModuleLookUpTarget( Path basePath, ImportPath importPath ) {
			this.basePath = basePath;
			this.importPath = importPath;
		}
	}

	/**
	 * a list of path to perform module lookup in
	 */
	private final Path[] packagePaths;

	/**
	 * the working directory path of the execution process
	 */
	private final Path workingDirectory;

	public ModuleFinderImpl( String[] packagePaths ) {
		this( Paths.get( "" ), packagePaths );
	}

	public ModuleFinderImpl( Path workingDirectory, String[] packagePaths ) {
		this.workingDirectory = workingDirectory;
		this.packagePaths = Arrays.stream( packagePaths )
			.map( Paths::get )
			.toArray( Path[]::new );
	}

	public ModuleSource find( URI parentUri, ImportPath importPath ) throws ModuleNotFoundException {
		try {
			if( importPath.isRelativeImport() ) {
				Path parentPath = Paths.get( parentUri );
				ModuleLookUpTarget target = this.resolveDotPrefix( importPath, parentPath );
				return this.moduleLookup( target.basePath, target.importPath );
			} else {
				return this.findAbsoluteImport( importPath );
			}
		} catch( ModuleNotFoundException e ) {
			throw e;
		} catch( FileNotFoundException e ) {
			throw new ModuleNotFoundException( importPath, Paths.get( e.getMessage() ) );
		}
	}

	private ModuleSource findAbsoluteImport( ImportPath importPath ) throws ModuleNotFoundException {
		/**
		 * 1. Try to resolve P directly from WDIR. 2. Check if FIRST.jap is in WDIR/lib. If so, resolve REST
		 * inside of this jap. 3. Try to resolve P from the list of packages directories.
		 */

		List< Path > errPathList = new ArrayList<>();
		try {
			// 1. resolve from Working directory
			return this.moduleLookup( this.workingDirectory, importPath );
		} catch( FileNotFoundException e ) {
			errPathList.add( this.workingDirectory );
		}
		Path japPath;
		try {
			// 2. WDIR/lib/FIRST.jap with entry of REST.ol
			// where importPath[0] = FIRST
			// and importPath[1...] = REST
			japPath =
				ModuleFinder.japLookup( this.workingDirectory.resolve( "lib" ), importPath.pathParts().get( 0 ) );
			List< String > rest = importPath.pathParts().subList( 1, importPath.pathParts().size() );
			return new JapSource( japPath, rest );
		} catch( IOException e ) {
			errPathList.add( Paths.get( e.getMessage() ) );
		}

		// 3. Try to resolve P from the list of packages directories.
		for( Path packagePath : this.packagePaths ) {
			try {
				ModuleSource moduleFile = moduleLookup( packagePath, importPath );
				return moduleFile;
			} catch( FileNotFoundException e ) {
				errPathList.add( packagePath );
			}
		}
		throw new ModuleNotFoundException( importPath, errPathList );
	}

	/**
	 * Perform a lookup for Jolie's executable source code (.ol file)
	 * 
	 * @param basePath a path to perform lookup
	 *
	 * @return source object to be parsed by module parser.
	 */
	private ModuleSource moduleLookup( Path basePath, ImportPath importPath ) throws FileNotFoundException {
		List< String > packageParts = importPath.pathParts().subList( 0, importPath.pathParts().size() - 1 );
		String moduleName = importPath.pathParts().get( importPath.pathParts().size() - 1 );
		for( String packageDir : packageParts ) {
			basePath = basePath.resolve( packageDir );
		}
		if( basePath.resolve( moduleName ).toFile().isDirectory() ) {
			basePath = basePath.resolve( moduleName );
			moduleName = "main";
		}
		Path olTargetFile = ModuleFinder.olLookup( basePath, moduleName );
		return new PathSource( olTargetFile );
	}

	/**
	 * resolve path from source, each dot prefix means 1 level higher from the caller path directory
	 */
	private ModuleLookUpTarget resolveDotPrefix( ImportPath importPath, Path sourcePath ) {
		Path basePath;
		List< String > pathParts = importPath.pathParts();
		if( !sourcePath.toFile().isDirectory() ) {
			basePath = sourcePath.getParent();
		} else {
			basePath = sourcePath;
		}
		int i = 1;
		for( ; i < pathParts.size() - 1; i++ ) {
			if( pathParts.get( i ).isEmpty() ) {
				basePath = basePath.getParent();
			} else {
				break;
			}
		}
		int packagesTokenStartIndex = i;
		ImportPath resolvedImportPath =
			new ImportPath( pathParts.subList( packagesTokenStartIndex, pathParts.size() ) );
		return new ModuleLookUpTarget( basePath, resolvedImportPath );
	}
}
