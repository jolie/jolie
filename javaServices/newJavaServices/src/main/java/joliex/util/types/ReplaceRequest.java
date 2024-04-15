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
 *     regex: {@link String}
 *     replacement: {@link String}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 * @see #construct()
 */
public final class ReplaceRequest implements JolieValue {
    
    private static final Set<String> FIELD_KEYS = Set.of( "regex", "replacement" );
    
    private final String contentValue;
    private final String regex;
    private final String replacement;
    
    public ReplaceRequest( String contentValue, String regex, String replacement ) {
        this.contentValue = ValueManager.validated( contentValue );
        this.regex = ValueManager.validated( regex );
        this.replacement = ValueManager.validated( replacement );
    }
    
    public String contentValue() { return contentValue; }
    public String regex() { return regex; }
    public String replacement() { return replacement; }
    
    public JolieString content() { return new JolieString( contentValue ); }
    public Map<String, List<JolieValue>> children() {
        return one.util.streamex.EntryStream.of(
            "regex", List.of( JolieValue.create( regex ) ),
            "replacement", List.of( JolieValue.create( replacement ) )
        ).filterValues( Objects::nonNull ).toImmutableMap();
    }
    
    public static Builder construct() { return new Builder(); }
    
    public static ListBuilder constructList() { return new ListBuilder(); }
    
    public static Builder constructFrom( JolieValue j ) { return new Builder( j ); }
    
    public static ListBuilder constructListFrom( SequencedCollection<? extends JolieValue> c ) { return new ListBuilder( c ); }
    
    public static ReplaceRequest createFrom( JolieValue j ) {
        return new ReplaceRequest(
            JolieString.createFrom( j ).value(),
            ValueManager.fieldFrom( j.getFirstChild( "regex" ), c -> c.content() instanceof JolieString content ? content.value() : null ),
            ValueManager.fieldFrom( j.getFirstChild( "replacement" ), c -> c.content() instanceof JolieString content ? content.value() : null )
        );
    }
    
    public static ReplaceRequest fromValue( Value v ) throws TypeCheckingException {
        ValueManager.requireChildren( v, FIELD_KEYS );
        return new ReplaceRequest(
            JolieString.contentFromValue( v ),
            ValueManager.fieldFrom( v.firstChildOrDefault( "regex", Function.identity(), null ), JolieString::fieldFromValue ),
            ValueManager.fieldFrom( v.firstChildOrDefault( "replacement", Function.identity(), null ), JolieString::fieldFromValue )
        );
    }
    
    public static Value toValue( ReplaceRequest t ) {
        final Value v = Value.create( t.contentValue() );
        
        v.getFirstChild( "regex" ).setValue( t.regex() );
        v.getFirstChild( "replacement" ).setValue( t.replacement() );
        
        return v;
    }
    
    public static class Builder {
        
        private String contentValue;
        private String regex;
        private String replacement;
        
        private Builder() {}
        private Builder( JolieValue j ) {
            
            contentValue = j.content() instanceof JolieString content ? content.value() : null;
            this.regex = ValueManager.fieldFrom( j.getFirstChild( "regex" ), c -> c.content() instanceof JolieString content ? content.value() : null );
            this.replacement = ValueManager.fieldFrom( j.getFirstChild( "replacement" ), c -> c.content() instanceof JolieString content ? content.value() : null );
        }
        
        public Builder contentValue( String contentValue ) { this.contentValue = contentValue; return this; }
        public Builder regex( String regex ) { this.regex = regex; return this; }
        public Builder replacement( String replacement ) { this.replacement = replacement; return this; }
        
        public ReplaceRequest build() {
            return new ReplaceRequest( contentValue, regex, replacement );
        }
    }
    
    public static class ListBuilder extends AbstractListBuilder<ListBuilder, ReplaceRequest> {
        
        private ListBuilder() {}
        private ListBuilder( SequencedCollection<? extends JolieValue> c ) { super( c, ReplaceRequest::createFrom ); }
        
        protected ListBuilder self() { return this; }
        
        public ListBuilder add( Function<Builder, ReplaceRequest> b ) { return add( b.apply( construct() ) ); }
        public ListBuilder set( int index, Function<Builder, ReplaceRequest> b ) { return set( index, b.apply( construct() ) ); }
        public ListBuilder reconstruct( int index, Function<Builder, ReplaceRequest> b ) { return replace( index, j -> b.apply( constructFrom( j ) ) ); }
    }
}