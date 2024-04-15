package joliex.util.faults;

import java.util.Objects;

import jolie.runtime.Value;
import jolie.runtime.ByteArray;
import jolie.runtime.FaultException;

import joliex.java.embedding.JolieValue;
import joliex.java.embedding.JolieNative;

import joliex.util.types.*;

public class InvalidTimestamp extends FaultException {
    
    private final JolieValue fault;
    public InvalidTimestamp( JolieValue fault ) {
        super( "InvalidTimestamp", JolieValue.toValue( fault ) );
        this.fault = Objects.requireNonNull( fault );
    }
    
    public JolieValue fault() { return fault; }
}