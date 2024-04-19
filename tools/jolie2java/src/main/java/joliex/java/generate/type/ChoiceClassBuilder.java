package joliex.java.generate.type;

import java.util.Optional;
import java.util.stream.Stream;
import java.util.HashSet;
import java.util.Objects;

import joliex.java.generate.JavaClassDirector;
import joliex.java.parse.ast.JolieType.Definition;
import joliex.java.parse.ast.JolieType.Native;
import joliex.java.parse.ast.JolieType.Definition.Basic;
import joliex.java.parse.ast.JolieType.Definition.Choice;
import joliex.java.parse.ast.JolieType.Definition.Structure;

public class ChoiceClassBuilder extends TypeClassBuilder {
    
    private final Choice choice;

    public ChoiceClassBuilder( Choice choice, String typesPackage ) { 
        super( choice.name(), typesPackage );
        this.choice = choice;
    }

    protected void appendDescriptionDocumentation() {
        builder.newlineAppend( "this class is a choice type which can be described as follows:" );
    }
    
    protected void appendDefinitionDocumentation() {
        choice.options()
            .parallelStream()
            .map( o -> switch ( o.type() ) {
                case Native n -> n.valueName();
                case Basic.Inline b -> b.nativeType().valueName() + Optional.ofNullable( b.refinement() ).map( r -> "( " + r.definitionString() + " )" ).orElse( "" );
                case Definition d -> d.name();
            } )
            .reduce( (s1, s2) -> s1 + " | " + s2 )
            .ifPresent( s -> builder.newlineAppend( className ).append( ": " ).append( s ) );
    }

    protected void appendSeeDocumentation() {
        builder.newline()
            .newlineAppend( "@see JolieValue" )
            .newlineAppend( "@see JolieNative" );
    }

    protected void appendSignature( boolean isInnerClass ) {
        builder.newlineAppend( "public " ).append( isInnerClass ? "static " : "" ).append( "sealed interface " ).append( className ).append( " extends JolieValue" );
    }

    protected void appendBody() {
        appendMethods();
        appendOptionClasses();
        appendStaticMethods();
        appendTypeClasses();
        if ( choice.hasBuilder() )
            appendListBuilder();
    }

    private void appendMethods() {
        builder.newNewlineAppend( "Value jolieRepr();" );
    }

    private void appendOptionClasses() {
        choice.numberedOptions()
            .mapKeyValue( (number, type) -> new OptionClassBuilder( type, "C" + number, className ) )
            .map( JavaClassDirector::constructInnerClass )
            .forEachOrdered( builder::newlineAppend );
    }

    private void appendStaticMethods() {
        // create() methods
        choice.numberedOptions().forKeyValue( (i,t) -> {
            if ( t == Native.VOID )
                builder.newNewlineAppend( "public static " ).append( className ).append( " create" ).append( i ).append( "() { return new C" ).append( i ).append( "(); }" );
            else
                builder.newNewlineAppend( "public static " ).append( className ).append( " create" ).append( i ).append( "( " ).append( typeName( t ) ).append( " option ) { return new C" ).append( i ).append( "( option ); }" );

            if ( t instanceof Structure s && s.hasBuilder() )
                builder.newlineAppend( "public static " ).append( className ).append( " create" ).append( i ).append( "( Function<" ).append( s.name() ).append( ".Builder, " ).append( s.name() ).append( "> b ) { return create" ).append( i ).append( "( b.apply( " ).append( s.name() ).append( ".construct() ) ); }" );
        } );
        // createFrom() method
        builder.newNewlineAppend( "public static " ).append( className ).append( " createFrom( JolieValue j ) throws TypeValidationException" ).body( () -> 
            builder.newlineAppend( "return ValueManager.choiceFrom( j, " )
                .append( fromFunctionList( "createFrom" ) )
                .append( " );" ) 
        );
        // fromValue() method
        builder.newNewlineAppend( "public static " ).append( className ).append( " fromValue( Value v ) throws TypeCheckingException" ).body( () -> 
            builder.newlineAppend( "return ValueManager.choiceFrom( v, " )
                .append( fromFunctionList( "fromValue" ) )
                .append( " );" )
        );
        // toValue() method
        builder.newNewlineAppend( "public static Value toValue( " ).append( className ).append( " t ) { return t.jolieRepr(); }" );
    }

