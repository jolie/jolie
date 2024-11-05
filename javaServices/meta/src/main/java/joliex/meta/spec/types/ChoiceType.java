package joliex.meta.spec.types;

/**
 * this class is a {@link jolie.runtime.embedding.java.TypedStructure} which can be described as follows:
 * <pre>
 * textLocation: {@link joliex.meta.spec.types.Location}
 * left: {@link joliex.meta.spec.types.Type}
 * right: {@link joliex.meta.spec.types.Type}
 * </pre>
 * 
 * @see jolie.runtime.embedding.java.JolieValue
 * @see jolie.runtime.embedding.java.JolieNative
 * @see joliex.meta.spec.types.Location
 * @see joliex.meta.spec.types.Type
 * @see #builder()
 */
public final class ChoiceType extends jolie.runtime.embedding.java.TypedStructure {
    
    private static final java.util.Set<java.lang.String> FIELD_KEYS = fieldKeys( ChoiceType.class );
    
    @jolie.runtime.embedding.java.util.JolieName("textLocation")
    private final joliex.meta.spec.types.Location textLocation;
    @jolie.runtime.embedding.java.util.JolieName("left")
    private final joliex.meta.spec.types.Type left;
    @jolie.runtime.embedding.java.util.JolieName("right")
    private final joliex.meta.spec.types.Type right;
    
    public ChoiceType( joliex.meta.spec.types.Location textLocation, joliex.meta.spec.types.Type left, joliex.meta.spec.types.Type right ) {
        this.textLocation = jolie.runtime.embedding.java.util.ValueManager.validated( "textLocation", textLocation );
        this.left = jolie.runtime.embedding.java.util.ValueManager.validated( "left", left );
        this.right = jolie.runtime.embedding.java.util.ValueManager.validated( "right", right );
    }
    
    public joliex.meta.spec.types.Location textLocation() { return textLocation; }
    public joliex.meta.spec.types.Type left() { return left; }
    public joliex.meta.spec.types.Type right() { return right; }
    
    public jolie.runtime.embedding.java.JolieNative.JolieVoid content() { return new jolie.runtime.embedding.java.JolieNative.JolieVoid(); }
    
    public static Builder builder() { return new Builder(); }
    public static Builder builder( jolie.runtime.embedding.java.JolieValue from ) { return from != null ? new Builder( from ) : builder(); }
    
    public static jolie.runtime.embedding.java.util.StructureListBuilder<ChoiceType, Builder> listBuilder() { return new jolie.runtime.embedding.java.util.StructureListBuilder<>( ChoiceType::builder ); }
    public static jolie.runtime.embedding.java.util.StructureListBuilder<ChoiceType, Builder> listBuilder( java.util.SequencedCollection<? extends jolie.runtime.embedding.java.JolieValue> from ) {
        return from != null ? new jolie.runtime.embedding.java.util.StructureListBuilder<>( from, ChoiceType::from, ChoiceType::builder ) : listBuilder();
    }
    
    public static ChoiceType from( jolie.runtime.embedding.java.JolieValue j ) throws jolie.runtime.embedding.java.TypeValidationException {
        return new ChoiceType(
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "textLocation" ), joliex.meta.spec.types.Location::from ),
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "left" ), joliex.meta.spec.types.Type::from ),
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "right" ), joliex.meta.spec.types.Type::from )
        );
    }
    
    public static ChoiceType fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
        jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
        return new ChoiceType(
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "textLocation", joliex.meta.spec.types.Location::fromValue ),
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "left", joliex.meta.spec.types.Type::fromValue ),
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "right", joliex.meta.spec.types.Type::fromValue )
        );
    }
    
    public static jolie.runtime.Value toValue( ChoiceType t ) {
        final jolie.runtime.Value v = jolie.runtime.Value.create();
        
        v.getFirstChild( "textLocation" ).deepCopy( joliex.meta.spec.types.Location.toValue( t.textLocation() ) );
        v.getFirstChild( "left" ).deepCopy( joliex.meta.spec.types.Type.toValue( t.left() ) );
        v.getFirstChild( "right" ).deepCopy( joliex.meta.spec.types.Type.toValue( t.right() ) );
        
        return v;
    }
    
    public static class Builder {
        
        private joliex.meta.spec.types.Location textLocation;
        private joliex.meta.spec.types.Type left;
        private joliex.meta.spec.types.Type right;
        
        private Builder() {}
        private Builder( jolie.runtime.embedding.java.JolieValue j ) {
            this.textLocation = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "textLocation" ), joliex.meta.spec.types.Location::from );
            this.left = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "left" ), joliex.meta.spec.types.Type::from );
            this.right = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "right" ), joliex.meta.spec.types.Type::from );
        }
        
        public Builder textLocation( joliex.meta.spec.types.Location textLocation ) { this.textLocation = textLocation; return this; }
        public Builder textLocation( java.util.function.Function<joliex.meta.spec.types.Location.Builder, joliex.meta.spec.types.Location> f ) { return textLocation( f.apply( joliex.meta.spec.types.Location.builder() ) ); }
        public Builder left( joliex.meta.spec.types.Type left ) { this.left = left; return this; }
        public Builder right( joliex.meta.spec.types.Type right ) { this.right = right; return this; }
        
        public ChoiceType build() {
            return new ChoiceType( textLocation, left, right );
        }
    }
}