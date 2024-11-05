package joliex.meta.spec.types;

/**
 * this class is a {@link jolie.runtime.embedding.java.TypedStructure} which can be described as follows:
 * <pre>
 * textLocation: {@link joliex.meta.spec.types.Location}
 * documentation[0,1]: {@link joliex.meta.spec.types.Documentation}
 * outputPorts[0,2147483647]: {@link joliex.meta.spec.types.OutputPort}
 * name: {@link joliex.meta.spec.types.LocatedString}
 * inputPorts[0,2147483647]: {@link joliex.meta.spec.types.InputPort}
 * </pre>
 * 
 * @see jolie.runtime.embedding.java.JolieValue
 * @see jolie.runtime.embedding.java.JolieNative
 * @see joliex.meta.spec.types.Location
 * @see joliex.meta.spec.types.Documentation
 * @see joliex.meta.spec.types.OutputPort
 * @see joliex.meta.spec.types.LocatedString
 * @see joliex.meta.spec.types.InputPort
 * @see #builder()
 */
public final class ServiceDef extends jolie.runtime.embedding.java.TypedStructure {
    
    private static final java.util.Set<java.lang.String> FIELD_KEYS = fieldKeys( ServiceDef.class );
    
    @jolie.runtime.embedding.java.util.JolieName("textLocation")
    private final joliex.meta.spec.types.Location textLocation;
    @jolie.runtime.embedding.java.util.JolieName("documentation")
    private final joliex.meta.spec.types.Documentation documentation;
    @jolie.runtime.embedding.java.util.JolieName("outputPorts")
    private final java.util.List<joliex.meta.spec.types.OutputPort> outputPorts;
    @jolie.runtime.embedding.java.util.JolieName("name")
    private final joliex.meta.spec.types.LocatedString name;
    @jolie.runtime.embedding.java.util.JolieName("inputPorts")
    private final java.util.List<joliex.meta.spec.types.InputPort> inputPorts;
    
    public ServiceDef( joliex.meta.spec.types.Location textLocation, joliex.meta.spec.types.Documentation documentation, java.util.SequencedCollection<joliex.meta.spec.types.OutputPort> outputPorts, joliex.meta.spec.types.LocatedString name, java.util.SequencedCollection<joliex.meta.spec.types.InputPort> inputPorts ) {
        this.textLocation = jolie.runtime.embedding.java.util.ValueManager.validated( "textLocation", textLocation );
        this.documentation = documentation;
        this.outputPorts = jolie.runtime.embedding.java.util.ValueManager.validated( "outputPorts", outputPorts, 0, 2147483647, t -> t );
        this.name = jolie.runtime.embedding.java.util.ValueManager.validated( "name", name );
        this.inputPorts = jolie.runtime.embedding.java.util.ValueManager.validated( "inputPorts", inputPorts, 0, 2147483647, t -> t );
    }
    
    public joliex.meta.spec.types.Location textLocation() { return textLocation; }
    public java.util.Optional<joliex.meta.spec.types.Documentation> documentation() { return java.util.Optional.ofNullable( documentation ); }
    public java.util.List<joliex.meta.spec.types.OutputPort> outputPorts() { return outputPorts; }
    public joliex.meta.spec.types.LocatedString name() { return name; }
    public java.util.List<joliex.meta.spec.types.InputPort> inputPorts() { return inputPorts; }
    
    public jolie.runtime.embedding.java.JolieNative.JolieVoid content() { return new jolie.runtime.embedding.java.JolieNative.JolieVoid(); }
    
    public static Builder builder() { return new Builder(); }
    public static Builder builder( jolie.runtime.embedding.java.JolieValue from ) { return from != null ? new Builder( from ) : builder(); }
    
    public static jolie.runtime.embedding.java.util.StructureListBuilder<ServiceDef, Builder> listBuilder() { return new jolie.runtime.embedding.java.util.StructureListBuilder<>( ServiceDef::builder ); }
    public static jolie.runtime.embedding.java.util.StructureListBuilder<ServiceDef, Builder> listBuilder( java.util.SequencedCollection<? extends jolie.runtime.embedding.java.JolieValue> from ) {
        return from != null ? new jolie.runtime.embedding.java.util.StructureListBuilder<>( from, ServiceDef::from, ServiceDef::builder ) : listBuilder();
    }
    
