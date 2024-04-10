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
 *     time1: {@link String}
 *     time2: {@link String}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 * @see #construct()
 */
public final class GetTimeDiffRequest extends ImmutableStructure<JolieVoid> {
    
    public String time1() { return getFirstChildValue( "time1", JolieString.class ).get(); }
    public String time2() { return getFirstChildValue( "time2", JolieString.class ).get(); }
    
    private GetTimeDiffRequest( Builder<?> builder ) {
        super( builder.content(), builder.children() );
    }
    
    public static InlineBuilder construct() { return new InlineBuilder(); }
    
    static <T> NestedBuilder<T> constructNested( Function<GetTimeDiffRequest, T> doneFunc ) { return new NestedBuilder<>( doneFunc ); }
    static <T> NestedBuilder<T> constructNested( Function<GetTimeDiffRequest, T> doneFunc, JolieValue t ) { return new NestedBuilder<>( doneFunc, t ); }
    
    static InlineListBuilder constructList() { return new InlineListBuilder(); }
    
    static <T> NestedListBuilder<T> constructNestedList( Function<List<GetTimeDiffRequest>, T> doneFunc ) { return new NestedListBuilder<>( doneFunc ); }
    static <T> NestedListBuilder<T> constructNestedList( Function<List<GetTimeDiffRequest>, T> doneFunc, SequencedCollection<? extends JolieValue> c ) { return new NestedListBuilder<>( doneFunc, c ); }
    
    public static InlineBuilder constructFrom( JolieValue t ) { return new InlineBuilder( t ); }
    
    public static GetTimeDiffRequest createFrom( JolieValue t ) throws TypeValidationException { return constructFrom( t ).build(); }
    
    public static Value toValue( GetTimeDiffRequest t ) { return JolieValue.toValue( t ); }
    public static GetTimeDiffRequest fromValue( Value value ) throws TypeCheckingException { return Builder.buildFrom( value ); }
    
    static abstract class Builder<B> extends StructureBuilder<JolieVoid, B> {
        
        private static final Map<String,FieldManager<?>> FIELD_MAP = Map.of(
            "time1", FieldManager.createNative( JolieString::fromValue, JolieString::createFrom ),
            "time2", FieldManager.createNative( JolieString::fromValue, JolieString::createFrom )
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
        
        public B setTime1( JolieString contentEntry ) { return putAs( "time1", contentEntry, JolieValue::create ); }
        public B setTime1( String valueEntry ) { return putAs( "time1", valueEntry, JolieValue::create ); }
        public B replaceTime1( UnaryOperator<String> valueOperator ) { return computeAs( "time1", (n,v) -> valueOperator.apply( v ), s -> JolieString.class.cast( s.content() ).value(), JolieValue::create ); }
        
        public B setTime2( JolieString contentEntry ) { return putAs( "time2", contentEntry, JolieValue::create ); }
        public B setTime2( String valueEntry ) { return putAs( "time2", valueEntry, JolieValue::create ); }
        public B replaceTime2( UnaryOperator<String> valueOperator ) { return computeAs( "time2", (n,v) -> valueOperator.apply( v ), s -> JolieString.class.cast( s.content() ).value(), JolieValue::create ); }
        
        protected GetTimeDiffRequest validatedBuild() throws TypeValidationException {
            validateChildren( FIELD_MAP );
            
            return new GetTimeDiffRequest( this );
        }
        
        private static GetTimeDiffRequest buildFrom( Value value ) throws TypeCheckingException {
            InlineBuilder builder = GetTimeDiffRequest.construct();
            
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
        
        public GetTimeDiffRequest build() throws TypeValidationException { return validatedBuild(); }
    }
    
    public static class NestedBuilder<T> extends Builder<NestedBuilder<T>> {
        
        private final Function<GetTimeDiffRequest, T> doneFunc;
        
        private NestedBuilder( Function<GetTimeDiffRequest, T> doneFunc, JolieValue t ) { super( t ); this.doneFunc = doneFunc; }
        private NestedBuilder( Function<GetTimeDiffRequest, T> doneFunc ) { this.doneFunc = doneFunc; }
        
        protected NestedBuilder<T> self() { return this; }
        
        public T done() throws TypeValidationException { return doneFunc.apply( validatedBuild() ); }
    }
    
    static abstract class ListBuilder<B> extends StructureListBuilder<GetTimeDiffRequest, B> {
        
        protected ListBuilder( SequencedCollection<? extends JolieValue> elements ) { super( elements.parallelStream().map( GetTimeDiffRequest::createFrom ).toList() ); }
        protected ListBuilder() {}
        
        public NestedBuilder<B> addConstructed() { return constructNested( this::add ); }
        public NestedBuilder<B> setConstructed( int index ) { return constructNested( e -> set( index, e ) ); }
        public NestedBuilder<B> addConstructedFrom( JolieValue t ) { return constructNested( this::add, t ); }
        public NestedBuilder<B> setConstructedFrom( int index, JolieValue t ) { return constructNested( e -> set( index, e ), t ); }
        public NestedBuilder<B> reconstruct( int index ) { return setConstructedFrom( index, elements.get( index ) ); }
    }
    
    public static class InlineListBuilder extends ListBuilder<InlineListBuilder> {
        
        protected InlineListBuilder self() { return this; }
        
        public List<GetTimeDiffRequest> build() { return super.build(); }
    }
    
    public static class NestedListBuilder<T> extends ListBuilder<NestedListBuilder<T>> {
        
        private final Function<List<GetTimeDiffRequest>, T> doneFunc;
        
        private NestedListBuilder( Function<List<GetTimeDiffRequest>, T> doneFunc, SequencedCollection<? extends JolieValue> c ) { super( c ); this.doneFunc = doneFunc; }
        private NestedListBuilder( Function<List<GetTimeDiffRequest>, T> doneFunc ) { this.doneFunc = doneFunc; }
        
        protected NestedListBuilder<T> self() { return this; }
        
        public T done() throws TypeValidationException { return doneFunc.apply( build() ); }
    }
}