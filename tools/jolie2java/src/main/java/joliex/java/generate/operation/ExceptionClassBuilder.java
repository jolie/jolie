package joliex.java.generate.operation;

import java.util.Optional;
import joliex.java.generate.JavaClassBuilder;
import joliex.java.parse.ast.JolieOperation.RequestResponse.Fault;
import joliex.java.parse.ast.JolieType.Definition;
import joliex.java.parse.ast.JolieType.Native;

public class ExceptionClassBuilder extends JavaClassBuilder {

    private final Fault fault;
    private final String packageName;
    private final String typesFolder;
    private final String faultsFolder;

    private final Optional<String> typeName;

    public ExceptionClassBuilder( Fault fault, String packageName, String typesFolder, String faultsFolder ) {
        this.fault = fault;
        this.packageName = packageName;
        this.typesFolder = typesFolder;
        this.faultsFolder = faultsFolder;

        typeName = Optional.ofNullable( switch( fault.type() ) {
            case Native n -> n == Native.VOID ? null : n.valueName();
            case Definition d -> d.name();
        } );
    }

    public String className() { return fault.className(); }

    public void appendHeader() { 
        builder.append( "package " ).append( packageName ).append( "." ).append( faultsFolder ).append( ";" )
            .newline()
            .newlineAppend( "import java.util.Objects;" )
            .newline()
            .newlineAppend( "import jolie.runtime.Value;" )
            .newlineAppend( "import jolie.runtime.ByteArray;" )
            .newlineAppend( "import jolie.runtime.FaultException;" )
            .newline()
            .newlineAppend( "import joliex.java.embedding.JolieValue;" )
            .newlineAppend( "import joliex.java.embedding.JolieNative;" );

        if ( fault.type() instanceof Definition )
            builder.newNewlineAppend( "import " ).append( packageName ).append( "." ).append( typesFolder ).append( ".*;" );
    }

    public void appendDefinition() {
        builder.newNewlineAppend( "public class " ).append( className() ).append( " extends FaultException" ).body( () -> {
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
                            case Native n -> n == Native.ANY ? "JolieNative.toValue( fault )" : "Value.create( fault )";
                            case Definition d -> d.name() + ".toValue( fault )";
                        } ).append( " );" )
                    .newlineAppend( "this.fault = Objects.requireNonNull( fault );" ), 
                () -> builder.newlineAppend( "super( \"" ).append( fault.name() ).append( "\" );" )
            );
        } );
    }

    private void appendMethods() {
        typeName.ifPresent( tn -> builder.newNewlineAppend( "public " ).append( tn ).append( " fault() { return fault; }" ) );
    }
}
