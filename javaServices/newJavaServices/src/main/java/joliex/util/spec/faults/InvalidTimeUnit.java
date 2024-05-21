package joliex.util.spec.faults;

public class InvalidTimeUnit extends jolie.runtime.FaultException {
    
    private final jolie.runtime.embedding.java.JolieValue fault;
    public InvalidTimeUnit( jolie.runtime.embedding.java.JolieValue fault ) {
        super( "InvalidTimeUnit", jolie.runtime.embedding.java.JolieValue.toValue( fault ) );
        this.fault = java.util.Objects.requireNonNull( fault );
    }
    
    public jolie.runtime.embedding.java.JolieValue fault() { return fault; }
}