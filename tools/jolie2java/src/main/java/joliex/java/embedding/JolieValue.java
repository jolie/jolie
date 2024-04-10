package joliex.java.embedding;

import java.util.SequencedCollection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import jolie.runtime.ByteArray;
import jolie.runtime.JavaService.ValueConverter;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;

import joliex.java.embedding.util.StructureListBuilder;
import joliex.java.embedding.util.UntypedBuilder;

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
    
    public static JolieValue create( JolieNative<?> content ) { return new ImmutableStructure<>( content, Map.of() ); }
    public static JolieValue create( Boolean contentValue ) { return create( JolieNative.create( contentValue ) ); }
    public static JolieValue create( Integer contentValue ) { return create( JolieNative.create( contentValue ) ); }
    public static JolieValue create( Long contentValue ) { return create( JolieNative.create( contentValue ) ); }
    public static JolieValue create( Double contentValue ) { return create( JolieNative.create( contentValue ) ); }
    public static JolieValue create( String contentValue ) { return create( JolieNative.create( contentValue ) ); }
    public static JolieValue create( ByteArray contentValue ) { return create( JolieNative.create( contentValue ) ); }
    public static JolieValue create() { return create( JolieNative.create() ); }

    public static InlineBuilder constructFrom( JolieValue t ) { return new InlineBuilder( Objects.requireNonNull( t ) ); }
    public static JolieValue createFrom( JolieValue t ) { return t; }

    public static <T> NestedBuilder<T> constructNested( Function<JolieValue, T> f ) { return new NestedBuilder<>( f ); }
    public static <T> NestedBuilder<T> constructNested( Function<JolieValue, T> f, JolieValue e ) { return new NestedBuilder<>( f, Objects.requireNonNull( e ) ); }

    public static InlineListBuilder constructList() { return new InlineListBuilder(); }
    public static InlineListBuilder constructListFrom( SequencedCollection<? extends JolieValue> c ) { return new InlineListBuilder( c ); }

    public static <T> NestedListBuilder<T> constructNestedList( Function<List<JolieValue>, T> f ) { return new NestedListBuilder<>( f ); }
    public static <T> NestedListBuilder<T> constructNestedList( Function<List<JolieValue>, T> f, SequencedCollection<? extends JolieValue> c ) { return new NestedListBuilder<>( f, Objects.requireNonNull( c ) ); }

    public static Value toValue( JolieValue t ) { 
        final Value value = t.content().jolieRepr();

        t.children().forEach( (name, ls) -> {
            final ValueVector vv = value.getChildren( name );
            ls.forEach( e -> vv.add( toValue( e ) ) );
        } );
        
        return value;
    }
    public static JolieValue fromValue( Value value ) { return InlineBuilder.buildFrom( value ); }


    public static abstract class Builder<B> extends UntypedBuilder<JolieNative<?>,B> {

        protected abstract B self();

        protected Builder() {}
        protected Builder( JolieValue structure ) { super( structure.content(), structure.children() ); }

        public B content( JolieNative<?> content ) { return super.content( content ); }
        public B content( Boolean value ) { return content( JolieNative.create( value ) ); }
        public B content( Integer value ) { return content( JolieNative.create( value ) ); }
        public B content( Long value ) { return content( JolieNative.create( value ) ); }
        public B content( Double value ) { return content( JolieNative.create( value ) ); }
        public B content( String value ) { return content( JolieNative.create( value ) ); }
        public B content( ByteArray value ) { return content( JolieNative.create( value ) ); }

        public B content( UnaryOperator<JolieNative<?>> contentOperator ) { return content( contentOperator.apply( content ) ); }

        protected JolieValue build() { return new ImmutableStructure<>( content == null ? JolieNative.create() : content, children ); }
    }

    public static class InlineBuilder extends Builder<InlineBuilder> {

        private InlineBuilder() {}
        private InlineBuilder( JolieValue t ) { super( t ); }

        protected InlineBuilder self() { return this; }

        public JolieValue build() { return super.build(); }

        private static JolieValue buildFrom( Value value ) {
            InlineBuilder b = new InlineBuilder();

            b.content( JolieNative.fromValue( value ) );

            value.children()
                .forEach( (name, vector) -> b.put( 
                    name, 
                    vector.stream()
                        .parallel()
                        .map( InlineBuilder::buildFrom )
                        .toList() 
                ) );

            return b.build();
        }
    }

    public static class NestedBuilder<T> extends Builder<NestedBuilder<T>> {

        private final Function<JolieValue, T> doneFunc;

        private NestedBuilder( Function<JolieValue, T> doneFunc ) { this.doneFunc = doneFunc; }
        private NestedBuilder( Function<JolieValue, T> doneFunc, JolieValue t ) { super( t ); this.doneFunc = doneFunc; }

        protected NestedBuilder<T> self() { return this; }

        public T done() { return doneFunc.apply( build() ); }
    }

    
    public static abstract class ListBuilder<B> extends StructureListBuilder<JolieValue,B> {

        protected ListBuilder( SequencedCollection<? extends JolieValue> c ) { super( c ); }
        protected ListBuilder() {}

        public B add( JolieNative<?> basic ) { return add( JolieValue.create( basic ) ); }
        public B add( Boolean basicValue ) { return add( JolieNative.create( basicValue ) ); }
        public B add( Integer basicValue ) { return add( JolieNative.create( basicValue ) ); }
        public B add( Long basicValue ) { return add( JolieNative.create( basicValue ) ); }
        public B add( Double basicValue ) { return add( JolieNative.create( basicValue ) ); }
        public B add( String basicValue ) { return add( JolieNative.create( basicValue ) ); }
        public B add( ByteArray basicValue ) { return add( JolieNative.create( basicValue ) ); }

        public NestedBuilder<B> addConstructed() { return constructNested( this::add ); }
        public NestedBuilder<B> addConstructed( JolieNative<?> content ) { return addConstructed().content( content ); }
        public NestedBuilder<B> addConstructed( Boolean contentValue ) { return addConstructed( JolieNative.create( contentValue ) ); }
        public NestedBuilder<B> addConstructed( Integer contentValue ) { return addConstructed( JolieNative.create( contentValue ) ); }
        public NestedBuilder<B> addConstructed( Long contentValue ) { return addConstructed( JolieNative.create( contentValue ) ); }
        public NestedBuilder<B> addConstructed( Double contentValue ) { return addConstructed( JolieNative.create( contentValue ) ); }
        public NestedBuilder<B> addConstructed( String contentValue ) { return addConstructed( JolieNative.create( contentValue ) ); }
        public NestedBuilder<B> addConstructed( ByteArray contentValue ) { return addConstructed( JolieNative.create( contentValue ) ); }

        public NestedBuilder<B> addConstructedFrom( JolieValue e ) { return constructNested( this::add, e ); }

        public B set( int index, JolieNative<?> basic ) { return set( index, JolieValue.create( basic ) ); }
        public B set( int index, Boolean basicValue ) { return set( index, JolieNative.create( basicValue ) ); }
        public B set( int index, Integer basicValue ) { return set( index, JolieNative.create( basicValue ) ); }
        public B set( int index, Long basicValue ) { return set( index, JolieNative.create( basicValue ) ); }
        public B set( int index, Double basicValue ) { return set( index, JolieNative.create( basicValue ) ); }
        public B set( int index, String basicValue ) { return set( index, JolieNative.create( basicValue ) ); }
        public B set( int index, ByteArray basicValue ) { return set( index, JolieNative.create( basicValue ) ); }

        public NestedBuilder<B> setConstructed( int index ) { return constructNested( e -> set( index, e ) ); }
        public NestedBuilder<B> setConstructed( int index, JolieNative<?> content ) { return setConstructed( index ).content( content ); }
        public NestedBuilder<B> setConstructed( int index, Boolean contentValue ) { return setConstructed( index, JolieNative.create( contentValue ) ); }
        public NestedBuilder<B> setConstructed( int index, Integer contentValue ) { return setConstructed( index, JolieNative.create( contentValue ) ); }
        public NestedBuilder<B> setConstructed( int index, Long contentValue ) { return setConstructed( index, JolieNative.create( contentValue ) ); }
        public NestedBuilder<B> setConstructed( int index, Double contentValue ) { return setConstructed( index, JolieNative.create( contentValue ) ); }
        public NestedBuilder<B> setConstructed( int index, String contentValue ) { return setConstructed( index, JolieNative.create( contentValue ) ); }
        public NestedBuilder<B> setConstructed( int index, ByteArray contentValue ) { return setConstructed( index, JolieNative.create( contentValue ) ); }

        public NestedBuilder<B> setConstructedFrom( int index, JolieValue e ) { return constructNested( s -> set( index, s ), e ); }

        public NestedBuilder<B> reconstruct( int index ) { return setConstructedFrom( index, get( index ).orElseThrow( IndexOutOfBoundsException::new ) ); }
        public NestedBuilder<B> reconstruct( int index, UnaryOperator<JolieNative<?>> contentOperator ) { return reconstruct( index ).content( contentOperator ); }
    }

    public static class InlineListBuilder extends ListBuilder<InlineListBuilder> {

        private InlineListBuilder( SequencedCollection<? extends JolieValue> elements ) { super( elements ); }
        private InlineListBuilder() {}

        protected InlineListBuilder self() { return this; }

        public List<JolieValue> build() { return super.build(); }
    }

    public static class NestedListBuilder<T> extends ListBuilder<NestedListBuilder<T>> {

        private final Function<List<JolieValue>, T> doneFunc;

        private NestedListBuilder( Function<List<JolieValue>, T> doneFunc, SequencedCollection<? extends JolieValue> elements ) { super( elements ); this.doneFunc = doneFunc; }
        private NestedListBuilder( Function<List<JolieValue>, T> doneFunc ) { this.doneFunc = doneFunc; }

        protected NestedListBuilder<T> self() { return this; }

        public T done() { return doneFunc.apply( build() ); }
    }
}
