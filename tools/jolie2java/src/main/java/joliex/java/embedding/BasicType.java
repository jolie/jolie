package joliex.java.embedding;

import java.util.SequencedCollection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import jolie.runtime.ByteArray;
import jolie.runtime.Value;
import jolie.runtime.typing.TypeCheckingException;

import joliex.java.embedding.util.StructureListBuilder;

public sealed interface BasicType<T> extends JolieType {
    
    default T value() { return null; }

    public static record JolieVoid() implements BasicType<Void> {
        public Value jolieRepr() { return Value.create(); }

        public boolean equals( Object obj ) {
            return obj != null && obj instanceof JolieVoid;
        }

        public String toString() { return ""; }

        public static JolieVoid createFrom( JolieType t ) { return new JolieVoid(); }

        public static Value toValue( JolieVoid t ) { return t.jolieRepr(); }
        public static JolieVoid fromValue( Value v ) throws TypeCheckingException {
            if ( v.valueObject() != null )
                throw new TypeCheckingException( "The valueObject of the given Value was of an unexpected type." );

            return new JolieVoid();
        }
    }

    public static record JolieBool( Boolean value ) implements BasicType<Boolean> {
        public Value jolieRepr() { return Value.create( value ); }

        public boolean equals( Object obj ) {
            return obj != null && switch ( obj ) {

                case BasicType<?> b -> value.equals( b.value() );

                case StructureType s -> value.equals( s.root().value() ) && s.children().isEmpty();

                default -> false;
            };
        }

        public String toString() { return value.toString(); }

        public static JolieBool createFrom( JolieType t ) throws TypeValidationException { 
            if ( t instanceof JolieBool v )
                return v;
            
            if ( t instanceof StructureType s && s.root() instanceof JolieBool root )
                return root;
            
            throw new TypeValidationException( "The given JolieType could not be converted to a JolieBool." );
        }

        public static Value toValue( JolieBool t ) { return t.jolieRepr(); }
        public static JolieBool fromValue( Value v ) throws TypeCheckingException {
            if ( !v.isBool() )
                throw new TypeCheckingException( "The given value isn't a Boolean." );

            return new JolieBool( v.boolValue() );
        }
    }

    public static record JolieInt( Integer value ) implements BasicType<Integer> {
        public Value jolieRepr() { return Value.create( value ); }

        public boolean equals( Object obj ) {
            return obj != null && obj instanceof BasicType<?> b && value.equals( b.value() );
        }

        public String toString() { return value.toString(); }

        public static JolieInt createFrom( JolieType t ) throws TypeValidationException { 
            if ( t instanceof JolieInt v )
                return v;
            
            if ( t instanceof StructureType s && s.root() instanceof JolieInt root )
                return root;
            
            throw new TypeValidationException( "The given JolieType could not be converted to a JolieInt." );
        }

        public static Value toValue( JolieInt t ) { return t.jolieRepr(); }
        public static JolieInt fromValue( Value v ) throws TypeCheckingException {
            if ( !v.isInt() )
                throw new TypeCheckingException( "The given value isn't an Integer." );

            return new JolieInt( v.intValue() );
        }
    }

    public static record JolieLong( Long value ) implements BasicType<Long> {
        public Value jolieRepr() { return Value.create( value ); }

        public boolean equals( Object obj ) {
            return obj != null && obj instanceof BasicType<?> b && value.equals( b.value() );
        }

        public String toString() { return value.toString(); }

        public static JolieLong createFrom( JolieType t ) throws TypeValidationException { 
            if ( t instanceof JolieLong v )
                return v;
            
            if ( t instanceof StructureType s && s.root() instanceof JolieLong root )
                return root;
            
            throw new TypeValidationException( "The given JolieType could not be converted to a JolieLong." );
        }

        public static Value toValue( JolieLong t ) { return t.jolieRepr(); }
        public static JolieLong fromValue( Value v ) throws TypeCheckingException {
            if ( !v.isLong() )
                throw new TypeCheckingException( "The given value isn't a Long." );

            return new JolieLong( v.longValue() );
        }
    }

    public static record JolieDouble( Double value ) implements BasicType<Double> {
        public Value jolieRepr() { return Value.create( value ); }

        public boolean equals( Object obj ) {
            return obj != null && obj instanceof BasicType<?> b && value.equals( b.value() );
        }

        public String toString() { return value.toString(); }

        public static JolieDouble createFrom( JolieType t ) throws TypeValidationException { 
            if ( t instanceof JolieDouble v )
                return v;
            
            if ( t instanceof StructureType s && s.root() instanceof JolieDouble root )
                return root;
            
            throw new TypeValidationException( "The given JolieType could not be converted to a JolieDouble." );
        }

        public static Value toValue( JolieDouble t ) { return t.jolieRepr(); }
        public static JolieDouble fromValue( Value v ) throws TypeCheckingException {
            if ( !v.isDouble() )
                throw new TypeCheckingException( "The given value isn't a Double." );

            return new JolieDouble( v.doubleValue() );
        }
    }

    public static record JolieString( String value ) implements BasicType<String> {
        public Value jolieRepr() { return Value.create( value ); }

        public boolean equals( Object obj ) {
            return obj != null && obj instanceof BasicType<?> b && value.equals( b.value() );
        }

        public String toString() { return value.toString(); }

        public static JolieString createFrom( JolieType t ) throws TypeValidationException { 
            if ( t instanceof JolieString v )
                return v;
            
            if ( t instanceof StructureType s && s.root() instanceof JolieString root )
                return root;
            
            throw new TypeValidationException( "The given JolieType could not be converted to a JolieString." );
        }

