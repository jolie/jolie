package joliex.java.generate.type;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import joliex.java.generate.JavaClassDirector;
import joliex.java.parse.ast.JolieType;
import joliex.java.parse.ast.JolieType.Definition;
import joliex.java.parse.ast.JolieType.Native;
import joliex.java.parse.ast.JolieType.Definition.Basic;
import joliex.java.parse.ast.JolieType.Definition.Choice;
import joliex.java.parse.ast.JolieType.Definition.Structure;
import joliex.java.parse.ast.JolieType.Definition.Structure.Undefined;

import org.apache.commons.lang3.StringUtils;

import one.util.streamex.StreamEx;

public class StructureClassBuilder extends TypeClassBuilder {

    private final List<FieldMethodBuilder> methodBuilders;
    private final Structure structure;
    private final boolean listable;

    public StructureClassBuilder( Structure structure, String packageName, String typeFolder, boolean listable ) {
        super( structure.name(), packageName, typeFolder ); 

        this.structure = structure; 
        this.listable = listable;
        
        methodBuilders = structure.fields().parallelStream().map( this::createFieldMethodBuilder ).filter( Objects::nonNull ).toList();
    }

    public void appendDefinition( boolean isInnerClass ) {
        builder.newline()
            .commentBlock( this::appendDocumentation )
            .newlineAppend( "public " ).append( isInnerClass ? "static " : "" ).append( "final class " ).append( className ).append( " extends ImmutableStructure<" ).append( structure.nativeType().wrapperName() ).append( ">" )
            .body( this::appendDefinitionBody );
    }

    private void appendDocumentation() {
        builder.newlineAppend( "this class is an {@link ImmutableStructure} which can be described as follows:" )
            .newline()
            .codeBlock( this::appendStructureDocumentation )
            .newline();

        StreamEx.of( "JolieType", "StructureType", "BasicType" )
            .append( 
                structure.fields()
                    .parallelStream()
                    .map( Structure.Field::type )
                    .map( t -> t instanceof Definition d ? d.name() : null )
                    .filter( Objects::nonNull )
            )
            .distinct() 
            .append( "#construct()" )
            .forEachOrdered( s -> builder.newlineAppend( "@see " ).append( s ) );
    }

    private void appendStructureDocumentation() {
        builder.newlineAppend( "root: {@link " ).append( structure.nativeType().valueName() ).append( "}" ).append( structure.possibleRefinement().map( r -> "( " + r.definitionString() + " )" ).orElse( "" ) ).append( structure.fields().isEmpty() ? " { ? }" : "" );

        structure.fields().forEach( field ->  
            builder.indentedNewlineAppend( field.possibleName().map( n -> field.key().equals( n ) ? n : n + "(\"" + field.key() + "\")" ).orElse( "\"" + field.key() + "\"" ) ).append( field.min() != 1 || field.max() != 1 ? "[" + field.min() + "," + field.max() + "]" : "" ).append( ": {@link " ).append( field.typeName().replaceAll( "<.*>", "" ) ).append( "}" )
        );
    }

    private void appendDefinitionBody() {
        appendAttributes();
        appendMethods();
        appendConstructors();
        appendStaticMethods();
        appendBuilders();
        if ( listable ) appendListBuilders();
        appendInnerClasses();
    }

    private void appendInnerClasses() {
        structure.fields()
            .parallelStream()
            .map( f -> TypeClassBuilder.create( f.type(), f.max() != 1 ) )
            .filter( Objects::nonNull )
            .map( JavaClassDirector::constructInnerClass )
            .forEachOrdered( builder::newlineAppend );
    }

    private void appendAttributes() {
        structure.possibleRefinement().ifPresent( r ->
            builder.newNewlineAppend( "private static final List<Refinement<" ).append( structure.nativeType().valueName() ).append( ">> refinements = " ).append( r.createString() ).append( ";" )
        );
    }

