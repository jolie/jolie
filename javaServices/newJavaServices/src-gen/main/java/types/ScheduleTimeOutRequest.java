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
     * timeunit[0,1]: {@link String}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 */
public final class ScheduleTimeOutRequest extends TypedStructure {
    
    private static final Set<String> FIELD_KEYS = fieldKeys( ScheduleTimeOutRequest.class );
    
    private final Integer contentValue;
    @JolieName("message")
    private final JolieValue message;
    @JolieName("operation")
    private final String operation;
    @JolieName("timeunit")
    private final String timeunit;
    
    public ScheduleTimeOutRequest( Integer contentValue, JolieValue message, String operation, String timeunit ) {
        this.contentValue = ValueManager.validated( "contentValue", contentValue );
        this.message = message;
        this.operation = operation;
        this.timeunit = timeunit;
    }
    
    public Integer contentValue() { return contentValue; }
    public Optional<JolieValue> message() { return Optional.ofNullable( message ); }
    public Optional<String> operation() { return Optional.ofNullable( operation ); }
    public Optional<String> timeunit() { return Optional.ofNullable( timeunit ); }
    
    public JolieInt content() { return new JolieInt( contentValue ); }
    
    public static ScheduleTimeOutRequest from( JolieValue j ) {
        return new ScheduleTimeOutRequest(
            JolieInt.from( j ).value(),
            ValueManager.fieldFrom( j.getFirstChild( "message" ), JolieValue::from ),
            ValueManager.fieldFrom( j.getFirstChild( "operation" ), c -> c.content() instanceof JolieString content ? content.value() : null ),
            ValueManager.fieldFrom( j.getFirstChild( "timeunit" ), c -> c.content() instanceof JolieString content ? content.value() : null )
        );
    }
    
    public static ScheduleTimeOutRequest fromValue( Value v ) throws TypeCheckingException {
        ValueManager.requireChildren( v, FIELD_KEYS );
        return new ScheduleTimeOutRequest(
            JolieInt.contentFromValue( v ),
            ValueManager.singleFieldFrom( v, "message", JolieValue::fromValue ),
            ValueManager.singleFieldFrom( v, "operation", JolieString::fieldFromValue ),
            ValueManager.singleFieldFrom( v, "timeunit", JolieString::fieldFromValue )
        );
    }
    
    public static Value toValue( ScheduleTimeOutRequest t ) {
        final Value v = Value.create( t.contentValue() );
        
        t.message().ifPresent( c -> v.getFirstChild( "message" ).deepCopy( JolieValue.toValue( c ) ) );
        t.operation().ifPresent( c -> v.getFirstChild( "operation" ).setValue( c ) );
        t.timeunit().ifPresent( c -> v.getFirstChild( "timeunit" ).setValue( c ) );
        
        return v;
    }
}