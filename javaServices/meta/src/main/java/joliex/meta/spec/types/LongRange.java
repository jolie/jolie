package joliex.meta.spec.types;

/**
 * this class is a {@link jolie.runtime.embedding.java.TypedStructure} which can be described as follows:
 * <pre>
 * min: {@link java.lang.Long}
 * max: {@link java.lang.Long}
 * </pre>
 * 
 * @see jolie.runtime.embedding.java.JolieValue
 * @see jolie.runtime.embedding.java.JolieNative
 * @see #builder()
 */
public final class LongRange extends jolie.runtime.embedding.java.TypedStructure {
    
    private static final java.util.Set<java.lang.String> FIELD_KEYS = fieldKeys( LongRange.class );
    
    @jolie.runtime.embedding.java.util.JolieName("min")
    private final java.lang.Long min;
    @jolie.runtime.embedding.java.util.JolieName("max")
    private final java.lang.Long max;
    
    public LongRange( java.lang.Long min, java.lang.Long max ) {
        this.min = jolie.runtime.embedding.java.util.ValueManager.validated( "min", min );
        this.max = jolie.runtime.embedding.java.util.ValueManager.validated( "max", max );
    }
    
    public java.lang.Long min() { return min; }
    public java.lang.Long max() { return max; }
    
    public jolie.runtime.embedding.java.JolieNative.JolieVoid content() { return new jolie.runtime.embedding.java.JolieNative.JolieVoid(); }
    
    public static Builder builder() { return new Builder(); }
    public static Builder builder( jolie.runtime.embedding.java.JolieValue from ) { return from != null ? new Builder( from ) : builder(); }
    
    public static jolie.runtime.embedding.java.util.StructureListBuilder<LongRange, Builder> listBuilder() { return new jolie.runtime.embedding.java.util.StructureListBuilder<>( LongRange::builder ); }
    public static jolie.runtime.embedding.java.util.StructureListBuilder<LongRange, Builder> listBuilder( java.util.SequencedCollection<? extends jolie.runtime.embedding.java.JolieValue> from ) {
        return from != null ? new jolie.runtime.embedding.java.util.StructureListBuilder<>( from, LongRange::from, LongRange::builder ) : listBuilder();
    }
    
    public static LongRange from( jolie.runtime.embedding.java.JolieValue j ) throws jolie.runtime.embedding.java.TypeValidationException {
        return new LongRange(
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "min" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieLong content ? content.value() : null ),
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "max" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieLong content ? content.value() : null )
        );
    }
    
    public static LongRange fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
        jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
        return new LongRange(
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "min", jolie.runtime.embedding.java.JolieNative.JolieLong::fieldFromValue ),
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "max", jolie.runtime.embedding.java.JolieNative.JolieLong::fieldFromValue )
        );
    }
    
    public static jolie.runtime.Value toValue( LongRange t ) {
        final jolie.runtime.Value v = jolie.runtime.Value.create();
        
        v.getFirstChild( "min" ).setValue( t.min() );
        v.getFirstChild( "max" ).setValue( t.max() );
        
        return v;
    }
    
    public static class Builder {
        
        private java.lang.Long min;
        private java.lang.Long max;
        
        private Builder() {}
        private Builder( jolie.runtime.embedding.java.JolieValue j ) {
            this.min = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "min" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieLong content ? content.value() : null );
            this.max = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "max" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieLong content ? content.value() : null );
        }
        
        public Builder min( java.lang.Long min ) { this.min = min; return this; }
        public Builder max( java.lang.Long max ) { this.max = max; return this; }
        
        public LongRange build() {
            return new LongRange( min, max );
        }
    }
}