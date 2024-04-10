package joliex.util.types;

import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.ByteArray;
import jolie.runtime.typing.TypeCheckingException;

import java.util.ArrayList;
import java.util.Map;
import java.util.SequencedCollection;
import java.util.List;
import java.util.Optional;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.function.BinaryOperator;
import java.util.stream.Stream;
import java.util.stream.Collectors;

import joliex.java.embedding.*;
import joliex.java.embedding.JolieNative.*;
import joliex.java.embedding.util.*;

/**
 * this class is a choice type which can be described as follows:
 * 
 * <pre>
 * FormatRequest: FormatRequest.S1 | FormatRequest.S2
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 * 
 * @see FormatRequest.S1
 * @see #create1(FormatRequest.S1)
 * @see #construct1()
 * 
 * @see FormatRequest.S2
 * @see #create2(FormatRequest.S2)
 * @see #construct2()
 */
public sealed interface FormatRequest extends JolieValue {
    
    public static record C1( FormatRequest.S1 option ) implements FormatRequest {
        
        public JolieString content() { return option.content(); }
        public Map<String, List<JolieValue>> children() { return option.children(); }
        
        public boolean equals( Object obj ) { return obj instanceof JolieValue j && content().equals( j.content() ) && children().equals( j.children() ); }
        public int hashCode() { return option.hashCode(); }
        public String toString() { return option.toString(); }
        
        public static InlineBuilder construct() { return new InlineBuilder(); }
        
        public static InlineBuilder constructFrom( JolieValue t ) { return new InlineBuilder( t ); }
        public static C1 createFrom( JolieValue t ) throws TypeValidationException { return constructFrom( t ).build(); }
        
        public static Value toValue( C1 t ) { return JolieValue.toValue( t ); }
        public static C1 fromValue( Value value ) throws TypeCheckingException { return new C1( FormatRequest.S1.fromValue( value ) ); }
        
        public static class InlineBuilder extends FormatRequest.S1.Builder<InlineBuilder> {
            
            private InlineBuilder() {}
            private InlineBuilder( JolieValue t ) { super( t ); }
            
            protected InlineBuilder self() { return this; }
            
            public C1 build() throws TypeValidationException { return new C1( validatedBuild() ); }
        }
    }
    
    public static record C2( FormatRequest.S2 option ) implements FormatRequest {
        
        public JolieVoid content() { return option.content(); }
        public Map<String, List<JolieValue>> children() { return option.children(); }
        
        public boolean equals( Object obj ) { return obj instanceof JolieValue j && content().equals( j.content() ) && children().equals( j.children() ); }
        public int hashCode() { return option.hashCode(); }
        public String toString() { return option.toString(); }
        
        public static InlineBuilder construct() { return new InlineBuilder(); }
        
        public static InlineBuilder constructFrom( JolieValue t ) { return new InlineBuilder( t ); }
        public static C2 createFrom( JolieValue t ) throws TypeValidationException { return constructFrom( t ).build(); }
        
        public static Value toValue( C2 t ) { return JolieValue.toValue( t ); }
        public static C2 fromValue( Value value ) throws TypeCheckingException { return new C2( FormatRequest.S2.fromValue( value ) ); }
        
        public static class InlineBuilder extends FormatRequest.S2.Builder<InlineBuilder> {
            
            private InlineBuilder() {}
            private InlineBuilder( JolieValue t ) { super( t ); }
            
            protected InlineBuilder self() { return this; }
            
            public C2 build() throws TypeValidationException { return new C2( validatedBuild() ); }
        }
    }
    
    public static FormatRequest create1( FormatRequest.S1 option ) { return new C1( option ); }
    
    public static C1.InlineBuilder construct1() { return C1.construct(); }
    public static C1.InlineBuilder construct1( JolieString content ) { return construct1().content( content ); }
    public static C1.InlineBuilder construct1( String contentValue ) { return construct1().content( contentValue ); }
    public static C1.InlineBuilder construct1From( JolieValue t ) { return C1.constructFrom( t ); }
    public static C1 create1From( JolieValue t ) throws TypeValidationException { return C1.createFrom( t ); }
    
    public static FormatRequest create2( FormatRequest.S2 option ) { return new C2( option ); }
    
    public static C2.InlineBuilder construct2() { return C2.construct(); }
    public static C2.InlineBuilder construct2From( JolieValue t ) { return C2.constructFrom( t ); }
    public static C2 create2From( JolieValue t ) throws TypeValidationException { return C2.createFrom( t ); }
    
