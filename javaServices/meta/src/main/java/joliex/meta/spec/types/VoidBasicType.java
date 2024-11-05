package joliex.meta.spec.types;

/**
 * this class is a {@link jolie.runtime.embedding.java.TypedStructure} which can be described as follows:
 * <pre>
 * _void("void"): {@link jolie.runtime.embedding.java.JolieNative.JolieVoid}
 * </pre>
 * 
 * @see jolie.runtime.embedding.java.JolieValue
 * @see jolie.runtime.embedding.java.JolieNative
 * @see #builder()
 */
public final class VoidBasicType extends jolie.runtime.embedding.java.TypedStructure {
    
    private static final java.util.Set<java.lang.String> FIELD_KEYS = fieldKeys( VoidBasicType.class );
    
    @jolie.runtime.embedding.java.util.JolieName("void")
    private final jolie.runtime.embedding.java.JolieNative.JolieVoid _void;
    
    public VoidBasicType( jolie.runtime.embedding.java.JolieNative.JolieVoid _void ) {
        this._void = jolie.runtime.embedding.java.util.ValueManager.validated( "_void", _void );
    }
    
    public jolie.runtime.embedding.java.JolieNative.JolieVoid _void() { return _void; }
    
    public jolie.runtime.embedding.java.JolieNative.JolieVoid content() { return new jolie.runtime.embedding.java.JolieNative.JolieVoid(); }
    
    public static Builder builder() { return new Builder(); }
    public static Builder builder( jolie.runtime.embedding.java.JolieValue from ) { return from != null ? new Builder( from ) : builder(); }
    
    public static jolie.runtime.embedding.java.util.StructureListBuilder<VoidBasicType, Builder> listBuilder() { return new jolie.runtime.embedding.java.util.StructureListBuilder<>( VoidBasicType::builder ); }
    public static jolie.runtime.embedding.java.util.StructureListBuilder<VoidBasicType, Builder> listBuilder( java.util.SequencedCollection<? extends jolie.runtime.embedding.java.JolieValue> from ) {
        return from != null ? new jolie.runtime.embedding.java.util.StructureListBuilder<>( from, VoidBasicType::from, VoidBasicType::builder ) : listBuilder();
    }
    
    public static VoidBasicType from( jolie.runtime.embedding.java.JolieValue j ) throws jolie.runtime.embedding.java.TypeValidationException {
        return new VoidBasicType(
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "void" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieVoid content ? content : null )
        );
    }
    
    public static VoidBasicType fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
        jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
        return new VoidBasicType(
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "void", jolie.runtime.embedding.java.JolieNative.JolieVoid::fromValue )
        );
    }
    
    public static jolie.runtime.Value toValue( VoidBasicType t ) {
        final jolie.runtime.Value v = jolie.runtime.Value.create();
        
        v.getFirstChild( "void" ).setValue( t._void().value() );
        
        return v;
    }
    
    public static class Builder {
        
        private jolie.runtime.embedding.java.JolieNative.JolieVoid _void;
        
        private Builder() {}
        private Builder( jolie.runtime.embedding.java.JolieValue j ) {
            this._void = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "void" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieVoid content ? content : null );
        }
        
        public Builder _void( jolie.runtime.embedding.java.JolieNative.JolieVoid _void ) { this._void = _void; return this; }
        
        public VoidBasicType build() {
            return new VoidBasicType( _void );
        }
    }
}