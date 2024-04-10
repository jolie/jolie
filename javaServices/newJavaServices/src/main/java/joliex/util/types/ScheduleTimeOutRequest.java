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
 * content: {@link Integer}
 *     message[0,1]: {@link JolieValue}
 *     operation[0,1]: {@link String}
 *     timeunit[0,1]: {@link String}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 * @see #construct()
 */
public final class ScheduleTimeOutRequest extends ImmutableStructure<JolieInt> {
    
    public Optional<JolieValue> message() { return getFirstChild( "message" ); }
    public Optional<String> operation() { return getFirstChildValue( "operation", JolieString.class ); }
    public Optional<String> timeunit() { return getFirstChildValue( "timeunit", JolieString.class ); }
    
    private ScheduleTimeOutRequest( Builder<?> builder ) {
        super( builder.content(), builder.children() );
    }
    
    public static InlineBuilder construct() { return new InlineBuilder(); }
    public static InlineBuilder construct( JolieInt content ) { return construct().content( content ); }
    public static InlineBuilder construct( Integer contentValue ) { return construct().content( contentValue ); }
    
    static <T> NestedBuilder<T> constructNested( Function<ScheduleTimeOutRequest, T> doneFunc ) { return new NestedBuilder<>( doneFunc ); }
    static <T> NestedBuilder<T> constructNested( Function<ScheduleTimeOutRequest, T> doneFunc, JolieValue t ) { return new NestedBuilder<>( doneFunc, t ); }
    
    static InlineListBuilder constructList() { return new InlineListBuilder(); }
    
    static <T> NestedListBuilder<T> constructNestedList( Function<List<ScheduleTimeOutRequest>, T> doneFunc ) { return new NestedListBuilder<>( doneFunc ); }
    static <T> NestedListBuilder<T> constructNestedList( Function<List<ScheduleTimeOutRequest>, T> doneFunc, SequencedCollection<? extends JolieValue> c ) { return new NestedListBuilder<>( doneFunc, c ); }
    
    public static InlineBuilder constructFrom( JolieValue t ) { return new InlineBuilder( t ); }
    
    public static ScheduleTimeOutRequest createFrom( JolieValue t ) throws TypeValidationException { return constructFrom( t ).build(); }
    
    public static Value toValue( ScheduleTimeOutRequest t ) { return JolieValue.toValue( t ); }
    public static ScheduleTimeOutRequest fromValue( Value value ) throws TypeCheckingException { return Builder.buildFrom( value ); }
    
    static abstract class Builder<B> extends StructureBuilder<JolieInt, B> {
        
        private static final Map<String,FieldManager<?>> FIELD_MAP = Map.of(
            "message", FieldManager.createCustom( 0, 1, JolieValue::fromValue, JolieValue::createFrom ),
            "operation", FieldManager.createNative( 0, 1, JolieString::fromValue, JolieString::createFrom ),
            "timeunit", FieldManager.createNative( 0, 1, JolieString::fromValue, JolieString::createFrom )
        );
        
