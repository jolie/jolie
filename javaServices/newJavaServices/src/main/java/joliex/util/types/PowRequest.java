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
 *     base: {@link Double}
 *     exponent: {@link Double}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 * @see #construct()
 */
public final class PowRequest implements JolieValue {
    
    private static final Set<String> FIELD_KEYS = Set.of( "base", "exponent" );
    
    private final Double base;
    private final Double exponent;
    
    public PowRequest( Double base, Double exponent ) {
        this.base = ValueManager.validated( base );
        this.exponent = ValueManager.validated( exponent );
    }
    
    public Double base() { return base; }
    public Double exponent() { return exponent; }
    
    public JolieVoid content() { return new JolieVoid(); }
    public Map<String, List<JolieValue>> children() {
        return one.util.streamex.EntryStream.of(
            "base", List.of( JolieValue.create( base ) ),
            "exponent", List.of( JolieValue.create( exponent ) )
        ).filterValues( Objects::nonNull ).toImmutableMap();
    }
    
    public static Builder construct() { return new Builder(); }
    
    public static ListBuilder constructList() { return new ListBuilder(); }
    
    public static Builder constructFrom( JolieValue j ) { return new Builder( j ); }
    
    public static ListBuilder constructListFrom( SequencedCollection<? extends JolieValue> c ) { return new ListBuilder( c ); }
    
    public static PowRequest createFrom( JolieValue j ) {
        return new PowRequest(
            ValueManager.fieldFrom( j.getFirstChild( "base" ), c -> c.content() instanceof JolieDouble content ? content.value() : null ),
            ValueManager.fieldFrom( j.getFirstChild( "exponent" ), c -> c.content() instanceof JolieDouble content ? content.value() : null )
        );
    }
    
    public static PowRequest fromValue( Value v ) throws TypeCheckingException {
        ValueManager.requireChildren( v, FIELD_KEYS );
        return new PowRequest(
            ValueManager.fieldFrom( v.firstChildOrDefault( "base", Function.identity(), null ), JolieDouble::fieldFromValue ),
            ValueManager.fieldFrom( v.firstChildOrDefault( "exponent", Function.identity(), null ), JolieDouble::fieldFromValue )
        );
    }
    
    public static Value toValue( PowRequest t ) {
        final Value v = Value.create();
        
        v.getFirstChild( "base" ).setValue( t.base() );
        v.getFirstChild( "exponent" ).setValue( t.exponent() );
        
        return v;
    }
    
    public static class Builder {
        
        private Double base;
        private Double exponent;
        
        private Builder() {}
        private Builder( JolieValue j ) {
            this.base = ValueManager.fieldFrom( j.getFirstChild( "base" ), c -> c.content() instanceof JolieDouble content ? content.value() : null );
            this.exponent = ValueManager.fieldFrom( j.getFirstChild( "exponent" ), c -> c.content() instanceof JolieDouble content ? content.value() : null );
        }
        
        public Builder base( Double base ) { this.base = base; return this; }
        public Builder exponent( Double exponent ) { this.exponent = exponent; return this; }
        
        public PowRequest build() {
            return new PowRequest( base, exponent );
        }
    }
    
    public static class ListBuilder extends AbstractListBuilder<ListBuilder, PowRequest> {
        
        private ListBuilder() {}
        private ListBuilder( SequencedCollection<? extends JolieValue> c ) { super( c, PowRequest::createFrom ); }
        
        protected ListBuilder self() { return this; }
        
        public ListBuilder add( Function<Builder, PowRequest> b ) { return add( b.apply( construct() ) ); }
        public ListBuilder set( int index, Function<Builder, PowRequest> b ) { return set( index, b.apply( construct() ) ); }
        public ListBuilder reconstruct( int index, Function<Builder, PowRequest> b ) { return replace( index, j -> b.apply( constructFrom( j ) ) ); }
    }
}