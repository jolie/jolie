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
 * contentValue: {@link Integer}
 *     group[0,2147483647]: {@link String}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 * @see #construct()
 */
public final class MatchResult implements JolieValue {
    
    private static final Set<String> FIELD_KEYS = Set.of( "group" );
    
    private final Integer contentValue;
    private final List<String> group;
    
    public MatchResult( Integer contentValue, SequencedCollection<String> group ) {
        this.contentValue = ValueManager.validated( contentValue );
        this.group = ValueManager.validated( group, 0, 2147483647 );
    }
    
    public Integer contentValue() { return contentValue; }
    public List<String> group() { return group; }
    
    public JolieInt content() { return new JolieInt( contentValue ); }
    public Map<String, List<JolieValue>> children() {
        return one.util.streamex.EntryStream.of(
            "group", group.parallelStream().map( JolieValue::create ).toList()
        ).filterValues( Objects::nonNull ).toImmutableMap();
    }
    
    public static Builder construct() { return new Builder(); }
    
    public static ListBuilder constructList() { return new ListBuilder(); }
    
    public static Builder constructFrom( JolieValue j ) { return new Builder( j ); }
    
    public static ListBuilder constructListFrom( SequencedCollection<? extends JolieValue> c ) { return new ListBuilder( c ); }
    
    public static MatchResult createFrom( JolieValue j ) {
        return new MatchResult(
            JolieInt.createFrom( j ).value(),
            ValueManager.fieldFrom( j.getChildOrDefault( "group", List.of() ), c -> c.content() instanceof JolieString content ? content.value() : null )
        );
    }
    
    public static MatchResult fromValue( Value v ) throws TypeCheckingException {
        ValueManager.requireChildren( v, FIELD_KEYS );
        return new MatchResult(
            JolieInt.contentFromValue( v ),
            ValueManager.fieldFrom( v.children().getOrDefault( "group", ValueVector.create() ), JolieString::fieldFromValue )
        );
    }
    
    public static Value toValue( MatchResult t ) {
        final Value v = Value.create( t.contentValue() );
        
        t.group().forEach( c -> v.getNewChild( "group" ).setValue( c ) );
        
        return v;
    }
    
    public static class Builder {
        
        private Integer contentValue;
        private SequencedCollection<String> group;
        
        private Builder() {}
        private Builder( JolieValue j ) {
            
            contentValue = j.content() instanceof JolieInt content ? content.value() : null;
            this.group = ValueManager.fieldFrom( j.getChildOrDefault( "group", List.of() ), c -> c.content() instanceof JolieString content ? content.value() : null );
        }
        
        public Builder contentValue( Integer contentValue ) { this.contentValue = contentValue; return this; }
        public Builder group( SequencedCollection<String> group ) { this.group = group; return this; }
        
        public MatchResult build() {
            return new MatchResult( contentValue, group );
        }
    }
    
    public static class ListBuilder extends AbstractListBuilder<ListBuilder, MatchResult> {
        
        private ListBuilder() {}
        private ListBuilder( SequencedCollection<? extends JolieValue> c ) { super( c, MatchResult::createFrom ); }
        
        protected ListBuilder self() { return this; }
        
        public ListBuilder add( Function<Builder, MatchResult> b ) { return add( b.apply( construct() ) ); }
        public ListBuilder set( int index, Function<Builder, MatchResult> b ) { return set( index, b.apply( construct() ) ); }
        public ListBuilder reconstruct( int index, Function<Builder, MatchResult> b ) { return replace( index, j -> b.apply( constructFrom( j ) ) ); }
    }
}