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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import jolie.lang.Constants;

/**
 * an Interface of Joile module Source
 */
public interface Source {

	/**
	 * @return URI location of the module
	 */
	URI source();

	/**
	 * @return an optional include path for parsing this module
	 */
	Optional< String > includePath();

	/**
	 * @return an InputStream of source
	 */
	Optional< InputStream > stream();
}


/**
 * Jolie Module in file (an .ol file)
 */
class FileSource implements Source {

	private final File file;

	public FileSource( File f ) {
		this.file = f;
	}

	@Override
	public URI source() {
		return this.file.toURI();
	}

	/**
	 * the include path of ol file should be empty
	 */
	@Override
	public Optional< String > includePath() {
		return Optional.empty();
	}

	@Override
	public Optional< InputStream > stream() {
		try {
			return Optional.of( new FileInputStream( this.file ) );
		} catch( FileNotFoundException e ) {
			return Optional.empty();
		}
	}
}


class JapSource implements Source {

	private final JarFile japFile;
	private final URI source;
	private final String filePath;
	private final String parentPath;
	private final ZipEntry moduleEntry;

	public JapSource( File f ) throws IOException {
		this.japFile = new JarFile( f );
		this.source = f.toURI();
		Manifest manifest = this.japFile.getManifest();

		if( manifest != null ) { // See if a main program is defined through a Manifest attribute
			Attributes attrs = manifest.getMainAttributes();
			this.filePath = attrs.getValue( Constants.Manifest.MAIN_PROGRAM );
			this.parentPath = Paths.get( this.filePath ).getParent().toString();
			moduleEntry = japFile.getEntry( this.filePath );
			if( moduleEntry == null ) {
				throw new IOException();
			}
		} else {
			throw new IOException();
		}
	}

	public JapSource( File f, String path ) throws IOException {
		this.japFile = new JarFile( f );
		this.source = f.toURI();
		this.filePath = path;
		moduleEntry = japFile.getEntry( this.filePath + ".ol" );
		if( moduleEntry == null ) {
			throw new FileNotFoundException(
				this.filePath + " in " + f.toString() );
		}
		this.parentPath = Paths.get( this.filePath ).getParent().toString();
	}

	/**
	 * additional includePath of JAP source is a parent path of the main execution file defined at main
	 * program
	 */
	@Override
	public Optional< String > includePath() {
		return Optional.of( "jap:" + this.source.toString() + "!/" + this.parentPath );
	}

	@Override
	public URI source() {
		return this.source;
	}

	@Override
	public Optional< InputStream > stream() {
		try {
			return Optional.of( this.japFile.getInputStream( this.moduleEntry ) );
		} catch( IOException e ) {
			return Optional.empty();
		}
	}
}
