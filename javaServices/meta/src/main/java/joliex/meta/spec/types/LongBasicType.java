package joliex.meta.spec.types;

/**
 * this class is a {@link jolie.runtime.embedding.java.TypedStructure} which can be described as follows:
 * <pre>
 * 
 * contentValue: {@link java.lang.Long}
     * refinements[0,2147483647]: {@link joliex.meta.spec.types.LongRefinement}
 * </pre>
 * 
 * @see jolie.runtime.embedding.java.JolieValue
 * @see jolie.runtime.embedding.java.JolieNative
 * @see joliex.meta.spec.types.LongRefinement
 * @see #builder()
 */
public final class LongBasicType extends jolie.runtime.embedding.java.TypedStructure {
    
    private static final java.util.Set<java.lang.String> FIELD_KEYS = fieldKeys( LongBasicType.class );
    
    private final java.lang.Long contentValue;
    @jolie.runtime.embedding.java.util.JolieName("refinements")
    private final java.util.List<joliex.meta.spec.types.LongRefinement> refinements;
    
    public LongBasicType( java.lang.Long contentValue, java.util.SequencedCollection<joliex.meta.spec.types.LongRefinement> refinements ) {
        this.contentValue = jolie.runtime.embedding.java.util.ValueManager.validated( "contentValue", contentValue );
        this.refinements = jolie.runtime.embedding.java.util.ValueManager.validated( "refinements", refinements, 0, 2147483647, t -> t );
    }
    
    public java.lang.Long contentValue() { return contentValue; }
    public java.util.List<joliex.meta.spec.types.LongRefinement> refinements() { return refinements; }
    
    public jolie.runtime.embedding.java.JolieNative.JolieLong content() { return new jolie.runtime.embedding.java.JolieNative.JolieLong( contentValue ); }
    
    public static Builder builder() { return new Builder(); }
    public static Builder builder( java.lang.Long contentValue ) { return builder().contentValue( contentValue ); }
    public static Builder builder( jolie.runtime.embedding.java.JolieValue from ) { return from != null ? new Builder( from ) : builder(); }
    
    public static jolie.runtime.embedding.java.util.StructureListBuilder<LongBasicType, Builder> listBuilder() { return new jolie.runtime.embedding.java.util.StructureListBuilder<>( LongBasicType::builder ); }
    public static jolie.runtime.embedding.java.util.StructureListBuilder<LongBasicType, Builder> listBuilder( java.util.SequencedCollection<? extends jolie.runtime.embedding.java.JolieValue> from ) {
        return from != null ? new jolie.runtime.embedding.java.util.StructureListBuilder<>( from, LongBasicType::from, LongBasicType::builder ) : listBuilder();
    }
    
    public static LongBasicType from( jolie.runtime.embedding.java.JolieValue j ) throws jolie.runtime.embedding.java.TypeValidationException {
        return new LongBasicType(
            jolie.runtime.embedding.java.JolieNative.JolieLong.from( j ).value(),
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getChildOrDefault( "refinements", java.util.List.of() ), joliex.meta.spec.types.LongRefinement::from )
        );
    }
    
    public static LongBasicType fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
        jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
        return new LongBasicType(
            jolie.runtime.embedding.java.JolieNative.JolieLong.contentFromValue( v ),
            jolie.runtime.embedding.java.util.ValueManager.vectorFieldFrom( v, "refinements", joliex.meta.spec.types.LongRefinement::fromValue )
        );
    }
    
    public static jolie.runtime.Value toValue( LongBasicType t ) {
        final jolie.runtime.Value v = jolie.runtime.Value.create( t.contentValue() );
        
        t.refinements().forEach( c -> v.getNewChild( "refinements" ).deepCopy( joliex.meta.spec.types.LongRefinement.toValue( c ) ) );
        
        return v;
    }
    
    public static class Builder {
        
        private java.lang.Long contentValue;
        private java.util.SequencedCollection<joliex.meta.spec.types.LongRefinement> refinements;
        
        private Builder() {}
        private Builder( jolie.runtime.embedding.java.JolieValue j ) {
            
            contentValue = j.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieLong content ? content.value() : null;
            this.refinements = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getChildOrDefault( "refinements", java.util.List.of() ), joliex.meta.spec.types.LongRefinement::from );
        }
        
        public Builder contentValue( java.lang.Long contentValue ) { this.contentValue = contentValue; return this; }
        public Builder refinements( java.util.SequencedCollection<joliex.meta.spec.types.LongRefinement> refinements ) { this.refinements = refinements; return this; }
        public Builder refinements( java.util.function.Function<jolie.runtime.embedding.java.util.StructureListBuilder<joliex.meta.spec.types.LongRefinement, joliex.meta.spec.types.LongRefinement.Builder>, java.util.List<joliex.meta.spec.types.LongRefinement>> f ) { return refinements( f.apply( new jolie.runtime.embedding.java.util.StructureListBuilder<>( joliex.meta.spec.types.LongRefinement::builder ) ) ); }
        
        public LongBasicType build() {
            return new LongBasicType( contentValue, refinements );
        }
    }
}