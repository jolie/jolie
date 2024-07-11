package joliex.java.generate.operation;

import java.util.Optional;
import joliex.java.generate.JavaClassBuilder;
import joliex.java.generate.util.ClassPath;
import joliex.java.parse.ast.JolieOperation.RequestResponse.Fault;
import joliex.java.parse.ast.JolieType.Definition;
import joliex.java.parse.ast.JolieType.Definition.Structure.Undefined;
import joliex.java.parse.ast.JolieType.Native;

public class ExceptionClassBuilder extends JavaClassBuilder {

    private final Fault fault;
    private final String packageName;

    private final Optional<String> typeName;

    public ExceptionClassBuilder( Fault fault, String packageName, String typesPackage ) {
        this.fault = fault;
        this.packageName = packageName;
        
        typeName = Optional.ofNullable( switch( fault.type() ) {
            case Native n -> n == Native.VOID ? null : n.nativeType();
            case Undefined d -> d.name();
            case Definition d -> typesPackage + "." + d.name();
        } );
    }

    @Override
    public String className() { return fault.className(); }

    @Override
    public void appendPackage() { 
        builder.append( "package " ).append( packageName ).append( ";" );
    }

    @Override
    public void appendDefinition() {
        builder.newNewlineAppend( "public class " ).append( className() ).append( " extends " ).append( ClassPath.FAULTEXCEPTION ).body( () -> {
            appendAttributes();
            appendConstructors();
            appendMethods();
        } );
    }

    private void appendAttributes() {
        typeName.ifPresent( tn -> builder.newNewlineAppend( "private final " ).append( tn ).append( " fault;" ) );
    }

    private void appendConstructors() {
        builder.newlineAppend( "public " ).append( className() ).append( typeName.map( n -> "( " + n + " fault )" ).orElse( "()" ) ).body( () -> {
            typeName.ifPresentOrElse(
                tn -> builder.newlineAppend( "super( \"" ).append( fault.name() ).append( "\", " )
                        .append( switch ( fault.type() ) { 
                            case Native n -> n == Native.ANY ? ClassPath.JOLIENATIVE.get() + ".toValue( fault )" : ClassPath.VALUE.get() + ".create( fault )";
                            default -> tn + ".toValue( fault )";
                        } ).append( " );" )
                    .newlineAppend( "this.fault = " ).append( ClassPath.OBJECTS ).append( ".requireNonNull( fault );" ),
                () -> builder.newlineAppend( "super( \"" ).append( fault.name() ).append( "\" );" )
            );
        } );
    }

    private void appendMethods() {
        typeName.ifPresent( tn -> builder.newNewlineAppend( "public " ).append( tn ).append( " fault() { return fault; }" ) );
    }
}
