package joliex.java.generate.type;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.Objects;

import joliex.java.generate.JavaClassDirector;
import joliex.java.parse.ast.JolieType.Definition;
import joliex.java.parse.ast.JolieType.Native;
import joliex.java.parse.ast.JolieType.Definition.Basic;
import joliex.java.parse.ast.JolieType.Definition.Choice;
import joliex.java.parse.ast.JolieType.Definition.Structure;

public class ChoiceClassBuilder extends TypeClassBuilder {
    
    private final Choice choice;
    private final boolean listable;
    private final List<OptionMethodBuilder> methodBuilders;

    public ChoiceClassBuilder( Choice choice, String packageName, String typeFolder, boolean listable ) { 
        super( choice.name(), packageName, typeFolder );

        this.choice = choice; 
        this.listable = listable;
        this.methodBuilders = choice.numberedOptions()
            .mapKeyValue( (number, option) -> {
                return switch ( option ) {
                    case Native n -> new BasicMethodBuilder( null, number, n );
                    case Basic.Inline b -> new BasicMethodBuilder( null, number, b.nativeType() );
                    case Basic.Link b -> new BasicMethodBuilder( b.name(), number, b.nativeType() );
                    case Structure s -> new StructureMethodBuilder( s.name(), number, s.nativeType() );
                    case Choice c -> new ChoiceMethodBuilder( c.name(), number );
                    default -> throw new IllegalArgumentException( "Didn't recognize the type of the option, unfolded options should only contain Basic and Structure options." );
                };
            } )
            .toList();
    }

    public void appendDefinition( boolean isInnerClass ) {
        builder.newline()
            .commentBlock( this::appendDocumentation )
            .newlineAppend( "public " ).append( isInnerClass ? "static " : "" ).append( "sealed interface " ).append( className ).append( " extends StructureType" )
            .body( this::appendDefinitionBody );
    }

    private void appendDocumentation() {
        builder.newlineAppend( "this class is a choice type which can be described as follows:" )
            .newline()
            .codeBlock( this::appendChoiceDocumentation )
            .newline()
            .newlineAppend( "@see JolieType" )
            .newlineAppend( "@see StructureType" )
            .newlineAppend( "@see BasicType" );

        methodBuilders.parallelStream().forEachOrdered( OptionMethodBuilder::appendDocumentationLinks );
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
        appendOptionClasses();
        appendMethods();
        if ( listable ) appendListBuilders();
        appendInnerClasses();
    }

    private void appendOptionClasses() {
        choice.numberedOptions()
            .mapKeyValue( (number, option) -> OptionClassBuilder.create( "C" + number, className, option ) )
            .map( JavaClassDirector::constructInnerClass )
            .forEachOrdered( builder::newlineAppend );
    }

    private void appendInnerClasses() {
        choice.options()
            .parallelStream()
            .map( Choice.Option::type )
            .map( o -> o instanceof Structure.Inline s
                ? new StructureClassBuilder( s, null, null, false )
                : null
            )
            .filter( Objects::nonNull )
            .map( JavaClassDirector::constructInnerClass )
            .forEachOrdered( builder::newlineAppend );
    }

    private void appendMethods() {
        methodBuilders.forEach( OptionMethodBuilder::appendStaticMethods );

        if ( listable )
            builder.newline()
                .newlineAppend( "static InlineListBuilder constructList() { return new InlineListBuilder(); }" )
                .newlineAppend( "static <T> NestedListBuilder<T> constructNestedList( Function<List<" ).append( className ).append( ">, T> doneFunc, SequencedCollection<? extends JolieType> c ) { return new NestedListBuilder<>( doneFunc, c ); }" )
                .newlineAppend( "static <T> NestedListBuilder<T> constructNestedList( Function<List<" ).append( className ).append( ">, T> doneFunc ) { return new NestedListBuilder<>( doneFunc ); }" );

        builder.newline()
            .newlineAppend( "public static " ).append( className ).append( " createFrom( JolieType t ) throws TypeValidationException" )
            .body( () -> appendFromMethodBody( "Function", "JolieType", "t", "createFrom", "TypeValidationException", "\"The given JolieType couldn't be converted to any of the option types.\"" ) )
            .newline()
            .newlineAppend( "public static Value toValue( " ).append( className ).append( " t ) { return t.jolieRepr(); }" )
            .newlineAppend( "public static " ).append( className ).append( " fromValue( Value value ) throws TypeCheckingException" )
            .body( () -> appendFromMethodBody( "ConversionFunction", "Value", "value", "fromValue", "TypeCheckingException", "\"The given Value couldn't be converted to any of the option types.\"" ) );
    }

