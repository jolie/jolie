package joliex.util.spec.types;

/**
 * this class is a {@link jolie.runtime.embedding.java.TypedStructure} which can be described as follows:
 * <pre>
 * 
 * contentValue: {@link java.lang.String}
     * suffix: {@link java.lang.String}
 * </pre>
 * 
 * @see jolie.runtime.embedding.java.JolieValue
 * @see jolie.runtime.embedding.java.JolieNative
 */
public final class EndsWithRequest extends jolie.runtime.embedding.java.TypedStructure {
    
    private static final java.util.Set<java.lang.String> FIELD_KEYS = fieldKeys( EndsWithRequest.class );
    
    private final java.lang.String contentValue;
    @jolie.runtime.embedding.java.util.JolieName("suffix")
    private final java.lang.String suffix;
    
    public EndsWithRequest( java.lang.String contentValue, java.lang.String suffix ) {
        this.contentValue = jolie.runtime.embedding.java.util.ValueManager.validated( "contentValue", contentValue );
        this.suffix = jolie.runtime.embedding.java.util.ValueManager.validated( "suffix", suffix );
    }
    
    public java.lang.String contentValue() { return contentValue; }
    public java.lang.String suffix() { return suffix; }
    
    public jolie.runtime.embedding.java.JolieNative.JolieString content() { return new jolie.runtime.embedding.java.JolieNative.JolieString( contentValue ); }
    
    public static EndsWithRequest from( jolie.runtime.embedding.java.JolieValue j ) throws jolie.runtime.embedding.java.TypeValidationException {
        return new EndsWithRequest(
            jolie.runtime.embedding.java.JolieNative.JolieString.from( j ).value(),
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "suffix" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieString content ? content.value() : null )
        );
    }
    
    public static EndsWithRequest fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
        jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
        return new EndsWithRequest(
            jolie.runtime.embedding.java.JolieNative.JolieString.contentFromValue( v ),
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "suffix", jolie.runtime.embedding.java.JolieNative.JolieString::fieldFromValue )
        );
    }
    
    public static jolie.runtime.Value toValue( EndsWithRequest t ) {
        final jolie.runtime.Value v = jolie.runtime.Value.create( t.contentValue() );
        
        v.getFirstChild( "suffix" ).setValue( t.suffix() );
        
        return v;
    }
}