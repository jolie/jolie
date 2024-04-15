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

public class TypedStructureClassBuilder extends TypeClassBuilder {

    private final Structure structure;
    private final List<Structure.Field> recordFields;

    public TypedStructureClassBuilder( Structure.Inline.Typed structure, String packageName, String typeFolder ) {
        super( structure.name(), packageName, typeFolder ); 

        this.structure = structure;
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

    public void appendDefinition( boolean isInnerClass ) {
        builder.newline()
            .commentBlock( this::appendDocumentation )
            .newlineAppend( "public " ).append( isInnerClass ? "static " : "" ).append( "final class " ).append( className ).append( " implements JolieValue" ).body( () -> {
                appendAttributes();
                appendConstructors();
                appendMethods();
                appendStaticMethods();
                appendBuilder();
                appendListBuilder();
                appendInnerClasses();
            } );
    }

    private void appendDocumentation() {
        builder.newlineAppend( "this class is an {@link JolieValue} which can be described as follows:" )
            .newline()
            .codeBlock( this::appendStructureDocumentation )
            .newline();

        StreamEx.of( "JolieValue", "JolieNative" )
            .append( 
                structure.fields()
                    .parallelStream()
                    .map( Structure.Field::type )
                    .map( t -> t instanceof Definition d ? d.name() : null )
                    .filter( Objects::nonNull )
            )
            .distinct() 
            .append( "#construct()" )
            .forEachOrdered( s -> builder.newlineAppend( "@see " ).append( s ) );
    }

    private void appendStructureDocumentation() {
        if ( structure.nativeType() != Native.VOID )
            builder.newlineAppend( structure.nativeType() == Native.ANY ? "content" : "contentValue" ).append( ": {@link " ).append( structure.nativeType().valueName() ).append( "}" ).append( structure.possibleRefinement().map( r -> "( " + r.definitionString() + " )" ).orElse( "" ) );

        structure.fields().forEach( field ->
            builder.indentedNewlineAppend( field.possibleName().map( n -> field.key().equals( n ) ? n : n + "(\"" + field.key() + "\")" ).orElse( "\"" + field.key() + "\"" ) ).append( field.min() != 1 || field.max() != 1 ? "[" + field.min() + "," + field.max() + "]" : "" ).append( ": {@link " ).append( field.typeName().replaceAll( "<.*>", "" ) ).append( "}" )
        );
    }

    private void appendAttributes() {
        // static set of all field names
        builder.newNewlineAppend( "private static final Set<String> FIELD_KEYS = " ).append( structure.fields()
            .parallelStream()
            .map( f -> "\"" + f.key() + "\"" )
            .reduce( (s1,s2) -> s1 + ", " + s2 )
            .map( s -> "Set.of( " + s + " );" )
            .orElse( "Set.of();" ) );

        // fields
        builder.newline();
        recordFields.parallelStream()
            .map( f -> new StringBuilder()
                .append( "private final " )
                .append( f.max() == 1 ? getTypeName( f ) : "List<" + getTypeName( f ) + ">" )
                .append( " " )
                .append( f.fieldName() )
                .append( ";" )
                .toString()
            )
            .forEachOrdered( builder::newlineAppend );
    }

    private void appendConstructors() {
        final String parameters = recordFields.parallelStream()
            .map( f -> new StringBuilder()
                .append( f.max() == 1 ? getTypeName( f ) : "SequencedCollection<" + getTypeName( f ) + ">" )
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
                            .append( f.fieldName() ).append( ", " )
                            .append( f.min() ).append( ", " )
                            .append( f.max() )
                            .append( refinement.map( r -> ", " + r ).orElse( "" ) )
                            .append( " );" )
                            .toString();

                    if ( f.min() == 0 )
                        return result.append( refinement.map( r -> "Refinement.validated( " + f.fieldName() + " )" ).orElse( f.fieldName() ) ).append( ";" ).toString();

                    return result.append( "ValueManager.validated( " )
                        .append( f.fieldName() )
                        .append( refinement.map( r -> ", " + r ).orElse( "" ) )
                        .append( " );" )
                        .toString();
                } )
                .forEachOrdered( builder::newlineAppend );
        } );
    }

    private void appendMethods() {
        builder.newline();
        recordFields.forEach( f -> {
            if ( f.max() != 1 )
                builder.newlineAppend( "public List<" ).append( getTypeName( f ) ).append( "> " ).append( f.fieldName() ).append( "() { return " ).append( f.fieldName() ).append( "; }" );
            else if ( f.min() == 0 )
                builder.newlineAppend( "public Optional<" ).append( getTypeName( f ) ).append( "> " ).append( f.fieldName() ).append( "() { return Optional.ofNullable( " ).append( f.fieldName() ).append( " ); }" );
            else
                builder.newlineAppend( "public " ).append( getTypeName( f ) ).append( " " ).append( f.fieldName() ).append( "() { return " ).append( f.fieldName() ).append( "; }" );
        } );

        builder.newline();
        if ( structure.nativeType() != Native.ANY )
            builder.newlineAppend( "public " ).append( structure.nativeType().wrapperName() ).append( " content() { return new " ).append( structure.nativeType().wrapperName() ).append( structure.nativeType() == Native.VOID ? "()" : "( contentValue )" ).append( "; }" );

        // TODO: make it so this only ever builds the Map once.
        builder.newlineAppend( "public Map<String, List<JolieValue>> children()" ).body( () -> {
            builder.newlineAppend( "return one.util.streamex.EntryStream.of(" );
            structure.fields()
                .parallelStream()
                .map( f -> new StringBuilder()
                    .append( "\"" ).append( f.key() ).append( "\", " )
                    .append( switch ( getType( f ) ) {
                        case Native n -> {
                            if ( f.max() != 1 )
                                yield f.fieldName() + ".parallelStream().map( JolieValue::create ).toList()";
                            if ( f.min() == 0 )
                                yield f.fieldName() + " == null ? null : List.of( JolieValue.create( " + f.fieldName() + " ) )";
                            yield "List.of( JolieValue.create( " + f.fieldName() + " ) )";
                        }
                        case Definition d -> {
                            if ( f.max() != 1 )
                                yield f.fieldName() + ".parallelStream().map( JolieValue.class::cast ).toList()";
                            if ( f.min() == 0 )
                                yield f.fieldName() + " == null ? null : List.<JolieValue>of( " + f.fieldName() + " )";
                            yield "List.<JolieValue>of( " + f.fieldName() + " )";
                        }
                    } )
                    .toString() )
                .reduce( (s1, s2) -> s1 + ",\n" + s2 )
                .ifPresent( builder::indentedNewlineAppend );
            builder.newlineAppend( ").filterValues( Objects::nonNull ).toImmutableMap();" );
        } );
    }

    private void appendStaticMethods() {
        // construct() method
        builder.newNewlineAppend( "public static Builder construct() { return new Builder(); }" );
        // constructList() method
        builder.newNewlineAppend( "public static ListBuilder constructList() { return new ListBuilder(); }" );
        // constructFrom() method
        builder.newNewlineAppend( "public static Builder constructFrom( JolieValue j ) { return new Builder( j ); }" );
        // constructListFrom() method
        builder.newNewlineAppend( "public static ListBuilder constructListFrom( SequencedCollection<? extends JolieValue> c ) { return new ListBuilder( c ); }" );
        // createFrom() method
        builder.newNewlineAppend( "public static " ).append( className ).append( " createFrom( JolieValue j )" ).body( () -> {
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
                .newlineAppend( ");" );
        } );
        // fromValue() method
        builder.newNewlineAppend( "public static " ).append( className ).append( " fromValue( Value v ) throws TypeCheckingException" ).body( () -> {
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
                            .append( switch ( getType( f ) ) {
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
                .newlineAppend( ");" );
        } );
        // toValue() method
        builder.newNewlineAppend( "public static Value toValue( " ).append( className ).append( " t )" ).body( () -> {
            builder.newlineAppend( "final Value v = " ).append( switch ( structure.nativeType() ) {
                case ANY -> "JolieNative.toValue( t.content() )";
                case VOID -> "Value.create()";
                default -> "Value.create( t.contentValue() )";
            } ).append( ";" );
            
            builder.newline();
            structure.fields().forEach( f -> {
                final String getField = "t." + f.fieldName() + "()";
                final UnaryOperator<String> setValue = x -> switch ( getType( f ) ) {
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

    private void appendBuilder() {
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
                .append( f.max() == 1 ? getTypeName( f ) : "SequencedCollection<" + getTypeName( f ) + ">" )
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
            builder.newlineAppend( "public Builder " ).append( f.fieldName() ).append( "( " ).append( f.max() == 1 ? getTypeName( f ) : "SequencedCollection<" + getTypeName( f ) + ">" ).append( " " ).append( f.fieldName() ).append( " ) { this." ).append( f.fieldName() ).append( " = " ).append( f.fieldName() ).append( "; return this; }" );
            
            if ( f.max() != 1 )
                Optional.ofNullable( switch ( f.type() ) {
                    case Native.ANY -> "JolieNative<?>";
                    case Structure s -> s.name();
                    default -> null;
                } ).ifPresent( n -> appendBuilderSetter( f.fieldName(), n, true ) );
            else if ( f.type() instanceof Structure s )
                appendBuilderSetter( f.fieldName(), s.name(), false );
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

    private void appendBuilderSetter( String fieldName, String typeName, boolean isList ) {
        if ( isList )
            appendBuilderSetter( fieldName, typeName.replace( "<?>", "" ), "ListBuilder", "constructList", "List<" + typeName + ">" );
        else
            appendBuilderSetter( fieldName, typeName.replace( "<?>", "" ), "Builder", "construct", typeName );
    }
    private void appendBuilderSetter( String fieldName, String typeName, String builderClass, String constructMethod, String returnType ) {
        builder.newlineAppend( "public Builder " ).append( fieldName ).append( "( Function<" ).append( typeName ).append( "." ).append( builderClass ).append( ", " ).append( returnType ).append( "> b ) { return " ).append( fieldName ).append( "( b.apply( " ).append( typeName ).append( "." ).append( constructMethod ).append( "() ) ); }" );
    }

    private void appendListBuilder() {
        builder.newNewlineAppend( "public static class ListBuilder extends AbstractListBuilder<ListBuilder, " ).append( className ).append( ">" ).body( () -> {
            appendListBuilderConstructors();
            appendListBuilderMethods();
        } );
    }

    private void appendListBuilderConstructors() {
        builder.newNewlineAppend( "private ListBuilder() {}" )
            .newlineAppend( "private ListBuilder( SequencedCollection<? extends JolieValue> c ) { super( c, " ).append( className ).append( "::createFrom ); }" );
    }

    private void appendListBuilderMethods() {
        builder.newNewlineAppend( "protected ListBuilder self() { return this; }" )
            .newline()
            .newlineAppend( "public ListBuilder add( Function<Builder, " ).append( className ).append( "> b ) { return add( b.apply( construct() ) ); }" )
            .newlineAppend( "public ListBuilder set( int index, Function<Builder, " ).append( className ).append( "> b ) { return set( index, b.apply( construct() ) ); }" )
            .newlineAppend( "public ListBuilder reconstruct( int index, Function<Builder, " ).append( className ).append( "> b ) { return replace( index, j -> b.apply( constructFrom( j ) ) ); }" );
    }

    private void appendInnerClasses() {
        structure.fields()
            .parallelStream()
            .map( f -> TypeClassBuilder.create( f.type() ) )
            .filter( Objects::nonNull )
            .map( JavaClassDirector::constructInnerClass )
            .forEachOrdered( builder::newlineAppend );
    }

    private static String fromChildString( Structure.Field f ) {
        final String key = "\"" + f.key() + "\"";
        return new StringBuilder()
            .append( "ValueManager.fieldFrom( " )
            .append( f.max() == 1 ? "j.getFirstChild( " + key + " )" : "j.getChildOrDefault( " + key + ", List.of() )" )
            .append( ", " )
            .append( switch ( getType( f ) ) {
                case Native.ANY -> "JolieValue::content";
                case Native.VOID -> "c -> c.content() instanceof JolieVoid content ? content : null";
                case Native n -> "c -> c.content() instanceof " + n.wrapperName() + " content ? content.value() : null";
                case Definition d -> d.name() + "::createFrom";
            } )
            .append( " )" )
            .toString();
    }
    
    /*
     * Convenience method to handle the fact that Basic.Inline and Native are stored in the same way.
     */
    private static JolieType getType( Structure.Field f ) {
        return f.type() instanceof Basic.Inline b ? b.nativeType() : f.type();
    }

    /*
     * Convenience method to handle the fact that Basic.Inline and Native are stored in the same way.
     */
    private static String getTypeName( Structure.Field f ) {
        return switch( getType( f ) ) {
            case Native n -> n == Native.VOID ? n.wrapperName() : n.valueName();
            case Definition d -> d.name();
        };
    }
}
