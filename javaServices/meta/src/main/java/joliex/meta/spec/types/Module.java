package joliex.meta.spec.types;

/**
 * this class is a {@link jolie.runtime.embedding.java.TypedStructure} which can be described as
 * follows:
 *
 * <pre>
 * types[0,2147483647]: {@link joliex.meta.spec.types.TypeDef}
 * interfaces[0,2147483647]: {@link joliex.meta.spec.types.InterfaceDef}
 * services[0,2147483647]: {@link joliex.meta.spec.types.ServiceDef}
 * </pre>
 *
 * @see jolie.runtime.embedding.java.JolieValue
 * @see jolie.runtime.embedding.java.JolieNative
 * @see joliex.meta.spec.types.TypeDef
 * @see joliex.meta.spec.types.InterfaceDef
 * @see joliex.meta.spec.types.ServiceDef
 * @see #builder()
 */
public final class Module extends jolie.runtime.embedding.java.TypedStructure {

	private static final java.util.Set< java.lang.String > FIELD_KEYS = fieldKeys( Module.class );

	@jolie.runtime.embedding.java.util.JolieName( "types" )
	private final java.util.List< joliex.meta.spec.types.TypeDef > types;
	@jolie.runtime.embedding.java.util.JolieName( "interfaces" )
	private final java.util.List< joliex.meta.spec.types.InterfaceDef > interfaces;
	@jolie.runtime.embedding.java.util.JolieName( "services" )
	private final java.util.List< joliex.meta.spec.types.ServiceDef > services;

	public Module( java.util.SequencedCollection< joliex.meta.spec.types.TypeDef > types,
		java.util.SequencedCollection< joliex.meta.spec.types.InterfaceDef > interfaces,
		java.util.SequencedCollection< joliex.meta.spec.types.ServiceDef > services ) {
		this.types = jolie.runtime.embedding.java.util.ValueManager.validated( "types", types, 0, 2147483647, t -> t );
		this.interfaces =
			jolie.runtime.embedding.java.util.ValueManager.validated( "interfaces", interfaces, 0, 2147483647, t -> t );
		this.services =
			jolie.runtime.embedding.java.util.ValueManager.validated( "services", services, 0, 2147483647, t -> t );
	}

	public java.util.List< joliex.meta.spec.types.TypeDef > types() {
		return types;
	}

	public java.util.List< joliex.meta.spec.types.InterfaceDef > interfaces() {
		return interfaces;
	}

	public java.util.List< joliex.meta.spec.types.ServiceDef > services() {
		return services;
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

	public static jolie.runtime.embedding.java.util.StructureListBuilder< Module, Builder > listBuilder() {
		return new jolie.runtime.embedding.java.util.StructureListBuilder<>( Module::builder );
	}

	public static jolie.runtime.embedding.java.util.StructureListBuilder< Module, Builder > listBuilder(
		java.util.SequencedCollection< ? extends jolie.runtime.embedding.java.JolieValue > from ) {
		return from != null
			? new jolie.runtime.embedding.java.util.StructureListBuilder<>( from, Module::from, Module::builder )
			: listBuilder();
	}

	public static Module from( jolie.runtime.embedding.java.JolieValue j )
		throws jolie.runtime.embedding.java.TypeValidationException {
		return new Module(
			jolie.runtime.embedding.java.util.ValueManager
				.fieldFrom( j.getChildOrDefault( "types", java.util.List.of() ), joliex.meta.spec.types.TypeDef::from ),
			jolie.runtime.embedding.java.util.ValueManager.fieldFrom(
				j.getChildOrDefault( "interfaces", java.util.List.of() ), joliex.meta.spec.types.InterfaceDef::from ),
			jolie.runtime.embedding.java.util.ValueManager.fieldFrom(
				j.getChildOrDefault( "services", java.util.List.of() ), joliex.meta.spec.types.ServiceDef::from ) );
	}

	public static Module fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
		jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
		return new Module(
			jolie.runtime.embedding.java.util.ValueManager.vectorFieldFrom( v, "types",
				joliex.meta.spec.types.TypeDef::fromValue ),
			jolie.runtime.embedding.java.util.ValueManager.vectorFieldFrom( v, "interfaces",
				joliex.meta.spec.types.InterfaceDef::fromValue ),
			jolie.runtime.embedding.java.util.ValueManager.vectorFieldFrom( v, "services",
				joliex.meta.spec.types.ServiceDef::fromValue ) );
	}

