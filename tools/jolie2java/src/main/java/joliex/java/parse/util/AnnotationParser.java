package joliex.java.parse.util;

import java.util.Optional;
import java.util.regex.Pattern;
import jolie.lang.parse.ast.types.TypeDefinition;

public class AnnotationParser {
	
	private static final Pattern JAVA_NAME = Pattern.compile( "@JavaName\\(\"(\\w+)\"\\)" );
	private static final Pattern INLINE_LINK = Pattern.compile( "@InlineLink\\((true|false)\\)" );
	private static final Pattern GENERATE_BUILDER = Pattern.compile( "@GenerateBuilder\\((true|false)\\)" );
	
	public static Optional<String> parseJavaName( TypeDefinition typeDefinition ) {
		return parseDocumentation( typeDefinition, JAVA_NAME );
	}

	public static Optional<Boolean> parseInlineLink( TypeDefinition typeDefinition ) {
		return parseDocumentation( typeDefinition, INLINE_LINK ).map( Boolean::parseBoolean );
	}

	public static Optional<Boolean> parseGenerateBuilder( TypeDefinition typeDefinition ) {
		return parseDocumentation( typeDefinition, GENERATE_BUILDER ).map( Boolean::parseBoolean );
	}

	private static Optional<String> parseDocumentation( TypeDefinition typeDefinition, Pattern pattern ) {
		return typeDefinition.getDocumentation()
			.map( pattern::matcher )
			.map( m -> m.find() ? m.group( 1 ) : null );	
	}
}
