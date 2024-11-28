package joliex.meta.spec.types;

/**
 * This is a sealed interface representing the following choice type:
 *
 * <pre>
 * Type: S1 | S2 | S3
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
public sealed interface Type extends jolie.runtime.embedding.java.JolieValue {

	jolie.runtime.Value jolieRepr();

	public static record C1(S1 option) implements Type {

		public C1 {
			jolie.runtime.embedding.java.util.ValueManager.validated( "option", option );
		}

		public jolie.runtime.embedding.java.JolieNative.JolieVoid content() {
			return option.content();
		}

		public java.util.Map< java.lang.String, java.util.List< jolie.runtime.embedding.java.JolieValue > > children() {
			return option.children();
		}

		public jolie.runtime.Value jolieRepr() {
			return S1.toValue( option );
		}

		public boolean equals( java.lang.Object obj ) {
			return obj != null && obj instanceof jolie.runtime.embedding.java.JolieValue j && option.equals( j );
		}

		public int hashCode() {
			return option.hashCode();
		}

		public java.lang.String toString() {
			return option.toString();
		}

		public static C1 from( jolie.runtime.embedding.java.JolieValue j )
			throws jolie.runtime.embedding.java.TypeValidationException {
			return new C1( S1.from( j ) );
		}

		public static C1 fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
			return new C1( S1.fromValue( v ) );
		}

		public static jolie.runtime.Value toValue( C1 t ) {
			return t.jolieRepr();
		}
	}

	public static record C2(S2 option) implements Type {

		public C2 {
			jolie.runtime.embedding.java.util.ValueManager.validated( "option", option );
		}

		public jolie.runtime.embedding.java.JolieNative.JolieVoid content() {
			return option.content();
		}

		public java.util.Map< java.lang.String, java.util.List< jolie.runtime.embedding.java.JolieValue > > children() {
			return option.children();
		}

		public jolie.runtime.Value jolieRepr() {
			return S2.toValue( option );
		}

		public boolean equals( java.lang.Object obj ) {
			return obj != null && obj instanceof jolie.runtime.embedding.java.JolieValue j && option.equals( j );
		}

		public int hashCode() {
			return option.hashCode();
		}

		public java.lang.String toString() {
			return option.toString();
		}

		public static C2 from( jolie.runtime.embedding.java.JolieValue j )
			throws jolie.runtime.embedding.java.TypeValidationException {
			return new C2( S2.from( j ) );
		}

		public static C2 fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
			return new C2( S2.fromValue( v ) );
		}

		public static jolie.runtime.Value toValue( C2 t ) {
			return t.jolieRepr();
		}
	}

	public static record C3(S3 option) implements Type {

		public C3 {
			jolie.runtime.embedding.java.util.ValueManager.validated( "option", option );
		}

		public jolie.runtime.embedding.java.JolieNative.JolieVoid content() {
			return option.content();
		}

		public java.util.Map< java.lang.String, java.util.List< jolie.runtime.embedding.java.JolieValue > > children() {
			return option.children();
		}

		public jolie.runtime.Value jolieRepr() {
			return S3.toValue( option );
		}

		public boolean equals( java.lang.Object obj ) {
			return obj != null && obj instanceof jolie.runtime.embedding.java.JolieValue j && option.equals( j );
		}

		public int hashCode() {
			return option.hashCode();
		}

		public java.lang.String toString() {
			return option.toString();
		}

		public static C3 from( jolie.runtime.embedding.java.JolieValue j )
			throws jolie.runtime.embedding.java.TypeValidationException {
			return new C3( S3.from( j ) );
		}

		public static C3 fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
			return new C3( S3.fromValue( v ) );
		}

		public static jolie.runtime.Value toValue( C3 t ) {
			return t.jolieRepr();
		}
	}

	public static ListBuilder listBuilder() {
		return new ListBuilder();
	}

	public static ListBuilder listBuilder(
		java.util.SequencedCollection< jolie.runtime.embedding.java.JolieValue > from ) {
		return from != null ? new ListBuilder( from ) : listBuilder();
	}

	public static Type of1( S1 option ) {
		return new C1( option );
	}

	public static Type of1( java.util.function.Function< S1.Builder, S1 > f ) {
		return of1( f.apply( S1.builder() ) );
	}

	public static Type of2( S2 option ) {
		return new C2( option );
	}

	public static Type of2( java.util.function.Function< S2.Builder, S2 > f ) {
		return of2( f.apply( S2.builder() ) );
	}

	public static Type of3( S3 option ) {
		return new C3( option );
	}

	public static Type of3( java.util.function.Function< S3.Builder, S3 > f ) {
		return of3( f.apply( S3.builder() ) );
	}

	public static Type from( jolie.runtime.embedding.java.JolieValue j )
		throws jolie.runtime.embedding.java.TypeValidationException {
		return jolie.runtime.embedding.java.util.ValueManager.choiceFrom( j,
			java.util.List.of( jolie.runtime.embedding.java.util.ValueManager.castFunc( C1::from ),
				jolie.runtime.embedding.java.util.ValueManager.castFunc( C2::from ),
				jolie.runtime.embedding.java.util.ValueManager.castFunc( C3::from ) ) );
	}

	public static Type fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
		return jolie.runtime.embedding.java.util.ValueManager.choiceFrom( v,
			java.util.List.of( jolie.runtime.embedding.java.util.ValueManager.castFunc( C1::fromValue ),
				jolie.runtime.embedding.java.util.ValueManager.castFunc( C2::fromValue ),
				jolie.runtime.embedding.java.util.ValueManager.castFunc( C3::fromValue ) ) );
	}

	public static jolie.runtime.Value toValue( Type t ) {
		return t.jolieRepr();
	}


	/**
	 * this class is a {@link jolie.runtime.embedding.java.TypedStructure} which can be described as
	 * follows:
	 *
	 * <pre>
	 * tree: {@link joliex.meta.spec.types.TreeType}
	 * </pre>
	 *
	 * @see jolie.runtime.embedding.java.JolieValue
	 * @see jolie.runtime.embedding.java.JolieNative
	 * @see joliex.meta.spec.types.TreeType
	 * @see #builder()
	 */
	public static final class S1 extends jolie.runtime.embedding.java.TypedStructure {

		private static final java.util.Set< java.lang.String > FIELD_KEYS = fieldKeys( S1.class );

		@jolie.runtime.embedding.java.util.JolieName( "tree" )
		private final joliex.meta.spec.types.TreeType tree;

		public S1( joliex.meta.spec.types.TreeType tree ) {
			this.tree = jolie.runtime.embedding.java.util.ValueManager.validated( "tree", tree );
		}

		public joliex.meta.spec.types.TreeType tree() {
			return tree;
		}

		public jolie.runtime.embedding.java.JolieNative.JolieVoid content() {
			return new jolie.runtime.embedding.java.JolieNative.JolieVoid();
		}

		public static Builder builder() {
			return new Builder();
		}

		public static Builder builder( jolie.runtime.embedding.java.JolieValue from ) {
			return from != null ? new Builder( from ) : builder();
		}

		public static jolie.runtime.embedding.java.util.StructureListBuilder< S1, Builder > listBuilder() {
			return new jolie.runtime.embedding.java.util.StructureListBuilder<>( S1::builder );
		}

		public static jolie.runtime.embedding.java.util.StructureListBuilder< S1, Builder > listBuilder(
			java.util.SequencedCollection< ? extends jolie.runtime.embedding.java.JolieValue > from ) {
			return from != null
				? new jolie.runtime.embedding.java.util.StructureListBuilder<>( from, S1::from, S1::builder )
				: listBuilder();
		}

		public static S1 from( jolie.runtime.embedding.java.JolieValue j )
			throws jolie.runtime.embedding.java.TypeValidationException {
			return new S1(
				jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "tree" ),
					joliex.meta.spec.types.TreeType::from ) );
		}

		public static S1 fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
			jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
			return new S1(
				jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "tree",
					joliex.meta.spec.types.TreeType::fromValue ) );
		}

		public static jolie.runtime.Value toValue( S1 t ) {
			final jolie.runtime.Value v = jolie.runtime.Value.create();

			v.getFirstChild( "tree" ).deepCopy( joliex.meta.spec.types.TreeType.toValue( t.tree() ) );

			return v;
		}

		public static class Builder {

			private joliex.meta.spec.types.TreeType tree;

			private Builder() {}

			private Builder( jolie.runtime.embedding.java.JolieValue j ) {
				this.tree = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "tree" ),
					joliex.meta.spec.types.TreeType::from );
			}

			public Builder tree( joliex.meta.spec.types.TreeType tree ) {
				this.tree = tree;
				return this;
			}

			public Builder tree(
				java.util.function.Function< joliex.meta.spec.types.TreeType.Builder, joliex.meta.spec.types.TreeType > f ) {
				return tree( f.apply( joliex.meta.spec.types.TreeType.builder() ) );
			}

			public S1 build() {
				return new S1( tree );
			}
		}
	}


	/**
	 * this class is a {@link jolie.runtime.embedding.java.TypedStructure} which can be described as
	 * follows:
	 *
	 * <pre>
	 * choice: {@link joliex.meta.spec.types.ChoiceType}
	 * </pre>
	 *
	 * @see jolie.runtime.embedding.java.JolieValue
	 * @see jolie.runtime.embedding.java.JolieNative
	 * @see joliex.meta.spec.types.ChoiceType
	 * @see #builder()
	 */
	public static final class S2 extends jolie.runtime.embedding.java.TypedStructure {

		private static final java.util.Set< java.lang.String > FIELD_KEYS = fieldKeys( S2.class );

		@jolie.runtime.embedding.java.util.JolieName( "choice" )
		private final joliex.meta.spec.types.ChoiceType choice;

		public S2( joliex.meta.spec.types.ChoiceType choice ) {
			this.choice = jolie.runtime.embedding.java.util.ValueManager.validated( "choice", choice );
		}

		public joliex.meta.spec.types.ChoiceType choice() {
			return choice;
		}

		public jolie.runtime.embedding.java.JolieNative.JolieVoid content() {
			return new jolie.runtime.embedding.java.JolieNative.JolieVoid();
		}

		public static Builder builder() {
			return new Builder();
		}

		public static Builder builder( jolie.runtime.embedding.java.JolieValue from ) {
			return from != null ? new Builder( from ) : builder();
		}

		public static jolie.runtime.embedding.java.util.StructureListBuilder< S2, Builder > listBuilder() {
			return new jolie.runtime.embedding.java.util.StructureListBuilder<>( S2::builder );
		}

		public static jolie.runtime.embedding.java.util.StructureListBuilder< S2, Builder > listBuilder(
			java.util.SequencedCollection< ? extends jolie.runtime.embedding.java.JolieValue > from ) {
			return from != null
				? new jolie.runtime.embedding.java.util.StructureListBuilder<>( from, S2::from, S2::builder )
				: listBuilder();
		}

		public static S2 from( jolie.runtime.embedding.java.JolieValue j )
			throws jolie.runtime.embedding.java.TypeValidationException {
			return new S2(
				jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "choice" ),
					joliex.meta.spec.types.ChoiceType::from ) );
		}

		public static S2 fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
			jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
			return new S2(
				jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "choice",
					joliex.meta.spec.types.ChoiceType::fromValue ) );
		}

		public static jolie.runtime.Value toValue( S2 t ) {
			final jolie.runtime.Value v = jolie.runtime.Value.create();

			v.getFirstChild( "choice" ).deepCopy( joliex.meta.spec.types.ChoiceType.toValue( t.choice() ) );

			return v;
		}

		public static class Builder {

			private joliex.meta.spec.types.ChoiceType choice;

			private Builder() {}

			private Builder( jolie.runtime.embedding.java.JolieValue j ) {
				this.choice = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "choice" ),
					joliex.meta.spec.types.ChoiceType::from );
			}

			public Builder choice( joliex.meta.spec.types.ChoiceType choice ) {
				this.choice = choice;
				return this;
			}

			public Builder choice(
				java.util.function.Function< joliex.meta.spec.types.ChoiceType.Builder, joliex.meta.spec.types.ChoiceType > f ) {
				return choice( f.apply( joliex.meta.spec.types.ChoiceType.builder() ) );
			}

			public S2 build() {
				return new S2( choice );
			}
		}
	}


	/**
	 * this class is a {@link jolie.runtime.embedding.java.TypedStructure} which can be described as
	 * follows:
	 *
	 * <pre>
	 * ref: {@link joliex.meta.spec.types.LocatedSymbolRef}
	 * </pre>
	 *
	 * @see jolie.runtime.embedding.java.JolieValue
	 * @see jolie.runtime.embedding.java.JolieNative
	 * @see joliex.meta.spec.types.LocatedSymbolRef
	 * @see #builder()
	 */
	public static final class S3 extends jolie.runtime.embedding.java.TypedStructure {

		private static final java.util.Set< java.lang.String > FIELD_KEYS = fieldKeys( S3.class );

		@jolie.runtime.embedding.java.util.JolieName( "ref" )
		private final joliex.meta.spec.types.LocatedSymbolRef ref;

		public S3( joliex.meta.spec.types.LocatedSymbolRef ref ) {
			this.ref = jolie.runtime.embedding.java.util.ValueManager.validated( "ref", ref );
		}

		public joliex.meta.spec.types.LocatedSymbolRef ref() {
			return ref;
		}

		public jolie.runtime.embedding.java.JolieNative.JolieVoid content() {
			return new jolie.runtime.embedding.java.JolieNative.JolieVoid();
		}

		public static Builder builder() {
			return new Builder();
		}

		public static Builder builder( jolie.runtime.embedding.java.JolieValue from ) {
			return from != null ? new Builder( from ) : builder();
		}

		public static jolie.runtime.embedding.java.util.StructureListBuilder< S3, Builder > listBuilder() {
			return new jolie.runtime.embedding.java.util.StructureListBuilder<>( S3::builder );
		}

		public static jolie.runtime.embedding.java.util.StructureListBuilder< S3, Builder > listBuilder(
			java.util.SequencedCollection< ? extends jolie.runtime.embedding.java.JolieValue > from ) {
			return from != null
				? new jolie.runtime.embedding.java.util.StructureListBuilder<>( from, S3::from, S3::builder )
				: listBuilder();
		}

		public static S3 from( jolie.runtime.embedding.java.JolieValue j )
			throws jolie.runtime.embedding.java.TypeValidationException {
			return new S3(
				jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "ref" ),
					joliex.meta.spec.types.LocatedSymbolRef::from ) );
		}

		public static S3 fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
			jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
			return new S3(
				jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "ref",
					joliex.meta.spec.types.LocatedSymbolRef::fromValue ) );
		}

		public static jolie.runtime.Value toValue( S3 t ) {
			final jolie.runtime.Value v = jolie.runtime.Value.create();

			v.getFirstChild( "ref" ).deepCopy( joliex.meta.spec.types.LocatedSymbolRef.toValue( t.ref() ) );

			return v;
		}

		public static class Builder {

			private joliex.meta.spec.types.LocatedSymbolRef ref;

			private Builder() {}

			private Builder( jolie.runtime.embedding.java.JolieValue j ) {
				this.ref = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "ref" ),
					joliex.meta.spec.types.LocatedSymbolRef::from );
			}

			public Builder ref( joliex.meta.spec.types.LocatedSymbolRef ref ) {
				this.ref = ref;
				return this;
			}

			public Builder ref(
				java.util.function.Function< joliex.meta.spec.types.LocatedSymbolRef.Builder, joliex.meta.spec.types.LocatedSymbolRef > f ) {
				return ref( f.apply( joliex.meta.spec.types.LocatedSymbolRef.builder() ) );
			}

			public S3 build() {
				return new S3( ref );
			}
		}
	}

	public static class ListBuilder extends jolie.runtime.embedding.java.util.AbstractListBuilder< ListBuilder, Type > {

		private ListBuilder() {}

		private ListBuilder( java.util.SequencedCollection< ? extends jolie.runtime.embedding.java.JolieValue > c ) {
			super( c, Type::from );
		}

		protected ListBuilder self() {
			return this;
		}

		public ListBuilder add1( S1 option ) {
			return add( new C1( option ) );
		}

		public ListBuilder add1( int index, S1 option ) {
			return add( index, new C1( option ) );
		}

		public ListBuilder set1( int index, S1 option ) {
			return set( index, new C1( option ) );
		}

		public ListBuilder add1( java.util.function.Function< S1.Builder, S1 > b ) {
			return add1( b.apply( S1.builder() ) );
		}

		public ListBuilder add1( int index, java.util.function.Function< S1.Builder, S1 > b ) {
			return add1( index, b.apply( S1.builder() ) );
		}

		public ListBuilder set1( int index, java.util.function.Function< S1.Builder, S1 > b ) {
			return set1( index, b.apply( S1.builder() ) );
		}

		public ListBuilder add2( S2 option ) {
			return add( new C2( option ) );
		}

		public ListBuilder add2( int index, S2 option ) {
			return add( index, new C2( option ) );
		}

		public ListBuilder set2( int index, S2 option ) {
			return set( index, new C2( option ) );
		}

		public ListBuilder add2( java.util.function.Function< S2.Builder, S2 > b ) {
			return add2( b.apply( S2.builder() ) );
		}

		public ListBuilder add2( int index, java.util.function.Function< S2.Builder, S2 > b ) {
			return add2( index, b.apply( S2.builder() ) );
		}

		public ListBuilder set2( int index, java.util.function.Function< S2.Builder, S2 > b ) {
			return set2( index, b.apply( S2.builder() ) );
		}

		public ListBuilder add3( S3 option ) {
			return add( new C3( option ) );
		}

		public ListBuilder add3( int index, S3 option ) {
			return add( index, new C3( option ) );
		}

		public ListBuilder set3( int index, S3 option ) {
			return set( index, new C3( option ) );
		}

		public ListBuilder add3( java.util.function.Function< S3.Builder, S3 > b ) {
			return add3( b.apply( S3.builder() ) );
		}

		public ListBuilder add3( int index, java.util.function.Function< S3.Builder, S3 > b ) {
			return add3( index, b.apply( S3.builder() ) );
		}

		public ListBuilder set3( int index, java.util.function.Function< S3.Builder, S3 > b ) {
			return set3( index, b.apply( S3.builder() ) );
		}

		public java.util.List< Type > build() {
			return super.build();
		}
	}
}
