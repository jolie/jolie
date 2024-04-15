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
 *     format[0,1]: {@link String}
 *     language[0,1]: {@link String}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 * @see #construct()
 */
public final class GetTimestampFromStringRequest implements JolieValue {
    
    private static final Set<String> FIELD_KEYS = Set.of( "format", "language" );
    
    private final String contentValue;
    private final String format;
    private final String language;
    
    public GetTimestampFromStringRequest( String contentValue, String format, String language ) {
        this.contentValue = ValueManager.validated( contentValue );
        this.format = format;
        this.language = language;
    }
    
    public String contentValue() { return contentValue; }
    public Optional<String> format() { return Optional.ofNullable( format ); }
    public Optional<String> language() { return Optional.ofNullable( language ); }
    
    public JolieString content() { return new JolieString( contentValue ); }
    public Map<String, List<JolieValue>> children() {
        return one.util.streamex.EntryStream.of(
            "format", format == null ? null : List.of( JolieValue.create( format ) ),
            "language", language == null ? null : List.of( JolieValue.create( language ) )
        ).filterValues( Objects::nonNull ).toImmutableMap();
    }
    
    public static Builder construct() { return new Builder(); }
    
    public static ListBuilder constructList() { return new ListBuilder(); }
    
    public static Builder constructFrom( JolieValue j ) { return new Builder( j ); }
    
    public static ListBuilder constructListFrom( SequencedCollection<? extends JolieValue> c ) { return new ListBuilder( c ); }
    
    public static GetTimestampFromStringRequest createFrom( JolieValue j ) {
        return new GetTimestampFromStringRequest(
            JolieString.createFrom( j ).value(),
            ValueManager.fieldFrom( j.getFirstChild( "format" ), c -> c.content() instanceof JolieString content ? content.value() : null ),
            ValueManager.fieldFrom( j.getFirstChild( "language" ), c -> c.content() instanceof JolieString content ? content.value() : null )
        );
    }
    
    public static GetTimestampFromStringRequest fromValue( Value v ) throws TypeCheckingException {
        ValueManager.requireChildren( v, FIELD_KEYS );
        return new GetTimestampFromStringRequest(
            JolieString.contentFromValue( v ),
            ValueManager.fieldFrom( v.firstChildOrDefault( "format", Function.identity(), null ), JolieString::fieldFromValue ),
            ValueManager.fieldFrom( v.firstChildOrDefault( "language", Function.identity(), null ), JolieString::fieldFromValue )
        );
    }
    
    public static Value toValue( GetTimestampFromStringRequest t ) {
        final Value v = Value.create( t.contentValue() );
        
        t.format().ifPresent( c -> v.getFirstChild( "format" ).setValue( c ) );
        t.language().ifPresent( c -> v.getFirstChild( "language" ).setValue( c ) );
        
        return v;
    }
    
    public static class Builder {
        
        private String contentValue;
        private String format;
        private String language;
        
        private Builder() {}
        private Builder( JolieValue j ) {
            
            contentValue = j.content() instanceof JolieString content ? content.value() : null;
            this.format = ValueManager.fieldFrom( j.getFirstChild( "format" ), c -> c.content() instanceof JolieString content ? content.value() : null );
            this.language = ValueManager.fieldFrom( j.getFirstChild( "language" ), c -> c.content() instanceof JolieString content ? content.value() : null );
        }
        
        public Builder contentValue( String contentValue ) { this.contentValue = contentValue; return this; }
        public Builder format( String format ) { this.format = format; return this; }
        public Builder language( String language ) { this.language = language; return this; }
        
        public GetTimestampFromStringRequest build() {
            return new GetTimestampFromStringRequest( contentValue, format, language );
        }
    }
    
    public static class ListBuilder extends AbstractListBuilder<ListBuilder, GetTimestampFromStringRequest> {
        
        private ListBuilder() {}
        private ListBuilder( SequencedCollection<? extends JolieValue> c ) { super( c, GetTimestampFromStringRequest::createFrom ); }
        
        protected ListBuilder self() { return this; }
        
        public ListBuilder add( Function<Builder, GetTimestampFromStringRequest> b ) { return add( b.apply( construct() ) ); }
        public ListBuilder set( int index, Function<Builder, GetTimestampFromStringRequest> b ) { return set( index, b.apply( construct() ) ); }
        public ListBuilder reconstruct( int index, Function<Builder, GetTimestampFromStringRequest> b ) { return replace( index, j -> b.apply( constructFrom( j ) ) ); }
    }
}