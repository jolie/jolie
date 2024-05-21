package joliex.util.spec.types;

/**
 * this class is a choice type which can be described as follows:
 * <pre>
 * FormatRequest: S1 | S2
 * </pre>
 * 
 * @see jolie.runtime.embedding.java.JolieValue
 * @see jolie.runtime.embedding.java.JolieNative
 * @see S1
 * @see S2
 * @see #of1(S1)
 * @see #of2(S2)
 */
public sealed interface FormatRequest extends jolie.runtime.embedding.java.JolieValue {
    
    jolie.runtime.Value jolieRepr();
    
    public static record C1( S1 option ) implements FormatRequest {
        
        public C1( S1 option ) {
            this.option = jolie.runtime.embedding.java.util.ValueManager.validated( "option", option );
        }
        
        public jolie.runtime.embedding.java.JolieNative.JolieString content() { return option.content(); }
        public java.util.Map<java.lang.String, java.util.List<jolie.runtime.embedding.java.JolieValue>> children() { return option.children(); }
        public jolie.runtime.Value jolieRepr() { return S1.toValue( option ); }
        
        public boolean equals( java.lang.Object obj ) { return obj != null && obj instanceof jolie.runtime.embedding.java.JolieValue j && option.equals( j ); }
        public int hashCode() { return option.hashCode(); }
        public java.lang.String toString() { return option.toString(); }
        
        public static C1 from( jolie.runtime.embedding.java.JolieValue j ) throws jolie.runtime.embedding.java.TypeValidationException { return new C1( S1.from( j ) ); }
        
        public static C1 fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException { return new C1( S1.fromValue( v ) ); }
        
        public static jolie.runtime.Value toValue( C1 t ) { return t.jolieRepr(); }
    }
    
    public static record C2( S2 option ) implements FormatRequest {
        
        public C2( S2 option ) {
            this.option = jolie.runtime.embedding.java.util.ValueManager.validated( "option", option );
        }
        
        public jolie.runtime.embedding.java.JolieNative.JolieVoid content() { return option.content(); }
        public java.util.Map<java.lang.String, java.util.List<jolie.runtime.embedding.java.JolieValue>> children() { return option.children(); }
        public jolie.runtime.Value jolieRepr() { return S2.toValue( option ); }
        
        public boolean equals( java.lang.Object obj ) { return obj != null && obj instanceof jolie.runtime.embedding.java.JolieValue j && option.equals( j ); }
        public int hashCode() { return option.hashCode(); }
        public java.lang.String toString() { return option.toString(); }
        
        public static C2 from( jolie.runtime.embedding.java.JolieValue j ) throws jolie.runtime.embedding.java.TypeValidationException { return new C2( S2.from( j ) ); }
        
        public static C2 fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException { return new C2( S2.fromValue( v ) ); }
        
        public static jolie.runtime.Value toValue( C2 t ) { return t.jolieRepr(); }
    }
    
    public static FormatRequest of1( S1 option ) { return new C1( option ); }
    public static FormatRequest of1( java.util.function.Function<S1.Builder, S1> f ) { return of1( f.apply( S1.builder() ) ); }
    
    public static FormatRequest of2( S2 option ) { return new C2( option ); }
    public static FormatRequest of2( java.util.function.Function<S2.Builder, S2> f ) { return of2( f.apply( S2.builder() ) ); }
    
    public static FormatRequest from( jolie.runtime.embedding.java.JolieValue j ) throws jolie.runtime.embedding.java.TypeValidationException {
        return jolie.runtime.embedding.java.util.ValueManager.choiceFrom( j, java.util.List.of( jolie.runtime.embedding.java.util.ValueManager.castFunc( C1::from ), jolie.runtime.embedding.java.util.ValueManager.castFunc( C2::from ) ) );
    }
    
