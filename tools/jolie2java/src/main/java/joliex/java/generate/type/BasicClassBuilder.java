package joliex.java.generate.type;

import joliex.java.generate.util.ClassPath;
import joliex.java.parse.ast.JolieType.Definition.Basic;

public class BasicClassBuilder extends TypeClassBuilder {

    private final Basic basic;

    public BasicClassBuilder( Basic basic, String typesPackage ) {
        super( basic.name(), typesPackage );
        this.basic = basic; 
    }

    protected void appendDescriptionDocumentation() {
        builder.newlineAppend( "this record is a {@link " ).append( ClassPath.JOLIEVALUE ).append( "} which can be described as follows:" );
    }

    protected void appendDefinitionDocumentation() {
        builder.newlineAppend( "contentValue: {@link " ).append( basic.type().nativeClass() ).append( "}( " ).append( basic.refinement().definitionString() ).append( " )" );
    }

    protected void appendSeeDocumentation() {
        builder.newline()
            .newlineAppend( "@see " ).append( ClassPath.JOLIEVALUE )
            .newlineAppend( "@see " ).append( ClassPath.JOLIENATIVE );
    }

    protected void appendSignature( boolean isInnerClass ) {
        builder.newlineAppend( "public " ).append( isInnerClass ? "static " : "" ).append( "record " ).append( className ).append( "( " ).append( basic.type().nativeClass() ).append( " contentValue ) implements " ).append( ClassPath.JOLIEVALUE );
    }

    protected void appendBody() {
        appendConstructors();
        appendMethods();
        appendStaticMethods();
    }

    private void appendConstructors() {
        builder.newNewlineAppend( "public " ).append( className ).append( "( " ).append( basic.type().nativeClass() ).append( " contentValue )" ).body( () -> 
            builder.newlineAppend( "this.contentValue = " ).append( validateMandatoryField( "contentValue", basic.refinement() ) ).append( ";" )
        );
    }

    private void appendMethods() {
        builder.newline()
            .newlineAppend( "public " ).append( basic.type().wrapperClass() ).append( " content() { return new " ).append( basic.type().wrapperClass() ).append("( contentValue ); }" )
            .newlineAppend( "public " ).append( ClassPath.MAP.parameterized( ClassPath.STRING, ClassPath.LIST.parameterized( ClassPath.JOLIEVALUE ) ) ).append( " children() { return " ).append( ClassPath.MAP ).append( ".of(); }" )
            .newline()
            .newlineAppend( "public boolean equals( " ).append( ClassPath.OBJECT ).append( " obj ) { return obj != null && obj instanceof " ).append( ClassPath.JOLIEVALUE ).append( " j && contentValue.equals( j.content().value() ) && j.children().isEmpty(); }" )
            .newlineAppend( "public int hashCode() { return contentValue.hashCode(); }" )
            .newlineAppend( "public " ).append( ClassPath.STRING ).append( " toString() { return contentValue.toString(); }" );
    }

    private void appendStaticMethods() {
        // from() method
        builder.newNewlineAppend( "public static " ).append( className ).append( " from( " ).append( ClassPath.JOLIEVALUE ).append( " j ) throws " ).append( ClassPath.TYPEVALIDATIONEXCEPTION ).body( () ->
            builder.newlineAppend( "return new " ).append( className ).append( "( " ).append( basic.type().wrapperClass() ).append( ".from( j ).value() );" ) );
        // fromValue() method
        builder.newNewlineAppend( "public static " ).append( className ).append( " fromValue( " ).append( ClassPath.VALUE ).append( " v ) throws " ).append( ClassPath.TYPECHECKINGEXCEPTION ).body( () ->
            builder.newlineAppend( "return new " ).append( className ).append( "( " ).append( basic.type().wrapperClass() ).append( ".fieldFromValue( v ) );" ) );
        // toValue() method
        builder.newNewlineAppend( "public static " ).append( ClassPath.VALUE ).append( " toValue( " ).append( className ).append( " t ) { return " ).append( ClassPath.VALUE ).append( ".create( t.contentValue() ); }" );
    }
}
