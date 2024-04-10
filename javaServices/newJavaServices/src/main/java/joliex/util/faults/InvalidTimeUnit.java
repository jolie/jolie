package joliex.util.faults;

import jolie.runtime.FaultException;
import joliex.java.embedding.*;

import java.util.function.Function;

public class InvalidTimeUnit extends FaultException {
    
    private final JolieValue fault;
    
    public JolieValue fault() { return fault; }
    
    public InvalidTimeUnit( JolieValue fault ) {
        super( "InvalidTimeUnit", JolieValue.toValue( fault ) );
        this.fault = fault;
    }
    
    public static InvalidTimeUnit create( Function<JolieValue.InlineBuilder, JolieValue> builder ) { return new InvalidTimeUnit( builder.apply( JolieValue.construct() ) ); }
    public static InvalidTimeUnit createFrom( JolieValue t, Function<JolieValue.InlineBuilder, JolieValue> rebuilder ) { return new InvalidTimeUnit( rebuilder.apply( JolieValue.constructFrom( t ) ) ); }
}