    public static FormatRequest fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
        return jolie.runtime.embedding.java.util.ValueManager.choiceFrom( v, java.util.List.of( jolie.runtime.embedding.java.util.ValueManager.castFunc( C1::fromValue ), jolie.runtime.embedding.java.util.ValueManager.castFunc( C2::fromValue ) ) );
    }
    
    public static jolie.runtime.Value toValue( FormatRequest t ) { return t.jolieRepr(); }
    
    
    /**
     * this class is an {@link jolie.runtime.embedding.java.UntypedStructure} which can be described as follows:
     * <pre>
     * 
     * contentValue: {@link java.lang.String}{ ? }
     * </pre>
     * 
     * @see jolie.runtime.embedding.java.JolieValue
     * @see jolie.runtime.embedding.java.JolieNative
     * @see #builder()
     */
    public static final class S1 extends jolie.runtime.embedding.java.UntypedStructure<jolie.runtime.embedding.java.JolieNative.JolieString> {
        
        public S1( java.lang.String contentValue, java.util.Map<java.lang.String, java.util.List<jolie.runtime.embedding.java.JolieValue>> children ) { super( jolie.runtime.embedding.java.JolieNative.of( contentValue ), children ); }
        
        public java.lang.String contentValue() { return content().value(); }
        
        public static Builder builder() { return new Builder(); }
        public static Builder builder( java.lang.String contentValue ) { return builder().contentValue( contentValue ); }
        public static Builder builder( jolie.runtime.embedding.java.JolieValue from ) { return new Builder( from ); }
        
        public static jolie.runtime.embedding.java.util.StructureListBuilder<S1, Builder> listBuilder() { return new jolie.runtime.embedding.java.util.StructureListBuilder<>( S1::builder, S1::builder ); }
        public static jolie.runtime.embedding.java.util.StructureListBuilder<S1, Builder> listBuilder( java.util.SequencedCollection<? extends jolie.runtime.embedding.java.JolieValue> from ) {
            return new jolie.runtime.embedding.java.util.StructureListBuilder<>( from, S1::from, S1::builder, S1::builder );
        }
        
        public static S1 from( jolie.runtime.embedding.java.JolieValue j ) throws jolie.runtime.embedding.java.TypeValidationException {
            return new S1( jolie.runtime.embedding.java.JolieNative.JolieString.from( j ).value(), j.children() );
        }
        
        public static S1 fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException { return from( jolie.runtime.embedding.java.JolieValue.fromValue( v ) ); }
        
        public static jolie.runtime.Value toValue( S1 t ) { return jolie.runtime.embedding.java.JolieValue.toValue( t ); }
        
        public static class Builder extends jolie.runtime.embedding.java.util.UntypedBuilder<Builder> {
            
            private java.lang.String contentValue;
            
            private Builder() {}
            
            private Builder( jolie.runtime.embedding.java.JolieValue j ) {
                super( j.children() );
                
                contentValue = j.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieString content ? content.value() : null;
            }
            
            protected Builder self() { return this; }
            
            public Builder contentValue( java.lang.String contentValue ) { this.contentValue = contentValue; return this; }
            
            public S1 build() { return new S1( contentValue, children ); }
        }
    }
    
    
    /**
     * this class is a {@link jolie.runtime.embedding.java.TypedStructure} which can be described as follows:
     * <pre>
     * data: {@link Data}
     * format: {@link java.lang.String}
     * locale: {@link java.lang.String}
     * </pre>
     * 
     * @see jolie.runtime.embedding.java.JolieValue
     * @see jolie.runtime.embedding.java.JolieNative
     * @see Data
     * @see #builder()
     */
    public static final class S2 extends jolie.runtime.embedding.java.TypedStructure {
        
        private static final java.util.Set<java.lang.String> FIELD_KEYS = fieldKeys( S2.class );
        
        @jolie.runtime.embedding.java.util.JolieName("data")
        private final Data data;
        @jolie.runtime.embedding.java.util.JolieName("format")
        private final java.lang.String format;
        @jolie.runtime.embedding.java.util.JolieName("locale")
        private final java.lang.String locale;
        
        public S2( Data data, java.lang.String format, java.lang.String locale ) {
            this.data = jolie.runtime.embedding.java.util.ValueManager.validated( "data", data );
            this.format = jolie.runtime.embedding.java.util.ValueManager.validated( "format", format );
            this.locale = jolie.runtime.embedding.java.util.ValueManager.validated( "locale", locale );
        }
        
        public Data data() { return data; }
        public java.lang.String format() { return format; }
        public java.lang.String locale() { return locale; }
        
        public jolie.runtime.embedding.java.JolieNative.JolieVoid content() { return new jolie.runtime.embedding.java.JolieNative.JolieVoid(); }
        
        public static Builder builder() { return new Builder(); }
        public static Builder builder( jolie.runtime.embedding.java.JolieValue from ) { return new Builder( from ); }
        
        public static jolie.runtime.embedding.java.util.StructureListBuilder<S2, Builder> listBuilder() { return new jolie.runtime.embedding.java.util.StructureListBuilder<>( S2::builder, S2::builder ); }
        public static jolie.runtime.embedding.java.util.StructureListBuilder<S2, Builder> listBuilder( java.util.SequencedCollection<? extends jolie.runtime.embedding.java.JolieValue> from ) {
            return new jolie.runtime.embedding.java.util.StructureListBuilder<>( from, S2::from, S2::builder, S2::builder );
        }
        
        public static S2 from( jolie.runtime.embedding.java.JolieValue j ) throws jolie.runtime.embedding.java.TypeValidationException {
            return new S2(
                jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "data" ), Data::from ),
                jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "format" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieString content ? content.value() : null ),
                jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "locale" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieString content ? content.value() : null )
            );
        }
        
        public static S2 fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
            jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
            return new S2(
                jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "data", Data::fromValue ),
                jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "format", jolie.runtime.embedding.java.JolieNative.JolieString::fieldFromValue ),
                jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "locale", jolie.runtime.embedding.java.JolieNative.JolieString::fieldFromValue )
            );
        }
        
        public static jolie.runtime.Value toValue( S2 t ) {
            final jolie.runtime.Value v = jolie.runtime.Value.create();
            
            v.getFirstChild( "data" ).deepCopy( Data.toValue( t.data() ) );
            v.getFirstChild( "format" ).setValue( t.format() );
            v.getFirstChild( "locale" ).setValue( t.locale() );
            
            return v;
        }
        
        public static class Builder {
            
            private Data data;
            private java.lang.String format;
            private java.lang.String locale;
            
            private Builder() {}
            private Builder( jolie.runtime.embedding.java.JolieValue j ) {
                this.data = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "data" ), Data::from );
                this.format = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "format" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieString content ? content.value() : null );
                this.locale = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "locale" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieString content ? content.value() : null );
            }
            
            public Builder data( Data data ) { this.data = data; return this; }
            public Builder data( java.util.function.Function<Data.Builder, Data> f ) { return data( f.apply( Data.builder() ) ); }
            public Builder format( java.lang.String format ) { this.format = format; return this; }
            public Builder locale( java.lang.String locale ) { this.locale = locale; return this; }
            
            public S2 build() {
                return new S2( data, format, locale );
            }
        }
        
        
        /**
         * this class is an {@link jolie.runtime.embedding.java.UntypedStructure} which can be described as follows:
         * <pre>
         * 
         * void { ? }
         * </pre>
         * 
         * @see jolie.runtime.embedding.java.JolieValue
         * @see jolie.runtime.embedding.java.JolieNative
         * @see #builder()
         */
        public static final class Data extends jolie.runtime.embedding.java.UntypedStructure<jolie.runtime.embedding.java.JolieNative.JolieVoid> {
            
            public Data( java.util.Map<java.lang.String, java.util.List<jolie.runtime.embedding.java.JolieValue>> children ) { super( new jolie.runtime.embedding.java.JolieNative.JolieVoid(), children ); }
            
            public static Builder builder() { return new Builder(); }
            public static Builder builder( jolie.runtime.embedding.java.JolieValue from ) { return new Builder( from ); }
            
            public static jolie.runtime.embedding.java.util.StructureListBuilder<Data, Builder> listBuilder() { return new jolie.runtime.embedding.java.util.StructureListBuilder<>( Data::builder, Data::builder ); }
            public static jolie.runtime.embedding.java.util.StructureListBuilder<Data, Builder> listBuilder( java.util.SequencedCollection<? extends jolie.runtime.embedding.java.JolieValue> from ) {
                return new jolie.runtime.embedding.java.util.StructureListBuilder<>( from, Data::from, Data::builder, Data::builder );
            }
            
            public static Data from( jolie.runtime.embedding.java.JolieValue j ) throws jolie.runtime.embedding.java.TypeValidationException {
                return new Data( j.children() );
            }
            
            public static Data fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException { return from( jolie.runtime.embedding.java.JolieValue.fromValue( v ) ); }
            
            public static jolie.runtime.Value toValue( Data t ) { return jolie.runtime.embedding.java.JolieValue.toValue( t ); }
            
            public static class Builder extends jolie.runtime.embedding.java.util.UntypedBuilder<Builder> {
                
                private Builder() {}
                
                private Builder( jolie.runtime.embedding.java.JolieValue j ) {
                    super( j.children() );
                }
                
                protected Builder self() { return this; }
                
                public Data build() { return new Data( children ); }
            }
        }
    }
}