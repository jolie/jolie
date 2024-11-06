package joliex.meta.spec.types;

/**
 * this class is a {@link jolie.runtime.embedding.java.TypedStructure} which can be described as
 * follows:
 *
 * <pre>
 * textLocation: {@link joliex.meta.spec.types.Location}
 * basicType: {@link joliex.meta.spec.types.BasicType}
 * rest[0,1]: {@link joliex.meta.spec.types.TreeNodeType}
 * nodes[0,2147483647]: {@link joliex.meta.spec.types.TreeNodeType}
 * documentation[0,1]: {@link joliex.meta.spec.types.Documentation}
 * </pre>
 *
 * @see jolie.runtime.embedding.java.JolieValue
 * @see jolie.runtime.embedding.java.JolieNative
 * @see joliex.meta.spec.types.Location
 * @see joliex.meta.spec.types.BasicType
 * @see joliex.meta.spec.types.TreeNodeType
 * @see joliex.meta.spec.types.Documentation
 * @see #builder()
 */
public final class TreeType extends jolie.runtime.embedding.java.TypedStructure {

	private static final java.util.Set< java.lang.String > FIELD_KEYS = fieldKeys( TreeType.class );

	@jolie.runtime.embedding.java.util.JolieName( "textLocation" )
	private final joliex.meta.spec.types.Location textLocation;
	@jolie.runtime.embedding.java.util.JolieName( "basicType" )
	private final joliex.meta.spec.types.BasicType basicType;
	@jolie.runtime.embedding.java.util.JolieName( "rest" )
	private final joliex.meta.spec.types.TreeNodeType rest;
	@jolie.runtime.embedding.java.util.JolieName( "nodes" )
	private final java.util.List< joliex.meta.spec.types.TreeNodeType > nodes;
	@jolie.runtime.embedding.java.util.JolieName( "documentation" )
	private final joliex.meta.spec.types.Documentation documentation;

	public TreeType( joliex.meta.spec.types.Location textLocation, joliex.meta.spec.types.BasicType basicType,
		joliex.meta.spec.types.TreeNodeType rest,
		java.util.SequencedCollection< joliex.meta.spec.types.TreeNodeType > nodes,
		joliex.meta.spec.types.Documentation documentation ) {
		this.textLocation = jolie.runtime.embedding.java.util.ValueManager.validated( "textLocation", textLocation );
		this.basicType = jolie.runtime.embedding.java.util.ValueManager.validated( "basicType", basicType );
		this.rest = rest;
		this.nodes = jolie.runtime.embedding.java.util.ValueManager.validated( "nodes", nodes, 0, 2147483647, t -> t );
		this.documentation = documentation;
	}

	public joliex.meta.spec.types.Location textLocation() {
		return textLocation;
	}

	public joliex.meta.spec.types.BasicType basicType() {
		return basicType;
	}

	public java.util.Optional< joliex.meta.spec.types.TreeNodeType > rest() {
		return java.util.Optional.ofNullable( rest );
	}

	public java.util.List< joliex.meta.spec.types.TreeNodeType > nodes() {
		return nodes;
	}

