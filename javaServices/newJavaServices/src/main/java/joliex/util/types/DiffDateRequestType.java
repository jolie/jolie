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
 *     date2: {@link String}
 *     date1: {@link String}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 * @see #construct()
 */
public final class DiffDateRequestType implements JolieValue {
    
    private static final Set<String> FIELD_KEYS = Set.of( "format", "date2", "date1" );
    
    private final String format;
    private final String date2;
    private final String date1;
    
    public DiffDateRequestType( String format, String date2, String date1 ) {
        this.format = format;
        this.date2 = ValueManager.validated( date2 );
        this.date1 = ValueManager.validated( date1 );
    }
    
    public Optional<String> format() { return Optional.ofNullable( format ); }
    public String date2() { return date2; }
    public String date1() { return date1; }
    
    public JolieVoid content() { return new JolieVoid(); }
    public Map<String, List<JolieValue>> children() {
        return one.util.streamex.EntryStream.of(
            "format", format == null ? null : List.of( JolieValue.create( format ) ),
            "date2", List.of( JolieValue.create( date2 ) ),
            "date1", List.of( JolieValue.create( date1 ) )
        ).filterValues( Objects::nonNull ).toImmutableMap();
    }
    
    public static Builder construct() { return new Builder(); }
    
    public static ListBuilder constructList() { return new ListBuilder(); }
    
    public static Builder constructFrom( JolieValue j ) { return new Builder( j ); }
    
    public static ListBuilder constructListFrom( SequencedCollection<? extends JolieValue> c ) { return new ListBuilder( c ); }
    
    public static DiffDateRequestType createFrom( JolieValue j ) {
        return new DiffDateRequestType(
            ValueManager.fieldFrom( j.getFirstChild( "format" ), c -> c.content() instanceof JolieString content ? content.value() : null ),
            ValueManager.fieldFrom( j.getFirstChild( "date2" ), c -> c.content() instanceof JolieString content ? content.value() : null ),
            ValueManager.fieldFrom( j.getFirstChild( "date1" ), c -> c.content() instanceof JolieString content ? content.value() : null )
        );
    }
    
    public static DiffDateRequestType fromValue( Value v ) throws TypeCheckingException {
        ValueManager.requireChildren( v, FIELD_KEYS );
        return new DiffDateRequestType(
            ValueManager.fieldFrom( v.firstChildOrDefault( "format", Function.identity(), null ), JolieString::fieldFromValue ),
            ValueManager.fieldFrom( v.firstChildOrDefault( "date2", Function.identity(), null ), JolieString::fieldFromValue ),
            ValueManager.fieldFrom( v.firstChildOrDefault( "date1", Function.identity(), null ), JolieString::fieldFromValue )
        );
    }
    
    public static Value toValue( DiffDateRequestType t ) {
        final Value v = Value.create();
        
        t.format().ifPresent( c -> v.getFirstChild( "format" ).setValue( c ) );
        v.getFirstChild( "date2" ).setValue( t.date2() );
        v.getFirstChild( "date1" ).setValue( t.date1() );
        
        return v;
    }
    
    public static class Builder {
        
        private String format;
        private String date2;
        private String date1;
        
        private Builder() {}
        private Builder( JolieValue j ) {
            this.format = ValueManager.fieldFrom( j.getFirstChild( "format" ), c -> c.content() instanceof JolieString content ? content.value() : null );
            this.date2 = ValueManager.fieldFrom( j.getFirstChild( "date2" ), c -> c.content() instanceof JolieString content ? content.value() : null );
            this.date1 = ValueManager.fieldFrom( j.getFirstChild( "date1" ), c -> c.content() instanceof JolieString content ? content.value() : null );
        }
        
        public Builder format( String format ) { this.format = format; return this; }
        public Builder date2( String date2 ) { this.date2 = date2; return this; }
        public Builder date1( String date1 ) { this.date1 = date1; return this; }
        
        public DiffDateRequestType build() {
            return new DiffDateRequestType( format, date2, date1 );
        }
    }
    
    public static class ListBuilder extends AbstractListBuilder<ListBuilder, DiffDateRequestType> {
        
        private ListBuilder() {}
        private ListBuilder( SequencedCollection<? extends JolieValue> c ) { super( c, DiffDateRequestType::createFrom ); }
        
        protected ListBuilder self() { return this; }
        
        public ListBuilder add( Function<Builder, DiffDateRequestType> b ) { return add( b.apply( construct() ) ); }
        public ListBuilder set( int index, Function<Builder, DiffDateRequestType> b ) { return set( index, b.apply( construct() ) ); }
        public ListBuilder reconstruct( int index, Function<Builder, DiffDateRequestType> b ) { return replace( index, j -> b.apply( constructFrom( j ) ) ); }
    }
}