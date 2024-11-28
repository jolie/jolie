package joliex.meta.spec.types;

/**
 * this class is a {@link jolie.runtime.embedding.java.TypedStructure} which can be described as
 * follows:
 *
 * <pre>
 * textLocation: {@link joliex.meta.spec.types.Location}
 * redirections[0,2147483647]: {@link joliex.meta.spec.types.Redirection}
 * protocol[0,1]: {@link joliex.meta.spec.types.LocatedString}
 * interfaces[0,2147483647]: {@link java.lang.String}
 * operations[0,2147483647]: {@link joliex.meta.spec.types.Operation}
 * name: {@link joliex.meta.spec.types.LocatedString}
 * location[0,1]: {@link joliex.meta.spec.types.LocatedString}
 * aggregations[0,2147483647]: {@link joliex.meta.spec.types.Aggregation}
 * </pre>
 *
 * @see jolie.runtime.embedding.java.JolieValue
 * @see jolie.runtime.embedding.java.JolieNative
 * @see joliex.meta.spec.types.Location
 * @see joliex.meta.spec.types.Redirection
 * @see joliex.meta.spec.types.LocatedString
 * @see joliex.meta.spec.types.Operation
 * @see joliex.meta.spec.types.Aggregation
 * @see #builder()
 */
public final class InputPort extends jolie.runtime.embedding.java.TypedStructure {

	private static final java.util.Set< java.lang.String > FIELD_KEYS = fieldKeys( InputPort.class );

	@jolie.runtime.embedding.java.util.JolieName( "textLocation" )
	private final joliex.meta.spec.types.Location textLocation;
	@jolie.runtime.embedding.java.util.JolieName( "redirections" )
	private final java.util.List< joliex.meta.spec.types.Redirection > redirections;
	@jolie.runtime.embedding.java.util.JolieName( "protocol" )
	private final joliex.meta.spec.types.LocatedString protocol;
	@jolie.runtime.embedding.java.util.JolieName( "interfaces" )
	private final java.util.List< java.lang.String > interfaces;
	@jolie.runtime.embedding.java.util.JolieName( "operations" )
	private final java.util.List< joliex.meta.spec.types.Operation > operations;
	@jolie.runtime.embedding.java.util.JolieName( "name" )
	private final joliex.meta.spec.types.LocatedString name;
	@jolie.runtime.embedding.java.util.JolieName( "location" )
	private final joliex.meta.spec.types.LocatedString location;
	@jolie.runtime.embedding.java.util.JolieName( "aggregations" )
	private final java.util.List< joliex.meta.spec.types.Aggregation > aggregations;

	public InputPort( joliex.meta.spec.types.Location textLocation,
		java.util.SequencedCollection< joliex.meta.spec.types.Redirection > redirections,
		joliex.meta.spec.types.LocatedString protocol, java.util.SequencedCollection< java.lang.String > interfaces,
		java.util.SequencedCollection< joliex.meta.spec.types.Operation > operations,
		joliex.meta.spec.types.LocatedString name, joliex.meta.spec.types.LocatedString location,
		java.util.SequencedCollection< joliex.meta.spec.types.Aggregation > aggregations ) {
		this.textLocation = jolie.runtime.embedding.java.util.ValueManager.validated( "textLocation", textLocation );
		this.redirections = jolie.runtime.embedding.java.util.ValueManager.validated( "redirections", redirections, 0,
			2147483647, t -> t );
		this.protocol = protocol;
		this.interfaces =
			jolie.runtime.embedding.java.util.ValueManager.validated( "interfaces", interfaces, 0, 2147483647, t -> t );
		this.operations =
			jolie.runtime.embedding.java.util.ValueManager.validated( "operations", operations, 0, 2147483647, t -> t );
		this.name = jolie.runtime.embedding.java.util.ValueManager.validated( "name", name );
		this.location = location;
		this.aggregations = jolie.runtime.embedding.java.util.ValueManager.validated( "aggregations", aggregations, 0,
			2147483647, t -> t );
	}

	public joliex.meta.spec.types.Location textLocation() {
		return textLocation;
	}

	public java.util.List< joliex.meta.spec.types.Redirection > redirections() {
		return redirections;
	}

	public java.util.Optional< joliex.meta.spec.types.LocatedString > protocol() {
		return java.util.Optional.ofNullable( protocol );
	}

	public java.util.List< java.lang.String > interfaces() {
		return interfaces;
	}

	public java.util.List< joliex.meta.spec.types.Operation > operations() {
		return operations;
	}

	public joliex.meta.spec.types.LocatedString name() {
		return name;
	}

	public java.util.Optional< joliex.meta.spec.types.LocatedString > location() {
		return java.util.Optional.ofNullable( location );
	}

