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
     * end[0,1]: {@link Integer}
     * begin: {@link Integer}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 */
public final class SubStringRequest implements JolieValue {
    
    private static final Set<String> FIELD_KEYS = Set.of( "end", "begin" );
    
    private final String contentValue;
    private final Integer end;
    private final Integer begin;
    
    public SubStringRequest( String contentValue, Integer end, Integer begin ) {
        this.contentValue = ValueManager.validated( "contentValue", contentValue );
        this.end = end;
        this.begin = ValueManager.validated( "begin", begin );
    }
    
    public String contentValue() { return contentValue; }
    public Optional<Integer> end() { return Optional.ofNullable( end ); }
    public Integer begin() { return begin; }
    
    public JolieString content() { return new JolieString( contentValue ); }
    public Map<String, List<JolieValue>> children() {
        return Map.of(
            "end", end == null ? List.of() : List.of( JolieValue.create( end ) ),
            "begin", List.of( JolieValue.create( begin ) )
        );
    }
    
    public static SubStringRequest createFrom( JolieValue j ) {
        return new SubStringRequest(
            JolieString.createFrom( j ).value(),
            ValueManager.fieldFrom( j.getFirstChild( "end" ), c -> c.content() instanceof JolieInt content ? content.value() : null ),
            ValueManager.fieldFrom( j.getFirstChild( "begin" ), c -> c.content() instanceof JolieInt content ? content.value() : null )
        );
    }
    
    public static SubStringRequest fromValue( Value v ) throws TypeCheckingException {
        ValueManager.requireChildren( v, FIELD_KEYS );
        return new SubStringRequest(
            JolieString.contentFromValue( v ),
            ValueManager.fieldFrom( v.firstChildOrDefault( "end", Function.identity(), null ), JolieInt::fieldFromValue ),
            ValueManager.fieldFrom( v.firstChildOrDefault( "begin", Function.identity(), null ), JolieInt::fieldFromValue )
        );
    }
    
    public static Value toValue( SubStringRequest t ) {
        final Value v = Value.create( t.contentValue() );
        
        t.end().ifPresent( c -> v.getFirstChild( "end" ).setValue( c ) );
        v.getFirstChild( "begin" ).setValue( t.begin() );
        
        return v;
    }
}