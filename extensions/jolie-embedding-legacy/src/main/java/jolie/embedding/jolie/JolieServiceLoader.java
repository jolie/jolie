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

import jolie.cli.CommandLineException;
import jolie.cli.CommandLineParser;
import jolie.Interpreter;
import jolie.runtime.Value;
import jolie.runtime.embedding.EmbeddedServiceLoader;
import jolie.runtime.embedding.EmbeddedServiceLoadingException;
import jolie.runtime.expression.Expression;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

public class JolieServiceLoader extends EmbeddedServiceLoader {
	private final static Pattern SERVICE_PATH_SPLIT_PATTERN = Pattern.compile( " " );
	private final static AtomicLong SERVICE_LOADER_COUNTER = new AtomicLong();
	private final Interpreter interpreter;

	public JolieServiceLoader( Expression channelDest, Interpreter currInterpreter, String servicePath,
		Optional< String > serviceName, Optional< Value > params )
		throws IOException, CommandLineException {
		super( channelDest );
		final String[] ss = SERVICE_PATH_SPLIT_PATTERN.split( servicePath );
		final String[] options = currInterpreter.optionArgs();

		final String[] newArgs = new String[ 2 + options.length + ss.length ];
		newArgs[ 0 ] = "-i";
		newArgs[ 1 ] = currInterpreter.programDirectory().getAbsolutePath();

		System.arraycopy( options, 0, newArgs, 2, options.length );
		System.arraycopy( ss, 0, newArgs, 2 + options.length, ss.length );
		CommandLineParser commandLineParser = new CommandLineParser( newArgs, currInterpreter.getClassLoader(), false );

		Interpreter.Configuration config = Interpreter.Configuration.create(
			commandLineParser.getInterpreterConfiguration().connectionsLimit(),
			commandLineParser.getInterpreterConfiguration().cellId(),
			commandLineParser.getInterpreterConfiguration().correlationAlgorithm(),
			commandLineParser.getInterpreterConfiguration().includePaths(),
			commandLineParser.getInterpreterConfiguration().optionArgs(),
			commandLineParser.getInterpreterConfiguration().libUrls(),
			commandLineParser.getInterpreterConfiguration().inputStream(),
			commandLineParser.getInterpreterConfiguration().charset(),
			commandLineParser.getInterpreterConfiguration().programFilepath(),
			commandLineParser.getInterpreterConfiguration().arguments(),
			commandLineParser.getInterpreterConfiguration().constants(),
			commandLineParser.getInterpreterConfiguration().jolieClassLoader(),
			commandLineParser.getInterpreterConfiguration().isProgramCompiled(),
			commandLineParser.getInterpreterConfiguration().typeCheck(),
			commandLineParser.getInterpreterConfiguration().tracer(),
			commandLineParser.getInterpreterConfiguration().tracerLevel(),
			commandLineParser.getInterpreterConfiguration().tracerMode(),
			commandLineParser.getInterpreterConfiguration().check(),
			commandLineParser.getInterpreterConfiguration().printStackTraces(),
			commandLineParser.getInterpreterConfiguration().responseTimeout(),
			commandLineParser.getInterpreterConfiguration().logLevel(),
			commandLineParser.getInterpreterConfiguration().programDirectory(),
			commandLineParser.getInterpreterConfiguration().packagePaths(),
			// difference:
			serviceName.orElse( commandLineParser.getInterpreterConfiguration().executionTarget() ),
			Optional.empty() );

		interpreter = new Interpreter(
			config,
			currInterpreter.programDirectory(),
			params );
	}

	public JolieServiceLoader( String code, Expression channelDest, Interpreter currInterpreter )
		throws IOException {
		super( channelDest );
		Interpreter.Configuration configuration =
			Interpreter.Configuration.create( currInterpreter.configuration(), new File( "#native_code_" +
				SERVICE_LOADER_COUNTER.getAndIncrement() ), new ByteArrayInputStream( code.getBytes() ) );
		interpreter = new Interpreter( configuration, currInterpreter.programDirectory(),
			Optional.empty() );
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
}
