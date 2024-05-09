package joliex.util.spec.interfaces;

import jolie.runtime.ByteArray;
import jolie.runtime.FaultException;
import jolie.runtime.embedding.java.JolieValue;
import jolie.runtime.embedding.java.JolieNative;
import joliex.util.spec.types.*;

public interface MathInterface {
    
    /**
     *  Returns a random number d such that 0.0 <= d < 1.0. 
     */
    Double random() throws FaultException;
    
    /**
     *  Returns the absolute value of the input integer. 
     */
    Integer abs( Integer request ) throws FaultException;
    
    Double round( RoundRequestType request ) throws FaultException;
    
    /**
     *  Returns the PI constant 
     */
    Double pi() throws FaultException;
    
    /**
     *  Returns the result of .base to the power of .exponent (see request data type). 
     */
    Double pow( PowRequest request ) throws FaultException;
    
    /**
     *  Returns the summation of values from .from to .to (see request data type). For example, .from=2 and .to=5 would produce a return value of 2+3+4+5=14. 
     */
    Integer summation( SummationRequest request ) throws FaultException;
}