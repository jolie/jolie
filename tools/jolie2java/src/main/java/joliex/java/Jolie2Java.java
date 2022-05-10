/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package joliex.java;

import jolie.cli.CommandLineException;
import jolie.lang.CodeCheckException;
import jolie.JolieURLStreamHandlerFactory;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.util.ParsingUtils;
import jolie.lang.parse.util.ProgramInspector;
import jolie.runtime.FaultException;
import joliex.java.impl.JavaDocumentCreator;
import joliex.java.impl.JavaGWTDocumentCreator;

import java.io.IOException;

/**
 *
 * @author balint
 */
public class Jolie2Java {

	static {
		JolieURLStreamHandlerFactory.registerInVM();
	}


	public static void main( String[] args ) {
		try {

			Jolie2JavaCommandLineParser cmdParser =
				Jolie2JavaCommandLineParser.create( args, Jolie2Java.class.getClassLoader() );

			Program program = ParsingUtils.parseProgram(
				cmdParser.getInterpreterConfiguration().inputStream(),
				cmdParser.getInterpreterConfiguration().programFilepath().toURI(),
				cmdParser.getInterpreterConfiguration().charset(),
				cmdParser.getInterpreterConfiguration().includePaths(),
				cmdParser.getInterpreterConfiguration().packagePaths(),
				cmdParser.getInterpreterConfiguration().jolieClassLoader(),
				cmdParser.getInterpreterConfiguration().constants(),
				cmdParser.getInterpreterConfiguration().executionTarget(), false );

			ProgramInspector inspector = ParsingUtils.createInspector( program );


			String format = cmdParser.getFormat();
			String packageName = cmdParser.getPackageName();
			if( format == null ) {
				format = "java";
			}
			if( format.equals( "java" ) && packageName != null ) {
				JavaDocumentCreator documentJava = new JavaDocumentCreator( inspector, cmdParser.getPackageName(),
					cmdParser.getTargetPort(), cmdParser.isAddSource(), cmdParser.getOutputDirectory(),
					cmdParser.isBuildXmlenabled(), cmdParser.javaService() );
				documentJava.ConvertDocument();
			} else if( format.equals( "gwt" ) && packageName != null ) {
				System.out.println( "WARNING: gwt conversion is deprecated, use it at your own risk" );
				JavaGWTDocumentCreator documentJava =
					new JavaGWTDocumentCreator( inspector, cmdParser.getPackageName(), cmdParser.getTargetPort() );
				documentJava.ConvertDocument();

			} else {
				System.out.print( cmdParser.getHelpString() );
			}

			System.out.println( "Generation done!" );
		} catch( CommandLineException e ) {
			System.out.println( e.getMessage() );
		} catch( IOException | FaultException | CodeCheckException e ) {
			e.printStackTrace();
		}
	}
}
