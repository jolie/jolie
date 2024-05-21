package joliex.util.spec.types;

/**
 * this class is a {@link jolie.runtime.embedding.java.TypedStructure} which can be described as follows:
 * <pre>
 * from: {@link java.lang.Integer}
 * to: {@link java.lang.Integer}
 * </pre>
 * 
 * @see jolie.runtime.embedding.java.JolieValue
 * @see jolie.runtime.embedding.java.JolieNative
 */
public final class SummationRequest extends jolie.runtime.embedding.java.TypedStructure {
    
    private static final java.util.Set<java.lang.String> FIELD_KEYS = fieldKeys( SummationRequest.class );
    
    @jolie.runtime.embedding.java.util.JolieName("from")
    private final java.lang.Integer from;
    @jolie.runtime.embedding.java.util.JolieName("to")
    private final java.lang.Integer to;
    
    public SummationRequest( java.lang.Integer from, java.lang.Integer to ) {
        this.from = jolie.runtime.embedding.java.util.ValueManager.validated( "from", from );
        this.to = jolie.runtime.embedding.java.util.ValueManager.validated( "to", to );
    }
    
    public java.lang.Integer from() { return from; }
    public java.lang.Integer to() { return to; }
    
    public jolie.runtime.embedding.java.JolieNative.JolieVoid content() { return new jolie.runtime.embedding.java.JolieNative.JolieVoid(); }
    
    public static SummationRequest from( jolie.runtime.embedding.java.JolieValue j ) throws jolie.runtime.embedding.java.TypeValidationException {
        return new SummationRequest(
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "from" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieInt content ? content.value() : null ),
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "to" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieInt content ? content.value() : null )
        );
    }
    
    public static SummationRequest fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
        jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
        return new SummationRequest(
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "from", jolie.runtime.embedding.java.JolieNative.JolieInt::fieldFromValue ),
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "to", jolie.runtime.embedding.java.JolieNative.JolieInt::fieldFromValue )
        );
    }
    
    public static jolie.runtime.Value toValue( SummationRequest t ) {
        final jolie.runtime.Value v = jolie.runtime.Value.create();
        
        v.getFirstChild( "from" ).setValue( t.from() );
        v.getFirstChild( "to" ).setValue( t.to() );
        
        return v;
    }
}