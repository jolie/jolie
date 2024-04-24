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

import java.util.Arrays;
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
 * contentValue: {@link String}
     * substring: {@link String}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 */
public final class ContainsRequest extends TypedStructure {
    
    private static final Set<String> FIELD_KEYS = fieldKeys( ContainsRequest.class );
    
    private final String contentValue;
    @JolieName("substring")
    private final String substring;
    
    public ContainsRequest( String contentValue, String substring ) {
        this.contentValue = ValueManager.validated( "contentValue", contentValue );
        this.substring = ValueManager.validated( "substring", substring );
    }
    
    public String contentValue() { return contentValue; }
    public String substring() { return substring; }
    
    public JolieString content() { return new JolieString( contentValue ); }
    
    public static ContainsRequest from( JolieValue j ) {
        return new ContainsRequest(
            JolieString.from( j ).value(),
            ValueManager.fieldFrom( j.getFirstChild( "substring" ), c -> c.content() instanceof JolieString content ? content.value() : null )
        );
    }
    
    public static ContainsRequest fromValue( Value v ) throws TypeCheckingException {
        ValueManager.requireChildren( v, FIELD_KEYS );
        return new ContainsRequest(
            JolieString.contentFromValue( v ),
            ValueManager.singleFieldFrom( v, "substring", JolieString::fieldFromValue )
        );
    }
    
    public static Value toValue( ContainsRequest t ) {
        final Value v = Value.create( t.contentValue() );
        
        v.getFirstChild( "substring" ).setValue( t.substring() );
        
        return v;
    }
}