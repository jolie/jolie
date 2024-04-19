package joliex.java.parse.util;

import java.util.Set;
import java.util.function.Supplier;
import javax.lang.model.SourceVersion;
import jolie.lang.parse.ast.types.TypeDefinition;

public class NameFormatter {

    private static final Set<String> RESERVED_FIELD_NAMES = Set.of(
        "content",
        "contentValue",
        "children",
        "construct",
        "constructList"
    );

    private static final Set<String> RESERVED_CLASS_NAMES = Set.of(
        "Value",
        "ValueVector",
        "TypeCheckingException",
        "ArrayList",
        "Map",
        "SequencedCollection",
        "List",
        "Optional",
        "Function",
        "Predicate",
        "UnaryOperator",
        "BinaryOperator",
        "Stream",
        "Collectors",
        "JolieValue",
        "JolieNative",
        "JolieVoid",
        "Void",
        "JolieBool",
        "Boolean",
        "JolieInt",
        "Integer",
        "JolieLong",
        "Long",
        "JolieDouble",
        "Double",
        "JolieString",
        "String",
        "JolieRaw",
        "ByteArray",
        "ImmutableStructure",
        "TypeValidationException",
        "ValueManager",
        "Refinement",
        "ConversionFunction",
        "UntypedBuilder",
        "Builder",
        "ListBuilder"
    );

    public static String getJavaName( String name, TypeDefinition typeDefinition ) {
        return AnnotationParser.parseJavaName( typeDefinition ).orElse( name );
    }

    public static Supplier<String> classNameSupplier( String name, TypeDefinition typeDefinition ) {
        return () -> requireValidClassName( getJavaName( name, typeDefinition ) );
    }

    public static Supplier<String> classNameSupplier( String javaName ) {
        return () -> requireValidClassName( javaName );
    }

    public static String requireValidClassName( String name ) {
        if ( RESERVED_CLASS_NAMES.contains( name ) )
            throw new InvalidNameException( "Class name was reserved, name=\"" + name + "\"." );

        if ( !name.matches( "[A-Z]\\w[\\w|\\d]*" ) )
            throw new InvalidNameException( "Class names must match the regex \"[A-Z]\\w[\\w|\\d]*\", name=\"" + name + "\"." );

        return name;
    }

    public static String requireValidFieldName( String name ) {
        if ( RESERVED_FIELD_NAMES.contains( name ) || SourceVersion.isKeyword( name ) )
            throw new InvalidNameException( "Field name was reserved, name=\"" + name + "\"." );

        if ( !name.matches( "[a-z][\\w|\\d]*" ) )
            throw new InvalidNameException( "Field names must match the regex \"[a-z][\\w|\\d]*\", name=\"" + name + "\"." );

        return name;
    }
}
