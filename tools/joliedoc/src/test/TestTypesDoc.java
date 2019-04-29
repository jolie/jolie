package test;

import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;
import jolie.CommandLineException;
import jolie.doc.JolieDoc;
import jolie.doc.JolieDocCommandLineParser;
import jolie.lang.parse.ParserException;
import jolie.lang.parse.SemanticException;
import jolie.lang.parse.SemanticVerifier;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.ast.types.TypeChoiceDefinition;
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import jolie.lang.parse.ast.types.TypeDefinitionUndefined;
import jolie.lang.parse.ast.types.TypeInlineDefinition;
import jolie.lang.parse.util.ParsingUtils;
import jolie.lang.parse.util.ProgramInspector;

public class TestTypesDoc
{
	public static <T> void _switch( Object o, Consumer... a )
	{
		for ( Consumer consumer : a ) {
			consumer.accept( o );
		}
	}

	public static <T> Consumer _case( Class<T> cls, Consumer<T> c )
	{
		return obj -> Optional.of( obj ).filter( cls::isInstance ).map( cls::cast ).ifPresent( c );
	}

	public static void main( String[] a ) throws IOException, ParserException, SemanticException, CommandLineException
	{

		String[] args = { "--outputPortEnabled", "true", "test.iol" };
		JolieDocCommandLineParser cmdParser = JolieDocCommandLineParser.create( args, JolieDoc.class.getClassLoader() );

		SemanticVerifier.Configuration configuration = new SemanticVerifier.Configuration();
		configuration.setCheckForMain( false );
		Program program = ParsingUtils.parseProgram(
			cmdParser.programStream(),
			cmdParser.programFilepath().toURI(),
			cmdParser.charset(),
			cmdParser.includePaths(),
			cmdParser.jolieClassLoader(),
			cmdParser.definedConstants(),
			configuration
		);

		ProgramInspector inspector = ParsingUtils.createInspector( program );

		for ( TypeDefinition type : inspector.getTypes() ) {
			_switch( type,
				_case( TypeChoiceDefinition.class, ( t ) -> {
					System.out.println( "TypeChoiceDefinition " + t.id() );
				} ),
				_case( TypeDefinitionLink.class, ( t ) -> {
					System.out.println( "TypeDefinitionLink " + t.id() );
				} ),
				_case( TypeDefinitionUndefined.class, ( t ) -> {
					System.out.println( "TypeDefinitionUndefined " + t.id() );
				} ),
				_case( TypeInlineDefinition.class, ( t ) -> {
					System.out.println( "TypeInlineDefinition " + t.id() );
				} )
			);
		}

	}
}
