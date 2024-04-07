/***************************************************************************
 *   Copyright (C) 2008 by Fabrizio Montesi                                *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU Library General Public License as       *
 *   published by the Free Software Foundation; either version 2 of the    *
 *   License, or (at your option) any later version.                       *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU Library General Public     *
 *   License along with this program; if not, write to the                 *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 *                                                                         *
 *   For details about the authors of this software, see the AUTHORS file. *
 ***************************************************************************/

package joliex.metaservice.impl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import jolie.cli.CommandLineException;
import jolie.cli.CommandLineParser;
import jolie.Interpreter;
import jolie.net.CommChannel;
import joliex.metaservice.MetaService;
import joliex.metaservice.MetaServiceChannel;

/**
 * This class executes a MetaService service in the local Java Virtual Machine.
 * 
 * @author Fabrizio Montesi
 */
public class EmbeddedMetaService extends MetaService {
	private final Interpreter interpreter;
	private final MetaServiceChannel channel;
	private static final String FS = jolie.lang.Constants.FILE_SEPARATOR;
	private static final String PS = jolie.lang.Constants.PATH_SEPARATOR;

	private static final String JOLIE_HOME_ENV = "JOLIE_HOME";

	private static final String DEFAULT_FILEPATH =
		FS + "include" + FS + "services" + FS + "metaservice" + FS + "metaservice.ol";

	private static String[] buildInterpreterArguments( String jh, String metaServiceFilepath ) {
		// jh stays for jolie_home, i.e. the JOLIE installation directory
		return new String[] {
			// Location
			"-C", "MetaServiceLocation=\"local\"",
			// Libraries
			"-l", jh + FS + "lib" + PS + jh + FS + "javaServices" + FS + "*" + PS + jh + FS + "extensions" + FS + "*",
			// Includes
			"-i", jh + FS + "include",
			// MetaService source file
			metaServiceFilepath
		};
	}

	/**
	 * Creates an embedded MetaService instance, executing a JOLIE interpreter in the local JVM.
	 */
	public EmbeddedMetaService()
		throws IOException, ExecutionException {
		this( System.getenv( JOLIE_HOME_ENV ) );
	}

	/**
	 * Creates an embedded MetaService instance, executing a JOLIE interpreter in the local JVM.
	 * 
	 * @param jolieHome the path pointing to the local JOLIE installation directory.
	 */
	public EmbeddedMetaService( String jolieHome )
		throws IOException, ExecutionException {
		this( jolieHome, jolieHome + DEFAULT_FILEPATH );
	}

	private void startInterpreter()
		throws ExecutionException {
		Future< Exception > f = interpreter.start();
		try {
			Exception e = f.get();
			if( e != null ) {
				throw new ExecutionException( e );
			}
		} catch( InterruptedException e ) {
			throw new ExecutionException( e );
		}
	}

	/**
	 * Creates an embedded MetaService instance, executing a JOLIE interpreter in the local JVM.
	 * 
	 * @param jolieHome the path pointing to the local JOLIE installation directory.
	 * @param metaserviceFilepath the path pointing to the metaservice source file to load.
	 */
	public EmbeddedMetaService( String jolieHome, String metaserviceFilepath )
		throws IOException, ExecutionException {
		try {
			CommandLineParser commandLineParser = new CommandLineParser(
				buildInterpreterArguments( jolieHome, metaserviceFilepath ), this.getClass().getClassLoader(), false );
			interpreter =
				new Interpreter( commandLineParser.getInterpreterConfiguration(), null, Optional.empty(),
					Optional.empty() );
			startInterpreter();
			channel = new MetaServiceChannel( this, "/" );
		} catch( CommandLineException | FileNotFoundException e ) {
			throw new IOException( e );
		}
	}

	@Override
	protected CommChannel createCommChannel() {
		return interpreter.commCore().getLocalCommChannel();
	}

	@Override
	public MetaServiceChannel getChannel() {
		return channel;
	}
}
