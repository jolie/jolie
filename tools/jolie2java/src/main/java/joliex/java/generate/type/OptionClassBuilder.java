package joliex.java.generate.type;

import java.util.Optional;

import joliex.java.generate.JavaClassBuilder;
import joliex.java.parse.ast.JolieType;
import joliex.java.parse.ast.NativeRefinement;
import joliex.java.parse.ast.JolieType.Definition.Basic;
import joliex.java.parse.ast.JolieType.Definition.Choice;
import joliex.java.parse.ast.JolieType.Native;
import joliex.java.parse.ast.JolieType.Definition.Structure;

public abstract class OptionClassBuilder extends JavaClassBuilder {

    protected final String className;
    protected final String superName;

    protected OptionClassBuilder( String className, String superName ) {
        this.className = className;
        this.superName = superName;
    }

    public String className() { return null; }
    public void appendHeader() {}

    public static OptionClassBuilder create( String className, String superName, JolieType option ) {
        return switch ( option ) {
            case Native n -> new BasicOptionBuilder( className, superName, n );
            case Basic b -> new BasicOptionBuilder( className, superName, b );
            case Structure s -> new StructureOptionBuilder( className, superName, s );
            case Choice c -> new ChoiceOptionBuilder( className, superName, c );
        };
    }

    public static class ChoiceOptionBuilder extends OptionClassBuilder {

        private final Choice choice;

        public ChoiceOptionBuilder( String className, String superName, Choice choice ) {
            super( className, superName );
            this.choice = choice;
        }

        public void appendDefinition() {
            builder.newlineAppend( "public static record " ).append( className ).append( "( " ).append( choice.name() ).append( " option )" ).append( " implements " ).append( superName )
                .body( this::appendDefinitionBody );
        }
    
        private void appendDefinitionBody() {
            builder.newline()
                .newlineAppend( "public JolieNative<?> content() { return option.content(); }" )
                .newlineAppend( "public Map<String, List<JolieValue>> children() { return option.children(); }" )
                .newline()
                .newlineAppend( "public boolean equals( Object obj ) { return obj instanceof JolieValue j && content().equals( j.content() ) && children().equals( j.children() ); }" )
                .newlineAppend( "public int hashCode() { return option.hashCode(); }" )
                .newlineAppend( "public String toString() { return option.toString(); }" )
                .newline()
                .newlineAppend( "public static " ).append( className ).append( " createFrom( JolieValue t ) throws TypeValidationException { return new " ).append( className ).append( "( " ).append( choice.name() ).append( ".createFrom( t ) ); }" )
                .newline()
                .newlineAppend( "public static Value toValue( " ).append( className ).append( " t ) { return JolieValue.toValue( t ); }" )
                .newlineAppend( "public static " ).append( className ).append( " fromValue( Value value ) throws TypeCheckingException { return new " ).append( className ).append( "( " ).append( choice.name() ).append( ".fromValue( value ) ); }" );
        }
    }

    public static class StructureOptionBuilder extends OptionClassBuilder {

        private final Structure structure;

        public StructureOptionBuilder( String className, String superName, Structure structure ) {
            super( className, superName );
            this.structure = structure;
        }

        public void appendDefinition() {
            builder.newlineAppend( "public static record " ).append( className ).append( "( " ).append( structure.name() ).append( " option )" ).append( " implements " ).append( superName )
                .body( this::appendDefinitionBody );
        }
    
        private void appendDefinitionBody() {
            appendMethods();
            appendBuilders();
        }

        private void appendMethods() {
            builder.newline()
                .newlineAppend( "public " ).append( structure.nativeType().wrapperName() ).append( " content() { return option.content(); }" )
                .newlineAppend( "public Map<String, List<JolieValue>> children() { return option.children(); }" )
                .newline()
                .newlineAppend( "public boolean equals( Object obj ) { return obj instanceof JolieValue j && content().equals( j.content() ) && children().equals( j.children() ); }" )
                .newlineAppend( "public int hashCode() { return option.hashCode(); }" )
                .newlineAppend( "public String toString() { return option.toString(); }" )
                .newline()
                .newlineAppend( "public static InlineBuilder construct() { return new InlineBuilder(); }" )
                .newline()
                .newlineAppend( "public static InlineBuilder constructFrom( JolieValue t ) { return new InlineBuilder( t ); }" )
                .newlineAppend( "public static " ).append( className ).append( " createFrom( JolieValue t ) throws TypeValidationException { return constructFrom( t ).build(); }" )
                .newline()
                .newlineAppend( "public static Value toValue( " ).append( className ).append( " t ) { return JolieValue.toValue( t ); }" )
                .newlineAppend( "public static " ).append( className ).append( " fromValue( Value value ) throws TypeCheckingException { return new " ).append( className ).append( "( " ).append( structure.name() ).append( ".fromValue( value ) ); }" );
        }
    
