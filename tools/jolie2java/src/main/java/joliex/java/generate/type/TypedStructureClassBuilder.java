package joliex.java.generate.type;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;
import joliex.java.generate.JavaClassDirector;
import joliex.java.parse.ast.JolieType.Definition;
import joliex.java.parse.ast.JolieType.Native;
import joliex.java.parse.ast.JolieType.Definition.Basic;
import joliex.java.parse.ast.JolieType.Definition.Choice;
import joliex.java.parse.ast.JolieType.Definition.Structure;
import joliex.java.parse.ast.JolieType.Definition.Structure.Undefined;
import one.util.streamex.StreamEx;

public class TypedStructureClassBuilder extends StructureClassBuilder {

    private final List<Structure.Field> recordFields;

    public TypedStructureClassBuilder( Structure.Inline.Typed structure, String typesPackage ) {
        super( structure, typesPackage ); 
        this.recordFields = structure.nativeType() == Native.VOID
            ? structure.fields()
            : StreamEx.of( structure.fields() )
                .prepend( new Structure.Field( null, 
                    structure.nativeType() == Native.ANY ? "content" : "contentValue",
                    1, 1, 
                    structure.nativeRefinement() == null 
                        ? structure.nativeType() 
                        : new Basic.Inline( null, structure.nativeType(), structure.nativeRefinement() ) ) )
                .toList();
    }

    protected void appendDescriptionDocumentation() {
        builder.newlineAppend( "this class is a {@link TypedStructure} which can be described as follows:" );
    }

    protected void appendDefinitionDocumentation() {
        if ( structure.nativeType() != Native.VOID )
            builder.newNewlineAppend( structure.nativeType() == Native.ANY ? "content" : "contentValue" ).append( ": {@link " ).append( structure.nativeType().valueName() ).append( "}" ).append( structure.possibleRefinement().map( r -> "( " + r.definitionString() + " )" ).orElse( "" ) )
                .indented( this::appendFieldDocumentation );
        else
            appendFieldDocumentation();
    }

    private void appendFieldDocumentation() {
        structure.fields().forEach( field ->
            builder.newlineAppend( field.javaName() ).append( field.jolieName().equals( field.javaName() ) ? "" : "(\"" + field.jolieName() + "\")" )
                .append( field.min() != 1 || field.max() != 1 ? "[" + field.min() + "," + field.max() + "]" : "" )
                .append( ": {@link " ).append( typeName( field.type() ).replace( "<?>", "" ) ).append( "}" )
        );
    }

    protected void appendSeeDocumentation() {
        builder.newline();
        StreamEx.of( "JolieValue", "JolieNative" )
            .append( structure.fields()
                .parallelStream()
                .map( f -> f.type() instanceof Definition d ? qualifiedName( d ) : null )
                .filter( Objects::nonNull ) )
            .distinct()
            .forEachOrdered( s -> builder.newlineAppend( "@see " ).append( s ) );

        if ( structure.hasBuilder() )
            builder.newlineAppend( "@see #builder()" );
    }

    protected void appendSignature( boolean isInnerClass ) {
        builder.newlineAppend( "public " ).append( isInnerClass ? "static " : "" ).append( "final class " ).append( className ).append( " extends TypedStructure" );
    }

    protected void appendAttributes() {
        appendStaticAttributes();
        appendFieldAttributes();
    }

    private void appendStaticAttributes() {
        builder.newNewlineAppend( "private static final Set<String> FIELD_KEYS = fieldKeys( " ).append( className ).append( ".class );" );
    }

    private void appendFieldAttributes() {
        builder.newline();
        recordFields.parallelStream()
            .map( f -> new StringBuilder()
                .append( f.jolieName() == null ? "" : "@JolieName(\"" + f.jolieName() + "\")\n" )
                .append( "private final " )
                .append( f.max() == 1 ? typeName( f.type() ) : "List<" + typeName( f.type() ) + ">" )
                .append( " " ).append( f.javaName() ).append( ";" )
                .toString() )
            .forEachOrdered( builder::newlineAppend );
    }

