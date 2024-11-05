package joliex.meta.spec.types;

/**
 * This is a sealed interface representing the following choice type:
 * <pre>
 * StringRefinement: S1 | S2 | S3
 * </pre>
 * 
 * @see jolie.runtime.embedding.java.JolieValue
 * @see jolie.runtime.embedding.java.JolieNative
 * @see S1
 * @see S2
 * @see S3
 * @see #of1(S1)
 * @see #of2(S2)
 * @see #of3(S3)
 */
public sealed interface StringRefinement extends jolie.runtime.embedding.java.JolieValue {
    
    jolie.runtime.Value jolieRepr();
    
    public static record C1( S1 option ) implements StringRefinement {
        
        public C1{ jolie.runtime.embedding.java.util.ValueManager.validated( "option", option ); }
        
        public jolie.runtime.embedding.java.JolieNative.JolieVoid content() { return option.content(); }
        public java.util.Map<java.lang.String, java.util.List<jolie.runtime.embedding.java.JolieValue>> children() { return option.children(); }
        public jolie.runtime.Value jolieRepr() { return S1.toValue( option ); }
        
        public boolean equals( java.lang.Object obj ) { return obj != null && obj instanceof jolie.runtime.embedding.java.JolieValue j && option.equals( j ); }
        public int hashCode() { return option.hashCode(); }
        public java.lang.String toString() { return option.toString(); }
        
        public static C1 from( jolie.runtime.embedding.java.JolieValue j ) throws jolie.runtime.embedding.java.TypeValidationException { return new C1( S1.from( j ) ); }
        
        public static C1 fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException { return new C1( S1.fromValue( v ) ); }
        
        public static jolie.runtime.Value toValue( C1 t ) { return t.jolieRepr(); }
    }
    
    public static record C2( S2 option ) implements StringRefinement {
        
        public C2{ jolie.runtime.embedding.java.util.ValueManager.validated( "option", option ); }
        
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
    
    public static record C3( S3 option ) implements StringRefinement {
        
        public C3{ jolie.runtime.embedding.java.util.ValueManager.validated( "option", option ); }
        
        public jolie.runtime.embedding.java.JolieNative.JolieVoid content() { return option.content(); }
        public java.util.Map<java.lang.String, java.util.List<jolie.runtime.embedding.java.JolieValue>> children() { return option.children(); }
        public jolie.runtime.Value jolieRepr() { return S3.toValue( option ); }
        
        public boolean equals( java.lang.Object obj ) { return obj != null && obj instanceof jolie.runtime.embedding.java.JolieValue j && option.equals( j ); }
        public int hashCode() { return option.hashCode(); }
        public java.lang.String toString() { return option.toString(); }
        
        public static C3 from( jolie.runtime.embedding.java.JolieValue j ) throws jolie.runtime.embedding.java.TypeValidationException { return new C3( S3.from( j ) ); }
        
        public static C3 fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException { return new C3( S3.fromValue( v ) ); }
        
        public static jolie.runtime.Value toValue( C3 t ) { return t.jolieRepr(); }
    }
    
    public static ListBuilder listBuilder() { return new ListBuilder(); }
    public static ListBuilder listBuilder( java.util.SequencedCollection<jolie.runtime.embedding.java.JolieValue> from ) { return from != null ? new ListBuilder( from ) : listBuilder(); }
    
    public static StringRefinement of1( S1 option ) { return new C1( option ); }
    public static StringRefinement of1( java.util.function.Function<S1.Builder, S1> f ) { return of1( f.apply( S1.builder() ) ); }
    
    public static StringRefinement of2( S2 option ) { return new C2( option ); }
    public static StringRefinement of2( java.util.function.Function<S2.Builder, S2> f ) { return of2( f.apply( S2.builder() ) ); }
    
    public static StringRefinement of3( S3 option ) { return new C3( option ); }
    public static StringRefinement of3( java.util.function.Function<S3.Builder, S3> f ) { return of3( f.apply( S3.builder() ) ); }
    
