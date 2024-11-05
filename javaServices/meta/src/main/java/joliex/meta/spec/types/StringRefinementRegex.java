package joliex.meta.spec.types;

/**
 * this class is a {@link jolie.runtime.embedding.java.TypedStructure} which can be described as follows:
 * <pre>
 * regex: {@link java.lang.String}
 * </pre>
 * 
 * @see jolie.runtime.embedding.java.JolieValue
 * @see jolie.runtime.embedding.java.JolieNative
 * @see #builder()
 */
public final class StringRefinementRegex extends jolie.runtime.embedding.java.TypedStructure {
    
    private static final java.util.Set<java.lang.String> FIELD_KEYS = fieldKeys( StringRefinementRegex.class );
    
    @jolie.runtime.embedding.java.util.JolieName("regex")
    private final java.lang.String regex;
    
    public StringRefinementRegex( java.lang.String regex ) {
        this.regex = jolie.runtime.embedding.java.util.ValueManager.validated( "regex", regex );
    }
    
    public java.lang.String regex() { return regex; }
    
    public jolie.runtime.embedding.java.JolieNative.JolieVoid content() { return new jolie.runtime.embedding.java.JolieNative.JolieVoid(); }
    
    public static Builder builder() { return new Builder(); }
    public static Builder builder( jolie.runtime.embedding.java.JolieValue from ) { return from != null ? new Builder( from ) : builder(); }
    
    public static jolie.runtime.embedding.java.util.StructureListBuilder<StringRefinementRegex, Builder> listBuilder() { return new jolie.runtime.embedding.java.util.StructureListBuilder<>( StringRefinementRegex::builder ); }
    public static jolie.runtime.embedding.java.util.StructureListBuilder<StringRefinementRegex, Builder> listBuilder( java.util.SequencedCollection<? extends jolie.runtime.embedding.java.JolieValue> from ) {
        return from != null ? new jolie.runtime.embedding.java.util.StructureListBuilder<>( from, StringRefinementRegex::from, StringRefinementRegex::builder ) : listBuilder();
    }
    
    public static StringRefinementRegex from( jolie.runtime.embedding.java.JolieValue j ) throws jolie.runtime.embedding.java.TypeValidationException {
        return new StringRefinementRegex(
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "regex" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieString content ? content.value() : null )
        );
    }
    
    public static StringRefinementRegex fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
        jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
        return new StringRefinementRegex(
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "regex", jolie.runtime.embedding.java.JolieNative.JolieString::fieldFromValue )
        );
    }
    
    public static jolie.runtime.Value toValue( StringRefinementRegex t ) {
        final jolie.runtime.Value v = jolie.runtime.Value.create();
        
        v.getFirstChild( "regex" ).setValue( t.regex() );
        
        return v;
    }
    
    public static class Builder {
        
        private java.lang.String regex;
        
        private Builder() {}
        private Builder( jolie.runtime.embedding.java.JolieValue j ) {
            this.regex = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "regex" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieString content ? content.value() : null );
        }
        
        public Builder regex( java.lang.String regex ) { this.regex = regex; return this; }
        
        public StringRefinementRegex build() {
            return new StringRefinementRegex( regex );
        }
    }
}