        protected Builder() {}
        protected Builder( JolieValue structure ) {
            super(
                structure.content() instanceof JolieInt content ? content : null,
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
        
        private JolieInt content() { return content; }
        private Map<String, List<JolieValue>> children() { return children; }
        
        public B content( JolieInt content ) { return super.content( content ); }
        public B content( Integer value ) { return content( JolieNative.create( value ) ); }
        public B content( UnaryOperator<Integer> valueOperator ) { return content( valueOperator.apply( content.value() ) ); }
        
        public B setMessage( JolieValue childEntry ) { return putAs( "message", childEntry ); }
        public JolieValue.NestedBuilder<B> constructMessage() { return JolieValue.constructNested( this::setMessage ); }
        public JolieValue.NestedBuilder<B> constructMessage( JolieNative<?> content ) { return constructMessage().content( content ); }
        public JolieValue.NestedBuilder<B> constructMessage( Boolean contentValue ) { return constructMessage().content( contentValue ); }
        public JolieValue.NestedBuilder<B> constructMessage( Integer contentValue ) { return constructMessage().content( contentValue ); }
        public JolieValue.NestedBuilder<B> constructMessage( Long contentValue ) { return constructMessage().content( contentValue ); }
        public JolieValue.NestedBuilder<B> constructMessage( Double contentValue ) { return constructMessage().content( contentValue ); }
        public JolieValue.NestedBuilder<B> constructMessage( String contentValue ) { return constructMessage().content( contentValue ); }
        public JolieValue.NestedBuilder<B> constructMessage( ByteArray contentValue ) { return constructMessage().content( contentValue ); }
        public JolieValue.NestedBuilder<B> constructMessageFrom( JolieValue t ) { return JolieValue.constructNested( this::setMessage, t ); }
        public JolieValue.NestedBuilder<B> reconstructMessage() { return firstChild( "message" ).map( e -> constructMessageFrom( e ) ).orElse( constructMessage() ); }
        public JolieValue.NestedBuilder<B> reconstructMessage( UnaryOperator<JolieNative<?>> contentOperator ) { return reconstructMessage().content( contentOperator ); }
        
        public B setOperation( JolieString contentEntry ) { return putAs( "operation", contentEntry, JolieValue::create ); }
        public B setOperation( String valueEntry ) { return putAs( "operation", valueEntry, JolieValue::create ); }
        public B replaceOperation( UnaryOperator<String> valueOperator ) { return computeAs( "operation", (n,v) -> valueOperator.apply( v ), s -> JolieString.class.cast( s.content() ).value(), JolieValue::create ); }
        
        public B setTimeunit( JolieString contentEntry ) { return putAs( "timeunit", contentEntry, JolieValue::create ); }
        public B setTimeunit( String valueEntry ) { return putAs( "timeunit", valueEntry, JolieValue::create ); }
        public B replaceTimeunit( UnaryOperator<String> valueOperator ) { return computeAs( "timeunit", (n,v) -> valueOperator.apply( v ), s -> JolieString.class.cast( s.content() ).value(), JolieValue::create ); }
        
        protected ScheduleTimeOutRequest validatedBuild() throws TypeValidationException {
            validateChildren( FIELD_MAP );
            
            return new ScheduleTimeOutRequest( this );
        }
        
        private static ScheduleTimeOutRequest buildFrom( Value value ) throws TypeCheckingException {
            InlineBuilder builder = ScheduleTimeOutRequest.construct();
            
            builder.content( JolieInt.fromValue( value ) );
            
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
        
        public ScheduleTimeOutRequest build() throws TypeValidationException { return validatedBuild(); }
    }
    
    public static class NestedBuilder<T> extends Builder<NestedBuilder<T>> {
        
        private final Function<ScheduleTimeOutRequest, T> doneFunc;
        
        private NestedBuilder( Function<ScheduleTimeOutRequest, T> doneFunc, JolieValue t ) { super( t ); this.doneFunc = doneFunc; }
        private NestedBuilder( Function<ScheduleTimeOutRequest, T> doneFunc ) { this.doneFunc = doneFunc; }
        
        protected NestedBuilder<T> self() { return this; }
        
        public T done() throws TypeValidationException { return doneFunc.apply( validatedBuild() ); }
    }
    
    static abstract class ListBuilder<B> extends StructureListBuilder<ScheduleTimeOutRequest, B> {
        
        protected ListBuilder( SequencedCollection<? extends JolieValue> elements ) { super( elements.parallelStream().map( ScheduleTimeOutRequest::createFrom ).toList() ); }
        protected ListBuilder() {}
        
        public NestedBuilder<B> addConstructed() { return constructNested( this::add ); }
        public NestedBuilder<B> setConstructed( int index ) { return constructNested( e -> set( index, e ) ); }
        public NestedBuilder<B> addConstructedFrom( JolieValue t ) { return constructNested( this::add, t ); }
        public NestedBuilder<B> setConstructedFrom( int index, JolieValue t ) { return constructNested( e -> set( index, e ), t ); }
        public NestedBuilder<B> reconstruct( int index ) { return setConstructedFrom( index, elements.get( index ) ); }
        
        public NestedBuilder<B> addConstructed( JolieInt content ) { return addConstructed().content( content ); }
        public NestedBuilder<B> setConstructed( int index, JolieInt content ) { return setConstructed( index ).content( content ); }
        public NestedBuilder<B> addConstructed( Integer contentValue ) { return addConstructed( JolieNative.create( contentValue ) ); }
        public NestedBuilder<B> setConstructed( int index, Integer contentValue ) { return setConstructed( index, JolieNative.create( contentValue ) ); }
        public NestedBuilder<B> reconstruct( int index, UnaryOperator<Integer> valueOperator ) { return reconstruct( index ).content( valueOperator ); }
    }
    
    public static class InlineListBuilder extends ListBuilder<InlineListBuilder> {
        
        protected InlineListBuilder self() { return this; }
        
        public List<ScheduleTimeOutRequest> build() { return super.build(); }
    }
    
    public static class NestedListBuilder<T> extends ListBuilder<NestedListBuilder<T>> {
        
        private final Function<List<ScheduleTimeOutRequest>, T> doneFunc;
        
        private NestedListBuilder( Function<List<ScheduleTimeOutRequest>, T> doneFunc, SequencedCollection<? extends JolieValue> c ) { super( c ); this.doneFunc = doneFunc; }
        private NestedListBuilder( Function<List<ScheduleTimeOutRequest>, T> doneFunc ) { this.doneFunc = doneFunc; }
        
        protected NestedListBuilder<T> self() { return this; }
        
        public T done() throws TypeValidationException { return doneFunc.apply( build() ); }
    }
}