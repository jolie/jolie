package joliex.util.spec.types;

/**
 * this class is a {@link jolie.runtime.embedding.java.TypedStructure} which can be described as follows:
 * <pre>
 * 
 * contentValue: {@link java.lang.Integer}
     * group[0,2147483647]: {@link java.lang.String}
 * </pre>
 * 
 * @see jolie.runtime.embedding.java.JolieValue
 * @see jolie.runtime.embedding.java.JolieNative
 * @see #builder()
 */
public final class MatchResult extends jolie.runtime.embedding.java.TypedStructure {
    
    private static final java.util.Set<java.lang.String> FIELD_KEYS = fieldKeys( MatchResult.class );
    
    private final java.lang.Integer contentValue;
    @jolie.runtime.embedding.java.util.JolieName("group")
    private final java.util.List<java.lang.String> group;
    
    public MatchResult( java.lang.Integer contentValue, java.util.SequencedCollection<java.lang.String> group ) {
        this.contentValue = jolie.runtime.embedding.java.util.ValueManager.validated( "contentValue", contentValue );
        this.group = jolie.runtime.embedding.java.util.ValueManager.validated( "group", group, 0, 2147483647, t -> t );
    }
    
    public java.lang.Integer contentValue() { return contentValue; }
    public java.util.List<java.lang.String> group() { return group; }
    
    public jolie.runtime.embedding.java.JolieNative.JolieInt content() { return new jolie.runtime.embedding.java.JolieNative.JolieInt( contentValue ); }
    
    public static Builder builder() { return new Builder(); }
    public static Builder builder( java.lang.Integer contentValue ) { return builder().contentValue( contentValue ); }
    public static Builder builder( jolie.runtime.embedding.java.JolieValue from ) { return new Builder( from ); }
    
    public static jolie.runtime.embedding.java.util.StructureListBuilder<MatchResult, Builder> listBuilder() { return new jolie.runtime.embedding.java.util.StructureListBuilder<>( MatchResult::builder, MatchResult::builder ); }
    public static jolie.runtime.embedding.java.util.StructureListBuilder<MatchResult, Builder> listBuilder( java.util.SequencedCollection<? extends jolie.runtime.embedding.java.JolieValue> from ) {
        return new jolie.runtime.embedding.java.util.StructureListBuilder<>( from, MatchResult::from, MatchResult::builder, MatchResult::builder );
    }
    
    public static MatchResult from( jolie.runtime.embedding.java.JolieValue j ) throws jolie.runtime.embedding.java.TypeValidationException {
        return new MatchResult(
            jolie.runtime.embedding.java.JolieNative.JolieInt.from( j ).value(),
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getChildOrDefault( "group", java.util.List.of() ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieString content ? content.value() : null )
        );
    }
    
    public static MatchResult fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
        jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
        return new MatchResult(
            jolie.runtime.embedding.java.JolieNative.JolieInt.contentFromValue( v ),
            jolie.runtime.embedding.java.util.ValueManager.vectorFieldFrom( v, "group", jolie.runtime.embedding.java.JolieNative.JolieString::fieldFromValue )
        );
    }
    
    public static jolie.runtime.Value toValue( MatchResult t ) {
        final jolie.runtime.Value v = jolie.runtime.Value.create( t.contentValue() );
        
        t.group().forEach( c -> v.getNewChild( "group" ).setValue( c ) );
        
        return v;
    }
    
    public static class Builder {
        
        private java.lang.Integer contentValue;
        private java.util.SequencedCollection<java.lang.String> group;
        
        private Builder() {}
        private Builder( jolie.runtime.embedding.java.JolieValue j ) {
            
            contentValue = j.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieInt content ? content.value() : null;
            this.group = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getChildOrDefault( "group", java.util.List.of() ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieString content ? content.value() : null );
        }
        
        public Builder contentValue( java.lang.Integer contentValue ) { this.contentValue = contentValue; return this; }
        public Builder group( java.util.SequencedCollection<java.lang.String> group ) { this.group = group; return this; }
        public Builder group( java.lang.String... values ) { return group( java.util.List.of( values ) ); }
        
        public MatchResult build() {
            return new MatchResult( contentValue, group );
        }
    }
}