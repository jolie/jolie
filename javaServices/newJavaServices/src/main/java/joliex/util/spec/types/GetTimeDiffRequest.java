package joliex.util.spec.types;

/**
 * this class is a {@link jolie.runtime.embedding.java.TypedStructure} which can be described as follows:
 * <pre>
 * time1: {@link java.lang.String}
 * time2: {@link java.lang.String}
 * </pre>
 * 
 * @see jolie.runtime.embedding.java.JolieValue
 * @see jolie.runtime.embedding.java.JolieNative
 */
public final class GetTimeDiffRequest extends jolie.runtime.embedding.java.TypedStructure {
    
    private static final java.util.Set<java.lang.String> FIELD_KEYS = fieldKeys( GetTimeDiffRequest.class );
    
    @jolie.runtime.embedding.java.util.JolieName("time1")
    private final java.lang.String time1;
    @jolie.runtime.embedding.java.util.JolieName("time2")
    private final java.lang.String time2;
    
    public GetTimeDiffRequest( java.lang.String time1, java.lang.String time2 ) {
        this.time1 = jolie.runtime.embedding.java.util.ValueManager.validated( "time1", time1 );
        this.time2 = jolie.runtime.embedding.java.util.ValueManager.validated( "time2", time2 );
    }
    
    public java.lang.String time1() { return time1; }
    public java.lang.String time2() { return time2; }
    
    public jolie.runtime.embedding.java.JolieNative.JolieVoid content() { return new jolie.runtime.embedding.java.JolieNative.JolieVoid(); }
    
    public static GetTimeDiffRequest from( jolie.runtime.embedding.java.JolieValue j ) throws jolie.runtime.embedding.java.TypeValidationException {
        return new GetTimeDiffRequest(
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "time1" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieString content ? content.value() : null ),
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "time2" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieString content ? content.value() : null )
        );
    }
    
    public static GetTimeDiffRequest fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
        jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
        return new GetTimeDiffRequest(
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "time1", jolie.runtime.embedding.java.JolieNative.JolieString::fieldFromValue ),
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "time2", jolie.runtime.embedding.java.JolieNative.JolieString::fieldFromValue )
        );
    }
    
    public static jolie.runtime.Value toValue( GetTimeDiffRequest t ) {
        final jolie.runtime.Value v = jolie.runtime.Value.create();
        
        v.getFirstChild( "time1" ).setValue( t.time1() );
        v.getFirstChild( "time2" ).setValue( t.time2() );
        
        return v;
    }
}