/*
 * Copyright (C) 2015 Martin Wolf <mw@martinwolf.eu>.
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
package jolie.embedding.jolie;

import jolie.cli.CommandLineException;
import jolie.cli.CommandLineParser;
import jolie.Interpreter;
import jolie.lang.parse.ast.Program;
import jolie.runtime.embedding.EmbeddedServiceLoader;
import jolie.runtime.embedding.EmbeddedServiceLoadingException;
import jolie.runtime.expression.Expression;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


public class InternalJolieServiceLoader extends EmbeddedServiceLoader {
	private final Interpreter interpreter;

	public InternalJolieServiceLoader( Expression channelDest, Interpreter currInterpreter, String serviceName,
		Program program )
		throws IOException, CommandLineException {
		super( channelDest );

		List< String > newArgs = new ArrayList<>();
		newArgs.add( "-i" );
		newArgs.add( currInterpreter.programDirectory().getAbsolutePath() );

		String[] options = currInterpreter.optionArgs();
		newArgs.addAll( Arrays.asList( options ) );
		newArgs.add( "#" + serviceName + ".ol" );
		CommandLineParser commandLineParser =
			new CommandLineParser( newArgs.toArray( new String[] {} ), currInterpreter.getClassLoader(), true );
		interpreter = new Interpreter(
			commandLineParser.getInterpreterConfiguration(),
			currInterpreter,
			program );
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