    public static StringRefinement from( jolie.runtime.embedding.java.JolieValue j ) throws jolie.runtime.embedding.java.TypeValidationException {
        return jolie.runtime.embedding.java.util.ValueManager.choiceFrom( j, java.util.List.of( jolie.runtime.embedding.java.util.ValueManager.castFunc( C1::from ), jolie.runtime.embedding.java.util.ValueManager.castFunc( C2::from ), jolie.runtime.embedding.java.util.ValueManager.castFunc( C3::from ) ) );
    }
    
    public static StringRefinement fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
        return jolie.runtime.embedding.java.util.ValueManager.choiceFrom( v, java.util.List.of( jolie.runtime.embedding.java.util.ValueManager.castFunc( C1::fromValue ), jolie.runtime.embedding.java.util.ValueManager.castFunc( C2::fromValue ), jolie.runtime.embedding.java.util.ValueManager.castFunc( C3::fromValue ) ) );
    }
    
    public static jolie.runtime.Value toValue( StringRefinement t ) { return t.jolieRepr(); }
    
    
    /**
     * this class is a {@link jolie.runtime.embedding.java.TypedStructure} which can be described as follows:
     * <pre>
     * length: {@link joliex.meta.spec.types.IntRange}
     * </pre>
     * 
     * @see jolie.runtime.embedding.java.JolieValue
     * @see jolie.runtime.embedding.java.JolieNative
     * @see joliex.meta.spec.types.IntRange
     * @see #builder()
     */
    public static final class S1 extends jolie.runtime.embedding.java.TypedStructure {
        
        private static final java.util.Set<java.lang.String> FIELD_KEYS = fieldKeys( S1.class );
        
        @jolie.runtime.embedding.java.util.JolieName("length")
        private final joliex.meta.spec.types.IntRange length;
        
        public S1( joliex.meta.spec.types.IntRange length ) {
            this.length = jolie.runtime.embedding.java.util.ValueManager.validated( "length", length );
        }
        
        public joliex.meta.spec.types.IntRange length() { return length; }
        
        public jolie.runtime.embedding.java.JolieNative.JolieVoid content() { return new jolie.runtime.embedding.java.JolieNative.JolieVoid(); }
        
        public static Builder builder() { return new Builder(); }
        public static Builder builder( jolie.runtime.embedding.java.JolieValue from ) { return from != null ? new Builder( from ) : builder(); }
        
        public static jolie.runtime.embedding.java.util.StructureListBuilder<S1, Builder> listBuilder() { return new jolie.runtime.embedding.java.util.StructureListBuilder<>( S1::builder ); }
        public static jolie.runtime.embedding.java.util.StructureListBuilder<S1, Builder> listBuilder( java.util.SequencedCollection<? extends jolie.runtime.embedding.java.JolieValue> from ) {
            return from != null ? new jolie.runtime.embedding.java.util.StructureListBuilder<>( from, S1::from, S1::builder ) : listBuilder();
        }
        