    public static ServiceDef from( jolie.runtime.embedding.java.JolieValue j ) throws jolie.runtime.embedding.java.TypeValidationException {
        return new ServiceDef(
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "textLocation" ), joliex.meta.spec.types.Location::from ),
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "documentation" ), joliex.meta.spec.types.Documentation::from ),
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getChildOrDefault( "outputPorts", java.util.List.of() ), joliex.meta.spec.types.OutputPort::from ),
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "name" ), joliex.meta.spec.types.LocatedString::from ),
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getChildOrDefault( "inputPorts", java.util.List.of() ), joliex.meta.spec.types.InputPort::from )
        );
    }
    
    public static ServiceDef fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
        jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
        return new ServiceDef(
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "textLocation", joliex.meta.spec.types.Location::fromValue ),
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "documentation", joliex.meta.spec.types.Documentation::fromValue ),
            jolie.runtime.embedding.java.util.ValueManager.vectorFieldFrom( v, "outputPorts", joliex.meta.spec.types.OutputPort::fromValue ),
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "name", joliex.meta.spec.types.LocatedString::fromValue ),
            jolie.runtime.embedding.java.util.ValueManager.vectorFieldFrom( v, "inputPorts", joliex.meta.spec.types.InputPort::fromValue )
        );
    }
    
    public static jolie.runtime.Value toValue( ServiceDef t ) {
        final jolie.runtime.Value v = jolie.runtime.Value.create();
        
        v.getFirstChild( "textLocation" ).deepCopy( joliex.meta.spec.types.Location.toValue( t.textLocation() ) );
        t.documentation().ifPresent( c -> v.getFirstChild( "documentation" ).deepCopy( joliex.meta.spec.types.Documentation.toValue( c ) ) );
        t.outputPorts().forEach( c -> v.getNewChild( "outputPorts" ).deepCopy( joliex.meta.spec.types.OutputPort.toValue( c ) ) );
        v.getFirstChild( "name" ).deepCopy( joliex.meta.spec.types.LocatedString.toValue( t.name() ) );
        t.inputPorts().forEach( c -> v.getNewChild( "inputPorts" ).deepCopy( joliex.meta.spec.types.InputPort.toValue( c ) ) );
        
        return v;
    }
    
    public static class Builder {
        
        private joliex.meta.spec.types.Location textLocation;
        private joliex.meta.spec.types.Documentation documentation;
        private java.util.SequencedCollection<joliex.meta.spec.types.OutputPort> outputPorts;
        private joliex.meta.spec.types.LocatedString name;
        private java.util.SequencedCollection<joliex.meta.spec.types.InputPort> inputPorts;
        
        private Builder() {}
        private Builder( jolie.runtime.embedding.java.JolieValue j ) {
            this.textLocation = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "textLocation" ), joliex.meta.spec.types.Location::from );
            this.documentation = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "documentation" ), joliex.meta.spec.types.Documentation::from );
            this.outputPorts = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getChildOrDefault( "outputPorts", java.util.List.of() ), joliex.meta.spec.types.OutputPort::from );
            this.name = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "name" ), joliex.meta.spec.types.LocatedString::from );
            this.inputPorts = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getChildOrDefault( "inputPorts", java.util.List.of() ), joliex.meta.spec.types.InputPort::from );
        }
        
        public Builder textLocation( joliex.meta.spec.types.Location textLocation ) { this.textLocation = textLocation; return this; }
        public Builder textLocation( java.util.function.Function<joliex.meta.spec.types.Location.Builder, joliex.meta.spec.types.Location> f ) { return textLocation( f.apply( joliex.meta.spec.types.Location.builder() ) ); }
        public Builder documentation( joliex.meta.spec.types.Documentation documentation ) { this.documentation = documentation; return this; }
        public Builder documentation( java.util.function.Function<joliex.meta.spec.types.Documentation.Builder, joliex.meta.spec.types.Documentation> f ) { return documentation( f.apply( joliex.meta.spec.types.Documentation.builder() ) ); }
        public Builder outputPorts( java.util.SequencedCollection<joliex.meta.spec.types.OutputPort> outputPorts ) { this.outputPorts = outputPorts; return this; }
        public Builder outputPorts( java.util.function.Function<jolie.runtime.embedding.java.util.StructureListBuilder<joliex.meta.spec.types.OutputPort, joliex.meta.spec.types.OutputPort.Builder>, java.util.List<joliex.meta.spec.types.OutputPort>> f ) { return outputPorts( f.apply( new jolie.runtime.embedding.java.util.StructureListBuilder<>( joliex.meta.spec.types.OutputPort::builder ) ) ); }
        public Builder name( joliex.meta.spec.types.LocatedString name ) { this.name = name; return this; }
        public Builder name( java.util.function.Function<joliex.meta.spec.types.LocatedString.Builder, joliex.meta.spec.types.LocatedString> f ) { return name( f.apply( joliex.meta.spec.types.LocatedString.builder() ) ); }
        public Builder inputPorts( java.util.SequencedCollection<joliex.meta.spec.types.InputPort> inputPorts ) { this.inputPorts = inputPorts; return this; }
        public Builder inputPorts( java.util.function.Function<jolie.runtime.embedding.java.util.StructureListBuilder<joliex.meta.spec.types.InputPort, joliex.meta.spec.types.InputPort.Builder>, java.util.List<joliex.meta.spec.types.InputPort>> f ) { return inputPorts( f.apply( new jolie.runtime.embedding.java.util.StructureListBuilder<>( joliex.meta.spec.types.InputPort::builder ) ) ); }
        
        public ServiceDef build() {
            return new ServiceDef( textLocation, documentation, outputPorts, name, inputPorts );
        }
    }
}