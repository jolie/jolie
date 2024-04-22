package jolie.runtime.embedding.java;

import java.util.SequencedCollection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import jolie.runtime.ByteArray;
import jolie.runtime.JavaService.ValueConverter;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.embedding.java.util.AbstractListBuilder;
import jolie.runtime.embedding.java.util.UntypedBuilder;

public interface JolieValue extends ValueConverter {

    JolieNative<?> content();
    Map<String, List<JolieValue>> children();

    default List<JolieValue> getChild( String name ) {
        return children().get( name );
    }

    default List<JolieValue> getChildOrDefault( String name, List<JolieValue> defaultValue ) {
        return children().getOrDefault( name, defaultValue );
    }

    default Optional<JolieValue> getFirstChild( String name ) {
        return Optional.ofNullable( getChildOrDefault( name, null ) ).map( c -> c.isEmpty() ? null : c.getFirst() );
    }

    public static InlineBuilder construct() { return new InlineBuilder(); }
    public static InlineBuilder construct( JolieNative<?> content ) { return construct().content( content ); }
    public static InlineBuilder construct( Boolean contentValue ) { return construct( JolieNative.create( contentValue ) ); }
    public static InlineBuilder construct( Integer contentValue ) { return construct( JolieNative.create( contentValue ) ); }
    public static InlineBuilder construct( Long contentValue ) { return construct( JolieNative.create( contentValue ) ); }
    public static InlineBuilder construct( Double contentValue ) { return construct( JolieNative.create( contentValue ) ); }
    public static InlineBuilder construct( String contentValue ) { return construct( JolieNative.create( contentValue ) ); }
    public static InlineBuilder construct( ByteArray contentValue ) { return construct( JolieNative.create( contentValue ) ); }
    public static InlineBuilder constructFrom( JolieValue j ) { return new InlineBuilder( Objects.requireNonNull( j ) ); }

    static <R> NestedBuilder<R> constructNested( Function<JolieValue,R> doneFunction ) { return new NestedBuilder<>( doneFunction ); }
    static <R> NestedBuilder<R> constructNestedFrom( JolieValue j, Function<JolieValue,R> doneFunction ) { return new NestedBuilder<>( j, doneFunction ); }

    public static InlineListBuilder constructList() { return new InlineListBuilder(); }
    public static InlineListBuilder constructListFrom( SequencedCollection<? extends JolieValue> c ) { return new InlineListBuilder( c ); }

    static <R> NestedListBuilder<R> constructNestedList( Function<List<JolieValue>,R> doneFunction ) { return new NestedListBuilder<>( doneFunction ); }
    static <R> NestedListBuilder<R> constructNestedListFrom( SequencedCollection<? extends JolieValue> c, Function<List<JolieValue>,R> doneFunction ) { return new NestedListBuilder<>( c, doneFunction ); }
    
    public static JolieValue create( JolieNative<?> content ) { return new UntypedStructure<>( content, Map.of() ); }
    public static JolieValue create( Boolean contentValue ) { return create( JolieNative.create( contentValue ) ); }
    public static JolieValue create( Integer contentValue ) { return create( JolieNative.create( contentValue ) ); }
    public static JolieValue create( Long contentValue ) { return create( JolieNative.create( contentValue ) ); }
    public static JolieValue create( Double contentValue ) { return create( JolieNative.create( contentValue ) ); }
    public static JolieValue create( String contentValue ) { return create( JolieNative.create( contentValue ) ); }
    public static JolieValue create( ByteArray contentValue ) { return create( JolieNative.create( contentValue ) ); }
    public static JolieValue create() { return create( JolieNative.create() ); }

    public static JolieValue createFrom( JolieValue j ) { return j; }

