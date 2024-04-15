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
 *     result[0,2147483647]: {@link String}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 * @see #construct()
 */
public final class SplitResult implements JolieValue {
    
    private static final Set<String> FIELD_KEYS = Set.of( "result" );
    
    private final List<String> result;
    
    public SplitResult( SequencedCollection<String> result ) {
        this.result = ValueManager.validated( result, 0, 2147483647 );
    }
    
    public List<String> result() { return result; }
    
    public JolieVoid content() { return new JolieVoid(); }
    public Map<String, List<JolieValue>> children() {
        return one.util.streamex.EntryStream.of(
            "result", result.parallelStream().map( JolieValue::create ).toList()
        ).filterValues( Objects::nonNull ).toImmutableMap();
    }
    
    public static Builder construct() { return new Builder(); }
    
    public static ListBuilder constructList() { return new ListBuilder(); }
    
    public static Builder constructFrom( JolieValue j ) { return new Builder( j ); }
    
    public static ListBuilder constructListFrom( SequencedCollection<? extends JolieValue> c ) { return new ListBuilder( c ); }
    
    public static SplitResult createFrom( JolieValue j ) {
        return new SplitResult(
            ValueManager.fieldFrom( j.getChildOrDefault( "result", List.of() ), c -> c.content() instanceof JolieString content ? content.value() : null )
        );
    }
    
    public static SplitResult fromValue( Value v ) throws TypeCheckingException {
        ValueManager.requireChildren( v, FIELD_KEYS );
        return new SplitResult(
            ValueManager.fieldFrom( v.children().getOrDefault( "result", ValueVector.create() ), JolieString::fieldFromValue )
        );
    }
    
    public static Value toValue( SplitResult t ) {
        final Value v = Value.create();
        
        t.result().forEach( c -> v.getNewChild( "result" ).setValue( c ) );
        
        return v;
    }
    
    public static class Builder {
        
        private SequencedCollection<String> result;
        
        private Builder() {}
        private Builder( JolieValue j ) {
            this.result = ValueManager.fieldFrom( j.getChildOrDefault( "result", List.of() ), c -> c.content() instanceof JolieString content ? content.value() : null );
        }
        
        public Builder result( SequencedCollection<String> result ) { this.result = result; return this; }
        
        public SplitResult build() {
            return new SplitResult( result );
        }
    }
    
    public static class ListBuilder extends AbstractListBuilder<ListBuilder, SplitResult> {
        
        private ListBuilder() {}
        private ListBuilder( SequencedCollection<? extends JolieValue> c ) { super( c, SplitResult::createFrom ); }
        
        protected ListBuilder self() { return this; }
        
        public ListBuilder add( Function<Builder, SplitResult> b ) { return add( b.apply( construct() ) ); }
        public ListBuilder set( int index, Function<Builder, SplitResult> b ) { return set( index, b.apply( construct() ) ); }
        public ListBuilder reconstruct( int index, Function<Builder, SplitResult> b ) { return replace( index, j -> b.apply( constructFrom( j ) ) ); }
    }
}