package joliex.java.generate.type;

import joliex.java.parse.ast.JolieType;
import joliex.java.parse.ast.JolieType.Definition;
import joliex.java.parse.ast.JolieType.Definition.Basic;
import joliex.java.parse.ast.JolieType.Definition.Choice;
import joliex.java.parse.ast.JolieType.Definition.Structure;
import joliex.java.parse.ast.JolieType.Native;

public final class OptionClassBuilder extends TypeClassBuilder {

    private final JolieType type;
    private final String superName;

    public OptionClassBuilder( JolieType type, String superName, String className, String typesPackage ) {
        super( className, typesPackage );
        this.type = type;
        this.superName = superName;
    }

    public void appendHeader() {}

    public void appendDefinition() {
        appendSignature();
        builder.body( this::appendBody );
    }

    protected void appendDocumentation() {}
    protected void appendDescriptionDocumentation() {}
    protected void appendDefinitionDocumentation() {}
    protected void appendSeeDocumentation() {}

    protected void appendSignature( boolean isInnerClass ) { appendSignature(); }
    private void appendSignature() {
        builder.newlineAppend( "public static record " ).append( className ).append( type == Native.VOID ? "()" : "( " + typeName( type ) + " option )" ).append( " implements " ).append( superName );
    }

    protected void appendBody() {
        appendConstructors();
        switch ( storedType( type ) ) {
            case Native n -> appendMethods( n );
            case Definition d -> appendMethods( d );
        }
    }

    private void appendConstructors() {
        if ( type != Native.VOID )
            builder.newNewlineAppend( "public " ).append( className ).append( "( " ).append( typeName( type ) ).append( " option ) { this.option = ValueManager.validated( \"option\", option" ).append( type instanceof Basic.Inline b ? ", " + b.refinement().createString() : "" ).append( " ); }" );
    }

    private void appendMethods( Native n ) {
        builder.newline()
            .newlineAppend( "public " ).append( n.wrapperName() ).append( " content() { return " ).append( switch ( n ) { case ANY -> "option"; case VOID -> "new JolieVoid()"; default -> "JolieNative.of( option )"; } ).append( "; }" )
            .newlineAppend( "public Map<String, List<JolieValue>> children() { return Map.of(); }" )
            .newlineAppend( "public Value jolieRepr() { return " ).append( switch ( n ) { case ANY -> "JolieNative.toValue( option )"; case VOID -> "Value.create()"; default -> "Value.create( option )"; } ).append( "; }" );

        appendOverrideMethods();
        appendFromMethod( n );
        appendFromValueMethod( n );
        appendToValueMethod();
    }
    
    private void appendMethods( Definition d ) {
        builder.newline()
            .newlineAppend( "public " ).append( switch ( d ) { case Basic b -> b.nativeType().wrapperName(); case Structure s -> s.nativeType().wrapperName(); case Choice c -> "JolieNative<?>"; } ).append( " content() { return option.content(); }" )
            .newlineAppend( "public Map<String, List<JolieValue>> children() { return option.children(); }" )
            .newlineAppend( "public Value jolieRepr() { return " ).append( qualifiedName( d ) ).append( ".toValue( option ); }" );

        appendOverrideMethods();
        appendFromMethod( d );
        appendFromValueMethod( d );
        appendToValueMethod();
    }

    private void appendOverrideMethods() {
        if ( type != Native.VOID )
            builder.newline()
                .newlineAppend( "public boolean equals( Object obj ) { return obj != null && obj instanceof JolieValue j && option.equals( " )
                    .append( storedType( type ) instanceof Native ? "j.content().value()" : "j" ).append( " ); }" )
                .newlineAppend( "public int hashCode() { return option.hashCode(); }" )
                .newlineAppend( "public String toString() { return option.toString(); }" );
        else
            builder.newline()
                .newlineAppend( "public boolean equals( Object obj ) { return obj != null && obj instanceof JolieValue j && j.content() instanceof JolieVoid && j.children().isEmpty(); }" )
                .newlineAppend( "public int hashCode() { return 0; }" )
                .newlineAppend( "public String toString() { return \"\"; }" );
    }

    private void appendFromMethod( Native n ) {
        builder.newNewlineAppend( "public static " ).append( className ).append( " from( JolieValue j ) throws TypeValidationException" ).body( () -> {
            if ( n == Native.VOID )
                builder.newlineAppend( "return new " ).append( className ).append( "();" );
            else if ( n == Native.ANY )
                builder.newlineAppend( "return new " ).append( className ).append( "( j.content() );" );
            else
                builder.newlineAppend( "return new " ).append( className ).append( "( " ).append( n.wrapperName() ).append( ".from( j ).value() );" );
        } );
    }

    private void appendFromMethod( Definition d ) {
        builder.newNewlineAppend( "public static " ).append( className ).append( " from( JolieValue j ) throws TypeValidationException { return new " ).append( className ).append( "( " ).append( qualifiedName( d ) ).append( ".from( j ) ); }" );
    }

    private void appendFromValueMethod( Native n ) {
        builder.newNewlineAppend( "public static " ).append( className ).append( " fromValue( Value v ) throws TypeCheckingException { " ).append(
            switch ( n ) {
                case VOID -> "JolieVoid.requireVoid( v ); return new " + className + "();";
                case ANY -> "return JolieNative.fromValue( v );";
                default -> "return new " + className + "( " + n.wrapperName() + ".fieldFromValue( v ) );";
            }
        ).append( " }" );
    }

    private void appendFromValueMethod( Definition d ) {
        builder.newNewlineAppend( "public static " ).append( className ).append( " fromValue( Value v ) throws TypeCheckingException { return new " ).append( className ).append( "( " ).append( qualifiedName( d ) ).append( ".fromValue( v ) ); }" );
    }

    private void appendToValueMethod() {
        builder.newNewlineAppend( "public static Value toValue( " ).append( className ).append( " t ) { return t.jolieRepr(); }" );
    }
}