    static InlineListBuilder constructList() { return new InlineListBuilder(); }
    static <T> NestedListBuilder<T> constructNestedList( Function<List<FormatRequest>, T> doneFunc, SequencedCollection<? extends JolieValue> c ) { return new NestedListBuilder<>( doneFunc, c ); }
    static <T> NestedListBuilder<T> constructNestedList( Function<List<FormatRequest>, T> doneFunc ) { return new NestedListBuilder<>( doneFunc ); }
    
    public static FormatRequest createFrom( JolieValue t ) throws TypeValidationException {
        return Stream.<Function<JolieValue, FormatRequest>>of(
            C1::createFrom,
            C2::createFrom
        )
            .map( f -> {
                try {
                    return f.apply( t );
                } catch ( TypeValidationException e ) {
                    return null;
                }
            } )
            .filter( Objects::nonNull )
            .findFirst()
            .orElseThrow( () -> new TypeValidationException( "The given JolieValue couldn't be converted to any of the option types." ) );
    }
    
    public static Value toValue( FormatRequest t ) { return JolieValue.toValue( t ); }
    public static FormatRequest fromValue( Value value ) throws TypeCheckingException {
        return Stream.<ConversionFunction<Value, FormatRequest>>of(
            C1::fromValue,
            C2::fromValue
        )
            .map( f -> {
                try {
                    return f.apply( value );
                } catch ( TypeCheckingException e ) {
                    return null;
                }
            } )
            .filter( Objects::nonNull )
            .findFirst()
            .orElseThrow( () -> new TypeCheckingException( "The given Value couldn't be converted to any of the option types." ) );
    }
    
    static abstract class ListBuilder<B> extends StructureListBuilder<FormatRequest, B> {
        
        protected ListBuilder( SequencedCollection<? extends JolieValue> elements ) { super( elements.parallelStream().map( FormatRequest::createFrom ).toList() ); }
        protected ListBuilder() {}
        
        public B add1( FormatRequest.S1 option ) { return add( create1( option ) ); }
        public B set1( int index, FormatRequest.S1 option ) { return set( index, create1( option ) ); }
        public FormatRequest.S1.NestedBuilder<B> addConstructed1() { return FormatRequest.S1.constructNested( this::add1 ); }
        public FormatRequest.S1.NestedBuilder<B> addConstructed1From( JolieValue t ) { return FormatRequest.S1.constructNested( this::add1, t ); }
        public FormatRequest.S1.NestedBuilder<B> setConstructed1( int index ) { return FormatRequest.S1.constructNested( option -> set1( index, option ) ); }
        public FormatRequest.S1.NestedBuilder<B> setConstructed1From( int index, JolieValue t ) { return FormatRequest.S1.constructNested( option -> set1( index, option ), t ); }
        public FormatRequest.S1.NestedBuilder<B> reconstruct1( int index ) { return FormatRequest.S1.constructNested( option -> set1( index, option ), elements.get( index ) ); }
        public FormatRequest.S1.NestedBuilder<B> addConstructed1( JolieString content ) { return addConstructed1().content( content ); }
        public FormatRequest.S1.NestedBuilder<B> setConstructed1( int index, JolieString content ) { return setConstructed1( index ).content( content ); }
        public FormatRequest.S1.NestedBuilder<B> addConstructed1( String contentValue ) { return addConstructed1( JolieNative.create( contentValue ) ); }
        public FormatRequest.S1.NestedBuilder<B> setConstructed1( int index, String contentValue ) { return setConstructed1( index, JolieNative.create( contentValue ) ); }
        public FormatRequest.S1.NestedBuilder<B> reconstruct1( int index, UnaryOperator<String> valueOperator ) { return reconstruct1( index ).content( valueOperator ); }
        
        public B add2( FormatRequest.S2 option ) { return add( create2( option ) ); }
        public B set2( int index, FormatRequest.S2 option ) { return set( index, create2( option ) ); }
        public FormatRequest.S2.NestedBuilder<B> addConstructed2() { return FormatRequest.S2.constructNested( this::add2 ); }
        public FormatRequest.S2.NestedBuilder<B> addConstructed2From( JolieValue t ) { return FormatRequest.S2.constructNested( this::add2, t ); }
        public FormatRequest.S2.NestedBuilder<B> setConstructed2( int index ) { return FormatRequest.S2.constructNested( option -> set2( index, option ) ); }
        public FormatRequest.S2.NestedBuilder<B> setConstructed2From( int index, JolieValue t ) { return FormatRequest.S2.constructNested( option -> set2( index, option ), t ); }
        public FormatRequest.S2.NestedBuilder<B> reconstruct2( int index ) { return FormatRequest.S2.constructNested( option -> set2( index, option ), elements.get( index ) ); }
    }
    
