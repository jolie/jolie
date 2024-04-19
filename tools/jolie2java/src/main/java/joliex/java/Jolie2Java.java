package joliex.java;

import java.io.IOException;

import jolie.Interpreter;
import jolie.JolieURLStreamHandlerFactory;
import jolie.cli.CommandLineException;
import jolie.lang.CodeCheckException;
import jolie.lang.parse.SemanticVerifier;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.util.ParsingUtils;
import jolie.lang.parse.util.ProgramInspector;

import joliex.java.generate.JavaDocumentCreator;

public class Jolie2Java {

	static {
		JolieURLStreamHandlerFactory.registerInVM();
	}

	public static void main( String[] args ) {
		try {
			final Jolie2JavaCommandLineParser cmdParser = Jolie2JavaCommandLineParser.create( args, Jolie2Java.class.getClassLoader() );

			if ( cmdParser.packageName() == null ) {
				System.out.print( cmdParser.getHelpString() );
				return;
			}

			final ProgramInspector inspector = getInspector( cmdParser );
			final JavaDocumentCreator jdc = new JavaDocumentCreator(
				cmdParser.packageName(),
				cmdParser.typesPackage(),
				cmdParser.faultsPackage(),
				cmdParser.interfacesPackage(),
				cmdParser.outputDirectory(),
				cmdParser.generateService(),
				cmdParser.serviceName()
			);
			
			jdc.generateClasses( inspector );

			System.out.println( "Generation done!" );
		} 
		catch( CommandLineException e ) { System.out.println( e.getMessage() ); }
		catch( IOException | CodeCheckException e ) { e.printStackTrace(); }
	}

	public static ProgramInspector getInspector( Jolie2JavaCommandLineParser cmdParser ) throws CommandLineException, IOException, CodeCheckException  {
		final Interpreter.Configuration interpreterConfiguration = cmdParser.getInterpreterConfiguration();
		final SemanticVerifier.Configuration semanticConfiguration = new SemanticVerifier.Configuration( interpreterConfiguration.executionTarget() );

		semanticConfiguration.setCheckForMain( false );

		final Program program = ParsingUtils.parseProgram(
			interpreterConfiguration.inputStream(),
			interpreterConfiguration.programFilepath().toURI(),
			interpreterConfiguration.charset(),
			interpreterConfiguration.includePaths(),
			interpreterConfiguration.packagePaths(),
			interpreterConfiguration.jolieClassLoader(),
			interpreterConfiguration.constants(),
			semanticConfiguration,
			true
		);

		return ParsingUtils.createInspector( program );
	}
}
