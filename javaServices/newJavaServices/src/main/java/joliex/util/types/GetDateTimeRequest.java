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
 * contentValue: {@link Long}
     * format[0,1]: {@link String}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 */
public final class GetDateTimeRequest extends TypedStructure {
    
    private static final Set<String> FIELD_KEYS = fieldKeys( GetDateTimeRequest.class );
    
    private final Long contentValue;
    @JolieName("format")
    private final String format;
    
    public GetDateTimeRequest( Long contentValue, String format ) {
        this.contentValue = ValueManager.validated( "contentValue", contentValue );
        this.format = format;
    }
    
    public Long contentValue() { return contentValue; }
    public Optional<String> format() { return Optional.ofNullable( format ); }
    
    public JolieLong content() { return new JolieLong( contentValue ); }
    
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