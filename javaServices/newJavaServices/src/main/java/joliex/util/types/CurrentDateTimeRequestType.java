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
import java.util.Set;
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
 * this class is an {@link JolieValue} which can be described as follows:
 * 
 * <pre>
 *     format[0,1]: {@link String}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 * @see #construct()
 */
public final class CurrentDateTimeRequestType implements JolieValue {
    
    private static final Set<String> FIELD_KEYS = Set.of( "format" );
    
    private final String format;
    
    public CurrentDateTimeRequestType( String format ) {
        this.format = format;
    }
    
    public Optional<String> format() { return Optional.ofNullable( format ); }
    
    public JolieVoid content() { return new JolieVoid(); }
    public Map<String, List<JolieValue>> children() {
        return one.util.streamex.EntryStream.of(
            "format", format == null ? null : List.of( JolieValue.create( format ) )
        ).filterValues( Objects::nonNull ).toImmutableMap();
    }
    
    public static Builder construct() { return new Builder(); }
    
    public static ListBuilder constructList() { return new ListBuilder(); }
    
    public static Builder constructFrom( JolieValue j ) { return new Builder( j ); }
    
    public static ListBuilder constructListFrom( SequencedCollection<? extends JolieValue> c ) { return new ListBuilder( c ); }
    
    public static CurrentDateTimeRequestType createFrom( JolieValue j ) {
        return new CurrentDateTimeRequestType(
            ValueManager.fieldFrom( j.getFirstChild( "format" ), c -> c.content() instanceof JolieString content ? content.value() : null )
        );
    }
    
    public static CurrentDateTimeRequestType fromValue( Value v ) throws TypeCheckingException {
        ValueManager.requireChildren( v, FIELD_KEYS );
        return new CurrentDateTimeRequestType(
            ValueManager.fieldFrom( v.firstChildOrDefault( "format", Function.identity(), null ), JolieString::fieldFromValue )
        );
    }
    
    public static Value toValue( CurrentDateTimeRequestType t ) {
        final Value v = Value.create();
        
        t.format().ifPresent( c -> v.getFirstChild( "format" ).setValue( c ) );
        
        return v;
    }
    
    public static class Builder {
        
        private String format;
        
        private Builder() {}
        private Builder( JolieValue j ) {
            this.format = ValueManager.fieldFrom( j.getFirstChild( "format" ), c -> c.content() instanceof JolieString content ? content.value() : null );
        }
        
        public Builder format( String format ) { this.format = format; return this; }
        
        public CurrentDateTimeRequestType build() {
            return new CurrentDateTimeRequestType( format );
        }
    }
    
    public static class ListBuilder extends AbstractListBuilder<ListBuilder, CurrentDateTimeRequestType> {
        
        private ListBuilder() {}
        private ListBuilder( SequencedCollection<? extends JolieValue> c ) { super( c, CurrentDateTimeRequestType::createFrom ); }
        
        protected ListBuilder self() { return this; }
        
        public ListBuilder add( Function<Builder, CurrentDateTimeRequestType> b ) { return add( b.apply( construct() ) ); }
        public ListBuilder set( int index, Function<Builder, CurrentDateTimeRequestType> b ) { return set( index, b.apply( construct() ) ); }
        public ListBuilder reconstruct( int index, Function<Builder, CurrentDateTimeRequestType> b ) { return replace( index, j -> b.apply( constructFrom( j ) ) ); }
    }
}