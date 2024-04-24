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

    public static InlineBuilder builder() { return new InlineBuilder(); }
    public static InlineBuilder builder( JolieNative<?> content ) { return builder().content( content ); }
    public static InlineBuilder builder( Boolean contentValue ) { return builder( JolieNative.of( contentValue ) ); }
    public static InlineBuilder builder( Integer contentValue ) { return builder( JolieNative.of( contentValue ) ); }
    public static InlineBuilder builder( Long contentValue ) { return builder( JolieNative.of( contentValue ) ); }
    public static InlineBuilder builder( Double contentValue ) { return builder( JolieNative.of( contentValue ) ); }
    public static InlineBuilder builder( String contentValue ) { return builder( JolieNative.of( contentValue ) ); }
    public static InlineBuilder builder( ByteArray contentValue ) { return builder( JolieNative.of( contentValue ) ); }
    public static InlineBuilder builder( JolieValue j ) { return new InlineBuilder( Objects.requireNonNull( j ) ); }

    static <R> NestedBuilder<R> nestedBuilder( Function<JolieValue,R> doneFunction ) { return new NestedBuilder<>( doneFunction ); }
    static <R> NestedBuilder<R> nestedBuilder( JolieValue j, Function<JolieValue,R> doneFunction ) { return new NestedBuilder<>( j, doneFunction ); }

    public static InlineListBuilder listBuilder() { return new InlineListBuilder(); }
    public static InlineListBuilder listBuilder( SequencedCollection<? extends JolieValue> c ) { return new InlineListBuilder( c ); }

    static <R> NestedListBuilder<R> nestedListBuilder( Function<List<JolieValue>,R> doneFunction ) { return new NestedListBuilder<>( doneFunction ); }
    static <R> NestedListBuilder<R> nestedListBuilder( SequencedCollection<? extends JolieValue> c, Function<List<JolieValue>,R> doneFunction ) { return new NestedListBuilder<>( c, doneFunction ); }
    
    public static JolieValue of( JolieNative<?> content ) { return new UntypedStructure<>( content, Map.of() ); }
    public static JolieValue of( Boolean contentValue ) { return of( JolieNative.of( contentValue ) ); }
    public static JolieValue of( Integer contentValue ) { return of( JolieNative.of( contentValue ) ); }
    public static JolieValue of( Long contentValue ) { return of( JolieNative.of( contentValue ) ); }
    public static JolieValue of( Double contentValue ) { return of( JolieNative.of( contentValue ) ); }
    public static JolieValue of( String contentValue ) { return of( JolieNative.of( contentValue ) ); }
    public static JolieValue of( ByteArray contentValue ) { return of( JolieNative.of( contentValue ) ); }
    public static JolieValue of() { return of( JolieNative.of() ); }

    public static JolieValue from( JolieValue j ) { return j; }

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

        public B content( Boolean value ) { return content( JolieNative.of( value ) ); }
        public B content( Integer value ) { return content( JolieNative.of( value ) ); }
        public B content( Long value ) { return content( JolieNative.of( value ) ); }
        public B content( Double value ) { return content( JolieNative.of( value ) ); }
        public B content( String value ) { return content( JolieNative.of( value ) ); }
        public B content( ByteArray value ) { return content( JolieNative.of( value ) ); }

        public B content( UnaryOperator<JolieNative<?>> contentOperator ) { return content( contentOperator.apply( content ) ); }

        protected JolieValue build() { return new UntypedStructure<>( content == null ? JolieNative.of() : content, children ); }
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
        private ListBuilder( SequencedCollection<? extends JolieValue> c ) { super( c, JolieValue::from ); }

        public B add( JolieNative<?> contentEntry ) { return add( JolieValue.of( contentEntry ) ); }
        public B add( Boolean valueEntry ) { return add( JolieNative.of( valueEntry ) ); }
        public B add( Integer valueEntry ) { return add( JolieNative.of( valueEntry ) ); }
        public B add( Long valueEntry ) { return add( JolieNative.of( valueEntry ) ); }
        public B add( Double valueEntry ) { return add( JolieNative.of( valueEntry ) ); }
        public B add( String valueEntry ) { return add( JolieNative.of( valueEntry ) ); }
        public B add( ByteArray valueEntry ) { return add( JolieNative.of( valueEntry ) ); }

        public B set( int index, JolieNative<?> contentEntry ) { return set( index, JolieValue.of( contentEntry ) ); }
        public B set( int index, Boolean valueEntry ) { return set( index, JolieNative.of( valueEntry ) ); }
        public B set( int index, Integer valueEntry ) { return set( index, JolieNative.of( valueEntry ) ); }
        public B set( int index, Long valueEntry ) { return set( index, JolieNative.of( valueEntry ) ); }
        public B set( int index, Double valueEntry ) { return set( index, JolieNative.of( valueEntry ) ); }
        public B set( int index, String valueEntry ) { return set( index, JolieNative.of( valueEntry ) ); }
        public B set( int index, ByteArray valueEntry ) { return set( index, JolieNative.of( valueEntry ) ); }
        
        public NestedBuilder<B> builder() { return nestedBuilder( this::add ); }
        public NestedBuilder<B> builder( JolieNative<?> content ) { return builder().content( content ); }
        public NestedBuilder<B> builder( Boolean contentValue ) { return builder( JolieNative.of( contentValue ) ); }
        public NestedBuilder<B> builder( Integer contentValue ) { return builder( JolieNative.of( contentValue ) ); }
        public NestedBuilder<B> builder( Long contentValue ) { return builder( JolieNative.of( contentValue ) ); }
        public NestedBuilder<B> builder( Double contentValue ) { return builder( JolieNative.of( contentValue ) ); }
        public NestedBuilder<B> builder( String contentValue ) { return builder( JolieNative.of( contentValue ) ); }
        public NestedBuilder<B> builder( ByteArray contentValue ) { return builder( JolieNative.of( contentValue ) ); }
        public NestedBuilder<B> builder( JolieValue from ) { return nestedBuilder( from, this::add ); }

        public NestedBuilder<B> builder( int index ) { return nestedBuilder( e -> set( index, e ) ); }
        public NestedBuilder<B> builder( int index, JolieNative<?> content ) { return builder( index ).content( content ); }
        public NestedBuilder<B> builder( int index, Boolean contentValue ) { return builder( index, JolieNative.of( contentValue ) ); }
        public NestedBuilder<B> builder( int index, Integer contentValue ) { return builder( index, JolieNative.of( contentValue ) ); }
        public NestedBuilder<B> builder( int index, Long contentValue ) { return builder( index, JolieNative.of( contentValue ) ); }
        public NestedBuilder<B> builder( int index, Double contentValue ) { return builder( index, JolieNative.of( contentValue ) ); }
        public NestedBuilder<B> builder( int index, String contentValue ) { return builder( index, JolieNative.of( contentValue ) ); }
        public NestedBuilder<B> builder( int index, ByteArray contentValue ) { return builder( index, JolieNative.of( contentValue ) ); }
        public NestedBuilder<B> builder( int index, JolieValue from ) { return nestedBuilder( from, e -> set( index, e ) ); }

        public NestedBuilder<B> rebuilder( int index ) { return builder( index, get( index ) ); }
        public NestedBuilder<B> rebuilder( int index, UnaryOperator<JolieNative<?>> operator ) { return rebuilder( index ).content( operator ); }
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
