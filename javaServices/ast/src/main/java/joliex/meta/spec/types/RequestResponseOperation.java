package joliex.meta.spec.types;

/**
 * this class is a {@link jolie.runtime.embedding.java.TypedStructure} which can be described as follows:
 * <pre>
 * textLocation: {@link joliex.meta.spec.types.Location}
 * responseType: {@link joliex.meta.spec.types.Type}
 * requestType: {@link joliex.meta.spec.types.Type}
 * name: {@link joliex.meta.spec.types.LocatedString}
 * faults[0,2147483647]: {@link joliex.meta.spec.types.FaultType}
 * </pre>
 * 
 * @see jolie.runtime.embedding.java.JolieValue
 * @see jolie.runtime.embedding.java.JolieNative
 * @see joliex.meta.spec.types.Location
 * @see joliex.meta.spec.types.Type
 * @see joliex.meta.spec.types.LocatedString
 * @see joliex.meta.spec.types.FaultType
 * @see #builder()
 */
public final class RequestResponseOperation extends jolie.runtime.embedding.java.TypedStructure {
    
    private static final java.util.Set<java.lang.String> FIELD_KEYS = fieldKeys( RequestResponseOperation.class );
    
    @jolie.runtime.embedding.java.util.JolieName("textLocation")
    private final joliex.meta.spec.types.Location textLocation;
    @jolie.runtime.embedding.java.util.JolieName("responseType")
    private final joliex.meta.spec.types.Type responseType;
    @jolie.runtime.embedding.java.util.JolieName("requestType")
    private final joliex.meta.spec.types.Type requestType;
    @jolie.runtime.embedding.java.util.JolieName("name")
    private final joliex.meta.spec.types.LocatedString name;
    @jolie.runtime.embedding.java.util.JolieName("faults")
    private final java.util.List<joliex.meta.spec.types.FaultType> faults;
    
    public RequestResponseOperation( joliex.meta.spec.types.Location textLocation, joliex.meta.spec.types.Type responseType, joliex.meta.spec.types.Type requestType, joliex.meta.spec.types.LocatedString name, java.util.SequencedCollection<joliex.meta.spec.types.FaultType> faults ) {
        this.textLocation = jolie.runtime.embedding.java.util.ValueManager.validated( "textLocation", textLocation );
        this.responseType = jolie.runtime.embedding.java.util.ValueManager.validated( "responseType", responseType );
        this.requestType = jolie.runtime.embedding.java.util.ValueManager.validated( "requestType", requestType );
        this.name = jolie.runtime.embedding.java.util.ValueManager.validated( "name", name );
        this.faults = jolie.runtime.embedding.java.util.ValueManager.validated( "faults", faults, 0, 2147483647, t -> t );
    }
    
    public joliex.meta.spec.types.Location textLocation() { return textLocation; }
    public joliex.meta.spec.types.Type responseType() { return responseType; }
    public joliex.meta.spec.types.Type requestType() { return requestType; }
    public joliex.meta.spec.types.LocatedString name() { return name; }
    public java.util.List<joliex.meta.spec.types.FaultType> faults() { return faults; }
    
    public jolie.runtime.embedding.java.JolieNative.JolieVoid content() { return new jolie.runtime.embedding.java.JolieNative.JolieVoid(); }
    
    public static Builder builder() { return new Builder(); }
    public static Builder builder( jolie.runtime.embedding.java.JolieValue from ) { return from != null ? new Builder( from ) : builder(); }
    
    public static jolie.runtime.embedding.java.util.StructureListBuilder<RequestResponseOperation, Builder> listBuilder() { return new jolie.runtime.embedding.java.util.StructureListBuilder<>( RequestResponseOperation::builder ); }
    public static jolie.runtime.embedding.java.util.StructureListBuilder<RequestResponseOperation, Builder> listBuilder( java.util.SequencedCollection<? extends jolie.runtime.embedding.java.JolieValue> from ) {
        return from != null ? new jolie.runtime.embedding.java.util.StructureListBuilder<>( from, RequestResponseOperation::from, RequestResponseOperation::builder ) : listBuilder();
    }
    
