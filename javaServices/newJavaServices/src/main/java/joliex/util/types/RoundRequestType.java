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
 * contentValue: {@link Double}
 *     decimals[0,1]: {@link Integer}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 * @see #construct()
 */
public final class RoundRequestType implements JolieValue {
    
    private static final Set<String> FIELD_KEYS = Set.of( "decimals" );
    
    private final Double contentValue;
    private final Integer decimals;
    
    public RoundRequestType( Double contentValue, Integer decimals ) {
        this.contentValue = ValueManager.validated( contentValue );
        this.decimals = decimals;
    }
    
    public Double contentValue() { return contentValue; }
    public Optional<Integer> decimals() { return Optional.ofNullable( decimals ); }
    
    public JolieDouble content() { return new JolieDouble( contentValue ); }
    public Map<String, List<JolieValue>> children() {
        return one.util.streamex.EntryStream.of(
            "decimals", decimals == null ? null : List.of( JolieValue.create( decimals ) )
        ).filterValues( Objects::nonNull ).toImmutableMap();
    }
    
    public static Builder construct() { return new Builder(); }
    
    public static ListBuilder constructList() { return new ListBuilder(); }
    
    public static Builder constructFrom( JolieValue j ) { return new Builder( j ); }
    
    public static ListBuilder constructListFrom( SequencedCollection<? extends JolieValue> c ) { return new ListBuilder( c ); }
    
    public static RoundRequestType createFrom( JolieValue j ) {
        return new RoundRequestType(
            JolieDouble.createFrom( j ).value(),
            ValueManager.fieldFrom( j.getFirstChild( "decimals" ), c -> c.content() instanceof JolieInt content ? content.value() : null )
        );
    }
    
    public static RoundRequestType fromValue( Value v ) throws TypeCheckingException {
        ValueManager.requireChildren( v, FIELD_KEYS );
        return new RoundRequestType(
            JolieDouble.contentFromValue( v ),
            ValueManager.fieldFrom( v.firstChildOrDefault( "decimals", Function.identity(), null ), JolieInt::fieldFromValue )
        );
    }
    
    public static Value toValue( RoundRequestType t ) {
        final Value v = Value.create( t.contentValue() );
        
        t.decimals().ifPresent( c -> v.getFirstChild( "decimals" ).setValue( c ) );
        
        return v;
    }
    
    public static class Builder {
        
        private Double contentValue;
        private Integer decimals;
        
        private Builder() {}
        private Builder( JolieValue j ) {
            
            contentValue = j.content() instanceof JolieDouble content ? content.value() : null;
            this.decimals = ValueManager.fieldFrom( j.getFirstChild( "decimals" ), c -> c.content() instanceof JolieInt content ? content.value() : null );
        }
        
        public Builder contentValue( Double contentValue ) { this.contentValue = contentValue; return this; }
        public Builder decimals( Integer decimals ) { this.decimals = decimals; return this; }
        
        public RoundRequestType build() {
            return new RoundRequestType( contentValue, decimals );
        }
    }
    
    public static class ListBuilder extends AbstractListBuilder<ListBuilder, RoundRequestType> {
        
        private ListBuilder() {}
        private ListBuilder( SequencedCollection<? extends JolieValue> c ) { super( c, RoundRequestType::createFrom ); }
        
        protected ListBuilder self() { return this; }
        
        public ListBuilder add( Function<Builder, RoundRequestType> b ) { return add( b.apply( construct() ) ); }
        public ListBuilder set( int index, Function<Builder, RoundRequestType> b ) { return set( index, b.apply( construct() ) ); }
        public ListBuilder reconstruct( int index, Function<Builder, RoundRequestType> b ) { return replace( index, j -> b.apply( constructFrom( j ) ) ); }
    }
}