package joliex.meta.spec.types;

/**
 * this class is a {@link jolie.runtime.embedding.java.TypedStructure} which can be described as
 * follows:
 *
 * <pre>
 * enumerated("enum")[1,2147483647]: {@link java.lang.String}
 * </pre>
 *
 * @see jolie.runtime.embedding.java.JolieValue
 * @see jolie.runtime.embedding.java.JolieNative
 * @see #builder()
 */
public final class StringRefinementEnum extends jolie.runtime.embedding.java.TypedStructure {

	private static final java.util.Set< java.lang.String > FIELD_KEYS = fieldKeys( StringRefinementEnum.class );

	@jolie.runtime.embedding.java.util.JolieName( "enum" )
	private final java.util.List< java.lang.String > enumerated;

	public StringRefinementEnum( java.util.SequencedCollection< java.lang.String > enumerated ) {
		this.enumerated =
			jolie.runtime.embedding.java.util.ValueManager.validated( "enumerated", enumerated, 1, 2147483647, t -> t );
	}

	public java.util.List< java.lang.String > enumerated() {
		return enumerated;
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

	public static jolie.runtime.embedding.java.util.StructureListBuilder< StringRefinementEnum, Builder > listBuilder() {
		return new jolie.runtime.embedding.java.util.StructureListBuilder<>( StringRefinementEnum::builder );
	}

	public static jolie.runtime.embedding.java.util.StructureListBuilder< StringRefinementEnum, Builder > listBuilder(
		java.util.SequencedCollection< ? extends jolie.runtime.embedding.java.JolieValue > from ) {
		return from != null
			? new jolie.runtime.embedding.java.util.StructureListBuilder<>( from, StringRefinementEnum::from,
				StringRefinementEnum::builder )
			: listBuilder();
	}

	public static StringRefinementEnum from( jolie.runtime.embedding.java.JolieValue j )
		throws jolie.runtime.embedding.java.TypeValidationException {
		return new StringRefinementEnum(
			jolie.runtime.embedding.java.util.ValueManager.fieldFrom(
				j.getChildOrDefault( "enum", java.util.List.of() ),
				c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieString content
					? content.value()
					: null ) );
	}

	public static StringRefinementEnum fromValue( jolie.runtime.Value v )
		throws jolie.runtime.typing.TypeCheckingException {
		jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
		return new StringRefinementEnum(
			jolie.runtime.embedding.java.util.ValueManager.vectorFieldFrom( v, "enum",
				jolie.runtime.embedding.java.JolieNative.JolieString::fieldFromValue ) );
	}

	public static jolie.runtime.Value toValue( StringRefinementEnum t ) {
		final jolie.runtime.Value v = jolie.runtime.Value.create();

		t.enumerated().forEach( c -> v.getNewChild( "enum" ).setValue( c ) );

		return v;
	}

	public static class Builder {

		private java.util.SequencedCollection< java.lang.String > enumerated;

		private Builder() {}

		private Builder( jolie.runtime.embedding.java.JolieValue j ) {
			this.enumerated = jolie.runtime.embedding.java.util.ValueManager.fieldFrom(
				j.getChildOrDefault( "enum", java.util.List.of() ),
				c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieString content
					? content.value()
					: null );
		}

		public Builder enumerated( java.util.SequencedCollection< java.lang.String > enumerated ) {
			this.enumerated = enumerated;
			return this;
		}

		public Builder enumerated( java.lang.String... values ) {
			return enumerated( values == null ? null : java.util.List.of( values ) );
		}

		public StringRefinementEnum build() {
			return new StringRefinementEnum( enumerated );
		}
	}
}
