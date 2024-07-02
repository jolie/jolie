package joliex.java.generate.type;

import java.util.Optional;
import joliex.java.generate.util.ClassPath;
import joliex.java.parse.ast.JolieType.Native;
import joliex.java.parse.ast.JolieType.Definition.Structure;

public class UntypedStructureClassBuilder extends StructureClassBuilder {

    private final Optional<String> contentFieldName;

    public UntypedStructureClassBuilder( Structure.Inline.Untyped structure, String typesPackage ) {
        super( structure, typesPackage ); 
        this.contentFieldName = Optional.ofNullable( switch ( structure.contentType() ) {
            case VOID -> null;
            case ANY -> "content";
            default -> "contentValue";
        } );
    }

    protected void appendDescriptionDocumentation() {
        builder.newlineAppend( "this class is an {@link " ).append( ClassPath.UNTYPEDSTRUCTURE ).append( "} which can be described as follows:" );
    }

    protected void appendDefinitionDocumentation() {
        if ( structure.contentType() != Native.VOID )
            builder.newNewlineAppend( structure.contentType() == Native.ANY ? "content" : "contentValue" )
                .append( ": {@link " ).append( structure.contentType().nativeClass() ).append( "}" )
                .append( structure.possibleRefinement().map( r -> "( " + r.definitionString() + " )" ).orElse( "" ) )
                .append( "{ ? }" );
        else
            builder.newNewlineAppend( "void { ? }" );
    }

    protected void appendSeeDocumentation() {
        builder.newline()
            .newlineAppend( "@see " ).append( ClassPath.JOLIEVALUE )
            .newlineAppend( "@see " ).append( ClassPath.JOLIENATIVE )
            .newlineAppend( "@see #builder()" );
    }

    protected void appendSignature( boolean isInnerClass ) {
        builder.newlineAppend( "public " ).append( isInnerClass ? "static " : "" ).append( "final class " ).append( className ).append( " extends " ).append( ClassPath.UNTYPEDSTRUCTURE.parameterized( structure.contentType().wrapperType() ) );
    }

    protected void appendAttributes() {}

    protected void appendConstructors() {
        switch ( structure.contentType() ) {
            case ANY    -> builder.newNewlineAppend( "public " ).append( className ).append( "( " ).append( structure.contentType().wrapperType() ).append( " content, " ).append( ClassPath.childrenMap() ).append( " children ) { super( content, children ); }" );
            case VOID   -> builder.newNewlineAppend( "public " ).append( className ).append( "( " ).append( ClassPath.childrenMap() ).append( " children ) { super( new " ).append( ClassPath.JOLIEVOID ).append( "(), children ); }" );
            default     -> builder.newNewlineAppend( "public " ).append( className ).append( "( " ).append( structure.contentType().nativeType() ).append( " contentValue, " ).append( ClassPath.childrenMap() ).append( " children ) { super( " ).append( ClassPath.JOLIENATIVE ).append( ".of( " ).append( validateRefinement( "contentValue", structure.nativeRefinement() ) ).append( " ), children ); }" );
        }
    }

    protected void appendMethods() {
        contentFieldName.ifPresent( n -> {
            if ( structure.contentType() != Native.ANY )
                builder.newNewlineAppend( "public " ).append( structure.contentType().nativeClass() ).append( " " ).append( n ).append( "() { return content().value(); }" );
        } );
    }

    protected void appendFromMethod() {
        builder.newNewlineAppend( "public static " ).append( className ).append( " from( " ).append( ClassPath.JOLIEVALUE ).append( " j ) throws " ).append( ClassPath.TYPEVALIDATIONEXCEPTION ).body( () -> 
            builder.newlineAppend( "return new " ).append( className ).append( "( " )
                .append( switch ( structure.contentType() ) {
                    case Native.VOID -> "";
                    case Native.ANY -> "j.content(), ";
                    default -> structure.contentType().wrapperClass().get() + ".from( j ).value(), ";
                } )
                .append( "j.children() );" ) );
    }

    protected void appendFromValueMethod() {
        builder.newNewlineAppend( "public static " ).append( className ).append( " fromValue( " ).append( ClassPath.VALUE ).append( " v ) throws " ).append( ClassPath.TYPECHECKINGEXCEPTION ).body( () -> 
            builder.newlineAppend( "return new " ).append( className ).append( "( " ).append( structure.contentType() == Native.VOID ? "" : structure.contentType().wrapperClass() + ".contentFromValue( v ), " ).append( ClassPath.VALUEMANAGER ).append( ".childrenFrom( v ) );" ) );
    }

    protected void appendToValueMethod() {
        builder.newNewlineAppend( "public static " ).append( ClassPath.VALUE ).append( " toValue( " ).append( className ).append( " t ) { return " ).append( ClassPath.JOLIEVALUE ).append( ".toValue( t ); }" );
    }

    protected void appendBuilder() {
        builder.newNewlineAppend( "public static class Builder extends " ).append( ClassPath.UNTYPEDBUILDER.parameterized( "Builder" ) ).body( () -> {
            appendBuilderAttributes();
            appendBuilderConstructors();
            appendBuilderMethods();
        } );
    }

    private void appendBuilderAttributes() {
        contentFieldName.ifPresent(
            n -> builder.newNewlineAppend( "private " ).append( structure.contentType().nativeType() ).append( " " ).append( n ).append( ";" )
        );
    }

    private void appendBuilderConstructors() {
        builder.newNewlineAppend( "private Builder() {}" );

        builder.newNewlineAppend( "private Builder( " ).append( ClassPath.JOLIEVALUE ).append( " j )" ).body( () -> {
            builder.newlineAppend( "super( j.children() );" );

            if ( structure.contentType() == Native.ANY )
                builder.newNewlineAppend( "content = j.content();" );
            else if ( structure.contentType() != Native.VOID )
                builder.newNewlineAppend( "contentValue = j.content() instanceof " ).append( structure.contentType().wrapperClass() ).append( " content ? content.value() : null;" );
        } );
    }

    private void appendBuilderMethods() {
        builder.newNewlineAppend( "protected Builder self() { return this; }" );
        contentFieldName.ifPresent( 
            n -> builder.newNewlineAppend( "public Builder " ).append( n ).append( "( " ).append( structure.contentType().nativeType() ).append( " " ).append( n ).append( " ) { this." ).append( n ).append( " = " ).append( n ).append( "; return this; }" )
        );
        builder.newNewlineAppend( "public " ).append( className ).append( " build() { return new " ).append( className ).append( "( " ).append( switch ( structure.contentType() ) { case VOID -> ""; case ANY -> "content == null ? new " + ClassPath.JOLIEVOID.get() + "() : content, "; default -> "contentValue, "; } ).append( "children ); }" );
    }

    protected void appendTypeClasses() {}
}
