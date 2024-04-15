package joliex.util.faults;

import java.util.Objects;

import jolie.runtime.Value;
import jolie.runtime.ByteArray;
import jolie.runtime.FaultException;

import joliex.java.embedding.JolieValue;
import joliex.java.embedding.JolieNative;

import joliex.util.types.*;

public class InvalidTimeUnit extends FaultException {
    
    private final JolieValue fault;
    public InvalidTimeUnit( JolieValue fault ) {
        super( "InvalidTimeUnit", JolieValue.toValue( fault ) );
        this.fault = Objects.requireNonNull( fault );
    }
    
    public JolieValue fault() { return fault; }
}