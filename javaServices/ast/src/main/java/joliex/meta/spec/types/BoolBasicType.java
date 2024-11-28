package joliex.meta.spec.types;

/**
 * this class is a {@link jolie.runtime.embedding.java.TypedStructure} which can be described as
 * follows:
 *
 * <pre>
 * boolTag("bool"): {@link jolie.runtime.embedding.java.JolieNative.JolieVoid}
 * </pre>
 *
 * @see jolie.runtime.embedding.java.JolieValue
 * @see jolie.runtime.embedding.java.JolieNative
 * @see #builder()
 */
public final class BoolBasicType extends jolie.runtime.embedding.java.TypedStructure {

	private static final java.util.Set< java.lang.String > FIELD_KEYS = fieldKeys( BoolBasicType.class );

	@jolie.runtime.embedding.java.util.JolieName( "bool" )
	private final jolie.runtime.embedding.java.JolieNative.JolieVoid boolTag;

	public BoolBasicType( jolie.runtime.embedding.java.JolieNative.JolieVoid boolTag ) {
		this.boolTag = jolie.runtime.embedding.java.util.ValueManager.validated( "boolTag", boolTag );
	}

	public jolie.runtime.embedding.java.JolieNative.JolieVoid boolTag() {
		return boolTag;
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

	public static jolie.runtime.embedding.java.util.StructureListBuilder< BoolBasicType, Builder > listBuilder() {
		return new jolie.runtime.embedding.java.util.StructureListBuilder<>( BoolBasicType::builder );
	}

	public static jolie.runtime.embedding.java.util.StructureListBuilder< BoolBasicType, Builder > listBuilder(
		java.util.SequencedCollection< ? extends jolie.runtime.embedding.java.JolieValue > from ) {
		return from != null
			? new jolie.runtime.embedding.java.util.StructureListBuilder<>( from, BoolBasicType::from,
				BoolBasicType::builder )
			: listBuilder();
	}

	public static BoolBasicType from( jolie.runtime.embedding.java.JolieValue j )
		throws jolie.runtime.embedding.java.TypeValidationException {
		return new BoolBasicType(
			jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "bool" ),
				c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieVoid content ? content
					: null ) );
	}

	public static BoolBasicType fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
		jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
		return new BoolBasicType(
			jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "bool",
				jolie.runtime.embedding.java.JolieNative.JolieVoid::fromValue ) );
	}

	public static jolie.runtime.Value toValue( BoolBasicType t ) {
		final jolie.runtime.Value v = jolie.runtime.Value.create();

		v.getFirstChild( "bool" ).setValue( t.boolTag().value() );

		return v;
	}

	public static class Builder {

		private jolie.runtime.embedding.java.JolieNative.JolieVoid boolTag;

		private Builder() {}

		private Builder( jolie.runtime.embedding.java.JolieValue j ) {
			this.boolTag = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "bool" ),
				c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieVoid content ? content
					: null );
		}

		public Builder boolTag( jolie.runtime.embedding.java.JolieNative.JolieVoid boolTag ) {
			this.boolTag = boolTag;
			return this;
		}

		public BoolBasicType build() {
			return new BoolBasicType( boolTag );
		}
	}
}