    private void appendConstructors() {
        builder.newNewlineAppend( "private " ).append( className ).append( "( Builder<?> builder )" )
            .body( () -> structure.possibleRefinement().ifPresentOrElse(
                r -> builder.newlineAppend( "super( Refinement.validated( builder.root(), refinements ), builder.children() );" ), 
                () -> builder.newlineAppend( "super( builder.root(), builder.children() );" )
            ) );
    }

    private void appendMethods() {
        if ( structure.nativeType() != Native.ANY && structure.nativeType() != Native.VOID )
            builder.newNewlineAppend( "public " ).append( structure.nativeType().valueName() ).append( " rootValue() { return root().value(); }" );
        else if ( !structure.fields().isEmpty() )
            builder.newline();

        methodBuilders.forEach( FieldMethodBuilder::appendGetter );
    }

    private void appendStaticMethods() {
        // inline construct methods
        builder.newNewlineAppend( "public static InlineBuilder construct() { return new InlineBuilder(); }" );

        if ( structure.nativeType() != Native.VOID ) {
            builder.newlineAppend( "public static InlineBuilder construct( " ).append( structure.nativeType().wrapperName() ).append( " root ) { return construct().root( root ); }" );
            
            structure.nativeType().valueNames().forEach( vn -> 
                builder.newlineAppend( "public static InlineBuilder construct( " ).append( vn ).append( " rootValue ) { return construct().root( rootValue ); }" ) 
            );
        }

        // nested construct methods
        builder.newline()
            .newlineAppend( "static <T> NestedBuilder<T> constructNested( Function<" ).append( className ).append( ", T> doneFunc ) { return new NestedBuilder<>( doneFunc ); }" )
            .newlineAppend( "static <T> NestedBuilder<T> constructNested( Function<" ).append( className ).append( ", T> doneFunc, JolieType t ) { return new NestedBuilder<>( doneFunc, t ); }" );
        
        // list construct methods
        if ( listable )
            builder.newline()
                .newlineAppend( "static InlineListBuilder constructList() { return new InlineListBuilder(); }" )
                .newline()
                .newlineAppend( "static <T> NestedListBuilder<T> constructNestedList( Function<List<" ).append( className ).append( ">, T> doneFunc ) { return new NestedListBuilder<>( doneFunc ); }" )
                .newlineAppend( "static <T> NestedListBuilder<T> constructNestedList( Function<List<" ).append( className ).append( ">, T> doneFunc, SequencedCollection<? extends JolieType> c ) { return new NestedListBuilder<>( doneFunc, c ); }" );

        // constructFrom method
        builder.newNewlineAppend( "public static InlineBuilder constructFrom( JolieType t ) { return new InlineBuilder( t ); }" );
                
        // createFrom method
        builder.newNewlineAppend( "public static " ).append( className ).append( " createFrom( JolieType t ) throws TypeValidationException { return constructFrom( t ).build(); }" );

        // ValueConverter methods
        builder.newline()
            .newlineAppend( "public static Value toValue( " ).append( className ).append( " t ) { return t.jolieRepr(); }" )
            .newlineAppend( "public static " ).append( className ).append( " fromValue( Value value ) throws TypeCheckingException { return Builder.buildFrom( value ); }" );
    }

    private void appendBuilders() {
        // abstract builder
        builder.newNewlineAppend( "static abstract class Builder<B> extends " ).append( structure.fields().isEmpty() ? "UntypedBuilder" : "StructureBuilder" ).append("<" ).append( structure.nativeType().wrapperName() ).append( ", B>" )
            .body( this::appendBuilderDefinition );

        // inline builder
        builder.newNewlineAppend( "public static class InlineBuilder extends Builder<InlineBuilder>" )
            .body( () -> builder
                .newline()
                .newlineAppend( "private InlineBuilder() {}" )
                .newlineAppend( "private InlineBuilder( JolieType t ) { super( JolieType.toStructure( t ) ); }" )
                .newline()
                .newlineAppend( "protected InlineBuilder self() { return this; }" )
                .newline()
                .newlineAppend( "public " ).append( className ).append( " build() throws TypeValidationException { return validatedBuild(); }" )
            );
        
        // nested builder
        builder.newNewlineAppend( "public static class NestedBuilder<T> extends Builder<NestedBuilder<T>>" )
            .body( () -> builder
                .newline()
                .newlineAppend( "private final Function<" ).append( className ).append( ", T> doneFunc;" )
                .newline()
                .newlineAppend( "private NestedBuilder( Function<" ).append( className ).append( ", T> doneFunc, JolieType t ) { super( JolieType.toStructure( t ) ); this.doneFunc = doneFunc; }" )
                .newlineAppend( "private NestedBuilder( Function<" ).append( className ).append( ", T> doneFunc ) { this.doneFunc = doneFunc; }" )
                .newline()
                .newlineAppend( "protected NestedBuilder<T> self() { return this; }" )
                .newline()
                .newlineAppend( "public T done() throws TypeValidationException { return doneFunc.apply( validatedBuild() ); }" )
            );
    }