    protected void appendConstructors() {
        final String parameters = recordFields.parallelStream()
            .map( f -> new StringBuilder()
                .append( f.max() == 1 ? typeName( f.type() ) : "SequencedCollection<" + typeName( f.type() ) + ">" )
                .append( " " )
                .append( f.javaName() )
                .toString()
            )
            .reduce( (s1, s2) -> s1 + ", " + s2 )
            .map( s -> "( " + s + " )" )
            .orElse( "()" );

        builder.newNewlineAppend( "public " ).append( className ).append( parameters ).body( () -> {
            recordFields.parallelStream()
                .map( f -> {
                    final StringBuilder result = new StringBuilder().append( "this." ).append( f.javaName() ).append( " = " );
                    final Optional<String> refinement = f.type() instanceof Basic.Inline b 
                        ? Optional.of( b.refinement().createString() ) 
                        : Optional.empty();

                    if ( f.max() != 1 )
                        return result.append( "ValueManager.validated( " )
                            .append( "\"" ).append( f.javaName() ).append( "\", " )
                            .append( f.javaName() ).append( ", " )
                            .append( f.min() ).append( ", " )
                            .append( f.max() )
                            .append( refinement.map( r -> ", " + r ).orElse( "" ) )
                            .append( " );" )
                            .toString();

                    if ( f.min() == 0 )
                        return result.append( refinement.map( r -> "Refinement.validated( " + f.javaName() + " )" ).orElse( f.javaName() ) ).append( ";" ).toString();

                    return result.append( "ValueManager.validated( " )
                        .append( "\"" ).append( f.javaName() ).append( "\", " )
                        .append( f.javaName() )
                        .append( refinement.map( r -> ", " + r ).orElse( "" ) )
                        .append( " );" )
                        .toString();
                } )
                .forEachOrdered( builder::newlineAppend );
        } );
    }

    protected void appendMethods() {
        builder.newline();
        recordFields.forEach( f -> {
            if ( f.max() != 1 )
                builder.newlineAppend( "public List<" ).append( typeName( f.type() ) ).append( "> " ).append( f.javaName() ).append( "() { return " ).append( f.javaName() ).append( "; }" );
            else if ( f.min() == 0 )
                builder.newlineAppend( "public Optional<" ).append( typeName( f.type() ) ).append( "> " ).append( f.javaName() ).append( "() { return Optional.ofNullable( " ).append( f.javaName() ).append( " ); }" );
            else
                builder.newlineAppend( "public " ).append( typeName( f.type() ) ).append( " " ).append( f.javaName() ).append( "() { return " ).append( f.javaName() ).append( "; }" );
        } );

        if ( structure.nativeType() != Native.ANY )
            builder.newNewlineAppend( "public " ).append( structure.nativeType().wrapperName() ).append( " content() { return new " ).append( structure.nativeType().wrapperName() ).append( structure.nativeType() == Native.VOID ? "()" : "( contentValue )" ).append( "; }" );
    }

    protected void appendFromMethod() {
        builder.newNewlineAppend( "public static " ).append( className ).append( " from( JolieValue j )" ).body( () ->
            builder.newlineAppend( "return new " ).append( className ).append( "(" ).indented( () -> {
                if ( structure.nativeType() == Native.ANY )
                    builder.newlineAppend( "j.content()," );
                else if ( structure.nativeType() != Native.VOID )
                    builder.newlineAppend( structure.nativeType().wrapperName() ).append( ".from( j ).value()," );
                    
                structure.fields()
                    .parallelStream()
                    .map( this::fromChildString )
                    .reduce( (s1, s2) -> s1 + ",\n" + s2 )
                    .ifPresent( builder::newlineAppend );
            } )
            .newlineAppend( ");" ) );
    }

    protected void appendFromValueMethod() {
        builder.newNewlineAppend( "public static " ).append( className ).append( " fromValue( Value v ) throws TypeCheckingException" ).body( () ->
            builder.newlineAppend( "ValueManager.requireChildren( v, FIELD_KEYS );" )
                .newlineAppend( "return new " ).append( className ).append( "(" ).indented( () -> {
                    if ( structure.nativeType() == Native.ANY )
                        builder.newlineAppend( "JolieNative.contentFromValue( v )," );
                    else if ( structure.nativeType() != Native.VOID )
                        builder.newlineAppend( structure.nativeType().wrapperName() ).append( ".contentFromValue( v )," );

                    structure.fields()
                        .parallelStream()
                        .map( f -> new StringBuilder()
                            .append( "ValueManager." ).append( f.max() == 1 ? "single" : "vector" )
                            .append( "FieldFrom( v, \"" ).append( f.jolieName() ).append( "\", " )
                            .append( switch ( storedType( f.type() ) ) {
                                case Native.ANY -> "JolieNative::fromValue";
                                case Native.VOID -> "JolieVoid::fromValue";
                                case Native n -> n.wrapperName() + "::fieldFromValue";
                                case Definition d -> qualifiedName( d ) + "::fromValue";
                            } ).append( " )" )
                            .toString() )
                        .reduce( (s1, s2) -> s1 + ",\n" + s2 )
                        .ifPresent( builder::newlineAppend );
                } )
                .newlineAppend( ");" ) );
    }

