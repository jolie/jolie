package joliex.util.spec.interfaces;

public interface MathInterface {
    
    /**
     *  Returns a random number d such that 0.0 <= d < 1.0. 
     */
    java.lang.Double random() throws jolie.runtime.FaultException;
    
    /**
     *  Returns the absolute value of the input integer. 
     */
    java.lang.Integer abs( java.lang.Integer request ) throws jolie.runtime.FaultException;
    
    java.lang.Double round( joliex.util.spec.types.RoundRequestType request ) throws jolie.runtime.FaultException;
    
    /**
     *  Returns the PI constant 
     */
    java.lang.Double pi() throws jolie.runtime.FaultException;
    
    /**
     *  Returns the result of .base to the power of .exponent (see request data type). 
     */
    java.lang.Double pow( joliex.util.spec.types.PowRequest request ) throws jolie.runtime.FaultException;
    
    /**
     *  Returns the summation of values from .from to .to (see request data type). For example, .from=2 and .to=5 would produce a return value of 2+3+4+5=14. 
     */
    java.lang.Integer summation( joliex.util.spec.types.SummationRequest request ) throws jolie.runtime.FaultException;
}