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

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jolie.CommandLineException;
import jolie.CommandLineParser;
import jolie.lang.parse.ParserException;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.util.ParsingUtils;
import jolie.lang.parse.util.ProgramInspector;

/**
 *
 * @author Claudio Guidi
 */
public class GetSurface
{
	/**
	 * @param args the command line arguments
	 */
	public static void main( String[] args )
	{
		// TODO code application logic here
		try {
			boolean noOutputPort = false;
			boolean noProtocol = false;
			boolean noLocation = false;
			for ( String arg : args )
			{
				if ( arg.equals( "-noOutputPort" ) ) noOutputPort = true;
				if ( arg.equals( "-noLocation" ) ) noLocation = true;
				if ( arg.equals( "-noProtocol" ) ) noProtocol = true;
			}
			CommandLineParser cmdParser = new CommandLineParser( args, GetSurface.class.getClassLoader() );
			Program program = ParsingUtils.parseProgram(
				cmdParser.programStream(),
				cmdParser.programFilepath().toURI(), cmdParser.charset(),
				cmdParser.includePaths(), cmdParser.jolieClassLoader(), cmdParser.definedConstants() );
			ProgramInspector inspector = ParsingUtils.createInspector( program );
			SurfaceCreator document = new SurfaceCreator( inspector, program.context().source() );
                       
			document.ConvertDocument( cmdParser.arguments()[0], noOutputPort, noLocation, noProtocol );

		} catch( CommandLineException ex ) {
			Logger.getLogger( GetSurface.class.getName() ).log( Level.SEVERE, null, ex );
		} catch( IOException e ) {
			e.printStackTrace();
		} catch( ParserException e ) {
			System.out.println( e.getMessage() );
		} catch( Exception e ) {
			e.printStackTrace();
		}
	}
}