    private Stream<String> inlineFromMethodCalls( String fromMethod, Choice choice, HashSet<String> choiceNames ) {
        if ( className.equals( choice.name() ) || !choiceNames.add( choice.name() ) )
            return Stream.empty();

        return choice.numberedOptions()
            .flatMapKeyValue( (i, d) -> d instanceof Choice c  
                ? inlineFromMethodCalls( fromMethod, c, choiceNames ).map( s -> choice.name() + ".create" + i + "( " + s + " )" )
                : Stream.of( choice.name() + ".C" + i + "." + fromMethod + "( v )" )
            );
    }

    private void appendFromMethodBody( String functionType, String paramType, String paramName, String fromMethod, String exceptionType, String exceptionMessage ) {
        builder.newlineAppend( "return Stream.<" ).append( functionType ).append( "<" ).append( paramType ).append( ", " ).append( className ).append( ">>of(" )
            .indentedNewlineAppend( 
                choice.numberedOptions()
                    .parallel()
                    .flatMapKeyValue( (i,d) -> d instanceof Choice c
                        ? inlineFromMethodCalls( fromMethod, c, new HashSet<>() ).map( s -> "v -> create" + i + "( " + s + " )" )
                        : Stream.of( "C" + i + "::" + fromMethod )
                    )
                    .reduce( (s1, s2) -> s1 + ",\n" + s2 )
                    .orElseGet( () -> "" )
            )
            .newlineAppend( ")" )
            .indent()
                .newlineAppend( ".map( f ->" ).body( () -> builder
                    .newlineAppend( "try" ).body( () -> builder
                        .newlineAppend( "return f.apply( " ).append( paramName ).append( " );" ) 
                    ).append( " catch ( " ).append( exceptionType ).append( " e )" ).body( () -> builder
                        .newlineAppend( "return null;" )
                    )
                ).append( " )" )
                .newlineAppend( ".filter( Objects::nonNull )" )
                .newlineAppend( ".findFirst()" )
                .newlineAppend( ".orElseThrow( () -> new " ).append( exceptionType ).append( "( " ).append( exceptionMessage ).append( " ) );" )
            .dedent();
    }

    private void appendListBuilders() {
        builder.newNewlineAppend( "static abstract class ListBuilder<B> extends StructureListBuilder<" ).append( className ).append( ", B>" )
            .body( this::appendListBuilderDefinition );

        builder.newNewlineAppend( "public static class InlineListBuilder extends ListBuilder<InlineListBuilder>" ).body( () -> builder
            .newline()
            .newlineAppend( "protected InlineListBuilder self() { return this; }" )
            .newline()
            .newlineAppend( "public List<" ).append( className ).append( "> build() { return super.build(); }" )
        );

        builder.newNewlineAppend( "public static class NestedListBuilder<T> extends ListBuilder<NestedListBuilder<T>>" ).body( () -> builder
            .newline()
            .newlineAppend( "private final Function<List<" ).append( className ).append( ">, T> doneFunc;" )
            .newline()
            .newlineAppend( "private NestedListBuilder( Function<List<" ).append( className ).append( ">, T> doneFunc, SequencedCollection<? extends JolieType> c ) { super( c ); this.doneFunc = doneFunc; }" )
            .newlineAppend( "private NestedListBuilder( Function<List<" ).append( className ).append( ">, T> doneFunc ) { this.doneFunc = doneFunc; }" )
            .newline()
            .newlineAppend( "protected NestedListBuilder<T> self() { return this; }" )
            .newline()
            .newlineAppend( "public T done() throws TypeValidationException { return doneFunc.apply( build() ); }" )
        );
    }

    private void appendListBuilderDefinition() {
        builder.newline()
            .newlineAppend( "protected ListBuilder( SequencedCollection<? extends JolieType> elements ) { super( elements.parallelStream().map( " ).append( className ).append( "::createFrom ).toList() ); }" )
            .newlineAppend( "protected ListBuilder() {}" );

        methodBuilders.forEach( OptionMethodBuilder::appendListBuilderMethods );
    }
    

    private abstract class OptionMethodBuilder {

        protected final String typeName;
        protected final int number;
        protected final String optionName;

        protected OptionMethodBuilder( String typeName, int number ) {
            this.typeName = typeName;
            this.number = number;
            this.optionName = "C" + number;
        }

