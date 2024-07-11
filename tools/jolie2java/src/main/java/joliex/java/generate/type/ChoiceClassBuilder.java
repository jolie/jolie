package joliex.java.generate.type;

import java.util.Optional;
import java.util.stream.Stream;
import java.util.HashSet;
import java.util.Objects;

import joliex.java.generate.JavaClassDirector;
import joliex.java.generate.util.ClassPath;
import joliex.java.parse.ast.JolieType.Definition;
import joliex.java.parse.ast.JolieType.Native;
import one.util.streamex.StreamEx;
import joliex.java.parse.ast.JolieType.Definition.Basic;
import joliex.java.parse.ast.JolieType.Definition.Choice;
import joliex.java.parse.ast.JolieType.Definition.Structure;

public class ChoiceClassBuilder extends TypeClassBuilder {

	private final Choice choice;

	public ChoiceClassBuilder( Choice choice, String typesPackage ) {
		super( choice.name(), typesPackage );
		this.choice = choice;
	}

	@Override
	protected void appendDescriptionDocumentation() {
		builder.newlineAppend( "This is a sealed interface representing the following choice type:" );
	}

	@Override
	protected void appendDefinitionDocumentation() {
		choice.options()
			.parallelStream()
			.map( t -> switch( t ) {
			case Native.VOID -> "void";
			case Native n -> n.nativeClass().get();
			case Basic.Inline b -> b.type().nativeClass().get()
				+ Optional.ofNullable( b.refinement() ).map( r -> "( " + r.definitionString() + " )" ).orElse( "" );
			case Definition d -> qualifiedName( d );
			} )
			.reduce( ( s1, s2 ) -> s1 + " | " + s2 )
			.ifPresent( s -> builder.newlineAppend( className ).append( ": " ).append( s ) );
	}

	@Override
	protected void appendSeeDocumentation() {
		builder.newline();
		StreamEx.of( ClassPath.JOLIEVALUE, ClassPath.JOLIENATIVE )
			.map( ClassPath::get )
			.append( choice.options()
				.parallelStream()
				.map( t -> t instanceof Definition d ? qualifiedName( d ) : null )
				.filter( Objects::nonNull ) )
			.distinct()
			.forEachOrdered( n -> builder.newlineAppend( "@see " ).append( n ) );

		choice.numberedOptions().forKeyValue( ( i, t ) -> builder.newlineAppend( "@see #of" ).append( i ).append( "(" )
			.append( typeName( t ) ).append( ")" ) );
	}

	@Override
	protected void appendSignature( boolean isInnerClass ) {
		builder.newlineAppend( "public " ).append( isInnerClass ? "static " : "" ).append( "sealed interface " )
			.append( className ).append( " extends " ).append( ClassPath.JOLIEVALUE );
	}

	@Override
	protected void appendBody() {
		appendMethods();
		appendOptionClasses();
		appendStaticMethods();
		appendTypeClasses();
		if( choice.hasBuilder() )
			appendListBuilder();
	}

	private void appendMethods() {
		builder.newNewlineAppend( ClassPath.VALUE.get() ).append( " jolieRepr();" );
	}

	private void appendOptionClasses() {
		choice.numberedOptions()
			.mapKeyValue( ( number, type ) -> new OptionClassBuilder( type, className, "C" + number, typesPackage ) )
			.map( JavaClassDirector::constructInnerClass )
			.forEachOrdered( builder::newlineAppend );
	}

	private void appendStaticMethods() {
		// listBuilder() methods
		if( choice.hasBuilder() )
			builder.newline()
				.newlineAppend( "public static ListBuilder listBuilder() { return new ListBuilder(); }" )
				.newlineAppend( "public static ListBuilder listBuilder( " )
				.append( ClassPath.SEQUENCEDCOLLECTION.parameterized( ClassPath.JOLIEVALUE ) )
				.append( " from ) { return from != null ? new ListBuilder( from ) : listBuilder(); }" );
		// of() methods
		choice.numberedOptions().forKeyValue( ( i, t ) -> {
			if( t == Native.VOID )
				builder.newNewlineAppend( "public static " ).append( className ).append( " of" ).append( i )
					.append( "() { return new C" ).append( i ).append( "(); }" );
			else {
				builder.newNewlineAppend( "public static " ).append( className ).append( " of" ).append( i )
					.append( "( " ).append( typeName( t ) ).append( " option ) { return new C" ).append( i )
					.append( "( option ); }" );

				switch( storedType( t ) ) {
				case Basic b -> builder.newlineAppend( "public static " ).append( className ).append( " of" )
					.append( i ).append( "( " ).append( b.type().nativeClass() ).append( " value ) { return of" )
					.append( i ).append( "( new " ).append( qualifiedName( b ) ).append( "( value ) ); }" );
				case Structure s when s.hasBuilder() -> builder.newlineAppend( "public static " ).append( className )
					.append( " of" ).append( i ).append( "( " )
					.append( ClassPath.FUNCTION.parameterized( qualifiedName( s ) + ".Builder", qualifiedName( s ) ) )
					.append( " f ) { return of" ).append( i ).append( "( f.apply( " ).append( qualifiedName( s ) )
					.append( ".builder() ) ); }" );
				default -> {
				}
				}
			}
		} );
		// from() method
		builder.newNewlineAppend( "public static " ).append( className ).append( " from( " )
			.append( ClassPath.JOLIEVALUE ).append( " j ) throws " ).append( ClassPath.TYPEVALIDATIONEXCEPTION ).body(
				() -> builder.newlineAppend( "return " ).append( ClassPath.VALUEMANAGER ).append( ".choiceFrom( j, " )
					.append( fromFunctionList( "from" ) )
					.append( " );" ) );
		// fromValue() method
		builder.newNewlineAppend( "public static " ).append( className ).append( " fromValue( " )
			.append( ClassPath.VALUE ).append( " v ) throws " ).append( ClassPath.TYPECHECKINGEXCEPTION ).body(
				() -> builder.newlineAppend( "return " ).append( ClassPath.VALUEMANAGER ).append( ".choiceFrom( v, " )
					.append( fromFunctionList( "fromValue" ) )
					.append( " );" ) );
		// toValue() method
		builder.newNewlineAppend( "public static " ).append( ClassPath.VALUE ).append( " toValue( " )
			.append( className ).append( " t ) { return t.jolieRepr(); }" );
	}

