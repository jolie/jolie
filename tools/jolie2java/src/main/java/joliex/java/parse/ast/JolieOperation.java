package joliex.java.parse.ast;

import java.util.List;
import java.util.Optional;

import joliex.java.parse.ast.JolieType.Definition;
import joliex.java.parse.ast.JolieType.Native;

public sealed interface JolieOperation {

    String name();
    JolieType request();
    JolieType response();
    Optional<String> possibleDocumentation();

    default Optional<String> requestType() { 
        return switch ( request() ) {
            case Native n -> n == Native.VOID ? Optional.empty() : Optional.of( n.valueName() );
            case Definition d -> Optional.of( d.name() );
        }; 
    }

    default Optional<String> responseType() { 
        return switch ( response() ) {
            case Native n -> n == Native.VOID ? Optional.empty() : Optional.of( n.valueName() );
            case Definition d -> Optional.of( d.name() );
        }; 
    }

    default List<RequestResponse.Fault> faults() { return List.of(); }

    public static record OneWay( String name, JolieType request, String documentation ) implements JolieOperation {
        
        public JolieType response() { return Native.VOID; }
        public Optional<String> responseType() { return Optional.empty(); }

        public Optional<String> possibleDocumentation() { return Optional.ofNullable( documentation ); }
    }

    public static record RequestResponse( String name, JolieType request, JolieType response, List<Fault> faults, String documentation ) implements JolieOperation {

        public Optional<String> possibleDocumentation() { return Optional.ofNullable( documentation ); }

        public static record Fault( String name, JolieType type, String className ) {

            public boolean equals( Object obj ) { return obj != null && obj instanceof Fault f && name.equals( f.name() ) && type.equals( f.type() ); }
        }
    }
}
