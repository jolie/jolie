package joliex.meta.spec.types;

/**
 * this class is a {@link jolie.runtime.embedding.java.TypedStructure} which can be described as
 * follows:
 *
 * <pre>
 * ranges[0,2147483647]: {@link joliex.meta.spec.types.LongRange}
 * </pre>
 *
 * @see jolie.runtime.embedding.java.JolieValue
 * @see jolie.runtime.embedding.java.JolieNative
 * @see joliex.meta.spec.types.LongRange
 * @see #builder()
 */
public final class LongRefinement extends jolie.runtime.embedding.java.TypedStructure {

	private static final java.util.Set< java.lang.String > FIELD_KEYS = fieldKeys( LongRefinement.class );

	@jolie.runtime.embedding.java.util.JolieName( "ranges" )
	private final java.util.List< joliex.meta.spec.types.LongRange > ranges;

	public LongRefinement( java.util.SequencedCollection< joliex.meta.spec.types.LongRange > ranges ) {
		this.ranges =
			jolie.runtime.embedding.java.util.ValueManager.validated( "ranges", ranges, 0, 2147483647, t -> t );
	}

	public java.util.List< joliex.meta.spec.types.LongRange > ranges() {
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

	public static jolie.runtime.embedding.java.util.StructureListBuilder< LongRefinement, Builder > listBuilder() {
		return new jolie.runtime.embedding.java.util.StructureListBuilder<>( LongRefinement::builder );
	}

	public static jolie.runtime.embedding.java.util.StructureListBuilder< LongRefinement, Builder > listBuilder(
		java.util.SequencedCollection< ? extends jolie.runtime.embedding.java.JolieValue > from ) {
		return from != null
			? new jolie.runtime.embedding.java.util.StructureListBuilder<>( from, LongRefinement::from,
				LongRefinement::builder )
			: listBuilder();
	}

	public static LongRefinement from( jolie.runtime.embedding.java.JolieValue j )
		throws jolie.runtime.embedding.java.TypeValidationException {
		return new LongRefinement(
			jolie.runtime.embedding.java.util.ValueManager.fieldFrom(
				j.getChildOrDefault( "ranges", java.util.List.of() ), joliex.meta.spec.types.LongRange::from ) );
	}

	public static LongRefinement fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
		jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
		return new LongRefinement(
			jolie.runtime.embedding.java.util.ValueManager.vectorFieldFrom( v, "ranges",
				joliex.meta.spec.types.LongRange::fromValue ) );
	}

	public static jolie.runtime.Value toValue( LongRefinement t ) {
		final jolie.runtime.Value v = jolie.runtime.Value.create();

		t.ranges().forEach( c -> v.getNewChild( "ranges" ).deepCopy( joliex.meta.spec.types.LongRange.toValue( c ) ) );

		return v;
	}

	public static class Builder {

		private java.util.SequencedCollection< joliex.meta.spec.types.LongRange > ranges;

		private Builder() {}

		private Builder( jolie.runtime.embedding.java.JolieValue j ) {
			this.ranges = jolie.runtime.embedding.java.util.ValueManager.fieldFrom(
				j.getChildOrDefault( "ranges", java.util.List.of() ), joliex.meta.spec.types.LongRange::from );
		}

		public Builder ranges( java.util.SequencedCollection< joliex.meta.spec.types.LongRange > ranges ) {
			this.ranges = ranges;
			return this;
		}

		public Builder ranges(
			java.util.function.Function< jolie.runtime.embedding.java.util.StructureListBuilder< joliex.meta.spec.types.LongRange, joliex.meta.spec.types.LongRange.Builder >, java.util.List< joliex.meta.spec.types.LongRange > > f ) {
			return ranges( f.apply( new jolie.runtime.embedding.java.util.StructureListBuilder<>(
				joliex.meta.spec.types.LongRange::builder ) ) );
		}

		public LongRefinement build() {
			return new LongRefinement( ranges );
		}
	}
}