    private void appendBuilderDefinition() {
        appendBuilderAttributes();
        appendBuilderConstructors();
        appendBuilderMethods();
    }

    private void appendBuilderAttributes() {
        buildFMEntriesString().ifPresent( entries ->
            builder.newNewlineAppend( "private static final Map<String,FieldManager<?>> FIELD_MAP = Map.of(" )
                .indentedNewlineAppend( entries )
                .newlineAppend( ");" )
        );
    }

    private Optional<String> buildFMEntriesString() {
        return structure.fields()
            .parallelStream()
            .map( f -> "\"" + f.key() + "\", " + buildFMCreationString( f ) )
            .reduce( (s1, s2) -> s1 + ",\n" + s2 );
    }

    private String buildFMCreationString( Structure.Field field ) {
        final StringBuilder b = new StringBuilder();
        final String wrapperName = switch( field.type() ) { 
            case Native n -> n.wrapperName().replaceAll( "<.+>", "" ); 
            case Definition d -> d.name(); 
        };

        return b.append( "FieldManager.create( " ).append( field.min() != 1 || field.max() != 1 ? field.min() + ", " + field.max() + ", " : "" ).append( wrapperName ).append( "::fromValue, " ).append( wrapperName ).append( "::createFrom )" ).toString();
    }

    private void appendBuilderConstructors() {
        builder.newline()
            .newlineAppend( "protected Builder() {}" )
            .newlineAppend( "protected Builder( StructureType structure )" ).body( this::appendBuildFromConstructorDefinition );
    }

    private void appendBuildFromConstructorDefinition() {
        final String rootString = switch( structure.nativeType() ) {
            case VOID -> "null";
            case ANY -> "structure.root()";
            default -> "structure.root() instanceof " + structure.nativeType().wrapperName() + " root ? root : null";
        };

        if ( structure.fields().isEmpty() )
            builder.newlineAppend( "super( " ).append( rootString ).append( ", structure.children() );" );
        else
            builder.newlineAppend( "super(" )
                .indent()
                    .newlineAppend( rootString ).append( "," )
                    .newlineAppend( "structure.children()" )
                    .indent()
                        .newlineAppend( ".entrySet()" )
                        .newlineAppend( ".parallelStream()" )
                        .newlineAppend( ".filter( e -> FIELD_MAP.containsKey( e.getKey() ) )" )
                        .newlineAppend( ".collect( Collectors.toConcurrentMap(" )
                        .indent()
                            .newlineAppend( "Map.Entry::getKey," )
                            .newlineAppend( "e -> FIELD_MAP.get( e.getKey() ).fromStructures( e.getValue() )" )
                        .dedent()
                        .newlineAppend( ") )" )
                    .dedent()
                .dedent()
                .newlineAppend( ");" );
    }
    
