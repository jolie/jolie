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

package joliex.java;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import jolie.CommandLineException;
import jolie.lang.parse.OLParser;
import jolie.lang.parse.ParserException;
import jolie.lang.parse.Scanner;
import jolie.lang.parse.SemanticVerifier;
import jolie.lang.parse.ast.Program;

import joliex.java.impl.JavaDocumentCreator;
import joliex.java.impl.JavaGWTDocumentCreator;
import joliex.java.impl.ProgramVisitor;

/**
 *
 * @author Fabrizio Montesi
 */
public class Jolie2Java
{
    public static void main( String[] args )
	{
		try {
			/*CommandLineParser cmdParser = new CommandLineParser( args, Jolie2Java.class.getClassLoader() );
			args = cmdParser.arguments();
			if ( args.length < 1 ) {
				throw new CommandLineException( "Syntax is: jolie2java [jolie options] <jolie filename> [interface name list]" );
			}

			//Writer writer = new BufferedWriter( new OutputStreamWriter( System.out ) );

			OLParser parser = new OLParser(
				new Scanner( cmdParser.programStream(), cmdParser.programFilepath() ),
				cmdParser.includePaths(),
				Jolie2Java.class.getClassLoader()
			);*/
                       Jolie2JavaCommandLineParser cmdParser = new Jolie2JavaCommandLineParser( args, Jolie2Java.class.getClassLoader() );
			args = cmdParser.arguments();

			OLParser parser = new OLParser(
				new Scanner( cmdParser.programStream(), cmdParser.programFilepath() ),
				cmdParser.includePaths(),
				Jolie2Java.class.getClassLoader()
			);
			Program program = parser.parse();
                        SemanticVerifier semantic = new SemanticVerifier(program);
                        if (semantic.validate()){
			//Program program = parser.parse();
                        ProgramVisitor visitor= new ProgramVisitor(program);
                        visitor.run();
                        
                        System.out.print(args.toString());
                        String format = cmdParser.GetFormat();
                        System.out.print(format);
                        if (format.equals("java")){
                        
                         JavaDocumentCreator documentJava = new JavaDocumentCreator(visitor);
                        documentJava.ConvertDocument();
                        
                        }else if (format.equals("gwt"))
                        {
                          JavaGWTDocumentCreator documentJava = new JavaGWTDocumentCreator(visitor);
                          documentJava.ConvertDocument();
                        
                        }else{
                        
                        System.out.print("type not yet implemented");
                        
                        
                        }
                        
                        }
            /*new InterfaceConverter(
            program,
            Arrays.copyOfRange( args, 0, args.length ),
            Logger.getLogger( "jolie2java" )
            ).convert( writer );*/

	} catch (formatExeption ex) {
            System.out.print(ex.getMessage());
        } catch( CommandLineException e ) {
			System.out.println( e.getMessage() );
		} catch( IOException e ) {
			e.printStackTrace();
		} catch( ParserException e ) {
			e.printStackTrace();
		}/* catch( InterfaceVisitor.InterfaceNotFound e ) {
			e.printStackTrace();
		}*/
    }
}
