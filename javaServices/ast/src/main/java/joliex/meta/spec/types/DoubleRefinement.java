package joliex.meta.spec.types;

/**
 * this class is a {@link jolie.runtime.embedding.java.TypedStructure} which can be described as
 * follows:
 *
 * <pre>
 * ranges[0,2147483647]: {@link joliex.meta.spec.types.DoubleRange}
 * </pre>
 *
 * @see jolie.runtime.embedding.java.JolieValue
 * @see jolie.runtime.embedding.java.JolieNative
 * @see joliex.meta.spec.types.DoubleRange
 * @see #builder()
 */
public final class DoubleRefinement extends jolie.runtime.embedding.java.TypedStructure {

	private static final java.util.Set< java.lang.String > FIELD_KEYS = fieldKeys( DoubleRefinement.class );

	@jolie.runtime.embedding.java.util.JolieName( "ranges" )
	private final java.util.List< joliex.meta.spec.types.DoubleRange > ranges;

	public DoubleRefinement( java.util.SequencedCollection< joliex.meta.spec.types.DoubleRange > ranges ) {
		this.ranges =
			jolie.runtime.embedding.java.util.ValueManager.validated( "ranges", ranges, 0, 2147483647, t -> t );
	}

	public java.util.List< joliex.meta.spec.types.DoubleRange > ranges() {
		return ranges;
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

	public static jolie.runtime.embedding.java.util.StructureListBuilder< DoubleRefinement, Builder > listBuilder() {
		return new jolie.runtime.embedding.java.util.StructureListBuilder<>( DoubleRefinement::builder );
	}

	public static jolie.runtime.embedding.java.util.StructureListBuilder< DoubleRefinement, Builder > listBuilder(
		java.util.SequencedCollection< ? extends jolie.runtime.embedding.java.JolieValue > from ) {
		return from != null
			? new jolie.runtime.embedding.java.util.StructureListBuilder<>( from, DoubleRefinement::from,
				DoubleRefinement::builder )
			: listBuilder();
	}

	public static DoubleRefinement from( jolie.runtime.embedding.java.JolieValue j )
		throws jolie.runtime.embedding.java.TypeValidationException {
		return new DoubleRefinement(
			jolie.runtime.embedding.java.util.ValueManager.fieldFrom(
				j.getChildOrDefault( "ranges", java.util.List.of() ), joliex.meta.spec.types.DoubleRange::from ) );
	}

	public static DoubleRefinement fromValue( jolie.runtime.Value v )
		throws jolie.runtime.typing.TypeCheckingException {
		jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
		return new DoubleRefinement(
			jolie.runtime.embedding.java.util.ValueManager.vectorFieldFrom( v, "ranges",
				joliex.meta.spec.types.DoubleRange::fromValue ) );
	}

	public static jolie.runtime.Value toValue( DoubleRefinement t ) {
		final jolie.runtime.Value v = jolie.runtime.Value.create();

		t.ranges()
			.forEach( c -> v.getNewChild( "ranges" ).deepCopy( joliex.meta.spec.types.DoubleRange.toValue( c ) ) );

		return v;
	}

	public static class Builder {

		private java.util.SequencedCollection< joliex.meta.spec.types.DoubleRange > ranges;

		private Builder() {}

		private Builder( jolie.runtime.embedding.java.JolieValue j ) {
			this.ranges = jolie.runtime.embedding.java.util.ValueManager.fieldFrom(
				j.getChildOrDefault( "ranges", java.util.List.of() ), joliex.meta.spec.types.DoubleRange::from );
		}

		public Builder ranges( java.util.SequencedCollection< joliex.meta.spec.types.DoubleRange > ranges ) {
			this.ranges = ranges;
			return this;
		}

		public Builder ranges(
			java.util.function.Function< jolie.runtime.embedding.java.util.StructureListBuilder< joliex.meta.spec.types.DoubleRange, joliex.meta.spec.types.DoubleRange.Builder >, java.util.List< joliex.meta.spec.types.DoubleRange > > f ) {
			return ranges( f.apply( new jolie.runtime.embedding.java.util.StructureListBuilder<>(
				joliex.meta.spec.types.DoubleRange::builder ) ) );
		}

		public DoubleRefinement build() {
			return new DoubleRefinement( ranges );
		}
	}
}
