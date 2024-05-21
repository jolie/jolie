package joliex.util.spec.types;

/**
 * this class is a {@link jolie.runtime.embedding.java.TypedStructure} which can be described as follows:
 * <pre>
 * 
 * contentValue: {@link java.lang.Integer}
     * message[0,1]: {@link jolie.runtime.embedding.java.JolieValue}
     * operation[0,1]: {@link java.lang.String}
     * timeunit[0,1]: {@link java.lang.String}
 * </pre>
 * 
 * @see jolie.runtime.embedding.java.JolieValue
 * @see jolie.runtime.embedding.java.JolieNative
 */
public final class ScheduleTimeOutRequest extends jolie.runtime.embedding.java.TypedStructure {
    
    private static final java.util.Set<java.lang.String> FIELD_KEYS = fieldKeys( ScheduleTimeOutRequest.class );
    
    private final java.lang.Integer contentValue;
    @jolie.runtime.embedding.java.util.JolieName("message")
    private final jolie.runtime.embedding.java.JolieValue message;
    @jolie.runtime.embedding.java.util.JolieName("operation")
    private final java.lang.String operation;
    @jolie.runtime.embedding.java.util.JolieName("timeunit")
    private final java.lang.String timeunit;
    
    public ScheduleTimeOutRequest( java.lang.Integer contentValue, jolie.runtime.embedding.java.JolieValue message, java.lang.String operation, java.lang.String timeunit ) {
        this.contentValue = jolie.runtime.embedding.java.util.ValueManager.validated( "contentValue", contentValue );
        this.message = message;
        this.operation = operation;
        this.timeunit = timeunit;
    }
    
    public java.lang.Integer contentValue() { return contentValue; }
    public java.util.Optional<jolie.runtime.embedding.java.JolieValue> message() { return java.util.Optional.ofNullable( message ); }
    public java.util.Optional<java.lang.String> operation() { return java.util.Optional.ofNullable( operation ); }
    public java.util.Optional<java.lang.String> timeunit() { return java.util.Optional.ofNullable( timeunit ); }
    
    public jolie.runtime.embedding.java.JolieNative.JolieInt content() { return new jolie.runtime.embedding.java.JolieNative.JolieInt( contentValue ); }
    
    public static ScheduleTimeOutRequest from( jolie.runtime.embedding.java.JolieValue j ) throws jolie.runtime.embedding.java.TypeValidationException {
        return new ScheduleTimeOutRequest(
            jolie.runtime.embedding.java.JolieNative.JolieInt.from( j ).value(),
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "message" ), jolie.runtime.embedding.java.JolieValue::from ),
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "operation" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieString content ? content.value() : null ),
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "timeunit" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieString content ? content.value() : null )
        );
    }
    
    public static ScheduleTimeOutRequest fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
        jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
        return new ScheduleTimeOutRequest(
            jolie.runtime.embedding.java.JolieNative.JolieInt.contentFromValue( v ),
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "message", jolie.runtime.embedding.java.JolieValue::fromValue ),
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "operation", jolie.runtime.embedding.java.JolieNative.JolieString::fieldFromValue ),
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "timeunit", jolie.runtime.embedding.java.JolieNative.JolieString::fieldFromValue )
        );
    }
    
    public static jolie.runtime.Value toValue( ScheduleTimeOutRequest t ) {
        final jolie.runtime.Value v = jolie.runtime.Value.create( t.contentValue() );
        
        t.message().ifPresent( c -> v.getFirstChild( "message" ).deepCopy( jolie.runtime.embedding.java.JolieValue.toValue( c ) ) );
        t.operation().ifPresent( c -> v.getFirstChild( "operation" ).setValue( c ) );
        t.timeunit().ifPresent( c -> v.getFirstChild( "timeunit" ).setValue( c ) );
        
        return v;
    }
}