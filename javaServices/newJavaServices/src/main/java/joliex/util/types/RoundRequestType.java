package joliex.util.types;

import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.ByteArray;
import jolie.runtime.typing.TypeCheckingException;
import jolie.runtime.embedding.java.JolieValue;
import jolie.runtime.embedding.java.JolieNative;
import jolie.runtime.embedding.java.JolieNative.*;
import jolie.runtime.embedding.java.TypedStructure;
import jolie.runtime.embedding.java.UntypedStructure;
import jolie.runtime.embedding.java.TypeValidationException;
import jolie.runtime.embedding.java.util.*;

import java.util.Map;
import java.util.SequencedCollection;
import java.util.List;
import java.util.Optional;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * this class is a {@link TypedStructure} which can be described as follows:
 * <pre>
 * 
 * contentValue: {@link Double}
     * decimals[0,1]: {@link Integer}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 */
public final class RoundRequestType extends TypedStructure {
    
    private static final Set<String> FIELD_KEYS = fieldKeys( RoundRequestType.class );
    
    private final Double contentValue;
    @JolieName("decimals")
    private final Integer decimals;
    
    public RoundRequestType( Double contentValue, Integer decimals ) {
        this.contentValue = ValueManager.validated( "contentValue", contentValue );
        this.decimals = decimals;
    }
    
    public Double contentValue() { return contentValue; }
    public Optional<Integer> decimals() { return Optional.ofNullable( decimals ); }
    
    public JolieDouble content() { return new JolieDouble( contentValue ); }
    
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
}