	public java.util.Optional< joliex.meta.spec.types.Documentation > documentation() {
		return java.util.Optional.ofNullable( documentation );
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

	public static jolie.runtime.embedding.java.util.StructureListBuilder< TreeType, Builder > listBuilder() {
		return new jolie.runtime.embedding.java.util.StructureListBuilder<>( TreeType::builder );
	}

	public static jolie.runtime.embedding.java.util.StructureListBuilder< TreeType, Builder > listBuilder(
		java.util.SequencedCollection< ? extends jolie.runtime.embedding.java.JolieValue > from ) {
		return from != null
			? new jolie.runtime.embedding.java.util.StructureListBuilder<>( from, TreeType::from, TreeType::builder )
			: listBuilder();
	}

	public static TreeType from( jolie.runtime.embedding.java.JolieValue j )
		throws jolie.runtime.embedding.java.TypeValidationException {
		return new TreeType(
			jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "textLocation" ),
				joliex.meta.spec.types.Location::from ),
			jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "basicType" ),
				joliex.meta.spec.types.BasicType::from ),
			jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "rest" ),
				joliex.meta.spec.types.TreeNodeType::from ),
			jolie.runtime.embedding.java.util.ValueManager.fieldFrom(
				j.getChildOrDefault( "nodes", java.util.List.of() ), joliex.meta.spec.types.TreeNodeType::from ),
			jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "documentation" ),
				joliex.meta.spec.types.Documentation::from ) );
	}

	public static TreeType fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
		jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
		return new TreeType(
			jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "textLocation",
				joliex.meta.spec.types.Location::fromValue ),
			jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "basicType",
				joliex.meta.spec.types.BasicType::fromValue ),
			jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "rest",
				joliex.meta.spec.types.TreeNodeType::fromValue ),
			jolie.runtime.embedding.java.util.ValueManager.vectorFieldFrom( v, "nodes",
				joliex.meta.spec.types.TreeNodeType::fromValue ),
			jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "documentation",
				joliex.meta.spec.types.Documentation::fromValue ) );
	}

	public static jolie.runtime.Value toValue( TreeType t ) {
		final jolie.runtime.Value v = jolie.runtime.Value.create();

		v.getFirstChild( "textLocation" ).deepCopy( joliex.meta.spec.types.Location.toValue( t.textLocation() ) );
		v.getFirstChild( "basicType" ).deepCopy( joliex.meta.spec.types.BasicType.toValue( t.basicType() ) );
		t.rest()
			.ifPresent( c -> v.getFirstChild( "rest" ).deepCopy( joliex.meta.spec.types.TreeNodeType.toValue( c ) ) );
		t.nodes().forEach( c -> v.getNewChild( "nodes" ).deepCopy( joliex.meta.spec.types.TreeNodeType.toValue( c ) ) );
		t.documentation().ifPresent(
			c -> v.getFirstChild( "documentation" ).deepCopy( joliex.meta.spec.types.Documentation.toValue( c ) ) );

		return v;
	}

	public static class Builder {

		private joliex.meta.spec.types.Location textLocation;
		private joliex.meta.spec.types.BasicType basicType;
		private joliex.meta.spec.types.TreeNodeType rest;
		private java.util.SequencedCollection< joliex.meta.spec.types.TreeNodeType > nodes;
		private joliex.meta.spec.types.Documentation documentation;

		private Builder() {}

		private Builder( jolie.runtime.embedding.java.JolieValue j ) {
			this.textLocation = jolie.runtime.embedding.java.util.ValueManager
				.fieldFrom( j.getFirstChild( "textLocation" ), joliex.meta.spec.types.Location::from );
			this.basicType = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "basicType" ),
				joliex.meta.spec.types.BasicType::from );
			this.rest = jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "rest" ),
				joliex.meta.spec.types.TreeNodeType::from );
			this.nodes = jolie.runtime.embedding.java.util.ValueManager.fieldFrom(
				j.getChildOrDefault( "nodes", java.util.List.of() ), joliex.meta.spec.types.TreeNodeType::from );
			this.documentation = jolie.runtime.embedding.java.util.ValueManager
				.fieldFrom( j.getFirstChild( "documentation" ), joliex.meta.spec.types.Documentation::from );
		}

		public Builder textLocation( joliex.meta.spec.types.Location textLocation ) {
			this.textLocation = textLocation;
			return this;
		}

		public Builder textLocation(
			java.util.function.Function< joliex.meta.spec.types.Location.Builder, joliex.meta.spec.types.Location > f ) {
			return textLocation( f.apply( joliex.meta.spec.types.Location.builder() ) );
		}

		public Builder basicType( joliex.meta.spec.types.BasicType basicType ) {
			this.basicType = basicType;
			return this;
		}

		public Builder rest( joliex.meta.spec.types.TreeNodeType rest ) {
			this.rest = rest;
			return this;
		}

		public Builder rest(
			java.util.function.Function< joliex.meta.spec.types.TreeNodeType.Builder, joliex.meta.spec.types.TreeNodeType > f ) {
			return rest( f.apply( joliex.meta.spec.types.TreeNodeType.builder() ) );
		}

		public Builder nodes( java.util.SequencedCollection< joliex.meta.spec.types.TreeNodeType > nodes ) {
			this.nodes = nodes;
			return this;
		}

		public Builder nodes(
			java.util.function.Function< jolie.runtime.embedding.java.util.StructureListBuilder< joliex.meta.spec.types.TreeNodeType, joliex.meta.spec.types.TreeNodeType.Builder >, java.util.List< joliex.meta.spec.types.TreeNodeType > > f ) {
			return nodes( f.apply( new jolie.runtime.embedding.java.util.StructureListBuilder<>(
				joliex.meta.spec.types.TreeNodeType::builder ) ) );
		}

		public Builder documentation( joliex.meta.spec.types.Documentation documentation ) {
			this.documentation = documentation;
			return this;
		}

		public Builder documentation(
			java.util.function.Function< joliex.meta.spec.types.Documentation.Builder, joliex.meta.spec.types.Documentation > f ) {
			return documentation( f.apply( joliex.meta.spec.types.Documentation.builder() ) );
		}

		public TreeType build() {
			return new TreeType( textLocation, basicType, rest, nodes, documentation );
		}
	}
}
