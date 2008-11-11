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

package jolie.compiler;

import jolie.*;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import jolie.lang.parse.OLParseTreeOptimizer;
import jolie.lang.parse.OLParser;
import jolie.lang.parse.ParserException;
import jolie.lang.parse.Scanner;
import jolie.lang.parse.ast.Program;

/**
 *
 * @author fmontesi
 */
public class Compiler
{
	final private InputStream programStream;
	final private String programFilepath;
	final private String[] includePaths;
	final private ClassLoader classLoader;
	
	public Compiler( String[] args )
		throws CommandLineException, IOException
	{
		CommandLineParser cmdParser = new CommandLineParser( args );
		classLoader = this.getClass().getClassLoader();
		programStream = cmdParser.programStream();
		programFilepath = cmdParser.programFilepath();
		includePaths = cmdParser.includePaths();
	}
	
	public void compile( OutputStream ostream )
		throws IOException, ParserException
	{
		OLParser parser = new OLParser( new Scanner( programStream, programFilepath ), includePaths, classLoader );
		Program program = parser.parse();
		OLParseTreeOptimizer optimizer = new OLParseTreeOptimizer( program );
		program = optimizer.optimize();
		ObjectOutputStream oos = new ObjectOutputStream( ostream );
		oos.writeObject( program );
	}
	
	public void compile()
		throws IOException, ParserException
	{
		compile( new FileOutputStream( programFilepath + "c" ) );
	}
}
