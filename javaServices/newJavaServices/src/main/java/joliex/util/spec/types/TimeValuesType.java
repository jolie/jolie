package joliex.util.spec.types;

/**
 * this class is a {@link jolie.runtime.embedding.java.TypedStructure} which can be described as follows:
 * <pre>
 * hour: {@link java.lang.Integer}
 * minute: {@link java.lang.Integer}
 * second: {@link java.lang.Integer}
 * </pre>
 * 
 * @see jolie.runtime.embedding.java.JolieValue
 * @see jolie.runtime.embedding.java.JolieNative
 * @see #builder()
 */
public final class TimeValuesType extends jolie.runtime.embedding.java.TypedStructure {
    
    private static final java.util.Set<java.lang.String> FIELD_KEYS = fieldKeys( TimeValuesType.class );
    
    @jolie.runtime.embedding.java.util.JolieName("hour")
    private final java.lang.Integer hour;
    @jolie.runtime.embedding.java.util.JolieName("minute")
    private final java.lang.Integer minute;
    @jolie.runtime.embedding.java.util.JolieName("second")
    private final java.lang.Integer second;
    
    public TimeValuesType( java.lang.Integer hour, java.lang.Integer minute, java.lang.Integer second ) {
        this.hour = jolie.runtime.embedding.java.util.ValueManager.validated( "hour", hour );
        this.minute = jolie.runtime.embedding.java.util.ValueManager.validated( "minute", minute );
        this.second = jolie.runtime.embedding.java.util.ValueManager.validated( "second", second );
    }
    
    public java.lang.Integer hour() { return hour; }
    public java.lang.Integer minute() { return minute; }
    public java.lang.Integer second() { return second; }
    
    public jolie.runtime.embedding.java.JolieNative.JolieVoid content() { return new jolie.runtime.embedding.java.JolieNative.JolieVoid(); }
    
    public static Builder builder() { return new Builder(); }
    public static Builder builder( jolie.runtime.embedding.java.JolieValue from ) { return new Builder( from ); }
    
    public static jolie.runtime.embedding.java.util.StructureListBuilder<TimeValuesType, Builder> listBuilder() { return new jolie.runtime.embedding.java.util.StructureListBuilder<>( TimeValuesType::builder, TimeValuesType::builder ); }
    public static jolie.runtime.embedding.java.util.StructureListBuilder<TimeValuesType, Builder> listBuilder( java.util.SequencedCollection<? extends jolie.runtime.embedding.java.JolieValue> from ) {
        return new jolie.runtime.embedding.java.util.StructureListBuilder<>( from, TimeValuesType::from, TimeValuesType::builder, TimeValuesType::builder );
    }
    
    public static TimeValuesType from( jolie.runtime.embedding.java.JolieValue j ) throws jolie.runtime.embedding.java.TypeValidationException {
        return new TimeValuesType(
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "hour" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieInt content ? content.value() : null ),
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "minute" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieInt content ? content.value() : null ),
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "second" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieInt content ? content.value() : null )
        );
    }
    
    public static TimeValuesType fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
        jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
        return new TimeValuesType(
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "hour", jolie.runtime.embedding.java.JolieNative.JolieInt::fieldFromValue ),
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "minute", jolie.runtime.embedding.java.JolieNative.JolieInt::fieldFromValue ),
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "second", jolie.runtime.embedding.java.JolieNative.JolieInt::fieldFromValue )
        );
    }
    
    public static jolie.runtime.Value toValue( TimeValuesType t ) {
        final jolie.runtime.Value v = jolie.runtime.Value.create();
        
        v.getFirstChild( "hour" ).setValue( t.hour() );
        v.getFirstChild( "minute" ).setValue( t.minute() );
        v.getFirstChild( "second" ).setValue( t.second() );
        
        return v;
    }
    
    public static class Builder {
        
        private java.lang.Integer hour;
        private java.lang.Integer minute;
        private java.lang.Integer second;
        
        private Builder() {}
        private Builder( jolie.runtime.embedding.java.JolieValue j ) {
            this.hour = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "hour" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieInt content ? content.value() : null );
            this.minute = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "minute" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieInt content ? content.value() : null );
            this.second = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "second" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieInt content ? content.value() : null );
        }
        
        public Builder hour( java.lang.Integer hour ) { this.hour = hour; return this; }
        public Builder minute( java.lang.Integer minute ) { this.minute = minute; return this; }
        public Builder second( java.lang.Integer second ) { this.second = second; return this; }
        
        public TimeValuesType build() {
            return new TimeValuesType( hour, minute, second );
        }
    }
}