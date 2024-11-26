package joliex.meta.spec.types;

/**
 * this class is a {@link jolie.runtime.embedding.java.TypedStructure} which can be described as follows:
 * <pre>
 * min: {@link java.lang.Double}
 * max: {@link java.lang.Double}
 * </pre>
 * 
 * @see jolie.runtime.embedding.java.JolieValue
 * @see jolie.runtime.embedding.java.JolieNative
 * @see #builder()
 */
public final class DoubleRange extends jolie.runtime.embedding.java.TypedStructure {
    
    private static final java.util.Set<java.lang.String> FIELD_KEYS = fieldKeys( DoubleRange.class );
    
    @jolie.runtime.embedding.java.util.JolieName("min")
    private final java.lang.Double min;
    @jolie.runtime.embedding.java.util.JolieName("max")
    private final java.lang.Double max;
    
    public DoubleRange( java.lang.Double min, java.lang.Double max ) {
        this.min = jolie.runtime.embedding.java.util.ValueManager.validated( "min", min );
        this.max = jolie.runtime.embedding.java.util.ValueManager.validated( "max", max );
    }
    
    public java.lang.Double min() { return min; }
    public java.lang.Double max() { return max; }
    
    public jolie.runtime.embedding.java.JolieNative.JolieVoid content() { return new jolie.runtime.embedding.java.JolieNative.JolieVoid(); }
    
    public static Builder builder() { return new Builder(); }
    public static Builder builder( jolie.runtime.embedding.java.JolieValue from ) { return from != null ? new Builder( from ) : builder(); }
    
    public static jolie.runtime.embedding.java.util.StructureListBuilder<DoubleRange, Builder> listBuilder() { return new jolie.runtime.embedding.java.util.StructureListBuilder<>( DoubleRange::builder ); }
    public static jolie.runtime.embedding.java.util.StructureListBuilder<DoubleRange, Builder> listBuilder( java.util.SequencedCollection<? extends jolie.runtime.embedding.java.JolieValue> from ) {
        return from != null ? new jolie.runtime.embedding.java.util.StructureListBuilder<>( from, DoubleRange::from, DoubleRange::builder ) : listBuilder();
    }
    
    public static DoubleRange from( jolie.runtime.embedding.java.JolieValue j ) throws jolie.runtime.embedding.java.TypeValidationException {
        return new DoubleRange(
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "min" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieDouble content ? content.value() : null ),
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "max" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieDouble content ? content.value() : null )
        );
    }
    
    public static DoubleRange fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
        jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
        return new DoubleRange(
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "min", jolie.runtime.embedding.java.JolieNative.JolieDouble::fieldFromValue ),
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "max", jolie.runtime.embedding.java.JolieNative.JolieDouble::fieldFromValue )
        );
    }
    
    public static jolie.runtime.Value toValue( DoubleRange t ) {
        final jolie.runtime.Value v = jolie.runtime.Value.create();
        
        v.getFirstChild( "min" ).setValue( t.min() );
        v.getFirstChild( "max" ).setValue( t.max() );
        
        return v;
    }
    
    public static class Builder {
        
        private java.lang.Double min;
        private java.lang.Double max;
        
        private Builder() {}
        private Builder( jolie.runtime.embedding.java.JolieValue j ) {
            this.min = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "min" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieDouble content ? content.value() : null );
            this.max = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "max" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieDouble content ? content.value() : null );
        }
        
        public Builder min( java.lang.Double min ) { this.min = min; return this; }
        public Builder max( java.lang.Double max ) { this.max = max; return this; }
        
        public DoubleRange build() {
            return new DoubleRange( min, max );
        }
    }
}