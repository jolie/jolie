/*********************************************************************************
 *   Copyright (C) 2006-2014 by Fabrizio Montesi <famontesi@gmail.com>     *
 *                                                                               *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU Library General Public License as       *
 *   published by the Free Software Foundation; either version 2 of the    *
 *   License, or (at your option) any later version.                         *
 *                                                                               *
 *   This program is distributed in the hope that it will be useful,             *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of              *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the               *
 *   GNU General Public License for more details.                                *
 *                                                                               *
 *   You should have received a copy of the GNU Library General Public           *
 *   License along with this program; if not, write to the                       *
 *   Free Software Foundation, Inc.,                                             *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.                   *
 *                                                                               *
 *   For details about the authors of this software, see the AUTHORS file.       *
 *********************************************************************************/

package jolie;

import java.io.FileNotFoundException;
import java.io.IOException;
import jolie.lang.parse.ParserException;


/** Starter class of the Interpreter.
 * @author Fabrizio Montesi
 */
public class Jolie
{
	private static final long TERMINATION_TIMEOUT = 100; // 0.1 seconds
	
	static {
		JolieURLStreamHandlerFactory.registerInVM();
	}
	
	private Jolie() {}
	
	private static void printErr( Throwable t )
	{
		System.err.println( t.getMessage() );
	}

	/** 
	 * Entry point of program execution.
	 * @param args the command line arguments
	 * TODO Standardize the exit codes.
	 */
	public static void main( String[] args )
	{
		int exitCode = 0;
		try {
			final Interpreter interpreter = new Interpreter( args, Jolie.class.getClassLoader(), null );
			Thread.currentThread().setContextClassLoader( interpreter.getClassLoader() );
			Runtime.getRuntime().addShutdownHook( new Thread() {
				@Override
				public void run()
				{
					interpreter.exit( TERMINATION_TIMEOUT );
				}
			} );
			interpreter.run();
		} catch( CommandLineException cle ) {
			printErr( cle );
		} catch( FileNotFoundException fe ) {
			printErr( fe );
			exitCode = 1;
		} catch( IOException ioe ) {
			printErr( ioe );
			exitCode = 2;
		} catch( InterpreterException ie ) {
			if ( ie.getCause() instanceof ParserException ) {
				printErr( ie.getCause() );
			} else {
				printErr( ie );
			}
			exitCode = 3;
		} catch( Exception e ) {
			printErr( e );
			exitCode = 4;
		}
		System.exit( exitCode );
	}
}
