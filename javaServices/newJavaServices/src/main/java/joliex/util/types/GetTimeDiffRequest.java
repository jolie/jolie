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
 *     time1: {@link String}
 *     time2: {@link String}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 * @see #construct()
 */
public final class GetTimeDiffRequest implements JolieValue {
    
    private static final Set<String> FIELD_KEYS = Set.of( "time1", "time2" );
    
    private final String time1;
    private final String time2;
    
    public GetTimeDiffRequest( String time1, String time2 ) {
        this.time1 = ValueManager.validated( time1 );
        this.time2 = ValueManager.validated( time2 );
    }
    
    public String time1() { return time1; }
    public String time2() { return time2; }
    
    public JolieVoid content() { return new JolieVoid(); }
    public Map<String, List<JolieValue>> children() {
        return one.util.streamex.EntryStream.of(
            "time1", List.of( JolieValue.create( time1 ) ),
            "time2", List.of( JolieValue.create( time2 ) )
        ).filterValues( Objects::nonNull ).toImmutableMap();
    }
    
    public static Builder construct() { return new Builder(); }
    
    public static ListBuilder constructList() { return new ListBuilder(); }
    
    public static Builder constructFrom( JolieValue j ) { return new Builder( j ); }
    
    public static ListBuilder constructListFrom( SequencedCollection<? extends JolieValue> c ) { return new ListBuilder( c ); }
    
    public static GetTimeDiffRequest createFrom( JolieValue j ) {
        return new GetTimeDiffRequest(
            ValueManager.fieldFrom( j.getFirstChild( "time1" ), c -> c.content() instanceof JolieString content ? content.value() : null ),
            ValueManager.fieldFrom( j.getFirstChild( "time2" ), c -> c.content() instanceof JolieString content ? content.value() : null )
        );
    }
    
    public static GetTimeDiffRequest fromValue( Value v ) throws TypeCheckingException {
        ValueManager.requireChildren( v, FIELD_KEYS );
        return new GetTimeDiffRequest(
            ValueManager.fieldFrom( v.firstChildOrDefault( "time1", Function.identity(), null ), JolieString::fieldFromValue ),
            ValueManager.fieldFrom( v.firstChildOrDefault( "time2", Function.identity(), null ), JolieString::fieldFromValue )
        );
    }
    
    public static Value toValue( GetTimeDiffRequest t ) {
        final Value v = Value.create();
        
        v.getFirstChild( "time1" ).setValue( t.time1() );
        v.getFirstChild( "time2" ).setValue( t.time2() );
        
        return v;
    }
    
    public static class Builder {
        
        private String time1;
        private String time2;
        
        private Builder() {}
        private Builder( JolieValue j ) {
            this.time1 = ValueManager.fieldFrom( j.getFirstChild( "time1" ), c -> c.content() instanceof JolieString content ? content.value() : null );
            this.time2 = ValueManager.fieldFrom( j.getFirstChild( "time2" ), c -> c.content() instanceof JolieString content ? content.value() : null );
        }
        
        public Builder time1( String time1 ) { this.time1 = time1; return this; }
        public Builder time2( String time2 ) { this.time2 = time2; return this; }
        
        public GetTimeDiffRequest build() {
            return new GetTimeDiffRequest( time1, time2 );
        }
    }
    
    public static class ListBuilder extends AbstractListBuilder<ListBuilder, GetTimeDiffRequest> {
        
        private ListBuilder() {}
        private ListBuilder( SequencedCollection<? extends JolieValue> c ) { super( c, GetTimeDiffRequest::createFrom ); }
        
        protected ListBuilder self() { return this; }
        
        public ListBuilder add( Function<Builder, GetTimeDiffRequest> b ) { return add( b.apply( construct() ) ); }
        public ListBuilder set( int index, Function<Builder, GetTimeDiffRequest> b ) { return set( index, b.apply( construct() ) ); }
        public ListBuilder reconstruct( int index, Function<Builder, GetTimeDiffRequest> b ) { return replace( index, j -> b.apply( constructFrom( j ) ) ); }
    }
}