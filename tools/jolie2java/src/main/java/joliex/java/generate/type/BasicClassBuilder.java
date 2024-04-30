package joliex.java.generate.type;

import joliex.java.parse.ast.JolieType.Definition.Basic;

public class BasicClassBuilder extends TypeClassBuilder {

    private final Basic basic;

    public BasicClassBuilder( Basic basic, String typesPackage ) {
        super( basic.name(), typesPackage );
        this.basic = basic; 
    }

    @Override
    public void appendHeader() {
        builder.append( "package " ).append( typesPackage ).append( ";" )
            .newline()
            .newlineAppend( "import jolie.runtime.Value;" )
            .newlineAppend( "import jolie.runtime.typing.TypeCheckingException;" )
            .newlineAppend( "import jolie.runtime.embedding.java.JolieValue;" )
            .newlineAppend( "import jolie.runtime.embedding.java.JolieNative;" )
            .newlineAppend( "import jolie.runtime.embedding.java.JolieNative.*;" )
            .newlineAppend( "import jolie.runtime.embedding.java.TypeValidationException;" )
            .newlineAppend( "import jolie.runtime.embedding.java.util.*;" )
            .newline()
            .newlineAppend( "import java.util.Map;" )
            .newlineAppend( "import java.util.List;" )
            .newlineAppend( "import java.util.Objects;" );
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
        appendConstructors();
        appendMethods();
        appendStaticMethods();
    }

    private void appendConstructors() {
        builder.newNewlineAppend( "public " ).append( className ).append( "( " ).append( basic.nativeType().valueName() ).append( " contentValue )" ).body( () -> 
            builder.newlineAppend( "this.contentValue = ValueManager.validated( \"contentValue\", contentValue, " ).append( basic.refinement().createString() ).append( " );" )
        );
    }

    private void appendMethods() {
        builder.newline()
            .newlineAppend( "public " ).append( basic.nativeType().wrapperName() ).append( " content() { return new " ).append( basic.nativeType().wrapperName() ).append("( contentValue ); }" )
            .newlineAppend( "public Map<String, List<JolieValue>> children() { return Map.of(); }" )
            .newline()
            .newlineAppend( "public boolean equals( Object obj ) { return obj != null && obj instanceof JolieValue j && contentValue.equals( j.content().value() ) && j.children().isEmpty(); }" )
            .newlineAppend( "public int hashCode() { return contentValue.hashCode(); }" )
            .newlineAppend( "public String toString() { return contentValue.toString(); }" );
    }

    private void appendStaticMethods() {
        // from() method
        builder.newNewlineAppend( "public static " ).append( className ).append( " from( JolieValue j ) throws TypeValidationException" ).body( () -> {
            builder.newlineAppend( "return new " ).append( className ).append( "( " ).append( basic.nativeType().wrapperName() ).append( ".from( j ).value() );" );
        } );
        // fromValue() method
        builder.newNewlineAppend( "public static " ).append( className ).append( " fromValue( Value v ) throws TypeCheckingException" ).body( () -> {
            builder.newlineAppend( "return new " ).append( className ).append( "( " ).append( basic.nativeType().wrapperName() ).append( ".fieldFromValue( v ) );" );
        } );
        // toValue() method
        builder.newNewlineAppend( "public static Value toValue( " ).append( className ).append( " t ) { return Value.create( t.contentValue() ); }" );
    }
}
