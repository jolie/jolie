package joliex.java.generate.type;

import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;
import joliex.java.generate.JavaClassDirector;
import joliex.java.generate.util.ClassPath;
import joliex.java.parse.ast.NativeRefinement;
import joliex.java.parse.ast.JolieType.Definition;
import joliex.java.parse.ast.JolieType.Native;
import joliex.java.parse.ast.JolieType.Definition.Basic;
import joliex.java.parse.ast.JolieType.Definition.Choice;
import joliex.java.parse.ast.JolieType.Definition.Structure;
import joliex.java.parse.ast.JolieType.Definition.Structure.Undefined;
import one.util.streamex.StreamEx;

public class TypedStructureClassBuilder extends StructureClassBuilder {

	private final List< Structure.Field > recordFields;

	public TypedStructureClassBuilder( Structure.Inline.Typed structure, String typesPackage ) {
		super( structure, typesPackage );
		this.recordFields = structure.contentType() == Native.VOID
			? structure.fields()
			: StreamEx.of( structure.fields() )
				.prepend( new Structure.Field( null,
					structure.contentType() == Native.ANY ? "content" : "contentValue",
					1, 1,
					structure.nativeRefinement() == null
						? structure.contentType()
						: new Basic.Inline( null, structure.contentType(), structure.nativeRefinement() ) ) )
				.toList();
	}

	@Override
	protected void appendDescriptionDocumentation() {
		builder.newlineAppend( "this class is a {@link " ).append( ClassPath.TYPEDSTRUCTURE )
			.append( "} which can be described as follows:" );
	}

	@Override
	protected void appendDefinitionDocumentation() {
		if( structure.contentType() != Native.VOID )
			builder.newNewlineAppend( structure.contentType() == Native.ANY ? "content" : "contentValue" )
				.append( ": {@link " ).append( structure.contentType().nativeClass() ).append( "}" )
				.append( structure.possibleRefinement().map( r -> "( " + r.definitionString() + " )" ).orElse( "" ) )
				.indented( this::appendFieldDocumentation );
		else
			appendFieldDocumentation();
	}

	private void appendFieldDocumentation() {
		structure.fields()
			.forEach( field -> builder.newlineAppend( field.javaName() )
				.append( field.jolieName().equals( field.javaName() ) ? "" : "(\"" + field.jolieName() + "\")" )
				.append( field.min() != 1 || field.max() != 1 ? "[" + field.min() + "," + field.max() + "]" : "" )
				.append( ": {@link " ).append( typeName( field.type() ).replace( "<?>", "" ) ).append( "}" ) );
	}

	@Override
	protected void appendSeeDocumentation() {
		builder.newline();
		StreamEx.of( ClassPath.JOLIEVALUE, ClassPath.JOLIENATIVE )
			.map( ClassPath::get )
			.append( structure.fields()
				.parallelStream()
				.map( f -> f.type() instanceof Definition d ? qualifiedName( d ) : null )
				.filter( Objects::nonNull ) )
			.distinct()
			.forEachOrdered( s -> builder.newlineAppend( "@see " ).append( s ) );

		if( structure.hasBuilder() )
			builder.newlineAppend( "@see #builder()" );
	}

	@Override
	protected void appendSignature( boolean isInnerClass ) {
		builder.newlineAppend( "public " ).append( isInnerClass ? "static " : "" ).append( "final class " )
			.append( className ).append( " extends " ).append( ClassPath.TYPEDSTRUCTURE );
	}

	@Override
	protected void appendAttributes() {
		appendStaticAttributes();
		appendFieldAttributes();
	}

	private void appendStaticAttributes() {
		builder.newNewlineAppend( "private static final " ).append( ClassPath.SET.parameterized( ClassPath.STRING ) )
			.append( " FIELD_KEYS = fieldKeys( " ).append( className ).append( ".class );" );
	}

