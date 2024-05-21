package joliex.util.spec.types;

/**
 * this class is a {@link jolie.runtime.embedding.java.TypedStructure} which can be described as follows:
 * <pre>
 * format[0,1]: {@link java.lang.String}
 * date2: {@link java.lang.String}
 * date1: {@link java.lang.String}
 * </pre>
 * 
 * @see jolie.runtime.embedding.java.JolieValue
 * @see jolie.runtime.embedding.java.JolieNative
 */
public final class DiffDateRequestType extends jolie.runtime.embedding.java.TypedStructure {
    
    private static final java.util.Set<java.lang.String> FIELD_KEYS = fieldKeys( DiffDateRequestType.class );
    
    @jolie.runtime.embedding.java.util.JolieName("format")
    private final java.lang.String format;
    @jolie.runtime.embedding.java.util.JolieName("date2")
    private final java.lang.String date2;
    @jolie.runtime.embedding.java.util.JolieName("date1")
    private final java.lang.String date1;
    
    public DiffDateRequestType( java.lang.String format, java.lang.String date2, java.lang.String date1 ) {
        this.format = format;
        this.date2 = jolie.runtime.embedding.java.util.ValueManager.validated( "date2", date2 );
        this.date1 = jolie.runtime.embedding.java.util.ValueManager.validated( "date1", date1 );
    }
    
    public java.util.Optional<java.lang.String> format() { return java.util.Optional.ofNullable( format ); }
    public java.lang.String date2() { return date2; }
    public java.lang.String date1() { return date1; }
    
    public jolie.runtime.embedding.java.JolieNative.JolieVoid content() { return new jolie.runtime.embedding.java.JolieNative.JolieVoid(); }
    
    public static DiffDateRequestType from( jolie.runtime.embedding.java.JolieValue j ) throws jolie.runtime.embedding.java.TypeValidationException {
        return new DiffDateRequestType(
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "format" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieString content ? content.value() : null ),
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "date2" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieString content ? content.value() : null ),
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "date1" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieString content ? content.value() : null )
        );
    }
    
    public static DiffDateRequestType fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
        jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
        return new DiffDateRequestType(
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "format", jolie.runtime.embedding.java.JolieNative.JolieString::fieldFromValue ),
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "date2", jolie.runtime.embedding.java.JolieNative.JolieString::fieldFromValue ),
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "date1", jolie.runtime.embedding.java.JolieNative.JolieString::fieldFromValue )
        );
    }
    
    public static jolie.runtime.Value toValue( DiffDateRequestType t ) {
        final jolie.runtime.Value v = jolie.runtime.Value.create();
        
        t.format().ifPresent( c -> v.getFirstChild( "format" ).setValue( c ) );
        v.getFirstChild( "date2" ).setValue( t.date2() );
        v.getFirstChild( "date1" ).setValue( t.date1() );
        
        return v;
    }
}