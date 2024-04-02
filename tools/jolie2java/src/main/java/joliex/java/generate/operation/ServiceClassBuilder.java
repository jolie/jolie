package joliex.java.generate.operation;

import java.util.Collection;
import java.util.Map;

import joliex.java.generate.JavaClassBuilder;
import joliex.java.parse.ast.JolieOperation;
import joliex.java.parse.ast.JolieOperation.RequestResponse.Fault;
import joliex.java.parse.ast.JolieType.Definition;
import joliex.java.parse.ast.JolieType.Native;
import joliex.java.parse.ast.JolieType.Definition.Basic;
import joliex.java.parse.ast.JolieType.Definition.Choice;
import joliex.java.parse.ast.JolieType.Definition.Structure;
import joliex.java.parse.ast.JolieType.Definition.Structure.Undefined;

public class ServiceClassBuilder extends JavaClassBuilder {
    
    private final String className;
    private final Map<String, Collection<JolieOperation>> operationsMap;
    private final String packageName;
    private final String typesFolder;
    private final String faultsFolder;
    private final String interfaceFolder;

    public ServiceClassBuilder( String className, Map<String, Collection<JolieOperation>> operationsMap, String packageName, String typesFolder, String faultsFolder, String interfaceFolder ) {
        this.className = className;
        this.operationsMap = operationsMap;
        this.packageName = packageName;
        this.typesFolder = typesFolder;
        this.faultsFolder = faultsFolder;
        this.interfaceFolder = interfaceFolder;
    }

    public String className() {
        return className;
    }

    public void appendHeader() {
        builder.append( "package " ).append( packageName ).append( ";" )
            .newline()
            .newlineAppend( "import jolie.runtime.JavaService;" )
            .newlineAppend( "import jolie.runtime.ByteArray;" )
            .newlineAppend( "import jolie.runtime.embedding.RequestResponse;" )
            .newline()
            .newlineAppend( "import joliex.java.embedding.*;" );

        if ( typesFolder != null )
            builder.newlineAppend( "import " ).append( packageName ).append( "." ).append( typesFolder ).append( ".*;" );

        if ( faultsFolder != null )
            builder.newlineAppend( "import " ).append( packageName ).append( "." ).append( faultsFolder ).append( ".*;" );

        if ( interfaceFolder != null )
            builder.newlineAppend( "import " ).append( packageName ).append( "." ).append( interfaceFolder ).append( ".*;" );
    }

    public void appendDefinition() {
        builder.newNewlineAppend( "public final class " ).append( className ).append( " extends JavaService implements " ).append( operationsMap.keySet().stream().reduce( (s1, s2) -> s1 + ", " + s2 ).get() )
            .body( () -> operationsMap.values().stream().flatMap( Collection::stream ).forEach( this::appendMethod ) );
    }

    private void appendMethod( JolieOperation operation ) {
        builder.newline();

        if ( operation instanceof JolieOperation.RequestResponse && operation.responseType().isEmpty() )
            builder.newlineAppend( "@RequestResponse" );

        builder.newlineAppend( "public " ).append( operation.responseType().orElse( "void" ) ).append( " " ).append( operation.name() ).append( operation.requestType().map( t -> "( " + t + " request )" ).orElse( "()" ) ).append( operation.faults().parallelStream().map( Fault::className ).reduce( (n1, n2) -> n1 + ", " + n2 ).map( s -> " throws " + s ) )
            .body( () -> {
                appendRequestUnpacking( operation );
                appendDefaultResponse( operation );
            } );
    }

    private void appendRequestUnpacking( JolieOperation operation ) {
        switch ( operation.request() ) {

            case Undefined u -> builder.newlineAppend( "final StructureType req = JolieType.toStructure( request );" );

            case Native n -> { if ( n == Native.ANY ) builder.newlineAppend( "switch ( request )" ).body( this::appendAnyCases ); }

            case Choice c -> builder.newlineAppend( "switch ( request )" ).body( () -> appendChoiceCases( c ) );

            default -> {}
        }
    }

    private void appendAnyCases() {
        builder.newlineAppend( "case BasicType.JolieVoid v -> {}" );
        Native.valueTypesOf( Native.ANY ).forEach( 
            t -> builder.newlineAppend( "case BasicType." ).append( t.wrapperName() ).append( "( " ).append( t.valueName() ).append( " value ) -> {}" )
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
        if ( operation.responseType().isEmpty() )
            builder.newlineAppend( "/* developer code */" );
        else
            switch( operation.response() ) {

                case Native n -> builder.newlineAppend( "return " ).append( n == Native.ANY ? "BasicType.create( /* value */ );" : "null; /* TODO: return actual value */" );

                case Basic b -> builder.newlineAppend( "return " ).append( b.name() ).append( ".create( null ); /* TODO: create with actual value */" );
                
                case Structure s -> builder
                    .newlineAppend( "return " ).append( s instanceof Undefined ? "StructureType" : s.name() ).append( ".construct(" ).append( s.nativeType() == Native.VOID ? "" : " /* root */ " ).append( ")" )
                    .indentedNewlineAppend( "/* fields */" )
                    .indentedNewlineAppend( ".build();" );

                case Choice c -> builder.newlineAppend( "return null; /* TODO: replace with static construction method call (e.g. \"return " ).append( c.name() ).append( ".create( VALUE );\") */" );
            }
    }
}
