package joliex.java;

import java.io.IOException;

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

	private static ProgramInspector getInspector( Jolie2JavaCommandLineParser cmdParser ) throws CommandLineException, IOException, CodeCheckException  {
		final SemanticVerifier.Configuration configuration = new SemanticVerifier.Configuration( cmdParser.getInterpreterConfiguration().executionTarget() );

		configuration.setCheckForMain( false );

		final Program program = ParsingUtils.parseProgram(
			cmdParser.getInterpreterConfiguration().inputStream(),
			cmdParser.getInterpreterConfiguration().programFilepath().toURI(),
			cmdParser.getInterpreterConfiguration().charset(),
			cmdParser.getInterpreterConfiguration().includePaths(),
			cmdParser.getInterpreterConfiguration().packagePaths(),
			cmdParser.getInterpreterConfiguration().jolieClassLoader(),
			cmdParser.getInterpreterConfiguration().constants(),
			configuration,
			true
		);

		return ParsingUtils.createInspector( program );
	}
}
