/***************************************************************************
 *   Copyright (C) 2009 by Fabrizio Montesi                                *
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

package joliex.plasma;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.logging.Logger;
import jolie.CommandLineException;
import jolie.CommandLineParser;
import jolie.lang.parse.ParserException;
import jolie.lang.parse.SemanticException;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.util.ParsingUtils;
import joliex.plasma.impl.InterfaceVisitor;

/**
 *
 * @author Fabrizio Montesi
 */
public class Jolie2Plasma {
	public static void main( String[] args ) {
		try {
			CommandLineParser cmdParser = new CommandLineParser( args, Jolie2Plasma.class.getClassLoader() );
			final String[] arguments = cmdParser.arguments();
			if( arguments.length < 2 ) {
				throw new CommandLineException( "Insufficient number of arguments" );
			}

			try( Writer writer = new BufferedWriter( new FileWriter( arguments[ 0 ] ) ) ) {
				Program program = ParsingUtils.parseProgram(
					cmdParser.programStream(),
					cmdParser.programFilepath().toURI(), cmdParser.charset(),
					cmdParser.includePaths(), cmdParser.jolieClassLoader(), cmdParser.definedConstants(), false );
				new InterfaceConverter(
					program,
					Arrays.copyOfRange( arguments, 1, arguments.length ),
					Logger.getLogger( "jolie2plasma" ) ).convert( writer );
			}
		} catch( CommandLineException e ) {
			System.out.println( e.getMessage() );
			System.out.println(
				"Syntax is: jolie2plasma [jolie options] <jolie filename> <output filename> [interface name list]" );
		} catch( IOException e ) {
			e.printStackTrace();
		} catch( ParserException e ) {
			e.printStackTrace();
		} catch( SemanticException e ) {
			e.printStackTrace();
		} catch( InterfaceVisitor.InterfaceNotFound e ) {
			e.printStackTrace();
		}
	}
}
