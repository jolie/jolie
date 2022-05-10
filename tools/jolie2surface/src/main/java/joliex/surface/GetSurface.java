/***************************************************************************
 *   Copyright (C) 2011 by Claudio Guidi <cguidi@italianasoftware.com>     *
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

package joliex.surface;

import jolie.cli.CommandLineException;
import jolie.JolieURLStreamHandlerFactory;
import jolie.lang.CodeCheckException;
import jolie.lang.parse.ParserException;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.module.ModuleException;
import jolie.lang.parse.util.ParsingUtils;
import jolie.lang.parse.util.ProgramInspector;

import java.io.IOException;

/**
 *
 * @author Claudio Guidi
 */
public class GetSurface {
	/**
	 * @param args the command line arguments
	 */
	static {
		JolieURLStreamHandlerFactory.registerInVM();
	}

	public static void main( String[] args ) {
		// TODO code application logic here
		try {
			JolieToSurfaceCommandLineParser cmdParser =
				JolieToSurfaceCommandLineParser.create( args, GetSurface.class.getClassLoader() );
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
			ProgramInspector inspector = ParsingUtils.createInspector( program );
			SurfaceCreator document = new SurfaceCreator( inspector );
			document.ConvertDocument( cmdParser.getInterpreterConfiguration().arguments()[ 0 ],
				cmdParser.noOutputPort(),
				cmdParser.noLocation(), cmdParser.noProtocol() );
		} catch( CommandLineException | ParserException | ModuleException e ) {
			System.out.println( e.getMessage() );
		} catch( IOException | CodeCheckException e ) {
			e.printStackTrace();
		}
	}
}
