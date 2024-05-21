package joliex.util.spec.types;

/**
 * this class is a {@link jolie.runtime.embedding.java.TypedStructure} which can be described as follows:
 * <pre>
 * base: {@link java.lang.Double}
 * exponent: {@link java.lang.Double}
 * </pre>
 * 
 * @see jolie.runtime.embedding.java.JolieValue
 * @see jolie.runtime.embedding.java.JolieNative
 */
public final class PowRequest extends jolie.runtime.embedding.java.TypedStructure {
    
    private static final java.util.Set<java.lang.String> FIELD_KEYS = fieldKeys( PowRequest.class );
    
    @jolie.runtime.embedding.java.util.JolieName("base")
    private final java.lang.Double base;
    @jolie.runtime.embedding.java.util.JolieName("exponent")
    private final java.lang.Double exponent;
    
    public PowRequest( java.lang.Double base, java.lang.Double exponent ) {
        this.base = jolie.runtime.embedding.java.util.ValueManager.validated( "base", base );
        this.exponent = jolie.runtime.embedding.java.util.ValueManager.validated( "exponent", exponent );
    }
    
    public java.lang.Double base() { return base; }
    public java.lang.Double exponent() { return exponent; }
    
    public jolie.runtime.embedding.java.JolieNative.JolieVoid content() { return new jolie.runtime.embedding.java.JolieNative.JolieVoid(); }
    
    public static PowRequest from( jolie.runtime.embedding.java.JolieValue j ) throws jolie.runtime.embedding.java.TypeValidationException {
        return new PowRequest(
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "base" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieDouble content ? content.value() : null ),
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "exponent" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieDouble content ? content.value() : null )
        );
    }
    
    public static PowRequest fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
        jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
        return new PowRequest(
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "base", jolie.runtime.embedding.java.JolieNative.JolieDouble::fieldFromValue ),
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "exponent", jolie.runtime.embedding.java.JolieNative.JolieDouble::fieldFromValue )
        );
    }
    
    public static jolie.runtime.Value toValue( PowRequest t ) {
        final jolie.runtime.Value v = jolie.runtime.Value.create();
        
        v.getFirstChild( "base" ).setValue( t.base() );
        v.getFirstChild( "exponent" ).setValue( t.exponent() );
        
        return v;
    }
}