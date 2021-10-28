/***************************************************************************
 *   Copyright (C) 2008-2014 by Fabrizio Montesi                           *
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
import jolie.cli.CommandLineException;
import jolie.cli.CommandLineParser;
import jolie.JolieURLStreamHandlerFactory;
import jolie.lang.CodeCheckException;
import jolie.lang.parse.ParserException;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.module.ModuleException;
import jolie.lang.parse.util.ParsingUtils;

/**
 *
 * @author Fabrizio Montesi
 */
public class Compiler {
	private final CommandLineParser cmdParser;

	static {
		JolieURLStreamHandlerFactory.registerInVM();
	}

	public Compiler( String[] args )
		throws CommandLineException, IOException {
		cmdParser = new CommandLineParser( args, Compiler.class.getClassLoader() );
	}

	public void compile( OutputStream ostream )
		throws IOException, ParserException, CodeCheckException, CommandLineException, ModuleException {
		Program program = ParsingUtils.parseProgram(
			cmdParser.getInterpreterConfiguration().inputStream(),
			cmdParser.getInterpreterConfiguration().programFilepath().toURI(),
			cmdParser.getInterpreterConfiguration().charset(),
			cmdParser.getInterpreterConfiguration().includePaths(),
			cmdParser.getInterpreterConfiguration().packagePaths(),
			cmdParser.getInterpreterConfiguration().jolieClassLoader(),
			cmdParser.getInterpreterConfiguration().constants(),
			cmdParser.getInterpreterConfiguration().executionTarget(),
			false );
		// GZIPOutputStream gzipstream = new GZIPOutputStream( ostream );
		ObjectOutputStream oos = new ObjectOutputStream( ostream );
		oos.writeObject( program );
		ostream.flush();
		// gzipstream.close();
	}

	public void compile()
		throws IOException, ParserException, CodeCheckException, CommandLineException, ModuleException {
		try( OutputStream os =
			new FileOutputStream( cmdParser.getInterpreterConfiguration().programFilepath() + "c" ) ) {
			compile( os );
		}
	}
}
