package joliex.java.generate.type;

import joliex.java.generate.JavaClassBuilder;
import joliex.java.parse.ast.JolieType;
import joliex.java.parse.ast.JolieType.Definition;
import joliex.java.parse.ast.JolieType.Definition.Basic;
import joliex.java.parse.ast.JolieType.Definition.Choice;
import joliex.java.parse.ast.JolieType.Definition.Structure;
import joliex.java.parse.ast.JolieType.Native;

public final class OptionClassBuilder extends JavaClassBuilder {

    private final JolieType type;
    private final String className;
    private final String superName;

    public OptionClassBuilder( JolieType type, String className, String superName ) {
        this.type = type;
        this.className = className;
        this.superName = superName;
    }

    public String className() { return null; }
    public void appendHeader() {}

    public void appendDefinition() {
        builder.newlineAppend( "public static record " ).append( className ).append( type == Native.VOID ? "()" : "( " + typeName( type ) + " option )" ).append( " implements " ).append( superName ).body( () -> {
            appendConstructors();
            appendMethods();
            appendStaticMethods();
        });
    }

    private void appendConstructors() {
        if ( type != Native.VOID )
            builder.newNewlineAppend( "public " ).append( className ).append( "( " ).append( typeName( type ) ).append( " option ) { this.option = Objects.requireNonNull( option ); }" );
    }

    private void appendMethods() {
        switch ( type( type ) ) {
            case Native n -> appendMethods( n );
            case Definition d -> appendMethods( d );
        }
        
        if ( type != Native.VOID )
            builder.newline()
                .newlineAppend( "public boolean equals( Object obj ) { return obj instanceof " ).append( className ).append( " o && option.equals( o.option() ); }" )
                .newlineAppend( "public int hashCode() { return option.hashCode(); }" )
                .newlineAppend( "public String toString() { return option.toString(); }" );
        else
            builder.newline()
                .newlineAppend( "public boolean equals( Object obj ) { return obj instanceof " ).append( className ).append( " o; }" )
                .newlineAppend( "public int hashCode() { return 0; }" )
                .newlineAppend( "public String toString() { return \"\"; }" );
    }

    private void appendMethods( Native n ) {
        builder.newline()
            .newlineAppend( "public " ).append( n.wrapperName() ).append( " content() { return " ).append( switch ( n ) { case ANY -> "option"; case VOID -> "new JolieVoid()"; default -> "JolieNative.create( option )"; } ).append( "; }" )
            .newlineAppend( "public Map<String, List<JolieValue>> children() { return Map.of(); }" )
            .newlineAppend( "public Value jolieRepr() { return " ).append( switch ( n ) { case ANY -> "JolieNative.toValue( option )"; case VOID -> "Value.create()"; default -> "Value.create( option )"; } ).append( "; }" );
    }
    
    private void appendMethods( Definition d ) {
        builder.newline()
            .newlineAppend( "public " ).append( switch ( d ) { case Basic b -> b.nativeType().wrapperName(); case Structure s -> s.nativeType().wrapperName(); case Choice c -> "JolieNative<?>"; } ).append( " content() { return option.content(); }" )
            .newlineAppend( "public Map<String, List<JolieValue>> children() { return option.children(); }" )
            .newlineAppend( "public Value jolieRepr() { return " ).append( d.name() ).append( ".toValue( option ); }" );
    }

    private void appendStaticMethods() {
        switch ( type( type ) ) {
            case Native n -> appendStaticMethods( n );
            case Definition d -> appendStaticMethods( d );
        }
        // toValue() method
        builder.newNewlineAppend( "public static Value toValue( " ).append( className ).append( " t ) { return t.jolieRepr(); }" );
    }

    private void appendStaticMethods( Native n ) {
        // createFrom() method
        builder.newNewlineAppend( "public static " ).append( className ).append( " createFrom( JolieValue j ) throws TypeValidationException" ).body( () -> {
            if ( n == Native.VOID )
                builder.newlineAppend( "return new " ).append( className ).append( "();" );
            else if ( n == Native.ANY )
                builder.newlineAppend( "return new " ).append( className ).append( "( j.content() );" );
            else
                builder.newlineAppend( "return new " ).append( className ).append( "( " ).append( n.wrapperName() ).append( ".createFrom( j ).value() );" );
        } );
        // fromValue() method
        builder.newNewlineAppend( "public static " ).append( className ).append( " fromValue( Value v ) throws TypeCheckingException { " ).append(
            switch ( n ) {
                case VOID -> "JolieVoid.requireVoid( v ); return new " + className + "();";
                case ANY -> "return JolieNative.fromValue( v );";
                default -> "return new " + className + "( " + n.wrapperName() + ".fieldFromValue( v ) );";
            }
        ).append( " }" );
    }

    private void appendStaticMethods( Definition d ) {
        // createFrom() method
        builder.newNewlineAppend( "public static " ).append( className ).append( " createFrom( JolieValue j ) throws TypeValidationException { return new " ).append( className ).append( "( " ).append( d.name() ).append( ".createFrom( j ) ); }" );
        // fromValue() method
        builder.newNewlineAppend( "public static " ).append( className ).append( " fromValue( Value v ) throws TypeCheckingException { return new " ).append( className ).append( "( " ).append( d.name() ).append( ".fromValue( v ) ); }" );
    }
    
    /*
     * Convenience method to handle the fact that Basic.Inline and Native are stored in the same way.
     */
    private static JolieType type( JolieType t ) {
        return t instanceof Basic.Inline b ? b.nativeType() : t;
    }

    /*
     * Convenience method to handle the fact that Basic.Inline and Native are stored in the same way.
     */
    private static String typeName( JolieType t ) {
        return switch( type( t ) ) {
            case Native n -> n == Native.VOID ? n.wrapperName() : n.valueName();
            case Definition d -> d.name();
        };
    }
}
