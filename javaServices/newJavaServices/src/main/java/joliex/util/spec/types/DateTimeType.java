package joliex.util.spec.types;

/**
 * this class is a {@link jolie.runtime.embedding.java.TypedStructure} which can be described as follows:
 * <pre>
 * month: {@link java.lang.Integer}
 * hour: {@link java.lang.Integer}
 * year: {@link java.lang.Integer}
 * day: {@link java.lang.Integer}
 * minute: {@link java.lang.Integer}
 * second: {@link java.lang.Integer}
 * </pre>
 * 
 * @see jolie.runtime.embedding.java.JolieValue
 * @see jolie.runtime.embedding.java.JolieNative
 * @see #builder()
 */
public final class DateTimeType extends jolie.runtime.embedding.java.TypedStructure {
    
    private static final java.util.Set<java.lang.String> FIELD_KEYS = fieldKeys( DateTimeType.class );
    
    @jolie.runtime.embedding.java.util.JolieName("month")
    private final java.lang.Integer month;
    @jolie.runtime.embedding.java.util.JolieName("hour")
    private final java.lang.Integer hour;
    @jolie.runtime.embedding.java.util.JolieName("year")
    private final java.lang.Integer year;
    @jolie.runtime.embedding.java.util.JolieName("day")
    private final java.lang.Integer day;
    @jolie.runtime.embedding.java.util.JolieName("minute")
    private final java.lang.Integer minute;
    @jolie.runtime.embedding.java.util.JolieName("second")
    private final java.lang.Integer second;
    
    public DateTimeType( java.lang.Integer month, java.lang.Integer hour, java.lang.Integer year, java.lang.Integer day, java.lang.Integer minute, java.lang.Integer second ) {
        this.month = jolie.runtime.embedding.java.util.ValueManager.validated( "month", month );
        this.hour = jolie.runtime.embedding.java.util.ValueManager.validated( "hour", hour );
        this.year = jolie.runtime.embedding.java.util.ValueManager.validated( "year", year );
        this.day = jolie.runtime.embedding.java.util.ValueManager.validated( "day", day );
        this.minute = jolie.runtime.embedding.java.util.ValueManager.validated( "minute", minute );
        this.second = jolie.runtime.embedding.java.util.ValueManager.validated( "second", second );
    }
    
    public java.lang.Integer month() { return month; }
    public java.lang.Integer hour() { return hour; }
    public java.lang.Integer year() { return year; }
    public java.lang.Integer day() { return day; }
    public java.lang.Integer minute() { return minute; }
    public java.lang.Integer second() { return second; }
    
    public jolie.runtime.embedding.java.JolieNative.JolieVoid content() { return new jolie.runtime.embedding.java.JolieNative.JolieVoid(); }
    
    public static Builder builder() { return new Builder(); }
    public static Builder builder( jolie.runtime.embedding.java.JolieValue from ) { return new Builder( from ); }
    
    public static jolie.runtime.embedding.java.util.StructureListBuilder<DateTimeType, Builder> listBuilder() { return new jolie.runtime.embedding.java.util.StructureListBuilder<>( DateTimeType::builder, DateTimeType::builder ); }
    public static jolie.runtime.embedding.java.util.StructureListBuilder<DateTimeType, Builder> listBuilder( java.util.SequencedCollection<? extends jolie.runtime.embedding.java.JolieValue> from ) {
        return new jolie.runtime.embedding.java.util.StructureListBuilder<>( from, DateTimeType::from, DateTimeType::builder, DateTimeType::builder );
    }
    
    public static DateTimeType from( jolie.runtime.embedding.java.JolieValue j ) throws jolie.runtime.embedding.java.TypeValidationException {
        return new DateTimeType(
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "month" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieInt content ? content.value() : null ),
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "hour" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieInt content ? content.value() : null ),
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "year" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieInt content ? content.value() : null ),
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "day" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieInt content ? content.value() : null ),
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "minute" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieInt content ? content.value() : null ),
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "second" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieInt content ? content.value() : null )
        );
    }
    
    public static DateTimeType fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
        jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
        return new DateTimeType(
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "month", jolie.runtime.embedding.java.JolieNative.JolieInt::fieldFromValue ),
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "hour", jolie.runtime.embedding.java.JolieNative.JolieInt::fieldFromValue ),
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "year", jolie.runtime.embedding.java.JolieNative.JolieInt::fieldFromValue ),
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "day", jolie.runtime.embedding.java.JolieNative.JolieInt::fieldFromValue ),
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "minute", jolie.runtime.embedding.java.JolieNative.JolieInt::fieldFromValue ),
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "second", jolie.runtime.embedding.java.JolieNative.JolieInt::fieldFromValue )
        );
    }
    
    public static jolie.runtime.Value toValue( DateTimeType t ) {
        final jolie.runtime.Value v = jolie.runtime.Value.create();
        
        v.getFirstChild( "month" ).setValue( t.month() );
        v.getFirstChild( "hour" ).setValue( t.hour() );
        v.getFirstChild( "year" ).setValue( t.year() );
        v.getFirstChild( "day" ).setValue( t.day() );
        v.getFirstChild( "minute" ).setValue( t.minute() );
        v.getFirstChild( "second" ).setValue( t.second() );
        
        return v;
    }
    
    public static class Builder {
        
        private java.lang.Integer month;
        private java.lang.Integer hour;
        private java.lang.Integer year;
        private java.lang.Integer day;
        private java.lang.Integer minute;
        private java.lang.Integer second;
        
        private Builder() {}
        private Builder( jolie.runtime.embedding.java.JolieValue j ) {
            this.month = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "month" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieInt content ? content.value() : null );
            this.hour = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "hour" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieInt content ? content.value() : null );
            this.year = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "year" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieInt content ? content.value() : null );
            this.day = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "day" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieInt content ? content.value() : null );
            this.minute = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "minute" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieInt content ? content.value() : null );
            this.second = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "second" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieInt content ? content.value() : null );
        }
        
        public Builder month( java.lang.Integer month ) { this.month = month; return this; }
        public Builder hour( java.lang.Integer hour ) { this.hour = hour; return this; }
        public Builder year( java.lang.Integer year ) { this.year = year; return this; }
        public Builder day( java.lang.Integer day ) { this.day = day; return this; }
        public Builder minute( java.lang.Integer minute ) { this.minute = minute; return this; }
        public Builder second( java.lang.Integer second ) { this.second = second; return this; }
        
        public DateTimeType build() {
            return new DateTimeType( month, hour, year, day, minute, second );
        }
    }
}