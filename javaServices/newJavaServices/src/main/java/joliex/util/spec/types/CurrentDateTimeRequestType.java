package joliex.util.spec.types;

/**
 * this class is a {@link jolie.runtime.embedding.java.TypedStructure} which can be described as follows:
 * <pre>
 * format[0,1]: {@link java.lang.String}
 * </pre>
 * 
 * @see jolie.runtime.embedding.java.JolieValue
 * @see jolie.runtime.embedding.java.JolieNative
 */
public final class CurrentDateTimeRequestType extends jolie.runtime.embedding.java.TypedStructure {
    
    private static final java.util.Set<java.lang.String> FIELD_KEYS = fieldKeys( CurrentDateTimeRequestType.class );
    
    @jolie.runtime.embedding.java.util.JolieName("format")
    private final java.lang.String format;
    
    public CurrentDateTimeRequestType( java.lang.String format ) {
        this.format = format;
    }
    
    public java.util.Optional<java.lang.String> format() { return java.util.Optional.ofNullable( format ); }
    
    public jolie.runtime.embedding.java.JolieNative.JolieVoid content() { return new jolie.runtime.embedding.java.JolieNative.JolieVoid(); }
    
    public static CurrentDateTimeRequestType from( jolie.runtime.embedding.java.JolieValue j ) throws jolie.runtime.embedding.java.TypeValidationException {
        return new CurrentDateTimeRequestType(
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "format" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieString content ? content.value() : null )
        );
    }
    
    public static CurrentDateTimeRequestType fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
        jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
        return new CurrentDateTimeRequestType(
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "format", jolie.runtime.embedding.java.JolieNative.JolieString::fieldFromValue )
        );
    }
    
    public static jolie.runtime.Value toValue( CurrentDateTimeRequestType t ) {
        final jolie.runtime.Value v = jolie.runtime.Value.create();
        
        t.format().ifPresent( c -> v.getFirstChild( "format" ).setValue( c ) );
        
        return v;
    }
}