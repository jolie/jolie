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
 *     word: {@link String}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 * @see #construct()
 */
public final class IndexOfRequest implements JolieValue {
    
    private static final Set<String> FIELD_KEYS = Set.of( "word" );
    
    private final String contentValue;
    private final String word;
    
    public IndexOfRequest( String contentValue, String word ) {
        this.contentValue = ValueManager.validated( contentValue );
        this.word = ValueManager.validated( word );
    }
    
    public String contentValue() { return contentValue; }
    public String word() { return word; }
    
    public JolieString content() { return new JolieString( contentValue ); }
    public Map<String, List<JolieValue>> children() {
        return one.util.streamex.EntryStream.of(
            "word", List.of( JolieValue.create( word ) )
        ).filterValues( Objects::nonNull ).toImmutableMap();
    }
    
    public static Builder construct() { return new Builder(); }
    
    public static ListBuilder constructList() { return new ListBuilder(); }
    
    public static Builder constructFrom( JolieValue j ) { return new Builder( j ); }
    
    public static ListBuilder constructListFrom( SequencedCollection<? extends JolieValue> c ) { return new ListBuilder( c ); }
    
    public static IndexOfRequest createFrom( JolieValue j ) {
        return new IndexOfRequest(
            JolieString.createFrom( j ).value(),
            ValueManager.fieldFrom( j.getFirstChild( "word" ), c -> c.content() instanceof JolieString content ? content.value() : null )
        );
    }
    
    public static IndexOfRequest fromValue( Value v ) throws TypeCheckingException {
        ValueManager.requireChildren( v, FIELD_KEYS );
        return new IndexOfRequest(
            JolieString.contentFromValue( v ),
            ValueManager.fieldFrom( v.firstChildOrDefault( "word", Function.identity(), null ), JolieString::fieldFromValue )
        );
    }
    
    public static Value toValue( IndexOfRequest t ) {
        final Value v = Value.create( t.contentValue() );
        
        v.getFirstChild( "word" ).setValue( t.word() );
        
        return v;
    }
    
    public static class Builder {
        
        private String contentValue;
        private String word;
        
        private Builder() {}
        private Builder( JolieValue j ) {
            
            contentValue = j.content() instanceof JolieString content ? content.value() : null;
            this.word = ValueManager.fieldFrom( j.getFirstChild( "word" ), c -> c.content() instanceof JolieString content ? content.value() : null );
        }
        
        public Builder contentValue( String contentValue ) { this.contentValue = contentValue; return this; }
        public Builder word( String word ) { this.word = word; return this; }
        
        public IndexOfRequest build() {
            return new IndexOfRequest( contentValue, word );
        }
    }
    
    public static class ListBuilder extends AbstractListBuilder<ListBuilder, IndexOfRequest> {
        
        private ListBuilder() {}
        private ListBuilder( SequencedCollection<? extends JolieValue> c ) { super( c, IndexOfRequest::createFrom ); }
        
        protected ListBuilder self() { return this; }
        
        public ListBuilder add( Function<Builder, IndexOfRequest> b ) { return add( b.apply( construct() ) ); }
        public ListBuilder set( int index, Function<Builder, IndexOfRequest> b ) { return set( index, b.apply( construct() ) ); }
        public ListBuilder reconstruct( int index, Function<Builder, IndexOfRequest> b ) { return replace( index, j -> b.apply( constructFrom( j ) ) ); }
    }
}