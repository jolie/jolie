package types;

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
 * contentValue: {@link Integer}
     * message[0,1]: {@link JolieValue}
     * operation[0,1]: {@link String}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 */
public final class SetNextTimeOutRequest extends TypedStructure {
    
    private static final Set<String> FIELD_KEYS = fieldKeys( SetNextTimeOutRequest.class );
    
    private final Integer contentValue;
    @JolieName("message")
    private final JolieValue message;
    @JolieName("operation")
    private final String operation;
    
    public SetNextTimeOutRequest( Integer contentValue, JolieValue message, String operation ) {
        this.contentValue = ValueManager.validated( "contentValue", contentValue );
        this.message = message;
        this.operation = operation;
    }
    
    public Integer contentValue() { return contentValue; }
    public Optional<JolieValue> message() { return Optional.ofNullable( message ); }
    public Optional<String> operation() { return Optional.ofNullable( operation ); }
    
    public JolieInt content() { return new JolieInt( contentValue ); }
    
    public static SetNextTimeOutRequest from( JolieValue j ) {
        return new SetNextTimeOutRequest(
            JolieInt.from( j ).value(),
            ValueManager.fieldFrom( j.getFirstChild( "message" ), JolieValue::from ),
            ValueManager.fieldFrom( j.getFirstChild( "operation" ), c -> c.content() instanceof JolieString content ? content.value() : null )
        );
    }
    
    public static SetNextTimeOutRequest fromValue( Value v ) throws TypeCheckingException {
        ValueManager.requireChildren( v, FIELD_KEYS );
        return new SetNextTimeOutRequest(
            JolieInt.contentFromValue( v ),
            ValueManager.singleFieldFrom( v, "message", JolieValue::fromValue ),
            ValueManager.singleFieldFrom( v, "operation", JolieString::fieldFromValue )
        );
    }
    
    public static Value toValue( SetNextTimeOutRequest t ) {
        final Value v = Value.create( t.contentValue() );
        
        t.message().ifPresent( c -> v.getFirstChild( "message" ).deepCopy( JolieValue.toValue( c ) ) );
        t.operation().ifPresent( c -> v.getFirstChild( "operation" ).setValue( c ) );
        
        return v;
    }
}