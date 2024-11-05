package joliex.meta.spec.types;

/**
 * This is a sealed interface representing the following choice type:
 * <pre>
 * BasicType: joliex.meta.spec.types.VoidBasicType | joliex.meta.spec.types.BoolBasicType | joliex.meta.spec.types.IntBasicType | joliex.meta.spec.types.LongBasicType | joliex.meta.spec.types.DoubleBasicType | joliex.meta.spec.types.StringBasicType
 * </pre>
 * 
 * @see jolie.runtime.embedding.java.JolieValue
 * @see jolie.runtime.embedding.java.JolieNative
 * @see joliex.meta.spec.types.VoidBasicType
 * @see joliex.meta.spec.types.BoolBasicType
 * @see joliex.meta.spec.types.IntBasicType
 * @see joliex.meta.spec.types.LongBasicType
 * @see joliex.meta.spec.types.DoubleBasicType
 * @see joliex.meta.spec.types.StringBasicType
 * @see #of1(joliex.meta.spec.types.VoidBasicType)
 * @see #of2(joliex.meta.spec.types.BoolBasicType)
 * @see #of3(joliex.meta.spec.types.IntBasicType)
 * @see #of4(joliex.meta.spec.types.LongBasicType)
 * @see #of5(joliex.meta.spec.types.DoubleBasicType)
 * @see #of6(joliex.meta.spec.types.StringBasicType)
 */
public sealed interface BasicType extends jolie.runtime.embedding.java.JolieValue {
    
    jolie.runtime.Value jolieRepr();
    
    public static record C1( joliex.meta.spec.types.VoidBasicType option ) implements BasicType {
        
        public C1{ jolie.runtime.embedding.java.util.ValueManager.validated( "option", option ); }
        
        public jolie.runtime.embedding.java.JolieNative.JolieVoid content() { return option.content(); }
        public java.util.Map<java.lang.String, java.util.List<jolie.runtime.embedding.java.JolieValue>> children() { return option.children(); }
        public jolie.runtime.Value jolieRepr() { return joliex.meta.spec.types.VoidBasicType.toValue( option ); }
        
        public boolean equals( java.lang.Object obj ) { return obj != null && obj instanceof jolie.runtime.embedding.java.JolieValue j && option.equals( j ); }
        public int hashCode() { return option.hashCode(); }
        public java.lang.String toString() { return option.toString(); }
        
        public static C1 from( jolie.runtime.embedding.java.JolieValue j ) throws jolie.runtime.embedding.java.TypeValidationException { return new C1( joliex.meta.spec.types.VoidBasicType.from( j ) ); }
        
        public static C1 fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException { return new C1( joliex.meta.spec.types.VoidBasicType.fromValue( v ) ); }
        
        public static jolie.runtime.Value toValue( C1 t ) { return t.jolieRepr(); }
    }
    
    public static record C2( joliex.meta.spec.types.BoolBasicType option ) implements BasicType {
        
        public C2{ jolie.runtime.embedding.java.util.ValueManager.validated( "option", option ); }
        
        public jolie.runtime.embedding.java.JolieNative.JolieVoid content() { return option.content(); }
        public java.util.Map<java.lang.String, java.util.List<jolie.runtime.embedding.java.JolieValue>> children() { return option.children(); }
        public jolie.runtime.Value jolieRepr() { return joliex.meta.spec.types.BoolBasicType.toValue( option ); }
        
        public boolean equals( java.lang.Object obj ) { return obj != null && obj instanceof jolie.runtime.embedding.java.JolieValue j && option.equals( j ); }
        public int hashCode() { return option.hashCode(); }
        public java.lang.String toString() { return option.toString(); }
        
        public static C2 from( jolie.runtime.embedding.java.JolieValue j ) throws jolie.runtime.embedding.java.TypeValidationException { return new C2( joliex.meta.spec.types.BoolBasicType.from( j ) ); }
        
        public static C2 fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException { return new C2( joliex.meta.spec.types.BoolBasicType.fromValue( v ) ); }
        
        public static jolie.runtime.Value toValue( C2 t ) { return t.jolieRepr(); }
    }
    
    public static record C3( joliex.meta.spec.types.IntBasicType option ) implements BasicType {
        
