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
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import jolie.lang.Constants;
import jolie.lang.parse.module.exceptions.ModuleNotFoundException;
import jolie.util.UriUtils;

public class ModuleFinderImpl implements ModuleFinder {

	private static class ModuleLookUpTarget {
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
	private final Path workingDirectoryPath;

	public ModuleFinderImpl( String[] packagePaths ) {
		this.workingDirectoryPath = Paths.get( System.getProperty( "user.dir" ) );
		this.packagePaths = Arrays.stream( packagePaths )
			.map( Paths::get )
			.toArray( Path[]::new );
	}

	private ModuleSource findFileScheme( URI source, ImportPath importPath ) throws ModuleNotFoundException {
		Path parentPath =
			Files.isRegularFile( Paths.get( source ) ) ? Paths.get( source ).getParent()
				: Paths.get( source );

		try {
			if( importPath.isRelativeImport() ) {
				ModuleLookUpTarget target = this.resolveDotPrefix( importPath, parentPath );
				return this.moduleLookup( target.basePath, target.importPath );
			} else {
				return this.findAbsoluteImport( importPath, parentPath );
			}
		} catch( ModuleNotFoundException e ) {
			throw e;
		} catch( FileNotFoundException e ) {
			throw new ModuleNotFoundException( importPath, Paths.get( e.getMessage() ) );
		}
	}

	private ModuleSource findJapScheme( URI source, ImportPath importPath ) throws ModuleNotFoundException {

		try {
			if( importPath.isRelativeImport() ) {
				if( source.getScheme() != null && source.getScheme().equals( "jap" ) ) {
					JapSource jSource = new JapSource( source );
					String entryPath = jSource.name();
					Path entryDir = Paths.get( entryPath ).getParent() == null ? Paths.get( "/" )
						: Paths.get( entryPath ).getParent();
					var importPathEntry = entryDir.resolve( Paths.get( importPath.toRelativePathString() ) );
					return new JapSource(
						new URI( jSource.japURI().toString() + "!"
							+ UriUtils.normalizeWindowsPath( importPathEntry.toString() ) ) );
				} else {
					throw new ModuleNotFoundException( importPath, Paths.get( source ) );
				}
			} else {
				return this.findAbsoluteImport( importPath, Paths.get( source.getSchemeSpecificPart() ) );
			}
		} catch( ModuleNotFoundException e ) {
			throw e;
		} catch( URISyntaxException | IOException e ) {
			throw new ModuleNotFoundException( importPath, Paths.get( source.getSchemeSpecificPart() ) );
		}
	}

	@Override
	public ModuleSource find( URI source, ImportPath importPath ) throws ModuleNotFoundException {
		if( source.getScheme().equals( "jap" ) ) {
			return this.findJapScheme( source, importPath );
		} else {
			return this.findFileScheme( source, importPath );
		}
	}

