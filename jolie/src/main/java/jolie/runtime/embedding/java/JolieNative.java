package jolie.runtime.embedding.java;

import java.util.SequencedCollection;
import java.util.Optional;
import jolie.runtime.ByteArray;
import jolie.runtime.Value;
import jolie.runtime.JavaService.ValueConverter;
import jolie.runtime.typing.TypeCheckingException;
import jolie.runtime.embedding.java.util.AbstractListBuilder;

public sealed interface JolieNative<T> extends ValueConverter {
    
    Value jolieRepr();

    default T value() { return null; }

    // TODO: should JolieVoid be a singleton?
    public static record JolieVoid() implements JolieNative<Void> {
        
        public Value jolieRepr() { return Value.create(); }

        public boolean equals( Object obj ) { return obj != null && obj instanceof JolieVoid; }
        public int hashCode() { return 0; }
        public String toString() { return ""; }

        public static JolieVoid createFrom( JolieValue j ) { return new JolieVoid(); }

        public static Value requireVoid( Value v ) throws TypeCheckingException {
            if ( v.isDefined() )
                throw new TypeCheckingException( "The given Value was defined, but expected void." );

            return requireNoChildren( v );
        }
        public static JolieVoid fromValue( Value v ) throws TypeCheckingException {
            requireVoid( v );
            return new JolieVoid();
        }

        public static Value toValue( JolieVoid t ) { return t.jolieRepr(); }
    }

    public static record JolieBool( Boolean value ) implements JolieNative<Boolean> {
        
        public Value jolieRepr() { return Value.create( value ); }

        public boolean equals( Object obj ) { return obj instanceof JolieBool b && value.equals( b.value() ); }
        public int hashCode() { return value.hashCode(); }
        public String toString() { return value.toString(); }

        public static JolieBool createFrom( JolieValue j ) throws TypeValidationException { 
            if ( j.content() instanceof JolieBool content )
                return content;
            
            throw new TypeValidationException( "The content of the given JolieValue was of an unexpected type: " + j.content().getClass().getName() + ", expected JolieBool." );
        }
        
        public static Boolean contentFromValue( Value v ) throws TypeCheckingException {
            if ( v.valueObject() instanceof Boolean b )
                return b;
            
            throw new TypeCheckingException( "The given value isn't a Boolean." );
        }
        
        public static Boolean fieldFromValue( Value v ) throws TypeCheckingException {
            return contentFromValue( requireNoChildren( v ) );
        }

        public static JolieBool fromValue( Value v ) throws TypeCheckingException { 
            return new JolieBool( fieldFromValue( v ) ); 
        }

        public static Value toValue( JolieBool t ) { return t.jolieRepr(); }
    }

    public static record JolieInt( Integer value ) implements JolieNative<Integer> {
        
        public Value jolieRepr() { return Value.create( value ); }

        public boolean equals( Object obj ) { return obj instanceof JolieInt n && value.equals( n.value() ); }
        public int hashCode() { return value.hashCode(); }
        public String toString() { return value.toString(); }

        public static JolieInt createFrom( JolieValue j ) throws TypeValidationException { 
            if ( j.content() instanceof JolieInt content )
                return content;
            
            throw new TypeValidationException( "The content of the given JolieValue was of an unexpected type: " + j.content().getClass().getName() + ", expected JolieInt." );
        }

        public static Integer contentFromValue( Value v ) throws TypeCheckingException {
            if ( v.valueObject() instanceof Integer i )
                return i;
            
            throw new TypeCheckingException( "The given value isn't an Integer." );
        }
        
        public static Integer fieldFromValue( Value v ) throws TypeCheckingException {
            return contentFromValue( requireNoChildren( v ) );
        }

        public static JolieInt fromValue( Value v ) throws TypeCheckingException {
            return new JolieInt( fieldFromValue( v ) );
        }

        public static Value toValue( JolieInt t ) { return t.jolieRepr(); }
    }

    public static record JolieLong( Long value ) implements JolieNative<Long> {
        
        public Value jolieRepr() { return Value.create( value ); }