    private void appendBuilderMethods() {
        builder.newline()
            .newlineAppend( "private " ).append( structure.nativeType().wrapperName() ).append( " root() { return " ).append( structure.nativeType() == Native.VOID ? "BasicType.create()" : "root" ).append( "; }" )
            .newlineAppend( "private Map<String, List<StructureType>> children() { return children; }" );
        
        if ( structure.nativeType() != Native.VOID ) {
            builder.newNewlineAppend( "public B root( " ).append( structure.nativeType().wrapperName() ).append( " root ) { return super.root( root ); }" );
            
            structure.nativeType().valueNames().forEach( vn ->
                builder.newlineAppend( "public B root( " ).append( vn ).append( " value ) { return root( BasicType.create( value ) ); }" )
            );

            if ( structure.nativeType() == Native.ANY ) {
                builder.newlineAppend( "public B root( UnaryOperator<" ).append( structure.nativeType().wrapperName() ).append( "> rootOperator ) { return root( rootOperator.apply( root ) ); }" );
            }
            else {
                builder.newlineAppend( "public B root( UnaryOperator<" ).append( structure.nativeType().valueName() ).append( "> valueOperator ) { return root( valueOperator.apply( root.value() ) ); }" );
            }
        }

        methodBuilders.forEach( FieldMethodBuilder::appendSetters );

        builder.newNewlineAppend( "protected " ).append( className ).append( " validatedBuild() throws TypeValidationException" )
            .body( () -> {
                if ( !structure.fields().isEmpty() )
                    builder.newlineAppend( "validateChildren( FIELD_MAP );" ).newline();

                builder.newlineAppend( "return new " ).append( className ).append( "( this );" );                
            } );

        builder.newNewlineAppend( "private static " ).append( className ).append( " buildFrom( Value value ) throws TypeCheckingException" )
            .body( () -> {
                builder.newlineAppend( "InlineBuilder builder = " ).append( className ).append( ".construct();" )
                    .newline()
                    .newlineAppend( "builder.root( " ).append( structure.nativeType().wrapperName().replaceAll( "<.*>", "" ) ).append( ".fromValue( value ) );" );
            
                builder.newNewlineAppend( "for ( Map.Entry<String, ValueVector> child : value.children().entrySet() )" );

				if( structure.fields().isEmpty() )
					builder.indentedNewlineAppend( "builder.put( child.getKey(), child.getValue().stream().parallel().map( StructureType::fromValue ).toList() );" );
				else
					builder.body( () -> builder
						.newlineAppend( "if ( !FIELD_MAP.containsKey( child.getKey() ) )" )
						.indentedNewlineAppend( "throw new TypeCheckingException( \"Unexpected field was set, field \\\"\" + child.getKey() + \"\\\".\" );" )
                        .newNewlineAppend( "builder.put( child.getKey(), FIELD_MAP.get( child.getKey() ).fromValueVector( child.getValue() ) );" ) 
                    );

                builder.newNewlineAppend( "try" ).body( () ->
                    builder.newlineAppend( "return builder.build();" )   
                ).append( " catch ( TypeValidationException e )" ).body( () ->
                    builder.newlineAppend( "throw new TypeCheckingException( e.getMessage() );" )
                );
            } );
    }

