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
 *     charset[0,1]: {@link String}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 * @see #construct()
 */
public final class UrlDecodeRequest implements JolieValue {
    
    private static final Set<String> FIELD_KEYS = Set.of( "charset" );
    
    private final String contentValue;
    private final String charset;
    
    public UrlDecodeRequest( String contentValue, String charset ) {
        this.contentValue = ValueManager.validated( contentValue );
        this.charset = charset;
    }
    
    public String contentValue() { return contentValue; }
    public Optional<String> charset() { return Optional.ofNullable( charset ); }
    
    public JolieString content() { return new JolieString( contentValue ); }
    public Map<String, List<JolieValue>> children() {
        return one.util.streamex.EntryStream.of(
            "charset", charset == null ? null : List.of( JolieValue.create( charset ) )
        ).filterValues( Objects::nonNull ).toImmutableMap();
    }
    
    public static Builder construct() { return new Builder(); }
    
    public static ListBuilder constructList() { return new ListBuilder(); }
    
    public static Builder constructFrom( JolieValue j ) { return new Builder( j ); }
    
    public static ListBuilder constructListFrom( SequencedCollection<? extends JolieValue> c ) { return new ListBuilder( c ); }
    
    public static UrlDecodeRequest createFrom( JolieValue j ) {
        return new UrlDecodeRequest(
            JolieString.createFrom( j ).value(),
            ValueManager.fieldFrom( j.getFirstChild( "charset" ), c -> c.content() instanceof JolieString content ? content.value() : null )
        );
    }
    
    public static UrlDecodeRequest fromValue( Value v ) throws TypeCheckingException {
        ValueManager.requireChildren( v, FIELD_KEYS );
        return new UrlDecodeRequest(
            JolieString.contentFromValue( v ),
            ValueManager.fieldFrom( v.firstChildOrDefault( "charset", Function.identity(), null ), JolieString::fieldFromValue )
        );
    }
    
    public static Value toValue( UrlDecodeRequest t ) {
        final Value v = Value.create( t.contentValue() );
        
        t.charset().ifPresent( c -> v.getFirstChild( "charset" ).setValue( c ) );
        
        return v;
    }
    
    public static class Builder {
        
        private String contentValue;
        private String charset;
        
        private Builder() {}
        private Builder( JolieValue j ) {
            
            contentValue = j.content() instanceof JolieString content ? content.value() : null;
            this.charset = ValueManager.fieldFrom( j.getFirstChild( "charset" ), c -> c.content() instanceof JolieString content ? content.value() : null );
        }
        
        public Builder contentValue( String contentValue ) { this.contentValue = contentValue; return this; }
        public Builder charset( String charset ) { this.charset = charset; return this; }
        
        public UrlDecodeRequest build() {
            return new UrlDecodeRequest( contentValue, charset );
        }
    }
    
    public static class ListBuilder extends AbstractListBuilder<ListBuilder, UrlDecodeRequest> {
        
        private ListBuilder() {}
        private ListBuilder( SequencedCollection<? extends JolieValue> c ) { super( c, UrlDecodeRequest::createFrom ); }
        
        protected ListBuilder self() { return this; }
        
        public ListBuilder add( Function<Builder, UrlDecodeRequest> b ) { return add( b.apply( construct() ) ); }
        public ListBuilder set( int index, Function<Builder, UrlDecodeRequest> b ) { return set( index, b.apply( construct() ) ); }
        public ListBuilder reconstruct( int index, Function<Builder, UrlDecodeRequest> b ) { return replace( index, j -> b.apply( constructFrom( j ) ) ); }
    }
}