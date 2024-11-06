package joliex.meta.spec.types;

/**
 * this class is a {@link jolie.runtime.embedding.java.TypedStructure} which can be described as
 * follows:
 *
 * <pre>
 *
 * contentValue: {@link java.lang.String}
     * textLocation: {@link joliex.meta.spec.types.Location}
 * </pre>
 *
 * @see jolie.runtime.embedding.java.JolieValue
 * @see jolie.runtime.embedding.java.JolieNative
 * @see joliex.meta.spec.types.Location
 * @see #builder()
 */
public final class LocatedSymbolRef extends jolie.runtime.embedding.java.TypedStructure {

	private static final java.util.Set< java.lang.String > FIELD_KEYS = fieldKeys( LocatedSymbolRef.class );

	private final java.lang.String contentValue;
	@jolie.runtime.embedding.java.util.JolieName( "textLocation" )
	private final joliex.meta.spec.types.Location textLocation;

	public LocatedSymbolRef( java.lang.String contentValue, joliex.meta.spec.types.Location textLocation ) {
		this.contentValue = jolie.runtime.embedding.java.util.ValueManager.validated( "contentValue", contentValue );
		this.textLocation = jolie.runtime.embedding.java.util.ValueManager.validated( "textLocation", textLocation );
	}

	public java.lang.String contentValue() {
		return contentValue;
	}

	public joliex.meta.spec.types.Location textLocation() {
		return textLocation;
	}

	public jolie.runtime.embedding.java.JolieNative.JolieString content() {
		return new jolie.runtime.embedding.java.JolieNative.JolieString( contentValue );
	}

	public static Builder builder() {
		return new Builder();
	}

	public static Builder builder( java.lang.String contentValue ) {
		return builder().contentValue( contentValue );
	}

	public static Builder builder( jolie.runtime.embedding.java.JolieValue from ) {
		return from != null ? new Builder( from ) : builder();
	}

	public static jolie.runtime.embedding.java.util.StructureListBuilder< LocatedSymbolRef, Builder > listBuilder() {
		return new jolie.runtime.embedding.java.util.StructureListBuilder<>( LocatedSymbolRef::builder );
	}

	public static jolie.runtime.embedding.java.util.StructureListBuilder< LocatedSymbolRef, Builder > listBuilder(
		java.util.SequencedCollection< ? extends jolie.runtime.embedding.java.JolieValue > from ) {
		return from != null
			? new jolie.runtime.embedding.java.util.StructureListBuilder<>( from, LocatedSymbolRef::from,
				LocatedSymbolRef::builder )
			: listBuilder();
	}

	public static LocatedSymbolRef from( jolie.runtime.embedding.java.JolieValue j )
		throws jolie.runtime.embedding.java.TypeValidationException {
		return new LocatedSymbolRef(
			jolie.runtime.embedding.java.JolieNative.JolieString.from( j ).value(),
			jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "textLocation" ),
				joliex.meta.spec.types.Location::from ) );
	}

	public static LocatedSymbolRef fromValue( jolie.runtime.Value v )
		throws jolie.runtime.typing.TypeCheckingException {
		jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
		return new LocatedSymbolRef(
			jolie.runtime.embedding.java.JolieNative.JolieString.contentFromValue( v ),
			jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "textLocation",
				joliex.meta.spec.types.Location::fromValue ) );
	}

	public static jolie.runtime.Value toValue( LocatedSymbolRef t ) {
		final jolie.runtime.Value v = jolie.runtime.Value.create( t.contentValue() );

		v.getFirstChild( "textLocation" ).deepCopy( joliex.meta.spec.types.Location.toValue( t.textLocation() ) );

		return v;
	}

	public static class Builder {

		private java.lang.String contentValue;
		private joliex.meta.spec.types.Location textLocation;

		private Builder() {}

		private Builder( jolie.runtime.embedding.java.JolieValue j ) {

			contentValue =
				j.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieString content ? content.value()
					: null;
			this.textLocation = jolie.runtime.embedding.java.util.ValueManager
				.fieldFrom( j.getFirstChild( "textLocation" ), joliex.meta.spec.types.Location::from );
		}

		public Builder contentValue( java.lang.String contentValue ) {
			this.contentValue = contentValue;
			return this;
		}

		public Builder textLocation( joliex.meta.spec.types.Location textLocation ) {
			this.textLocation = textLocation;
			return this;
		}

		public Builder textLocation(
			java.util.function.Function< joliex.meta.spec.types.Location.Builder, joliex.meta.spec.types.Location > f ) {
			return textLocation( f.apply( joliex.meta.spec.types.Location.builder() ) );
		}

		public LocatedSymbolRef build() {
			return new LocatedSymbolRef( contentValue, textLocation );
		}
	}
}
