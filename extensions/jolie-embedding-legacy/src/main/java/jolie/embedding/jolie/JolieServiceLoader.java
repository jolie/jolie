/***************************************************************************
 *   Copyright (C) by Fabrizio Montesi                                     *
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

package jolie.embedding.jolie;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import jolie.Interpreter;
import jolie.cli.CommandLineException;
import jolie.cli.CommandLineParser;
import jolie.lang.parse.ast.ServiceNode;
import jolie.lang.parse.module.ModuleSource;
import jolie.runtime.Value;
import jolie.runtime.embedding.EmbeddedServiceLoader;
import jolie.runtime.embedding.EmbeddedServiceLoadingException;
import jolie.runtime.expression.Expression;

public class JolieServiceLoader extends EmbeddedServiceLoader {
	private final static AtomicLong SERVICE_LOADER_COUNTER = new AtomicLong();
	private final Interpreter interpreter;

	/**
	 * Creates a new instance of ServiceLoader by file path.
	 *
	 * @param channelDest the channel destination to use to communicate with the service loaded by this
	 *        loader.
	 * @param currInterpreter the Jolie interpreter to use to load the service.
	 * @param servicePath the path to the service to load. This path can be a Jolie service file or a
	 *        directory containing a Jolie service file. If it is a directory, the service file must be
	 *        named "service.ol".
	 * @throws IOException if an IO error occurs while loading the service.
	 * @throws CommandLineException if a command line error occurs while loading the service.
	 */
	public JolieServiceLoader( Expression channelDest, Interpreter currInterpreter, String servicePath,
		Optional< String > serviceName, Optional< Value > params )
		throws IOException, CommandLineException {
		super( channelDest );
		final String[] ss = new String[] { servicePath };
		final String[] options = currInterpreter.optionArgs();
		final String[] newArgs = new String[ 2 + options.length + ss.length ];
		newArgs[ 0 ] = "-i";
		newArgs[ 1 ] = currInterpreter.programDirectory().getAbsolutePath();

		System.arraycopy( options, 0, newArgs, 2, options.length );
		System.arraycopy( ss, 0, newArgs, 2 + options.length, ss.length );
		try( CommandLineParser commandLineParser = new CommandLineParser( newArgs,
			currInterpreter.getClassLoader(), false ) ) {
			Interpreter.Configuration cmdConfig = commandLineParser.getInterpreterConfiguration();
			Interpreter.Configuration config = Interpreter.Configuration
				.create(
					cmdConfig,
					serviceName.orElse( cmdConfig.executionTarget() ) );

			interpreter = new Interpreter(
				config,
				params,
				Optional.of( currInterpreter.logPrefix() ) );
		}
	}

	/**
	 * Creates a new instance of ServiceLoader by Jolie code.
	 *
	 * @param code the Jolie code of the service to load.
	 * @param channelDest the channel destination to use to communicate with the service loaded by this
	 *        loader.
	 * @param currInterpreter the Jolie interpreter to use to load the service.
	 * @throws IOException if an IO error occurs while loading the service.
	 */
	public JolieServiceLoader( String code, Expression channelDest, Interpreter currInterpreter )
		throws IOException {
		super( channelDest );

		ModuleSource source;
		try {
			source = ModuleSource.create(
				new File( "#native_code_" + SERVICE_LOADER_COUNTER.getAndIncrement() ).toURI(),
				new ByteArrayInputStream( code.getBytes() ), ServiceNode.DEFAULT_MAIN_SERVICE_NAME );

			Interpreter.Configuration configuration = Interpreter.Configuration.create(
				currInterpreter.configuration(), source );

			interpreter = new Interpreter( configuration,
				Optional.empty(), Optional.of( currInterpreter.logPrefix() ) );
		} catch( FileNotFoundException e ) {
			throw new IOException(
				new IllegalStateException( "URI creation should not fail. error: " + e.getMessage() ) );
		}

	}

	@Override
	public void load()
		throws EmbeddedServiceLoadingException {
		Future< Exception > f = interpreter.start();
		try {
			Exception e = f.get();
			if( e == null ) {
				setChannel( interpreter.commCore().getLocalCommChannel() );
			} else {
				throw new EmbeddedServiceLoadingException( e );
			}
		} catch( InterruptedException | ExecutionException | EmbeddedServiceLoadingException e ) {
			throw new EmbeddedServiceLoadingException( e );
		}
	}

	public Interpreter interpreter() {
		return interpreter;
	}

	public void exit() {
		interpreter.exit();
	}
}