    public static class InlineListBuilder extends ListBuilder<InlineListBuilder> {
        
        protected InlineListBuilder self() { return this; }
        
        public List<FormatRequest> build() { return super.build(); }
    }
    
    public static class NestedListBuilder<T> extends ListBuilder<NestedListBuilder<T>> {
        
        private final Function<List<FormatRequest>, T> doneFunc;
        
        private NestedListBuilder( Function<List<FormatRequest>, T> doneFunc, SequencedCollection<? extends JolieValue> c ) { super( c ); this.doneFunc = doneFunc; }
        private NestedListBuilder( Function<List<FormatRequest>, T> doneFunc ) { this.doneFunc = doneFunc; }
        
        protected NestedListBuilder<T> self() { return this; }
        
        public T done() throws TypeValidationException { return doneFunc.apply( build() ); }
    }
    
    
    /**
     * this class is an {@link ImmutableStructure} which can be described as follows:
     * 
     * <pre>
     * content: {@link String} { ? }
     * </pre>
     * 
     * @see JolieValue
     * @see JolieNative
     * @see #construct()
     */
    public static final class S1 extends ImmutableStructure<JolieString> {
        
        private S1( Builder<?> builder ) {
            super( builder.content(), builder.children() );
        }
        
        public static InlineBuilder construct() { return new InlineBuilder(); }
        public static InlineBuilder construct( JolieString content ) { return construct().content( content ); }
        public static InlineBuilder construct( String contentValue ) { return construct().content( contentValue ); }
        
        static <T> NestedBuilder<T> constructNested( Function<S1, T> doneFunc ) { return new NestedBuilder<>( doneFunc ); }
        static <T> NestedBuilder<T> constructNested( Function<S1, T> doneFunc, JolieValue t ) { return new NestedBuilder<>( doneFunc, t ); }
        
        public static InlineBuilder constructFrom( JolieValue t ) { return new InlineBuilder( t ); }
        
        public static S1 createFrom( JolieValue t ) throws TypeValidationException { return constructFrom( t ).build(); }
        
        public static Value toValue( S1 t ) { return JolieValue.toValue( t ); }
        public static S1 fromValue( Value value ) throws TypeCheckingException { return Builder.buildFrom( value ); }
        
        static abstract class Builder<B> extends UntypedBuilder<JolieString, B> {
            
            protected Builder() {}
            protected Builder( JolieValue structure ) {
                super( structure.content() instanceof JolieString content ? content : null, structure.children() );
            }
            
            private JolieString content() { return content; }
            private Map<String, List<JolieValue>> children() { return children; }
            
            public B content( JolieString content ) { return super.content( content ); }
            public B content( String value ) { return content( JolieNative.create( value ) ); }
            public B content( UnaryOperator<String> valueOperator ) { return content( valueOperator.apply( content.value() ) ); }
            
            protected S1 validatedBuild() throws TypeValidationException {
                return new S1( this );
            }
            
            private static S1 buildFrom( Value value ) throws TypeCheckingException {
                InlineBuilder builder = S1.construct();
                
                builder.content( JolieString.fromValue( value ) );
                
                for ( Map.Entry<String, ValueVector> child : value.children().entrySet() )
                    builder.put( child.getKey(), child.getValue().stream().parallel().map( JolieValue::fromValue ).toList() );
                
                try {
                    return builder.build();
                } catch ( TypeValidationException e ) {
                    throw new TypeCheckingException( e.getMessage() );
                }
            }
        }
        
        public static class InlineBuilder extends Builder<InlineBuilder> {
            
            private InlineBuilder() {}
            private InlineBuilder( JolieValue t ) { super( t ); }
            
            protected InlineBuilder self() { return this; }
            
            public S1 build() throws TypeValidationException { return validatedBuild(); }
        }
        
        public static class NestedBuilder<T> extends Builder<NestedBuilder<T>> {
            
            private final Function<S1, T> doneFunc;
            
            private NestedBuilder( Function<S1, T> doneFunc, JolieValue t ) { super( t ); this.doneFunc = doneFunc; }
            private NestedBuilder( Function<S1, T> doneFunc ) { this.doneFunc = doneFunc; }
            
