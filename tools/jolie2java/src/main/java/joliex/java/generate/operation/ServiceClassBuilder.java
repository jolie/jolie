package joliex.java.generate.operation;

import java.util.Collection;
import java.util.Map;
import joliex.java.generate.JavaClassBuilder;
import joliex.java.generate.util.ClassPath;
import joliex.java.parse.ast.JolieOperation;
import joliex.java.parse.ast.JolieType.Definition;
import joliex.java.parse.ast.JolieType.Native;
import joliex.java.parse.ast.JolieType.Definition.Basic;
import joliex.java.parse.ast.JolieType.Definition.Choice;
import joliex.java.parse.ast.JolieType.Definition.Structure;

public class ServiceClassBuilder extends JavaClassBuilder {
    
    private final Map<String, Collection<JolieOperation>> operationsMap;
    private final String className;
    private final String packageName;
    private final String typesPackage;
    private final String faultsPackage;
    private final String interfacePackage;

    public ServiceClassBuilder( Map<String, Collection<JolieOperation>> operationsMap, String className, String packageName, String typesPackage, String faultsPackage, String interfacePackage ) {
        this.operationsMap = operationsMap;
        this.className = className;
        this.packageName = packageName;
        this.typesPackage = typesPackage;
        this.faultsPackage = faultsPackage;
        this.interfacePackage = interfacePackage;
    }

    public String className() {
        return className;
    }

    public void appendPackage() {
        builder.append( "package " ).append( packageName ).append( ";" );
    }

    public void appendDefinition() {
        builder.newNewlineAppend( "public final class " ).append( className ).append( " extends " ).append( ClassPath.JAVASERVICE ).append( " implements " ).append( operationsMap.keySet().parallelStream().map( k -> interfacePackage + "." + k ).reduce( (s1, s2) -> s1 + ", " + s2 ).get() )
            .body( () -> operationsMap.values().stream().flatMap( Collection::stream ).forEach( this::appendMethod ) );
    }

    private void appendMethod( JolieOperation operation ) {
        builder.newNewlineAppend( "public " ).append( operation.responseType( typesPackage ) ).append( " " ).append( operation.name() ).append( operation.requestType( typesPackage ).map( t -> "( " + t + " request )" ).orElse( "()" ) ).append( operation.faults().parallelStream().map( f -> faultsPackage + "." + f.className() ).reduce( (n1, n2) -> n1 + ", " + n2 ).map( s -> " throws " + s ).orElse( "" ) )
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
        builder.newlineAppend( "case " ).append( ClassPath.JOLIEVOID ).append( " v -> {}" );
        Native.nativeClassesOf( Native.ANY ).forEach(
            t -> builder.newlineAppend( "case " ).append( t.wrapperType() ).append( "( " ).append( t.nativeType() ).append( " value ) -> {}" )
        );
    }

    private void appendChoiceCases( Choice choice ) {
        choice.numberedOptions().forKeyValue( (i,t) -> {
            final String recordPattern = switch ( t ) {
                case Native n -> n == Native.VOID ? "()" : "( " + n.nativeType() + " option )";
                case Definition d -> "( " + switch ( d ) { 
                    case Basic.Inline b -> b.type().nativeClass().get();
                    case Structure.Inline s -> typesPackage + "." + choice.name() + "." + s.name();
                    default -> typesPackage + "." + d.name();
                } + " option )";
            };
            builder.newlineAppend( "case " ).append( typesPackage ).append( "." ).append( choice.name() ).append( ".C" ).append( i ).append( recordPattern ).append( " -> {}" );
        } );
    }

    private void appendDefaultResponse( JolieOperation operation ) {
        switch( operation.response() ) {

            case Native.VOID -> builder.newlineAppend( "/* developer code */" );

            case Native.ANY -> builder.newlineAppend( "return " ).append( ClassPath.JOLIENATIVE ).append( ".of( /* TODO: create with actual value */ );" );
            
            case Basic b -> builder.newlineAppend( "return new " ).append( typesPackage ).append( "." ).append( b.name() ).append( "( null ); /* TODO: create with actual value */" );
            
            case Structure s when s.hasBuilder() -> builder
                .newlineAppend( "return " ).append( s instanceof Structure.Undefined ? ClassPath.JOLIEVALUE.get() : typesPackage + "." + s.name() ).append( s.contentType() == Native.VOID ? ".builder()" : ".builder( /* content */ )" )
                .indentedNewlineAppend( "/* children */" )
                .indentedNewlineAppend( ".build();" );

            default -> builder.newlineAppend( "return null; /* TODO: return actual value */" );
        }
    }
}
