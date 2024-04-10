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
 * content: {@link String}
 *     end[0,1]: {@link Integer}
 *     begin: {@link Integer}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 * @see #construct()
 */
public final class SubStringRequest extends ImmutableStructure<JolieString> {
    
    public Optional<Integer> end() { return getFirstChildValue( "end", JolieInt.class ); }
    public Integer begin() { return getFirstChildValue( "begin", JolieInt.class ).get(); }
    
    private SubStringRequest( Builder<?> builder ) {
        super( builder.content(), builder.children() );
    }
    
    public static InlineBuilder construct() { return new InlineBuilder(); }
    public static InlineBuilder construct( JolieString content ) { return construct().content( content ); }
    public static InlineBuilder construct( String contentValue ) { return construct().content( contentValue ); }
    
    static <T> NestedBuilder<T> constructNested( Function<SubStringRequest, T> doneFunc ) { return new NestedBuilder<>( doneFunc ); }
    static <T> NestedBuilder<T> constructNested( Function<SubStringRequest, T> doneFunc, JolieValue t ) { return new NestedBuilder<>( doneFunc, t ); }
    
    static InlineListBuilder constructList() { return new InlineListBuilder(); }
    
    static <T> NestedListBuilder<T> constructNestedList( Function<List<SubStringRequest>, T> doneFunc ) { return new NestedListBuilder<>( doneFunc ); }
    static <T> NestedListBuilder<T> constructNestedList( Function<List<SubStringRequest>, T> doneFunc, SequencedCollection<? extends JolieValue> c ) { return new NestedListBuilder<>( doneFunc, c ); }
    
    public static InlineBuilder constructFrom( JolieValue t ) { return new InlineBuilder( t ); }
    
    public static SubStringRequest createFrom( JolieValue t ) throws TypeValidationException { return constructFrom( t ).build(); }
    
    public static Value toValue( SubStringRequest t ) { return JolieValue.toValue( t ); }
    public static SubStringRequest fromValue( Value value ) throws TypeCheckingException { return Builder.buildFrom( value ); }
    
    static abstract class Builder<B> extends StructureBuilder<JolieString, B> {
        
        private static final Map<String,FieldManager<?>> FIELD_MAP = Map.of(
            "end", FieldManager.createNative( 0, 1, JolieInt::fromValue, JolieInt::createFrom ),
            "begin", FieldManager.createNative( JolieInt::fromValue, JolieInt::createFrom )
        );
        
        protected Builder() {}
        protected Builder( JolieValue structure ) {
            super(
                structure.content() instanceof JolieString content ? content : null,
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
        
        private JolieString content() { return content; }
        private Map<String, List<JolieValue>> children() { return children; }
        
        public B content( JolieString content ) { return super.content( content ); }
        public B content( String value ) { return content( JolieNative.create( value ) ); }
        public B content( UnaryOperator<String> valueOperator ) { return content( valueOperator.apply( content.value() ) ); }
        
        public B setEnd( JolieInt contentEntry ) { return putAs( "end", contentEntry, JolieValue::create ); }
        public B setEnd( Integer valueEntry ) { return putAs( "end", valueEntry, JolieValue::create ); }
        public B replaceEnd( UnaryOperator<Integer> valueOperator ) { return computeAs( "end", (n,v) -> valueOperator.apply( v ), s -> JolieInt.class.cast( s.content() ).value(), JolieValue::create ); }
        
        public B setBegin( JolieInt contentEntry ) { return putAs( "begin", contentEntry, JolieValue::create ); }
        public B setBegin( Integer valueEntry ) { return putAs( "begin", valueEntry, JolieValue::create ); }
        public B replaceBegin( UnaryOperator<Integer> valueOperator ) { return computeAs( "begin", (n,v) -> valueOperator.apply( v ), s -> JolieInt.class.cast( s.content() ).value(), JolieValue::create ); }
        
        protected SubStringRequest validatedBuild() throws TypeValidationException {
            validateChildren( FIELD_MAP );
            
            return new SubStringRequest( this );
        }
        
        private static SubStringRequest buildFrom( Value value ) throws TypeCheckingException {
            InlineBuilder builder = SubStringRequest.construct();
            
            builder.content( JolieString.fromValue( value ) );
            
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
        
        public SubStringRequest build() throws TypeValidationException { return validatedBuild(); }
    }
    
    public static class NestedBuilder<T> extends Builder<NestedBuilder<T>> {
        
        private final Function<SubStringRequest, T> doneFunc;
        
        private NestedBuilder( Function<SubStringRequest, T> doneFunc, JolieValue t ) { super( t ); this.doneFunc = doneFunc; }
        private NestedBuilder( Function<SubStringRequest, T> doneFunc ) { this.doneFunc = doneFunc; }
        
        protected NestedBuilder<T> self() { return this; }
        
        public T done() throws TypeValidationException { return doneFunc.apply( validatedBuild() ); }
    }
    
    static abstract class ListBuilder<B> extends StructureListBuilder<SubStringRequest, B> {
        
        protected ListBuilder( SequencedCollection<? extends JolieValue> elements ) { super( elements.parallelStream().map( SubStringRequest::createFrom ).toList() ); }
        protected ListBuilder() {}
        
        public NestedBuilder<B> addConstructed() { return constructNested( this::add ); }
        public NestedBuilder<B> setConstructed( int index ) { return constructNested( e -> set( index, e ) ); }
        public NestedBuilder<B> addConstructedFrom( JolieValue t ) { return constructNested( this::add, t ); }
        public NestedBuilder<B> setConstructedFrom( int index, JolieValue t ) { return constructNested( e -> set( index, e ), t ); }
        public NestedBuilder<B> reconstruct( int index ) { return setConstructedFrom( index, elements.get( index ) ); }
        
        public NestedBuilder<B> addConstructed( JolieString content ) { return addConstructed().content( content ); }
        public NestedBuilder<B> setConstructed( int index, JolieString content ) { return setConstructed( index ).content( content ); }
        public NestedBuilder<B> addConstructed( String contentValue ) { return addConstructed( JolieNative.create( contentValue ) ); }
        public NestedBuilder<B> setConstructed( int index, String contentValue ) { return setConstructed( index, JolieNative.create( contentValue ) ); }
        public NestedBuilder<B> reconstruct( int index, UnaryOperator<String> valueOperator ) { return reconstruct( index ).content( valueOperator ); }
    }
    
    public static class InlineListBuilder extends ListBuilder<InlineListBuilder> {
        
        protected InlineListBuilder self() { return this; }
        
        public List<SubStringRequest> build() { return super.build(); }
    }
    
    public static class NestedListBuilder<T> extends ListBuilder<NestedListBuilder<T>> {
        
        private final Function<List<SubStringRequest>, T> doneFunc;
        
        private NestedListBuilder( Function<List<SubStringRequest>, T> doneFunc, SequencedCollection<? extends JolieValue> c ) { super( c ); this.doneFunc = doneFunc; }
        private NestedListBuilder( Function<List<SubStringRequest>, T> doneFunc ) { this.doneFunc = doneFunc; }
        
        protected NestedListBuilder<T> self() { return this; }
        
        public T done() throws TypeValidationException { return doneFunc.apply( build() ); }
    }
}