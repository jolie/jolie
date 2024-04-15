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
 *     piece[0,2147483647]: {@link String}
 *     delimiter: {@link String}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 * @see #construct()
 */
public final class JoinRequest implements JolieValue {
    
    private static final Set<String> FIELD_KEYS = Set.of( "piece", "delimiter" );
    
    private final List<String> piece;
    private final String delimiter;
    
    public JoinRequest( SequencedCollection<String> piece, String delimiter ) {
        this.piece = ValueManager.validated( piece, 0, 2147483647 );
        this.delimiter = ValueManager.validated( delimiter );
    }
    
    public List<String> piece() { return piece; }
    public String delimiter() { return delimiter; }
    
    public JolieVoid content() { return new JolieVoid(); }
    public Map<String, List<JolieValue>> children() {
        return one.util.streamex.EntryStream.of(
            "piece", piece.parallelStream().map( JolieValue::create ).toList(),
            "delimiter", List.of( JolieValue.create( delimiter ) )
        ).filterValues( Objects::nonNull ).toImmutableMap();
    }
    
    public static Builder construct() { return new Builder(); }
    
    public static ListBuilder constructList() { return new ListBuilder(); }
    
    public static Builder constructFrom( JolieValue j ) { return new Builder( j ); }
    
    public static ListBuilder constructListFrom( SequencedCollection<? extends JolieValue> c ) { return new ListBuilder( c ); }
    
    public static JoinRequest createFrom( JolieValue j ) {
        return new JoinRequest(
            ValueManager.fieldFrom( j.getChildOrDefault( "piece", List.of() ), c -> c.content() instanceof JolieString content ? content.value() : null ),
            ValueManager.fieldFrom( j.getFirstChild( "delimiter" ), c -> c.content() instanceof JolieString content ? content.value() : null )
        );
    }
    
    public static JoinRequest fromValue( Value v ) throws TypeCheckingException {
        ValueManager.requireChildren( v, FIELD_KEYS );
        return new JoinRequest(
            ValueManager.fieldFrom( v.children().getOrDefault( "piece", ValueVector.create() ), JolieString::fieldFromValue ),
            ValueManager.fieldFrom( v.firstChildOrDefault( "delimiter", Function.identity(), null ), JolieString::fieldFromValue )
        );
    }
    
    public static Value toValue( JoinRequest t ) {
        final Value v = Value.create();
        
        t.piece().forEach( c -> v.getNewChild( "piece" ).setValue( c ) );
        v.getFirstChild( "delimiter" ).setValue( t.delimiter() );
        
        return v;
    }
    
    public static class Builder {
        
        private SequencedCollection<String> piece;
        private String delimiter;
        
        private Builder() {}
        private Builder( JolieValue j ) {
            this.piece = ValueManager.fieldFrom( j.getChildOrDefault( "piece", List.of() ), c -> c.content() instanceof JolieString content ? content.value() : null );
            this.delimiter = ValueManager.fieldFrom( j.getFirstChild( "delimiter" ), c -> c.content() instanceof JolieString content ? content.value() : null );
        }
        
        public Builder piece( SequencedCollection<String> piece ) { this.piece = piece; return this; }
        public Builder delimiter( String delimiter ) { this.delimiter = delimiter; return this; }
        
        public JoinRequest build() {
            return new JoinRequest( piece, delimiter );
        }
    }
    
    public static class ListBuilder extends AbstractListBuilder<ListBuilder, JoinRequest> {
        
        private ListBuilder() {}
        private ListBuilder( SequencedCollection<? extends JolieValue> c ) { super( c, JoinRequest::createFrom ); }
        
        protected ListBuilder self() { return this; }
        
        public ListBuilder add( Function<Builder, JoinRequest> b ) { return add( b.apply( construct() ) ); }
        public ListBuilder set( int index, Function<Builder, JoinRequest> b ) { return set( index, b.apply( construct() ) ); }
        public ListBuilder reconstruct( int index, Function<Builder, JoinRequest> b ) { return replace( index, j -> b.apply( constructFrom( j ) ) ); }
    }
}