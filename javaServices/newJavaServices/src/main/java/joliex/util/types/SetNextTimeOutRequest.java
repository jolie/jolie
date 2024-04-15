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
 *     message[0,1]: {@link JolieValue}
 *     operation[0,1]: {@link String}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 * @see #construct()
 */
public final class SetNextTimeOutRequest implements JolieValue {
    
    private static final Set<String> FIELD_KEYS = Set.of( "message", "operation" );
    
    private final Integer contentValue;
    private final JolieValue message;
    private final String operation;
    
    public SetNextTimeOutRequest( Integer contentValue, JolieValue message, String operation ) {
        this.contentValue = ValueManager.validated( contentValue );
        this.message = message;
        this.operation = operation;
    }
    
    public Integer contentValue() { return contentValue; }
    public Optional<JolieValue> message() { return Optional.ofNullable( message ); }
    public Optional<String> operation() { return Optional.ofNullable( operation ); }
    
    public JolieInt content() { return new JolieInt( contentValue ); }
    public Map<String, List<JolieValue>> children() {
        return one.util.streamex.EntryStream.of(
            "message", message == null ? null : List.<JolieValue>of( message ),
            "operation", operation == null ? null : List.of( JolieValue.create( operation ) )
        ).filterValues( Objects::nonNull ).toImmutableMap();
    }
    
    public static Builder construct() { return new Builder(); }
    
    public static ListBuilder constructList() { return new ListBuilder(); }
    
    public static Builder constructFrom( JolieValue j ) { return new Builder( j ); }
    
    public static ListBuilder constructListFrom( SequencedCollection<? extends JolieValue> c ) { return new ListBuilder( c ); }
    
    public static SetNextTimeOutRequest createFrom( JolieValue j ) {
        return new SetNextTimeOutRequest(
            JolieInt.createFrom( j ).value(),
            ValueManager.fieldFrom( j.getFirstChild( "message" ), JolieValue::createFrom ),
            ValueManager.fieldFrom( j.getFirstChild( "operation" ), c -> c.content() instanceof JolieString content ? content.value() : null )
        );
    }
    
    public static SetNextTimeOutRequest fromValue( Value v ) throws TypeCheckingException {
        ValueManager.requireChildren( v, FIELD_KEYS );
        return new SetNextTimeOutRequest(
            JolieInt.contentFromValue( v ),
            ValueManager.fieldFrom( v.firstChildOrDefault( "message", Function.identity(), null ), JolieValue::fromValue ),
            ValueManager.fieldFrom( v.firstChildOrDefault( "operation", Function.identity(), null ), JolieString::fieldFromValue )
        );
    }
    
    public static Value toValue( SetNextTimeOutRequest t ) {
        final Value v = Value.create( t.contentValue() );
        
        t.message().ifPresent( c -> v.getFirstChild( "message" ).deepCopy( JolieValue.toValue( c ) ) );
        t.operation().ifPresent( c -> v.getFirstChild( "operation" ).setValue( c ) );
        
        return v;
    }
    
    public static class Builder {
        
        private Integer contentValue;
        private JolieValue message;
        private String operation;
        
        private Builder() {}
        private Builder( JolieValue j ) {
            
            contentValue = j.content() instanceof JolieInt content ? content.value() : null;
            this.message = ValueManager.fieldFrom( j.getFirstChild( "message" ), JolieValue::createFrom );
            this.operation = ValueManager.fieldFrom( j.getFirstChild( "operation" ), c -> c.content() instanceof JolieString content ? content.value() : null );
        }
        
        public Builder contentValue( Integer contentValue ) { this.contentValue = contentValue; return this; }
        public Builder message( JolieValue message ) { this.message = message; return this; }
        public Builder message( Function<JolieValue.Builder, JolieValue> b ) { return message( b.apply( JolieValue.construct() ) ); }
        public Builder operation( String operation ) { this.operation = operation; return this; }
        
        public SetNextTimeOutRequest build() {
            return new SetNextTimeOutRequest( contentValue, message, operation );
        }
    }
    
    public static class ListBuilder extends AbstractListBuilder<ListBuilder, SetNextTimeOutRequest> {
        
        private ListBuilder() {}
        private ListBuilder( SequencedCollection<? extends JolieValue> c ) { super( c, SetNextTimeOutRequest::createFrom ); }
        
        protected ListBuilder self() { return this; }
        
        public ListBuilder add( Function<Builder, SetNextTimeOutRequest> b ) { return add( b.apply( construct() ) ); }
        public ListBuilder set( int index, Function<Builder, SetNextTimeOutRequest> b ) { return set( index, b.apply( construct() ) ); }
        public ListBuilder reconstruct( int index, Function<Builder, SetNextTimeOutRequest> b ) { return replace( index, j -> b.apply( constructFrom( j ) ) ); }
    }
}