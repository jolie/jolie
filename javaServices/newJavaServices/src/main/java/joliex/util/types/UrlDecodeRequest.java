package joliex.util.types;

import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.ByteArray;
import jolie.runtime.typing.TypeCheckingException;
import jolie.runtime.embedding.java.JolieValue;
import jolie.runtime.embedding.java.JolieNative;
import jolie.runtime.embedding.java.JolieNative.*;
import jolie.runtime.embedding.java.ImmutableStructure;
import jolie.runtime.embedding.java.TypeValidationException;
import jolie.runtime.embedding.java.util.*;

import java.util.ArrayList;
import java.util.Map;
import java.util.SequencedCollection;
import java.util.List;
import java.util.Optional;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.function.BinaryOperator;
import java.util.stream.Stream;
import java.util.stream.Collectors;

/**
 * this class is an {@link JolieValue} which can be described as follows:
 * <pre>
 * 
 * contentValue: {@link String}
     * charset[0,1]: {@link String}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 */
public final class UrlDecodeRequest implements JolieValue {
    
    private static final Set<String> FIELD_KEYS = Set.of( "charset" );
    
    private final String contentValue;
    private final String charset;
    
    public UrlDecodeRequest( String contentValue, String charset ) {
        this.contentValue = ValueManager.validated( "contentValue", contentValue );
        this.charset = charset;
    }
    
    public String contentValue() { return contentValue; }
    public Optional<String> charset() { return Optional.ofNullable( charset ); }
    
    public JolieString content() { return new JolieString( contentValue ); }
    public Map<String, List<JolieValue>> children() {
        return Map.of(
            "charset", charset == null ? List.of() : List.of( JolieValue.create( charset ) )
        );
    }
    
    public static UrlDecodeRequest createFrom( JolieValue j ) {
        return new UrlDecodeRequest(
            JolieString.createFrom( j ).value(),
            ValueManager.fieldFrom( j.getFirstChild( "charset" ), c -> c.content() instanceof JolieString content ? content.value() : null )
        );
    }
    
    public static UrlDecodeRequest fromValue( Value v ) throws TypeCheckingException {
        ValueManager.requireChildren( v, FIELD_KEYS );
        return new UrlDecodeRequest(
            JolieString.contentFromValue( v ),
            ValueManager.fieldFrom( v.firstChildOrDefault( "charset", Function.identity(), null ), JolieString::fieldFromValue )
        );
    }
    
    public static Value toValue( UrlDecodeRequest t ) {
        final Value v = Value.create( t.contentValue() );
        
        t.charset().ifPresent( c -> v.getFirstChild( "charset" ).setValue( c ) );
        
        return v;
    }
}