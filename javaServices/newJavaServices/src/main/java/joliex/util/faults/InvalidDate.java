package joliex.util.faults;

import jolie.runtime.FaultException;
import joliex.java.embedding.*;

import java.util.function.Function;

public class InvalidDate extends FaultException {
    
    private final JolieValue fault;
    
    public JolieValue fault() { return fault; }
    
    public InvalidDate( JolieValue fault ) {
        super( "InvalidDate", JolieValue.toValue( fault ) );
        this.fault = fault;
    }
    
    public static InvalidDate create( Function<JolieValue.InlineBuilder, JolieValue> builder ) { return new InvalidDate( builder.apply( JolieValue.construct() ) ); }
    public static InvalidDate createFrom( JolieValue t, Function<JolieValue.InlineBuilder, JolieValue> rebuilder ) { return new InvalidDate( rebuilder.apply( JolieValue.constructFrom( t ) ) ); }
}