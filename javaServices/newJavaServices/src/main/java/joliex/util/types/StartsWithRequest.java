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
 *     prefix: {@link String}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 * @see #construct()
 */
public final class StartsWithRequest implements JolieValue {
    
    private static final Set<String> FIELD_KEYS = Set.of( "prefix" );
    
    private final String contentValue;
    private final String prefix;
    
    public StartsWithRequest( String contentValue, String prefix ) {
        this.contentValue = ValueManager.validated( contentValue );
        this.prefix = ValueManager.validated( prefix );
    }
    
    public String contentValue() { return contentValue; }
    public String prefix() { return prefix; }
    
    public JolieString content() { return new JolieString( contentValue ); }
    public Map<String, List<JolieValue>> children() {
        return one.util.streamex.EntryStream.of(
            "prefix", List.of( JolieValue.create( prefix ) )
        ).filterValues( Objects::nonNull ).toImmutableMap();
    }
    
    public static Builder construct() { return new Builder(); }
    
    public static ListBuilder constructList() { return new ListBuilder(); }
    
    public static Builder constructFrom( JolieValue j ) { return new Builder( j ); }
    
    public static ListBuilder constructListFrom( SequencedCollection<? extends JolieValue> c ) { return new ListBuilder( c ); }
    
    public static StartsWithRequest createFrom( JolieValue j ) {
        return new StartsWithRequest(
            JolieString.createFrom( j ).value(),
            ValueManager.fieldFrom( j.getFirstChild( "prefix" ), c -> c.content() instanceof JolieString content ? content.value() : null )
        );
    }
    
    public static StartsWithRequest fromValue( Value v ) throws TypeCheckingException {
        ValueManager.requireChildren( v, FIELD_KEYS );
        return new StartsWithRequest(
            JolieString.contentFromValue( v ),
            ValueManager.fieldFrom( v.firstChildOrDefault( "prefix", Function.identity(), null ), JolieString::fieldFromValue )
        );
    }
    
    public static Value toValue( StartsWithRequest t ) {
        final Value v = Value.create( t.contentValue() );
        
        v.getFirstChild( "prefix" ).setValue( t.prefix() );
        
        return v;
    }
    
    public static class Builder {
        
        private String contentValue;
        private String prefix;
        
        private Builder() {}
        private Builder( JolieValue j ) {
            
            contentValue = j.content() instanceof JolieString content ? content.value() : null;
            this.prefix = ValueManager.fieldFrom( j.getFirstChild( "prefix" ), c -> c.content() instanceof JolieString content ? content.value() : null );
        }
        
        public Builder contentValue( String contentValue ) { this.contentValue = contentValue; return this; }
        public Builder prefix( String prefix ) { this.prefix = prefix; return this; }
        
        public StartsWithRequest build() {
            return new StartsWithRequest( contentValue, prefix );
        }
    }
    
    public static class ListBuilder extends AbstractListBuilder<ListBuilder, StartsWithRequest> {
        
        private ListBuilder() {}
        private ListBuilder( SequencedCollection<? extends JolieValue> c ) { super( c, StartsWithRequest::createFrom ); }
        
        protected ListBuilder self() { return this; }
        
        public ListBuilder add( Function<Builder, StartsWithRequest> b ) { return add( b.apply( construct() ) ); }
        public ListBuilder set( int index, Function<Builder, StartsWithRequest> b ) { return set( index, b.apply( construct() ) ); }
        public ListBuilder reconstruct( int index, Function<Builder, StartsWithRequest> b ) { return replace( index, j -> b.apply( constructFrom( j ) ) ); }
    }
}