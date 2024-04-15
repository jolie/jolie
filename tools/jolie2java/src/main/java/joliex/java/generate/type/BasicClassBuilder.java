package joliex.java.generate.type;

import joliex.java.parse.ast.JolieType.Definition.Basic;

public class BasicClassBuilder extends TypeClassBuilder {

    private final Basic basic;

    public BasicClassBuilder( Basic basic, String packageName, String typeFolder ) {
        super( basic.name(), packageName, typeFolder );
        this.basic = basic; 
    }

    public void appendDefinition( boolean isInnerClass ) {
        builder.newline()
            .commentBlock( this::appendDocumentation )
            .newlineAppend( "public " ).append( isInnerClass ? "static " : "" ).append( "record " ).append( className ).append( "( " ).append( basic.nativeType().valueName() ).append( " contentValue ) implements JolieValue" )
            .body( this::appendDefinitionBody );
    }

    private void appendDocumentation() {
        builder.newlineAppend( "this record is a {@link JolieValue} which can be described as follows:" )
            .newline()
            .codeBlock( () -> builder
                .newlineAppend( "contentValue: {@link " ).append( basic.nativeType().valueName() ).append( "}( " ).append( basic.refinement().definitionString() ).append( " )" )
            )
            .newline()
            .newlineAppend( "@see JolieValue" )
            .newlineAppend( "@see JolieNative" );
    }

    private void appendDefinitionBody() {
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