        public static Value toValue( JolieString t ) { return t.jolieRepr(); }
        public static JolieString fromValue( Value v ) throws TypeCheckingException {
            if ( !v.isString() )
                throw new TypeCheckingException( "The given value isn't a String." );

            return new JolieString( v.strValue() );
        }
    }

    public static record JolieRaw( ByteArray value ) implements BasicType<ByteArray> {
        public Value jolieRepr() { return Value.create( value ); }

        public boolean equals( Object obj ) {
            return obj != null && obj instanceof BasicType<?> b && value.equals( b.value() );
        }

        public String toString() { return value.toString(); }

        public static JolieRaw createFrom( JolieType t ) throws TypeValidationException { 
            if ( t instanceof JolieRaw v )
                return v;
            
            if ( t instanceof StructureType s && s.root() instanceof JolieRaw root )
                return root;
            
            throw new TypeValidationException( "The given JolieType could not be converted to a JolieRaw." );
        }

        public static Value toValue( JolieRaw t ) { return t.jolieRepr(); }
        public static JolieRaw fromValue( Value v ) throws TypeCheckingException {
            if ( !v.isByteArray() )
                throw new TypeCheckingException( "The given value isn't a ByteArray." );

            return new JolieRaw( v.byteArrayValue() );
        }
    }

    public static JolieVoid create() { return new JolieVoid(); }
    public static JolieBool create( Boolean value ) { return Optional.ofNullable( value ).map( JolieBool::new ).orElse( null ); }
    public static JolieInt create( Integer value ) { return Optional.ofNullable( value ).map( JolieInt::new ).orElse( null ); }
    public static JolieLong create( Long value ) { return Optional.ofNullable( value ).map( JolieLong::new ).orElse( null ); }
    public static JolieDouble create( Double value ) { return Optional.ofNullable( value ).map( JolieDouble::new ).orElse( null ); }
    public static JolieString create( String value ) { return Optional.ofNullable( value ).map( JolieString::new ).orElse( null ); }
    public static JolieRaw create( ByteArray value ) { return Optional.ofNullable( value ).map( JolieRaw::new ).orElse( null ); }

    public static BasicType<?> createFrom( JolieType t ) { return switch( t ) { case StructureType s -> s.root(); case BasicType<?> b -> b; }; }

    public static InlineListBuilder constructList() { return new InlineListBuilder(); }
    
    public static <T> NestedListBuilder<T> constructNestedList( Function<List<BasicType<?>>, T> f, SequencedCollection<? extends JolieType> c ) { return new NestedListBuilder<>( f, c ); }
    public static <T> NestedListBuilder<T> constructNestedList( Function<List<BasicType<?>>, T> f ) { return new NestedListBuilder<>( f ); }
    
    public static Value toValue( BasicType<?> any ) { return any.jolieRepr(); }
    public static BasicType<?> fromValue( Value value ) {
        if ( value.isBool() )
            return new JolieBool( value.boolValue() );
        if ( value.isInt() )
            return new JolieInt( value.intValue() );
        if ( value.isLong() )
            return new JolieLong( value.longValue() );
        if ( value.isDouble() )
            return new JolieDouble( value.doubleValue() );
        if ( value.isString() )
            return new JolieString( value.strValue() );
        if ( value.isByteArray() )
            return new JolieRaw( value.byteArrayValue() );
        return new JolieVoid();
    }

    public static abstract class ListBuilder<B> extends StructureListBuilder<BasicType<?>, B> {

        protected ListBuilder( SequencedCollection<? extends JolieType> c ) { super( c.parallelStream().map( e -> switch ( e ) { case BasicType<?> b -> b; case StructureType s -> createFrom( s ); } ).toList() ); } 
        protected ListBuilder() {}

        protected abstract B self();

        public B add( Boolean value ) { return add( create( value ) ); }
        public B add( Integer value ) { return add( create( value ) ); }
        public B add( Long value ) { return add( create( value ) ); }
        public B add( Double value ) { return add( create( value ) ); }
        public B add( String value ) { return add( create( value ) ); }
        public B add( ByteArray value ) { return add( create( value ) ); }

        public B set( int index, Boolean value ) { return set( index, create( value ) ); }
        public B set( int index, Integer value ) { return set( index, create( value ) ); }
        public B set( int index, Long value ) { return set( index, create( value ) ); }
        public B set( int index, Double value ) { return set( index, create( value ) ); }
        public B set( int index, String value ) { return set( index, create( value ) ); }
        public B set( int index, ByteArray value ) { return set( index, create( value ) ); }
    }

    public static class InlineListBuilder extends ListBuilder<InlineListBuilder> {

        private InlineListBuilder() {}

        protected InlineListBuilder self() { return this; }

        public List<BasicType<?>> build() { return super.build(); }
    }

    public static class NestedListBuilder<B> extends ListBuilder<NestedListBuilder<B>> {

        private final Function<List<BasicType<?>>, B> doneFunc;

        private NestedListBuilder( Function<List<BasicType<?>>, B> doneFunc, SequencedCollection<? extends JolieType> structures ) { super( structures ); this.doneFunc = doneFunc; }
        private NestedListBuilder( Function<List<BasicType<?>>, B> doneFunc ) { this.doneFunc = doneFunc; }

        protected NestedListBuilder<B> self() { return this; }

        public B done() throws TypeValidationException { return doneFunc.apply( build() ); }
    }
}
