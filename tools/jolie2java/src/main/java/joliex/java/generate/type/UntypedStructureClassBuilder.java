package joliex.java.generate.type;

import java.util.Objects;
import java.util.Optional;
import joliex.java.parse.ast.JolieType.Definition;
import joliex.java.parse.ast.JolieType.Native;
import joliex.java.parse.ast.JolieType.Definition.Structure;

import one.util.streamex.StreamEx;

public class UntypedStructureClassBuilder extends TypeClassBuilder {

    private final Structure structure;
    private final Optional<String> contentFieldName;

    public UntypedStructureClassBuilder( Structure.Inline.Untyped structure, String packageName, String typeFolder ) {
        super( structure.name(), packageName, typeFolder ); 

        this.structure = structure;
        this.contentFieldName = Optional.ofNullable( switch ( structure.nativeType() ) {
            case VOID -> null;
            case ANY -> "content";
            default -> "contentValue";
        } );
    }

    public void appendDefinition( boolean isInnerClass ) {
        builder.newline()
            .commentBlock( this::appendDocumentation )
            .newlineAppend( "public " ).append( isInnerClass ? "static " : "" ).append( "final class " ).append( className ).append( " extends ImmutableStructure<" ).append( structure.nativeType().wrapperName() ).append( ">" ).body( () -> {
                appendAttributes();
                appendMethods();
                appendConstructors();
                appendStaticMethods();
                appendBuilder();
                appendListBuilder();
            } );
    }

    private void appendDocumentation() {
        builder.newlineAppend( "this class is an {@link ImmutableStructure} which can be described as follows:" )
            .newline()
            .codeBlock( () -> builder.newlineAppend( "content: {@link " ).append( structure.nativeType().valueName() ).append( "}" )
                .append( structure.possibleRefinement().map( r -> "( " + r.definitionString() + " )" ).orElse( "" ) )
                .append( " { ? }" ) )
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

    private void appendAttributes() {
        structure.possibleRefinement().ifPresent( r ->
            builder.newNewlineAppend( "private static final List<Refinement<" ).append( structure.nativeType().valueName() ).append( ">> REFINEMENTS = " ).append( r.createString() ).append( ";" )
        );
    }

    private void appendConstructors() {
        switch ( structure.nativeType() ) {
            case ANY    -> builder.newNewlineAppend( "public " ).append( className ).append( "( " ).append( structure.nativeType().wrapperName() ).append( " content, Map<String, List<JolieValue>> children ) { super( content, children ); }" );
            case VOID   -> builder.newNewlineAppend( "public " ).append( className ).append( "( Map<String, List<JolieValue>> children ) { super( new JolieVoid(), children ); }" );
            default     -> builder.newNewlineAppend( "public " ).append( className ).append( "( " ).append( structure.nativeType().valueName() ).append( " contentValue, Map<String, List<JolieValue>> children ) { super( JolieNative.create( " ).append( structure.possibleRefinement().map( r -> "Refinement.validated( contentValue, REFINEMENTS )" ).orElse( "contentValue" ) ).append( " ), children ); }" );
        }
    }

    private void appendMethods() {
        contentFieldName.ifPresent( n -> {
            if ( structure.nativeType() != Native.ANY )
                builder.newNewlineAppend( "public " ).append( structure.nativeType().valueName() ).append( " " ).append( n ).append( "() { return content().value(); }" );
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
        builder.newNewlineAppend( "public static " ).append( className ).append( " createFrom( JolieValue j ) throws TypeValidationException" ).body( () -> 
            builder.newlineAppend( "return new " ).append( className ).append( "( " )
                .append( switch ( structure.nativeType() ) {
                    case Native.VOID -> "";
                    case Native.ANY -> "j.content(), ";
                    default -> structure.nativeType().wrapperName() + ".createFrom( j ).value(), ";
                } )
                .append( "j.children() );" )
        );
        // fromValue() method
        // TODO: make this more efficient
        builder.newNewlineAppend( "public static " ).append( className ).append( " fromValue( Value v ) throws TypeCheckingException { return createFrom( JolieValue.fromValue( v ) ); }" );
        // toValue() method
        builder.newNewlineAppend( "public static Value toValue( " ).append( className ).append( " t ) { return JolieValue.toValue( t ); }" );
    }

    private void appendBuilder() {
        builder.newNewlineAppend( "public static class Builder extends UntypedBuilder<Builder>" ).body( () -> {
            appendBuilderAttributes();
            appendBuilderConstructors();
            appendBuilderMethods();
        } );
    }

    private void appendBuilderAttributes() {
        contentFieldName.ifPresent(
            n -> builder.newNewlineAppend( "private " ).append( structure.nativeType().valueName() ).append( " " ).append( n ).append( ";" )
        );
    }

    private void appendBuilderConstructors() {
        builder.newNewlineAppend( "private Builder() {}" );

        builder.newNewlineAppend( "private Builder( JolieValue j )" ).body( () -> {
            builder.newlineAppend( "super( j.children() );" );

            if ( structure.nativeType() == Native.ANY )
                builder.newNewlineAppend( "content = j.content();" );
            else if ( structure.nativeType() != Native.VOID )
                builder.newNewlineAppend( "contentValue = j.content() instanceof " ).append( structure.nativeType().wrapperName() ).append( " content ? content.value() : null;" );
        } );
    }

    private void appendBuilderMethods() {
        builder.newNewlineAppend( "protected Builder self() { return this; }" );
        contentFieldName.ifPresent( 
            n -> builder.newNewlineAppend( "public Builder " ).append( n ).append( "( " ).append( structure.nativeType().valueName() ).append( " " ).append( n ).append( " ) { this." ).append( n ).append( " = " ).append( n ).append( "; return this; }" )
        );
        builder.newNewlineAppend( "public " ).append( className ).append( " build() { return new " ).append( className ).append( "( " ).append( contentFieldName.map( n -> n + ", " ).orElse( "" ) ).append( "children ); }" );
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
}
