package joliex.util.spec.types;

/**
 * this class is a {@link jolie.runtime.embedding.java.TypedStructure} which can be described as follows:
 * <pre>
 * 
 * contentValue: {@link java.lang.String}
     * end[0,1]: {@link java.lang.Integer}
     * begin: {@link java.lang.Integer}
 * </pre>
 * 
 * @see jolie.runtime.embedding.java.JolieValue
 * @see jolie.runtime.embedding.java.JolieNative
 */
public final class SubStringRequest extends jolie.runtime.embedding.java.TypedStructure {
    
    private static final java.util.Set<java.lang.String> FIELD_KEYS = fieldKeys( SubStringRequest.class );
    
    private final java.lang.String contentValue;
    @jolie.runtime.embedding.java.util.JolieName("end")
    private final java.lang.Integer end;
    @jolie.runtime.embedding.java.util.JolieName("begin")
    private final java.lang.Integer begin;
    
    public SubStringRequest( java.lang.String contentValue, java.lang.Integer end, java.lang.Integer begin ) {
        this.contentValue = jolie.runtime.embedding.java.util.ValueManager.validated( "contentValue", contentValue );
        this.end = end;
        this.begin = jolie.runtime.embedding.java.util.ValueManager.validated( "begin", begin );
    }
    
    public java.lang.String contentValue() { return contentValue; }
    public java.util.Optional<java.lang.Integer> end() { return java.util.Optional.ofNullable( end ); }
    public java.lang.Integer begin() { return begin; }
    
    public jolie.runtime.embedding.java.JolieNative.JolieString content() { return new jolie.runtime.embedding.java.JolieNative.JolieString( contentValue ); }
    
    public static SubStringRequest from( jolie.runtime.embedding.java.JolieValue j ) throws jolie.runtime.embedding.java.TypeValidationException {
        return new SubStringRequest(
            jolie.runtime.embedding.java.JolieNative.JolieString.from( j ).value(),
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "end" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieInt content ? content.value() : null ),
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "begin" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieInt content ? content.value() : null )
        );
    }
    
    public static SubStringRequest fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
        jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
        return new SubStringRequest(
            jolie.runtime.embedding.java.JolieNative.JolieString.contentFromValue( v ),
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "end", jolie.runtime.embedding.java.JolieNative.JolieInt::fieldFromValue ),
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "begin", jolie.runtime.embedding.java.JolieNative.JolieInt::fieldFromValue )
        );
    }
    
    public static jolie.runtime.Value toValue( SubStringRequest t ) {
        final jolie.runtime.Value v = jolie.runtime.Value.create( t.contentValue() );
        
        t.end().ifPresent( c -> v.getFirstChild( "end" ).setValue( c ) );
        v.getFirstChild( "begin" ).setValue( t.begin() );
        
        return v;
    }
}