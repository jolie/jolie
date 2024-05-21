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
    private final String faultsPackage;

    private final Optional<String> typeName;

    public ExceptionClassBuilder( Fault fault, String faultsPackage, String typesPackage ) {
        this.fault = fault;
        this.faultsPackage = faultsPackage;
        
        typeName = Optional.ofNullable( switch( fault.type() ) {
            case Native n -> n == Native.VOID ? null : n.nativeType();
            case Undefined d -> ClassPath.JOLIEVALUE.get();
            case Definition d -> typesPackage + "." + d.name();
        } );
    }

    public String className() { return fault.className(); }

    public void appendPackage() { 
        builder.append( "package " ).append( faultsPackage ).append( ";" );
    }

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
                            case Definition d -> tn + ".toValue( fault )";
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
