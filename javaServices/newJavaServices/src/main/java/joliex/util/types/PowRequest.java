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
 * base: {@link Double}
 * exponent: {@link Double}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 */
public final class PowRequest implements JolieValue {
    
    private static final Set<String> FIELD_KEYS = Set.of( "base", "exponent" );
    
    private final Double base;
    private final Double exponent;
    
    public PowRequest( Double base, Double exponent ) {
        this.base = ValueManager.validated( "base", base );
        this.exponent = ValueManager.validated( "exponent", exponent );
    }
    
    public Double base() { return base; }
    public Double exponent() { return exponent; }
    
    public JolieVoid content() { return new JolieVoid(); }
    public Map<String, List<JolieValue>> children() {
        return Map.of(
            "base", List.of( JolieValue.create( base ) ),
            "exponent", List.of( JolieValue.create( exponent ) )
        );
    }
    
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
}