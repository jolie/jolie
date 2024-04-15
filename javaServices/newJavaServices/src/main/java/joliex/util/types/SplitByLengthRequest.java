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
 *     length: {@link Integer}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 * @see #construct()
 */
public final class SplitByLengthRequest implements JolieValue {
    
    private static final Set<String> FIELD_KEYS = Set.of( "length" );
    
    private final String contentValue;
    private final Integer length;
    
    public SplitByLengthRequest( String contentValue, Integer length ) {
        this.contentValue = ValueManager.validated( contentValue );
        this.length = ValueManager.validated( length );
    }
    
    public String contentValue() { return contentValue; }
    public Integer length() { return length; }
    
    public JolieString content() { return new JolieString( contentValue ); }
    public Map<String, List<JolieValue>> children() {
        return one.util.streamex.EntryStream.of(
            "length", List.of( JolieValue.create( length ) )
        ).filterValues( Objects::nonNull ).toImmutableMap();
    }
    
    public static Builder construct() { return new Builder(); }
    
    public static ListBuilder constructList() { return new ListBuilder(); }
    
    public static Builder constructFrom( JolieValue j ) { return new Builder( j ); }
    
    public static ListBuilder constructListFrom( SequencedCollection<? extends JolieValue> c ) { return new ListBuilder( c ); }
    
    public static SplitByLengthRequest createFrom( JolieValue j ) {
        return new SplitByLengthRequest(
            JolieString.createFrom( j ).value(),
            ValueManager.fieldFrom( j.getFirstChild( "length" ), c -> c.content() instanceof JolieInt content ? content.value() : null )
        );
    }
    
    public static SplitByLengthRequest fromValue( Value v ) throws TypeCheckingException {
        ValueManager.requireChildren( v, FIELD_KEYS );
        return new SplitByLengthRequest(
            JolieString.contentFromValue( v ),
            ValueManager.fieldFrom( v.firstChildOrDefault( "length", Function.identity(), null ), JolieInt::fieldFromValue )
        );
    }
    
    public static Value toValue( SplitByLengthRequest t ) {
        final Value v = Value.create( t.contentValue() );
        
        v.getFirstChild( "length" ).setValue( t.length() );
        
        return v;
    }
    
    public static class Builder {
        
        private String contentValue;
        private Integer length;
        
        private Builder() {}
        private Builder( JolieValue j ) {
            
            contentValue = j.content() instanceof JolieString content ? content.value() : null;
            this.length = ValueManager.fieldFrom( j.getFirstChild( "length" ), c -> c.content() instanceof JolieInt content ? content.value() : null );
        }
        
        public Builder contentValue( String contentValue ) { this.contentValue = contentValue; return this; }
        public Builder length( Integer length ) { this.length = length; return this; }
        
        public SplitByLengthRequest build() {
            return new SplitByLengthRequest( contentValue, length );
        }
    }
    
    public static class ListBuilder extends AbstractListBuilder<ListBuilder, SplitByLengthRequest> {
        
        private ListBuilder() {}
        private ListBuilder( SequencedCollection<? extends JolieValue> c ) { super( c, SplitByLengthRequest::createFrom ); }
        
        protected ListBuilder self() { return this; }
        
        public ListBuilder add( Function<Builder, SplitByLengthRequest> b ) { return add( b.apply( construct() ) ); }
        public ListBuilder set( int index, Function<Builder, SplitByLengthRequest> b ) { return set( index, b.apply( construct() ) ); }
        public ListBuilder reconstruct( int index, Function<Builder, SplitByLengthRequest> b ) { return replace( index, j -> b.apply( constructFrom( j ) ) ); }
    }
}