package joliex.java.parse.ast;

import java.util.List;
import java.util.Optional;
import joliex.java.generate.util.ClassPath;
import joliex.java.parse.ast.JolieType.Definition;
import joliex.java.parse.ast.JolieType.Definition.Structure.Undefined;
import joliex.java.parse.ast.JolieType.Native;

public sealed interface JolieOperation {

    String name();
    JolieType request();
    JolieType response();
    Optional<String> possibleDocumentation();

    default Optional<String> requestType( String typesPackage ) {
        return switch ( request() ) {
            case Native.VOID -> Optional.empty();
            case Native n -> Optional.of( n.nativeType() );
            case Undefined d -> Optional.of( ClassPath.JOLIEVALUE.get() );
            case Definition d -> Optional.of( typesPackage + "." + d.name() );
        };
    }

    default String responseType( String typesPackage ) {
        return switch( response() ) { 
            case Native.VOID -> "void"; 
            case Native n -> n.nativeType(); 
            case Undefined d -> ClassPath.JOLIEVALUE.get();
            case Definition d -> typesPackage + "." + d.name(); 
        };
    }

    default List<RequestResponse.Fault> faults() { return List.of(); }

    public static record OneWay( String name, JolieType request, String documentation ) implements JolieOperation {
        
        @Override
        public JolieType response() { return Native.VOID; }

        @Override
        public Optional<String> possibleDocumentation() { return Optional.ofNullable( documentation ); }
    }

    public static record RequestResponse( String name, JolieType request, JolieType response, List<Fault> faults, String documentation ) implements JolieOperation {

        @Override
        public Optional<String> possibleDocumentation() { return Optional.ofNullable( documentation ); }

        public static record Fault( String name, JolieType type, String className ) {}
    }
}