	private void appendFieldAttributes() {
		builder.newline();
		recordFields.parallelStream()
			.map( f -> new StringBuilder()
				.append( f.jolieName() == null
					? ""
					: "@" + ClassPath.JOLIENAME.get() + "(\"" + f.jolieName() + "\")\n" )
				.append( "private final " )
				.append( f.max() == 1
					? typeName( f.type() )
					: ClassPath.LIST.parameterized( typeName( f.type() ) ) )
				.append( " " ).append( f.javaName() ).append( ";" )
				.toString() )
			.forEachOrdered( builder::newlineAppend );
	}

	@Override
	protected void appendConstructors() {
		final String parameters = recordFields.parallelStream()
			.map( f -> new StringBuilder()
				.append( f.max() == 1
					? typeName( f.type() )
					: ClassPath.SEQUENCEDCOLLECTION.parameterized( typeName( f.type() ) ) )
				.append( " " )
				.append( f.javaName() )
				.toString() )
			.reduce( ( s1, s2 ) -> s1 + ", " + s2 )
			.map( s -> "( " + s + " )" )
			.orElse( "()" );

		builder.newNewlineAppend( "public " ).append( className ).append( parameters ).body( () -> {
			recordFields.parallelStream()
				.map( f -> {
					final StringBuilder result =
						new StringBuilder().append( "this." ).append( f.javaName() ).append( " = " );
					final NativeRefinement refinement = f.type() instanceof Basic.Inline b ? b.refinement() : null;

					if( f.max() != 1 )
						result.append( validateVectorField( f.javaName(), f.min(), f.max(), refinement ) );

					else if( f.min() != 0 )
						result.append( validateMandatoryField( f.javaName(), refinement ) );

					else
						result.append( validateRefinement( f.javaName(), refinement ) );

					return result.append( ";" ).toString();
				} )
				.forEachOrdered( builder::newlineAppend );
		} );
	}

	@Override
	protected void appendMethods() {
		builder.newline();
		recordFields.forEach( f -> {
			if( f.max() != 1 )
				builder.newlineAppend( "public " ).append( ClassPath.LIST.parameterized( typeName( f.type() ) ) )
					.append( " " ).append( f.javaName() ).append( "() { return " ).append( f.javaName() )
					.append( "; }" );
			else if( f.min() == 0 )
				builder.newlineAppend( "public " ).append( ClassPath.OPTIONAL.parameterized( typeName( f.type() ) ) )
					.append( " " ).append( f.javaName() ).append( "() { return " ).append( ClassPath.OPTIONAL )
					.append( ".ofNullable( " ).append( f.javaName() ).append( " ); }" );
			else
				builder.newlineAppend( "public " ).append( typeName( f.type() ) ).append( " " ).append( f.javaName() )
					.append( "() { return " ).append( f.javaName() ).append( "; }" );
		} );

		if( structure.contentType() != Native.ANY )
			builder.newNewlineAppend( "public " ).append( structure.contentType().wrapperClass() )
				.append( " content() { return new " ).append( structure.contentType().wrapperClass() )
				.append( structure.contentType() == Native.VOID ? "()" : "( contentValue )" ).append( "; }" );
	}

	@Override
	protected void appendFromMethod() {
		builder.newNewlineAppend( "public static " ).append( className ).append( " from( " )
			.append( ClassPath.JOLIEVALUE ).append( " j ) throws " ).append( ClassPath.TYPEVALIDATIONEXCEPTION )
			.body( () -> builder.newlineAppend( "return new " ).append( className ).append( "(" ).indented( () -> {
				if( structure.contentType() == Native.ANY )
					builder.newlineAppend( "j.content()," );
				else if( structure.contentType() != Native.VOID )
					builder.newlineAppend( structure.contentType().wrapperClass().get() )
						.append( ".from( j ).value()," );

				structure.fields()
					.parallelStream()
					.map( this::fromChildString )
					.reduce( ( s1, s2 ) -> s1 + ",\n" + s2 )
					.ifPresent( builder::newlineAppend );
			} )
				.newlineAppend( ");" ) );
	}

