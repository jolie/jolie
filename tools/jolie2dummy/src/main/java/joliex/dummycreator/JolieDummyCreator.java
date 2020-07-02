/***************************************************************************
 *   Copyright (C) 2011 by Balint Maschio <bmaschio@italianasoftware.com>  *
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

package joliex.dummycreator;

import jolie.CommandLineException;
import jolie.lang.CodeCheckingException;
import jolie.lang.parse.ParserException;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.module.ModuleException;
import jolie.lang.parse.util.ParsingUtils;
import jolie.lang.parse.util.ProgramInspector;
import joliex.dummycreator.impl.JolieDummyDocumentCreator;

import java.io.IOException;

/**
 *
 * @author Balint Maschio
 */
public class JolieDummyCreator {

	/**
	 * @param args the command line arguments
	 */
	public static void main( String[] args ) {
		try {
			JolieDummyCommandLineParser cmdParser =
				JolieDummyCommandLineParser.create( args, JolieDummyCommandLineParser.class.getClassLoader() );
			Program program = ParsingUtils.parseProgram( cmdParser.getInterpreterConfiguration().inputStream(),
				cmdParser.getInterpreterConfiguration().programFilepath().toURI(),
				cmdParser.getInterpreterConfiguration().charset(),
				cmdParser.getInterpreterConfiguration().includePaths(),
				cmdParser.getInterpreterConfiguration().packagePaths(),
				cmdParser.getInterpreterConfiguration().jolieClassLoader(),
				cmdParser.getInterpreterConfiguration().constants(), false );
			ProgramInspector inspector = ParsingUtils.createInspector( program );
			JolieDummyDocumentCreator document =
				new JolieDummyDocumentCreator( inspector, cmdParser.getInterpreterConfiguration().programFilepath() );
			document.createDocument();
		} catch( CommandLineException e ) {
			System.out.println( e.getMessage() );
		} catch( IOException | CodeCheckingException | ParserException e ) {
			e.printStackTrace();
		} catch( ModuleException e ) {
			e.printStackTrace();
		}
	}

}