    private void appendTypeClasses() {
        choice.options()
            .parallelStream()
            .map( o -> switch ( o.type() ) {
                case Structure.Inline.Typed s -> new TypedStructureClassBuilder( s, null );
                case Structure.Inline.Untyped s -> new UntypedStructureClassBuilder( s, null );
                default -> null;
            } )
            .filter( Objects::nonNull )
            .map( JavaClassDirector::constructInnerClass )
            .forEachOrdered( builder::newlineAppend );
    }

    private void appendListBuilder() {
        builder.newNewlineAppend( "public static class ListBuilder extends AbstractListBuilder<ListBuilder, " ).append( className ).append( ">" ).body( () -> {
			builder.newline()
				.newlineAppend( "private ListBuilder() {}" )
				.newlineAppend( "private ListBuilder( SequencedCollection<? extends JolieValue> c ) { super( c, " ).append( className ).append( "::createFrom ); }" )
				.newline()
				.newlineAppend( "protected ListBuilder self() { return this; }" );
            
            choice.numberedOptions().forKeyValue( (i,t) -> {
                if ( t == Native.VOID )
                    builder.newline()
                        .newlineAppend( "public ListBuilder add" ).append( i ).append( "() { return add( new C" ).append( i ).append( "() ); }" )
                        .newlineAppend( "public ListBuilder set" ).append( i ).append( "( int index ) { return set( index, new C" ).append( i ).append( "() ); }" );
                else
                    builder.newline()
                        .newlineAppend( "public ListBuilder add" ).append( i ).append( "( " ).append( typeName( t ) ).append( " option ) { return add( new C" ).append( i ).append( "( option ) ); }" )
                        .newlineAppend( "public ListBuilder set" ).append( i ).append( "( int index, " ).append( typeName( t ) ).append( " option ) { return set( index, new C" ).append( i ).append( "( option ) ); }" );

                if ( t instanceof Structure s && s.hasBuilder() )
                    builder.newline()
                        .newlineAppend( "public ListBuilder add" ).append( i ).append( "( Function<" ).append( s.name() ).append( ".Builder, " ).append( s.name() ).append( "> b ) { return add" ).append( i ).append( "( b.apply( " ).append( s.name() ).append( ".construct() ) ); }" )
                        .newlineAppend( "public ListBuilder set" ).append( i ).append( "( int index, Function<" ).append( s.name() ).append( ".Builder, " ).append( s.name() ).append( "> b ) { return set" ).append( i ).append( "( index, b.apply( " ).append( s.name() ).append( ".construct() ) ); }" );
            } );
        } );
    }

    private String fromFunctionList( String methodName ) {
        return fromFunctionStream( choice, methodName, new HashSet<>() )
            .map( StringBuilder::toString )
            .reduce( (s1, s2) -> s1 + ", " + s2 )
            .map( s -> "List.of( " + s + " )" )
            .orElse( "List.of()" );
    }

    private Stream<StringBuilder> fromFunctionStream( Choice choice, String methodName, HashSet<String> choiceNames ) {
        if ( !choiceNames.add( choice.name() ) )
            return Stream.empty();

        final String optionPrefix = choice.equals( this.choice ) ? "" : choice.name() + ".";
        return choice.numberedOptions()
            .flatMapKeyValue( (i,t) -> t instanceof Choice c
                ? fromFunctionStream( c, methodName, choiceNames ).map( b -> b
                    .append( ".andThen( " )
                    .append( optionPrefix )
                    .append( "C" ).append( i ).append( "::new" )
                    .append( " )" ) )
                : Stream.of( new StringBuilder()
                    .append( "ValueManager.castFunc( " )
                    .append( optionPrefix )
                    .append( "C" ).append( i ).append( "::" ).append( methodName )
                    .append( " )" ) )
            );
    }
}
