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
 *     from: {@link Integer}
 *     to: {@link Integer}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 * @see #construct()
 */
public final class SummationRequest implements JolieValue {
    
    private static final Set<String> FIELD_KEYS = Set.of( "from", "to" );
    
    private final Integer from;
    private final Integer to;
    
    public SummationRequest( Integer from, Integer to ) {
        this.from = ValueManager.validated( from );
        this.to = ValueManager.validated( to );
    }
    
    public Integer from() { return from; }
    public Integer to() { return to; }
    
    public JolieVoid content() { return new JolieVoid(); }
    public Map<String, List<JolieValue>> children() {
        return one.util.streamex.EntryStream.of(
            "from", List.of( JolieValue.create( from ) ),
            "to", List.of( JolieValue.create( to ) )
        ).filterValues( Objects::nonNull ).toImmutableMap();
    }
    
    public static Builder construct() { return new Builder(); }
    
    public static ListBuilder constructList() { return new ListBuilder(); }
    
    public static Builder constructFrom( JolieValue j ) { return new Builder( j ); }
    
    public static ListBuilder constructListFrom( SequencedCollection<? extends JolieValue> c ) { return new ListBuilder( c ); }
    
    public static SummationRequest createFrom( JolieValue j ) {
        return new SummationRequest(
            ValueManager.fieldFrom( j.getFirstChild( "from" ), c -> c.content() instanceof JolieInt content ? content.value() : null ),
            ValueManager.fieldFrom( j.getFirstChild( "to" ), c -> c.content() instanceof JolieInt content ? content.value() : null )
        );
    }
    
    public static SummationRequest fromValue( Value v ) throws TypeCheckingException {
        ValueManager.requireChildren( v, FIELD_KEYS );
        return new SummationRequest(
            ValueManager.fieldFrom( v.firstChildOrDefault( "from", Function.identity(), null ), JolieInt::fieldFromValue ),
            ValueManager.fieldFrom( v.firstChildOrDefault( "to", Function.identity(), null ), JolieInt::fieldFromValue )
        );
    }
    
    public static Value toValue( SummationRequest t ) {
        final Value v = Value.create();
        
        v.getFirstChild( "from" ).setValue( t.from() );
        v.getFirstChild( "to" ).setValue( t.to() );
        
        return v;
    }
    
    public static class Builder {
        
        private Integer from;
        private Integer to;
        
        private Builder() {}
        private Builder( JolieValue j ) {
            this.from = ValueManager.fieldFrom( j.getFirstChild( "from" ), c -> c.content() instanceof JolieInt content ? content.value() : null );
            this.to = ValueManager.fieldFrom( j.getFirstChild( "to" ), c -> c.content() instanceof JolieInt content ? content.value() : null );
        }
        
        public Builder from( Integer from ) { this.from = from; return this; }
        public Builder to( Integer to ) { this.to = to; return this; }
        
        public SummationRequest build() {
            return new SummationRequest( from, to );
        }
    }
    
    public static class ListBuilder extends AbstractListBuilder<ListBuilder, SummationRequest> {
        
        private ListBuilder() {}
        private ListBuilder( SequencedCollection<? extends JolieValue> c ) { super( c, SummationRequest::createFrom ); }
        
        protected ListBuilder self() { return this; }
        
        public ListBuilder add( Function<Builder, SummationRequest> b ) { return add( b.apply( construct() ) ); }
        public ListBuilder set( int index, Function<Builder, SummationRequest> b ) { return set( index, b.apply( construct() ) ); }
        public ListBuilder reconstruct( int index, Function<Builder, SummationRequest> b ) { return replace( index, j -> b.apply( constructFrom( j ) ) ); }
    }
}