        public C3{ jolie.runtime.embedding.java.util.ValueManager.validated( "option", option ); }
        
        public jolie.runtime.embedding.java.JolieNative.JolieVoid content() { return option.content(); }
        public java.util.Map<java.lang.String, java.util.List<jolie.runtime.embedding.java.JolieValue>> children() { return option.children(); }
        public jolie.runtime.Value jolieRepr() { return joliex.meta.spec.types.IntBasicType.toValue( option ); }
        
        public boolean equals( java.lang.Object obj ) { return obj != null && obj instanceof jolie.runtime.embedding.java.JolieValue j && option.equals( j ); }
        public int hashCode() { return option.hashCode(); }
        public java.lang.String toString() { return option.toString(); }
        
        public static C3 from( jolie.runtime.embedding.java.JolieValue j ) throws jolie.runtime.embedding.java.TypeValidationException { return new C3( joliex.meta.spec.types.IntBasicType.from( j ) ); }
        
        public static C3 fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException { return new C3( joliex.meta.spec.types.IntBasicType.fromValue( v ) ); }
        
        public static jolie.runtime.Value toValue( C3 t ) { return t.jolieRepr(); }
    }
    
    public static record C4( joliex.meta.spec.types.LongBasicType option ) implements BasicType {
        
        public C4{ jolie.runtime.embedding.java.util.ValueManager.validated( "option", option ); }
        
        public jolie.runtime.embedding.java.JolieNative.JolieVoid content() { return option.content(); }
        public java.util.Map<java.lang.String, java.util.List<jolie.runtime.embedding.java.JolieValue>> children() { return option.children(); }
        public jolie.runtime.Value jolieRepr() { return joliex.meta.spec.types.LongBasicType.toValue( option ); }
        
        public boolean equals( java.lang.Object obj ) { return obj != null && obj instanceof jolie.runtime.embedding.java.JolieValue j && option.equals( j ); }
        public int hashCode() { return option.hashCode(); }
        public java.lang.String toString() { return option.toString(); }
        
        public static C4 from( jolie.runtime.embedding.java.JolieValue j ) throws jolie.runtime.embedding.java.TypeValidationException { return new C4( joliex.meta.spec.types.LongBasicType.from( j ) ); }
        
        public static C4 fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException { return new C4( joliex.meta.spec.types.LongBasicType.fromValue( v ) ); }
        
        public static jolie.runtime.Value toValue( C4 t ) { return t.jolieRepr(); }
    }
    
    public static record C5( joliex.meta.spec.types.DoubleBasicType option ) implements BasicType {
        
        public C5{ jolie.runtime.embedding.java.util.ValueManager.validated( "option", option ); }
        
        public jolie.runtime.embedding.java.JolieNative.JolieVoid content() { return option.content(); }
        public java.util.Map<java.lang.String, java.util.List<jolie.runtime.embedding.java.JolieValue>> children() { return option.children(); }
        public jolie.runtime.Value jolieRepr() { return joliex.meta.spec.types.DoubleBasicType.toValue( option ); }
        
        public boolean equals( java.lang.Object obj ) { return obj != null && obj instanceof jolie.runtime.embedding.java.JolieValue j && option.equals( j ); }
        public int hashCode() { return option.hashCode(); }
        public java.lang.String toString() { return option.toString(); }
        
        public static C5 from( jolie.runtime.embedding.java.JolieValue j ) throws jolie.runtime.embedding.java.TypeValidationException { return new C5( joliex.meta.spec.types.DoubleBasicType.from( j ) ); }
        
        public static C5 fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException { return new C5( joliex.meta.spec.types.DoubleBasicType.fromValue( v ) ); }
        
        public static jolie.runtime.Value toValue( C5 t ) { return t.jolieRepr(); }
    }
    
    public static record C6( joliex.meta.spec.types.StringBasicType option ) implements BasicType {
        
        public C6{ jolie.runtime.embedding.java.util.ValueManager.validated( "option", option ); }
        
        public jolie.runtime.embedding.java.JolieNative.JolieVoid content() { return option.content(); }
        public java.util.Map<java.lang.String, java.util.List<jolie.runtime.embedding.java.JolieValue>> children() { return option.children(); }
        public jolie.runtime.Value jolieRepr() { return joliex.meta.spec.types.StringBasicType.toValue( option ); }
        