        public boolean equals( Object obj ) { return obj instanceof JolieLong n && value.equals( n.value() ); }
        public int hashCode() { return value.hashCode(); }
        public String toString() { return value.toString(); }

        public static JolieLong createFrom( JolieValue j ) throws TypeValidationException {
            if ( j.content() instanceof JolieLong content )
                return content;
            
            throw new TypeValidationException( "The content of the given JolieValue was of an unexpected type: " + j.content().getClass().getName() + ", expected JolieLong." );
        }

        public static Long contentFromValue( Value v ) throws TypeCheckingException {
            if ( v.valueObject() instanceof Long l )
                return l;
            
            throw new TypeCheckingException( "The given value isn't a Long." );
        }
        
        public static Long fieldFromValue( Value v ) throws TypeCheckingException {
            return contentFromValue( requireNoChildren( v ) );
        }

        public static JolieLong fromValue( Value v ) throws TypeCheckingException {
            return new JolieLong( fieldFromValue( v ) );
        }

        public static Value toValue( JolieLong t ) { return t.jolieRepr(); }
    }

    public static record JolieDouble( Double value ) implements JolieNative<Double> {
        
        public Value jolieRepr() { return Value.create( value ); }

        public boolean equals( Object obj ) { return obj instanceof JolieDouble n && value.equals( n.value() ); }
        public int hashCode() { return value.hashCode(); }
        public String toString() { return value.toString(); }

        public static JolieDouble createFrom( JolieValue j ) throws TypeValidationException { 
            if ( j.content() instanceof JolieDouble content )
                return content;
            
            throw new TypeValidationException( "The content of the given JolieValue was of an unexpected type: " + j.content().getClass().getName() + ", expected JolieDouble." );
        }

        public static Double contentFromValue( Value v ) throws TypeCheckingException {
            if ( v.valueObject() instanceof Double d )
                return d;
            
            throw new TypeCheckingException( "The given value isn't a Double." );
        }
        
        public static Double fieldFromValue( Value v ) throws TypeCheckingException {
            return contentFromValue( requireNoChildren( v ) );
        }

        public static JolieDouble fromValue( Value v ) throws TypeCheckingException {
            return new JolieDouble( fieldFromValue( v ) );
        }

        public static Value toValue( JolieDouble t ) { return t.jolieRepr(); }
    }

    public static record JolieString( String value ) implements JolieNative<String> {
        
        public Value jolieRepr() { return Value.create( value ); }

        public boolean equals( Object obj ) { return obj instanceof JolieString n && value.equals( n.value() ); }
        public int hashCode() { return value.hashCode(); }
        public String toString() { return value; }

        public static JolieString createFrom( JolieValue j ) throws TypeValidationException {
            if ( j.content() instanceof JolieString content )
                return content;
            
            throw new TypeValidationException( "The content of the given JolieValue was of an unexpected type: " + j.content().getClass().getName() + ", expected JolieString." );
        }

        public static String contentFromValue( Value v ) throws TypeCheckingException {
            if ( v.valueObject() instanceof String i )
                return i;
            
            throw new TypeCheckingException( "The given value isn't a String." );
        }
        
        public static String fieldFromValue( Value v ) throws TypeCheckingException {
            return contentFromValue( requireNoChildren( v ) );
        }

        public static JolieString fromValue( Value v ) throws TypeCheckingException {
            return new JolieString( fieldFromValue( v ) );
        }

        public static Value toValue( JolieString t ) { return t.jolieRepr(); }
    }

    public static record JolieRaw( ByteArray value ) implements JolieNative<ByteArray> {
        
        public Value jolieRepr() { return Value.create( value ); }

        public boolean equals( Object obj ) { return obj instanceof JolieRaw n && value.equals( n.value() ); }
        public int hashCode() { return value.hashCode(); }
        public String toString() { return value.toString(); }

        public static JolieRaw createFrom( JolieValue j ) throws TypeValidationException {
            if ( j.content() instanceof JolieRaw content )
                return content;
            
            throw new TypeValidationException( "The content of the given JolieValue was of an unexpected type: " + j.content().getClass().getName() + ", expected JolieRaw." );
        }

