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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import jolie.lang.Constants;
import jolie.lang.parse.module.exceptions.ModuleNotFoundException;

public class FinderImpl implements Finder {

	private class ModuleLookUpTarget {
		private final Path basePath;
		private final List< String > pathParts;

		private ModuleLookUpTarget( Path basePath, List< String > pathParts ) {
			this.basePath = basePath;
			this.pathParts = pathParts;
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

	public FinderImpl( String[] packagePaths ) {
		this( Paths.get( "" ), packagePaths );
	}

	public FinderImpl( Path workingDirectory, String[] packagePaths ) {
		this.workingDirectory = workingDirectory;
		this.packagePaths = Arrays.stream( packagePaths )
			.map( Paths::get )
			.toArray( Path[]::new );
	}

	public Source find( List< String > pathParts, URI source ) throws ModuleNotFoundException {
		try {
			if( Finder.isRelativeImport( pathParts ) ) {
				Path sourcePath = Paths.get( source );
				ModuleLookUpTarget target = this.resolveDotPrefix( pathParts, sourcePath );
				return this.moduleLookup( target );
			} else {
				return this.findAbsoluteImport( pathParts );
			}
		} catch( FileNotFoundException e ) {
			throw new ModuleNotFoundException( pathParts.toString(), e.getMessage() );
		}
	}

	private Source findAbsoluteImport( List< String > pathParts ) throws ModuleNotFoundException {
		/**
		 * 1. Try to resolve P directly from WDIR. 2. Check if FIRST.jap is in WDIR/lib. If so, resolve REST
		 * inside of this jap. 3. Try to resolve P from the list of packages directories.
		 */

		List< String > errMessageList = new ArrayList<>();
		try {
			// 1. resolve from Working directory
			Source moduleFile = this.moduleLookup( this.workingDirectory, pathParts );
			return moduleFile;
		} catch( FileNotFoundException e ) {
			errMessageList.add( e.getMessage() );
		}

		try {
			// 2. WDIR/lib/FIRST.jap with entry of REST.ol
			// where pathParts[0] = FIRST
			// and pathParts[1...] = REST
			File japFile = Finder.japLookup( this.workingDirectory.resolve( "lib" ), pathParts.get( 0 ) );
			List< String > rest = pathParts.subList( 1, pathParts.size() );
			return new JapSource( japFile, rest );
		} catch( IOException e ) {
			errMessageList.add( e.getMessage() );
		}

		try {
			// 3. Try to resolve P from the list of packages directories.
			for( Path packagePath : this.packagePaths ) {
				Source moduleFile = moduleLookup( packagePath, pathParts );
				return moduleFile;
			}
		} catch( FileNotFoundException e ) {
			errMessageList.add( e.getMessage() );
		}

		throw new ModuleNotFoundException( pathParts.toString(), errMessageList );
	}

	private Source moduleLookup( ModuleLookUpTarget target ) throws FileNotFoundException {
		return moduleLookup( target.basePath, target.pathParts );
	}

	/**
	 * Perform a lookup for Jolie's executable source code (.ol file)
	 * 
	 * @param basePath a path to perform lookup
	 * @param pathParts a dot separated string represent a target module eg. package.module forms
	 *        ['package', 'module']
	 * 
	 * @return source object to be parsed by module parser.
	 */
	private Source moduleLookup( Path basePath, List< String > pathParts ) throws FileNotFoundException {
		List< String > packageParts = pathParts.subList( 0, pathParts.size() - 1 );
		String moduleName = pathParts.get( pathParts.size() - 1 );
		for( String packageDir : packageParts ) {
			basePath = basePath.resolve( packageDir );
		}
		File olTargetFile = Finder.olLookup( basePath, moduleName );
		return new FileSource( olTargetFile );
	}

	/**
	 * returns package paths this finder is considering
	 * 
	 * @return packagePaths
	 */
	public Path[] packagePaths() {
		return packagePaths;
	}

	/**
	 * resolve path from source, each dot prefix means 1 level higher from the caller path directory
	 */
	private ModuleLookUpTarget resolveDotPrefix( List< String > pathParts, Path sourcePath ) {
		Path basePath;
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
		List< String > moduleTargetPart = pathParts.subList( packagesTokenStartIndex, pathParts.size() );
		ModuleLookUpTarget result =
			new ModuleLookUpTarget( basePath, moduleTargetPart );
		return result;
	}
}
