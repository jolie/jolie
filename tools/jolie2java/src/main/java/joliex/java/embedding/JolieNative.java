package joliex.java.embedding;

import java.util.SequencedCollection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import jolie.runtime.ByteArray;
import jolie.runtime.Value;
import jolie.runtime.JavaService.ValueConverter;
import jolie.runtime.typing.TypeCheckingException;

import joliex.java.embedding.util.StructureListBuilder;

public sealed interface JolieNative<T> extends ValueConverter {
    
    Value jolieRepr();

    default T value() { return null; }

    public static record JolieVoid() implements JolieNative<Void> {
        
        public Value jolieRepr() { return Value.create(); }

        public boolean equals( Object obj ) { return obj != null && obj instanceof JolieVoid; }
        public int hashCode() { return 0; }
        public String toString() { return ""; }

        public static JolieVoid createFrom( JolieValue t ) { return new JolieVoid(); }

        public static Value toValue( JolieVoid t ) { return t.jolieRepr(); }
        public static JolieVoid fromValue( Value v ) throws TypeCheckingException {
            if ( v.valueObject() != null )
                throw new TypeCheckingException( "The valueObject of the given Value was of an unexpected type." );

            return new JolieVoid();
        }
    }

    public static record JolieBool( Boolean value ) implements JolieNative<Boolean> {
        
        public Value jolieRepr() { return Value.create( value ); }

        public boolean equals( Object obj ) { return obj instanceof JolieBool b && value.equals( b.value() ); }
        public int hashCode() { return value.hashCode(); }

        public String toString() { return value.toString(); }

        public static JolieBool createFrom( JolieValue j ) throws TypeValidationException { 
            if ( j.content() instanceof JolieBool content )
                return content;
            
            throw new TypeValidationException( "The given JolieValue could not be converted to a JolieBool." );
        }

        public static Value toValue( JolieBool t ) { return t.jolieRepr(); }
        public static JolieBool fromValue( Value v ) throws TypeCheckingException {
            if ( !v.isBool() )
                throw new TypeCheckingException( "The given value isn't a Boolean." );

            return new JolieBool( v.boolValue() );
        }
    }

    public static record JolieInt( Integer value ) implements JolieNative<Integer> {
        
        public Value jolieRepr() { return Value.create( value ); }

        public boolean equals( Object obj ) { return obj instanceof JolieInt n && value.equals( n.value() ); }
        public int hashCode() { return value.hashCode(); }

        public String toString() { return value.toString(); }

        public static JolieInt createFrom( JolieValue j ) throws TypeValidationException { 
            if ( j.content() instanceof JolieInt content )
                return content;
            
            throw new TypeValidationException( "The given JolieValue could not be converted to a JolieInt." );
        }

        public static Value toValue( JolieInt t ) { return t.jolieRepr(); }
        public static JolieInt fromValue( Value v ) throws TypeCheckingException {
            if ( !v.isInt() )
                throw new TypeCheckingException( "The given value isn't an Integer." );

            return new JolieInt( v.intValue() );
        }
    }

    public static record JolieLong( Long value ) implements JolieNative<Long> {
        
        public Value jolieRepr() { return Value.create( value ); }

        public boolean equals( Object obj ) { return obj instanceof JolieLong n && value.equals( n.value() ); }
        public int hashCode() { return value.hashCode(); }

        public String toString() { return value.toString(); }

        public static JolieLong createFrom( JolieValue j ) throws TypeValidationException {
            if ( j.content() instanceof JolieLong content )
                return content;
            
            throw new TypeValidationException( "The given JolieValue could not be converted to a JolieLong." );
        }

        public static Value toValue( JolieLong t ) { return t.jolieRepr(); }
        public static JolieLong fromValue( Value v ) throws TypeCheckingException {
            if ( !v.isLong() )
                throw new TypeCheckingException( "The given value isn't a Long." );

            return new JolieLong( v.longValue() );
        }
    }

    public static record JolieDouble( Double value ) implements JolieNative<Double> {
        
