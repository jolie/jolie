package joliex.meta.spec.types;

/**
 * this class is a {@link jolie.runtime.embedding.java.TypedStructure} which can be described as follows:
 * <pre>
 * textLocation: {@link joliex.meta.spec.types.Location}
 * outputPort: {@link joliex.meta.spec.types.LocatedSymbolRef}
 * name: {@link joliex.meta.spec.types.LocatedString}
 * </pre>
 * 
 * @see jolie.runtime.embedding.java.JolieValue
 * @see jolie.runtime.embedding.java.JolieNative
 * @see joliex.meta.spec.types.Location
 * @see joliex.meta.spec.types.LocatedSymbolRef
 * @see joliex.meta.spec.types.LocatedString
 * @see #builder()
 */
public final class Redirection extends jolie.runtime.embedding.java.TypedStructure {
    
    private static final java.util.Set<java.lang.String> FIELD_KEYS = fieldKeys( Redirection.class );
    
    @jolie.runtime.embedding.java.util.JolieName("textLocation")
    private final joliex.meta.spec.types.Location textLocation;
    @jolie.runtime.embedding.java.util.JolieName("outputPort")
    private final joliex.meta.spec.types.LocatedSymbolRef outputPort;
    @jolie.runtime.embedding.java.util.JolieName("name")
    private final joliex.meta.spec.types.LocatedString name;
    
    public Redirection( joliex.meta.spec.types.Location textLocation, joliex.meta.spec.types.LocatedSymbolRef outputPort, joliex.meta.spec.types.LocatedString name ) {
        this.textLocation = jolie.runtime.embedding.java.util.ValueManager.validated( "textLocation", textLocation );
        this.outputPort = jolie.runtime.embedding.java.util.ValueManager.validated( "outputPort", outputPort );
        this.name = jolie.runtime.embedding.java.util.ValueManager.validated( "name", name );
    }
    
    public joliex.meta.spec.types.Location textLocation() { return textLocation; }
    public joliex.meta.spec.types.LocatedSymbolRef outputPort() { return outputPort; }
    public joliex.meta.spec.types.LocatedString name() { return name; }
    
    public jolie.runtime.embedding.java.JolieNative.JolieVoid content() { return new jolie.runtime.embedding.java.JolieNative.JolieVoid(); }
    
    public static Builder builder() { return new Builder(); }
    public static Builder builder( jolie.runtime.embedding.java.JolieValue from ) { return from != null ? new Builder( from ) : builder(); }
    
    public static jolie.runtime.embedding.java.util.StructureListBuilder<Redirection, Builder> listBuilder() { return new jolie.runtime.embedding.java.util.StructureListBuilder<>( Redirection::builder ); }
    public static jolie.runtime.embedding.java.util.StructureListBuilder<Redirection, Builder> listBuilder( java.util.SequencedCollection<? extends jolie.runtime.embedding.java.JolieValue> from ) {
        return from != null ? new jolie.runtime.embedding.java.util.StructureListBuilder<>( from, Redirection::from, Redirection::builder ) : listBuilder();
    }
    
    public static Redirection from( jolie.runtime.embedding.java.JolieValue j ) throws jolie.runtime.embedding.java.TypeValidationException {
        return new Redirection(
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "textLocation" ), joliex.meta.spec.types.Location::from ),
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "outputPort" ), joliex.meta.spec.types.LocatedSymbolRef::from ),
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "name" ), joliex.meta.spec.types.LocatedString::from )
        );
    }
    
    public static Redirection fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
        jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
        return new Redirection(
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "textLocation", joliex.meta.spec.types.Location::fromValue ),
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "outputPort", joliex.meta.spec.types.LocatedSymbolRef::fromValue ),
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "name", joliex.meta.spec.types.LocatedString::fromValue )
        );
    }
    
    public static jolie.runtime.Value toValue( Redirection t ) {
        final jolie.runtime.Value v = jolie.runtime.Value.create();
        
        v.getFirstChild( "textLocation" ).deepCopy( joliex.meta.spec.types.Location.toValue( t.textLocation() ) );
        v.getFirstChild( "outputPort" ).deepCopy( joliex.meta.spec.types.LocatedSymbolRef.toValue( t.outputPort() ) );
        v.getFirstChild( "name" ).deepCopy( joliex.meta.spec.types.LocatedString.toValue( t.name() ) );
        
        return v;
    }
    
    public static class Builder {
        
        private joliex.meta.spec.types.Location textLocation;
        private joliex.meta.spec.types.LocatedSymbolRef outputPort;
        private joliex.meta.spec.types.LocatedString name;
        
        private Builder() {}
        private Builder( jolie.runtime.embedding.java.JolieValue j ) {
            this.textLocation = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "textLocation" ), joliex.meta.spec.types.Location::from );
            this.outputPort = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "outputPort" ), joliex.meta.spec.types.LocatedSymbolRef::from );
            this.name = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "name" ), joliex.meta.spec.types.LocatedString::from );
        }
        
        public Builder textLocation( joliex.meta.spec.types.Location textLocation ) { this.textLocation = textLocation; return this; }
        public Builder textLocation( java.util.function.Function<joliex.meta.spec.types.Location.Builder, joliex.meta.spec.types.Location> f ) { return textLocation( f.apply( joliex.meta.spec.types.Location.builder() ) ); }
        public Builder outputPort( joliex.meta.spec.types.LocatedSymbolRef outputPort ) { this.outputPort = outputPort; return this; }
        public Builder outputPort( java.util.function.Function<joliex.meta.spec.types.LocatedSymbolRef.Builder, joliex.meta.spec.types.LocatedSymbolRef> f ) { return outputPort( f.apply( joliex.meta.spec.types.LocatedSymbolRef.builder() ) ); }
        public Builder name( joliex.meta.spec.types.LocatedString name ) { this.name = name; return this; }
        public Builder name( java.util.function.Function<joliex.meta.spec.types.LocatedString.Builder, joliex.meta.spec.types.LocatedString> f ) { return name( f.apply( joliex.meta.spec.types.LocatedString.builder() ) ); }
        
        public Redirection build() {
            return new Redirection( textLocation, outputPort, name );
        }
    }
}