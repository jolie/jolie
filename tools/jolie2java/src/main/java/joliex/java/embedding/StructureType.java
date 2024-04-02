package joliex.java.embedding;

import java.util.SequencedCollection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import jolie.runtime.ByteArray;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;

import joliex.java.embedding.util.StructureListBuilder;
import joliex.java.embedding.util.UntypedBuilder;

public non-sealed interface StructureType extends JolieType {

    BasicType<?> root();
    Map<String, List<StructureType>> children();

    default Value jolieRepr() {
        Value value = root().jolieRepr();

        children().forEach( (name, ls) -> {
            final ValueVector vv = value.getChildren( name );
            ls.forEach( s -> vv.add( s.jolieRepr() ) );
        } );
        
        return value;
    }

    default List<StructureType> child( String name ) {
        return children().get( name );
    }

    default List<StructureType> childOrDefault( String name, List<StructureType> defaultValue ) {
        return children().getOrDefault( name, defaultValue );
    }

    default Optional<StructureType> firstChild( String name ) {
        return Optional.ofNullable( childOrDefault( name, null ) ).map( c -> c.isEmpty() ? null : c.getFirst() );
    }

    public static InlineBuilder construct() { return new InlineBuilder(); }
    public static InlineBuilder construct( BasicType<?> root ) { return construct().root( root ); }
    public static InlineBuilder construct( Boolean rootValue ) { return construct( BasicType.create( rootValue ) ); }
    public static InlineBuilder construct( Integer rootValue ) { return construct( BasicType.create( rootValue ) ); }
    public static InlineBuilder construct( Long rootValue ) { return construct( BasicType.create( rootValue ) ); }
    public static InlineBuilder construct( Double rootValue ) { return construct( BasicType.create( rootValue ) ); }
    public static InlineBuilder construct( String rootValue ) { return construct( BasicType.create( rootValue ) ); }
    public static InlineBuilder construct( ByteArray rootValue ) { return construct( BasicType.create( rootValue ) ); }

    public static InlineBuilder constructFrom( JolieType t ) { return new InlineBuilder( Objects.requireNonNull( t ) ); }
    public static StructureType createFrom( JolieType t ) { return JolieType.toStructure( t ); }

    public static <T> NestedBuilder<T> constructNested( Function<StructureType, T> f ) { return new NestedBuilder<>( f ); }
    public static <T> NestedBuilder<T> constructNested( Function<StructureType, T> f, JolieType e ) { return new NestedBuilder<>( f, Objects.requireNonNull( e ) ); }

    public static InlineListBuilder constructList() { return new InlineListBuilder(); }
    public static InlineListBuilder constructListFrom( SequencedCollection<? extends JolieType> c ) { return new InlineListBuilder( c ); }

    public static <T> NestedListBuilder<T> constructNestedList( Function<List<StructureType>, T> f ) { return new NestedListBuilder<>( f ); }
    public static <T> NestedListBuilder<T> constructNestedList( Function<List<StructureType>, T> f, SequencedCollection<? extends JolieType> c ) { return new NestedListBuilder<>( f, Objects.requireNonNull( c ) ); }

    public static Value toValue( StructureType structure ) { return structure.jolieRepr(); }
    public static StructureType fromValue( Value value ) { return InlineBuilder.buildFrom( value ); }


    public static abstract class Builder<B> extends UntypedBuilder<BasicType<?>,B> {

        protected abstract B self();

        protected Builder() {}
        protected Builder( StructureType structure ) { super( structure.root(), structure.children() ); }

        public B root( BasicType<?> root ) { return super.root( root ); }
        public B root( Boolean value ) { return root( BasicType.create( value ) ); }
        public B root( Integer value ) { return root( BasicType.create( value ) ); }
        public B root( Long value ) { return root( BasicType.create( value ) ); }
        public B root( Double value ) { return root( BasicType.create( value ) ); }
        public B root( String value ) { return root( BasicType.create( value ) ); }
        public B root( ByteArray value ) { return root( BasicType.create( value ) ); }

        public B root( UnaryOperator<BasicType<?>> rootOperator ) { return root( rootOperator.apply( root ) ); }

        protected StructureType build() { return new ImmutableStructure<>( root == null ? BasicType.create() : root, children ); }
    }

    public static class InlineBuilder extends Builder<InlineBuilder> {

        private InlineBuilder() {}
        private InlineBuilder( JolieType jolieType ) { super( JolieType.toStructure( jolieType ) ); }

        protected InlineBuilder self() { return this; }

        public StructureType build() { return super.build(); }

        private static StructureType buildFrom( Value value ) {
            InlineBuilder b = new InlineBuilder();

            b.root( BasicType.fromValue( value ) );

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

        private final Function<StructureType, T> doneFunc;

        private NestedBuilder( Function<StructureType, T> doneFunc ) { this.doneFunc = doneFunc; }
        private NestedBuilder( Function<StructureType, T> doneFunc, JolieType jolieType ) { super( JolieType.toStructure( jolieType ) ); this.doneFunc = doneFunc; }

        protected NestedBuilder<T> self() { return this; }

        public T done() { return doneFunc.apply( build() ); }
    }

    
    public static abstract class ListBuilder<B> extends StructureListBuilder<StructureType,B> {

        protected ListBuilder( SequencedCollection<? extends JolieType> c ) { super( c.parallelStream().map( JolieType::toStructure ).toList() ); }
        protected ListBuilder() {}

        public B add( BasicType<?> basic ) { return add( JolieType.toStructure( basic ) ); }
        public B add( Boolean basicValue ) { return add( BasicType.create( basicValue ) ); }
        public B add( Integer basicValue ) { return add( BasicType.create( basicValue ) ); }
        public B add( Long basicValue ) { return add( BasicType.create( basicValue ) ); }
        public B add( Double basicValue ) { return add( BasicType.create( basicValue ) ); }
        public B add( String basicValue ) { return add( BasicType.create( basicValue ) ); }
        public B add( ByteArray basicValue ) { return add( BasicType.create( basicValue ) ); }

        public NestedBuilder<B> addConstructed() { return constructNested( this::add ); }
        public NestedBuilder<B> addConstructed( BasicType<?> root ) { return addConstructed().root( root ); }
        public NestedBuilder<B> addConstructed( Boolean rootValue ) { return addConstructed( BasicType.create( rootValue ) ); }
        public NestedBuilder<B> addConstructed( Integer rootValue ) { return addConstructed( BasicType.create( rootValue ) ); }
        public NestedBuilder<B> addConstructed( Long rootValue ) { return addConstructed( BasicType.create( rootValue ) ); }
        public NestedBuilder<B> addConstructed( Double rootValue ) { return addConstructed( BasicType.create( rootValue ) ); }
        public NestedBuilder<B> addConstructed( String rootValue ) { return addConstructed( BasicType.create( rootValue ) ); }
        public NestedBuilder<B> addConstructed( ByteArray rootValue ) { return addConstructed( BasicType.create( rootValue ) ); }

        public NestedBuilder<B> addConstructedFrom( StructureType e ) { return constructNested( this::add, e ); }

        public B set( int index, BasicType<?> basic ) { return set( index, JolieType.toStructure( basic ) ); }
        public B set( int index, Boolean basicValue ) { return set( index, BasicType.create( basicValue ) ); }
        public B set( int index, Integer basicValue ) { return set( index, BasicType.create( basicValue ) ); }
        public B set( int index, Long basicValue ) { return set( index, BasicType.create( basicValue ) ); }
        public B set( int index, Double basicValue ) { return set( index, BasicType.create( basicValue ) ); }
        public B set( int index, String basicValue ) { return set( index, BasicType.create( basicValue ) ); }
        public B set( int index, ByteArray basicValue ) { return set( index, BasicType.create( basicValue ) ); }

        public NestedBuilder<B> setConstructed( int index ) { return constructNested( e -> set( index, e ) ); }
        public NestedBuilder<B> setConstructed( int index, BasicType<?> root ) { return setConstructed( index ).root( root ); }
        public NestedBuilder<B> setConstructed( int index, Boolean rootValue ) { return setConstructed( index, BasicType.create( rootValue ) ); }
        public NestedBuilder<B> setConstructed( int index, Integer rootValue ) { return setConstructed( index, BasicType.create( rootValue ) ); }
        public NestedBuilder<B> setConstructed( int index, Long rootValue ) { return setConstructed( index, BasicType.create( rootValue ) ); }
        public NestedBuilder<B> setConstructed( int index, Double rootValue ) { return setConstructed( index, BasicType.create( rootValue ) ); }
        public NestedBuilder<B> setConstructed( int index, String rootValue ) { return setConstructed( index, BasicType.create( rootValue ) ); }
        public NestedBuilder<B> setConstructed( int index, ByteArray rootValue ) { return setConstructed( index, BasicType.create( rootValue ) ); }

        public NestedBuilder<B> setConstructedFrom( int index, StructureType e ) { return constructNested( s -> set( index, s ), e ); }

        public NestedBuilder<B> reconstruct( int index ) { return setConstructedFrom( index, get( index ).orElseThrow( IndexOutOfBoundsException::new ) ); }
        public NestedBuilder<B> reconstruct( int index, UnaryOperator<BasicType<?>> rootOperator ) { return reconstruct( index ).root( rootOperator ); }
    }

    public static class InlineListBuilder extends ListBuilder<InlineListBuilder> {

        private InlineListBuilder( SequencedCollection<? extends JolieType> elements ) { super( elements ); }
        private InlineListBuilder() {}

        protected InlineListBuilder self() { return this; }

        public List<StructureType> build() { return super.build(); }
    }

    public static class NestedListBuilder<T> extends ListBuilder<NestedListBuilder<T>> {

        private final Function<List<StructureType>, T> doneFunc;

        private NestedListBuilder( Function<List<StructureType>, T> doneFunc, SequencedCollection<? extends JolieType> elements ) { super( elements ); this.doneFunc = doneFunc; }
        private NestedListBuilder( Function<List<StructureType>, T> doneFunc ) { this.doneFunc = doneFunc; }

        protected NestedListBuilder<T> self() { return this; }

        public T done() { return doneFunc.apply( build() ); }
    }
}
