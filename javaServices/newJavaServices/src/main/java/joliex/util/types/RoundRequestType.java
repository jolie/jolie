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
 * content: {@link Double}
 *     decimals[0,1]: {@link Integer}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 * @see #construct()
 */
public final class RoundRequestType extends ImmutableStructure<JolieDouble> {
    
    public Optional<Integer> decimals() { return getFirstChildValue( "decimals", JolieInt.class ); }
    
    private RoundRequestType( Builder<?> builder ) {
        super( builder.content(), builder.children() );
    }
    
    public static InlineBuilder construct() { return new InlineBuilder(); }
    public static InlineBuilder construct( JolieDouble content ) { return construct().content( content ); }
    public static InlineBuilder construct( Double contentValue ) { return construct().content( contentValue ); }
    
    static <T> NestedBuilder<T> constructNested( Function<RoundRequestType, T> doneFunc ) { return new NestedBuilder<>( doneFunc ); }
    static <T> NestedBuilder<T> constructNested( Function<RoundRequestType, T> doneFunc, JolieValue t ) { return new NestedBuilder<>( doneFunc, t ); }
    
    static InlineListBuilder constructList() { return new InlineListBuilder(); }
    
    static <T> NestedListBuilder<T> constructNestedList( Function<List<RoundRequestType>, T> doneFunc ) { return new NestedListBuilder<>( doneFunc ); }
    static <T> NestedListBuilder<T> constructNestedList( Function<List<RoundRequestType>, T> doneFunc, SequencedCollection<? extends JolieValue> c ) { return new NestedListBuilder<>( doneFunc, c ); }
    
    public static InlineBuilder constructFrom( JolieValue t ) { return new InlineBuilder( t ); }
    
    public static RoundRequestType createFrom( JolieValue t ) throws TypeValidationException { return constructFrom( t ).build(); }
    
    public static Value toValue( RoundRequestType t ) { return JolieValue.toValue( t ); }
    public static RoundRequestType fromValue( Value value ) throws TypeCheckingException { return Builder.buildFrom( value ); }
    
    static abstract class Builder<B> extends StructureBuilder<JolieDouble, B> {
        
        private static final Map<String,FieldManager<?>> FIELD_MAP = Map.of(
            "decimals", FieldManager.createNative( 0, 1, JolieInt::fromValue, JolieInt::createFrom )
        );
        
        protected Builder() {}
        protected Builder( JolieValue structure ) {
            super(
                structure.content() instanceof JolieDouble content ? content : null,
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
        
        private JolieDouble content() { return content; }
        private Map<String, List<JolieValue>> children() { return children; }
        
        public B content( JolieDouble content ) { return super.content( content ); }
        public B content( Double value ) { return content( JolieNative.create( value ) ); }
        public B content( UnaryOperator<Double> valueOperator ) { return content( valueOperator.apply( content.value() ) ); }
        
        public B setDecimals( JolieInt contentEntry ) { return putAs( "decimals", contentEntry, JolieValue::create ); }
        public B setDecimals( Integer valueEntry ) { return putAs( "decimals", valueEntry, JolieValue::create ); }
        public B replaceDecimals( UnaryOperator<Integer> valueOperator ) { return computeAs( "decimals", (n,v) -> valueOperator.apply( v ), s -> JolieInt.class.cast( s.content() ).value(), JolieValue::create ); }
        
        protected RoundRequestType validatedBuild() throws TypeValidationException {
            validateChildren( FIELD_MAP );
            
            return new RoundRequestType( this );
        }
        
        private static RoundRequestType buildFrom( Value value ) throws TypeCheckingException {
            InlineBuilder builder = RoundRequestType.construct();
            
            builder.content( JolieDouble.fromValue( value ) );
            
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
        
        public RoundRequestType build() throws TypeValidationException { return validatedBuild(); }
    }
    
    public static class NestedBuilder<T> extends Builder<NestedBuilder<T>> {
        
        private final Function<RoundRequestType, T> doneFunc;
        
        private NestedBuilder( Function<RoundRequestType, T> doneFunc, JolieValue t ) { super( t ); this.doneFunc = doneFunc; }
        private NestedBuilder( Function<RoundRequestType, T> doneFunc ) { this.doneFunc = doneFunc; }
        
        protected NestedBuilder<T> self() { return this; }
        
        public T done() throws TypeValidationException { return doneFunc.apply( validatedBuild() ); }
    }
    
    static abstract class ListBuilder<B> extends StructureListBuilder<RoundRequestType, B> {
        
        protected ListBuilder( SequencedCollection<? extends JolieValue> elements ) { super( elements.parallelStream().map( RoundRequestType::createFrom ).toList() ); }
        protected ListBuilder() {}
        
        public NestedBuilder<B> addConstructed() { return constructNested( this::add ); }
        public NestedBuilder<B> setConstructed( int index ) { return constructNested( e -> set( index, e ) ); }
        public NestedBuilder<B> addConstructedFrom( JolieValue t ) { return constructNested( this::add, t ); }
        public NestedBuilder<B> setConstructedFrom( int index, JolieValue t ) { return constructNested( e -> set( index, e ), t ); }
        public NestedBuilder<B> reconstruct( int index ) { return setConstructedFrom( index, elements.get( index ) ); }
        
        public NestedBuilder<B> addConstructed( JolieDouble content ) { return addConstructed().content( content ); }
        public NestedBuilder<B> setConstructed( int index, JolieDouble content ) { return setConstructed( index ).content( content ); }
        public NestedBuilder<B> addConstructed( Double contentValue ) { return addConstructed( JolieNative.create( contentValue ) ); }
        public NestedBuilder<B> setConstructed( int index, Double contentValue ) { return setConstructed( index, JolieNative.create( contentValue ) ); }
        public NestedBuilder<B> reconstruct( int index, UnaryOperator<Double> valueOperator ) { return reconstruct( index ).content( valueOperator ); }
    }
    
    public static class InlineListBuilder extends ListBuilder<InlineListBuilder> {
        
        protected InlineListBuilder self() { return this; }
        
        public List<RoundRequestType> build() { return super.build(); }
    }
    
    public static class NestedListBuilder<T> extends ListBuilder<NestedListBuilder<T>> {
        
        private final Function<List<RoundRequestType>, T> doneFunc;
        
        private NestedListBuilder( Function<List<RoundRequestType>, T> doneFunc, SequencedCollection<? extends JolieValue> c ) { super( c ); this.doneFunc = doneFunc; }
        private NestedListBuilder( Function<List<RoundRequestType>, T> doneFunc ) { this.doneFunc = doneFunc; }
        
        protected NestedListBuilder<T> self() { return this; }
        
        public T done() throws TypeValidationException { return doneFunc.apply( build() ); }
    }
}