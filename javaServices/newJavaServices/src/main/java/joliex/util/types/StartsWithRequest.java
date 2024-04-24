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
     * prefix: {@link String}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 */
public final class StartsWithRequest extends TypedStructure {
    
    private static final Set<String> FIELD_KEYS = fieldKeys( StartsWithRequest.class );
    
    private final String contentValue;
    @JolieName("prefix")
    private final String prefix;
    
    public StartsWithRequest( String contentValue, String prefix ) {
        this.contentValue = ValueManager.validated( "contentValue", contentValue );
        this.prefix = ValueManager.validated( "prefix", prefix );
    }
    
    public String contentValue() { return contentValue; }
    public String prefix() { return prefix; }
    
    public JolieString content() { return new JolieString( contentValue ); }
    
    public static StartsWithRequest from( JolieValue j ) {
        return new StartsWithRequest(
            JolieString.from( j ).value(),
            ValueManager.fieldFrom( j.getFirstChild( "prefix" ), c -> c.content() instanceof JolieString content ? content.value() : null )
        );
    }
    
    public static StartsWithRequest fromValue( Value v ) throws TypeCheckingException {
        ValueManager.requireChildren( v, FIELD_KEYS );
        return new StartsWithRequest(
            JolieString.contentFromValue( v ),
            ValueManager.singleFieldFrom( v, "prefix", JolieString::fieldFromValue )
        );
    }
    
    public static Value toValue( StartsWithRequest t ) {
        final Value v = Value.create( t.contentValue() );
        
        v.getFirstChild( "prefix" ).setValue( t.prefix() );
        
        return v;
    }
}