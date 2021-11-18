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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.commons.text.similarity.LevenshteinDistance;

import jolie.lang.Constants;
import jolie.lang.parse.context.URIParsingContext;
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

	public String getHelp( URIParsingContext context ) {
		StringBuilder message = new StringBuilder();
		Set< String > fileNames = new HashSet<>();
		Stream< Path > stream;
		try {
			stream = Files.list( Paths.get( ".\\" ) );
			fileNames.addAll( stream.filter( file -> !Files.isDirectory( file ) ).map( Path::getFileName )
				.map( Path::toString ).collect( Collectors.toSet() ) );
			stream.close();
		} catch( IOException e ) {
		}
		for( Path path : this.lookedPaths ) {
			Stream< Path > forloopStream;
			try {
				Path currentPath = path;
				while( !Files.isDirectory( currentPath ) ) {
					currentPath = currentPath.getParent();
				}
				forloopStream = Files.list( currentPath );
				fileNames.addAll( forloopStream.filter( file -> !Files.isDirectory( file ) ).map( Path::getFileName )
					.map( Path::toString ).collect( Collectors.toSet() ) );
				forloopStream.close();
			} catch( IOException e ) {
			}
		}

		LevenshteinDistance dist = new LevenshteinDistance();
		ArrayList< String > proposedModules = new ArrayList<>();
		for( String correctModule : fileNames ) {
			String moduleName;
			if( correctModule.contains( "." ) ) {
				int column = correctModule.indexOf( "." );
				moduleName = correctModule.substring( 0, column );
			} else {
				moduleName = correctModule;
			}
			if( dist.apply( this.importPath.toString(), moduleName ) <= 2 ) {
				proposedModules.add( moduleName );
			}

		}
		if( !proposedModules.isEmpty() ) {
			message.append( "Maybe you meant:\n" );
			for( String module : proposedModules ) {
				String temp = module.substring( 0, 1 ).toUpperCase() + module.substring( 1 );
				message.append( temp ).append( "\n" );
			}
		} else {
			message.append( "Could not find modules mathing \"" ).append( this.importPath )
				.append( "\". Here are some modules that can be imported:\n" );
			for( String module : fileNames ) {
				String temp;
				if( module.contains( "." ) ) {
					int column = module.indexOf( "." );
					temp = module.substring( 0, 1 ).toUpperCase() + module.substring( 1, column );
				} else {
					temp = module.substring( 0, 1 ).toUpperCase() + module.substring( 1 );
				}
				message.append( temp ).append( "\n" );
			}
		}
		return message.toString();
	}

	@Override
	public String getMessage() {
		StringBuilder message =
			new StringBuilder().append( "Module " ).append( '\"' ).append( this.importPath )
				.append( "\" not found from lookup paths.\n" );
		return message.toString();
	}

}
