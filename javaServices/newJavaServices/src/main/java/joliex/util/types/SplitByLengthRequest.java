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
 * contentValue: {@link String}
     * length: {@link Integer}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 */
public final class SplitByLengthRequest extends TypedStructure {
    
    private static final Set<String> FIELD_KEYS = fieldKeys( SplitByLengthRequest.class );
    
    private final String contentValue;
    @JolieName("length")
    private final Integer length;
    
    public SplitByLengthRequest( String contentValue, Integer length ) {
        this.contentValue = ValueManager.validated( "contentValue", contentValue );
        this.length = ValueManager.validated( "length", length );
    }
    
    public String contentValue() { return contentValue; }
    public Integer length() { return length; }
    
    public JolieString content() { return new JolieString( contentValue ); }
    
    public static SplitByLengthRequest createFrom( JolieValue j ) {
        return new SplitByLengthRequest(
            JolieString.createFrom( j ).value(),
            ValueManager.fieldFrom( j.getFirstChild( "length" ), c -> c.content() instanceof JolieInt content ? content.value() : null )
        );
    }
    
    public static SplitByLengthRequest fromValue( Value v ) throws TypeCheckingException {
        ValueManager.requireChildren( v, FIELD_KEYS );
        return new SplitByLengthRequest(
            JolieString.contentFromValue( v ),
            ValueManager.fieldFrom( v.firstChildOrDefault( "length", Function.identity(), null ), JolieInt::fieldFromValue )
        );
    }
    
    public static Value toValue( SplitByLengthRequest t ) {
        final Value v = Value.create( t.contentValue() );
        
        v.getFirstChild( "length" ).setValue( t.length() );
        
        return v;
    }
}