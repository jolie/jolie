package joliex.util.spec.faults;

public class InvalidDate extends jolie.runtime.FaultException {
    
    private final jolie.runtime.embedding.java.JolieValue fault;
    public InvalidDate( jolie.runtime.embedding.java.JolieValue fault ) {
        super( "InvalidDate", jolie.runtime.embedding.java.JolieValue.toValue( fault ) );
        this.fault = java.util.Objects.requireNonNull( fault );
    }
    
    public jolie.runtime.embedding.java.JolieValue fault() { return fault; }
}