        private void appendBuilders() {
            builder.newNewlineAppend( "public static class InlineBuilder extends " ).append( structure.name() ).append( ".Builder<InlineBuilder>" )
                .body( () -> builder.newline()
                    .newlineAppend( "private InlineBuilder() {}" )
                    .newlineAppend( "private InlineBuilder( JolieValue t ) { super( t ); }" )
                    .newline()
                    .newlineAppend( "protected InlineBuilder self() { return this; }" )
                    .newline()
                    .newlineAppend( "public " ).append( className ).append( " build() throws TypeValidationException { return new " ).append( className ).append( "( validatedBuild() ); }" )
                    
                );
        }
    }

    public static class BasicOptionBuilder extends OptionClassBuilder {

        private final Native type;
        private final Optional<NativeRefinement> refinement;
        private final Optional<String> linkName;

        private BasicOptionBuilder( String className, String superName, Native type, NativeRefinement refinement, String linkName ) {
            super( className, superName );
            this.type = type;
            this.refinement = Optional.ofNullable( refinement );
            this.linkName = Optional.ofNullable( linkName );
        }

        public BasicOptionBuilder( String className, String superName, Native type ) { this( className, superName, type, null, null ); }
        public BasicOptionBuilder( String className, String superName, Basic basic ) { this( className, superName, basic.nativeType(), basic.refinement(), basic instanceof Basic.Link ? basic.name() : null ); }

        public void appendDefinition() {
            builder.newlineAppend( "public static record " ).append( className ).append( type == Native.VOID ? "()" : "( " + linkName.orElse( type.valueName() ) + " option )" ).append( " implements " ).append( superName )
                .body( this::appendDefinitionBody );
        }
    
        private void appendDefinitionBody() {
            linkName.ifPresentOrElse(
                this::appendLinkBody, 
                this::appendInlineBody
            );
        }

        private void appendLinkBody( String name ) {
            appendMethods(
                "option.content()", 
                name + ".createFrom( t )", 
                () -> builder.newlineAppend( "return new " ).append( className ).append( "( " ).append( name ).append( ".fromValue( value ) );" )
            );
        }

        private void appendInlineBody() {
            refinement.ifPresent( r -> builder
                .newNewlineAppend( "private final static List<Refinement<" ).append( type.valueName() ).append( ">> refinements = " ).append( r.createString() ).append( ";" )
                .newline()
                .newlineAppend( "public " ).append( className ).append( "( " ).append( type.valueName() ).append( " option )" )
                .body( () -> builder.newlineAppend( "this.option = Refinement.validated( option, refinements );" ) )
            );

            switch ( type ) {
                case ANY -> appendMethods( 
                    "option", 
                    "JolieNative.createFrom( t )", 
                    this::appendAnyFromValue 
                );
                case VOID -> appendMethods( 
                    "JolieNative.create()", 
                    null, 
                    this::appendVoidFromValue 
                );
                default -> appendMethods( 
                    "JolieNative.create( option )", 
                    type.wrapperName() + ".createFrom( t ).value()", 
                    this::appendNativeFromValue
                );
            }
        }

        private void appendMethods( String contentAccess, String createFrom, Runnable fromValueBody ) {
            final Optional<String> cf = Optional.ofNullable( createFrom );
            builder.newline()
                .newlineAppend( "public " ).append( type.wrapperName() ).append( " content() { return " ).append( contentAccess ).append( "; }" )
                .newlineAppend( "public Map<String, List<JolieValue>> children() { return Map.of(); }" )
                .newline()
                .newlineAppend( "public boolean equals( Object obj ) { return obj instanceof JolieValue j && content().equals( j.content() ) && children().equals( j.children() ); }" )
                .newlineAppend( "public String toString() { return content().toString(); }" )
                .newline()
                .newlineAppend( "public static " ).append( className ).append( " createFrom( JolieValue t ) throws TypeValidationException { return new " ).append( className ).append( cf.map( c -> "( " + c + " )" ).orElse( "()" ) ).append( "; }" )
                .newline()
                .newlineAppend( "public static Value toValue( " ).append( className ).append( " t ) { return JolieValue.toValue( t ); }" )
                .newlineAppend( "public static " ).append( className ).append( " fromValue( Value value ) throws TypeCheckingException" )
                .body( () -> builder
                    .newlineAppend( "if ( value.hasChildren() )" )
                    .indentedNewlineAppend( "throw new TypeCheckingException( \"The given value has children.\" );" )
                    .newline()
                    .run( fromValueBody ) 
                );
        }

        private void appendAnyFromValue() {
            builder.newlineAppend( "return new " ).append( className ).append( "( JolieNative.fromValue( value ) );" );
        }

        private void appendVoidFromValue() {
            builder.newlineAppend( "if ( value.valueObject() != null )" )
                .indentedNewlineAppend( "throw new TypeCheckingException( \"The given value is not of the correct type.\" );" )
                .newline()
                .newlineAppend( "return new " ).append( className ).append( "();" );
        }

        private void appendNativeFromValue() {
            builder.newlineAppend( "if ( !value." ).append( type.valueChecker() ).append( "() )" )
                .indentedNewlineAppend( "throw new TypeCheckingException( \"The given value is not of the correct type.\" );" )
                .newline()
                .newlineAppend( "return new " ).append( className ).append( "( value." ).append( type.valueGetter() ).append( "() );" );
        }
    }
}