	public java.util.List< joliex.meta.spec.types.Aggregation > aggregations() {
		return aggregations;
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

	public static jolie.runtime.embedding.java.util.StructureListBuilder< InputPort, Builder > listBuilder() {
		return new jolie.runtime.embedding.java.util.StructureListBuilder<>( InputPort::builder );
	}

	public static jolie.runtime.embedding.java.util.StructureListBuilder< InputPort, Builder > listBuilder(
		java.util.SequencedCollection< ? extends jolie.runtime.embedding.java.JolieValue > from ) {
		return from != null
			? new jolie.runtime.embedding.java.util.StructureListBuilder<>( from, InputPort::from, InputPort::builder )
			: listBuilder();
	}

	public static InputPort from( jolie.runtime.embedding.java.JolieValue j )
		throws jolie.runtime.embedding.java.TypeValidationException {
		return new InputPort(
			jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "textLocation" ),
				joliex.meta.spec.types.Location::from ),
			jolie.runtime.embedding.java.util.ValueManager.fieldFrom(
				j.getChildOrDefault( "redirections", java.util.List.of() ), joliex.meta.spec.types.Redirection::from ),
			jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "protocol" ),
				joliex.meta.spec.types.LocatedString::from ),
			jolie.runtime.embedding.java.util.ValueManager.fieldFrom(
				j.getChildOrDefault( "interfaces", java.util.List.of() ),
				c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieString content
					? content.value()
					: null ),
			jolie.runtime.embedding.java.util.ValueManager.fieldFrom(
				j.getChildOrDefault( "operations", java.util.List.of() ), joliex.meta.spec.types.Operation::from ),
			jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "name" ),
				joliex.meta.spec.types.LocatedString::from ),
			jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "location" ),
				joliex.meta.spec.types.LocatedString::from ),
			jolie.runtime.embedding.java.util.ValueManager.fieldFrom(
				j.getChildOrDefault( "aggregations", java.util.List.of() ),
				joliex.meta.spec.types.Aggregation::from ) );
	}

	public static InputPort fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
		jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
		return new InputPort(
			jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "textLocation",
				joliex.meta.spec.types.Location::fromValue ),
			jolie.runtime.embedding.java.util.ValueManager.vectorFieldFrom( v, "redirections",
				joliex.meta.spec.types.Redirection::fromValue ),
			jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "protocol",
				joliex.meta.spec.types.LocatedString::fromValue ),
			jolie.runtime.embedding.java.util.ValueManager.vectorFieldFrom( v, "interfaces",
				jolie.runtime.embedding.java.JolieNative.JolieString::fieldFromValue ),
			jolie.runtime.embedding.java.util.ValueManager.vectorFieldFrom( v, "operations",
				joliex.meta.spec.types.Operation::fromValue ),
			jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "name",
				joliex.meta.spec.types.LocatedString::fromValue ),
			jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "location",
				joliex.meta.spec.types.LocatedString::fromValue ),
			jolie.runtime.embedding.java.util.ValueManager.vectorFieldFrom( v, "aggregations",
				joliex.meta.spec.types.Aggregation::fromValue ) );
	}

	public static jolie.runtime.Value toValue( InputPort t ) {
		final jolie.runtime.Value v = jolie.runtime.Value.create();

		v.getFirstChild( "textLocation" ).deepCopy( joliex.meta.spec.types.Location.toValue( t.textLocation() ) );
		t.redirections().forEach(
			c -> v.getNewChild( "redirections" ).deepCopy( joliex.meta.spec.types.Redirection.toValue( c ) ) );
		t.protocol().ifPresent(
			c -> v.getFirstChild( "protocol" ).deepCopy( joliex.meta.spec.types.LocatedString.toValue( c ) ) );
		t.interfaces().forEach( c -> v.getNewChild( "interfaces" ).setValue( c ) );
		t.operations()
			.forEach( c -> v.getNewChild( "operations" ).deepCopy( joliex.meta.spec.types.Operation.toValue( c ) ) );
		v.getFirstChild( "name" ).deepCopy( joliex.meta.spec.types.LocatedString.toValue( t.name() ) );
		t.location().ifPresent(
			c -> v.getFirstChild( "location" ).deepCopy( joliex.meta.spec.types.LocatedString.toValue( c ) ) );
		t.aggregations().forEach(
			c -> v.getNewChild( "aggregations" ).deepCopy( joliex.meta.spec.types.Aggregation.toValue( c ) ) );

		return v;
	}

	public static class Builder {

		private joliex.meta.spec.types.Location textLocation;
		private java.util.SequencedCollection< joliex.meta.spec.types.Redirection > redirections;
		private joliex.meta.spec.types.LocatedString protocol;
		private java.util.SequencedCollection< java.lang.String > interfaces;
		private java.util.SequencedCollection< joliex.meta.spec.types.Operation > operations;
		private joliex.meta.spec.types.LocatedString name;
		private joliex.meta.spec.types.LocatedString location;
		private java.util.SequencedCollection< joliex.meta.spec.types.Aggregation > aggregations;

		private Builder() {}

		private Builder( jolie.runtime.embedding.java.JolieValue j ) {
			this.textLocation = jolie.runtime.embedding.java.util.ValueManager
				.fieldFrom( j.getFirstChild( "textLocation" ), joliex.meta.spec.types.Location::from );
			this.redirections = jolie.runtime.embedding.java.util.ValueManager.fieldFrom(
				j.getChildOrDefault( "redirections", java.util.List.of() ), joliex.meta.spec.types.Redirection::from );
			this.protocol = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "protocol" ),
				joliex.meta.spec.types.LocatedString::from );
			this.interfaces = jolie.runtime.embedding.java.util.ValueManager.fieldFrom(
				j.getChildOrDefault( "interfaces", java.util.List.of() ),
				c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieString content
					? content.value()
					: null );
			this.operations = jolie.runtime.embedding.java.util.ValueManager.fieldFrom(
				j.getChildOrDefault( "operations", java.util.List.of() ), joliex.meta.spec.types.Operation::from );
			this.name = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "name" ),
				joliex.meta.spec.types.LocatedString::from );
			this.location = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "location" ),
				joliex.meta.spec.types.LocatedString::from );
			this.aggregations = jolie.runtime.embedding.java.util.ValueManager.fieldFrom(
				j.getChildOrDefault( "aggregations", java.util.List.of() ), joliex.meta.spec.types.Aggregation::from );
		}

		public Builder textLocation( joliex.meta.spec.types.Location textLocation ) {
			this.textLocation = textLocation;
			return this;
		}

		public Builder textLocation(
			java.util.function.Function< joliex.meta.spec.types.Location.Builder, joliex.meta.spec.types.Location > f ) {
			return textLocation( f.apply( joliex.meta.spec.types.Location.builder() ) );
		}

		public Builder redirections(
			java.util.SequencedCollection< joliex.meta.spec.types.Redirection > redirections ) {
			this.redirections = redirections;
			return this;
		}

		public Builder redirections(
			java.util.function.Function< jolie.runtime.embedding.java.util.StructureListBuilder< joliex.meta.spec.types.Redirection, joliex.meta.spec.types.Redirection.Builder >, java.util.List< joliex.meta.spec.types.Redirection > > f ) {
			return redirections( f.apply( new jolie.runtime.embedding.java.util.StructureListBuilder<>(
				joliex.meta.spec.types.Redirection::builder ) ) );
		}

		public Builder protocol( joliex.meta.spec.types.LocatedString protocol ) {
			this.protocol = protocol;
			return this;
		}

		public Builder protocol(
			java.util.function.Function< joliex.meta.spec.types.LocatedString.Builder, joliex.meta.spec.types.LocatedString > f ) {
			return protocol( f.apply( joliex.meta.spec.types.LocatedString.builder() ) );
		}

		public Builder interfaces( java.util.SequencedCollection< java.lang.String > interfaces ) {
			this.interfaces = interfaces;
			return this;
		}

		public Builder interfaces( java.lang.String... values ) {
			return interfaces( values == null ? null : java.util.List.of( values ) );
		}

		public Builder operations( java.util.SequencedCollection< joliex.meta.spec.types.Operation > operations ) {
			this.operations = operations;
			return this;
		}

		public Builder operations(
			java.util.function.Function< joliex.meta.spec.types.Operation.ListBuilder, java.util.List< joliex.meta.spec.types.Operation > > f ) {
			return operations( f.apply( joliex.meta.spec.types.Operation.listBuilder() ) );
		}

		public Builder name( joliex.meta.spec.types.LocatedString name ) {
			this.name = name;
			return this;
		}

		public Builder name(
			java.util.function.Function< joliex.meta.spec.types.LocatedString.Builder, joliex.meta.spec.types.LocatedString > f ) {
			return name( f.apply( joliex.meta.spec.types.LocatedString.builder() ) );
		}

		public Builder location( joliex.meta.spec.types.LocatedString location ) {
			this.location = location;
			return this;
		}

		public Builder location(
			java.util.function.Function< joliex.meta.spec.types.LocatedString.Builder, joliex.meta.spec.types.LocatedString > f ) {
			return location( f.apply( joliex.meta.spec.types.LocatedString.builder() ) );
		}

		public Builder aggregations(
			java.util.SequencedCollection< joliex.meta.spec.types.Aggregation > aggregations ) {
			this.aggregations = aggregations;
			return this;
		}

		public Builder aggregations(
			java.util.function.Function< jolie.runtime.embedding.java.util.StructureListBuilder< joliex.meta.spec.types.Aggregation, joliex.meta.spec.types.Aggregation.Builder >, java.util.List< joliex.meta.spec.types.Aggregation > > f ) {
			return aggregations( f.apply( new jolie.runtime.embedding.java.util.StructureListBuilder<>(
				joliex.meta.spec.types.Aggregation::builder ) ) );
		}

		public InputPort build() {
			return new InputPort( textLocation, redirections, protocol, interfaces, operations, name, location,
				aggregations );
		}
	}
}
