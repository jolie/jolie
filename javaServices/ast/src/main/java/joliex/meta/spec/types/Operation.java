package joliex.meta.spec.types;

/**
 * This is a sealed interface representing the following choice type:
 *
 * <pre>
 * Operation: joliex.meta.spec.types.OneWayOperation | joliex.meta.spec.types.RequestResponseOperation
 * </pre>
 *
 * @see jolie.runtime.embedding.java.JolieValue
 * @see jolie.runtime.embedding.java.JolieNative
 * @see joliex.meta.spec.types.OneWayOperation
 * @see joliex.meta.spec.types.RequestResponseOperation
 * @see #of1(joliex.meta.spec.types.OneWayOperation)
 * @see #of2(joliex.meta.spec.types.RequestResponseOperation)
 */
public sealed interface Operation extends jolie.runtime.embedding.java.JolieValue {

	jolie.runtime.Value jolieRepr();

	public static record C1(joliex.meta.spec.types.OneWayOperation option) implements Operation {

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
			return joliex.meta.spec.types.OneWayOperation.toValue( option );
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
			return new C1( joliex.meta.spec.types.OneWayOperation.from( j ) );
		}

		public static C1 fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
			return new C1( joliex.meta.spec.types.OneWayOperation.fromValue( v ) );
		}

		public static jolie.runtime.Value toValue( C1 t ) {
			return t.jolieRepr();
		}
	}

	public static record C2(joliex.meta.spec.types.RequestResponseOperation option) implements Operation {

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
			return joliex.meta.spec.types.RequestResponseOperation.toValue( option );
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
			return new C2( joliex.meta.spec.types.RequestResponseOperation.from( j ) );
		}

		public static C2 fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
			return new C2( joliex.meta.spec.types.RequestResponseOperation.fromValue( v ) );
		}

		public static jolie.runtime.Value toValue( C2 t ) {
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

	public static Operation of1( joliex.meta.spec.types.OneWayOperation option ) {
		return new C1( option );
	}

	public static Operation of1(
		java.util.function.Function< joliex.meta.spec.types.OneWayOperation.Builder, joliex.meta.spec.types.OneWayOperation > f ) {
		return of1( f.apply( joliex.meta.spec.types.OneWayOperation.builder() ) );
	}

	public static Operation of2( joliex.meta.spec.types.RequestResponseOperation option ) {
		return new C2( option );
	}

	public static Operation of2(
		java.util.function.Function< joliex.meta.spec.types.RequestResponseOperation.Builder, joliex.meta.spec.types.RequestResponseOperation > f ) {
		return of2( f.apply( joliex.meta.spec.types.RequestResponseOperation.builder() ) );
	}

	public static Operation from( jolie.runtime.embedding.java.JolieValue j )
		throws jolie.runtime.embedding.java.TypeValidationException {
		return jolie.runtime.embedding.java.util.ValueManager.choiceFrom( j,
			java.util.List.of( jolie.runtime.embedding.java.util.ValueManager.castFunc( C1::from ),
				jolie.runtime.embedding.java.util.ValueManager.castFunc( C2::from ) ) );
	}

	public static Operation fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
		return jolie.runtime.embedding.java.util.ValueManager.choiceFrom( v,
			java.util.List.of( jolie.runtime.embedding.java.util.ValueManager.castFunc( C1::fromValue ),
				jolie.runtime.embedding.java.util.ValueManager.castFunc( C2::fromValue ) ) );
	}

	public static jolie.runtime.Value toValue( Operation t ) {
		return t.jolieRepr();
	}

	public static class ListBuilder
		extends jolie.runtime.embedding.java.util.AbstractListBuilder< ListBuilder, Operation > {

		private ListBuilder() {}

		private ListBuilder( java.util.SequencedCollection< ? extends jolie.runtime.embedding.java.JolieValue > c ) {
			super( c, Operation::from );
		}

		protected ListBuilder self() {
			return this;
		}

		public ListBuilder add1( joliex.meta.spec.types.OneWayOperation option ) {
			return add( new C1( option ) );
		}

		public ListBuilder add1( int index, joliex.meta.spec.types.OneWayOperation option ) {
			return add( index, new C1( option ) );
		}

		public ListBuilder set1( int index, joliex.meta.spec.types.OneWayOperation option ) {
			return set( index, new C1( option ) );
		}

		public ListBuilder add1(
			java.util.function.Function< joliex.meta.spec.types.OneWayOperation.Builder, joliex.meta.spec.types.OneWayOperation > b ) {
			return add1( b.apply( joliex.meta.spec.types.OneWayOperation.builder() ) );
		}

		public ListBuilder add1( int index,
			java.util.function.Function< joliex.meta.spec.types.OneWayOperation.Builder, joliex.meta.spec.types.OneWayOperation > b ) {
			return add1( index, b.apply( joliex.meta.spec.types.OneWayOperation.builder() ) );
		}

		public ListBuilder set1( int index,
			java.util.function.Function< joliex.meta.spec.types.OneWayOperation.Builder, joliex.meta.spec.types.OneWayOperation > b ) {
			return set1( index, b.apply( joliex.meta.spec.types.OneWayOperation.builder() ) );
		}

		public ListBuilder add2( joliex.meta.spec.types.RequestResponseOperation option ) {
			return add( new C2( option ) );
		}

		public ListBuilder add2( int index, joliex.meta.spec.types.RequestResponseOperation option ) {
			return add( index, new C2( option ) );
		}

		public ListBuilder set2( int index, joliex.meta.spec.types.RequestResponseOperation option ) {
			return set( index, new C2( option ) );
		}

		public ListBuilder add2(
			java.util.function.Function< joliex.meta.spec.types.RequestResponseOperation.Builder, joliex.meta.spec.types.RequestResponseOperation > b ) {
			return add2( b.apply( joliex.meta.spec.types.RequestResponseOperation.builder() ) );
		}

		public ListBuilder add2( int index,
			java.util.function.Function< joliex.meta.spec.types.RequestResponseOperation.Builder, joliex.meta.spec.types.RequestResponseOperation > b ) {
			return add2( index, b.apply( joliex.meta.spec.types.RequestResponseOperation.builder() ) );
		}

		public ListBuilder set2( int index,
			java.util.function.Function< joliex.meta.spec.types.RequestResponseOperation.Builder, joliex.meta.spec.types.RequestResponseOperation > b ) {
			return set2( index, b.apply( joliex.meta.spec.types.RequestResponseOperation.builder() ) );
		}

		public java.util.List< Operation > build() {
			return super.build();
		}
	}
}