	@Override
	protected void appendFromValueMethod() {
		builder.newNewlineAppend( "public static " ).append( className ).append( " fromValue( " )
			.append( ClassPath.VALUE ).append( " v ) throws " ).append( ClassPath.TYPECHECKINGEXCEPTION )
			.body( () -> builder.newlineAppend( ClassPath.VALUEMANAGER.get() )
				.append( ".requireChildren( v, FIELD_KEYS );" )
				.newlineAppend( "return new " ).append( className ).append( "(" ).indented( () -> {
					if( structure.contentType() != Native.VOID )
						builder.newlineAppend( structure.contentType().wrapperClass().get() )
							.append( ".contentFromValue( v )," );

					structure.fields()
						.parallelStream()
						.map( f -> new StringBuilder()
							.append( ClassPath.VALUEMANAGER ).append( "." ).append( f.max() == 1 ? "single" : "vector" )
							.append( "FieldFrom( v, \"" ).append( f.jolieName() ).append( "\", " )
							.append( switch( storedType( f.type() ) ) {
							case Native n -> n.wrapperClass().get()
								+ (n == Native.ANY || n == Native.VOID ? "::fromValue" : "::fieldFromValue");
							case Definition d -> qualifiedName( d ) + "::fromValue";
							} ).append( " )" )
							.toString() )
						.reduce( ( s1, s2 ) -> s1 + ",\n" + s2 )
						.ifPresent( builder::newlineAppend );
				} )
				.newlineAppend( ");" ) );
	}

	@Override
	protected void appendToValueMethod() {
		builder.newNewlineAppend( "public static " ).append( ClassPath.VALUE ).append( " toValue( " )
			.append( className ).append( " t )" ).body( () -> {
				builder.newlineAppend( "final " ).append( ClassPath.VALUE ).append( " v = " )
					.append( switch( structure.contentType() ) {
					case ANY -> ClassPath.JOLIENATIVE.get() + ".toValue( t.content() )";
					case VOID -> ClassPath.VALUE.get() + ".create()";
					default -> ClassPath.VALUE.get() + ".create( t.contentValue() )";
					} ).append( ";" );

				builder.newline();
				structure.fields().forEach( f -> {
					final String getField = "t." + f.javaName() + "()";
					final UnaryOperator< String > setValue = x -> switch( storedType( f.type() ) ) {
					case Native n when n == Native.ANY || n == Native.VOID -> ".setValue( " + x + ".value() )";
					case Native n -> ".setValue( " + x + " )";
					case Definition d -> ".deepCopy( " + qualifiedName( d ) + ".toValue( " + x + " ) )";
					};
					if( f.max() != 1 )
						builder.newlineAppend( getField ).append( ".forEach( c -> v.getNewChild( \"" )
							.append( f.jolieName() ).append( "\" )" ).append( setValue.apply( "c" ) ).append( " );" );
					else if( f.min() == 0 )
						builder.newlineAppend( getField ).append( ".ifPresent( c -> v.getFirstChild( \"" )
							.append( f.jolieName() ).append( "\" )" ).append( setValue.apply( "c" ) ).append( " );" );
					else
						builder.newlineAppend( "v.getFirstChild( \"" ).append( f.jolieName() ).append( "\" )" )
							.append( setValue.apply( getField ) ).append( ";" );
				} );

				builder.newNewlineAppend( "return v;" );
			} );
	}

	@Override
	protected void appendBuilder() {
		builder.newNewlineAppend( "public static class Builder" ).body( () -> {
			appendBuilderAttributes();
			appendBuilderConstructors();
			appendBuilderMethods();
		} );
	}

	private void appendBuilderAttributes() {
		builder.newline();
		recordFields.parallelStream()
			.map( f -> new StringBuilder()
				.append( "private " )
				.append( f.max() == 1 ? typeName( f.type() )
					: ClassPath.SEQUENCEDCOLLECTION.parameterized( typeName( f.type() ) ) )
				.append( " " )
				.append( f.javaName() )
				.append( ";" )
				.toString() )
			.forEachOrdered( builder::newlineAppend );
	}