    protected void appendToValueMethod() {
        builder.newNewlineAppend( "public static Value toValue( " ).append( className ).append( " t )" ).body( () -> {
            builder.newlineAppend( "final Value v = " ).append( switch ( structure.nativeType() ) {
                case ANY -> "JolieNative.toValue( t.content() )";
                case VOID -> "Value.create()";
                default -> "Value.create( t.contentValue() )";
            } ).append( ";" );
            
            builder.newline();
            structure.fields().forEach( f -> {
                final String getField = "t." + f.javaName() + "()";
                final UnaryOperator<String> setValue = x -> switch ( storedType( f.type() ) ) {
                    case Native n when n == Native.ANY || n == Native.VOID -> ".setValue( " + x + ".value() )";
                    case Native n -> ".setValue( " + x + " )";
                    case Definition d -> ".deepCopy( " + qualifiedName( d ) + ".toValue( " + x + " ) )";
                };
                if ( f.max() != 1 )
                    builder.newlineAppend( getField ).append( ".forEach( c -> v.getNewChild( \"" ).append( f.jolieName() ).append( "\" )" ).append( setValue.apply( "c" ) ).append( " );" );
                else if ( f.min() == 0 ) 
                    builder.newlineAppend( getField ).append( ".ifPresent( c -> v.getFirstChild( \"" ).append( f.jolieName() ).append( "\" )" ).append( setValue.apply( "c" ) ).append( " );" );
                else
                    builder.newlineAppend( "v.getFirstChild( \"" ).append( f.jolieName() ).append( "\" )" ).append( setValue.apply( getField ) ).append( ";" );
            } );

            builder.newNewlineAppend( "return v;" );
        } );
    }

    protected void appendBuilder() {
        builder.newNewlineAppend( "public static class Builder" ).body( () -> {
            appendBuilderAttributes();
            appendBuilderConstructors();
            appendBuilderMethods();
        } );
    }

    private void appendBuilderAttributes() {
        builder.newline();
        recordFields.parallelStream()
            .map( f -> new StringBuilder()
                .append( "private " )
                .append( f.max() == 1 ? typeName( f.type() ) : "SequencedCollection<" + typeName( f.type() ) + ">" )
                .append( " " )
                .append( f.javaName() )
                .append( ";" )
                .toString()
            )
            .forEachOrdered( builder::newlineAppend );
    }

    private void appendBuilderConstructors() {
        builder.newNewlineAppend( "private Builder() {}" )
            .newlineAppend( "private Builder( JolieValue j )" ).body( () -> {
            switch ( structure.nativeType() ) {
                case VOID -> {}
                case ANY -> builder.newNewlineAppend( "content = j.content();" );
                default -> builder.newNewlineAppend( "contentValue = j.content() instanceof " ).append( structure.nativeType().wrapperName() ).append( " content ? content.value() : null;" );
            }
            structure.fields()
                .parallelStream()
                .map( f -> new StringBuilder()
                    .append( "this." ).append( f.javaName() )
                    .append( " = " )
                    .append( fromChildString( f ) )
                    .append( ";" )
                    .toString() )
                .forEachOrdered( builder::newlineAppend );
        } );
    }

    private void appendBuilderMethods() {
        builder.newline();

        if ( structure.nativeType() != Native.VOID )
            appendFieldSetter( structure.nativeType() == Native.ANY ? "content" : "contentValue", structure.nativeType().valueName() );
        
        structure.fields().forEach( f -> {
            if ( f.max() == 1 ) {
                appendFieldSetter( f.javaName(), typeName( f.type() ) );
                switch ( storedType( f.type() ) ) {
                    case Basic b        -> appendExtraSetters( f.javaName(), b );
                    case Structure s    -> appendExtraSetters( f.javaName(), s );
                    case Choice c       -> appendExtraSetters( f.javaName(), c );
                    case Native n       -> appendExtraSetters( f.javaName(), n ); 
                }
            } else {
                appendFieldSetter( f.javaName(), "SequencedCollection<" + typeName( f.type() ) + ">" );
                switch ( storedType( f.type() ) ) {
                    case Basic b        -> appendExtraListSetters( f.javaName(), b );
                    case Structure s    -> appendExtraListSetters( f.javaName(), s );
                    case Choice c       -> appendExtraListSetters( f.javaName(), c );
                    case Native n       -> appendExtraListSetters( f.javaName(), n ); 
                }
            }
        } );

        builder.newNewlineAppend( "public " ).append( className ).append( " build()" ).body( () -> 
            recordFields.parallelStream()
                .map( Structure.Field::javaName )
                .reduce( (s1, s2) -> s1 + ", " + s2 )
                .ifPresent( 
                    s -> builder.newlineAppend( "return new " ).append( className ).append( "( " ).append( s ).append( " );" ) 
                )
        );
    }

