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
        builder.newlineAppend( "this class is a {@link JolieValue} which can be described as follows:" )
            .newline()
            .codeBlock( () -> builder
                .newlineAppend( "content: {@link " ).append( basic.nativeType().valueName() ).append( "}( " ).append( basic.refinement().definitionString() ).append( " )" )
            )
            .newline()
            .newlineAppend( "@see JolieValue" )
            .newlineAppend( "@see JolieNative" )
            .newlineAppend( "@see #create(" ).append( basic.nativeType().valueName() ).append( ")" );
    }

    private void appendDefinitionBody() {
        appendAttributes();
        appendMethods();
        appendConstructors();
        appendStaticMethods();
    }

    private void appendAttributes() {
        builder.newNewlineAppend( "private static final List<Refinement<" ).append( basic.nativeType().valueName() ).append( ">> refinements = " ).append( basic.refinement().createString() ).append( ";" );
    }

    private void appendConstructors() {
        builder.newNewlineAppend( "public " ).append( className ).append( "( " ).append( basic.nativeType().valueName() ).append( " contentValue )" )
            .body( () -> builder.newlineAppend( "this.contentValue = Refinement.validated( contentValue, refinements );" ) );
    }

    private void appendMethods() {
        builder.newline()
            .newlineAppend( "public " ).append( basic.nativeType().wrapperName() ).append( " content() { return new " ).append( basic.nativeType().wrapperName() ).append("( contentValue ); }" )
            .newlineAppend( "public Map<String, List<JolieValue>> children() { return Map.of(); }" )
            .newline()
            .newlineAppend( "public boolean equals( Object obj ) { return obj instanceof JolieValue j && content().equals( j.content() ) && children().equals( j.children() ); }" )
            .newlineAppend( "public int hashCode() { return contentValue.hashCode(); }" )
            .newlineAppend( "public String toString() { return contentValue.toString(); }" );
    }

    private void appendStaticMethods() {
        builder.newline()
            .newlineAppend( "public static " ).append( className ).append( " create( " ).append( basic.nativeType().valueName() ).append( " v ) throws TypeValidationException { return new " ).append( basic.name() ).append( "( v ); }" )
            .newlineAppend( "public static " ).append( className ).append( " createFrom( JolieType t ) throws TypeValidationException { return new " ).append( basic.name() ).append( "( " ).append( basic.nativeType().wrapperName() ).append( ".createFrom( t ).value() ); }" )
            .newline()
            .newlineAppend( "public static Value toValue( " ).append( className ).append( " t ) { return JolieValue.toValue( t ); }" )
            .newlineAppend( "public static " ).append( className ).append( " fromValue( Value value ) throws TypeCheckingException { return new " ).append( basic.name() ).append( "( " + basic.nativeType().wrapperName() + ".fromValue( value ).value() ); }" );
    }
}
