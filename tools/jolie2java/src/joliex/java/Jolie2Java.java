/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package joliex.java;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;
import jolie.CommandLineException;

import jolie.lang.parse.ParserException;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.util.ParsingUtils;
import jolie.lang.parse.util.ProgramInspector;

import joliex.java.impl.JavaDocumentCreator;

import joliex.java.impl.JavaGWTDocumentCreator;


import joliex.java.impl.ProgramVisitor;

/**
 *
 * @author balint
 */
public class Jolie2Java {
public static void main( String[] args )
	{
		try {
			
			Jolie2JavaCommandLineParser cmdParser = Jolie2JavaCommandLineParser.create( args, Jolie2Java.class.getClassLoader() );
			args = cmdParser.arguments();

			Program program = ParsingUtils.parseProgram(
				cmdParser.programStream(),
				URI.create( "file:" + cmdParser.programFilepath() ),
				cmdParser.includePaths(), Jolie2Java.class.getClassLoader(), cmdParser.definedConstants()
			);
			
			//Program program = parser.parse();
			ProgramInspector inspector=ParsingUtils.createInspector( program );
			ProgramVisitor visitor = new ProgramVisitor( program );
			visitor.run();

			System.out.print( args.toString() );
			String format = cmdParser.getFormat();
			System.out.print( format );
			if ( format.equals( "java" ) ) {
                String namespace=cmdParser.getNameSpace();
				JavaDocumentCreator documentJava = new JavaDocumentCreator( inspector,namespace );
				documentJava.ConvertDocument();


			} else if ( format.equals( "gwt" ) ) {
				String namespace=cmdParser.getNameSpace();
				JavaGWTDocumentCreator documentJava = new JavaGWTDocumentCreator( inspector, namespace );
				documentJava.ConvertDocument();
				

			} else {

				System.out.print( "type not yet implemented" );


			}
			


		} catch( formatExeption ex ) {
			Logger.getLogger( Jolie2Java.class.getName() ).log( Level.SEVERE, null, ex );
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