        public static ByteArray contentFromValue( Value v ) throws TypeCheckingException {
            if ( v.valueObject() instanceof ByteArray i )
                return i;
            
            throw new TypeCheckingException( "The given value isn't a ByteArray." );
        }

        public static ByteArray fieldFromValue( Value v ) throws TypeCheckingException {
            return contentFromValue( requireNoChildren( v ) );
        }

        public static JolieRaw fromValue( Value v ) throws TypeCheckingException {
            return new JolieRaw( fieldFromValue( v ) );
        }

        public static Value toValue( JolieRaw t ) { return t.jolieRepr(); }
    }

    public static JolieVoid create() { return new JolieVoid(); }
    public static JolieBool create( Boolean value ) { return Optional.ofNullable( value ).map( JolieBool::new ).orElse( null ); }
    public static JolieInt create( Integer value ) { return Optional.ofNullable( value ).map( JolieInt::new ).orElse( null ); }
    public static JolieLong create( Long value ) { return Optional.ofNullable( value ).map( JolieLong::new ).orElse( null ); }
    public static JolieDouble create( Double value ) { return Optional.ofNullable( value ).map( JolieDouble::new ).orElse( null ); }
    public static JolieString create( String value ) { return Optional.ofNullable( value ).map( JolieString::new ).orElse( null ); }
    public static JolieRaw create( ByteArray value ) { return Optional.ofNullable( value ).map( JolieRaw::new ).orElse( null ); }

    public static JolieNative<?> createFrom( JolieValue j ) { return j.content(); }

    public static ListBuilder constructList() { return new ListBuilder(); }

    public static ListBuilder constructListFrom( SequencedCollection<? extends JolieValue> c ) { return new ListBuilder( c ); }
    
    public static JolieNative<?> contentFromValue( Value v ) {
        if ( v.valueObject() instanceof Boolean n )
            return new JolieBool( n );
        if ( v.valueObject() instanceof Integer n )
            return new JolieInt( n );
        if ( v.valueObject() instanceof Long n )
            return new JolieLong( n );
        if ( v.valueObject() instanceof Double n )
            return new JolieDouble( n );
        if ( v.valueObject() instanceof String n )
            return new JolieString( n );
        if ( v.valueObject() instanceof ByteArray n )
            return new JolieRaw( n );
        return new JolieVoid();
    }

    public static JolieNative<?> fromValue( Value v ) throws TypeCheckingException { 
        return contentFromValue( requireNoChildren( v ) ); 
    }

    public static Value toValue( JolieNative<?> any ) { return any.jolieRepr(); }

    private static Value requireNoChildren( Value v ) throws TypeCheckingException {
        if ( v.hasChildren() )
            throw new TypeCheckingException( "The given Value had unexpected children." );

        return v;
    }

    public static class ListBuilder extends AbstractListBuilder<ListBuilder, JolieNative<?>> {

        private ListBuilder() {}
        private ListBuilder( SequencedCollection<? extends JolieValue> c ) { super( c, JolieNative::createFrom ); }

        protected ListBuilder self() { return this; }

        public ListBuilder add( Boolean value ) { return add( create( value ) ); }
        public ListBuilder add( Integer value ) { return add( create( value ) ); }
        public ListBuilder add( Long value ) { return add( create( value ) ); }
        public ListBuilder add( Double value ) { return add( create( value ) ); }
        public ListBuilder add( String value ) { return add( create( value ) ); }
        public ListBuilder add( ByteArray value ) { return add( create( value ) ); }

        public ListBuilder set( int index, Boolean value ) { return set( index, create( value ) ); }
        public ListBuilder set( int index, Integer value ) { return set( index, create( value ) ); }
        public ListBuilder set( int index, Long value ) { return set( index, create( value ) ); }
        public ListBuilder set( int index, Double value ) { return set( index, create( value ) ); }
        public ListBuilder set( int index, String value ) { return set( index, create( value ) ); }
        public ListBuilder set( int index, ByteArray value ) { return set( index, create( value ) ); }
    }
}
