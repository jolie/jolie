/***************************************************************************
 *   Copyright (C) 2008-2010 by Fabrizio Montesi                           *
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

package jolie.compiler;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import jolie.CommandLineException;
import jolie.CommandLineParser;
import jolie.lang.parse.OLParseTreeOptimizer;
import jolie.lang.parse.OLParser;
import jolie.lang.parse.ParserException;
import jolie.lang.parse.Scanner;
import jolie.lang.parse.SemanticVerifier;
import jolie.lang.parse.ast.Program;

/**
 *
 * @author Fabrizio Montesi
 */
public class Compiler
{
	private final ClassLoader classLoader;
	private final CommandLineParser cmdParser;
	
	public Compiler( String[] args )
		throws CommandLineException, IOException
	{
		cmdParser = new CommandLineParser( args, this.getClass().getClassLoader() );
		classLoader = this.getClass().getClassLoader();
	}
	
	public void compile( OutputStream ostream )
		throws IOException, ParserException
	{
		OLParser parser = new OLParser( new Scanner( cmdParser.programStream(), cmdParser.programFilepath() ), cmdParser.includePaths(), classLoader );
		parser.putConstants( cmdParser.definedConstants() );
		Program program = parser.parse();

		// TODO: Use the SemanticVerifier
		OLParseTreeOptimizer optimizer = new OLParseTreeOptimizer( program );
		program = optimizer.optimize();
		SemanticVerifier semanticVerifier = new SemanticVerifier( program );
		if ( !semanticVerifier.validate() ) {
			throw new IOException( "Exiting" );
		}
		//GZIPOutputStream gzipstream = new GZIPOutputStream( ostream );
		ObjectOutputStream oos = new ObjectOutputStream( ostream );
		oos.writeObject( program );
		ostream.flush();
		ostream.close();
		//gzipstream.close();
	}
	
	public void compile()
		throws IOException, ParserException
	{
		compile( new FileOutputStream( cmdParser.programFilepath() + "c" ) );
	}
}
