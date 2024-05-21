package joliex.util.spec.types;

/**
 * this class is a {@link jolie.runtime.embedding.java.TypedStructure} which can be described as follows:
 * <pre>
 * 
 * contentValue: {@link java.lang.String}
     * charset[0,1]: {@link java.lang.String}
 * </pre>
 * 
 * @see jolie.runtime.embedding.java.JolieValue
 * @see jolie.runtime.embedding.java.JolieNative
 */
public final class UrlDecodeRequest extends jolie.runtime.embedding.java.TypedStructure {
    
    private static final java.util.Set<java.lang.String> FIELD_KEYS = fieldKeys( UrlDecodeRequest.class );
    
    private final java.lang.String contentValue;
    @jolie.runtime.embedding.java.util.JolieName("charset")
    private final java.lang.String charset;
    
    public UrlDecodeRequest( java.lang.String contentValue, java.lang.String charset ) {
        this.contentValue = jolie.runtime.embedding.java.util.ValueManager.validated( "contentValue", contentValue );
        this.charset = charset;
    }
    
    public java.lang.String contentValue() { return contentValue; }
    public java.util.Optional<java.lang.String> charset() { return java.util.Optional.ofNullable( charset ); }
    
    public jolie.runtime.embedding.java.JolieNative.JolieString content() { return new jolie.runtime.embedding.java.JolieNative.JolieString( contentValue ); }
    
    public static UrlDecodeRequest from( jolie.runtime.embedding.java.JolieValue j ) throws jolie.runtime.embedding.java.TypeValidationException {
        return new UrlDecodeRequest(
            jolie.runtime.embedding.java.JolieNative.JolieString.from( j ).value(),
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "charset" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieString content ? content.value() : null )
        );
    }
    
    public static UrlDecodeRequest fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
        jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
        return new UrlDecodeRequest(
            jolie.runtime.embedding.java.JolieNative.JolieString.contentFromValue( v ),
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "charset", jolie.runtime.embedding.java.JolieNative.JolieString::fieldFromValue )
        );
    }
    
    public static jolie.runtime.Value toValue( UrlDecodeRequest t ) {
        final jolie.runtime.Value v = jolie.runtime.Value.create( t.contentValue() );
        
        t.charset().ifPresent( c -> v.getFirstChild( "charset" ).setValue( c ) );
        
        return v;
    }
}