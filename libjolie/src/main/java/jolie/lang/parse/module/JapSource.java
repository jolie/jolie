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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import jolie.lang.Constants;



class JapSource implements ModuleSource {

	private final JarFile japFile;
	private final URI japURI;
	private final URI uri;
	private final String filePath;
	private final Path parentPath;
	private final ZipEntry moduleEntry;

	/**
	 * Constructs a JapSource object with the specified URI.
	 *
	 * @param uri the URI of the JAP source
	 * @throws FileNotFoundException if the specified JAP file cannot be found
	 * @throws IllegalArgumentException if the URI scheme is not 'jap'
	 */
	public JapSource( URI uri ) throws FileNotFoundException {
		try {
			URI fileURI;
			if( uri.getScheme().equals( "jap" ) ) {
				fileURI = new URI( uri.getSchemeSpecificPart() );
			} else if( uri.getScheme().equals( "file" ) ) {
				fileURI = uri;
			} else {
				throw new IllegalArgumentException( "Passing uri is scheme is invalid, expected 'jap' or 'file'" );
			}
			if( fileURI.toString().contains( "!" ) ) {
				// uri contains entry point
				String japURIStrings[] = fileURI.toString().split( "!" );
				String moduleTarget =
					japURIStrings[ 1 ].endsWith( ".ol" ) ? japURIStrings[ 1 ] : japURIStrings[ 1 ] + ".ol";
				this.japFile = new JarFile( Paths.get( new URI( japURIStrings[ 0 ] ) ).toFile() );
				if( this.japFile.getEntry( japURIStrings[ 1 ] ) != null ) {
					this.filePath = moduleTarget;
					this.moduleEntry = this.japFile.getEntry( this.filePath );
				} else {
					if( moduleTarget.startsWith( "/" ) ) { // try with/without trailing slash
						this.filePath = moduleTarget.substring( 1 );
						this.moduleEntry = this.japFile.getEntry( this.filePath );
					} else {
						this.filePath = "/" + moduleTarget;
						this.moduleEntry = this.japFile.getEntry( this.filePath );
					}
				}
			} else {
				// lookup entrypoint from Manifest or guess by name
				this.japFile = new JarFile( Paths.get( fileURI ).toFile() );
				Manifest manifest = this.japFile.getManifest();
				if( manifest != null ) {
					// See if a main program is defined through a Manifest attribute
					Attributes attrs = manifest.getMainAttributes();
					if( attrs.containsKey( Constants.Manifest.MAIN_PROGRAM ) ) {
						this.filePath = attrs.getValue( Constants.Manifest.MAIN_PROGRAM );
					} else {
						this.filePath = Paths.get( this.japFile.getName() ).toUri().toString() + ".ol";
					}
				} else {
					// guess by name
					this.filePath = Paths.get( this.japFile.getName() ).toUri().toString() + ".ol";
				}
				this.moduleEntry = this.japFile.getEntry( this.filePath );
			}
			if( this.moduleEntry == null ) {
				throw new FileNotFoundException( uri.toString() );
			}
		} catch( IOException | URISyntaxException e ) {
			throw new FileNotFoundException( uri.toString() );
		}
		this.japURI = URI.create(
			"jap:" + Paths.get( this.japFile.getName() ).toUri().toString() );
		this.uri = URI.create(
			"jap:" + Paths.get( this.japFile.getName() ).toUri().toString() + "!/"
				+ this.moduleEntry.getName() );
		this.parentPath = Paths.get( this.filePath ).getParent();
	}

	/**
	 * Constructs a JapSource from the specified path and file path list. This constructor is use for
	 * lookup on lib directory
	 *
	 * @param path the base path of the .jap file
	 * @param importParts a list of strings representing the path within the .jap file
	 * @throws FileNotFoundException if the specified .jap file or entry does not exist
	 */
	public JapSource( Path path, List< String > importParts ) throws FileNotFoundException {
		if( !path.toFile().exists() || !path.toFile().getName().endsWith( "jap" ) ) {
			throw new FileNotFoundException( path.toString() );
		}
		try {
			this.japFile = new JarFile( path.toFile() );
			this.filePath = String.join( "/", importParts );
			moduleEntry = japFile.getEntry( this.filePath + ".ol" );
			if( moduleEntry == null ) {
				throw new FileNotFoundException(
					this.filePath + " in " + path.toString() );
			}
			this.parentPath = Paths.get( this.filePath ).getParent();
			this.japURI = URI.create(
				"jap:" + path.toUri().toString() );
			this.uri = URI.create(
				"jap:" + Paths.get( this.japFile.getName() ).toUri().toString() + "!/"
					+ this.moduleEntry.getName() );
		} catch( IOException e ) {
			throw new FileNotFoundException( path.toString() );
		}
	}

	/**
	 * additional includePath of JAP source is a parent path of the main execution file defined at main
	 * program
	 */
	@Override
	public Optional< URI > includePath() {
		return this.parentURI();
	}

	@Override
	public URI uri() {
		return this.uri;
	}

	@Override
	public InputStream openStream() throws IOException {
		return this.japFile.getInputStream( this.moduleEntry );
	}

	@Override
	public String name() {
		return this.moduleEntry.getName();
	}

	@Override
	public Optional< URI > parentURI() {
		return Optional
			.of( URI.create(
				"jap:" + Paths.get( this.japFile.getName() ).toUri().toString() + "!/"
					+ (this.parentPath != null ? this.parentPath.toString().replace( "\\", "/" ) : "") ) );
	}

	/**
	 * @return URI to jap file
	 */
	public URI japURI() {
		return this.japURI;
	}
}

