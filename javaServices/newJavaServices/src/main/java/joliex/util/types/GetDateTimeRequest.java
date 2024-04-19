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
 * contentValue: {@link Long}
     * format[0,1]: {@link String}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 */
public final class GetDateTimeRequest implements JolieValue {
    
    private static final Set<String> FIELD_KEYS = Set.of( "format" );
    
    private final Long contentValue;
    private final String format;
    
    public GetDateTimeRequest( Long contentValue, String format ) {
        this.contentValue = ValueManager.validated( "contentValue", contentValue );
        this.format = format;
    }
    
    public Long contentValue() { return contentValue; }
    public Optional<String> format() { return Optional.ofNullable( format ); }
    
    public JolieLong content() { return new JolieLong( contentValue ); }
    public Map<String, List<JolieValue>> children() {
        return Map.of(
            "format", format == null ? List.of() : List.of( JolieValue.create( format ) )
        );
    }
    
    public static GetDateTimeRequest createFrom( JolieValue j ) {
        return new GetDateTimeRequest(
            JolieLong.createFrom( j ).value(),
            ValueManager.fieldFrom( j.getFirstChild( "format" ), c -> c.content() instanceof JolieString content ? content.value() : null )
        );
    }
    
    public static GetDateTimeRequest fromValue( Value v ) throws TypeCheckingException {
        ValueManager.requireChildren( v, FIELD_KEYS );
        return new GetDateTimeRequest(
            JolieLong.contentFromValue( v ),
            ValueManager.fieldFrom( v.firstChildOrDefault( "format", Function.identity(), null ), JolieString::fieldFromValue )
        );
    }
    
    public static Value toValue( GetDateTimeRequest t ) {
        final Value v = Value.create( t.contentValue() );
        
        t.format().ifPresent( c -> v.getFirstChild( "format" ).setValue( c ) );
        
        return v;
    }
}