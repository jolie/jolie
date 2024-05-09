package joliex.util.spec.faults;

import java.util.Objects;

import jolie.runtime.Value;
import jolie.runtime.ByteArray;
import jolie.runtime.FaultException;
import jolie.runtime.embedding.java.JolieValue;
import jolie.runtime.embedding.java.JolieNative;

import joliex.util.spec.types.*;

public class InvalidTimestamp extends FaultException {
    
    private final JolieValue fault;
    public InvalidTimestamp( JolieValue fault ) {
        super( "InvalidTimestamp", JolieValue.toValue( fault ) );
        this.fault = Objects.requireNonNull( fault );
    }
    
    public JolieValue fault() { return fault; }
}