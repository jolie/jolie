/***************************************************************************
 *   Copyright (C) by Fabrizio Montesi <famontesi@gmail.com>               *
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
 ***************************************************************************/


/**
 * \mainpage JOLIE: a Java Orchestration Language Interpreter Engine
 * \image html transparentEyes.gif
 * \section sec_intro Introduction
 * This is the reference documentation for JOLIE: a Java Orchestration Language Interpreter Engine.
 */

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import jolie.*;

/** Stater class of the interpreter.
 * Analyzes the command line in search for the input source file and the optional port option.
 *
 */
public class Jolie
{
	public static void main( String[] args )
	{
		try {
			Interpreter interpreter = null;
			if ( args.length > 0 ) {
				InputStream stream = new FileInputStream( args[ 0 ] );
				if ( args.length == 1 )
					interpreter = new Interpreter( stream, args[ 0 ] );
				else if ( args.length == 2 )
					interpreter = new Interpreter( stream, args[ 0 ], Integer.parseInt(args[ 1 ]) );
			}
			if ( interpreter != null )
				interpreter.run();
			else
				System.out.println( "\nUsage: java Jolie <input file> [<port>]" );
		} catch ( IOException e ) {
			e.printStackTrace();
		} catch ( ParserException pe ) {
			pe.printStackTrace();
		} catch ( InterpreterException ie ) {
			ie.printStackTrace();
		}
	}
}