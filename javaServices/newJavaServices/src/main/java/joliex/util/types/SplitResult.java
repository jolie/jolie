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
 * result[0,2147483647]: {@link String}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 */
public final class SplitResult extends TypedStructure {
    
    private static final Set<String> FIELD_KEYS = fieldKeys( SplitResult.class );
    
    @JolieName("result")
    private final List<String> result;
    
    public SplitResult( SequencedCollection<String> result ) {
        this.result = ValueManager.validated( "result", result, 0, 2147483647 );
    }
    
    public List<String> result() { return result; }
    
    public JolieVoid content() { return new JolieVoid(); }
    
    public static SplitResult createFrom( JolieValue j ) {
        return new SplitResult(
            ValueManager.fieldFrom( j.getChildOrDefault( "result", List.of() ), c -> c.content() instanceof JolieString content ? content.value() : null )
        );
    }
    
    public static SplitResult fromValue( Value v ) throws TypeCheckingException {
        ValueManager.requireChildren( v, FIELD_KEYS );
        return new SplitResult(
            ValueManager.vectorFieldFrom( v, "result", JolieString::fieldFromValue )
        );
    }
    
    public static Value toValue( SplitResult t ) {
        final Value v = Value.create();
        
        t.result().forEach( c -> v.getNewChild( "result" ).setValue( c ) );
        
        return v;
    }
}