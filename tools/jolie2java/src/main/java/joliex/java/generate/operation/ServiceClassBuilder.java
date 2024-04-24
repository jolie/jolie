package joliex.java.generate.operation;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;
import joliex.java.generate.JavaClassBuilder;
import joliex.java.parse.ast.JolieOperation;
import joliex.java.parse.ast.JolieOperation.RequestResponse.Fault;
import joliex.java.parse.ast.JolieType.Definition;
import joliex.java.parse.ast.JolieType.Native;
import joliex.java.parse.ast.JolieType.Definition.Basic;
import joliex.java.parse.ast.JolieType.Definition.Choice;
import joliex.java.parse.ast.JolieType.Definition.Structure;

public class ServiceClassBuilder extends JavaClassBuilder {
    
    private final String className;
    private final Map<String, Collection<JolieOperation>> operationsMap;
    private final String servicePackage;
    private final String typesPackage;
    private final String faultsPackage;
    private final String interfacesPackage;

    public ServiceClassBuilder( String className, Map<String, Collection<JolieOperation>> operationsMap, String servicePackage, String typesPackage, String faultsPackage, String interfacesPackage ) {
        this.className = className;
        this.operationsMap = operationsMap;
        this.servicePackage = servicePackage;
        this.typesPackage = typesPackage;
        this.faultsPackage = faultsPackage;
        this.interfacesPackage = interfacesPackage;
    }

    public String className() {
        return className;
    }

    public void appendHeader() {
        builder.append( "package " ).append( servicePackage ).append( ";" )
            .newline()
            .newlineAppend( "import jolie.runtime.JavaService;" )
            .newlineAppend( "import jolie.runtime.ByteArray;" )
            .newlineAppend( "import jolie.runtime.embedding.RequestResponse;" )
            .newlineAppend( "import jolie.runtime.embedding.java.JolieValue;" )
            .newlineAppend( "import jolie.runtime.embedding.java.JolieNative;" )
            .newlineAppend( "import jolie.runtime.embedding.java.JolieNative.*;" );

        Stream.of( typesPackage, faultsPackage, interfacesPackage )
            .filter( p -> p != null && !p.equals( servicePackage ) )
            .forEach( p -> builder.newlineAppend( "import " ).append( p ).append( ".*;" ) );
    }

    public void appendDefinition() {
        builder.newNewlineAppend( "public final class " ).append( className ).append( " extends JavaService implements " ).append( operationsMap.keySet().stream().reduce( (s1, s2) -> s1 + ", " + s2 ).get() )
            .body( () -> operationsMap.values().stream().flatMap( Collection::stream ).forEach( this::appendMethod ) );
    }

    private void appendMethod( JolieOperation operation ) {
        builder.newline();

        if ( operation instanceof JolieOperation.RequestResponse && operation.responseType().isEmpty() )
            builder.newlineAppend( "@RequestResponse" );

        builder.newlineAppend( "public " ).append( operation.responseType().orElse( "void" ) ).append( " " ).append( operation.name() ).append( operation.requestType().map( t -> "( " + t + " request )" ).orElse( "()" ) ).append( operation.faults().parallelStream().map( Fault::className ).reduce( (n1, n2) -> n1 + ", " + n2 ).map( s -> " throws " + s ).orElse( "" ) )
            .body( () -> {
                appendRequestUnpacking( operation );
                appendDefaultResponse( operation );
            } );
    }

    private void appendRequestUnpacking( JolieOperation operation ) {
        switch ( operation.request() ) {

            case Native n when n == Native.ANY -> builder.newlineAppend( "switch ( request )" ).body( this::appendAnyCases );

            case Choice c -> builder.newlineAppend( "switch ( request )" ).body( () -> appendChoiceCases( c ) );

            default -> {}
        }
    }

    private void appendAnyCases() {
        builder.newlineAppend( "case JolieVoid v -> {}" );
        Native.valueTypesOf( Native.ANY ).forEach( 
            t -> builder.newlineAppend( "case " ).append( t.wrapperName() ).append( "( " ).append( t.valueName() ).append( " value ) -> {}" )
        );
    }

    private void appendChoiceCases( Choice choice ) {
        choice.numberedOptions().forKeyValue( (i,t) -> {
            final String recordPattern = switch ( t ) {
                case Native n -> n == Native.VOID ? "()" : "( " + n.valueName() + " option )";
                case Definition d -> "( " + switch ( d ) { 
                    case Basic.Inline b -> b.nativeType().valueName();
                    case Structure.Inline s -> choice.name() + "." + s.name();
                    default -> d.name();
                } + " option )";
            };
            builder.newlineAppend( "case " ).append( choice.name() ).append( ".C" ).append( i ).append( recordPattern ).append( " -> {}" );
        } );
    }

    private void appendDefaultResponse( JolieOperation operation ) {
        switch( operation.response() ) {

            case Native.VOID -> builder.newlineAppend( "/* developer code */" );

            case Native.ANY -> builder.newlineAppend( "return JolieNative.of( /* TODO: create with actual value */ );" );
            
            case Basic b -> builder.newlineAppend( "return new " ).append( b.name() ).append( "( null ); /* TODO: create with actual value */" );
            
            case Structure s when s.hasBuilder() -> builder
                .newlineAppend( "return " ).append( s.name() ).append( s.nativeType() == Native.VOID ? ".builder()" : ".builder( /* content */ )" )
                .indentedNewlineAppend( "/* children */" )
                .indentedNewlineAppend( ".build();" );

            default -> builder.newlineAppend( "return null; /* TODO: return actual value */" );
        }
    }
}