        public abstract void appendDocumentationLinks();
        public abstract void appendStaticMethods();
        public abstract void appendListBuilderMethods();
    }

    private class StructureMethodBuilder extends OptionMethodBuilder {

        private final String typeName;
        private final Native root;

        public StructureMethodBuilder( String typeName, int number, Native root ) {
            super( typeName, number );
            this.typeName = typeName;
            this.root = root;
        }

        public void appendDocumentationLinks() {
            builder.newline()
                .newlineAppend( "@see " ).append( typeName )
                .newlineAppend( "@see #create" ).append( number ).append( "(" ).append( typeName ).append( ")" )
                .newlineAppend( "@see #construct" ).append( number ).append( "()" );
        }

        public void appendStaticMethods() {
            builder.newNewlineAppend( "public static " ).append( className ).append( " create" ).append( number ).append( "( " ).append( typeName ).append( " option ) { return new " ).append( optionName ).append( "( option ); }" )
                .newline()
                .newlineAppend( "public static " ).append( optionName ).append( ".InlineBuilder construct" ).append( number ).append( "() { return " ).append( optionName ).append( ".construct(); }" );
                        
            if ( root != Native.VOID ) {
                builder.newlineAppend( "public static " ).append( optionName ).append( ".InlineBuilder construct" ).append( number ).append( "( " ).append( root.wrapperName() ).append( " root ) { return construct" ).append( number ).append( "().root( root ); }" );
                root.valueNames().forEach( vn -> builder.newlineAppend( "public static " ).append( optionName ).append( ".InlineBuilder construct" ).append( number ).append("( " ).append( vn ).append( " rootValue ) { return construct" ).append( number ).append( "().root( rootValue ); }" ) );
            }

            builder.newlineAppend( "public static " ).append( optionName ).append( ".InlineBuilder construct" ).append( number ).append( "From( JolieType t ) { return " ).append( optionName ).append( ".constructFrom( t ); }" );
            builder.newlineAppend( "public static " ).append( optionName ).append( " create" ).append( number ).append( "From( JolieType t ) throws TypeValidationException { return " ).append( optionName ).append( ".createFrom( t ); }" );
        }

        public void appendListBuilderMethods() {
            builder.newline()
                .newlineAppend( "public B add" ).append( number ).append( "( " ).append( typeName ).append( " option ) { return add( create" ).append( number ).append( "( option ) ); }" )
                .newlineAppend( "public B set" ).append( number ).append( "( int index, " ).append( typeName ).append( " option ) { return set( index, create" ).append( number ).append( "( option ) ); }" )
                .newlineAppend( "public " ).append( typeName ).append( ".NestedBuilder<B> addConstructed" ).append( number ).append( "() { return " ).append( typeName ).append( ".constructNested( this::add" ).append( number ).append( " ); }" )
                .newlineAppend( "public " ).append( typeName ).append( ".NestedBuilder<B> addConstructed" ).append( number ).append( "From( JolieType t ) { return " ).append( typeName ).append( ".constructNested( this::add" ).append( number ).append( ", t ); }" )
                .newlineAppend( "public " ).append( typeName ).append( ".NestedBuilder<B> setConstructed" ).append( number ).append( "( int index ) { return " ).append( typeName ).append( ".constructNested( option -> set" ).append( number ).append( "( index, option ) ); }" )
                .newlineAppend( "public " ).append( typeName ).append( ".NestedBuilder<B> setConstructed" ).append( number ).append( "From( int index, JolieType t ) { return " ).append( typeName ).append( ".constructNested( option -> set" ).append( number ).append( "( index, option ), t ); }" )
                .newlineAppend( "public " ).append( typeName ).append( ".NestedBuilder<B> reconstruct" ).append( number ).append( "( int index ) { return " ).append( typeName ).append( ".constructNested( option -> set" ).append( number ).append( "( index, option ), elements.get( index ) ); }" );
            
            if ( root != Native.VOID ) {
                builder
                    .newlineAppend( "public " ).append( typeName ).append( ".NestedBuilder<B> addConstructed" ).append( number ).append( "( " ).append( root.wrapperName() ).append( " root ) { return addConstructed" ).append( number ).append( "().root( root ); }" )
                    .newlineAppend( "public " ).append( typeName ).append( ".NestedBuilder<B> setConstructed" ).append( number ).append( "( int index, " ).append( root.wrapperName() ).append( " root ) { return setConstructed" ).append( number ).append( "( index ).root( root ); }" );
                
                root.valueNames().forEach( vn -> builder
                    .newlineAppend( "public " ).append( typeName ).append( ".NestedBuilder<B> addConstructed" ).append( number ).append( "( " ).append( vn ).append( " rootValue ) { return addConstructed" ).append( number ).append( "( BasicType.create( rootValue ) ); }" )
                    .newlineAppend( "public " ).append( typeName ).append( ".NestedBuilder<B> setConstructed" ).append( number ).append( "( int index, " ).append( vn ).append( " rootValue ) { return setConstructed" ).append( number ).append( "( index, BasicType.create( rootValue ) ); }" )
                );
                
                if ( root == Native.ANY )
                    builder.newlineAppend( "public " ).append( typeName ).append( ".NestedBuilder<B> reconstruct" ).append( number ).append( "( int index, UnaryOperator<" ).append( root.wrapperName() ).append( "> rootOperator ) { return reconstruct" ).append( number ).append( "( index ).root( rootOperator ); }" );
                else
                    builder.newlineAppend( "public " ).append( typeName ).append( ".NestedBuilder<B> reconstruct" ).append( number ).append( "( int index, UnaryOperator<" ).append( root.valueName() ).append( "> valueOperator ) { return reconstruct" ).append( number ).append( "( index ).root( valueOperator ); }" );
            }
        }
    }

