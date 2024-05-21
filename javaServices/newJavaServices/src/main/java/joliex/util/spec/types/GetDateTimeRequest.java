package joliex.util.spec.types;

/**
 * this class is a {@link jolie.runtime.embedding.java.TypedStructure} which can be described as follows:
 * <pre>
 * 
 * contentValue: {@link java.lang.Long}
     * format[0,1]: {@link java.lang.String}
 * </pre>
 * 
 * @see jolie.runtime.embedding.java.JolieValue
 * @see jolie.runtime.embedding.java.JolieNative
 */
public final class GetDateTimeRequest extends jolie.runtime.embedding.java.TypedStructure {
    
    private static final java.util.Set<java.lang.String> FIELD_KEYS = fieldKeys( GetDateTimeRequest.class );
    
    private final java.lang.Long contentValue;
    @jolie.runtime.embedding.java.util.JolieName("format")
    private final java.lang.String format;
    
    public GetDateTimeRequest( java.lang.Long contentValue, java.lang.String format ) {
        this.contentValue = jolie.runtime.embedding.java.util.ValueManager.validated( "contentValue", contentValue );
        this.format = format;
    }
    
    public java.lang.Long contentValue() { return contentValue; }
    public java.util.Optional<java.lang.String> format() { return java.util.Optional.ofNullable( format ); }
    
    public jolie.runtime.embedding.java.JolieNative.JolieLong content() { return new jolie.runtime.embedding.java.JolieNative.JolieLong( contentValue ); }
    
    public static GetDateTimeRequest from( jolie.runtime.embedding.java.JolieValue j ) throws jolie.runtime.embedding.java.TypeValidationException {
        return new GetDateTimeRequest(
            jolie.runtime.embedding.java.JolieNative.JolieLong.from( j ).value(),
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "format" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieString content ? content.value() : null )
        );
    }
    
    public static GetDateTimeRequest fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
        jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
        return new GetDateTimeRequest(
            jolie.runtime.embedding.java.JolieNative.JolieLong.contentFromValue( v ),
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "format", jolie.runtime.embedding.java.JolieNative.JolieString::fieldFromValue )
        );
    }
    
    public static jolie.runtime.Value toValue( GetDateTimeRequest t ) {
        final jolie.runtime.Value v = jolie.runtime.Value.create( t.contentValue() );
        
        t.format().ifPresent( c -> v.getFirstChild( "format" ).setValue( c ) );
        
        return v;
    }
}