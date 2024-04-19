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
		appendCreateFromMethod();
		appendFromValueMethod();
		appendToValueMethod();
	}

	private void appendConstructMethods() {
		builder.newline()
			.newlineAppend( "public static Builder construct() { return new Builder(); }" )
			.newlineAppend( "public static ListBuilder constructList() { return new ListBuilder(); }" )
        	.newlineAppend( "public static Builder constructFrom( JolieValue j ) { return new Builder( j ); }" )
        	.newlineAppend( "public static ListBuilder constructListFrom( SequencedCollection<? extends JolieValue> c ) { return new ListBuilder( c ); }" );
		
		if ( structure.nativeType() == Native.ANY ) {
			builder.newNewlineAppend( "public static Builder construct( JolieNative<?> content ) { return construct().content( content ); }" );
			structure.nativeType().valueNames().forEach( n ->
				builder.newlineAppend( "public static Builder construct( " ).append( n ).append( " contentValue ) { return construct().content( JolieNative.create( contentValue ) ); }" ) );
		} else if ( structure.nativeType() != Native.VOID )
			builder.newNewlineAppend( "public static Builder construct( " ).append( structure.nativeType().valueName() ).append( " contentValue ) { return construct().contentValue( contentValue ); }" );
	}

	private final void appendInnerClasses() {
		if ( structure.hasBuilder() ) {
			appendBuilder();
			appendListBuilder();
		}
		appendTypeClasses();
	}	

    private void appendListBuilder() {
        builder.newNewlineAppend( "public static class ListBuilder extends AbstractListBuilder<ListBuilder, " ).append( className ).append( ">" ).body( () ->
			builder.newline()
				.newlineAppend( "private ListBuilder() {}" )
				.newlineAppend( "private ListBuilder( SequencedCollection<? extends JolieValue> c ) { super( c, " ).append( className ).append( "::createFrom ); }" )
				.newline()
				.newlineAppend( "protected ListBuilder self() { return this; }" )
            	.newline()
            	.newlineAppend( "public ListBuilder add( Function<Builder, " ).append( className ).append( "> b ) { return add( b.apply( construct() ) ); }" )
            	.newlineAppend( "public ListBuilder set( int index, Function<Builder, " ).append( className ).append( "> b ) { return set( index, b.apply( construct() ) ); }" )
            	.newlineAppend( "public ListBuilder reconstruct( int index, Function<Builder, " ).append( className ).append( "> b ) { return replace( index, j -> b.apply( constructFrom( j ) ) ); }" ) );
    }

	protected abstract void appendAttributes();
	protected abstract void appendConstructors();
	protected abstract void appendMethods();

	protected abstract void appendCreateFromMethod();
	protected abstract void appendFromValueMethod();
	protected abstract void appendToValueMethod();

	protected abstract void appendBuilder();
	protected abstract void appendTypeClasses();
}
