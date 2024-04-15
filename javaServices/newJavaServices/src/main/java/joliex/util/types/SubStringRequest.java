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
 * contentValue: {@link String}
 *     end[0,1]: {@link Integer}
 *     begin: {@link Integer}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 * @see #construct()
 */
public final class SubStringRequest implements JolieValue {
    
    private static final Set<String> FIELD_KEYS = Set.of( "end", "begin" );
    
    private final String contentValue;
    private final Integer end;
    private final Integer begin;
    
    public SubStringRequest( String contentValue, Integer end, Integer begin ) {
        this.contentValue = ValueManager.validated( contentValue );
        this.end = end;
        this.begin = ValueManager.validated( begin );
    }
    
    public String contentValue() { return contentValue; }
    public Optional<Integer> end() { return Optional.ofNullable( end ); }
    public Integer begin() { return begin; }
    
    public JolieString content() { return new JolieString( contentValue ); }
    public Map<String, List<JolieValue>> children() {
        return one.util.streamex.EntryStream.of(
            "end", end == null ? null : List.of( JolieValue.create( end ) ),
            "begin", List.of( JolieValue.create( begin ) )
        ).filterValues( Objects::nonNull ).toImmutableMap();
    }
    
    public static Builder construct() { return new Builder(); }
    
    public static ListBuilder constructList() { return new ListBuilder(); }
    
    public static Builder constructFrom( JolieValue j ) { return new Builder( j ); }
    
    public static ListBuilder constructListFrom( SequencedCollection<? extends JolieValue> c ) { return new ListBuilder( c ); }
    
    public static SubStringRequest createFrom( JolieValue j ) {
        return new SubStringRequest(
            JolieString.createFrom( j ).value(),
            ValueManager.fieldFrom( j.getFirstChild( "end" ), c -> c.content() instanceof JolieInt content ? content.value() : null ),
            ValueManager.fieldFrom( j.getFirstChild( "begin" ), c -> c.content() instanceof JolieInt content ? content.value() : null )
        );
    }
    
    public static SubStringRequest fromValue( Value v ) throws TypeCheckingException {
        ValueManager.requireChildren( v, FIELD_KEYS );
        return new SubStringRequest(
            JolieString.contentFromValue( v ),
            ValueManager.fieldFrom( v.firstChildOrDefault( "end", Function.identity(), null ), JolieInt::fieldFromValue ),
            ValueManager.fieldFrom( v.firstChildOrDefault( "begin", Function.identity(), null ), JolieInt::fieldFromValue )
        );
    }
    
    public static Value toValue( SubStringRequest t ) {
        final Value v = Value.create( t.contentValue() );
        
        t.end().ifPresent( c -> v.getFirstChild( "end" ).setValue( c ) );
        v.getFirstChild( "begin" ).setValue( t.begin() );
        
        return v;
    }
    
    public static class Builder {
        
        private String contentValue;
        private Integer end;
        private Integer begin;
        
        private Builder() {}
        private Builder( JolieValue j ) {
            
            contentValue = j.content() instanceof JolieString content ? content.value() : null;
            this.end = ValueManager.fieldFrom( j.getFirstChild( "end" ), c -> c.content() instanceof JolieInt content ? content.value() : null );
            this.begin = ValueManager.fieldFrom( j.getFirstChild( "begin" ), c -> c.content() instanceof JolieInt content ? content.value() : null );
        }
        
        public Builder contentValue( String contentValue ) { this.contentValue = contentValue; return this; }
        public Builder end( Integer end ) { this.end = end; return this; }
        public Builder begin( Integer begin ) { this.begin = begin; return this; }
        
        public SubStringRequest build() {
            return new SubStringRequest( contentValue, end, begin );
        }
    }
    
    public static class ListBuilder extends AbstractListBuilder<ListBuilder, SubStringRequest> {
        
        private ListBuilder() {}
        private ListBuilder( SequencedCollection<? extends JolieValue> c ) { super( c, SubStringRequest::createFrom ); }
        
        protected ListBuilder self() { return this; }
        
        public ListBuilder add( Function<Builder, SubStringRequest> b ) { return add( b.apply( construct() ) ); }
        public ListBuilder set( int index, Function<Builder, SubStringRequest> b ) { return set( index, b.apply( construct() ) ); }
        public ListBuilder reconstruct( int index, Function<Builder, SubStringRequest> b ) { return replace( index, j -> b.apply( constructFrom( j ) ) ); }
    }
}