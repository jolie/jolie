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
     * regex: {@link String}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 */
public final class MatchRequest extends TypedStructure {
    
    private static final Set<String> FIELD_KEYS = fieldKeys( MatchRequest.class );
    
    private final String contentValue;
    @JolieName("regex")
    private final String regex;
    
    public MatchRequest( String contentValue, String regex ) {
        this.contentValue = ValueManager.validated( "contentValue", contentValue );
        this.regex = ValueManager.validated( "regex", regex );
    }
    
    public String contentValue() { return contentValue; }
    public String regex() { return regex; }
    
    public JolieString content() { return new JolieString( contentValue ); }
    
    public static MatchRequest createFrom( JolieValue j ) {
        return new MatchRequest(
            JolieString.createFrom( j ).value(),
            ValueManager.fieldFrom( j.getFirstChild( "regex" ), c -> c.content() instanceof JolieString content ? content.value() : null )
        );
    }
    
    public static MatchRequest fromValue( Value v ) throws TypeCheckingException {
        ValueManager.requireChildren( v, FIELD_KEYS );
        return new MatchRequest(
            JolieString.contentFromValue( v ),
            ValueManager.singleFieldFrom( v, "regex", JolieString::fieldFromValue )
        );
    }
    
    public static Value toValue( MatchRequest t ) {
        final Value v = Value.create( t.contentValue() );
        
        v.getFirstChild( "regex" ).setValue( t.regex() );
        
        return v;
    }
}