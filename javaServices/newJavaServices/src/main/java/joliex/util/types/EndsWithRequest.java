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
 *     suffix: {@link String}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 * @see #construct()
 */
public final class EndsWithRequest implements JolieValue {
    
    private static final Set<String> FIELD_KEYS = Set.of( "suffix" );
    
    private final String contentValue;
    private final String suffix;
    
    public EndsWithRequest( String contentValue, String suffix ) {
        this.contentValue = ValueManager.validated( contentValue );
        this.suffix = ValueManager.validated( suffix );
    }
    
    public String contentValue() { return contentValue; }
    public String suffix() { return suffix; }
    
    public JolieString content() { return new JolieString( contentValue ); }
    public Map<String, List<JolieValue>> children() {
        return one.util.streamex.EntryStream.of(
            "suffix", List.of( JolieValue.create( suffix ) )
        ).filterValues( Objects::nonNull ).toImmutableMap();
    }
    
    public static Builder construct() { return new Builder(); }
    
    public static ListBuilder constructList() { return new ListBuilder(); }
    
    public static Builder constructFrom( JolieValue j ) { return new Builder( j ); }
    
    public static ListBuilder constructListFrom( SequencedCollection<? extends JolieValue> c ) { return new ListBuilder( c ); }
    
    public static EndsWithRequest createFrom( JolieValue j ) {
        return new EndsWithRequest(
            JolieString.createFrom( j ).value(),
            ValueManager.fieldFrom( j.getFirstChild( "suffix" ), c -> c.content() instanceof JolieString content ? content.value() : null )
        );
    }
    
    public static EndsWithRequest fromValue( Value v ) throws TypeCheckingException {
        ValueManager.requireChildren( v, FIELD_KEYS );
        return new EndsWithRequest(
            JolieString.contentFromValue( v ),
            ValueManager.fieldFrom( v.firstChildOrDefault( "suffix", Function.identity(), null ), JolieString::fieldFromValue )
        );
    }
    
    public static Value toValue( EndsWithRequest t ) {
        final Value v = Value.create( t.contentValue() );
        
        v.getFirstChild( "suffix" ).setValue( t.suffix() );
        
        return v;
    }
    
    public static class Builder {
        
        private String contentValue;
        private String suffix;
        
        private Builder() {}
        private Builder( JolieValue j ) {
            
            contentValue = j.content() instanceof JolieString content ? content.value() : null;
            this.suffix = ValueManager.fieldFrom( j.getFirstChild( "suffix" ), c -> c.content() instanceof JolieString content ? content.value() : null );
        }
        
        public Builder contentValue( String contentValue ) { this.contentValue = contentValue; return this; }
        public Builder suffix( String suffix ) { this.suffix = suffix; return this; }
        
        public EndsWithRequest build() {
            return new EndsWithRequest( contentValue, suffix );
        }
    }
    
    public static class ListBuilder extends AbstractListBuilder<ListBuilder, EndsWithRequest> {
        
        private ListBuilder() {}
        private ListBuilder( SequencedCollection<? extends JolieValue> c ) { super( c, EndsWithRequest::createFrom ); }
        
        protected ListBuilder self() { return this; }
        
        public ListBuilder add( Function<Builder, EndsWithRequest> b ) { return add( b.apply( construct() ) ); }
        public ListBuilder set( int index, Function<Builder, EndsWithRequest> b ) { return set( index, b.apply( construct() ) ); }
        public ListBuilder reconstruct( int index, Function<Builder, EndsWithRequest> b ) { return replace( index, j -> b.apply( constructFrom( j ) ) ); }
    }
}