    public static JolieValue fromValue( Value v ) {
        return new UntypedStructure<>(
            JolieNative.contentFromValue( v ), 
            v.children()
                .entrySet()
                .parallelStream()
                .collect( Collectors.toUnmodifiableMap(
                    Map.Entry::getKey,
                    e -> e.getValue().stream().map( JolieValue::fromValue ).toList()
                ) )
        );
    }

    public static Value toValue( JolieValue t ) { 
        final Value value = t.content().jolieRepr();
        t.children().forEach( (name, ls) -> {
            if ( !ls.isEmpty() ) {
                final ValueVector vv = value.getChildren( name );
                ls.forEach( e -> vv.add( toValue( e ) ) );
            }
        } );
        return value;
    }

    static abstract class Builder<B> extends UntypedBuilder<B> {

        private JolieNative<?> content;

        protected Builder() {}
        protected Builder( JolieValue j ) { 
            super( j.children() );
            content = j.content();
        }

        public B content( JolieNative<?> content ) { this.content = content; return self(); }

        public B content( Boolean value ) { return content( JolieNative.create( value ) ); }
        public B content( Integer value ) { return content( JolieNative.create( value ) ); }
        public B content( Long value ) { return content( JolieNative.create( value ) ); }
        public B content( Double value ) { return content( JolieNative.create( value ) ); }
        public B content( String value ) { return content( JolieNative.create( value ) ); }
        public B content( ByteArray value ) { return content( JolieNative.create( value ) ); }

        public B content( UnaryOperator<JolieNative<?>> contentOperator ) { return content( contentOperator.apply( content ) ); }

        protected JolieValue build() { return new UntypedStructure<>( content == null ? JolieNative.create() : content, children ); }
    }

    public static class InlineBuilder extends Builder<InlineBuilder> {

        private InlineBuilder() {}
        private InlineBuilder( JolieValue j ) { super( j ); }

        protected InlineBuilder self() { return this; }

        public JolieValue build() { return super.build(); }
    }

    public static class NestedBuilder<R> extends Builder<NestedBuilder<R>> {

        private Function<JolieValue,R> doneFunction;

        private NestedBuilder( Function<JolieValue,R> doneFunction ) { this.doneFunction = doneFunction; }
        private NestedBuilder( JolieValue j, Function<JolieValue,R> doneFunction ) { 
            super( j );
            this.doneFunction = doneFunction;
        }

        protected NestedBuilder<R> self() { return this; }

        public R done() { return doneFunction.apply( build() ); }
    }
    
    static abstract class ListBuilder<B> extends AbstractListBuilder<B, JolieValue> {
        
        private ListBuilder() {}
        private ListBuilder( SequencedCollection<? extends JolieValue> c ) { super( c, JolieValue::createFrom ); }

        public B add( JolieNative<?> contentEntry ) { return add( JolieValue.create( contentEntry ) ); }
        public B add( Boolean valueEntry ) { return add( JolieNative.create( valueEntry ) ); }
        public B add( Integer valueEntry ) { return add( JolieNative.create( valueEntry ) ); }
        public B add( Long valueEntry ) { return add( JolieNative.create( valueEntry ) ); }
        public B add( Double valueEntry ) { return add( JolieNative.create( valueEntry ) ); }
        public B add( String valueEntry ) { return add( JolieNative.create( valueEntry ) ); }
        public B add( ByteArray valueEntry ) { return add( JolieNative.create( valueEntry ) ); }

        public B set( int index, JolieNative<?> contentEntry ) { return set( index, JolieValue.create( contentEntry ) ); }
        public B set( int index, Boolean valueEntry ) { return set( index, JolieNative.create( valueEntry ) ); }
        public B set( int index, Integer valueEntry ) { return set( index, JolieNative.create( valueEntry ) ); }
        public B set( int index, Long valueEntry ) { return set( index, JolieNative.create( valueEntry ) ); }
        public B set( int index, Double valueEntry ) { return set( index, JolieNative.create( valueEntry ) ); }
        public B set( int index, String valueEntry ) { return set( index, JolieNative.create( valueEntry ) ); }
        public B set( int index, ByteArray valueEntry ) { return set( index, JolieNative.create( valueEntry ) ); }
        
