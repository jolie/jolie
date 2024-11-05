package joliex.meta.spec.types;

/**
 * this class is a {@link jolie.runtime.embedding.java.TypedStructure} which can be described as follows:
 * <pre>
 * 
 * contentValue: {@link java.lang.Double}
     * refinements[0,2147483647]: {@link joliex.meta.spec.types.DoubleRefinement}
 * </pre>
 * 
 * @see jolie.runtime.embedding.java.JolieValue
 * @see jolie.runtime.embedding.java.JolieNative
 * @see joliex.meta.spec.types.DoubleRefinement
 * @see #builder()
 */
public final class DoubleBasicType extends jolie.runtime.embedding.java.TypedStructure {
    
    private static final java.util.Set<java.lang.String> FIELD_KEYS = fieldKeys( DoubleBasicType.class );
    
    private final java.lang.Double contentValue;
    @jolie.runtime.embedding.java.util.JolieName("refinements")
    private final java.util.List<joliex.meta.spec.types.DoubleRefinement> refinements;
    
    public DoubleBasicType( java.lang.Double contentValue, java.util.SequencedCollection<joliex.meta.spec.types.DoubleRefinement> refinements ) {
        this.contentValue = jolie.runtime.embedding.java.util.ValueManager.validated( "contentValue", contentValue );
        this.refinements = jolie.runtime.embedding.java.util.ValueManager.validated( "refinements", refinements, 0, 2147483647, t -> t );
    }
    
    public java.lang.Double contentValue() { return contentValue; }
    public java.util.List<joliex.meta.spec.types.DoubleRefinement> refinements() { return refinements; }
    
    public jolie.runtime.embedding.java.JolieNative.JolieDouble content() { return new jolie.runtime.embedding.java.JolieNative.JolieDouble( contentValue ); }
    
    public static Builder builder() { return new Builder(); }
    public static Builder builder( java.lang.Double contentValue ) { return builder().contentValue( contentValue ); }
    public static Builder builder( jolie.runtime.embedding.java.JolieValue from ) { return from != null ? new Builder( from ) : builder(); }
    
    public static jolie.runtime.embedding.java.util.StructureListBuilder<DoubleBasicType, Builder> listBuilder() { return new jolie.runtime.embedding.java.util.StructureListBuilder<>( DoubleBasicType::builder ); }
    public static jolie.runtime.embedding.java.util.StructureListBuilder<DoubleBasicType, Builder> listBuilder( java.util.SequencedCollection<? extends jolie.runtime.embedding.java.JolieValue> from ) {
        return from != null ? new jolie.runtime.embedding.java.util.StructureListBuilder<>( from, DoubleBasicType::from, DoubleBasicType::builder ) : listBuilder();
    }
    
    public static DoubleBasicType from( jolie.runtime.embedding.java.JolieValue j ) throws jolie.runtime.embedding.java.TypeValidationException {
        return new DoubleBasicType(
            jolie.runtime.embedding.java.JolieNative.JolieDouble.from( j ).value(),
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getChildOrDefault( "refinements", java.util.List.of() ), joliex.meta.spec.types.DoubleRefinement::from )
        );
    }
    
    public static DoubleBasicType fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
        jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
        return new DoubleBasicType(
            jolie.runtime.embedding.java.JolieNative.JolieDouble.contentFromValue( v ),
            jolie.runtime.embedding.java.util.ValueManager.vectorFieldFrom( v, "refinements", joliex.meta.spec.types.DoubleRefinement::fromValue )
        );
    }
    
    public static jolie.runtime.Value toValue( DoubleBasicType t ) {
        final jolie.runtime.Value v = jolie.runtime.Value.create( t.contentValue() );
        
        t.refinements().forEach( c -> v.getNewChild( "refinements" ).deepCopy( joliex.meta.spec.types.DoubleRefinement.toValue( c ) ) );
        
        return v;
    }
    
    public static class Builder {
        
        private java.lang.Double contentValue;
        private java.util.SequencedCollection<joliex.meta.spec.types.DoubleRefinement> refinements;
        
        private Builder() {}
        private Builder( jolie.runtime.embedding.java.JolieValue j ) {
            
            contentValue = j.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieDouble content ? content.value() : null;
            this.refinements = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getChildOrDefault( "refinements", java.util.List.of() ), joliex.meta.spec.types.DoubleRefinement::from );
        }
        
        public Builder contentValue( java.lang.Double contentValue ) { this.contentValue = contentValue; return this; }
        public Builder refinements( java.util.SequencedCollection<joliex.meta.spec.types.DoubleRefinement> refinements ) { this.refinements = refinements; return this; }
        public Builder refinements( java.util.function.Function<jolie.runtime.embedding.java.util.StructureListBuilder<joliex.meta.spec.types.DoubleRefinement, joliex.meta.spec.types.DoubleRefinement.Builder>, java.util.List<joliex.meta.spec.types.DoubleRefinement>> f ) { return refinements( f.apply( new jolie.runtime.embedding.java.util.StructureListBuilder<>( joliex.meta.spec.types.DoubleRefinement::builder ) ) ); }
        
        public DoubleBasicType build() {
            return new DoubleBasicType( contentValue, refinements );
        }
    }
}