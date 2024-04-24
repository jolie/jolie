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
     * length: {@link Integer}
     * chars("char"): {@link String}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 */
public final class PadRequest extends TypedStructure {
    
    private static final Set<String> FIELD_KEYS = fieldKeys( PadRequest.class );
    
    private final String contentValue;
    @JolieName("length")
    private final Integer length;
    @JolieName("char")
    private final String chars;
    
    public PadRequest( String contentValue, Integer length, String chars ) {
        this.contentValue = ValueManager.validated( "contentValue", contentValue );
        this.length = ValueManager.validated( "length", length );
        this.chars = ValueManager.validated( "chars", chars );
    }
    
    public String contentValue() { return contentValue; }
    public Integer length() { return length; }
    public String chars() { return chars; }
    
    public JolieString content() { return new JolieString( contentValue ); }
    
    public static PadRequest from( JolieValue j ) {
        return new PadRequest(
            JolieString.from( j ).value(),
            ValueManager.fieldFrom( j.getFirstChild( "length" ), c -> c.content() instanceof JolieInt content ? content.value() : null ),
            ValueManager.fieldFrom( j.getFirstChild( "char" ), c -> c.content() instanceof JolieString content ? content.value() : null )
        );
    }
    
    public static PadRequest fromValue( Value v ) throws TypeCheckingException {
        ValueManager.requireChildren( v, FIELD_KEYS );
        return new PadRequest(
            JolieString.contentFromValue( v ),
            ValueManager.singleFieldFrom( v, "length", JolieInt::fieldFromValue ),
            ValueManager.singleFieldFrom( v, "char", JolieString::fieldFromValue )
        );
    }
    
    public static Value toValue( PadRequest t ) {
        final Value v = Value.create( t.contentValue() );
        
        v.getFirstChild( "length" ).setValue( t.length() );
        v.getFirstChild( "char" ).setValue( t.chars() );
        
        return v;
    }
}