        public NestedBuilder<B> constructAndAdd() { return constructNested( this::add ); }
        public NestedBuilder<B> constructAndAdd( JolieNative<?> content ) { return constructAndAdd().content( content ); }
        public NestedBuilder<B> constructAndAdd( Boolean contentValue ) { return constructAndAdd( JolieNative.create( contentValue ) ); }
        public NestedBuilder<B> constructAndAdd( Integer contentValue ) { return constructAndAdd( JolieNative.create( contentValue ) ); }
        public NestedBuilder<B> constructAndAdd( Long contentValue ) { return constructAndAdd( JolieNative.create( contentValue ) ); }
        public NestedBuilder<B> constructAndAdd( Double contentValue ) { return constructAndAdd( JolieNative.create( contentValue ) ); }
        public NestedBuilder<B> constructAndAdd( String contentValue ) { return constructAndAdd( JolieNative.create( contentValue ) ); }
        public NestedBuilder<B> constructAndAdd( ByteArray contentValue ) { return constructAndAdd( JolieNative.create( contentValue ) ); }
        
        public NestedBuilder<B> constructFromAndAdd( JolieValue j ) { return constructNestedFrom( j, this::add ); }

        public NestedBuilder<B> constructAndSet( int index ) { return constructNested( e -> set( index, e ) ); }
        public NestedBuilder<B> constructAndSet( int index, JolieNative<?> content ) { return constructAndSet( index ).content( content ); }
        public NestedBuilder<B> constructAndSet( int index, Boolean contentValue ) { return constructAndSet( index, JolieNative.create( contentValue ) ); }
        public NestedBuilder<B> constructAndSet( int index, Integer contentValue ) { return constructAndSet( index, JolieNative.create( contentValue ) ); }
        public NestedBuilder<B> constructAndSet( int index, Long contentValue ) { return constructAndSet( index, JolieNative.create( contentValue ) ); }
        public NestedBuilder<B> constructAndSet( int index, Double contentValue ) { return constructAndSet( index, JolieNative.create( contentValue ) ); }
        public NestedBuilder<B> constructAndSet( int index, String contentValue ) { return constructAndSet( index, JolieNative.create( contentValue ) ); }
        public NestedBuilder<B> constructAndSet( int index, ByteArray contentValue ) { return constructAndSet( index, JolieNative.create( contentValue ) ); }

        public NestedBuilder<B> constructFromAndSet( int index, JolieValue j ) { return constructNestedFrom( j, e -> set( index, e ) ); }

        public NestedBuilder<B> reconstructAndSet( int index ) { return constructFromAndSet( index, get( index ) ); }
        public NestedBuilder<B> reconstructAndSet( int index, UnaryOperator<JolieNative<?>> operator ) { return reconstructAndSet( index ).content( operator ); }
    }
    
    public static class InlineListBuilder extends ListBuilder<InlineListBuilder> {
        
        private InlineListBuilder() {}
        private InlineListBuilder( SequencedCollection<? extends JolieValue> c ) { super( c ); }

        protected InlineListBuilder self() { return this; }

        public List<JolieValue> build() { return super.build(); }
    }
    
    public static class NestedListBuilder<R> extends ListBuilder<NestedListBuilder<R>> {
        
        private Function<List<JolieValue>,R> doneFunction;

        private NestedListBuilder( Function<List<JolieValue>,R> doneFunction ) { this.doneFunction = doneFunction; }
        private NestedListBuilder( SequencedCollection<? extends JolieValue> c, Function<List<JolieValue>,R> doneFunction ) { 
            super( c );
            this.doneFunction = doneFunction;
        }

        protected NestedListBuilder<R> self() { return this; }

        public R done() { return doneFunction.apply( build() ); }
    }
}
