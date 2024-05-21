package joliex.java.parse.util;

import java.util.Set;
import javax.lang.model.SourceVersion;
import jolie.lang.parse.ast.types.TypeDefinition;

public class NameFormatter {

    private static final Set<String> RESERVED_FIELD_NAMES = Set.of(
        "content",
        "contentValue",
        "children",
        "builder",
        "listBuilder"
    );

    private static final Set<String> RESERVED_CLASS_NAMES = Set.of(
        "Builder",
        "ListBuilder"
    );

    public static String getJavaName( String name, TypeDefinition typeDefinition ) {
        return AnnotationParser.parseJavaName( typeDefinition ).orElse( name );
    }

    public static String requireValidClassName( String name, Set<String> namingScope ) {
        if ( RESERVED_CLASS_NAMES.contains( name ) )
            throw new InvalidNameException( "Class name was reserved, qualified name=\"" + qualifiedName( name, namingScope ) + "\"." );

        if ( !name.matches( "[A-Z][\\w|\\d]*\\w[\\w|\\d]*" ) )
            throw new InvalidNameException( "Class name must match the regex \"[A-Z][\\w|\\d]*\\w[\\w|\\d]*\", qualified name=\"" + qualifiedName( name, namingScope ) + "\"." );

        return name;
    }

    public static String requireValidFieldName( String name ) {
        if ( RESERVED_FIELD_NAMES.contains( name ) || SourceVersion.isKeyword( name ) )
            throw new InvalidNameException( "Field name was reserved, name=\"" + name + "\"." );

        if ( !name.matches( "[a-z][\\w|\\d]*" ) )
            throw new InvalidNameException( "Field names must match the regex \"[a-z][\\w|\\d]*\", name=\"" + name + "\"." );

        return name;
    }

    public static String qualifiedName( String name, Set<String> namingScope ) {
        return namingScope.parallelStream().reduce( (n1,n2) -> n1 + "." + n2 ).map( n -> n + "." + name ).orElse( name );
    }
}
