package joliex.java.generate.type;

import joliex.java.parse.ast.JolieType.Definition.Structure;
import joliex.java.generate.util.ClassPath;
import joliex.java.parse.ast.JolieType.Native;

public abstract class StructureClassBuilder extends TypeClassBuilder {

	protected final Structure structure;

	protected StructureClassBuilder( Structure structure, String typesPackage ) {
        super( structure.name(), typesPackage );
        this.structure = structure;
    }

	@Override
	protected void appendBody() {
		appendAttributes();
		appendConstructors();
		appendMethods();
		appendStaticMethods();
		appendInnerClasses();
	}

	private void appendStaticMethods() {
		if ( structure.hasBuilder() )
			appendBuildingMethods();
		appendFromMethod();
		appendFromValueMethod();
		appendToValueMethod();
	}

	private void appendBuildingMethods() {
		builder.newNewlineAppend( "public static Builder builder() { return new Builder(); }" );

		if ( structure.contentType() == Native.ANY ) {
			builder.newlineAppend( "public static Builder builder( " ).append( ClassPath.JOLIENATIVE.parameterized( "?" ) ).append( " content ) { return builder().content( content ); }" );
			structure.contentType().nativeClasses().forEach( cp ->
				builder.newlineAppend( "public static Builder builder( " ).append( cp ).append( " contentValue ) { return builder().content( " ).append( ClassPath.JOLIENATIVE ).append( ".of( contentValue ) ); }" ) );
		} else if ( structure.contentType() != Native.VOID )
			builder.newlineAppend( "public static Builder builder( " ).append( structure.contentType().nativeClass() ).append( " contentValue ) { return builder().contentValue( contentValue ); }" );

        builder.newlineAppend( "public static Builder builder( " ).append( ClassPath.JOLIEVALUE ).append( " from ) { return from != null ? new Builder( from ) : builder(); }" )
			.newline()
			.newlineAppend( "public static " ).append( ClassPath.STRUCTURELISTBUILDER.parameterized( className, "Builder" ) ).append( " listBuilder() { return new " ).append( ClassPath.STRUCTURELISTBUILDER.parameterized( "" ) ).append( "( " ).append( className ).append( "::builder ); }" )
        	.newlineAppend( "public static " ).append( ClassPath.STRUCTURELISTBUILDER.parameterized( className, "Builder" ) ).append( " listBuilder( " ).append( ClassPath.SEQUENCEDCOLLECTION.parameterized( "? extends " + ClassPath.JOLIEVALUE.get() ) ).append( " from )" ).body( () -> 
				builder.newlineAppend( "return from != null ? new " ).append( ClassPath.STRUCTURELISTBUILDER.parameterized( "" ) ).append( "( from, " ).append( className ).append( "::from, " ).append( className ).append( "::builder ) : listBuilder();" ) );
	}

	private final void appendInnerClasses() {
		if ( structure.hasBuilder() )
			appendBuilder();
		appendTypeClasses();
	}	

	protected abstract void appendAttributes();
	protected abstract void appendConstructors();
	protected abstract void appendMethods();

	protected abstract void appendFromMethod();
	protected abstract void appendFromValueMethod();
	protected abstract void appendToValueMethod();

	protected abstract void appendBuilder();
	protected abstract void appendTypeClasses();
}