	private ModuleSource findAbsoluteImport( ImportPath importPath, Path parentPath ) throws ModuleNotFoundException {
		/**
		 * Given WDIR = current working directory P = importing tokens FIRST = first token of import path
		 * REST = the rest of importing tokens
		 *
		 * 1. Check if FIRST.jap is in WDIR/lib. If so, resolve REST inside of it. 2. Try to resolve P from
		 * the packages directory, up one level, until system root 3. Try to resolve P from the list of
		 * packages directories passing through -p flag.
		 */

		List< Path > errPathList = new ArrayList<>();

		try {
			// 1. WDIR/lib/FIRST.jap with entry of REST.ol
			// where importPath[0] = FIRST
			// and importPath[1...] = REST
			Path japPath =
				ModuleFinder.japLookup( this.workingDirectoryPath.resolve( "lib" ),
					importPath.pathParts().get( 0 ) );
			List< String > rest = importPath.pathParts().subList( 1, importPath.pathParts().size() );
			if( !rest.isEmpty() ) {
				return new JapSource( japPath, rest );
			} else {
				return new JapSource( japPath.toUri() );
			}
		} catch( IOException e ) {
			errPathList.add( Paths.get( e.getMessage() ) );
		}
		try {
			// 2. ./lib/FIRST.jap with entry of REST.ol
			// where importPath[0] = FIRST
			// and importPath[1...] = REST
			Path japPath =
				ModuleFinder.japLookup( parentPath.resolve( "lib" ),
					importPath.pathParts().get( 0 ) );
			List< String > rest = importPath.pathParts().subList( 1, importPath.pathParts().size() );
			return new JapSource( japPath, rest );
		} catch( IOException e ) {
			errPathList.add( Paths.get( e.getMessage() ) );
		}
		try {
			// 1. ./lib/FIRST.jap with entry of REST.ol
			// where importPath[0] = FIRST
			// and importPath[1...] = REST
			Path japPath =
				ModuleFinder.japLookup( parentPath.resolve( "lib" ),
					importPath.pathParts().get( 0 ) );
			List< String > rest = importPath.pathParts().subList( 1, importPath.pathParts().size() );
			return new JapSource( japPath, rest );
		} catch( IOException e ) {
			errPathList.add( Paths.get( e.getMessage() ) );
		}

		// 3. Try to resolve P from the packages directory from self to parent, until system root is
		// reached.
		try {
			return this.moduleLookupFromPackages( parentPath, importPath );
		} catch( FileNotFoundException e ) {
			errPathList.addAll( Arrays.stream( e.getMessage().split( "," ) ).map( Paths::get )
				.collect( Collectors.toList() ) );
		}

		// 4. Try to resolve P from the list of packages directories.
		for( Path packagePath : this.packagePaths ) {
			try {
				ModuleSource moduleFile = this.moduleLookup( packagePath, importPath );
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
	 * @param basePath path to perform lookup
	 * @param importPath import target
	 *
	 * @return source object to be parsed by module parser.
	 */
	private ModuleSource moduleLookup( Path basePath, ImportPath importPath ) throws FileNotFoundException {
		List< String > packageParts;
		String moduleName;
		if( importPath.pathParts().isEmpty() ) {
			packageParts = new ArrayList<>();
			moduleName = "";
		} else {
			packageParts = importPath.pathParts().subList( 0, importPath.pathParts().size() - 1 );
			moduleName = importPath.pathParts().get( importPath.pathParts().size() - 1 );
		}
		for( String packageDir : packageParts ) {
			basePath = basePath.resolve( packageDir );
		}
		if( basePath.resolve( moduleName ).toFile().isDirectory() ) {
			basePath = basePath.resolve( moduleName );
			moduleName = DEFAULT_MODULE_NAME;
		}
		Path olTargetFile = ModuleFinder.olLookup( basePath, moduleName );
		return new PathSource( olTargetFile );
	}

	/**
	 * Perform a lookup for Jolie's executable source code (.ol file) at `packages` folder for each
	 * parent directory from basePath.
	 *
	 * @param basePath a path to perform lookup
	 *
	 * @return source object to be parsed by module parser.
	 */
	private ModuleSource moduleLookupFromPackages( Path basePath, ImportPath importPath )
		throws FileNotFoundException {

		Path targetPath = basePath;
		String[] lookupPaths = new String[ basePath.getNameCount() ];
		for( int i = 0; i < basePath.getNameCount(); i++ ) {
			boolean shouldPopTwice = false;
			if( !targetPath.getFileName().toString().equals( Constants.PACKAGE_DIR ) ) {
				targetPath = targetPath.resolve( Constants.PACKAGE_DIR );
				shouldPopTwice = true;
			}

			try {
				return this.moduleLookup( targetPath, importPath );
			} catch( FileNotFoundException e ) {
				lookupPaths[ i ] = targetPath.toString();
			}

			targetPath = shouldPopTwice ? targetPath.getParent().getParent() : targetPath.getParent();
		}

		throw new FileNotFoundException(
			Arrays.stream( lookupPaths ).filter( s -> s != null && s.length() != 0 )
				.collect( Collectors.joining( "," ) ) );
	}

	/**
	 * resolve path from source, each dot prefix means 1 level higher from the caller path directory
	 */
	private ModuleLookUpTarget resolveDotPrefix( ImportPath importPath, Path sourcePath ) {
		List< String > pathParts = importPath.pathParts();
		Path basePath = sourcePath;
		if( !sourcePath.toFile().isDirectory() ) {
			basePath = sourcePath.getParent();
		} else {
			basePath = sourcePath;
		}
		int packagesTokenStartIndex = 1;
		for( int i = 1; i < pathParts.size(); i++ ) {
			if( !pathParts.get( i ).isEmpty() ) {
				packagesTokenStartIndex = i;
				break;
			} else {
				basePath = basePath.getParent();
			}
		}
		// org.opentest4j.AssertionFailedError: Unexpected exception thrown:
		// jolie.lang.parse.module.exceptions.ModuleNotFoundException: Module "..service" not found. I
		// looked for modules in the following paths:
		// /home/nau/sdu/jolie/libjolie/src/test/resources/tmp/service.ol
		ImportPath resolvedImportPath =
			new ImportPath( pathParts.subList( packagesTokenStartIndex, pathParts.size() ) );
		return new ModuleLookUpTarget( basePath, resolvedImportPath );
	}
}