    private void appendFieldSetter( String fieldName, String fieldType ) {
        appendSetter( fieldName, fieldType, fieldName, () -> builder.append( "this." ).append( fieldName ).append( " = " ).append( fieldName ).append( "; return this;" ) );
    }

    private void appendExtraSetters( String fieldName, Basic b ) {
        appendValueSetter( fieldName, b.nativeType().valueName(),
            "value", "new " + qualifiedName( b ) + "( value )" );
    }

    private void appendExtraSetters( String fieldName, Structure s ) {
        if ( s.hasBuilder() )
            appendBuilderSetter( fieldName, qualifiedName( s ), 
                qualifiedName( s ) + "." + (s instanceof Undefined ? "InlineBuilder" : "Builder"), 
                qualifiedName( s ) + ".builder()" );
    }

    private void appendExtraSetters( String fieldName, Choice c ) {
        // TODO: figure out a good way to allow one to easily set an option as the field value
    }

    private void appendExtraSetters( String fieldName, Native n ) {
        if ( n == Native.ANY )
            Native.ANY.valueNames().forEach( v -> 
                appendValueSetter( fieldName, v, "value", "JolieNative.of( value )" ) );
    }

    private void appendExtraListSetters( String fieldName, Basic b ) {
        appendValueSetter( fieldName, b.nativeType().valueName() + "...",
            "values", "Arrays.stream( values ).map( " + qualifiedName( b ) + "::new ).toList()" );
    }

    private void appendExtraListSetters( String fieldName, Structure s ) {
        if ( s instanceof Undefined )
            appendBuilderSetter( fieldName, "List<JolieValue>", 
                "JolieValue.InlineListBuilder", 
                "JolieValue.listBuilder()" );
        else if ( s.hasBuilder() )
            appendBuilderSetter( fieldName, "List<" + qualifiedName( s ) + ">",
                "StructureListBuilder<" + qualifiedName( s ) + "," + qualifiedName( s ) + ".Builder>", 
                "new StructureListBuilder<>( " + qualifiedName( s ) + "::builder, " + qualifiedName( s ) + "::builder )" );
    }

    private void appendExtraListSetters( String fieldName, Choice c ) {
        if ( c.hasBuilder() )
            appendBuilderSetter( fieldName, "List<" + qualifiedName( c ) + ">", 
                qualifiedName( c ) + ".ListBuilder", qualifiedName( c ) + ".listBuilder()" );
    }

    private void appendExtraListSetters( String fieldName, Native n ) {
        if ( n == Native.ANY )
            appendBuilderSetter( fieldName, "List<JolieNative<?>>", 
                "JolieNative.ListBuilder", "JolieNative.listBuilder()" );
        else if ( n != Native.VOID )
            appendValueSetter( fieldName, n.valueName() + "...", 
                "values", "List.of( values )" );
    }

    private void appendBuilderSetter( String fieldName, String fieldType, String builderClass, String builderSupplier ) {
        appendSetter( fieldName, "Function<" + builderClass + "," + fieldType + ">", "f", () -> 
            builder.append( "return " ).append( fieldName ).append( "( f.apply( " ).append( builderSupplier ).append( " ) );" ) );
    }

    private void appendValueSetter( String fieldName, String paramType, String paramName, String wrappingCall ) {
        appendSetter( fieldName, paramType, paramName, () -> builder.append( "return " ).append( fieldName ).append( "( " ).append( wrappingCall ).append( " );" ) );
    }

    private void appendSetter( String name, String paramType, String paramName, Runnable bodyAppender ) {
        builder.newlineAppend( "public Builder " ).append( name ).append( "( " ).append( paramType ).append( " " ).append( paramName ).append( " ) { " ).run( bodyAppender ).append( " }" );
    }

    protected void appendTypeClasses() {
        structure.fields()
            .parallelStream()
            .map( f -> TypeClassBuilder.create( storedType( f.type() ), typesPackage ) )
            .filter( Objects::nonNull )
            .map( JavaClassDirector::constructInnerClass )
            .forEachOrdered( builder::newlineAppend );
    }

    private String fromChildString( Structure.Field f ) {
        return new StringBuilder()
            .append( "ValueManager.fieldFrom( " )
            .append( f.max() == 1 ? "j.getFirstChild( \"" + f.jolieName() + "\" )" : "j.getChildOrDefault( \"" + f.jolieName() + "\", List.of() )" )
            .append( ", " )
            .append( switch ( storedType( f.type() ) ) {
                case Native.ANY -> "JolieValue::content";
                case Native.VOID -> "c -> c.content() instanceof JolieVoid content ? content : null";
                case Native n -> "c -> c.content() instanceof " + n.wrapperName() + " content ? content.value() : null";
                case Definition d -> qualifiedName( d ) + "::from";
            } )
            .append( " )" )
            .toString();
    }
}