        public Value jolieRepr() { return Value.create( value ); }

        public boolean equals( Object obj ) { return obj instanceof JolieDouble n && value.equals( n.value() ); }
        public int hashCode() { return value.hashCode(); }

        public String toString() { return value.toString(); }

        public static JolieDouble createFrom( JolieValue j ) throws TypeValidationException { 
            if ( j.content() instanceof JolieDouble content )
                return content;
            
            throw new TypeValidationException( "The given JolieValue could not be converted to a JolieDouble." );
        }

        public static Value toValue( JolieDouble t ) { return t.jolieRepr(); }
        public static JolieDouble fromValue( Value v ) throws TypeCheckingException {
            if ( !v.isDouble() )
                throw new TypeCheckingException( "The given value isn't a Double." );

            return new JolieDouble( v.doubleValue() );
        }
    }

    public static record JolieString( String value ) implements JolieNative<String> {
        
        public Value jolieRepr() { return Value.create( value ); }

        public boolean equals( Object obj ) { return obj instanceof JolieString n && value.equals( n.value() ); }
        public int hashCode() { return value.hashCode(); }

        public String toString() { return value; }

        public static JolieString createFrom( JolieValue j ) throws TypeValidationException {
            if ( j.content() instanceof JolieString content )
                return content;
            
            throw new TypeValidationException( "The given JolieValue could not be converted to a JolieString." );
        }

        public static Value toValue( JolieString t ) { return t.jolieRepr(); }
        public static JolieString fromValue( Value v ) throws TypeCheckingException {
            if ( !v.isString() )
                throw new TypeCheckingException( "The given value isn't a String." );

            return new JolieString( v.strValue() );
        }
    }

    public static record JolieRaw( ByteArray value ) implements JolieNative<ByteArray> {
        
        public Value jolieRepr() { return Value.create( value ); }

        public boolean equals( Object obj ) { return obj instanceof JolieRaw n && value.equals( n.value() ); }
        public int hashCode() { return value.hashCode(); }

        public String toString() { return value.toString(); }

        public static JolieRaw createFrom( JolieValue j ) throws TypeValidationException {
            if ( j.content() instanceof JolieRaw content )
                return content;
            
            throw new TypeValidationException( "The given JolieValue could not be converted to a JolieRaw." );
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

    public static JolieNative<?> createFrom( JolieValue t ) { return t.content(); }

    public static InlineListBuilder constructList() { return new InlineListBuilder(); }
    
    public static <T> NestedListBuilder<T> constructNestedList( Function<List<JolieNative<?>>, T> f, SequencedCollection<? extends JolieValue> c ) { return new NestedListBuilder<>( f, c ); }
    public static <T> NestedListBuilder<T> constructNestedList( Function<List<JolieNative<?>>, T> f ) { return new NestedListBuilder<>( f ); }
    
    public static Value toValue( JolieNative<?> any ) { return any.jolieRepr(); }
    public static JolieNative<?> fromValue( Value value ) {
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

    public static abstract class ListBuilder<B> extends StructureListBuilder<JolieNative<?>, B> {

        protected ListBuilder( SequencedCollection<? extends JolieValue> c ) { super( c.parallelStream().map( JolieValue::content ).toList() ); }
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

        public List<JolieNative<?>> build() { return super.build(); }
    }

    public static class NestedListBuilder<B> extends ListBuilder<NestedListBuilder<B>> {

        private final Function<List<JolieNative<?>>, B> doneFunc;

        private NestedListBuilder( Function<List<JolieNative<?>>, B> doneFunc, SequencedCollection<? extends JolieValue> structures ) { super( structures ); this.doneFunc = doneFunc; }
        private NestedListBuilder( Function<List<JolieNative<?>>, B> doneFunc ) { this.doneFunc = doneFunc; }

        protected NestedListBuilder<B> self() { return this; }

        public B done() throws TypeValidationException { return doneFunc.apply( build() ); }
    }
}
