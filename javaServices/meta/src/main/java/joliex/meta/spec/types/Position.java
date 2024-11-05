package joliex.meta.spec.types;

/**
 * this class is a {@link jolie.runtime.embedding.java.TypedStructure} which can be described as follows:
 * <pre>
 * character: {@link java.lang.Integer}
 * line: {@link java.lang.Integer}
 * </pre>
 * 
 * @see jolie.runtime.embedding.java.JolieValue
 * @see jolie.runtime.embedding.java.JolieNative
 * @see #builder()
 */
public final class Position extends jolie.runtime.embedding.java.TypedStructure {
    
    private static final java.util.Set<java.lang.String> FIELD_KEYS = fieldKeys( Position.class );
    
    @jolie.runtime.embedding.java.util.JolieName("character")
    private final java.lang.Integer character;
    @jolie.runtime.embedding.java.util.JolieName("line")
    private final java.lang.Integer line;
    
    public Position( java.lang.Integer character, java.lang.Integer line ) {
        this.character = jolie.runtime.embedding.java.util.ValueManager.validated( "character", character );
        this.line = jolie.runtime.embedding.java.util.ValueManager.validated( "line", line );
    }
    
    public java.lang.Integer character() { return character; }
    public java.lang.Integer line() { return line; }
    
    public jolie.runtime.embedding.java.JolieNative.JolieVoid content() { return new jolie.runtime.embedding.java.JolieNative.JolieVoid(); }
    
    public static Builder builder() { return new Builder(); }
    public static Builder builder( jolie.runtime.embedding.java.JolieValue from ) { return from != null ? new Builder( from ) : builder(); }
    
    public static jolie.runtime.embedding.java.util.StructureListBuilder<Position, Builder> listBuilder() { return new jolie.runtime.embedding.java.util.StructureListBuilder<>( Position::builder ); }
    public static jolie.runtime.embedding.java.util.StructureListBuilder<Position, Builder> listBuilder( java.util.SequencedCollection<? extends jolie.runtime.embedding.java.JolieValue> from ) {
        return from != null ? new jolie.runtime.embedding.java.util.StructureListBuilder<>( from, Position::from, Position::builder ) : listBuilder();
    }
    
    public static Position from( jolie.runtime.embedding.java.JolieValue j ) throws jolie.runtime.embedding.java.TypeValidationException {
        return new Position(
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "character" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieInt content ? content.value() : null ),
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "line" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieInt content ? content.value() : null )
        );
    }
    
    public static Position fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
        jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
        return new Position(
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "character", jolie.runtime.embedding.java.JolieNative.JolieInt::fieldFromValue ),
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "line", jolie.runtime.embedding.java.JolieNative.JolieInt::fieldFromValue )
        );
    }
    
    public static jolie.runtime.Value toValue( Position t ) {
        final jolie.runtime.Value v = jolie.runtime.Value.create();
        
        v.getFirstChild( "character" ).setValue( t.character() );
        v.getFirstChild( "line" ).setValue( t.line() );
        
        return v;
    }
    
    public static class Builder {
        
        private java.lang.Integer character;
        private java.lang.Integer line;
        
        private Builder() {}
        private Builder( jolie.runtime.embedding.java.JolieValue j ) {
            this.character = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "character" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieInt content ? content.value() : null );
            this.line = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "line" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieInt content ? content.value() : null );
        }
        
        public Builder character( java.lang.Integer character ) { this.character = character; return this; }
        public Builder line( java.lang.Integer line ) { this.line = line; return this; }
        
        public Position build() {
            return new Position( character, line );
        }
    }
}