        public boolean equals( java.lang.Object obj ) { return obj != null && obj instanceof jolie.runtime.embedding.java.JolieValue j && option.equals( j ); }
        public int hashCode() { return option.hashCode(); }
        public java.lang.String toString() { return option.toString(); }
        
        public static C6 from( jolie.runtime.embedding.java.JolieValue j ) throws jolie.runtime.embedding.java.TypeValidationException { return new C6( joliex.meta.spec.types.StringBasicType.from( j ) ); }
        
        public static C6 fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException { return new C6( joliex.meta.spec.types.StringBasicType.fromValue( v ) ); }
        
        public static jolie.runtime.Value toValue( C6 t ) { return t.jolieRepr(); }
    }
    
    public static ListBuilder listBuilder() { return new ListBuilder(); }
    public static ListBuilder listBuilder( java.util.SequencedCollection<jolie.runtime.embedding.java.JolieValue> from ) { return from != null ? new ListBuilder( from ) : listBuilder(); }
    
    public static BasicType of1( joliex.meta.spec.types.VoidBasicType option ) { return new C1( option ); }
    public static BasicType of1( java.util.function.Function<joliex.meta.spec.types.VoidBasicType.Builder, joliex.meta.spec.types.VoidBasicType> f ) { return of1( f.apply( joliex.meta.spec.types.VoidBasicType.builder() ) ); }
    
    public static BasicType of2( joliex.meta.spec.types.BoolBasicType option ) { return new C2( option ); }
    public static BasicType of2( java.util.function.Function<joliex.meta.spec.types.BoolBasicType.Builder, joliex.meta.spec.types.BoolBasicType> f ) { return of2( f.apply( joliex.meta.spec.types.BoolBasicType.builder() ) ); }
    
    public static BasicType of3( joliex.meta.spec.types.IntBasicType option ) { return new C3( option ); }
    public static BasicType of3( java.util.function.Function<joliex.meta.spec.types.IntBasicType.Builder, joliex.meta.spec.types.IntBasicType> f ) { return of3( f.apply( joliex.meta.spec.types.IntBasicType.builder() ) ); }
    
    public static BasicType of4( joliex.meta.spec.types.LongBasicType option ) { return new C4( option ); }
    public static BasicType of4( java.util.function.Function<joliex.meta.spec.types.LongBasicType.Builder, joliex.meta.spec.types.LongBasicType> f ) { return of4( f.apply( joliex.meta.spec.types.LongBasicType.builder() ) ); }
    
    public static BasicType of5( joliex.meta.spec.types.DoubleBasicType option ) { return new C5( option ); }
    public static BasicType of5( java.util.function.Function<joliex.meta.spec.types.DoubleBasicType.Builder, joliex.meta.spec.types.DoubleBasicType> f ) { return of5( f.apply( joliex.meta.spec.types.DoubleBasicType.builder() ) ); }
    
    public static BasicType of6( joliex.meta.spec.types.StringBasicType option ) { return new C6( option ); }
    public static BasicType of6( java.util.function.Function<joliex.meta.spec.types.StringBasicType.Builder, joliex.meta.spec.types.StringBasicType> f ) { return of6( f.apply( joliex.meta.spec.types.StringBasicType.builder() ) ); }
    
    public static BasicType from( jolie.runtime.embedding.java.JolieValue j ) throws jolie.runtime.embedding.java.TypeValidationException {
        return jolie.runtime.embedding.java.util.ValueManager.choiceFrom( j, java.util.List.of( jolie.runtime.embedding.java.util.ValueManager.castFunc( C1::from ), jolie.runtime.embedding.java.util.ValueManager.castFunc( C2::from ), jolie.runtime.embedding.java.util.ValueManager.castFunc( C3::from ), jolie.runtime.embedding.java.util.ValueManager.castFunc( C4::from ), jolie.runtime.embedding.java.util.ValueManager.castFunc( C5::from ), jolie.runtime.embedding.java.util.ValueManager.castFunc( C6::from ) ) );
    }
    
