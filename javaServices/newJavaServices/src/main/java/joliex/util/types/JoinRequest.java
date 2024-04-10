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
 * this class is an {@link ImmutableStructure} which can be described as follows:
 * 
 * <pre>
 * content: {@link Void}
 *     piece[0,2147483647]: {@link String}
 *     delimiter: {@link String}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 * @see #construct()
 */
public final class JoinRequest extends ImmutableStructure<JolieVoid> {
    
    public List<String> piece() { return getChildValues( "piece", JolieString.class ); }
    public String delimiter() { return getFirstChildValue( "delimiter", JolieString.class ).get(); }
    
    private JoinRequest( Builder<?> builder ) {
        super( builder.content(), builder.children() );
    }
    
    public static InlineBuilder construct() { return new InlineBuilder(); }
    
    static <T> NestedBuilder<T> constructNested( Function<JoinRequest, T> doneFunc ) { return new NestedBuilder<>( doneFunc ); }
    static <T> NestedBuilder<T> constructNested( Function<JoinRequest, T> doneFunc, JolieValue t ) { return new NestedBuilder<>( doneFunc, t ); }
    
    static InlineListBuilder constructList() { return new InlineListBuilder(); }
    
    static <T> NestedListBuilder<T> constructNestedList( Function<List<JoinRequest>, T> doneFunc ) { return new NestedListBuilder<>( doneFunc ); }
    static <T> NestedListBuilder<T> constructNestedList( Function<List<JoinRequest>, T> doneFunc, SequencedCollection<? extends JolieValue> c ) { return new NestedListBuilder<>( doneFunc, c ); }
    
    public static InlineBuilder constructFrom( JolieValue t ) { return new InlineBuilder( t ); }
    
    public static JoinRequest createFrom( JolieValue t ) throws TypeValidationException { return constructFrom( t ).build(); }
    
    public static Value toValue( JoinRequest t ) { return JolieValue.toValue( t ); }
    public static JoinRequest fromValue( Value value ) throws TypeCheckingException { return Builder.buildFrom( value ); }
    
    static abstract class Builder<B> extends StructureBuilder<JolieVoid, B> {
        
        private static final Map<String,FieldManager<?>> FIELD_MAP = Map.of(
            "piece", FieldManager.createNative( 0, 2147483647, JolieString::fromValue, JolieString::createFrom ),
            "delimiter", FieldManager.createNative( JolieString::fromValue, JolieString::createFrom )
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
        
        public B setPiece( SequencedCollection<String> values ) { return put( "piece", values, JolieValue::create ); }
        public B setPiece( String... values ) { return setPiece( List.of( values ) ); }
        public B mergePiece( SequencedCollection<String> values, BinaryOperator<SequencedCollection<String>> valuesOperator ) { return compute( "piece", (n,c) -> c == null ? values : valuesOperator.apply( c, values ), s -> JolieString.class.cast( s.content() ).value(), JolieValue::create ); }
        
        public B setDelimiter( JolieString contentEntry ) { return putAs( "delimiter", contentEntry, JolieValue::create ); }
        public B setDelimiter( String valueEntry ) { return putAs( "delimiter", valueEntry, JolieValue::create ); }
        public B replaceDelimiter( UnaryOperator<String> valueOperator ) { return computeAs( "delimiter", (n,v) -> valueOperator.apply( v ), s -> JolieString.class.cast( s.content() ).value(), JolieValue::create ); }
        
        protected JoinRequest validatedBuild() throws TypeValidationException {
            validateChildren( FIELD_MAP );
            
            return new JoinRequest( this );
        }
        
        private static JoinRequest buildFrom( Value value ) throws TypeCheckingException {
            InlineBuilder builder = JoinRequest.construct();
            
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
        
        public JoinRequest build() throws TypeValidationException { return validatedBuild(); }
    }
    
    public static class NestedBuilder<T> extends Builder<NestedBuilder<T>> {
        
        private final Function<JoinRequest, T> doneFunc;
        
        private NestedBuilder( Function<JoinRequest, T> doneFunc, JolieValue t ) { super( t ); this.doneFunc = doneFunc; }
        private NestedBuilder( Function<JoinRequest, T> doneFunc ) { this.doneFunc = doneFunc; }
        
        protected NestedBuilder<T> self() { return this; }
        
        public T done() throws TypeValidationException { return doneFunc.apply( validatedBuild() ); }
    }
    
    static abstract class ListBuilder<B> extends StructureListBuilder<JoinRequest, B> {
        
        protected ListBuilder( SequencedCollection<? extends JolieValue> elements ) { super( elements.parallelStream().map( JoinRequest::createFrom ).toList() ); }
        protected ListBuilder() {}
        
        public NestedBuilder<B> addConstructed() { return constructNested( this::add ); }
        public NestedBuilder<B> setConstructed( int index ) { return constructNested( e -> set( index, e ) ); }
        public NestedBuilder<B> addConstructedFrom( JolieValue t ) { return constructNested( this::add, t ); }
        public NestedBuilder<B> setConstructedFrom( int index, JolieValue t ) { return constructNested( e -> set( index, e ), t ); }
        public NestedBuilder<B> reconstruct( int index ) { return setConstructedFrom( index, elements.get( index ) ); }
    }
    
    public static class InlineListBuilder extends ListBuilder<InlineListBuilder> {
        
        protected InlineListBuilder self() { return this; }
        
        public List<JoinRequest> build() { return super.build(); }
    }
    
    public static class NestedListBuilder<T> extends ListBuilder<NestedListBuilder<T>> {
        
        private final Function<List<JoinRequest>, T> doneFunc;
        
        private NestedListBuilder( Function<List<JoinRequest>, T> doneFunc, SequencedCollection<? extends JolieValue> c ) { super( c ); this.doneFunc = doneFunc; }
        private NestedListBuilder( Function<List<JoinRequest>, T> doneFunc ) { this.doneFunc = doneFunc; }
        
        protected NestedListBuilder<T> self() { return this; }
        
        public T done() throws TypeValidationException { return doneFunc.apply( build() ); }
    }
}