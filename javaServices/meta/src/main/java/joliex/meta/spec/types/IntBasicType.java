package joliex.meta.spec.types;

/**
 * this class is a {@link jolie.runtime.embedding.java.TypedStructure} which can be described as
 * follows:
 *
 * <pre>
 * intTag("int"): {@link jolie.runtime.embedding.java.JolieNative.JolieVoid}
 * refinements[0,2147483647]: {@link joliex.meta.spec.types.IntRefinement}
 * </pre>
 *
 * @see jolie.runtime.embedding.java.JolieValue
 * @see jolie.runtime.embedding.java.JolieNative
 * @see joliex.meta.spec.types.IntRefinement
 * @see #builder()
 */
public final class IntBasicType extends jolie.runtime.embedding.java.TypedStructure {

	private static final java.util.Set< java.lang.String > FIELD_KEYS = fieldKeys( IntBasicType.class );

	@jolie.runtime.embedding.java.util.JolieName( "int" )
	private final jolie.runtime.embedding.java.JolieNative.JolieVoid intTag;
	@jolie.runtime.embedding.java.util.JolieName( "refinements" )
	private final java.util.List< joliex.meta.spec.types.IntRefinement > refinements;

	public IntBasicType( jolie.runtime.embedding.java.JolieNative.JolieVoid intTag,
		java.util.SequencedCollection< joliex.meta.spec.types.IntRefinement > refinements ) {
		this.intTag = jolie.runtime.embedding.java.util.ValueManager.validated( "intTag", intTag );
		this.refinements = jolie.runtime.embedding.java.util.ValueManager.validated( "refinements", refinements, 0,
			2147483647, t -> t );
	}

	public jolie.runtime.embedding.java.JolieNative.JolieVoid intTag() {
		return intTag;
	}

	public java.util.List< joliex.meta.spec.types.IntRefinement > refinements() {
		return refinements;
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

	public static jolie.runtime.embedding.java.util.StructureListBuilder< IntBasicType, Builder > listBuilder() {
		return new jolie.runtime.embedding.java.util.StructureListBuilder<>( IntBasicType::builder );
	}

	public static jolie.runtime.embedding.java.util.StructureListBuilder< IntBasicType, Builder > listBuilder(
		java.util.SequencedCollection< ? extends jolie.runtime.embedding.java.JolieValue > from ) {
		return from != null
			? new jolie.runtime.embedding.java.util.StructureListBuilder<>( from, IntBasicType::from,
				IntBasicType::builder )
			: listBuilder();
	}

	public static IntBasicType from( jolie.runtime.embedding.java.JolieValue j )
		throws jolie.runtime.embedding.java.TypeValidationException {
		return new IntBasicType(
			jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "int" ),
				c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieVoid content ? content
					: null ),
			jolie.runtime.embedding.java.util.ValueManager.fieldFrom(
				j.getChildOrDefault( "refinements", java.util.List.of() ),
				joliex.meta.spec.types.IntRefinement::from ) );
	}

	public static IntBasicType fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
		jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
		return new IntBasicType(
			jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "int",
				jolie.runtime.embedding.java.JolieNative.JolieVoid::fromValue ),
			jolie.runtime.embedding.java.util.ValueManager.vectorFieldFrom( v, "refinements",
				joliex.meta.spec.types.IntRefinement::fromValue ) );
	}

	public static jolie.runtime.Value toValue( IntBasicType t ) {
		final jolie.runtime.Value v = jolie.runtime.Value.create();

		v.getFirstChild( "int" ).setValue( t.intTag().value() );
		t.refinements().forEach(
			c -> v.getNewChild( "refinements" ).deepCopy( joliex.meta.spec.types.IntRefinement.toValue( c ) ) );

		return v;
	}

	public static class Builder {

		private jolie.runtime.embedding.java.JolieNative.JolieVoid intTag;
		private java.util.SequencedCollection< joliex.meta.spec.types.IntRefinement > refinements;

		private Builder() {}

		private Builder( jolie.runtime.embedding.java.JolieValue j ) {
			this.intTag = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "int" ),
				c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieVoid content ? content
					: null );
			this.refinements = jolie.runtime.embedding.java.util.ValueManager.fieldFrom(
				j.getChildOrDefault( "refinements", java.util.List.of() ), joliex.meta.spec.types.IntRefinement::from );
		}

		public Builder intTag( jolie.runtime.embedding.java.JolieNative.JolieVoid intTag ) {
			this.intTag = intTag;
			return this;
		}

		public Builder refinements(
			java.util.SequencedCollection< joliex.meta.spec.types.IntRefinement > refinements ) {
			this.refinements = refinements;
			return this;
		}

		public Builder refinements(
			java.util.function.Function< jolie.runtime.embedding.java.util.StructureListBuilder< joliex.meta.spec.types.IntRefinement, joliex.meta.spec.types.IntRefinement.Builder >, java.util.List< joliex.meta.spec.types.IntRefinement > > f ) {
			return refinements( f.apply( new jolie.runtime.embedding.java.util.StructureListBuilder<>(
				joliex.meta.spec.types.IntRefinement::builder ) ) );
		}

		public IntBasicType build() {
			return new IntBasicType( intTag, refinements );
		}
	}
}
