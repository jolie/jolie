package joliex.java.generate.type;

import java.util.Optional;
import joliex.java.parse.ast.JolieType.Native;
import joliex.java.parse.ast.JolieType.Definition.Structure;

public class UntypedStructureClassBuilder extends StructureClassBuilder {

    private final Optional<String> contentFieldName;

    public UntypedStructureClassBuilder( Structure.Inline.Untyped structure, String typesPackage ) {
        super( structure, typesPackage ); 
        this.contentFieldName = Optional.ofNullable( switch ( structure.nativeType() ) {
            case VOID -> null;
            case ANY -> "content";
            default -> "contentValue";
        } );
    }

    @Override
    public void appendHeader() {
        builder.append( "package " ).append( typesPackage ).append( ";" )
            .newline()
            .newlineAppend( "import jolie.runtime.Value;" )
            .newlineAppend( "import jolie.runtime.ByteArray;" )
            .newlineAppend( "import jolie.runtime.typing.TypeCheckingException;" )
            .newlineAppend( "import jolie.runtime.embedding.java.JolieValue;" )
            .newlineAppend( "import jolie.runtime.embedding.java.JolieNative;" )
            .newlineAppend( "import jolie.runtime.embedding.java.JolieNative.*;" )
            .newlineAppend( "import jolie.runtime.embedding.java.UntypedStructure;" )
            .newlineAppend( "import jolie.runtime.embedding.java.TypeValidationException;" )
            .newlineAppend( "import jolie.runtime.embedding.java.util.*;" )
            .newline()
            .newlineAppend( "import java.util.Map;" )
            .newlineAppend( "import java.util.List;" );
    }

    protected void appendDescriptionDocumentation() {
        builder.newlineAppend( "this class is an {@link UntypedStructure} which can be described as follows:" );
    }

    protected void appendDefinitionDocumentation() {
        if ( structure.nativeType() != Native.VOID )
            builder.newNewlineAppend( structure.nativeType() == Native.ANY ? "content" : "contentValue" )
                .append( ": {@link " ).append( structure.nativeType().valueName() ).append( "}" )
                .append( structure.possibleRefinement().map( r -> "( " + r.definitionString() + " )" ).orElse( "" ) )
                .append( "{ ? }" );
        else
            builder.newNewlineAppend( "content: {@link " ).append( structure.nativeType().wrapperName() ).append( "} { ? }" );
    }

    protected void appendSeeDocumentation() {
        builder.newline()
            .newlineAppend( "@see JolieValue" )
            .newlineAppend( "@see JolieNative" )
            .newlineAppend( "@see #construct()" );
    }

    protected void appendSignature( boolean isInnerClass ) {
        builder.newlineAppend( "public " ).append( isInnerClass ? "static " : "" ).append( "final class " ).append( className ).append( " extends UntypedStructure<" ).append( structure.nativeType().wrapperName() ).append( ">" );
    }

    protected void appendAttributes() {
        structure.possibleRefinement().ifPresent( r ->
            builder.newNewlineAppend( "private static final List<Refinement<" ).append( structure.nativeType().valueName() ).append( ">> REFINEMENTS = " ).append( r.createString() ).append( ";" )
        );
    }

    protected void appendConstructors() {
        switch ( structure.nativeType() ) {
            case ANY    -> builder.newNewlineAppend( "public " ).append( className ).append( "( " ).append( structure.nativeType().wrapperName() ).append( " content, Map<String, List<JolieValue>> children ) { super( content, children ); }" );
            case VOID   -> builder.newNewlineAppend( "public " ).append( className ).append( "( Map<String, List<JolieValue>> children ) { super( new JolieVoid(), children ); }" );
            default     -> builder.newNewlineAppend( "public " ).append( className ).append( "( " ).append( structure.nativeType().valueName() ).append( " contentValue, Map<String, List<JolieValue>> children ) { super( JolieNative.create( " ).append( structure.possibleRefinement().map( r -> "Refinement.validated( contentValue, REFINEMENTS )" ).orElse( "contentValue" ) ).append( " ), children ); }" );
        }
    }

    protected void appendMethods() {
        contentFieldName.ifPresent( n -> {
            if ( structure.nativeType() != Native.ANY )
                builder.newNewlineAppend( "public " ).append( structure.nativeType().valueName() ).append( " " ).append( n ).append( "() { return content().value(); }" );
        } );
    }

    protected void appendCreateFromMethod() {
        builder.newNewlineAppend( "public static " ).append( className ).append( " createFrom( JolieValue j ) throws TypeValidationException" ).body( () -> 
            builder.newlineAppend( "return new " ).append( className ).append( "( " )
                .append( switch ( structure.nativeType() ) {
                    case Native.VOID -> "";
                    case Native.ANY -> "j.content(), ";
                    default -> structure.nativeType().wrapperName() + ".createFrom( j ).value(), ";
                } )
                .append( "j.children() );" ) );
    }

    protected void appendFromValueMethod() {
        // TODO: make this more efficient
        builder.newNewlineAppend( "public static " ).append( className ).append( " fromValue( Value v ) throws TypeCheckingException { return createFrom( JolieValue.fromValue( v ) ); }" );
    }

    protected void appendToValueMethod() {
        builder.newNewlineAppend( "public static Value toValue( " ).append( className ).append( " t ) { return JolieValue.toValue( t ); }" );
    }

    protected void appendBuilder() {
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
        builder.newNewlineAppend( "public " ).append( className ).append( " build() { return new " ).append( className ).append( "( " ).append( switch ( structure.nativeType() ) { case VOID -> ""; case ANY -> "content == null ? new JolieVoid() : content, "; default -> "contentValue, "; } ).append( "children ); }" );
    }

    protected void appendTypeClasses() {}
}
