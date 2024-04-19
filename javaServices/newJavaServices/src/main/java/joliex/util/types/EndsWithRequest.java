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
     * suffix: {@link String}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 */
public final class EndsWithRequest implements JolieValue {
    
    private static final Set<String> FIELD_KEYS = Set.of( "suffix" );
    
    private final String contentValue;
    private final String suffix;
    
    public EndsWithRequest( String contentValue, String suffix ) {
        this.contentValue = ValueManager.validated( "contentValue", contentValue );
        this.suffix = ValueManager.validated( "suffix", suffix );
    }
    
    public String contentValue() { return contentValue; }
    public String suffix() { return suffix; }
    
    public JolieString content() { return new JolieString( contentValue ); }
    public Map<String, List<JolieValue>> children() {
        return Map.of(
            "suffix", List.of( JolieValue.create( suffix ) )
        );
    }
    
    public static EndsWithRequest createFrom( JolieValue j ) {
        return new EndsWithRequest(
            JolieString.createFrom( j ).value(),
            ValueManager.fieldFrom( j.getFirstChild( "suffix" ), c -> c.content() instanceof JolieString content ? content.value() : null )
        );
    }
    
    public static EndsWithRequest fromValue( Value v ) throws TypeCheckingException {
        ValueManager.requireChildren( v, FIELD_KEYS );
        return new EndsWithRequest(
            JolieString.contentFromValue( v ),
            ValueManager.fieldFrom( v.firstChildOrDefault( "suffix", Function.identity(), null ), JolieString::fieldFromValue )
        );
    }
    
    public static Value toValue( EndsWithRequest t ) {
        final Value v = Value.create( t.contentValue() );
        
        v.getFirstChild( "suffix" ).setValue( t.suffix() );
        
        return v;
    }
}