    public static BasicType fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
        return jolie.runtime.embedding.java.util.ValueManager.choiceFrom( v, java.util.List.of( jolie.runtime.embedding.java.util.ValueManager.castFunc( C1::fromValue ), jolie.runtime.embedding.java.util.ValueManager.castFunc( C2::fromValue ), jolie.runtime.embedding.java.util.ValueManager.castFunc( C3::fromValue ), jolie.runtime.embedding.java.util.ValueManager.castFunc( C4::fromValue ), jolie.runtime.embedding.java.util.ValueManager.castFunc( C5::fromValue ), jolie.runtime.embedding.java.util.ValueManager.castFunc( C6::fromValue ) ) );
    }
    
    public static jolie.runtime.Value toValue( BasicType t ) { return t.jolieRepr(); }
    
    public static class ListBuilder extends jolie.runtime.embedding.java.util.AbstractListBuilder<ListBuilder, BasicType> {
        
        private ListBuilder() {}
        private ListBuilder( java.util.SequencedCollection<? extends jolie.runtime.embedding.java.JolieValue> c ) { super( c, BasicType::from ); }
        
        protected ListBuilder self() { return this; }
        
        public ListBuilder add1( joliex.meta.spec.types.VoidBasicType option ) { return add( new C1( option ) ); }
        public ListBuilder add1( int index, joliex.meta.spec.types.VoidBasicType option ) { return add( index, new C1( option ) ); }
        public ListBuilder set1( int index, joliex.meta.spec.types.VoidBasicType option ) { return set( index, new C1( option ) ); }
        
        public ListBuilder add1( java.util.function.Function<joliex.meta.spec.types.VoidBasicType.Builder, joliex.meta.spec.types.VoidBasicType> b ) { return add1( b.apply( joliex.meta.spec.types.VoidBasicType.builder() ) ); }
        public ListBuilder add1( int index, java.util.function.Function<joliex.meta.spec.types.VoidBasicType.Builder, joliex.meta.spec.types.VoidBasicType> b ) { return add1( index, b.apply( joliex.meta.spec.types.VoidBasicType.builder() ) ); }
        public ListBuilder set1( int index, java.util.function.Function<joliex.meta.spec.types.VoidBasicType.Builder, joliex.meta.spec.types.VoidBasicType> b ) { return set1( index, b.apply( joliex.meta.spec.types.VoidBasicType.builder() ) ); }
        
        public ListBuilder add2( joliex.meta.spec.types.BoolBasicType option ) { return add( new C2( option ) ); }
        public ListBuilder add2( int index, joliex.meta.spec.types.BoolBasicType option ) { return add( index, new C2( option ) ); }
        public ListBuilder set2( int index, joliex.meta.spec.types.BoolBasicType option ) { return set( index, new C2( option ) ); }
        
        public ListBuilder add2( java.util.function.Function<joliex.meta.spec.types.BoolBasicType.Builder, joliex.meta.spec.types.BoolBasicType> b ) { return add2( b.apply( joliex.meta.spec.types.BoolBasicType.builder() ) ); }
        public ListBuilder add2( int index, java.util.function.Function<joliex.meta.spec.types.BoolBasicType.Builder, joliex.meta.spec.types.BoolBasicType> b ) { return add2( index, b.apply( joliex.meta.spec.types.BoolBasicType.builder() ) ); }
        public ListBuilder set2( int index, java.util.function.Function<joliex.meta.spec.types.BoolBasicType.Builder, joliex.meta.spec.types.BoolBasicType> b ) { return set2( index, b.apply( joliex.meta.spec.types.BoolBasicType.builder() ) ); }
        
        public ListBuilder add3( joliex.meta.spec.types.IntBasicType option ) { return add( new C3( option ) ); }
        public ListBuilder add3( int index, joliex.meta.spec.types.IntBasicType option ) { return add( index, new C3( option ) ); }
        public ListBuilder set3( int index, joliex.meta.spec.types.IntBasicType option ) { return set( index, new C3( option ) ); }
        
        public ListBuilder add3( java.util.function.Function<joliex.meta.spec.types.IntBasicType.Builder, joliex.meta.spec.types.IntBasicType> b ) { return add3( b.apply( joliex.meta.spec.types.IntBasicType.builder() ) ); }
        public ListBuilder add3( int index, java.util.function.Function<joliex.meta.spec.types.IntBasicType.Builder, joliex.meta.spec.types.IntBasicType> b ) { return add3( index, b.apply( joliex.meta.spec.types.IntBasicType.builder() ) ); }
        public ListBuilder set3( int index, java.util.function.Function<joliex.meta.spec.types.IntBasicType.Builder, joliex.meta.spec.types.IntBasicType> b ) { return set3( index, b.apply( joliex.meta.spec.types.IntBasicType.builder() ) ); }
        
        public ListBuilder add4( joliex.meta.spec.types.LongBasicType option ) { return add( new C4( option ) ); }
        public ListBuilder add4( int index, joliex.meta.spec.types.LongBasicType option ) { return add( index, new C4( option ) ); }
        public ListBuilder set4( int index, joliex.meta.spec.types.LongBasicType option ) { return set( index, new C4( option ) ); }
        
        public ListBuilder add4( java.util.function.Function<joliex.meta.spec.types.LongBasicType.Builder, joliex.meta.spec.types.LongBasicType> b ) { return add4( b.apply( joliex.meta.spec.types.LongBasicType.builder() ) ); }
        public ListBuilder add4( int index, java.util.function.Function<joliex.meta.spec.types.LongBasicType.Builder, joliex.meta.spec.types.LongBasicType> b ) { return add4( index, b.apply( joliex.meta.spec.types.LongBasicType.builder() ) ); }
        public ListBuilder set4( int index, java.util.function.Function<joliex.meta.spec.types.LongBasicType.Builder, joliex.meta.spec.types.LongBasicType> b ) { return set4( index, b.apply( joliex.meta.spec.types.LongBasicType.builder() ) ); }
        
        public ListBuilder add5( joliex.meta.spec.types.DoubleBasicType option ) { return add( new C5( option ) ); }
        public ListBuilder add5( int index, joliex.meta.spec.types.DoubleBasicType option ) { return add( index, new C5( option ) ); }
        public ListBuilder set5( int index, joliex.meta.spec.types.DoubleBasicType option ) { return set( index, new C5( option ) ); }
        
        public ListBuilder add5( java.util.function.Function<joliex.meta.spec.types.DoubleBasicType.Builder, joliex.meta.spec.types.DoubleBasicType> b ) { return add5( b.apply( joliex.meta.spec.types.DoubleBasicType.builder() ) ); }
        public ListBuilder add5( int index, java.util.function.Function<joliex.meta.spec.types.DoubleBasicType.Builder, joliex.meta.spec.types.DoubleBasicType> b ) { return add5( index, b.apply( joliex.meta.spec.types.DoubleBasicType.builder() ) ); }
        public ListBuilder set5( int index, java.util.function.Function<joliex.meta.spec.types.DoubleBasicType.Builder, joliex.meta.spec.types.DoubleBasicType> b ) { return set5( index, b.apply( joliex.meta.spec.types.DoubleBasicType.builder() ) ); }
        
        public ListBuilder add6( joliex.meta.spec.types.StringBasicType option ) { return add( new C6( option ) ); }
        public ListBuilder add6( int index, joliex.meta.spec.types.StringBasicType option ) { return add( index, new C6( option ) ); }
        public ListBuilder set6( int index, joliex.meta.spec.types.StringBasicType option ) { return set( index, new C6( option ) ); }
        
        public ListBuilder add6( java.util.function.Function<joliex.meta.spec.types.StringBasicType.Builder, joliex.meta.spec.types.StringBasicType> b ) { return add6( b.apply( joliex.meta.spec.types.StringBasicType.builder() ) ); }
        public ListBuilder add6( int index, java.util.function.Function<joliex.meta.spec.types.StringBasicType.Builder, joliex.meta.spec.types.StringBasicType> b ) { return add6( index, b.apply( joliex.meta.spec.types.StringBasicType.builder() ) ); }
        public ListBuilder set6( int index, java.util.function.Function<joliex.meta.spec.types.StringBasicType.Builder, joliex.meta.spec.types.StringBasicType> b ) { return set6( index, b.apply( joliex.meta.spec.types.StringBasicType.builder() ) ); }
        
        public java.util.List<BasicType> build() { return super.build(); }
    }
}