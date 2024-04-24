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
     * word: {@link String}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 */
public final class IndexOfRequest extends TypedStructure {
    
    private static final Set<String> FIELD_KEYS = fieldKeys( IndexOfRequest.class );
    
    private final String contentValue;
    @JolieName("word")
    private final String word;
    
    public IndexOfRequest( String contentValue, String word ) {
        this.contentValue = ValueManager.validated( "contentValue", contentValue );
        this.word = ValueManager.validated( "word", word );
    }
    
    public String contentValue() { return contentValue; }
    public String word() { return word; }
    
    public JolieString content() { return new JolieString( contentValue ); }
    
    public static IndexOfRequest from( JolieValue j ) {
        return new IndexOfRequest(
            JolieString.from( j ).value(),
            ValueManager.fieldFrom( j.getFirstChild( "word" ), c -> c.content() instanceof JolieString content ? content.value() : null )
        );
    }
    
    public static IndexOfRequest fromValue( Value v ) throws TypeCheckingException {
        ValueManager.requireChildren( v, FIELD_KEYS );
        return new IndexOfRequest(
            JolieString.contentFromValue( v ),
            ValueManager.singleFieldFrom( v, "word", JolieString::fieldFromValue )
        );
    }
    
    public static Value toValue( IndexOfRequest t ) {
        final Value v = Value.create( t.contentValue() );
        
        v.getFirstChild( "word" ).setValue( t.word() );
        
        return v;
    }
}