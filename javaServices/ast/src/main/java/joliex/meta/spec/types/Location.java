package joliex.meta.spec.types;

/**
 * this class is a {@link jolie.runtime.embedding.java.TypedStructure} which can be described as follows:
 * <pre>
 * range: {@link joliex.meta.spec.types.Range}
 * source: {@link java.lang.String}
 * </pre>
 * 
 * @see jolie.runtime.embedding.java.JolieValue
 * @see jolie.runtime.embedding.java.JolieNative
 * @see joliex.meta.spec.types.Range
 * @see #builder()
 */
public final class Location extends jolie.runtime.embedding.java.TypedStructure {
    
    private static final java.util.Set<java.lang.String> FIELD_KEYS = fieldKeys( Location.class );
    
    @jolie.runtime.embedding.java.util.JolieName("range")
    private final joliex.meta.spec.types.Range range;
    @jolie.runtime.embedding.java.util.JolieName("source")
    private final java.lang.String source;
    
    public Location( joliex.meta.spec.types.Range range, java.lang.String source ) {
        this.range = jolie.runtime.embedding.java.util.ValueManager.validated( "range", range );
        this.source = jolie.runtime.embedding.java.util.ValueManager.validated( "source", source );
    }
    
    public joliex.meta.spec.types.Range range() { return range; }
    public java.lang.String source() { return source; }
    
    public jolie.runtime.embedding.java.JolieNative.JolieVoid content() { return new jolie.runtime.embedding.java.JolieNative.JolieVoid(); }
    
    public static Builder builder() { return new Builder(); }
    public static Builder builder( jolie.runtime.embedding.java.JolieValue from ) { return from != null ? new Builder( from ) : builder(); }
    
    public static jolie.runtime.embedding.java.util.StructureListBuilder<Location, Builder> listBuilder() { return new jolie.runtime.embedding.java.util.StructureListBuilder<>( Location::builder ); }
    public static jolie.runtime.embedding.java.util.StructureListBuilder<Location, Builder> listBuilder( java.util.SequencedCollection<? extends jolie.runtime.embedding.java.JolieValue> from ) {
        return from != null ? new jolie.runtime.embedding.java.util.StructureListBuilder<>( from, Location::from, Location::builder ) : listBuilder();
    }
    
    public static Location from( jolie.runtime.embedding.java.JolieValue j ) throws jolie.runtime.embedding.java.TypeValidationException {
        return new Location(
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "range" ), joliex.meta.spec.types.Range::from ),
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "source" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieString content ? content.value() : null )
        );
    }
    
    public static Location fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
        jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
        return new Location(
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "range", joliex.meta.spec.types.Range::fromValue ),
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "source", jolie.runtime.embedding.java.JolieNative.JolieString::fieldFromValue )
        );
    }
    
    public static jolie.runtime.Value toValue( Location t ) {
        final jolie.runtime.Value v = jolie.runtime.Value.create();
        
        v.getFirstChild( "range" ).deepCopy( joliex.meta.spec.types.Range.toValue( t.range() ) );
        v.getFirstChild( "source" ).setValue( t.source() );
        
        return v;
    }
    
    public static class Builder {
        
        private joliex.meta.spec.types.Range range;
        private java.lang.String source;
        
        private Builder() {}
        private Builder( jolie.runtime.embedding.java.JolieValue j ) {
            this.range = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "range" ), joliex.meta.spec.types.Range::from );
            this.source = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "source" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieString content ? content.value() : null );
        }
        
        public Builder range( joliex.meta.spec.types.Range range ) { this.range = range; return this; }
        public Builder range( java.util.function.Function<joliex.meta.spec.types.Range.Builder, joliex.meta.spec.types.Range> f ) { return range( f.apply( joliex.meta.spec.types.Range.builder() ) ); }
        public Builder source( java.lang.String source ) { this.source = source; return this; }
        
        public Location build() {
            return new Location( range, source );
        }
    }
}