package joliex.java.generate.type;

import joliex.java.generate.util.ClassPath;
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

    public void appendDefinition() {
        appendSignature();
        builder.body( this::appendBody );
    }
    
    @Override
    protected void appendDocumentation() {}
    
    @Override
    protected void appendDescriptionDocumentation() {}
    
    @Override
    protected void appendDefinitionDocumentation() {}

    @Override
    protected void appendSeeDocumentation() {}

    @Override
    protected void appendSignature( boolean isInnerClass ) { appendSignature(); }

    private void appendSignature() {
        builder.newlineAppend( "public static record " ).append( className ).append( type == Native.VOID ? "()" : "( " + typeName( type ) + " option )" ).append( " implements " ).append( superName );
    }

    @Override
    protected void appendBody() {
        appendConstructors();
        switch ( storedType( type ) ) {
            case Native n -> appendMethods( n );
            case Definition d -> appendMethods( d );
        }
    }

    private void appendConstructors() {
        if ( type != Native.VOID )
            builder.newNewlineAppend( "public " ).append( className ).append( "{ " ).append( validateMandatoryField( "option", type instanceof Basic.Inline b ? b.refinement() : null ) ).append( "; }" );
    }

    private void appendMethods( Native n ) {
        builder.newline()
            .newlineAppend( "public " ).append( n.wrapperType() ).append( " content() { return " ).append( switch ( n ) { case ANY -> "option"; case VOID -> "new " + ClassPath.JOLIEVOID.get() + "()"; default -> ClassPath.JOLIENATIVE.get() + ".of( option )"; } ).append( "; }" )
            .newlineAppend( "public " ).append( ClassPath.MAP.parameterized( ClassPath.STRING.get(), ClassPath.LIST.parameterized( ClassPath.JOLIEVALUE ) ) ).append( " children() { return " ).append( ClassPath.MAP ).append( ".of(); }" )
            .newlineAppend( "public " ).append( ClassPath.VALUE ).append( " jolieRepr() { return " ).append( switch ( n ) { case ANY -> ClassPath.JOLIENATIVE.get() + ".toValue( option )"; case VOID -> ClassPath.VALUE.get() + ".create()"; default -> ClassPath.VALUE.get() + ".create( option )"; } ).append( "; }" );

        appendOverrideMethods();
        appendFromMethod( n );
        appendFromValueMethod( n );
        appendToValueMethod();
    }
    
    private void appendMethods( Definition d ) {
        builder.newline()
            .newlineAppend( "public " ).append( switch ( d ) { case Basic b -> b.type().wrapperType(); case Structure s -> s.contentType().wrapperType(); case Choice c -> ClassPath.JOLIENATIVE.parameterized( "?" ); } ).append( " content() { return option.content(); }" )
            .newlineAppend( "public " ).append( ClassPath.MAP.parameterized( ClassPath.STRING.get(), ClassPath.LIST.parameterized( ClassPath.JOLIEVALUE ) ) ).append( " children() { return option.children(); }" )
            .newlineAppend( "public " ).append( ClassPath.VALUE ).append( " jolieRepr() { return " ).append( qualifiedName( d ) ).append( ".toValue( option ); }" );

        appendOverrideMethods();
        appendFromMethod( d );
        appendFromValueMethod( d );
        appendToValueMethod();
    }

    private void appendOverrideMethods() {
        if ( type != Native.VOID )
            builder.newline()
                .newlineAppend( "public boolean equals( " ).append( ClassPath.OBJECT ).append( " obj ) { return obj != null && obj instanceof " ).append( ClassPath.JOLIEVALUE ).append( " j && " ).append( storedType( type ) instanceof Native ? "option.equals( j.content().value() ) && j.children().isEmpty()" : "option.equals( j )" ).append( "; }" )
                .newlineAppend( "public int hashCode() { return option.hashCode(); }" )
                .newlineAppend( "public " ).append( ClassPath.STRING ).append( " toString() { return option.toString(); }" );
        else
            builder.newline()
                .newlineAppend( "public boolean equals( " ).append( ClassPath.OBJECT ).append( " obj ) { return obj != null && obj instanceof " ).append( ClassPath.JOLIEVALUE ).append( " j && j.content() instanceof " ).append( ClassPath.JOLIEVOID ).append( " && j.children().isEmpty(); }" )
                .newlineAppend( "public int hashCode() { return 0; }" )
                .newlineAppend( "public " ).append( ClassPath.STRING ).append( " toString() { return \"\"; }" );
    }

    private void appendFromMethod( Native n ) {
        builder.newNewlineAppend( "public static " ).append( className ).append( " from( " ).append( ClassPath.JOLIEVALUE ).append( " j ) throws " ).append( ClassPath.TYPEVALIDATIONEXCEPTION ).body( () -> {
            if ( n == Native.VOID )
                builder.newlineAppend( "return new " ).append( className ).append( "();" );
            else if ( n == Native.ANY )
                builder.newlineAppend( "return new " ).append( className ).append( "( j.content() );" );
            else
                builder.newlineAppend( "return new " ).append( className ).append( "( " ).append( n.wrapperClass() ).append( ".from( j ).value() );" );
        } );
    }

    private void appendFromMethod( Definition d ) {
        builder.newNewlineAppend( "public static " ).append( className ).append( " from( " ).append( ClassPath.JOLIEVALUE ).append( " j ) throws " ).append( ClassPath.TYPEVALIDATIONEXCEPTION ).append( " { return new " ).append( className ).append( "( " ).append( qualifiedName( d ) ).append( ".from( j ) ); }" );
    }

    private void appendFromValueMethod( Native n ) {
        builder.newNewlineAppend( "public static " ).append( className ).append( " fromValue( " ).append( ClassPath.VALUE ).append( " v ) throws " ).append( ClassPath.TYPECHECKINGEXCEPTION ).append( " { " ).append(
            switch ( n ) {
                case VOID -> ClassPath.JOLIEVOID.get() + ".requireVoid( v ); return new " + className + "();";
                case ANY -> "return " + ClassPath.JOLIENATIVE.get() + ".fromValue( v );";
                default -> "return new " + className + "( " + n.wrapperClass().get() + ".fieldFromValue( v ) );";
            }
        ).append( " }" );
    }

    private void appendFromValueMethod( Definition d ) {
        builder.newNewlineAppend( "public static " ).append( className ).append( " fromValue( " ).append( ClassPath.VALUE ).append( " v ) throws " ).append( ClassPath.TYPECHECKINGEXCEPTION ).append( " { return new " ).append( className ).append( "( " ).append( qualifiedName( d ) ).append( ".fromValue( v ) ); }" );
    }

    private void appendToValueMethod() {
        builder.newNewlineAppend( "public static " ).append( ClassPath.VALUE ).append( " toValue( " ).append( className ).append( " t ) { return t.jolieRepr(); }" );
    }
}
