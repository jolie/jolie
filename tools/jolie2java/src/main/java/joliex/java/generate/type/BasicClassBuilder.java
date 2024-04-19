package joliex.java.generate.type;

import joliex.java.parse.ast.JolieType.Definition.Basic;

public class BasicClassBuilder extends TypeClassBuilder {

    private final Basic basic;

    public BasicClassBuilder( Basic basic, String typesPackage ) {
        super( basic.name(), typesPackage );
        this.basic = basic; 
    }

    protected void appendDescriptionDocumentation() {
        builder.newlineAppend( "this record is a {@link JolieValue} which can be described as follows:" );
    }

    protected void appendDefinitionDocumentation() {
        builder.newlineAppend( "contentValue: {@link " ).append( basic.nativeType().valueName() ).append( "}( " ).append( basic.refinement().definitionString() ).append( " )" );
    }

    protected void appendSeeDocumentation() {
        builder.newline()
            .newlineAppend( "@see JolieValue" )
            .newlineAppend( "@see JolieNative" );
    }

    protected void appendSignature( boolean isInnerClass ) {
        builder.newlineAppend( "public " ).append( isInnerClass ? "static " : "" ).append( "record " ).append( className ).append( "( " ).append( basic.nativeType().valueName() ).append( " contentValue ) implements JolieValue" );
    }

    protected void appendBody() {
        appendAttributes();
        appendConstructors();
        appendMethods();
        appendStaticMethods();
    }

    private void appendAttributes() {
        builder.newNewlineAppend( "private static final List<Refinement<" ).append( basic.nativeType().valueName() ).append( ">> refinements = " ).append( basic.refinement().createString() ).append( ";" );
    }

    private void appendConstructors() {
        builder.newNewlineAppend( "public " ).append( className ).append( "( " ).append( basic.nativeType().valueName() ).append( " contentValue )" ).body( () -> 
            builder.newlineAppend( "this.contentValue = Refinement.validated( Objects.requireNonNull( contentValue ), refinements );" ) 
        );
    }

    private void appendMethods() {
        builder.newline()
            .newlineAppend( "public " ).append( basic.nativeType().wrapperName() ).append( " content() { return new " ).append( basic.nativeType().wrapperName() ).append("( contentValue ); }" )
            .newlineAppend( "public Map<String, List<JolieValue>> children() { return Map.of(); }" )
            .newline()
            .newlineAppend( "public boolean equals( Object obj ) { return obj instanceof " ).append( className ).append( " other && contentValue.equals( other.contentValue() ); }" )
            .newlineAppend( "public int hashCode() { return contentValue.hashCode(); }" )
            .newlineAppend( "public String toString() { return contentValue.toString(); }" );
    }

    private void appendStaticMethods() {
        // createFrom() method
        builder.newNewlineAppend( "public static " ).append( className ).append( " createFrom( JolieValue j ) throws TypeValidationException" ).body( () -> {
            builder.newlineAppend( "return new " ).append( className ).append( "( " ).append( basic.nativeType().wrapperName() ).append( ".createFrom( j ).value() );" );
        } );
        // fromValue() method
        builder.newNewlineAppend( "public static " ).append( className ).append( " fromValue( Value v ) throws TypeCheckingException" ).body( () -> {
            builder.newlineAppend( "return new " ).append( className ).append( "( " ).append( basic.nativeType().wrapperName() ).append( ".fieldFromValue( v ) );" );
        } );
        // toValue() method
        builder.newNewlineAppend( "public static Value toValue( " ).append( className ).append( " t ) { return Value.create( t.contentValue() ); }" );
    }
}
