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

import jolie.lang.Constants;

public class ModuleNotFoundException extends FileNotFoundException {
	private static final long serialVersionUID = Constants.serialVersionUID();

	private final List< String > lookedPaths;
	private final String moduleName;

	public ModuleNotFoundException( String moduleName, List< String > lookedPaths ) {
		super( moduleName );
		this.moduleName = moduleName;
		this.lookedPaths = lookedPaths;
	}

	public ModuleNotFoundException( String moduleName, String lookedPath ) {
		super( moduleName );
		this.moduleName = moduleName;
		this.lookedPaths = new ArrayList<>();
		this.lookedPaths.add( lookedPath );
	}

	@Override
	public String getMessage() {
		StringBuilder message =
			new StringBuilder( "Module \"" ).append( this.moduleName )
				.append( "\" not found from lookup paths: (this line is printed twice for some reason)\n" );
		Set< String > fileNames = new HashSet<>();
		String[] moduleNameParts = (this.moduleName.substring( 1, this.moduleName.length() - 1 )).split( ", " );
		for( String ms : this.lookedPaths ) {
			message.append( ms ).append( "\n" );
			try {
				if( ms.startsWith( moduleNameParts[ 0 ] ) ) {
					Stream< Path > stream;
					stream = Files.list( Paths.get( ".\\" ) );
					fileNames
						.addAll( stream.map( Path::getFileName ).map( Path::toString ).collect( Collectors.toSet() ) );
					stream.close();
					String trailing = "";
					for( String parts : moduleNameParts ) {
						Stream< Path > forloopStream;
						if( !moduleNameParts[ moduleNameParts.length - 1 ].equals( parts ) ) {
							trailing += parts + "\\";
							forloopStream = Files.list( Paths.get( ".\\" + trailing ) );
							fileNames.addAll( forloopStream.map( Path::getFileName ).map( Path::toString )
								.collect( Collectors.toSet() ) );
							forloopStream.close();
						}

					}
				} else if( ms.startsWith( ".\\" ) ) {
					String trailing = "";
					String[] pathParts = ms.split( "\\\\" );
					String lastPart = pathParts[ pathParts.length - 1 ];
					for( String parts : pathParts ) {
						Stream< Path > forloopStream;
						if( !lastPart.equals( parts ) ) {
							trailing += parts + "\\";
							forloopStream = Files.list( Paths.get( trailing ) );
							fileNames.addAll( forloopStream.map( Path::getFileName ).map( Path::toString )
								.collect( Collectors.toSet() ) );
							forloopStream.close();
						}

					}
				} else if( ms.startsWith( System.getenv( "JOLIE_HOME" ) ) ) {
					Stream< Path > stream;
					String path = System.getenv( "JOLIE_HOME" ) + "\\packages";
					stream = Files.list( Paths.get( path ) );
					fileNames
						.addAll( stream.map( Path::getFileName ).map( Path::toString ).collect( Collectors.toSet() ) );
					stream.close();
					String trailing = path + "\\";
					for( String parts : moduleNameParts ) {
						Stream< Path > forloopStream;
						if( !moduleNameParts[ moduleNameParts.length - 1 ].equals( parts ) ) {
							trailing += parts + "\\";
							forloopStream = Files.list( Paths.get( trailing ) );
							fileNames.addAll( forloopStream.map( Path::getFileName ).map( Path::toString )
								.collect( Collectors.toSet() ) );
							forloopStream.close();
						}

					}
				} else if( ms.startsWith( System.getProperty( "user.dir" ) + "\\lib" ) ) {
					Stream< Path > stream;
					int index = ms.indexOf( moduleNameParts[ 0 ] + ".jap" );
					String path = ms.substring( 0, index );
					stream = Files.list( Paths.get( path ) );
					fileNames
						.addAll( stream.map( Path::getFileName ).map( Path::toString ).collect( Collectors.toSet() ) );
					stream.close();
				}
			} catch( IOException e ) {
			}
		}
		message.append( "maybe you meant: (this is also printed twice for some reason)\n" );
		for( String p : fileNames ) {
			message.append( p ).append( "\n" );
		}
		return message.toString();
	}

}
