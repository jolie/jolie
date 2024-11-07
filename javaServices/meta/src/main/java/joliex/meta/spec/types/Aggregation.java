package joliex.meta.spec.types;

/**
 * this class is a {@link jolie.runtime.embedding.java.TypedStructure} which can be described as
 * follows:
 *
 * <pre>
 * textLocation: {@link joliex.meta.spec.types.Location}
 * extender[0,2147483647]: {@link joliex.meta.spec.types.LocatedSymbolRef}
 * outputPort: {@link joliex.meta.spec.types.LocatedSymbolRef}
 * </pre>
 *
 * @see jolie.runtime.embedding.java.JolieValue
 * @see jolie.runtime.embedding.java.JolieNative
 * @see joliex.meta.spec.types.Location
 * @see joliex.meta.spec.types.LocatedSymbolRef
 * @see #builder()
 */
public final class Aggregation extends jolie.runtime.embedding.java.TypedStructure {

	private static final java.util.Set< java.lang.String > FIELD_KEYS = fieldKeys( Aggregation.class );

	@jolie.runtime.embedding.java.util.JolieName( "textLocation" )
	private final joliex.meta.spec.types.Location textLocation;
	@jolie.runtime.embedding.java.util.JolieName( "extender" )
	private final java.util.List< joliex.meta.spec.types.LocatedSymbolRef > extender;
	@jolie.runtime.embedding.java.util.JolieName( "outputPort" )
	private final joliex.meta.spec.types.LocatedSymbolRef outputPort;

	public Aggregation( joliex.meta.spec.types.Location textLocation,
		java.util.SequencedCollection< joliex.meta.spec.types.LocatedSymbolRef > extender,
		joliex.meta.spec.types.LocatedSymbolRef outputPort ) {
		this.textLocation = jolie.runtime.embedding.java.util.ValueManager.validated( "textLocation", textLocation );
		this.extender =
			jolie.runtime.embedding.java.util.ValueManager.validated( "extender", extender, 0, 2147483647, t -> t );
		this.outputPort = jolie.runtime.embedding.java.util.ValueManager.validated( "outputPort", outputPort );
	}

	public joliex.meta.spec.types.Location textLocation() {
		return textLocation;
	}

	public java.util.List< joliex.meta.spec.types.LocatedSymbolRef > extender() {
		return extender;
	}

	public joliex.meta.spec.types.LocatedSymbolRef outputPort() {
		return outputPort;
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

	public static jolie.runtime.embedding.java.util.StructureListBuilder< Aggregation, Builder > listBuilder() {
		return new jolie.runtime.embedding.java.util.StructureListBuilder<>( Aggregation::builder );
	}

	public static jolie.runtime.embedding.java.util.StructureListBuilder< Aggregation, Builder > listBuilder(
		java.util.SequencedCollection< ? extends jolie.runtime.embedding.java.JolieValue > from ) {
		return from != null
			? new jolie.runtime.embedding.java.util.StructureListBuilder<>( from, Aggregation::from,
				Aggregation::builder )
			: listBuilder();
	}

	public static Aggregation from( jolie.runtime.embedding.java.JolieValue j )
		throws jolie.runtime.embedding.java.TypeValidationException {
		return new Aggregation(
			jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "textLocation" ),
				joliex.meta.spec.types.Location::from ),
			jolie.runtime.embedding.java.util.ValueManager.fieldFrom(
				j.getChildOrDefault( "extender", java.util.List.of() ), joliex.meta.spec.types.LocatedSymbolRef::from ),
			jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "outputPort" ),
				joliex.meta.spec.types.LocatedSymbolRef::from ) );
	}

	public static Aggregation fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
		jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
		return new Aggregation(
			jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "textLocation",
				joliex.meta.spec.types.Location::fromValue ),
			jolie.runtime.embedding.java.util.ValueManager.vectorFieldFrom( v, "extender",
				joliex.meta.spec.types.LocatedSymbolRef::fromValue ),
			jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "outputPort",
				joliex.meta.spec.types.LocatedSymbolRef::fromValue ) );
	}

	public static jolie.runtime.Value toValue( Aggregation t ) {
		final jolie.runtime.Value v = jolie.runtime.Value.create();

		v.getFirstChild( "textLocation" ).deepCopy( joliex.meta.spec.types.Location.toValue( t.textLocation() ) );
		t.extender().forEach(
			c -> v.getNewChild( "extender" ).deepCopy( joliex.meta.spec.types.LocatedSymbolRef.toValue( c ) ) );
		v.getFirstChild( "outputPort" ).deepCopy( joliex.meta.spec.types.LocatedSymbolRef.toValue( t.outputPort() ) );

		return v;
	}

	public static class Builder {

		private joliex.meta.spec.types.Location textLocation;
		private java.util.SequencedCollection< joliex.meta.spec.types.LocatedSymbolRef > extender;
		private joliex.meta.spec.types.LocatedSymbolRef outputPort;

		private Builder() {}

		private Builder( jolie.runtime.embedding.java.JolieValue j ) {
			this.textLocation = jolie.runtime.embedding.java.util.ValueManager
				.fieldFrom( j.getFirstChild( "textLocation" ), joliex.meta.spec.types.Location::from );
			this.extender = jolie.runtime.embedding.java.util.ValueManager.fieldFrom(
				j.getChildOrDefault( "extender", java.util.List.of() ), joliex.meta.spec.types.LocatedSymbolRef::from );
			this.outputPort = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "outputPort" ),
				joliex.meta.spec.types.LocatedSymbolRef::from );
		}

		public Builder textLocation( joliex.meta.spec.types.Location textLocation ) {
			this.textLocation = textLocation;
			return this;
		}

		public Builder textLocation(
			java.util.function.Function< joliex.meta.spec.types.Location.Builder, joliex.meta.spec.types.Location > f ) {
			return textLocation( f.apply( joliex.meta.spec.types.Location.builder() ) );
		}

		public Builder extender( java.util.SequencedCollection< joliex.meta.spec.types.LocatedSymbolRef > extender ) {
			this.extender = extender;
			return this;
		}

		public Builder extender(
			java.util.function.Function< jolie.runtime.embedding.java.util.StructureListBuilder< joliex.meta.spec.types.LocatedSymbolRef, joliex.meta.spec.types.LocatedSymbolRef.Builder >, java.util.List< joliex.meta.spec.types.LocatedSymbolRef > > f ) {
			return extender( f.apply( new jolie.runtime.embedding.java.util.StructureListBuilder<>(
				joliex.meta.spec.types.LocatedSymbolRef::builder ) ) );
		}

		public Builder outputPort( joliex.meta.spec.types.LocatedSymbolRef outputPort ) {
			this.outputPort = outputPort;
			return this;
		}

		public Builder outputPort(
			java.util.function.Function< joliex.meta.spec.types.LocatedSymbolRef.Builder, joliex.meta.spec.types.LocatedSymbolRef > f ) {
			return outputPort( f.apply( joliex.meta.spec.types.LocatedSymbolRef.builder() ) );
		}

		public Aggregation build() {
			return new Aggregation( textLocation, extender, outputPort );
		}
	}
}
