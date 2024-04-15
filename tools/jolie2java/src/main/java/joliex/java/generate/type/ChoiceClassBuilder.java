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

    public ChoiceClassBuilder( Choice choice, String packageName, String typeFolder ) { 
        super( choice.name(), packageName, typeFolder );

        this.choice = choice;
    }

    public void appendDefinition( boolean isInnerClass ) {
        builder.newline()
            .commentBlock( this::appendDocumentation )
            .newlineAppend( "public " ).append( isInnerClass ? "static " : "" ).append( "sealed interface " ).append( className ).append( " extends JolieValue" )
            .body( this::appendDefinitionBody );
    }

    private void appendDocumentation() {
        builder.newlineAppend( "this class is a choice type which can be described as follows:" )
            .newline()
            .codeBlock( this::appendChoiceDocumentation )
            .newline()
            .newlineAppend( "@see JolieValue" )
            .newlineAppend( "@see JolieNative" );
    }
    
    private void appendChoiceDocumentation() {
        builder.newlineAppend( className ).append( ": " );
        
        choice.options()
            .parallelStream()
            .map( o -> switch ( o.type() ) {
                case Native n -> n.valueName();
                case Basic.Inline b -> b.nativeType().valueName() + Optional.ofNullable( b.refinement() ).map( r -> "( " + r.definitionString() + " )" ).orElse( "" );
                case Definition d -> d.name();
            } )
            .reduce( (s1, s2) -> s1 + " | " + s2 )
            .ifPresent( builder::append );
    }

    private void appendDefinitionBody() {
        appendMethods();
        appendOptionClasses();
        appendStaticMethods();
        appendInnerClasses();
    }

    private void appendMethods() {
        builder.newNewlineAppend( "Value jolieRepr();" );
    }

    private void appendStaticMethods() {
        // create() methods
        choice.options().forEach( o -> {

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

    private void appendOptionClasses() {
        choice.numberedOptions()
            .mapKeyValue( (number, type) -> new OptionClassBuilder( type, "C" + number, className ) )
            .map( JavaClassDirector::constructInnerClass )
            .forEachOrdered( builder::newlineAppend );
    }

    private void appendInnerClasses() {
        choice.options()
            .parallelStream()
            .map( o -> switch ( o.type() ) {
                case Structure.Inline.Typed s -> new TypedStructureClassBuilder( s, null, null );
                case Structure.Inline.Untyped s -> new UntypedStructureClassBuilder( s, null, null );
                default -> null;
            } )
            .filter( Objects::nonNull )
            .map( JavaClassDirector::constructInnerClass )
            .forEachOrdered( builder::newlineAppend );
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
