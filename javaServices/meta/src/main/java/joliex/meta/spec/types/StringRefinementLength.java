package joliex.meta.spec.types;

/**
 * this class is a {@link jolie.runtime.embedding.java.TypedStructure} which can be described as
 * follows:
 *
 * <pre>
 * length: {@link joliex.meta.spec.types.IntRange}
 * </pre>
 *
 * @see jolie.runtime.embedding.java.JolieValue
 * @see jolie.runtime.embedding.java.JolieNative
 * @see joliex.meta.spec.types.IntRange
 * @see #builder()
 */
public final class StringRefinementLength extends jolie.runtime.embedding.java.TypedStructure {

	private static final java.util.Set< java.lang.String > FIELD_KEYS = fieldKeys( StringRefinementLength.class );

	@jolie.runtime.embedding.java.util.JolieName( "length" )
	private final joliex.meta.spec.types.IntRange length;

	public StringRefinementLength( joliex.meta.spec.types.IntRange length ) {
		this.length = jolie.runtime.embedding.java.util.ValueManager.validated( "length", length );
	}

	public joliex.meta.spec.types.IntRange length() {
		return length;
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

	public static jolie.runtime.embedding.java.util.StructureListBuilder< StringRefinementLength, Builder > listBuilder() {
		return new jolie.runtime.embedding.java.util.StructureListBuilder<>( StringRefinementLength::builder );
	}

	public static jolie.runtime.embedding.java.util.StructureListBuilder< StringRefinementLength, Builder > listBuilder(
		java.util.SequencedCollection< ? extends jolie.runtime.embedding.java.JolieValue > from ) {
		return from != null
			? new jolie.runtime.embedding.java.util.StructureListBuilder<>( from, StringRefinementLength::from,
				StringRefinementLength::builder )
			: listBuilder();
	}

	public static StringRefinementLength from( jolie.runtime.embedding.java.JolieValue j )
		throws jolie.runtime.embedding.java.TypeValidationException {
		return new StringRefinementLength(
			jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "length" ),
				joliex.meta.spec.types.IntRange::from ) );
	}

	public static StringRefinementLength fromValue( jolie.runtime.Value v )
		throws jolie.runtime.typing.TypeCheckingException {
		jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
		return new StringRefinementLength(
			jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "length",
				joliex.meta.spec.types.IntRange::fromValue ) );
	}

	public static jolie.runtime.Value toValue( StringRefinementLength t ) {
		final jolie.runtime.Value v = jolie.runtime.Value.create();

		v.getFirstChild( "length" ).deepCopy( joliex.meta.spec.types.IntRange.toValue( t.length() ) );

		return v;
	}

	public static class Builder {

		private joliex.meta.spec.types.IntRange length;

		private Builder() {}

		private Builder( jolie.runtime.embedding.java.JolieValue j ) {
			this.length = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "length" ),
				joliex.meta.spec.types.IntRange::from );
		}

		public Builder length( joliex.meta.spec.types.IntRange length ) {
			this.length = length;
			return this;
		}

		public Builder length(
			java.util.function.Function< joliex.meta.spec.types.IntRange.Builder, joliex.meta.spec.types.IntRange > f ) {
			return length( f.apply( joliex.meta.spec.types.IntRange.builder() ) );
		}

		public StringRefinementLength build() {
			return new StringRefinementLength( length );
		}
	}
}
