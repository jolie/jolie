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
     * replacement: {@link String}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 */
public final class ReplaceRequest extends TypedStructure {
    
    private static final Set<String> FIELD_KEYS = fieldKeys( ReplaceRequest.class );
    
    private final String contentValue;
    @JolieName("regex")
    private final String regex;
    @JolieName("replacement")
    private final String replacement;
    
    public ReplaceRequest( String contentValue, String regex, String replacement ) {
        this.contentValue = ValueManager.validated( "contentValue", contentValue );
        this.regex = ValueManager.validated( "regex", regex );
        this.replacement = ValueManager.validated( "replacement", replacement );
    }
    
    public String contentValue() { return contentValue; }
    public String regex() { return regex; }
    public String replacement() { return replacement; }
    
    public JolieString content() { return new JolieString( contentValue ); }
    
    public static ReplaceRequest createFrom( JolieValue j ) {
        return new ReplaceRequest(
            JolieString.createFrom( j ).value(),
            ValueManager.fieldFrom( j.getFirstChild( "regex" ), c -> c.content() instanceof JolieString content ? content.value() : null ),
            ValueManager.fieldFrom( j.getFirstChild( "replacement" ), c -> c.content() instanceof JolieString content ? content.value() : null )
        );
    }
    
    public static ReplaceRequest fromValue( Value v ) throws TypeCheckingException {
        ValueManager.requireChildren( v, FIELD_KEYS );
        return new ReplaceRequest(
            JolieString.contentFromValue( v ),
            ValueManager.fieldFrom( v.firstChildOrDefault( "regex", Function.identity(), null ), JolieString::fieldFromValue ),
            ValueManager.fieldFrom( v.firstChildOrDefault( "replacement", Function.identity(), null ), JolieString::fieldFromValue )
        );
    }
    
    public static Value toValue( ReplaceRequest t ) {
        final Value v = Value.create( t.contentValue() );
        
        v.getFirstChild( "regex" ).setValue( t.regex() );
        v.getFirstChild( "replacement" ).setValue( t.replacement() );
        
        return v;
    }
}