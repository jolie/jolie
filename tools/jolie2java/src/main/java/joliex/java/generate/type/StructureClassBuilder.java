package joliex.java.generate.type;

import joliex.java.parse.ast.JolieType.Definition.Structure;
import joliex.java.parse.ast.JolieType.Native;

public abstract class StructureClassBuilder extends TypeClassBuilder {

	protected final Structure structure;

	protected StructureClassBuilder( Structure structure, String typesPackage ) {
        super( structure.name(), typesPackage );
        this.structure = structure;
    }

	protected void appendBody() {
		appendAttributes();
		appendConstructors();
		appendMethods();
		appendStaticMethods();
		appendInnerClasses();
	}

	private void appendStaticMethods() {
		if ( structure.hasBuilder() )
			appendConstructMethods();
		appendFromMethod();
		appendFromValueMethod();
		appendToValueMethod();
	}

	private void appendConstructMethods() {
		builder.newline()
			.newlineAppend( "public static Builder builder() { return new Builder(); }" )
        	.newlineAppend( "public static Builder builder( JolieValue from ) { return new Builder( from ); }" )
			.newlineAppend( "public static StructureListBuilder<" ).append( className ).append( ",Builder> listBuilder() { return new StructureListBuilder<>( " ).append( className ).append( "::builder, " ).append( className ).append( "::builder ); }" )
        	.newlineAppend( "public static StructureListBuilder<" ).append( className ).append( ",Builder> listBuilder( SequencedCollection<? extends JolieValue> from )" ).body( () -> 
				builder.newlineAppend( "return new StructureListBuilder<>( from, " ).append( className ).append( "::from, " ).append( className ).append( "::builder, " ).append( className ).append( "::builder );" ) );
		
		if ( structure.nativeType() == Native.ANY ) {
			builder.newNewlineAppend( "public static Builder builder( JolieNative<?> content ) { return builder().content( content ); }" );
			structure.nativeType().valueNames().forEach( n ->
				builder.newlineAppend( "public static Builder builder( " ).append( n ).append( " contentValue ) { return builder().content( JolieNative.of( contentValue ) ); }" ) );
		} else if ( structure.nativeType() != Native.VOID )
			builder.newNewlineAppend( "public static Builder builder( " ).append( structure.nativeType().valueName() ).append( " contentValue ) { return builder().contentValue( contentValue ); }" );
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