	private void appendBuilderConstructors() {
		builder.newNewlineAppend( "private Builder() {}" )
			.newlineAppend( "private Builder( " ).append( ClassPath.JOLIEVALUE ).append( " j )" ).body( () -> {
				switch( structure.contentType() ) {
				case VOID -> {
				}
				case ANY -> builder.newNewlineAppend( "content = j.content();" );
				default -> builder.newNewlineAppend( "contentValue = j.content() instanceof " )
					.append( structure.contentType().wrapperClass() ).append( " content ? content.value() : null;" );
				}
				structure.fields()
					.parallelStream()
					.map( f -> new StringBuilder()
						.append( "this." ).append( f.javaName() )
						.append( " = " )
						.append( fromChildString( f ) )
						.append( ";" )
						.toString() )
					.forEachOrdered( builder::newlineAppend );
			} );
	}

	private void appendBuilderMethods() {
		builder.newline();

		final String contentField = structure.contentType() == Native.ANY ? "content" : "contentValue";
		if( structure.contentType() != Native.VOID )
			appendFieldSetter( contentField, structure.contentType().nativeType() );
		appendExtraSetters( contentField, structure.contentType() );

		structure.fields().forEach( f -> {
			if( f.max() == 1 ) {
				appendFieldSetter( f.javaName(), typeName( f.type() ) );
				switch( storedType( f.type() ) ) {
				case Basic b -> appendExtraSetters( f.javaName(), b );
				case Structure s -> appendExtraSetters( f.javaName(), s );
				case Choice c -> appendExtraSetters( f.javaName(), c );
				case Native n -> appendExtraSetters( f.javaName(), n );
				}
			} else {
				appendFieldSetter( f.javaName(), ClassPath.SEQUENCEDCOLLECTION.parameterized( typeName( f.type() ) ) );
				switch( storedType( f.type() ) ) {
				case Basic b -> appendExtraListSetters( f.javaName(), b );
				case Structure s -> appendExtraListSetters( f.javaName(), s );
				case Choice c -> appendExtraListSetters( f.javaName(), c );
				case Native n -> appendExtraListSetters( f.javaName(), n );
				}
			}
		} );

		builder.newNewlineAppend( "public " ).append( className ).append( " build()" )
			.body( () -> recordFields.parallelStream()
				.map( Structure.Field::javaName )
				.reduce( ( s1, s2 ) -> s1 + ", " + s2 )
				.ifPresent(
					s -> builder.newlineAppend( "return new " ).append( className ).append( "( " ).append( s )
						.append( " );" ) ) );
	}

	private void appendFieldSetter( String fieldName, String fieldType ) {
		appendSetter( fieldName, fieldType, fieldName, () -> builder.append( "this." ).append( fieldName )
			.append( " = " ).append( fieldName ).append( "; return this;" ) );
	}

	private void appendExtraSetters( String fieldName, Basic b ) {
		appendValueSetter( fieldName, b.type().nativeType(),
			"value", "new " + qualifiedName( b ) + "( value )" );
	}

	private void appendExtraSetters( String fieldName, Structure s ) {
		if( s.hasBuilder() )
			appendBuilderSetter( fieldName, qualifiedName( s ),
				qualifiedName( s ) + "." + (s instanceof Undefined ? "InlineBuilder" : "Builder"),
				qualifiedName( s ) + ".builder()" );
	}

	private void appendExtraSetters( String fieldName, Choice c ) {
		// TODO: figure out a good way to allow one to easily set an option as the field value
	}

	private void appendExtraSetters( String fieldName, Native n ) {
		if( n == Native.ANY )
			Native.ANY.nativeClasses().forEach(
				cp -> appendValueSetter( fieldName, cp.get(), "value", ClassPath.JOLIENATIVE.get() + ".of( value )" ) );
	}

	private void appendExtraListSetters( String fieldName, Basic b ) {
		appendValueSetter( fieldName, b.type().nativeType() + "...",
			"values", "Arrays.stream( values ).map( " + qualifiedName( b ) + "::new ).toList()" );
	}