        public static S1 from( jolie.runtime.embedding.java.JolieValue j ) throws jolie.runtime.embedding.java.TypeValidationException {
            return new S1(
                jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "length" ), joliex.meta.spec.types.IntRange::from )
            );
        }
        
        public static S1 fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
            jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
            return new S1(
                jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "length", joliex.meta.spec.types.IntRange::fromValue )
            );
        }
        
        public static jolie.runtime.Value toValue( S1 t ) {
            final jolie.runtime.Value v = jolie.runtime.Value.create();
            
            v.getFirstChild( "length" ).deepCopy( joliex.meta.spec.types.IntRange.toValue( t.length() ) );
            
            return v;
        }
        
        public static class Builder {
            
            private joliex.meta.spec.types.IntRange length;
            
            private Builder() {}
            private Builder( jolie.runtime.embedding.java.JolieValue j ) {
                this.length = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "length" ), joliex.meta.spec.types.IntRange::from );
            }
            
            public Builder length( joliex.meta.spec.types.IntRange length ) { this.length = length; return this; }
            public Builder length( java.util.function.Function<joliex.meta.spec.types.IntRange.Builder, joliex.meta.spec.types.IntRange> f ) { return length( f.apply( joliex.meta.spec.types.IntRange.builder() ) ); }
            
            public S1 build() {
                return new S1( length );
            }
        }
    }
    
    
    /**
     * this class is a {@link jolie.runtime.embedding.java.TypedStructure} which can be described as follows:
     * <pre>
     * _enum("enum")[1,2147483647]: {@link java.lang.String}
     * </pre>
     * 
     * @see jolie.runtime.embedding.java.JolieValue
     * @see jolie.runtime.embedding.java.JolieNative
     * @see #builder()
     */
    public static final class S2 extends jolie.runtime.embedding.java.TypedStructure {
        
        private static final java.util.Set<java.lang.String> FIELD_KEYS = fieldKeys( S2.class );
        
        @jolie.runtime.embedding.java.util.JolieName("enum")
        private final java.util.List<java.lang.String> _enum;
        
        public S2( java.util.SequencedCollection<java.lang.String> _enum ) {
            this._enum = jolie.runtime.embedding.java.util.ValueManager.validated( "_enum", _enum, 1, 2147483647, t -> t );
        }
        
        public java.util.List<java.lang.String> _enum() { return _enum; }
        
        public jolie.runtime.embedding.java.JolieNative.JolieVoid content() { return new jolie.runtime.embedding.java.JolieNative.JolieVoid(); }
        
        public static Builder builder() { return new Builder(); }
        public static Builder builder( jolie.runtime.embedding.java.JolieValue from ) { return from != null ? new Builder( from ) : builder(); }
        
        public static jolie.runtime.embedding.java.util.StructureListBuilder<S2, Builder> listBuilder() { return new jolie.runtime.embedding.java.util.StructureListBuilder<>( S2::builder ); }
        public static jolie.runtime.embedding.java.util.StructureListBuilder<S2, Builder> listBuilder( java.util.SequencedCollection<? extends jolie.runtime.embedding.java.JolieValue> from ) {
            return from != null ? new jolie.runtime.embedding.java.util.StructureListBuilder<>( from, S2::from, S2::builder ) : listBuilder();
        }
        
        public static S2 from( jolie.runtime.embedding.java.JolieValue j ) throws jolie.runtime.embedding.java.TypeValidationException {
            return new S2(
                jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getChildOrDefault( "enum", java.util.List.of() ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieString content ? content.value() : null )
            );
        }
        
        public static S2 fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
            jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
            return new S2(
                jolie.runtime.embedding.java.util.ValueManager.vectorFieldFrom( v, "enum", jolie.runtime.embedding.java.JolieNative.JolieString::fieldFromValue )
            );
        }
        
        public static jolie.runtime.Value toValue( S2 t ) {
            final jolie.runtime.Value v = jolie.runtime.Value.create();
            
            t._enum().forEach( c -> v.getNewChild( "enum" ).setValue( c ) );
            
            return v;
        }
        
        public static class Builder {
            
            private java.util.SequencedCollection<java.lang.String> _enum;
            
            private Builder() {}
            private Builder( jolie.runtime.embedding.java.JolieValue j ) {
                this._enum = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getChildOrDefault( "enum", java.util.List.of() ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieString content ? content.value() : null );
            }
            
            public Builder _enum( java.util.SequencedCollection<java.lang.String> _enum ) { this._enum = _enum; return this; }
            public Builder _enum( java.lang.String... values ) { return _enum( values == null ? null : java.util.List.of( values ) ); }
            
            public S2 build() {
                return new S2( _enum );
            }
        }
    }
    
    
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
    public static final class S3 extends jolie.runtime.embedding.java.TypedStructure {
        
        private static final java.util.Set<java.lang.String> FIELD_KEYS = fieldKeys( S3.class );
        
        @jolie.runtime.embedding.java.util.JolieName("regex")
        private final java.lang.String regex;
        
        public S3( java.lang.String regex ) {
            this.regex = jolie.runtime.embedding.java.util.ValueManager.validated( "regex", regex );
        }
        
        public java.lang.String regex() { return regex; }
        
        public jolie.runtime.embedding.java.JolieNative.JolieVoid content() { return new jolie.runtime.embedding.java.JolieNative.JolieVoid(); }
        
        public static Builder builder() { return new Builder(); }
        public static Builder builder( jolie.runtime.embedding.java.JolieValue from ) { return from != null ? new Builder( from ) : builder(); }
        
        public static jolie.runtime.embedding.java.util.StructureListBuilder<S3, Builder> listBuilder() { return new jolie.runtime.embedding.java.util.StructureListBuilder<>( S3::builder ); }
        public static jolie.runtime.embedding.java.util.StructureListBuilder<S3, Builder> listBuilder( java.util.SequencedCollection<? extends jolie.runtime.embedding.java.JolieValue> from ) {
            return from != null ? new jolie.runtime.embedding.java.util.StructureListBuilder<>( from, S3::from, S3::builder ) : listBuilder();
        }
        
        public static S3 from( jolie.runtime.embedding.java.JolieValue j ) throws jolie.runtime.embedding.java.TypeValidationException {
            return new S3(
                jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "regex" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieString content ? content.value() : null )
            );
        }
        
        public static S3 fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
            jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
            return new S3(
                jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "regex", jolie.runtime.embedding.java.JolieNative.JolieString::fieldFromValue )
            );
        }
        
        public static jolie.runtime.Value toValue( S3 t ) {
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
            
            public S3 build() {
                return new S3( regex );
            }
        }
    }
    
    public static class ListBuilder extends jolie.runtime.embedding.java.util.AbstractListBuilder<ListBuilder, StringRefinement> {
        
        private ListBuilder() {}
        private ListBuilder( java.util.SequencedCollection<? extends jolie.runtime.embedding.java.JolieValue> c ) { super( c, StringRefinement::from ); }
        
        protected ListBuilder self() { return this; }
        
        public ListBuilder add1( S1 option ) { return add( new C1( option ) ); }
        public ListBuilder add1( int index, S1 option ) { return add( index, new C1( option ) ); }
        public ListBuilder set1( int index, S1 option ) { return set( index, new C1( option ) ); }
        
        public ListBuilder add1( java.util.function.Function<S1.Builder, S1> b ) { return add1( b.apply( S1.builder() ) ); }
        public ListBuilder add1( int index, java.util.function.Function<S1.Builder, S1> b ) { return add1( index, b.apply( S1.builder() ) ); }
        public ListBuilder set1( int index, java.util.function.Function<S1.Builder, S1> b ) { return set1( index, b.apply( S1.builder() ) ); }
        
        public ListBuilder add2( S2 option ) { return add( new C2( option ) ); }
        public ListBuilder add2( int index, S2 option ) { return add( index, new C2( option ) ); }
        public ListBuilder set2( int index, S2 option ) { return set( index, new C2( option ) ); }
        
        public ListBuilder add2( java.util.function.Function<S2.Builder, S2> b ) { return add2( b.apply( S2.builder() ) ); }
        public ListBuilder add2( int index, java.util.function.Function<S2.Builder, S2> b ) { return add2( index, b.apply( S2.builder() ) ); }
        public ListBuilder set2( int index, java.util.function.Function<S2.Builder, S2> b ) { return set2( index, b.apply( S2.builder() ) ); }
        
        public ListBuilder add3( S3 option ) { return add( new C3( option ) ); }
        public ListBuilder add3( int index, S3 option ) { return add( index, new C3( option ) ); }
        public ListBuilder set3( int index, S3 option ) { return set( index, new C3( option ) ); }
        
        public ListBuilder add3( java.util.function.Function<S3.Builder, S3> b ) { return add3( b.apply( S3.builder() ) ); }
        public ListBuilder add3( int index, java.util.function.Function<S3.Builder, S3> b ) { return add3( index, b.apply( S3.builder() ) ); }
        public ListBuilder set3( int index, java.util.function.Function<S3.Builder, S3> b ) { return set3( index, b.apply( S3.builder() ) ); }
        
        public java.util.List<StringRefinement> build() { return super.build(); }
    }
}