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

import java.io.IOException;
import jolie.CommandLineException;
import jolie.lang.parse.ParserException;
import jolie.lang.parse.SemanticException;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.util.ParsingUtils;
import jolie.lang.parse.util.ProgramInspector;
import joliex.dummycreator.impl.JolieDummyDocumentCreator;

/**
 *
 * @author Balint Maschio
 */
public class JolieDummyCreator {

	/**
	* @param args the command line arguments
	*/
	public static void main(String[] args) {
		try{
			JolieDummyCommandLineParser cmdParser= JolieDummyCommandLineParser.create( args, JolieDummyCommandLineParser.class.getClassLoader() );
			Program program = ParsingUtils.parseProgram( cmdParser.programStream(),
			cmdParser.programFilepath().toURI(), cmdParser.charset(),
			cmdParser.includePaths(), cmdParser.jolieClassLoader(), cmdParser.definedConstants());
			ProgramInspector inspector=ParsingUtils.createInspector( program );
			JolieDummyDocumentCreator document= new JolieDummyDocumentCreator( inspector,cmdParser.programFilepath());
			document.createDocument();
		} catch( CommandLineException e ) {
			System.out.println( e.getMessage() );
		} catch( IOException e ) {
			e.printStackTrace();
		} catch( ParserException e ) {
			e.printStackTrace();
		} catch( SemanticException e ) {
			e.printStackTrace();
		}
	}

}