            protected NestedBuilder<T> self() { return this; }
            
            public T done() throws TypeValidationException { return doneFunc.apply( validatedBuild() ); }
        }
    }
    
    
    /**
     * this class is an {@link ImmutableStructure} which can be described as follows:
     * 
     * <pre>
     * content: {@link Void}
     *     data: {@link Data}
     *     format: {@link String}
     *     locale: {@link String}
     * </pre>
     * 
     * @see JolieValue
     * @see JolieNative
     * @see Data
     * @see #construct()
     */
    public static final class S2 extends ImmutableStructure<JolieVoid> {
        
        public Data data() { return getFirstChild( "data", Data.class ).get(); }
        public String format() { return getFirstChildValue( "format", JolieString.class ).get(); }
        public String locale() { return getFirstChildValue( "locale", JolieString.class ).get(); }
        
        private S2( Builder<?> builder ) {
            super( builder.content(), builder.children() );
        }
        
        public static InlineBuilder construct() { return new InlineBuilder(); }
        
        static <T> NestedBuilder<T> constructNested( Function<S2, T> doneFunc ) { return new NestedBuilder<>( doneFunc ); }
        static <T> NestedBuilder<T> constructNested( Function<S2, T> doneFunc, JolieValue t ) { return new NestedBuilder<>( doneFunc, t ); }
        
        public static InlineBuilder constructFrom( JolieValue t ) { return new InlineBuilder( t ); }
        
        public static S2 createFrom( JolieValue t ) throws TypeValidationException { return constructFrom( t ).build(); }
        
        public static Value toValue( S2 t ) { return JolieValue.toValue( t ); }
        public static S2 fromValue( Value value ) throws TypeCheckingException { return Builder.buildFrom( value ); }
        
        static abstract class Builder<B> extends StructureBuilder<JolieVoid, B> {
            
            private static final Map<String,FieldManager<?>> FIELD_MAP = Map.of(
                "data", FieldManager.createCustom( Data::fromValue, Data::createFrom ),
                "format", FieldManager.createNative( JolieString::fromValue, JolieString::createFrom ),
                "locale", FieldManager.createNative( JolieString::fromValue, JolieString::createFrom )
            );
            
            protected Builder() {}
            protected Builder( JolieValue structure ) {
                super(
                    null,
                    structure.children()
                        .entrySet()
                        .parallelStream()
                        .filter( e -> FIELD_MAP.containsKey( e.getKey() ) )
                        .collect( Collectors.toConcurrentMap(
                            Map.Entry::getKey,
                            e -> FIELD_MAP.get( e.getKey() ).fromJolieValues( e.getValue() )
                        ) )
                );
            }
            
            private JolieVoid content() { return JolieNative.create(); }
            private Map<String, List<JolieValue>> children() { return children; }
            
            public B setData( Data childEntry ) { return putAs( "data", childEntry ); }
            public Data.NestedBuilder<B> constructData() { return Data.constructNested( this::setData ); }
            public Data.NestedBuilder<B> constructDataFrom( JolieValue t ) { return Data.constructNested( this::setData, t ); }
            public Data.NestedBuilder<B> reconstructData() { return firstChild( "data" ).map( e -> constructDataFrom( e ) ).orElse( constructData() ); }
            
            public B setFormat( JolieString contentEntry ) { return putAs( "format", contentEntry, JolieValue::create ); }
            public B setFormat( String valueEntry ) { return putAs( "format", valueEntry, JolieValue::create ); }
            public B replaceFormat( UnaryOperator<String> valueOperator ) { return computeAs( "format", (n,v) -> valueOperator.apply( v ), s -> JolieString.class.cast( s.content() ).value(), JolieValue::create ); }
            
            public B setLocale( JolieString contentEntry ) { return putAs( "locale", contentEntry, JolieValue::create ); }
            public B setLocale( String valueEntry ) { return putAs( "locale", valueEntry, JolieValue::create ); }
            public B replaceLocale( UnaryOperator<String> valueOperator ) { return computeAs( "locale", (n,v) -> valueOperator.apply( v ), s -> JolieString.class.cast( s.content() ).value(), JolieValue::create ); }
            
            protected S2 validatedBuild() throws TypeValidationException {
                validateChildren( FIELD_MAP );
                
                return new S2( this );
            }
            
