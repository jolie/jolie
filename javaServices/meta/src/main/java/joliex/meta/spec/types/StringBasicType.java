package joliex.meta.spec.types;

/**
 * this class is a {@link jolie.runtime.embedding.java.TypedStructure} which can be described as follows:
 * <pre>
 * 
 * contentValue: {@link java.lang.String}
     * refinements[0,2147483647]: {@link joliex.meta.spec.types.StringRefinement}
 * </pre>
 * 
 * @see jolie.runtime.embedding.java.JolieValue
 * @see jolie.runtime.embedding.java.JolieNative
 * @see joliex.meta.spec.types.StringRefinement
 * @see #builder()
 */
public final class StringBasicType extends jolie.runtime.embedding.java.TypedStructure {
    
    private static final java.util.Set<java.lang.String> FIELD_KEYS = fieldKeys( StringBasicType.class );
    
    private final java.lang.String contentValue;
    @jolie.runtime.embedding.java.util.JolieName("refinements")
    private final java.util.List<joliex.meta.spec.types.StringRefinement> refinements;
    
    public StringBasicType( java.lang.String contentValue, java.util.SequencedCollection<joliex.meta.spec.types.StringRefinement> refinements ) {
        this.contentValue = jolie.runtime.embedding.java.util.ValueManager.validated( "contentValue", contentValue );
        this.refinements = jolie.runtime.embedding.java.util.ValueManager.validated( "refinements", refinements, 0, 2147483647, t -> t );
    }
    
    public java.lang.String contentValue() { return contentValue; }
    public java.util.List<joliex.meta.spec.types.StringRefinement> refinements() { return refinements; }
    
    public jolie.runtime.embedding.java.JolieNative.JolieString content() { return new jolie.runtime.embedding.java.JolieNative.JolieString( contentValue ); }
    
    public static Builder builder() { return new Builder(); }
    public static Builder builder( java.lang.String contentValue ) { return builder().contentValue( contentValue ); }
    public static Builder builder( jolie.runtime.embedding.java.JolieValue from ) { return from != null ? new Builder( from ) : builder(); }
    
    public static jolie.runtime.embedding.java.util.StructureListBuilder<StringBasicType, Builder> listBuilder() { return new jolie.runtime.embedding.java.util.StructureListBuilder<>( StringBasicType::builder ); }
    public static jolie.runtime.embedding.java.util.StructureListBuilder<StringBasicType, Builder> listBuilder( java.util.SequencedCollection<? extends jolie.runtime.embedding.java.JolieValue> from ) {
        return from != null ? new jolie.runtime.embedding.java.util.StructureListBuilder<>( from, StringBasicType::from, StringBasicType::builder ) : listBuilder();
    }
    
    public static StringBasicType from( jolie.runtime.embedding.java.JolieValue j ) throws jolie.runtime.embedding.java.TypeValidationException {
        return new StringBasicType(
            jolie.runtime.embedding.java.JolieNative.JolieString.from( j ).value(),
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getChildOrDefault( "refinements", java.util.List.of() ), joliex.meta.spec.types.StringRefinement::from )
        );
    }
    
    public static StringBasicType fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
        jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
        return new StringBasicType(
            jolie.runtime.embedding.java.JolieNative.JolieString.contentFromValue( v ),
            jolie.runtime.embedding.java.util.ValueManager.vectorFieldFrom( v, "refinements", joliex.meta.spec.types.StringRefinement::fromValue )
        );
    }
    
    public static jolie.runtime.Value toValue( StringBasicType t ) {
        final jolie.runtime.Value v = jolie.runtime.Value.create( t.contentValue() );
        
        t.refinements().forEach( c -> v.getNewChild( "refinements" ).deepCopy( joliex.meta.spec.types.StringRefinement.toValue( c ) ) );
        
        return v;
    }
    
    public static class Builder {
        
        private java.lang.String contentValue;
        private java.util.SequencedCollection<joliex.meta.spec.types.StringRefinement> refinements;
        
        private Builder() {}
        private Builder( jolie.runtime.embedding.java.JolieValue j ) {
            
            contentValue = j.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieString content ? content.value() : null;
            this.refinements = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getChildOrDefault( "refinements", java.util.List.of() ), joliex.meta.spec.types.StringRefinement::from );
        }
        
        public Builder contentValue( java.lang.String contentValue ) { this.contentValue = contentValue; return this; }
        public Builder refinements( java.util.SequencedCollection<joliex.meta.spec.types.StringRefinement> refinements ) { this.refinements = refinements; return this; }
        public Builder refinements( java.util.function.Function<joliex.meta.spec.types.StringRefinement.ListBuilder, java.util.List<joliex.meta.spec.types.StringRefinement>> f ) { return refinements( f.apply( joliex.meta.spec.types.StringRefinement.listBuilder() ) ); }
        
        public StringBasicType build() {
            return new StringBasicType( contentValue, refinements );
        }
    }
}