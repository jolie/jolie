package joliex.meta.spec.types;

/**
 * this class is a {@link jolie.runtime.embedding.java.TypedStructure} which can be described as follows:
 * <pre>
 * doubleTag("double"): {@link jolie.runtime.embedding.java.JolieNative.JolieVoid}
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
    
    @jolie.runtime.embedding.java.util.JolieName("double")
    private final jolie.runtime.embedding.java.JolieNative.JolieVoid doubleTag;
    @jolie.runtime.embedding.java.util.JolieName("refinements")
    private final java.util.List<joliex.meta.spec.types.DoubleRefinement> refinements;
    
    public DoubleBasicType( jolie.runtime.embedding.java.JolieNative.JolieVoid doubleTag, java.util.SequencedCollection<joliex.meta.spec.types.DoubleRefinement> refinements ) {
        this.doubleTag = jolie.runtime.embedding.java.util.ValueManager.validated( "doubleTag", doubleTag );
        this.refinements = jolie.runtime.embedding.java.util.ValueManager.validated( "refinements", refinements, 0, 2147483647, t -> t );
    }
    
    public jolie.runtime.embedding.java.JolieNative.JolieVoid doubleTag() { return doubleTag; }
    public java.util.List<joliex.meta.spec.types.DoubleRefinement> refinements() { return refinements; }
    
    public jolie.runtime.embedding.java.JolieNative.JolieVoid content() { return new jolie.runtime.embedding.java.JolieNative.JolieVoid(); }
    
    public static Builder builder() { return new Builder(); }
    public static Builder builder( jolie.runtime.embedding.java.JolieValue from ) { return from != null ? new Builder( from ) : builder(); }
    
    public static jolie.runtime.embedding.java.util.StructureListBuilder<DoubleBasicType, Builder> listBuilder() { return new jolie.runtime.embedding.java.util.StructureListBuilder<>( DoubleBasicType::builder ); }
    public static jolie.runtime.embedding.java.util.StructureListBuilder<DoubleBasicType, Builder> listBuilder( java.util.SequencedCollection<? extends jolie.runtime.embedding.java.JolieValue> from ) {
        return from != null ? new jolie.runtime.embedding.java.util.StructureListBuilder<>( from, DoubleBasicType::from, DoubleBasicType::builder ) : listBuilder();
    }
    
    public static DoubleBasicType from( jolie.runtime.embedding.java.JolieValue j ) throws jolie.runtime.embedding.java.TypeValidationException {
        return new DoubleBasicType(
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "double" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieVoid content ? content : null ),
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getChildOrDefault( "refinements", java.util.List.of() ), joliex.meta.spec.types.DoubleRefinement::from )
        );
    }
    
    public static DoubleBasicType fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
        jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
        return new DoubleBasicType(
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "double", jolie.runtime.embedding.java.JolieNative.JolieVoid::fromValue ),
            jolie.runtime.embedding.java.util.ValueManager.vectorFieldFrom( v, "refinements", joliex.meta.spec.types.DoubleRefinement::fromValue )
        );
    }
    
    public static jolie.runtime.Value toValue( DoubleBasicType t ) {
        final jolie.runtime.Value v = jolie.runtime.Value.create();
        
        v.getFirstChild( "double" ).setValue( t.doubleTag().value() );
        t.refinements().forEach( c -> v.getNewChild( "refinements" ).deepCopy( joliex.meta.spec.types.DoubleRefinement.toValue( c ) ) );
        
        return v;
    }
    
    public static class Builder {
        
        private jolie.runtime.embedding.java.JolieNative.JolieVoid doubleTag;
        private java.util.SequencedCollection<joliex.meta.spec.types.DoubleRefinement> refinements;
        
        private Builder() {}
        private Builder( jolie.runtime.embedding.java.JolieValue j ) {
            this.doubleTag = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "double" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieVoid content ? content : null );
            this.refinements = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getChildOrDefault( "refinements", java.util.List.of() ), joliex.meta.spec.types.DoubleRefinement::from );
        }
        
        public Builder doubleTag( jolie.runtime.embedding.java.JolieNative.JolieVoid doubleTag ) { this.doubleTag = doubleTag; return this; }
        public Builder refinements( java.util.SequencedCollection<joliex.meta.spec.types.DoubleRefinement> refinements ) { this.refinements = refinements; return this; }
        public Builder refinements( java.util.function.Function<jolie.runtime.embedding.java.util.StructureListBuilder<joliex.meta.spec.types.DoubleRefinement, joliex.meta.spec.types.DoubleRefinement.Builder>, java.util.List<joliex.meta.spec.types.DoubleRefinement>> f ) { return refinements( f.apply( new jolie.runtime.embedding.java.util.StructureListBuilder<>( joliex.meta.spec.types.DoubleRefinement::builder ) ) ); }
        
        public DoubleBasicType build() {
            return new DoubleBasicType( doubleTag, refinements );
        }
    }
}