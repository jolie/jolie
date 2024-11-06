package joliex.meta.spec.types;

/**
 * this class is a {@link jolie.runtime.embedding.java.TypedStructure} which can be described as
 * follows:
 *
 * <pre>
 * textLocation: {@link joliex.meta.spec.types.Location}
 * name: {@link joliex.meta.spec.types.LocatedString}
 * type: {@link joliex.meta.spec.types.Type}
 * </pre>
 *
 * @see jolie.runtime.embedding.java.JolieValue
 * @see jolie.runtime.embedding.java.JolieNative
 * @see joliex.meta.spec.types.Location
 * @see joliex.meta.spec.types.LocatedString
 * @see joliex.meta.spec.types.Type
 * @see #builder()
 */
public final class TypeDef extends jolie.runtime.embedding.java.TypedStructure {

	private static final java.util.Set< java.lang.String > FIELD_KEYS = fieldKeys( TypeDef.class );

	@jolie.runtime.embedding.java.util.JolieName( "textLocation" )
	private final joliex.meta.spec.types.Location textLocation;
	@jolie.runtime.embedding.java.util.JolieName( "name" )
	private final joliex.meta.spec.types.LocatedString name;
	@jolie.runtime.embedding.java.util.JolieName( "type" )
	private final joliex.meta.spec.types.Type type;

	public TypeDef( joliex.meta.spec.types.Location textLocation, joliex.meta.spec.types.LocatedString name,
		joliex.meta.spec.types.Type type ) {
		this.textLocation = jolie.runtime.embedding.java.util.ValueManager.validated( "textLocation", textLocation );
		this.name = jolie.runtime.embedding.java.util.ValueManager.validated( "name", name );
		this.type = jolie.runtime.embedding.java.util.ValueManager.validated( "type", type );
	}

	public joliex.meta.spec.types.Location textLocation() {
		return textLocation;
	}

	public joliex.meta.spec.types.LocatedString name() {
		return name;
	}

	public joliex.meta.spec.types.Type type() {
		return type;
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

	public static jolie.runtime.embedding.java.util.StructureListBuilder< TypeDef, Builder > listBuilder() {
		return new jolie.runtime.embedding.java.util.StructureListBuilder<>( TypeDef::builder );
	}

	public static jolie.runtime.embedding.java.util.StructureListBuilder< TypeDef, Builder > listBuilder(
		java.util.SequencedCollection< ? extends jolie.runtime.embedding.java.JolieValue > from ) {
		return from != null
			? new jolie.runtime.embedding.java.util.StructureListBuilder<>( from, TypeDef::from, TypeDef::builder )
			: listBuilder();
	}

	public static TypeDef from( jolie.runtime.embedding.java.JolieValue j )
		throws jolie.runtime.embedding.java.TypeValidationException {
		return new TypeDef(
			jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "textLocation" ),
				joliex.meta.spec.types.Location::from ),
			jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "name" ),
				joliex.meta.spec.types.LocatedString::from ),
			jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "type" ),
				joliex.meta.spec.types.Type::from ) );
	}

	public static TypeDef fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
		jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
		return new TypeDef(
			jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "textLocation",
				joliex.meta.spec.types.Location::fromValue ),
			jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "name",
				joliex.meta.spec.types.LocatedString::fromValue ),
			jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "type",
				joliex.meta.spec.types.Type::fromValue ) );
	}

	public static jolie.runtime.Value toValue( TypeDef t ) {
		final jolie.runtime.Value v = jolie.runtime.Value.create();

		v.getFirstChild( "textLocation" ).deepCopy( joliex.meta.spec.types.Location.toValue( t.textLocation() ) );
		v.getFirstChild( "name" ).deepCopy( joliex.meta.spec.types.LocatedString.toValue( t.name() ) );
		v.getFirstChild( "type" ).deepCopy( joliex.meta.spec.types.Type.toValue( t.type() ) );

		return v;
	}

	public static class Builder {

		private joliex.meta.spec.types.Location textLocation;
		private joliex.meta.spec.types.LocatedString name;
		private joliex.meta.spec.types.Type type;

		private Builder() {}

		private Builder( jolie.runtime.embedding.java.JolieValue j ) {
			this.textLocation = jolie.runtime.embedding.java.util.ValueManager
				.fieldFrom( j.getFirstChild( "textLocation" ), joliex.meta.spec.types.Location::from );
			this.name = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "name" ),
				joliex.meta.spec.types.LocatedString::from );
			this.type = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "type" ),
				joliex.meta.spec.types.Type::from );
		}

		public Builder textLocation( joliex.meta.spec.types.Location textLocation ) {
			this.textLocation = textLocation;
			return this;
		}

		public Builder textLocation(
			java.util.function.Function< joliex.meta.spec.types.Location.Builder, joliex.meta.spec.types.Location > f ) {
			return textLocation( f.apply( joliex.meta.spec.types.Location.builder() ) );
		}

		public Builder name( joliex.meta.spec.types.LocatedString name ) {
			this.name = name;
			return this;
		}

		public Builder name(
			java.util.function.Function< joliex.meta.spec.types.LocatedString.Builder, joliex.meta.spec.types.LocatedString > f ) {
			return name( f.apply( joliex.meta.spec.types.LocatedString.builder() ) );
		}

		public Builder type( joliex.meta.spec.types.Type type ) {
			this.type = type;
			return this;
		}

		public TypeDef build() {
			return new TypeDef( textLocation, name, type );
		}
	}
}