    private class BasicMethodBuilder extends OptionMethodBuilder {

        private final Native type;

        public BasicMethodBuilder( String typeName, int number, Native type ) {
            super( typeName, number );
            this.type = type;
        }

        public void appendDocumentationLinks() {
            if ( typeName != null )
                builder.newlineAppend( "@see #create" ).append( number ).append( "(" ).append( typeName ).append( ")" );
            builder.newlineAppend( "@see #create" ).append( number ).append( "(" ).append( type == Native.VOID ? "" : type.valueName() ).append( ")" );
        }

        public void appendStaticMethods() {
            if ( typeName != null )
                builder.newNewlineAppend( "public static " ).append( className ).append( " create" ).append( number ).append( "( " ).append( typeName ).append( " option ) { return new " ).append( optionName ).append( "( option ); }" )
                    .newlineAppend( "public static " ).append( className ).append( " create" ).append( number ).append( type == Native.VOID ? "()" : "( " + type.valueName() + " option )" ).append( " { return create" ).append( number ).append( "( " ).append( typeName ).append( ".create( option ) ); }" );
            else
                builder.newNewlineAppend( "public static " ).append( className ).append( " create" ).append( number ).append( type == Native.VOID ? "()" : "( " + type.valueName() + " option )" ).append( " { return new " ).append( optionName ).append( type == Native.VOID ? "()" : "( option )" ).append( "; }" );
        }

        public void appendListBuilderMethods() {
            if ( type == Native.VOID )
                builder.newline()
                    .newlineAppend( "public B add" ).append( number ).append( "() { return add( create" ).append( number ).append( "() ); }" )
                    .newlineAppend( "public B set" ).append( number ).append( "( int index ) { return set( index, create" ).append( number ).append( "() ); }" );
            else
                builder.newline()
                    .newlineAppend( "public B add" ).append( number ).append( "( " ).append( type.valueName() ).append( " option ) { return add( create" ).append( number ).append( "( option ) ); }" )
                    .newlineAppend( "public B set" ).append( number ).append( "( int index, " ).append( type.valueName() ).append( " option ) { return set( index, create" ).append( number ).append( "( option ) ); }" );
        }
    }

    private class ChoiceMethodBuilder extends OptionMethodBuilder {

        public ChoiceMethodBuilder( String typeName, int number ) {
            super( typeName, number );
        }

        public void appendDocumentationLinks() {
            builder.newlineAppend( "@see #create" ).append( number ).append( "(" ).append( typeName ).append( ")" );
        }

        public void appendStaticMethods() {
            builder.newNewlineAppend( "public static " ).append( className ).append( " create" ).append( number ).append( "( " ).append( typeName ).append( " option ) { return new " ).append( optionName ).append( "( option ); }" );
        }

        public void appendListBuilderMethods() {
            builder.newline()
                .newlineAppend( "public B add" ).append( number ).append( "( " ).append( typeName ).append( " option ) { return add( create" ).append( number ).append( "( option ) ); }" )
                .newlineAppend( "public B set" ).append( number ).append( "( int index, " ).append( typeName ).append( " option ) { return set( index, create" ).append( number ).append( "( option ) ); }" );
        }
    }
}
