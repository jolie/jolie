package joliex.java.parse.util;

import java.util.Set;
import javax.lang.model.SourceVersion;
import jolie.lang.parse.ast.types.TypeDefinition;

public class NameFormatter {

	private static final Set< String > RESERVED_FIELD_NAMES = Set.of(
		"content",
		"contentValue",
		"children",
		"builder",
		"listBuilder" );

	private static final Set< String > RESERVED_CLASS_NAMES = Set.of(
		"Builder",
		"ListBuilder" );

	public static String getJavaName( String name, TypeDefinition typeDefinition ) {
		return AnnotationParser.parseJavaName( typeDefinition ).orElse( name );
	}

	public static String requireValidClassName( String name, Set< String > namingScope ) {
		if( RESERVED_CLASS_NAMES.contains( name ) )
			throw new InvalidNameException(
				"Class name was reserved, got (qualified): \"" + qualifiedName( name, namingScope ) + "\"." );

		if( !(SourceVersion.isIdentifier( name )
			&& (Character.isUpperCase( name.charAt( 0 ) ) || name.startsWith( "_" ))) )
			throw new InvalidNameException(
				"Class names must be a valid identifier starting with an uppercase letter or underscore, got (qualified): \""
					+ qualifiedName( name, namingScope ) + "\"." );

		return name;
	}

	public static String requireValidFieldName( String name ) {
		if( RESERVED_FIELD_NAMES.contains( name ) || SourceVersion.isKeyword( name ) )
			throw new InvalidNameException( "Field name was reserved, got: \"" + name + "\"." );

		if( !(SourceVersion.isIdentifier( name )
			&& (Character.isLowerCase( name.charAt( 0 ) ) || name.startsWith( "_" ))) )
			throw new InvalidNameException(
				"Field names must be a valid identifier starting with a lowercase letter or underscore, got: \"" + name
					+ "\"." );

		return name;
	}

	public static String qualifiedName( String name, Set< String > namingScope ) {
		return namingScope.parallelStream().reduce( ( n1, n2 ) -> n1 + "." + n2 ).map( n -> n + "." + name )
			.orElse( name );
	}
}
