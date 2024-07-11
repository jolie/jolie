package joliex.java.generate.type;

import java.util.List;
import joliex.java.generate.JavaClassBuilder;
import joliex.java.generate.util.ClassPath;
import joliex.java.parse.ast.JolieType;
import joliex.java.parse.ast.NativeRefinement;
import joliex.java.parse.ast.NativeRefinement.Enumeration;
import joliex.java.parse.ast.NativeRefinement.Length;
import joliex.java.parse.ast.NativeRefinement.Ranges;
import joliex.java.parse.ast.NativeRefinement.Regex;
import joliex.java.parse.ast.JolieType.Definition;
import joliex.java.parse.ast.JolieType.Definition.Basic;
import joliex.java.parse.ast.JolieType.Definition.Choice;
import joliex.java.parse.ast.JolieType.Definition.Structure;
import joliex.java.parse.ast.JolieType.Definition.Structure.Undefined;
import joliex.java.parse.ast.JolieType.Native;

public abstract class TypeClassBuilder extends JavaClassBuilder {

	protected final String className;
	protected final String typesPackage;

	protected TypeClassBuilder( String className, String typesPackage ) {
		this.className = className;
		this.typesPackage = typesPackage;
	}

	@Override
	public String className() {
		return className;
	}

	@Override
	public final void appendPackage() {
		builder.append( "package " ).append( typesPackage ).append( ";" );
	}

	@Override
	public void appendDefinition() {
		appendDefinition( false );
	}

	public void appendDefinition( boolean isInnerClass ) {
		appendDocumentation();
		appendSignature( isInnerClass );
		builder.body( this::appendBody );
	}

	protected void appendDocumentation() {
		builder.newline().commentBlock( () -> {
			appendDescriptionDocumentation();
			builder.codeBlock( this::appendDefinitionDocumentation );
			appendSeeDocumentation();
		} );
	}

	protected abstract void appendDescriptionDocumentation();

	protected abstract void appendDefinitionDocumentation();

	protected abstract void appendSeeDocumentation();

	protected abstract void appendSignature( boolean isInnerClass );

	protected abstract void appendBody();

	public static TypeClassBuilder create( JolieType type, String typesPackage ) {
		return switch( type ) {
		case Basic.Inline b -> new BasicClassBuilder( b, typesPackage );
		case Structure.Inline.Typed s -> new TypedStructureClassBuilder( s, typesPackage );
		case Structure.Inline.Untyped s -> new UntypedStructureClassBuilder( s, typesPackage );
		case Choice.Inline c -> new ChoiceClassBuilder( c, typesPackage );
		default -> null;
		};
	}

	protected String qualifiedName( Definition d ) {
		if( d instanceof Undefined )
			return ClassPath.JOLIEVALUE.get();

		return d.isLink() ? typesPackage + "." + d.name() : d.name();
	}

	protected String typeName( JolieType t ) {
		return switch( storedType( t ) ) {
		case Native.VOID -> Native.VOID.wrapperClass().get();
		case Native n -> n.nativeType();
		case Definition d -> qualifiedName( d );
		};
	}

	/*
	 * Convenience method to handle the fact that Basic.Inline and Native are stored in the same way.
	 */
	protected static JolieType storedType( JolieType t ) {
		return t instanceof Basic.Inline b ? b.type() : t;
	}

	protected static String validateVectorField( String name, int min, int max, NativeRefinement refinement ) {
		return new StringBuilder()
			.append( ClassPath.VALUEMANAGER ).append( ".validated( " )
			.append( "\"" ).append( name ).append( "\", " )
			.append( name ).append( ", " ).append( min ).append( ", " ).append( max )
			.append( ", t -> " ).append( validateRefinement( "t", refinement ) )
			.append( " )" )
			.toString();
	}

	protected static String validateMandatoryField( String name, NativeRefinement refinement ) {
		return new StringBuilder()
			.append( ClassPath.VALUEMANAGER ).append( ".validated( " )
			.append( "\"" ).append( name ).append( "\", " )
			.append( validateRefinement( name, refinement ) )
			.append( " )" )
			.toString();
	}

	protected static String validateRefinement( String name, NativeRefinement refinement ) {
		return switch( refinement ) {
		case null -> name;

		case Ranges(List< Ranges.Interval > intervals) -> new StringBuilder()
			.append( ClassPath.REFINEMENTVALIDATOR ).append( ".ranges( " )
			.append( name )
			.append( intervals.parallelStream()
				.map( i -> ", " + i.minString() + ", " + i.maxString() )
				.reduce( ( s1, s2 ) -> s1 + s2 )
				.orElse( "" ) )
			.append( " )" ).toString();

		case Length(int min, int max) -> new StringBuilder()
			.append( ClassPath.REFINEMENTVALIDATOR ).append( ".length( " )
			.append( name ).append( ", " ).append( min ).append( ", " ).append( max )
			.append( " )" ).toString();

		case Enumeration(List< String > values) -> new StringBuilder()
			.append( ClassPath.REFINEMENTVALIDATOR ).append( ".enumeration( " )
			.append( name )
			.append( values.parallelStream()
				.map( v -> ", \"" + v + "\"" )
				.reduce( ( s1, s2 ) -> s1 + s2 )
				.orElse( "" ) )
			.append( " )" ).toString();

		case Regex(String regex) -> new StringBuilder()
			.append( ClassPath.REFINEMENTVALIDATOR ).append( ".regex( " )
			.append( name ).append( ", \"" ).append( regex ).append( "\"" )
			.append( " )" ).toString();
		};
	}
}