	private void appendExtraListSetters( String fieldName, Structure s ) {
		if( s instanceof Undefined )
			appendBuilderSetter( fieldName, ClassPath.LIST.parameterized( ClassPath.JOLIEVALUE ),
				ClassPath.JOLIEVALUE + ".InlineListBuilder", ClassPath.JOLIEVALUE + ".listBuilder()" );
		else if( s.hasBuilder() )
			appendBuilderSetter( fieldName, ClassPath.LIST.parameterized( qualifiedName( s ) ),
				ClassPath.STRUCTURELISTBUILDER.parameterized( qualifiedName( s ), qualifiedName( s ) + ".Builder" ),
				"new " + ClassPath.STRUCTURELISTBUILDER.parameterized( "" ) + "( " + qualifiedName( s )
					+ "::builder )" );
	}

	private void appendExtraListSetters( String fieldName, Choice c ) {
		if( c.hasBuilder() )
			appendBuilderSetter( fieldName, ClassPath.LIST.parameterized( qualifiedName( c ) ),
				qualifiedName( c ) + ".ListBuilder", qualifiedName( c ) + ".listBuilder()" );
	}

	private void appendExtraListSetters( String fieldName, Native n ) {
		if( n == Native.ANY )
			appendBuilderSetter( fieldName, ClassPath.LIST.parameterized( ClassPath.JOLIENATIVE.parameterized( "?" ) ),
				ClassPath.JOLIENATIVE.get() + ".ListBuilder", ClassPath.JOLIENATIVE.get() + ".listBuilder()" );
		else if( n != Native.VOID )
			appendValueSetter( fieldName, n.nativeType() + "...",
				"values", ClassPath.LIST.get() + ".of( values )" );
	}

	private void appendBuilderSetter( String fieldName, String fieldType, String builderClass,
		String builderSupplier ) {
		appendSetter( fieldName, ClassPath.FUNCTION.parameterized( builderClass, fieldType ), "f",
			() -> builder.append( "return " ).append( fieldName ).append( "( f.apply( " ).append( builderSupplier )
				.append( " ) );" ) );
	}

	private void appendValueSetter( String fieldName, String paramType, String paramName, String wrappingCall ) {
		appendSetter( fieldName, paramType, paramName, () -> builder.append( "return " ).append( fieldName )
			.append( "( " ).append( paramName ).append( " == null ? null : " ).append( wrappingCall ).append( " );" ) );
	}

	private void appendSetter( String name, String paramType, String paramName, Runnable bodyAppender ) {
		builder.newlineAppend( "public Builder " ).append( name ).append( "( " ).append( paramType ).append( " " )
			.append( paramName ).append( " ) { " ).run( bodyAppender ).append( " }" );
	}

	@Override
	protected void appendTypeClasses() {
		structure.fields()
			.parallelStream()
			.map( f -> TypeClassBuilder.create( storedType( f.type() ), typesPackage ) )
			.filter( Objects::nonNull )
			.map( JavaClassDirector::constructInnerClass )
			.forEachOrdered( builder::newlineAppend );
	}

	private String fromChildString( Structure.Field f ) {
		return new StringBuilder()
			.append( ClassPath.VALUEMANAGER ).append( ".fieldFrom( " )
			.append( f.max() == 1
				? "j.getFirstChild( \"" + f.jolieName() + "\" )"
				: "j.getChildOrDefault( \"" + f.jolieName() + "\", " + ClassPath.LIST.get() + ".of() )" )
			.append( ", " )
			.append( switch( storedType( f.type() ) ) {
			case Native.ANY -> ClassPath.JOLIEVALUE.get() + "::content";
			case Native.VOID ->
				"c -> c.content() instanceof " + ClassPath.JOLIEVOID.get() + " content ? content : null";
			case Native n ->
				"c -> c.content() instanceof " + n.wrapperClass().get() + " content ? content.value() : null";
			case Definition d -> qualifiedName( d ) + "::from";
			} )
			.append( " )" )
			.toString();
	}
}
