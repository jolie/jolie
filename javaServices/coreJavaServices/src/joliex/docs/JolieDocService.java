/*******************************************************************************
 *   Copyright (C) 2019 by Saverio Giallorenzo <saverio.giallorenzo@gmail.com> *
 *                                                                             *
 *   This program is free software; you can redistribute it and/or modify      *
 *   it under the terms of the GNU Library General Public License as           *
 *   published by the Free Software Foundation; either version 2 of the        *
 *   License, or (at your option) any later version.                           *
 *                                                                             *
 *   This program is distributed in the hope that it will be useful,           *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of            *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             *
 *   GNU General Public License for more details.                              *
 *                                                                             *
 *   You should have received a copy of the GNU Library General Public         *
 *   License along with this program; if not, write to the                     *
 *   Free Software Foundation, Inc.,                                           *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.                 *
 *                                                                             *
 *   For details about the authors of this software, see the AUTHORS file.     *
 *******************************************************************************/

package joliex.docs;

import java.io.IOException;
import java.util.ArrayList;
import jolie.CommandLineException;
import jolie.CommandLineParser;
import jolie.lang.parse.ParserException;
import jolie.lang.parse.SemanticException;
import jolie.lang.parse.SemanticVerifier;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.util.ParsingUtils;
import jolie.lang.parse.util.ProgramInspector;
import jolie.runtime.FaultException;
import jolie.runtime.JavaService;
import jolie.runtime.Value;

public class JolieDocService extends JavaService
{
	private static final class GetDocumentationRequest {
		private static final String FILE = "file";
		private static final String LIBRARIES = "libraries";
		private static final String INCLUDES = "includes";
	}
	
	private static final String LIBRARY_OPTION = "-l";
	private static final String INCLUDE_OPTION = "-i";
	
	public Value getDocumentation( Value request ) throws FaultException {
			ArrayList<String> args = new ArrayList<>();
			for ( Value library : request.getChildren( GetDocumentationRequest.LIBRARIES ) ) {
				args.add( LIBRARY_OPTION );
				args.add( library.strValue() );
			}
			for ( Value include : request.getChildren( GetDocumentationRequest.INCLUDES ) ){
				args.add( INCLUDE_OPTION );
				args.add( include.strValue() );
			}
			args.add( request.getFirstChild( GetDocumentationRequest.FILE ).strValue() );
			SemanticVerifier.Configuration configuration = new SemanticVerifier.Configuration();
			configuration.setCheckForMain( false );
			CommandLineParser commandLineParser;
		try {
			commandLineParser = new CommandLineParser( args.toArray( new String[ args.size() ] ), JolieDocService.class.getClassLoader() );
			Program program = ParsingUtils.parseProgram(
				commandLineParser.programStream(),
				commandLineParser.programFilepath().toURI(),
				commandLineParser.charset(),
				commandLineParser.includePaths(), 
				commandLineParser.jolieClassLoader(), 
				commandLineParser.definedConstants(),
				configuration
			);
			ProgramInspector inspector = ParsingUtils.createInspector( program );
			Value returnValue = JolieToValue.buildProgramInfo( inspector );
			returnValue.setFirstChild( "filename", commandLineParser.programFilepath().getName() );
			return returnValue;
			
		} catch( CommandLineException | IOException | ParserException | SemanticException e ) {
			throw new FaultException( e.getClass().getName(), e );
		}
	}
	
	
}
