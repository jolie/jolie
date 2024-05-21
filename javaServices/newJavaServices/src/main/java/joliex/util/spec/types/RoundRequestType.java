package joliex.util.spec.types;

/**
 * this class is a {@link jolie.runtime.embedding.java.TypedStructure} which can be described as follows:
 * <pre>
 * 
 * contentValue: {@link java.lang.Double}
     * decimals[0,1]: {@link java.lang.Integer}
 * </pre>
 * 
 * @see jolie.runtime.embedding.java.JolieValue
 * @see jolie.runtime.embedding.java.JolieNative
 */
public final class RoundRequestType extends jolie.runtime.embedding.java.TypedStructure {
    
    private static final java.util.Set<java.lang.String> FIELD_KEYS = fieldKeys( RoundRequestType.class );
    
    private final java.lang.Double contentValue;
    @jolie.runtime.embedding.java.util.JolieName("decimals")
    private final java.lang.Integer decimals;
    
    public RoundRequestType( java.lang.Double contentValue, java.lang.Integer decimals ) {
        this.contentValue = jolie.runtime.embedding.java.util.ValueManager.validated( "contentValue", contentValue );
        this.decimals = decimals;
    }
    
    public java.lang.Double contentValue() { return contentValue; }
    public java.util.Optional<java.lang.Integer> decimals() { return java.util.Optional.ofNullable( decimals ); }
    
    public jolie.runtime.embedding.java.JolieNative.JolieDouble content() { return new jolie.runtime.embedding.java.JolieNative.JolieDouble( contentValue ); }
    
    public static RoundRequestType from( jolie.runtime.embedding.java.JolieValue j ) throws jolie.runtime.embedding.java.TypeValidationException {
        return new RoundRequestType(
            jolie.runtime.embedding.java.JolieNative.JolieDouble.from( j ).value(),
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "decimals" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieInt content ? content.value() : null )
        );
    }
    
    public static RoundRequestType fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
        jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
        return new RoundRequestType(
            jolie.runtime.embedding.java.JolieNative.JolieDouble.contentFromValue( v ),
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "decimals", jolie.runtime.embedding.java.JolieNative.JolieInt::fieldFromValue )
        );
    }
    
    public static jolie.runtime.Value toValue( RoundRequestType t ) {
        final jolie.runtime.Value v = jolie.runtime.Value.create( t.contentValue() );
        
        t.decimals().ifPresent( c -> v.getFirstChild( "decimals" ).setValue( c ) );
        
        return v;
    }
}