    private void appendListBuilders() {
        builder.newNewlineAppend( "static abstract class ListBuilder<B> extends StructureListBuilder<" ).append( className ).append( ", B>" )
            .body( this::appendListBuilderDefinition );

        builder.newNewlineAppend( "public static class InlineListBuilder extends ListBuilder<InlineListBuilder>" )
            .body( () -> builder
                .newline()
                .newlineAppend( "protected InlineListBuilder self() { return this; }" )
                .newline()
                .newlineAppend( "public List<" ).append( className ).append( "> build() { return super.build(); }" ) 
            );

        builder.newNewlineAppend( "public static class NestedListBuilder<T> extends ListBuilder<NestedListBuilder<T>>" )
            .body( () -> builder
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
            .newlineAppend( "protected ListBuilder() {}" )
            .newline()
            .newlineAppend( "public NestedBuilder<B> addConstructed() { return constructNested( this::add ); }" )
            .newlineAppend( "public NestedBuilder<B> setConstructed( int index ) { return constructNested( e -> set( index, e ) ); }" )
            .newlineAppend( "public NestedBuilder<B> addConstructedFrom( JolieType t ) { return constructNested( this::add, t ); }" )
            .newlineAppend( "public NestedBuilder<B> setConstructedFrom( int index, JolieType t ) { return constructNested( e -> set( index, e ), t ); }" )
            .newlineAppend( "public NestedBuilder<B> reconstruct( int index ) { return setConstructedFrom( index, elements.get( index ) ); }" );

        if ( structure.nativeType() != Native.VOID ) {
            builder.newline()
                .newlineAppend( "public NestedBuilder<B> addConstructed( " ).append( structure.nativeType().wrapperName() ).append( " root ) { return addConstructed().root( root ); }" )
                .newlineAppend( "public NestedBuilder<B> setConstructed( int index, " ).append( structure.nativeType().wrapperName() ).append( " root ) { return setConstructed( index ).root( root ); }" );

            structure.nativeType().valueNames().forEach( vn -> 
                builder.newlineAppend( "public NestedBuilder<B> addConstructed( " ).append( vn ).append( " rootValue ) { return addConstructed( BasicType.create( rootValue ) ); }" )
                    .newlineAppend( "public NestedBuilder<B> setConstructed( int index, " ).append( vn ).append( " rootValue ) { return setConstructed( index, BasicType.create( rootValue ) ); }" )
            );

            if ( structure.nativeType() == Native.ANY )
                builder.newlineAppend( "public NestedBuilder<B> reconstruct( int index, UnaryOperator<" ).append( structure.nativeType().wrapperName() ).append( "> rootOperator ) { return reconstruct( index ).root( rootOperator ); }" );
            else
                builder.newlineAppend( "public NestedBuilder<B> reconstruct( int index, UnaryOperator<" ).append( structure.nativeType().valueName() ).append( "> valueOperator ) { return reconstruct( index ).root( valueOperator ); }" );
        }
    }

    private FieldMethodBuilder createFieldMethodBuilder( Structure.Field field ) {
        return field.possibleName().map( n -> {
            final String fieldKeyString = "\"" + field.key() + "\"";
            final String fieldTypeName = switch( field.type() ) {
                case Undefined u -> "StructureType";
                case Basic b -> b.nativeType().valueName();
                default -> field.typeName();
            };

            return field.max() == 1
                ? new SingleFieldMethodBuilder( n, fieldKeyString, fieldTypeName, field.type(), field.min() == 0 ? FieldCard.OPTIONAL : FieldCard.MANDATORY )
                : new VectorFieldMethodBuilder( n, fieldKeyString, fieldTypeName, field.type(), FieldCard.MULTIPLE );
        } ).orElse( null );
    }

    private static enum FieldCard {
        MANDATORY( null, RetrieveMethod.SINGLE, ".get()" ),
        OPTIONAL( "Optional", RetrieveMethod.SINGLE, "" ),
        MULTIPLE( "List", RetrieveMethod.LIST, "" );

        private final String returnTypeWrapper;
        private final RetrieveMethod retrieveMethod;
        private final String retrievalPostfix;

        private FieldCard( String returnTypeWrapper, RetrieveMethod retrieveMethod, String retrievalPostfix ) {
            this.returnTypeWrapper = returnTypeWrapper;
            this.retrieveMethod = retrieveMethod;
            this.retrievalPostfix = retrievalPostfix;
        }

        public Optional<String> returnTypeWrapper() { return Optional.ofNullable( returnTypeWrapper ); }
        public String childRetrievalName() { return retrieveMethod.methodName; }
        public String rootRetrievalName() { return retrieveMethod.methodName + retrieveMethod.rootPostfix; }
        public String valueRetrievalName() { return retrieveMethod.methodName + retrieveMethod.valuePostfix; }
        public String retrievalPostfix() { return retrievalPostfix; }


        private enum RetrieveMethod {
            SINGLE( "firstChild", "Root", "Value" ),
            LIST( "child", "Roots", "Values" );
    
            private final String methodName;
            private final String rootPostfix;
            private final String valuePostfix;
    
            private RetrieveMethod( String methodName, String rootPostfix, String valuePostfix ) {
                this.methodName = methodName;
                this.rootPostfix = rootPostfix;
                this.valuePostfix = valuePostfix;
            }
        }
    }

    private abstract class FieldMethodBuilder {

        protected final String fieldName;
        protected final String fieldKeyString;
        protected final String fieldTypeName;
        protected final JolieType fieldDefinition;
        protected final FieldCard fieldCardinality;

        protected FieldMethodBuilder( String fieldName, String fieldKeyString, String fieldTypeName, JolieType fieldDefinition, FieldCard fieldCardinality ) {
            this.fieldName = fieldName;
            this.fieldKeyString = fieldKeyString;
            this.fieldTypeName = fieldTypeName;
            this.fieldDefinition = fieldDefinition;
            this.fieldCardinality = fieldCardinality;
        }

        public void appendGetter() {
            if ( fieldDefinition instanceof Native n && n == Native.VOID )
                return;

            final String returnType = fieldCardinality.returnTypeWrapper().map( w -> w + "<" + fieldTypeName + ">" ).orElse( fieldTypeName );

            final String retrieveCall = switch ( fieldDefinition ) {
                case Native n -> switch ( n ) {
                    case ANY        -> buildRetrieveCall( fieldCardinality.rootRetrievalName(), null );
                    default         -> buildRetrieveCall( fieldCardinality.valueRetrievalName(), n.wrapperName() );
                };
                case Undefined u    -> buildRetrieveCall( fieldCardinality.childRetrievalName(), null );
                case Basic b        -> buildRetrieveCall( fieldCardinality.valueRetrievalName(), b.nativeType().wrapperName() );
                case Definition d -> buildRetrieveCall( fieldCardinality.childRetrievalName(), d.name() );
            };

            builder.newlineAppend( "public " ).append( returnType ).append( " " ).append( fieldName ).append( "() { return " ).append( retrieveCall ).append( "; }" );
        }

        private String buildRetrieveCall( String retrieveMethod, String typeCast ) {
            final StringBuilder b = new StringBuilder();
            final String args = Optional.ofNullable( typeCast ).map( t -> fieldKeyString + ", " + t + ".class" ).orElse( fieldKeyString );
            return b.append( retrieveMethod ).append( "( " ).append( args ).append( " )" ).append( fieldCardinality.retrievalPostfix() ).toString();
        }

        public abstract void appendSetters();
    }

    private class SingleFieldMethodBuilder extends FieldMethodBuilder {

        private final String fieldMethodName;

        public SingleFieldMethodBuilder( String fieldName, String fieldKeyString, String fieldTypeName, JolieType fieldDefinition, FieldCard fieldCardinality ) {
            super( fieldName, fieldKeyString, fieldTypeName, fieldDefinition, fieldCardinality );
            fieldMethodName = StringUtils.capitalize( fieldName );
        }

        public void appendSetters() {
            builder.newline();

            switch ( fieldDefinition ) {
                case Native n ->    appendValueSetters( n.wrapperName(), "rootEntry", n, "BasicType" );
                case Basic b ->     appendValueSetters( b.name(), "childEntry", b.nativeType(), b.name() );

                case Undefined u -> appendStructureSetters( u.name(), "childEntry", "StructureType", Native.ANY );
                case Structure s -> appendStructureSetters( s.name(), "childEntry", s.name(), s.nativeType() );
                
                case Choice c ->    appendChoiceSetters( c );
            }
        }

        private void appendStructureSetters( String setType, String setName, String className, Native nativeType ) {
            appendSetMethod( setType, setName );
            appendConstructMethods( className, nativeType );
        }

        private void appendValueSetters( String setType, String setName, Native type, String wrapperName ) {
            appendSetMethod( setType, setName );
            if ( type != Native.VOID ) {
                type.valueNames().forEach( vn -> appendSetMethod( vn, "valueEntry", wrapperName, "" ) );
                appendReplaceMethod( type, wrapperName );
            }
        }

        private void appendChoiceSetters( Choice choice ) {
            appendSetMethod( choice.name(), "childEntry" );
            choice.numberedOptions().forKeyValue( (i,t) -> {
                final String postfix = String.valueOf( i );
                appendSetMethod( 
                    switch( t ) {
                        case Native n -> n.valueName();
                        case Basic.Inline b -> b.nativeType().valueName();
                        case Definition d -> d.name();
                    }, 
                    "optionEntry", 
                    choice.name(), 
                    postfix 
                );
                if ( t instanceof Structure s )
                    appendConstructMethods( s.name(), s.nativeType(), postfix );
            } );
        }

        private void appendSetMethod( String paramType, String paramName ) { appendSetMethod( paramType, paramName, null, "" ); }
        private void appendSetMethod( String paramType, String paramName, String wrapperName, String methodPostfix ) {
            final Optional<String> w = Optional.ofNullable( wrapperName );
            builder.newlineAppend( "public B set" ).append( fieldMethodName ).append( methodPostfix ).append( "( " ).append( paramType ).append( " " ).append( paramName ).append( " ) { return putAs( " ).append( fieldKeyString ).append( ", " ).append( paramName ).append( w.map( t -> ", " + t + "::create" + methodPostfix ).orElse( "" ) ).append( " ); }" );
        }

        private void appendConstructMethods( String className, Native nativeType ) { appendConstructMethods( className, nativeType, "" ); }
        private void appendConstructMethods( String className, Native nativeType, String methodPostfix ) {
            final String methodName = fieldMethodName + methodPostfix;
            
            builder.newlineAppend( "public " ).append( className ).append( ".NestedBuilder<B> construct" ).append( methodName ).append( "() { return " ).append( className ).append( ".constructNested( this::set" ).append( methodName ).append( " ); }" );
            if ( nativeType != Native.VOID ) {
                builder.newlineAppend( "public " ).append( className ).append( ".NestedBuilder<B> construct" ).append( methodName ).append( "( " ).append( nativeType.wrapperName() ).append( " root ) { return construct" ).append( methodName ).append( "().root( root ); }" );
                nativeType.valueNames().forEach( vn -> builder.newlineAppend( "public " ).append( className ).append( ".NestedBuilder<B> construct" ).append( methodName ).append( "( " ).append( vn ).append( " rootValue ) { return construct" ).append( methodName ).append( "().root( rootValue ); }" ) );
            }

            builder.newlineAppend( "public " ).append( className ).append( ".NestedBuilder<B> construct" ).append( methodName ).append( "From( JolieType t ) { return " ).append( className ).append( ".constructNested( this::set" ).append( methodName ).append( ", t ); }" );
            
            builder.newlineAppend( "public " ).append( className ).append( ".NestedBuilder<B> reconstruct" ).append( methodName ).append( "() { return firstChild( " ).append( fieldKeyString ).append( " ).map( e -> construct" ).append( methodName ).append( "From( e ) ).orElse( construct" ).append( methodName ).append( "() ); }" );
            if ( nativeType == Native.ANY )
                builder.newlineAppend( "public " ).append( className ).append( ".NestedBuilder<B> reconstruct" ).append( methodName ).append( "( UnaryOperator<" ).append( nativeType.wrapperName() ).append( "> rootOperator ) { return reconstruct" ).append( methodName ).append( "().root( rootOperator ); }" );
            else if ( nativeType != Native.VOID )
                builder.newlineAppend( "public " ).append( className ).append( ".NestedBuilder<B> reconstruct" ).append( methodName ).append( "( UnaryOperator<" ).append( nativeType.valueName() ).append( "> valueOperator ) { return reconstruct" ).append( methodName ).append( "().root( valueOperator ); }" );
        }

        private void appendReplaceMethod( Native valueType, String wrapperName ) {
            if ( valueType == Native.ANY )
                builder.newlineAppend( "public B replace" ).append( fieldMethodName ).append( "( UnaryOperator<" ).append( valueType.wrapperName() ).append( "> rootOperator ) { return computeAs( " ).append( fieldKeyString ).append( ", (n,s) -> rootOperator.apply( s.root() ) ); }" );
            else if ( valueType != Native.VOID )
                builder.newlineAppend( "public B replace" ).append( fieldMethodName ).append( "( UnaryOperator<" ).append( valueType.valueName() ).append( "> valueOperator ) { return computeAs( " ).append( fieldKeyString ).append( ", (n,v) -> valueOperator.apply( v ), s -> " ).append( valueType.wrapperName() ).append( ".class.cast( s.root() ).value(), " ).append( wrapperName ).append( "::create ); }" );
        }
    }

    private class VectorFieldMethodBuilder extends FieldMethodBuilder {

        private final String fieldMethodName;

        public VectorFieldMethodBuilder( String fieldName, String fieldKeyString, String fieldTypeName, JolieType fieldDefinition, FieldCard fieldCardinality ) {
            super( fieldName, fieldKeyString, fieldTypeName, fieldDefinition, fieldCardinality );
            fieldMethodName = StringUtils.capitalize( fieldName );
        }

        public void appendSetters() {
            builder.newline();

            switch ( fieldDefinition ) {
                case Native n -> {
                    if ( n == Native.ANY )  appendConstructibleListSetters( "BasicType", "SequencedCollection<? extends BasicType<?>>", "childRoots" );
                    else                    appendValueListSetters( n, "BasicType" );
                }
                case Undefined u ->         appendConstructibleListSetters( "StructureType", "SequencedCollection<? extends " + u.name() + ">", "child" );
                case Basic b ->             appendValueListSetters( b.nativeType(), b.name() );
                case Definition d ->        appendConstructibleListSetters( d.name(), "SequencedCollection<? extends " + d.name() + ">", "child" );
            }
        }

        private void appendConstructibleListSetters( String className, String paramType, String paramName ) {
            builder.newlineAppend( "public B set" ).append( fieldMethodName ).append( "( " ).append( paramType ).append( " " ).append( paramName ).append( " ) { return put( " ).append( fieldKeyString ).append( ", " ).append( paramName ).append( " ); }" )
                .newlineAppend( "public " ).append( className ).append( ".NestedListBuilder<B> construct" ).append( fieldMethodName ).append( "() { return " ).append( className ).append( ".constructNestedList( this::set" ).append( fieldMethodName ).append( " ); }" )
                .newlineAppend( "public " ).append( className ).append( ".NestedListBuilder<B> construct" ).append( fieldMethodName ).append( "From( SequencedCollection<? extends JolieType> c ) { return " ).append( className ).append( ".constructNestedList( this::set" ).append( fieldMethodName ).append( ", c ); }" )
                .newlineAppend( "public " ).append( className ).append( ".NestedListBuilder<B> reconstruct" ).append( fieldMethodName ).append( "() { return Optional.ofNullable( child( " ).append( fieldKeyString ).append( " ) ).map( c -> construct" ).append( fieldMethodName ).append( "From( c ) ).orElse( construct" ).append( fieldMethodName ).append( "() ); }" );
        }

        private void appendValueListSetters( Native nativeType, String wrapperName ) {
            if ( nativeType != Native.VOID )
                builder.newlineAppend( "public B set" ).append( fieldMethodName ).append( "( SequencedCollection<" ).append( nativeType.valueName() ).append( "> values ) { return put( " ).append( fieldKeyString ).append( ", values, " ).append( wrapperName ).append( "::create ); }" )
                    .newlineAppend( "public B set" ).append( fieldMethodName ).append( "( " ).append( nativeType.valueName() ).append( "... values ) { return set" ).append( fieldMethodName ).append( "( List.of( values ) ); }" )
                    .newlineAppend( "public B merge" ).append( fieldMethodName ).append( "( SequencedCollection<" ).append( nativeType.valueName() ).append( "> values, BinaryOperator<SequencedCollection<" ).append( nativeType.valueName() ).append( ">> valuesOperator ) { return compute( " ).append( fieldKeyString ).append( ", (n,c) -> c == null ? values : valuesOperator.apply( c, values ), s -> " ).append( nativeType.wrapperName() ).append( ".class.cast( s.root() ).value(), " ).append( wrapperName ).append( "::create ); }" );
            else
                builder.newlineAppend( "public B set" ).append( fieldMethodName ).append( "( SequencedCollection<" ).append( nativeType.wrapperName() ).append( "> roots ) { return put( " ).append( fieldKeyString ).append( ", roots ); }" );
        }
    }
}
