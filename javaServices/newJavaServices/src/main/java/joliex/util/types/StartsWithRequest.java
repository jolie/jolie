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
 * 
 * contentValue: {@link String}
     * prefix: {@link String}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 */
public final class StartsWithRequest implements JolieValue {
    
    private static final Set<String> FIELD_KEYS = Set.of( "prefix" );
    
    private final String contentValue;
    private final String prefix;
    
    public StartsWithRequest( String contentValue, String prefix ) {
        this.contentValue = ValueManager.validated( "contentValue", contentValue );
        this.prefix = ValueManager.validated( "prefix", prefix );
    }
    
    public String contentValue() { return contentValue; }
    public String prefix() { return prefix; }
    
    public JolieString content() { return new JolieString( contentValue ); }
    public Map<String, List<JolieValue>> children() {
        return Map.of(
            "prefix", List.of( JolieValue.create( prefix ) )
        );
    }
    
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
}