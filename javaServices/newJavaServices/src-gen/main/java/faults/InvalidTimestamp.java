package faults;

import java.util.Objects;

import jolie.runtime.Value;
import jolie.runtime.ByteArray;
import jolie.runtime.FaultException;
import jolie.runtime.embedding.java.JolieValue;
import types.*;
import jolie.runtime.embedding.java.JolieNative;

public class InvalidTimestamp extends FaultException {
    
    private final JolieValue fault;
    public InvalidTimestamp( JolieValue fault ) {
        super( "InvalidTimestamp", JolieValue.toValue( fault ) );
        this.fault = Objects.requireNonNull( fault );
    }
    
    public JolieValue fault() { return fault; }
}