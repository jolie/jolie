/*
 * Copyright (C) 2018 Fabrizio Montesi <famontesi@gmail.com>
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
package joliex.formatter;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import jolie.lang.parse.OLParseTreeOptimizer;
import jolie.lang.parse.OLParser;
import jolie.lang.parse.ParserException;
import jolie.lang.parse.Scanner;
import jolie.lang.parse.ast.Program;

/**
 * The main class (entry point) of the jofmt tool.
 *
 * @author Fabrizio Montesi
 */
public class JolieFormatter
{
	final static Logger LOGGER = Logger.getLogger( "jofmt" );
	
	private static void printHelp( PrintStream outStream )
	{
		outStream.println( "Usage: jofmt <file>" );
	}
	
	public static void main( String[] args )
	{
		if ( args.length != 1  || "--help".equals( args[0] ) ) {
			printHelp( System.err );
			return;
		}
		
		try {
			Path sourceFile = Paths.get( args[0] );
			if ( !Files.isRegularFile( sourceFile ) ) {
				throw new IOException( sourceFile.toString() + " is not a regular file" );
			}
			
			final Program program;
			try( InputStream sourceStream = new BufferedInputStream( Files.newInputStream( sourceFile ) ) ) {
				OLParser olParser = new OLParser(
					new Scanner( sourceStream, sourceFile.toUri(), null ),
					new String[0],
					JolieFormatter.class.getClassLoader()
				);
				program = OLParseTreeOptimizer.optimize( olParser.parse() );
			}
			try( Writer sourceWriter = Files.newBufferedWriter( sourceFile ) ) {
				PrettyPrinter printer = new PrettyPrinter( sourceWriter );
				Formatter.format( program, printer );
				printer.flush();
			}
		} catch( IOException | ParserException e ) {
			LOGGER.log( Level.SEVERE, "an exception was thrown", e );
		} catch( FormattingException e ) {
			LOGGER.severe( e.getErrorMessages() );
		}
	}
}
