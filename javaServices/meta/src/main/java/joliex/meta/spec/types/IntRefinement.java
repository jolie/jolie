package joliex.meta.spec.types;

/**
 * this class is a {@link jolie.runtime.embedding.java.TypedStructure} which can be described as
 * follows:
 *
 * <pre>
 * ranges[0,2147483647]: {@link joliex.meta.spec.types.IntRange}
 * </pre>
 *
 * @see jolie.runtime.embedding.java.JolieValue
 * @see jolie.runtime.embedding.java.JolieNative
 * @see joliex.meta.spec.types.IntRange
 * @see #builder()
 */
public final class IntRefinement extends jolie.runtime.embedding.java.TypedStructure {

	private static final java.util.Set< java.lang.String > FIELD_KEYS = fieldKeys( IntRefinement.class );

	@jolie.runtime.embedding.java.util.JolieName( "ranges" )
	private final java.util.List< joliex.meta.spec.types.IntRange > ranges;

	public IntRefinement( java.util.SequencedCollection< joliex.meta.spec.types.IntRange > ranges ) {
		this.ranges =
			jolie.runtime.embedding.java.util.ValueManager.validated( "ranges", ranges, 0, 2147483647, t -> t );
	}

	public java.util.List< joliex.meta.spec.types.IntRange > ranges() {
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

	public static jolie.runtime.embedding.java.util.StructureListBuilder< IntRefinement, Builder > listBuilder() {
		return new jolie.runtime.embedding.java.util.StructureListBuilder<>( IntRefinement::builder );
	}

	public static jolie.runtime.embedding.java.util.StructureListBuilder< IntRefinement, Builder > listBuilder(
		java.util.SequencedCollection< ? extends jolie.runtime.embedding.java.JolieValue > from ) {
		return from != null
			? new jolie.runtime.embedding.java.util.StructureListBuilder<>( from, IntRefinement::from,
				IntRefinement::builder )
			: listBuilder();
	}

	public static IntRefinement from( jolie.runtime.embedding.java.JolieValue j )
		throws jolie.runtime.embedding.java.TypeValidationException {
		return new IntRefinement(
			jolie.runtime.embedding.java.util.ValueManager.fieldFrom(
				j.getChildOrDefault( "ranges", java.util.List.of() ), joliex.meta.spec.types.IntRange::from ) );
	}

	public static IntRefinement fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
		jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
		return new IntRefinement(
			jolie.runtime.embedding.java.util.ValueManager.vectorFieldFrom( v, "ranges",
				joliex.meta.spec.types.IntRange::fromValue ) );
	}

	public static jolie.runtime.Value toValue( IntRefinement t ) {
		final jolie.runtime.Value v = jolie.runtime.Value.create();

		t.ranges().forEach( c -> v.getNewChild( "ranges" ).deepCopy( joliex.meta.spec.types.IntRange.toValue( c ) ) );

		return v;
	}

	public static class Builder {

		private java.util.SequencedCollection< joliex.meta.spec.types.IntRange > ranges;

		private Builder() {}

		private Builder( jolie.runtime.embedding.java.JolieValue j ) {
			this.ranges = jolie.runtime.embedding.java.util.ValueManager.fieldFrom(
				j.getChildOrDefault( "ranges", java.util.List.of() ), joliex.meta.spec.types.IntRange::from );
		}

		public Builder ranges( java.util.SequencedCollection< joliex.meta.spec.types.IntRange > ranges ) {
			this.ranges = ranges;
			return this;
		}

		public Builder ranges(
			java.util.function.Function< jolie.runtime.embedding.java.util.StructureListBuilder< joliex.meta.spec.types.IntRange, joliex.meta.spec.types.IntRange.Builder >, java.util.List< joliex.meta.spec.types.IntRange > > f ) {
			return ranges( f.apply( new jolie.runtime.embedding.java.util.StructureListBuilder<>(
				joliex.meta.spec.types.IntRange::builder ) ) );
		}

		public IntRefinement build() {
			return new IntRefinement( ranges );
		}
	}
}
