package joliex.meta.spec.types;

/**
 * this class is a {@link jolie.runtime.embedding.java.TypedStructure} which can be described as
 * follows:
 *
 * <pre>
 * textLocation: {@link joliex.meta.spec.types.Location}
 * requestType: {@link joliex.meta.spec.types.Type}
 * name: {@link joliex.meta.spec.types.LocatedString}
 * </pre>
 *
 * @see jolie.runtime.embedding.java.JolieValue
 * @see jolie.runtime.embedding.java.JolieNative
 * @see joliex.meta.spec.types.Location
 * @see joliex.meta.spec.types.Type
 * @see joliex.meta.spec.types.LocatedString
 * @see #builder()
 */
public final class OneWayOperation extends jolie.runtime.embedding.java.TypedStructure {

	private static final java.util.Set< java.lang.String > FIELD_KEYS = fieldKeys( OneWayOperation.class );

	@jolie.runtime.embedding.java.util.JolieName( "textLocation" )
	private final joliex.meta.spec.types.Location textLocation;
	@jolie.runtime.embedding.java.util.JolieName( "requestType" )
	private final joliex.meta.spec.types.Type requestType;
	@jolie.runtime.embedding.java.util.JolieName( "name" )
	private final joliex.meta.spec.types.LocatedString name;

	public OneWayOperation( joliex.meta.spec.types.Location textLocation, joliex.meta.spec.types.Type requestType,
		joliex.meta.spec.types.LocatedString name ) {
		this.textLocation = jolie.runtime.embedding.java.util.ValueManager.validated( "textLocation", textLocation );
		this.requestType = jolie.runtime.embedding.java.util.ValueManager.validated( "requestType", requestType );
		this.name = jolie.runtime.embedding.java.util.ValueManager.validated( "name", name );
	}

	public joliex.meta.spec.types.Location textLocation() {
		return textLocation;
	}

	public joliex.meta.spec.types.Type requestType() {
		return requestType;
	}

	public joliex.meta.spec.types.LocatedString name() {
		return name;
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

	public static jolie.runtime.embedding.java.util.StructureListBuilder< OneWayOperation, Builder > listBuilder() {
		return new jolie.runtime.embedding.java.util.StructureListBuilder<>( OneWayOperation::builder );
	}

	public static jolie.runtime.embedding.java.util.StructureListBuilder< OneWayOperation, Builder > listBuilder(
		java.util.SequencedCollection< ? extends jolie.runtime.embedding.java.JolieValue > from ) {
		return from != null
			? new jolie.runtime.embedding.java.util.StructureListBuilder<>( from, OneWayOperation::from,
				OneWayOperation::builder )
			: listBuilder();
	}

	public static OneWayOperation from( jolie.runtime.embedding.java.JolieValue j )
		throws jolie.runtime.embedding.java.TypeValidationException {
		return new OneWayOperation(
			jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "textLocation" ),
				joliex.meta.spec.types.Location::from ),
			jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "requestType" ),
				joliex.meta.spec.types.Type::from ),
			jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "name" ),
				joliex.meta.spec.types.LocatedString::from ) );
	}

	public static OneWayOperation fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
		jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
		return new OneWayOperation(
			jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "textLocation",
				joliex.meta.spec.types.Location::fromValue ),
			jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "requestType",
				joliex.meta.spec.types.Type::fromValue ),
			jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "name",
				joliex.meta.spec.types.LocatedString::fromValue ) );
	}

	public static jolie.runtime.Value toValue( OneWayOperation t ) {
		final jolie.runtime.Value v = jolie.runtime.Value.create();

		v.getFirstChild( "textLocation" ).deepCopy( joliex.meta.spec.types.Location.toValue( t.textLocation() ) );
		v.getFirstChild( "requestType" ).deepCopy( joliex.meta.spec.types.Type.toValue( t.requestType() ) );
		v.getFirstChild( "name" ).deepCopy( joliex.meta.spec.types.LocatedString.toValue( t.name() ) );

		return v;
	}

	public static class Builder {

		private joliex.meta.spec.types.Location textLocation;
		private joliex.meta.spec.types.Type requestType;
		private joliex.meta.spec.types.LocatedString name;

		private Builder() {}

		private Builder( jolie.runtime.embedding.java.JolieValue j ) {
			this.textLocation = jolie.runtime.embedding.java.util.ValueManager
				.fieldFrom( j.getFirstChild( "textLocation" ), joliex.meta.spec.types.Location::from );
			this.requestType = jolie.runtime.embedding.java.util.ValueManager
				.fieldFrom( j.getFirstChild( "requestType" ), joliex.meta.spec.types.Type::from );
			this.name = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "name" ),
				joliex.meta.spec.types.LocatedString::from );
		}

		public Builder textLocation( joliex.meta.spec.types.Location textLocation ) {
			this.textLocation = textLocation;
			return this;
		}

		public Builder textLocation(
			java.util.function.Function< joliex.meta.spec.types.Location.Builder, joliex.meta.spec.types.Location > f ) {
			return textLocation( f.apply( joliex.meta.spec.types.Location.builder() ) );
		}

		public Builder requestType( joliex.meta.spec.types.Type requestType ) {
			this.requestType = requestType;
			return this;
		}

		public Builder name( joliex.meta.spec.types.LocatedString name ) {
			this.name = name;
			return this;
		}

		public Builder name(
			java.util.function.Function< joliex.meta.spec.types.LocatedString.Builder, joliex.meta.spec.types.LocatedString > f ) {
			return name( f.apply( joliex.meta.spec.types.LocatedString.builder() ) );
		}

		public OneWayOperation build() {
			return new OneWayOperation( textLocation, requestType, name );
		}
	}
}
