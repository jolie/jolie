package joliex.util.faults;

import jolie.runtime.FaultException;
import joliex.java.embedding.*;

import java.util.function.Function;

public class InvalidTimestamp extends FaultException {
    
    private final JolieValue fault;
    
    public JolieValue fault() { return fault; }
    
    public InvalidTimestamp( JolieValue fault ) {
        super( "InvalidTimestamp", JolieValue.toValue( fault ) );
        this.fault = fault;
    }
    
    public static InvalidTimestamp create( Function<JolieValue.InlineBuilder, JolieValue> builder ) { return new InvalidTimestamp( builder.apply( JolieValue.construct() ) ); }
    public static InvalidTimestamp createFrom( JolieValue t, Function<JolieValue.InlineBuilder, JolieValue> rebuilder ) { return new InvalidTimestamp( rebuilder.apply( JolieValue.constructFrom( t ) ) ); }
}