	public static jolie.runtime.Value toValue( Module t ) {
		final jolie.runtime.Value v = jolie.runtime.Value.create();

		t.types().forEach( c -> v.getNewChild( "types" ).deepCopy( joliex.meta.spec.types.TypeDef.toValue( c ) ) );
		t.interfaces()
			.forEach( c -> v.getNewChild( "interfaces" ).deepCopy( joliex.meta.spec.types.InterfaceDef.toValue( c ) ) );
		t.services()
			.forEach( c -> v.getNewChild( "services" ).deepCopy( joliex.meta.spec.types.ServiceDef.toValue( c ) ) );

		return v;
	}

	public static class Builder {

		private java.util.SequencedCollection< joliex.meta.spec.types.TypeDef > types;
		private java.util.SequencedCollection< joliex.meta.spec.types.InterfaceDef > interfaces;
		private java.util.SequencedCollection< joliex.meta.spec.types.ServiceDef > services;

		private Builder() {}

		private Builder( jolie.runtime.embedding.java.JolieValue j ) {
			this.types = jolie.runtime.embedding.java.util.ValueManager
				.fieldFrom( j.getChildOrDefault( "types", java.util.List.of() ), joliex.meta.spec.types.TypeDef::from );
			this.interfaces = jolie.runtime.embedding.java.util.ValueManager.fieldFrom(
				j.getChildOrDefault( "interfaces", java.util.List.of() ), joliex.meta.spec.types.InterfaceDef::from );
			this.services = jolie.runtime.embedding.java.util.ValueManager.fieldFrom(
				j.getChildOrDefault( "services", java.util.List.of() ), joliex.meta.spec.types.ServiceDef::from );
		}

		public Builder types( java.util.SequencedCollection< joliex.meta.spec.types.TypeDef > types ) {
			this.types = types;
			return this;
		}

		public Builder types(
			java.util.function.Function< jolie.runtime.embedding.java.util.StructureListBuilder< joliex.meta.spec.types.TypeDef, joliex.meta.spec.types.TypeDef.Builder >, java.util.List< joliex.meta.spec.types.TypeDef > > f ) {
			return types( f.apply( new jolie.runtime.embedding.java.util.StructureListBuilder<>(
				joliex.meta.spec.types.TypeDef::builder ) ) );
		}

		public Builder interfaces( java.util.SequencedCollection< joliex.meta.spec.types.InterfaceDef > interfaces ) {
			this.interfaces = interfaces;
			return this;
		}

		public Builder interfaces(
			java.util.function.Function< jolie.runtime.embedding.java.util.StructureListBuilder< joliex.meta.spec.types.InterfaceDef, joliex.meta.spec.types.InterfaceDef.Builder >, java.util.List< joliex.meta.spec.types.InterfaceDef > > f ) {
			return interfaces( f.apply( new jolie.runtime.embedding.java.util.StructureListBuilder<>(
				joliex.meta.spec.types.InterfaceDef::builder ) ) );
		}

		public Builder services( java.util.SequencedCollection< joliex.meta.spec.types.ServiceDef > services ) {
			this.services = services;
			return this;
		}

		public Builder services(
			java.util.function.Function< jolie.runtime.embedding.java.util.StructureListBuilder< joliex.meta.spec.types.ServiceDef, joliex.meta.spec.types.ServiceDef.Builder >, java.util.List< joliex.meta.spec.types.ServiceDef > > f ) {
			return services( f.apply( new jolie.runtime.embedding.java.util.StructureListBuilder<>(
				joliex.meta.spec.types.ServiceDef::builder ) ) );
		}

		public Module build() {
			return new Module( types, interfaces, services );
		}
	}
}
