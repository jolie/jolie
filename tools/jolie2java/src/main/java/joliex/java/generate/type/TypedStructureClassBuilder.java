package joliex.java.generate.type;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;
import joliex.java.generate.JavaClassDirector;
import joliex.java.parse.ast.JolieType;
import joliex.java.parse.ast.JolieType.Definition;
import joliex.java.parse.ast.JolieType.Native;
import joliex.java.parse.ast.JolieType.Definition.Basic;
import joliex.java.parse.ast.JolieType.Definition.Structure;
import one.util.streamex.StreamEx;

public class TypedStructureClassBuilder extends StructureClassBuilder {

    private final List<Structure.Field> recordFields;

    public TypedStructureClassBuilder( Structure.Inline.Typed structure, String typesPackage ) {
        super( structure, typesPackage ); 
        this.recordFields = structure.nativeType() == Native.VOID
            ? structure.fields()
            : StreamEx.of( structure.fields() )
                .prepend( new Structure.Field( 
                    null, 
                    structure.nativeType() == Native.ANY ? "content" : "contentValue", 
                    CompletableFuture.completedFuture( structure.possibleRefinement()
                        .<JolieType>map( r -> new Basic.Inline( null, structure.nativeType(), r ) )
                        .orElse( structure.nativeType() ) ), 
                    1, 1 ) )
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
            builder.newlineAppend( field.possibleName().map( n -> field.key().equals( n ) ? n : n + "(\"" + field.key() + "\")" ).orElse( "\"" + field.key() + "\"" ) ).append( field.min() != 1 || field.max() != 1 ? "[" + field.min() + "," + field.max() + "]" : "" ).append( ": {@link " ).append( field.typeName().replaceAll( "<.*>", "" ) ).append( "}" )
        );
    }

    protected void appendSeeDocumentation() {
        builder.newline();
        StreamEx.of( "JolieValue", "JolieNative" )
            .append( 
                structure.fields()
                    .parallelStream()
                    .map( Structure.Field::type )
                    .map( t -> t instanceof Definition d ? d.name() : null )
                    .filter( Objects::nonNull )
            )
            .distinct()
            .forEachOrdered( s -> builder.newlineAppend( "@see " ).append( s ) );

        if ( structure.hasBuilder() )
            builder.newlineAppend( "@see #construct()" );
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
                .append( f.key() == null ? "" : "@JolieName(\"" + f.key() + "\")\n" )
                .append( "private final " )
                .append( f.max() == 1 ? typeName( f.type() ) : "List<" + typeName( f.type() ) + ">" )
                .append( " " ).append( f.fieldName() ).append( ";" )
                .toString() )
            .forEachOrdered( builder::newlineAppend );
    }

    protected void appendConstructors() {
        final String parameters = recordFields.parallelStream()
            .map( f -> new StringBuilder()
                .append( f.max() == 1 ? typeName( f.type() ) : "SequencedCollection<" + typeName( f.type() ) + ">" )
                .append( " " )
                .append( f.fieldName() )
                .toString()
            )
            .reduce( (s1, s2) -> s1 + ", " + s2 )
            .map( s -> "( " + s + " )" )
            .orElse( "()" );

        builder.newNewlineAppend( "public " ).append( className ).append( parameters ).body( () -> {
            recordFields.parallelStream()
                .map( f -> {
                    final StringBuilder result = new StringBuilder().append( "this." ).append( f.fieldName() ).append( " = " );
                    final Optional<String> refinement = f.type() instanceof Basic.Inline b 
                        ? Optional.of( b.refinement().createString() ) 
                        : Optional.empty();

                    if ( f.max() != 1 )
                        return result.append( "ValueManager.validated( " )
                            .append( "\"" ).append( f.fieldName() ).append( "\", " )
                            .append( f.fieldName() ).append( ", " )
                            .append( f.min() ).append( ", " )
                            .append( f.max() )
                            .append( refinement.map( r -> ", " + r ).orElse( "" ) )
                            .append( " );" )
                            .toString();

                    if ( f.min() == 0 )
                        return result.append( refinement.map( r -> "Refinement.validated( " + f.fieldName() + " )" ).orElse( f.fieldName() ) ).append( ";" ).toString();

                    return result.append( "ValueManager.validated( " )
                        .append( "\"" ).append( f.fieldName() ).append( "\", " )
                        .append( f.fieldName() )
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
                builder.newlineAppend( "public List<" ).append( typeName( f.type() ) ).append( "> " ).append( f.fieldName() ).append( "() { return " ).append( f.fieldName() ).append( "; }" );
            else if ( f.min() == 0 )
                builder.newlineAppend( "public Optional<" ).append( typeName( f.type() ) ).append( "> " ).append( f.fieldName() ).append( "() { return Optional.ofNullable( " ).append( f.fieldName() ).append( " ); }" );
            else
                builder.newlineAppend( "public " ).append( typeName( f.type() ) ).append( " " ).append( f.fieldName() ).append( "() { return " ).append( f.fieldName() ).append( "; }" );
        } );

        if ( structure.nativeType() != Native.ANY )
            builder.newNewlineAppend( "public " ).append( structure.nativeType().wrapperName() ).append( " content() { return new " ).append( structure.nativeType().wrapperName() ).append( structure.nativeType() == Native.VOID ? "()" : "( contentValue )" ).append( "; }" );
    }

    protected void appendCreateFromMethod() {
        builder.newNewlineAppend( "public static " ).append( className ).append( " createFrom( JolieValue j )" ).body( () ->
            builder.newlineAppend( "return new " ).append( className ).append( "(" ).indented( () -> {
                if ( structure.nativeType() == Native.ANY )
                    builder.newlineAppend( "j.content()," );
                else if ( structure.nativeType() != Native.VOID )
                    builder.newlineAppend( structure.nativeType().wrapperName() ).append( ".createFrom( j ).value()," );
                    
                structure.fields()
                    .parallelStream()
                    .map( TypedStructureClassBuilder::fromChildString )
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
                            .append( "ValueManager.fieldFrom( " )
                            .append( f.max() == 1 
                                ? "v.firstChildOrDefault( \"" + f.key() + "\", Function.identity(), null )" 
                                : "v.children().getOrDefault( \"" + f.key() + "\", ValueVector.create() )"
                            )
                            .append( ", " )
                            .append( switch ( storedType( f.type() ) ) {
                                case Native.ANY -> "JolieNative::fromValue";
                                case Native.VOID -> "JolieVoid::fromValue";
                                case Native n -> n.wrapperName() + "::fieldFromValue";
                                case Definition d -> d.name() + "::fromValue";
                            } )
                            .append( " )" )
                            .toString()
                        )
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
                final String getField = "t." + f.fieldName() + "()";
                final UnaryOperator<String> setValue = x -> switch ( storedType( f.type() ) ) {
                    case Native n when n == Native.ANY || n == Native.VOID -> ".setValue( " + x + ".value() )";
                    case Native n -> ".setValue( " + x + " )";
                    case Definition d -> ".deepCopy( " + d.name() + ".toValue( " + x + " ) )";
                };
                if ( f.max() != 1 )
                    builder.newlineAppend( getField ).append( ".forEach( c -> v.getNewChild( \"" ).append( f.key() ).append( "\" )" ).append( setValue.apply( "c" ) ).append( " );" );
                else if ( f.min() == 0 ) 
                    builder.newlineAppend( getField ).append( ".ifPresent( c -> v.getFirstChild( \"" ).append( f.key() ).append( "\" )" ).append( setValue.apply( "c" ) ).append( " );" );
                else
                    builder.newlineAppend( "v.getFirstChild( \"" ).append( f.key() ).append( "\" )" ).append( setValue.apply( getField ) ).append( ";" );
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
                .append( f.fieldName() )
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
                    .append( "this." ).append( f.fieldName() )
                    .append( " = " )
                    .append( fromChildString( f ) )
                    .append( ";" )
                    .toString() )
                .forEachOrdered( builder::newlineAppend );
        } );
    }

    private void appendBuilderMethods() {
        if ( structure.nativeType() == Native.ANY )
            builder.newNewlineAppend( "public Builder content( JolieNative<?> content ) { this.content = content; return this; }" );
        else if ( structure.nativeType() != Native.VOID )
            builder.newNewlineAppend( "public Builder contentValue( " ).append( structure.nativeType().valueName() ).append( " contentValue ) { this.contentValue = contentValue; return this; }" );
        else
            builder.newline();
        
        structure.fields().forEach( f -> {
            builder.newlineAppend( "public Builder " ).append( f.fieldName() ).append( "( " ).append( f.max() == 1 ? typeName( f.type() ) : "SequencedCollection<" + typeName( f.type() ) + ">" ).append( " " ).append( f.fieldName() ).append( " ) { this." ).append( f.fieldName() ).append( " = " ).append( f.fieldName() ).append( "; return this; }" );
            buildingSetterString( f ).ifPresent( builder::newlineAppend );
        } );

        builder.newNewlineAppend( "public " ).append( className ).append( " build()" ).body( () -> 
            recordFields.parallelStream()
                .map( Structure.Field::fieldName )
                .reduce( (s1, s2) -> s1 + ", " + s2 )
                .ifPresent( 
                    s -> builder.newlineAppend( "return new " ).append( className ).append( "( " ).append( s ).append( " );" ) 
                )
        );
    }

    protected void appendTypeClasses() {
        structure.fields()
            .parallelStream()
            .map( f -> TypeClassBuilder.create( storedType( f.type() ) ) )
            .filter( Objects::nonNull )
            .map( JavaClassDirector::constructInnerClass )
            .forEachOrdered( builder::newlineAppend );
    }

    private static Optional<String> buildingSetterString( Structure.Field f ) {
        return f.max() == 1
            ? singleBuildingSetterString( f.fieldName(), f.type() )
            : listBuildingSetterString( f.fieldName(), f.type() );
    }

    private static Optional<String> singleBuildingSetterString( String fieldName, JolieType fieldType ) {
        return fieldType instanceof Structure s && s.hasBuilder()
            ? Optional.of( buildingSetterString( fieldName, s.name(), "Builder", "construct", s.name() ) )
            : Optional.empty();
    }

    private static Optional<String> listBuildingSetterString( String fieldName, JolieType fieldType ) {
        return Optional.ofNullable( switch ( fieldType ) {
            case Native.ANY -> "JolieNative<?>";
            case Structure s when s.hasBuilder() -> s.name();
            default -> null;
        } ).map( typeName -> buildingSetterString( 
            fieldName, typeName.replace( "<?>", "" ), 
            "ListBuilder", "constructList", 
            "List<" + typeName + ">" 
        ) );
    }

    private static String buildingSetterString( String fieldName, String typeName, String builderClass, String constructMethod, String returnType ) {
        return new StringBuilder()
            .append( "public Builder " ).append( fieldName )
            .append( "( Function<" ).append( typeName ).append( "." ).append( builderClass ).append( ", " ).append( returnType ).append( "> b )" )
            .append( "{ return " ).append( fieldName ).append( "( b.apply( " ).append( typeName ).append( "." ).append( constructMethod ).append( "() ) ); }" )
            .toString();
    }

    private static String fromChildString( Structure.Field f ) {
        final String key = "\"" + f.key() + "\"";
        return new StringBuilder()
            .append( "ValueManager.fieldFrom( " )
            .append( f.max() == 1 ? "j.getFirstChild( " + key + " )" : "j.getChildOrDefault( " + key + ", List.of() )" )
            .append( ", " )
            .append( switch ( storedType( f.type() ) ) {
                case Native.ANY -> "JolieValue::content";
                case Native.VOID -> "c -> c.content() instanceof JolieVoid content ? content : null";
                case Native n -> "c -> c.content() instanceof " + n.wrapperName() + " content ? content.value() : null";
                case Definition d -> d.name() + "::createFrom";
            } )
            .append( " )" )
            .toString();
    }
}
