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
 * piece[0,2147483647]: {@link String}
 * delimiter: {@link String}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 */
public final class JoinRequest extends TypedStructure {
    
    private static final Set<String> FIELD_KEYS = fieldKeys( JoinRequest.class );
    
    @JolieName("piece")
    private final List<String> piece;
    @JolieName("delimiter")
    private final String delimiter;
    
    public JoinRequest( SequencedCollection<String> piece, String delimiter ) {
        this.piece = ValueManager.validated( "piece", piece, 0, 2147483647 );
        this.delimiter = ValueManager.validated( "delimiter", delimiter );
    }
    
    public List<String> piece() { return piece; }
    public String delimiter() { return delimiter; }
    
    public JolieVoid content() { return new JolieVoid(); }
    
    public static JoinRequest createFrom( JolieValue j ) {
        return new JoinRequest(
            ValueManager.fieldFrom( j.getChildOrDefault( "piece", List.of() ), c -> c.content() instanceof JolieString content ? content.value() : null ),
            ValueManager.fieldFrom( j.getFirstChild( "delimiter" ), c -> c.content() instanceof JolieString content ? content.value() : null )
        );
    }
    
    public static JoinRequest fromValue( Value v ) throws TypeCheckingException {
        ValueManager.requireChildren( v, FIELD_KEYS );
        return new JoinRequest(
            ValueManager.fieldFrom( v.children().getOrDefault( "piece", ValueVector.create() ), JolieString::fieldFromValue ),
            ValueManager.fieldFrom( v.firstChildOrDefault( "delimiter", Function.identity(), null ), JolieString::fieldFromValue )
        );
    }
    
    public static Value toValue( JoinRequest t ) {
        final Value v = Value.create();
        
        t.piece().forEach( c -> v.getNewChild( "piece" ).setValue( c ) );
        v.getFirstChild( "delimiter" ).setValue( t.delimiter() );
        
        return v;
    }
}