            private static S2 buildFrom( Value value ) throws TypeCheckingException {
                InlineBuilder builder = S2.construct();
                
                builder.content( JolieVoid.fromValue( value ) );
                
                for ( Map.Entry<String, ValueVector> child : value.children().entrySet() ) {
                    if ( !FIELD_MAP.containsKey( child.getKey() ) )
                        throw new TypeCheckingException( "Unexpected field was set, field \"" + child.getKey() + "\"." );
                    
                    builder.put( child.getKey(), FIELD_MAP.get( child.getKey() ).fromValueVector( child.getValue() ) );
                }
                
                try {
                    return builder.build();
                } catch ( TypeValidationException e ) {
                    throw new TypeCheckingException( e.getMessage() );
                }
            }
        }
        
        public static class InlineBuilder extends Builder<InlineBuilder> {
            
            private InlineBuilder() {}
            private InlineBuilder( JolieValue t ) { super( t ); }
            
            protected InlineBuilder self() { return this; }
            
            public S2 build() throws TypeValidationException { return validatedBuild(); }
        }
        
        public static class NestedBuilder<T> extends Builder<NestedBuilder<T>> {
            
            private final Function<S2, T> doneFunc;
            
            private NestedBuilder( Function<S2, T> doneFunc, JolieValue t ) { super( t ); this.doneFunc = doneFunc; }
            private NestedBuilder( Function<S2, T> doneFunc ) { this.doneFunc = doneFunc; }
            
            protected NestedBuilder<T> self() { return this; }
            
            public T done() throws TypeValidationException { return doneFunc.apply( validatedBuild() ); }
        }
        
        
        /**
         * this class is an {@link ImmutableStructure} which can be described as follows:
         * 
         * <pre>
         * content: {@link Void} { ? }
         * </pre>
         * 
         * @see JolieValue
         * @see JolieNative
         * @see #construct()
         */
        public static final class Data extends ImmutableStructure<JolieVoid> {
            
            private Data( Builder<?> builder ) {
                super( builder.content(), builder.children() );
            }
            
            public static InlineBuilder construct() { return new InlineBuilder(); }
            
            static <T> NestedBuilder<T> constructNested( Function<Data, T> doneFunc ) { return new NestedBuilder<>( doneFunc ); }
            static <T> NestedBuilder<T> constructNested( Function<Data, T> doneFunc, JolieValue t ) { return new NestedBuilder<>( doneFunc, t ); }
            
            public static InlineBuilder constructFrom( JolieValue t ) { return new InlineBuilder( t ); }
            
            public static Data createFrom( JolieValue t ) throws TypeValidationException { return constructFrom( t ).build(); }
            
            public static Value toValue( Data t ) { return JolieValue.toValue( t ); }
            public static Data fromValue( Value value ) throws TypeCheckingException { return Builder.buildFrom( value ); }
            
            static abstract class Builder<B> extends UntypedBuilder<JolieVoid, B> {
                
                protected Builder() {}
                protected Builder( JolieValue structure ) {
                    super( null, structure.children() );
                }
                
                private JolieVoid content() { return JolieNative.create(); }
                private Map<String, List<JolieValue>> children() { return children; }
                
                protected Data validatedBuild() throws TypeValidationException {
                    return new Data( this );
                }
                
                private static Data buildFrom( Value value ) throws TypeCheckingException {
                    InlineBuilder builder = Data.construct();
                    
                    builder.content( JolieVoid.fromValue( value ) );
                    
                    for ( Map.Entry<String, ValueVector> child : value.children().entrySet() )
                        builder.put( child.getKey(), child.getValue().stream().parallel().map( JolieValue::fromValue ).toList() );
                    
                    try {
                        return builder.build();
                    } catch ( TypeValidationException e ) {
                        throw new TypeCheckingException( e.getMessage() );
                    }
                }
            }
            
            public static class InlineBuilder extends Builder<InlineBuilder> {
                
                private InlineBuilder() {}
                private InlineBuilder( JolieValue t ) { super( t ); }
                
                protected InlineBuilder self() { return this; }
                
                public Data build() throws TypeValidationException { return validatedBuild(); }
            }
            
            public static class NestedBuilder<T> extends Builder<NestedBuilder<T>> {
                
                private final Function<Data, T> doneFunc;
                
                private NestedBuilder( Function<Data, T> doneFunc, JolieValue t ) { super( t ); this.doneFunc = doneFunc; }
                private NestedBuilder( Function<Data, T> doneFunc ) { this.doneFunc = doneFunc; }
                
                protected NestedBuilder<T> self() { return this; }
                
                public T done() throws TypeValidationException { return doneFunc.apply( validatedBuild() ); }
            }
        }
    }
}