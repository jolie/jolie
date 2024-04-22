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
     * limit[0,1]: {@link Integer}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 */
public final class SplitRequest extends TypedStructure {
    
    private static final Set<String> FIELD_KEYS = fieldKeys( SplitRequest.class );
    
    private final String contentValue;
    @JolieName("regex")
    private final String regex;
    @JolieName("limit")
    private final Integer limit;
    
    public SplitRequest( String contentValue, String regex, Integer limit ) {
        this.contentValue = ValueManager.validated( "contentValue", contentValue );
        this.regex = ValueManager.validated( "regex", regex );
        this.limit = limit;
    }
    
    public String contentValue() { return contentValue; }
    public String regex() { return regex; }
    public Optional<Integer> limit() { return Optional.ofNullable( limit ); }
    
    public JolieString content() { return new JolieString( contentValue ); }
    
    public static SplitRequest createFrom( JolieValue j ) {
        return new SplitRequest(
            JolieString.createFrom( j ).value(),
            ValueManager.fieldFrom( j.getFirstChild( "regex" ), c -> c.content() instanceof JolieString content ? content.value() : null ),
            ValueManager.fieldFrom( j.getFirstChild( "limit" ), c -> c.content() instanceof JolieInt content ? content.value() : null )
        );
    }
    
    public static SplitRequest fromValue( Value v ) throws TypeCheckingException {
        ValueManager.requireChildren( v, FIELD_KEYS );
        return new SplitRequest(
            JolieString.contentFromValue( v ),
            ValueManager.singleFieldFrom( v, "regex", JolieString::fieldFromValue ),
            ValueManager.singleFieldFrom( v, "limit", JolieInt::fieldFromValue )
        );
    }
    
    public static Value toValue( SplitRequest t ) {
        final Value v = Value.create( t.contentValue() );
        
        v.getFirstChild( "regex" ).setValue( t.regex() );
        t.limit().ifPresent( c -> v.getFirstChild( "limit" ).setValue( c ) );
        
        return v;
    }
}