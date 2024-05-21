package joliex.util.spec.types;

/**
 * this class is a {@link jolie.runtime.embedding.java.TypedStructure} which can be described as follows:
 * <pre>
 * month: {@link java.lang.Integer}
 * year: {@link java.lang.Integer}
 * day: {@link java.lang.Integer}
 * </pre>
 * 
 * @see jolie.runtime.embedding.java.JolieValue
 * @see jolie.runtime.embedding.java.JolieNative
 * @see #builder()
 */
public final class DateValuesType extends jolie.runtime.embedding.java.TypedStructure {
    
    private static final java.util.Set<java.lang.String> FIELD_KEYS = fieldKeys( DateValuesType.class );
    
    @jolie.runtime.embedding.java.util.JolieName("month")
    private final java.lang.Integer month;
    @jolie.runtime.embedding.java.util.JolieName("year")
    private final java.lang.Integer year;
    @jolie.runtime.embedding.java.util.JolieName("day")
    private final java.lang.Integer day;
    
    public DateValuesType( java.lang.Integer month, java.lang.Integer year, java.lang.Integer day ) {
        this.month = jolie.runtime.embedding.java.util.ValueManager.validated( "month", month );
        this.year = jolie.runtime.embedding.java.util.ValueManager.validated( "year", year );
        this.day = jolie.runtime.embedding.java.util.ValueManager.validated( "day", day );
    }
    
    public java.lang.Integer month() { return month; }
    public java.lang.Integer year() { return year; }
    public java.lang.Integer day() { return day; }
    
    public jolie.runtime.embedding.java.JolieNative.JolieVoid content() { return new jolie.runtime.embedding.java.JolieNative.JolieVoid(); }
    
    public static Builder builder() { return new Builder(); }
    public static Builder builder( jolie.runtime.embedding.java.JolieValue from ) { return new Builder( from ); }
    
    public static jolie.runtime.embedding.java.util.StructureListBuilder<DateValuesType, Builder> listBuilder() { return new jolie.runtime.embedding.java.util.StructureListBuilder<>( DateValuesType::builder, DateValuesType::builder ); }
    public static jolie.runtime.embedding.java.util.StructureListBuilder<DateValuesType, Builder> listBuilder( java.util.SequencedCollection<? extends jolie.runtime.embedding.java.JolieValue> from ) {
        return new jolie.runtime.embedding.java.util.StructureListBuilder<>( from, DateValuesType::from, DateValuesType::builder, DateValuesType::builder );
    }
    
    public static DateValuesType from( jolie.runtime.embedding.java.JolieValue j ) throws jolie.runtime.embedding.java.TypeValidationException {
        return new DateValuesType(
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "month" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieInt content ? content.value() : null ),
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "year" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieInt content ? content.value() : null ),
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "day" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieInt content ? content.value() : null )
        );
    }
    
    public static DateValuesType fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
        jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
        return new DateValuesType(
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "month", jolie.runtime.embedding.java.JolieNative.JolieInt::fieldFromValue ),
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "year", jolie.runtime.embedding.java.JolieNative.JolieInt::fieldFromValue ),
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "day", jolie.runtime.embedding.java.JolieNative.JolieInt::fieldFromValue )
        );
    }
    
    public static jolie.runtime.Value toValue( DateValuesType t ) {
        final jolie.runtime.Value v = jolie.runtime.Value.create();
        
        v.getFirstChild( "month" ).setValue( t.month() );
        v.getFirstChild( "year" ).setValue( t.year() );
        v.getFirstChild( "day" ).setValue( t.day() );
        
        return v;
    }
    
    public static class Builder {
        
        private java.lang.Integer month;
        private java.lang.Integer year;
        private java.lang.Integer day;
        
        private Builder() {}
        private Builder( jolie.runtime.embedding.java.JolieValue j ) {
            this.month = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "month" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieInt content ? content.value() : null );
            this.year = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "year" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieInt content ? content.value() : null );
            this.day = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "day" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieInt content ? content.value() : null );
        }
        
        public Builder month( java.lang.Integer month ) { this.month = month; return this; }
        public Builder year( java.lang.Integer year ) { this.year = year; return this; }
        public Builder day( java.lang.Integer day ) { this.day = day; return this; }
        
        public DateValuesType build() {
            return new DateValuesType( month, year, day );
        }
    }
}