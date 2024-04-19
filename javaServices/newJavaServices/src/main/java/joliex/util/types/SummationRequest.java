package joliex.util.types;

import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.ByteArray;
import jolie.runtime.typing.TypeCheckingException;
import jolie.runtime.embedding.java.JolieValue;
import jolie.runtime.embedding.java.JolieNative;
import jolie.runtime.embedding.java.JolieNative.*;
import jolie.runtime.embedding.java.ImmutableStructure;
import jolie.runtime.embedding.java.TypeValidationException;
import jolie.runtime.embedding.java.util.*;

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

/**
 * this class is an {@link JolieValue} which can be described as follows:
 * <pre>
 * from: {@link Integer}
 * to: {@link Integer}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 */
public final class SummationRequest implements JolieValue {
    
    private static final Set<String> FIELD_KEYS = Set.of( "from", "to" );
    
    private final Integer from;
    private final Integer to;
    
    public SummationRequest( Integer from, Integer to ) {
        this.from = ValueManager.validated( "from", from );
        this.to = ValueManager.validated( "to", to );
    }
    
    public Integer from() { return from; }
    public Integer to() { return to; }
    
    public JolieVoid content() { return new JolieVoid(); }
    public Map<String, List<JolieValue>> children() {
        return Map.of(
            "from", List.of( JolieValue.create( from ) ),
            "to", List.of( JolieValue.create( to ) )
        );
    }
    
    public static SummationRequest createFrom( JolieValue j ) {
        return new SummationRequest(
            ValueManager.fieldFrom( j.getFirstChild( "from" ), c -> c.content() instanceof JolieInt content ? content.value() : null ),
            ValueManager.fieldFrom( j.getFirstChild( "to" ), c -> c.content() instanceof JolieInt content ? content.value() : null )
        );
    }
    
    public static SummationRequest fromValue( Value v ) throws TypeCheckingException {
        ValueManager.requireChildren( v, FIELD_KEYS );
        return new SummationRequest(
            ValueManager.fieldFrom( v.firstChildOrDefault( "from", Function.identity(), null ), JolieInt::fieldFromValue ),
            ValueManager.fieldFrom( v.firstChildOrDefault( "to", Function.identity(), null ), JolieInt::fieldFromValue )
        );
    }
    
    public static Value toValue( SummationRequest t ) {
        final Value v = Value.create();
        
        v.getFirstChild( "from" ).setValue( t.from() );
        v.getFirstChild( "to" ).setValue( t.to() );
        
        return v;
    }
}