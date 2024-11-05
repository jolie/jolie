package joliex.meta.spec.types;

/**
 * this class is a {@link jolie.runtime.embedding.java.TypedStructure} which can be described as follows:
 * <pre>
 * _string("string"): {@link jolie.runtime.embedding.java.JolieNative.JolieVoid}
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
    
    @jolie.runtime.embedding.java.util.JolieName("string")
    private final jolie.runtime.embedding.java.JolieNative.JolieVoid _string;
    @jolie.runtime.embedding.java.util.JolieName("refinements")
    private final java.util.List<joliex.meta.spec.types.StringRefinement> refinements;
    
    public StringBasicType( jolie.runtime.embedding.java.JolieNative.JolieVoid _string, java.util.SequencedCollection<joliex.meta.spec.types.StringRefinement> refinements ) {
        this._string = jolie.runtime.embedding.java.util.ValueManager.validated( "_string", _string );
        this.refinements = jolie.runtime.embedding.java.util.ValueManager.validated( "refinements", refinements, 0, 2147483647, t -> t );
    }
    
    public jolie.runtime.embedding.java.JolieNative.JolieVoid _string() { return _string; }
    public java.util.List<joliex.meta.spec.types.StringRefinement> refinements() { return refinements; }
    
    public jolie.runtime.embedding.java.JolieNative.JolieVoid content() { return new jolie.runtime.embedding.java.JolieNative.JolieVoid(); }
    
    public static Builder builder() { return new Builder(); }
    public static Builder builder( jolie.runtime.embedding.java.JolieValue from ) { return from != null ? new Builder( from ) : builder(); }
    
    public static jolie.runtime.embedding.java.util.StructureListBuilder<StringBasicType, Builder> listBuilder() { return new jolie.runtime.embedding.java.util.StructureListBuilder<>( StringBasicType::builder ); }
    public static jolie.runtime.embedding.java.util.StructureListBuilder<StringBasicType, Builder> listBuilder( java.util.SequencedCollection<? extends jolie.runtime.embedding.java.JolieValue> from ) {
        return from != null ? new jolie.runtime.embedding.java.util.StructureListBuilder<>( from, StringBasicType::from, StringBasicType::builder ) : listBuilder();
    }
    
    public static StringBasicType from( jolie.runtime.embedding.java.JolieValue j ) throws jolie.runtime.embedding.java.TypeValidationException {
        return new StringBasicType(
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "string" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieVoid content ? content : null ),
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getChildOrDefault( "refinements", java.util.List.of() ), joliex.meta.spec.types.StringRefinement::from )
        );
    }
    
    public static StringBasicType fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
        jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
        return new StringBasicType(
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "string", jolie.runtime.embedding.java.JolieNative.JolieVoid::fromValue ),
            jolie.runtime.embedding.java.util.ValueManager.vectorFieldFrom( v, "refinements", joliex.meta.spec.types.StringRefinement::fromValue )
        );
    }
    
    public static jolie.runtime.Value toValue( StringBasicType t ) {
        final jolie.runtime.Value v = jolie.runtime.Value.create();
        
        v.getFirstChild( "string" ).setValue( t._string().value() );
        t.refinements().forEach( c -> v.getNewChild( "refinements" ).deepCopy( joliex.meta.spec.types.StringRefinement.toValue( c ) ) );
        
        return v;
    }
    
    public static class Builder {
        
        private jolie.runtime.embedding.java.JolieNative.JolieVoid _string;
        private java.util.SequencedCollection<joliex.meta.spec.types.StringRefinement> refinements;
        
        private Builder() {}
        private Builder( jolie.runtime.embedding.java.JolieValue j ) {
            this._string = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "string" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieVoid content ? content : null );
            this.refinements = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getChildOrDefault( "refinements", java.util.List.of() ), joliex.meta.spec.types.StringRefinement::from );
        }
        
        public Builder _string( jolie.runtime.embedding.java.JolieNative.JolieVoid _string ) { this._string = _string; return this; }
        public Builder refinements( java.util.SequencedCollection<joliex.meta.spec.types.StringRefinement> refinements ) { this.refinements = refinements; return this; }
        public Builder refinements( java.util.function.Function<joliex.meta.spec.types.StringRefinement.ListBuilder, java.util.List<joliex.meta.spec.types.StringRefinement>> f ) { return refinements( f.apply( joliex.meta.spec.types.StringRefinement.listBuilder() ) ); }
        
        public StringBasicType build() {
            return new StringBasicType( _string, refinements );
        }
    }
}