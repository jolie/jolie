package joliex.meta.spec.types;

/**
 * this class is a {@link jolie.runtime.embedding.java.TypedStructure} which can be described as follows:
 * <pre>
 * start: {@link joliex.meta.spec.types.Position}
 * end: {@link joliex.meta.spec.types.Position}
 * </pre>
 * 
 * @see jolie.runtime.embedding.java.JolieValue
 * @see jolie.runtime.embedding.java.JolieNative
 * @see joliex.meta.spec.types.Position
 * @see #builder()
 */
public final class Range extends jolie.runtime.embedding.java.TypedStructure {
    
    private static final java.util.Set<java.lang.String> FIELD_KEYS = fieldKeys( Range.class );
    
    @jolie.runtime.embedding.java.util.JolieName("start")
    private final joliex.meta.spec.types.Position start;
    @jolie.runtime.embedding.java.util.JolieName("end")
    private final joliex.meta.spec.types.Position end;
    
    public Range( joliex.meta.spec.types.Position start, joliex.meta.spec.types.Position end ) {
        this.start = jolie.runtime.embedding.java.util.ValueManager.validated( "start", start );
        this.end = jolie.runtime.embedding.java.util.ValueManager.validated( "end", end );
    }
    
    public joliex.meta.spec.types.Position start() { return start; }
    public joliex.meta.spec.types.Position end() { return end; }
    
    public jolie.runtime.embedding.java.JolieNative.JolieVoid content() { return new jolie.runtime.embedding.java.JolieNative.JolieVoid(); }
    
    public static Builder builder() { return new Builder(); }
    public static Builder builder( jolie.runtime.embedding.java.JolieValue from ) { return from != null ? new Builder( from ) : builder(); }
    
    public static jolie.runtime.embedding.java.util.StructureListBuilder<Range, Builder> listBuilder() { return new jolie.runtime.embedding.java.util.StructureListBuilder<>( Range::builder ); }
    public static jolie.runtime.embedding.java.util.StructureListBuilder<Range, Builder> listBuilder( java.util.SequencedCollection<? extends jolie.runtime.embedding.java.JolieValue> from ) {
        return from != null ? new jolie.runtime.embedding.java.util.StructureListBuilder<>( from, Range::from, Range::builder ) : listBuilder();
    }
    
    public static Range from( jolie.runtime.embedding.java.JolieValue j ) throws jolie.runtime.embedding.java.TypeValidationException {
        return new Range(
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "start" ), joliex.meta.spec.types.Position::from ),
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "end" ), joliex.meta.spec.types.Position::from )
        );
    }
    
    public static Range fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
        jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
        return new Range(
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "start", joliex.meta.spec.types.Position::fromValue ),
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "end", joliex.meta.spec.types.Position::fromValue )
        );
    }
    
    public static jolie.runtime.Value toValue( Range t ) {
        final jolie.runtime.Value v = jolie.runtime.Value.create();
        
        v.getFirstChild( "start" ).deepCopy( joliex.meta.spec.types.Position.toValue( t.start() ) );
        v.getFirstChild( "end" ).deepCopy( joliex.meta.spec.types.Position.toValue( t.end() ) );
        
        return v;
    }
    
    public static class Builder {
        
        private joliex.meta.spec.types.Position start;
        private joliex.meta.spec.types.Position end;
        
        private Builder() {}
        private Builder( jolie.runtime.embedding.java.JolieValue j ) {
            this.start = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "start" ), joliex.meta.spec.types.Position::from );
            this.end = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "end" ), joliex.meta.spec.types.Position::from );
        }
        
        public Builder start( joliex.meta.spec.types.Position start ) { this.start = start; return this; }
        public Builder start( java.util.function.Function<joliex.meta.spec.types.Position.Builder, joliex.meta.spec.types.Position> f ) { return start( f.apply( joliex.meta.spec.types.Position.builder() ) ); }
        public Builder end( joliex.meta.spec.types.Position end ) { this.end = end; return this; }
        public Builder end( java.util.function.Function<joliex.meta.spec.types.Position.Builder, joliex.meta.spec.types.Position> f ) { return end( f.apply( joliex.meta.spec.types.Position.builder() ) ); }
        
        public Range build() {
            return new Range( start, end );
        }
    }
}