    public static RequestResponseOperation from( jolie.runtime.embedding.java.JolieValue j ) throws jolie.runtime.embedding.java.TypeValidationException {
        return new RequestResponseOperation(
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "textLocation" ), joliex.meta.spec.types.Location::from ),
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "responseType" ), joliex.meta.spec.types.Type::from ),
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "requestType" ), joliex.meta.spec.types.Type::from ),
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "name" ), joliex.meta.spec.types.LocatedString::from ),
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getChildOrDefault( "faults", java.util.List.of() ), joliex.meta.spec.types.FaultType::from )
        );
    }
    
    public static RequestResponseOperation fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
        jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
        return new RequestResponseOperation(
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "textLocation", joliex.meta.spec.types.Location::fromValue ),
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "responseType", joliex.meta.spec.types.Type::fromValue ),
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "requestType", joliex.meta.spec.types.Type::fromValue ),
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "name", joliex.meta.spec.types.LocatedString::fromValue ),
            jolie.runtime.embedding.java.util.ValueManager.vectorFieldFrom( v, "faults", joliex.meta.spec.types.FaultType::fromValue )
        );
    }
    
    public static jolie.runtime.Value toValue( RequestResponseOperation t ) {
        final jolie.runtime.Value v = jolie.runtime.Value.create();
        
        v.getFirstChild( "textLocation" ).deepCopy( joliex.meta.spec.types.Location.toValue( t.textLocation() ) );
        v.getFirstChild( "responseType" ).deepCopy( joliex.meta.spec.types.Type.toValue( t.responseType() ) );
        v.getFirstChild( "requestType" ).deepCopy( joliex.meta.spec.types.Type.toValue( t.requestType() ) );
        v.getFirstChild( "name" ).deepCopy( joliex.meta.spec.types.LocatedString.toValue( t.name() ) );
        t.faults().forEach( c -> v.getNewChild( "faults" ).deepCopy( joliex.meta.spec.types.FaultType.toValue( c ) ) );
        
        return v;
    }
    
    public static class Builder {
        
        private joliex.meta.spec.types.Location textLocation;
        private joliex.meta.spec.types.Type responseType;
        private joliex.meta.spec.types.Type requestType;
        private joliex.meta.spec.types.LocatedString name;
        private java.util.SequencedCollection<joliex.meta.spec.types.FaultType> faults;
        
        private Builder() {}
        private Builder( jolie.runtime.embedding.java.JolieValue j ) {
            this.textLocation = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "textLocation" ), joliex.meta.spec.types.Location::from );
            this.responseType = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "responseType" ), joliex.meta.spec.types.Type::from );
            this.requestType = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "requestType" ), joliex.meta.spec.types.Type::from );
            this.name = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "name" ), joliex.meta.spec.types.LocatedString::from );
            this.faults = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getChildOrDefault( "faults", java.util.List.of() ), joliex.meta.spec.types.FaultType::from );
        }
        
        public Builder textLocation( joliex.meta.spec.types.Location textLocation ) { this.textLocation = textLocation; return this; }
        public Builder textLocation( java.util.function.Function<joliex.meta.spec.types.Location.Builder, joliex.meta.spec.types.Location> f ) { return textLocation( f.apply( joliex.meta.spec.types.Location.builder() ) ); }
        public Builder responseType( joliex.meta.spec.types.Type responseType ) { this.responseType = responseType; return this; }
        public Builder requestType( joliex.meta.spec.types.Type requestType ) { this.requestType = requestType; return this; }
        public Builder name( joliex.meta.spec.types.LocatedString name ) { this.name = name; return this; }
        public Builder name( java.util.function.Function<joliex.meta.spec.types.LocatedString.Builder, joliex.meta.spec.types.LocatedString> f ) { return name( f.apply( joliex.meta.spec.types.LocatedString.builder() ) ); }
        public Builder faults( java.util.SequencedCollection<joliex.meta.spec.types.FaultType> faults ) { this.faults = faults; return this; }
        public Builder faults( java.util.function.Function<jolie.runtime.embedding.java.util.StructureListBuilder<joliex.meta.spec.types.FaultType, joliex.meta.spec.types.FaultType.Builder>, java.util.List<joliex.meta.spec.types.FaultType>> f ) { return faults( f.apply( new jolie.runtime.embedding.java.util.StructureListBuilder<>( joliex.meta.spec.types.FaultType::builder ) ) ); }
        
        public RequestResponseOperation build() {
            return new RequestResponseOperation( textLocation, responseType, requestType, name, faults );
        }
    }
}