	private void appendTypeClasses() {
		choice.options()
			.parallelStream()
			.map( t -> switch( t ) {
			case Structure.Inline.Typed s -> new TypedStructureClassBuilder( s, typesPackage );
			case Structure.Inline.Untyped s -> new UntypedStructureClassBuilder( s, typesPackage );
			default -> null;
			} )
			.filter( Objects::nonNull )
			.map( JavaClassDirector::constructInnerClass )
			.forEachOrdered( builder::newlineAppend );
	}

	private void appendListBuilder() {
		builder.newNewlineAppend( "public static class ListBuilder extends " )
			.append( ClassPath.ABSTRACTLISTBUILDER.parameterized( "ListBuilder", className ) ).body( () -> {
				builder.newline()
					.newlineAppend( "private ListBuilder() {}" )
					.newlineAppend( "private ListBuilder( " )
					.append( ClassPath.SEQUENCEDCOLLECTION.parameterized( "? extends " + ClassPath.JOLIEVALUE.get() ) )
					.append( " c ) { super( c, " ).append( className ).append( "::from ); }" )
					.newline()
					.newlineAppend( "protected ListBuilder self() { return this; }" );

				choice.numberedOptions().forKeyValue( ( i, t ) -> {
					if( t == Native.VOID )
						builder.newline()
							.newlineAppend( "public ListBuilder add" ).append( i ).append( "() { return add( new C" )
							.append( i ).append( "() ); }" )
							.newlineAppend( "public ListBuilder add" ).append( i )
							.append( "( int index ) { return add( index, new C" ).append( i ).append( "() ); }" )
							.newlineAppend( "public ListBuilder set" ).append( i )
							.append( "( int index ) { return set( index, new C" ).append( i ).append( "() ); }" );
					else
						builder.newline()
							.newlineAppend( "public ListBuilder add" ).append( i ).append( "( " )
							.append( typeName( t ) ).append( " option ) { return add( new C" ).append( i )
							.append( "( option ) ); }" )
							.newlineAppend( "public ListBuilder add" ).append( i ).append( "( int index, " )
							.append( typeName( t ) ).append( " option ) { return add( index, new C" ).append( i )
							.append( "( option ) ); }" )
							.newlineAppend( "public ListBuilder set" ).append( i ).append( "( int index, " )
							.append( typeName( t ) ).append( " option ) { return set( index, new C" ).append( i )
							.append( "( option ) ); }" );

					if( t instanceof Structure s && s.hasBuilder() )
						builder.newline()
							.newlineAppend( "public ListBuilder add" ).append( i ).append( "( " )
							.append( ClassPath.FUNCTION.parameterized( qualifiedName( s ) + ".Builder",
								qualifiedName( s ) ) )
							.append( " b ) { return add" ).append( i ).append( "( b.apply( " )
							.append( qualifiedName( s ) ).append( ".builder() ) ); }" )
							.newlineAppend( "public ListBuilder add" ).append( i ).append( "( int index, " )
							.append( ClassPath.FUNCTION.parameterized( qualifiedName( s ) + ".Builder",
								qualifiedName( s ) ) )
							.append( " b ) { return add" ).append( i ).append( "( index, b.apply( " )
							.append( qualifiedName( s ) ).append( ".builder() ) ); }" )
							.newlineAppend( "public ListBuilder set" ).append( i ).append( "( int index, " )
							.append( ClassPath.FUNCTION.parameterized( qualifiedName( s ) + ".Builder",
								qualifiedName( s ) ) )
							.append( " b ) { return set" ).append( i ).append( "( index, b.apply( " )
							.append( qualifiedName( s ) ).append( ".builder() ) ); }" );
				} );

				builder.newNewlineAppend( "public " ).append( ClassPath.LIST.parameterized( className ) )
					.append( " build() { return super.build(); }" );
			} );
	}

	private String fromFunctionList( String methodName ) {
		return fromFunctionStream( choice, methodName, new HashSet<>() )
			.map( StringBuilder::toString )
			.reduce( ( s1, s2 ) -> s1 + ", " + s2 )
			.map( s -> ClassPath.LIST + ".of( " + s + " )" )
			.orElse( ClassPath.LIST + ".of()" );
	}

	private Stream< StringBuilder > fromFunctionStream( Choice choice, String methodName,
		HashSet< String > choiceNames ) {
		if( !choiceNames.add( qualifiedName( choice ) ) )
			return Stream.empty();

		final String optionPrefix = choice.equals( this.choice ) ? "" : qualifiedName( choice ) + ".";
		return choice.numberedOptions()
			.flatMapKeyValue( ( i, t ) -> t instanceof Choice c
				? fromFunctionStream( c, methodName, choiceNames ).map( b -> b
					.append( ".andThen( " )
					.append( optionPrefix )
					.append( "C" ).append( i ).append( "::new" )
					.append( " )" ) )
				: Stream.of( new StringBuilder()
					.append( "" ).append( ClassPath.VALUEMANAGER ).append( ".castFunc( " )
					.append( optionPrefix )
					.append( "C" ).append( i ).append( "::" ).append( methodName )
					.append( " )" ) ) );
	}
}
