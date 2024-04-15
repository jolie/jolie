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
 *     substring: {@link String}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 * @see #construct()
 */
public final class ContainsRequest implements JolieValue {
    
    private static final Set<String> FIELD_KEYS = Set.of( "substring" );
    
    private final String contentValue;
    private final String substring;
    
    public ContainsRequest( String contentValue, String substring ) {
        this.contentValue = ValueManager.validated( contentValue );
        this.substring = ValueManager.validated( substring );
    }
    
    public String contentValue() { return contentValue; }
    public String substring() { return substring; }
    
    public JolieString content() { return new JolieString( contentValue ); }
    public Map<String, List<JolieValue>> children() {
        return one.util.streamex.EntryStream.of(
            "substring", List.of( JolieValue.create( substring ) )
        ).filterValues( Objects::nonNull ).toImmutableMap();
    }
    
    public static Builder construct() { return new Builder(); }
    
    public static ListBuilder constructList() { return new ListBuilder(); }
    
    public static Builder constructFrom( JolieValue j ) { return new Builder( j ); }
    
    public static ListBuilder constructListFrom( SequencedCollection<? extends JolieValue> c ) { return new ListBuilder( c ); }
    
    public static ContainsRequest createFrom( JolieValue j ) {
        return new ContainsRequest(
            JolieString.createFrom( j ).value(),
            ValueManager.fieldFrom( j.getFirstChild( "substring" ), c -> c.content() instanceof JolieString content ? content.value() : null )
        );
    }
    
    public static ContainsRequest fromValue( Value v ) throws TypeCheckingException {
        ValueManager.requireChildren( v, FIELD_KEYS );
        return new ContainsRequest(
            JolieString.contentFromValue( v ),
            ValueManager.fieldFrom( v.firstChildOrDefault( "substring", Function.identity(), null ), JolieString::fieldFromValue )
        );
    }
    
    public static Value toValue( ContainsRequest t ) {
        final Value v = Value.create( t.contentValue() );
        
        v.getFirstChild( "substring" ).setValue( t.substring() );
        
        return v;
    }
    
    public static class Builder {
        
        private String contentValue;
        private String substring;
        
        private Builder() {}
        private Builder( JolieValue j ) {
            
            contentValue = j.content() instanceof JolieString content ? content.value() : null;
            this.substring = ValueManager.fieldFrom( j.getFirstChild( "substring" ), c -> c.content() instanceof JolieString content ? content.value() : null );
        }
        
        public Builder contentValue( String contentValue ) { this.contentValue = contentValue; return this; }
        public Builder substring( String substring ) { this.substring = substring; return this; }
        
        public ContainsRequest build() {
            return new ContainsRequest( contentValue, substring );
        }
    }
    
    public static class ListBuilder extends AbstractListBuilder<ListBuilder, ContainsRequest> {
        
        private ListBuilder() {}
        private ListBuilder( SequencedCollection<? extends JolieValue> c ) { super( c, ContainsRequest::createFrom ); }
        
        protected ListBuilder self() { return this; }
        
        public ListBuilder add( Function<Builder, ContainsRequest> b ) { return add( b.apply( construct() ) ); }
        public ListBuilder set( int index, Function<Builder, ContainsRequest> b ) { return set( index, b.apply( construct() ) ); }
        public ListBuilder reconstruct( int index, Function<Builder, ContainsRequest> b ) { return replace( index, j -> b.apply( constructFrom( j ) ) ); }
    }
}