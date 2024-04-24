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
 * time1: {@link String}
 * time2: {@link String}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 */
public final class GetTimeDiffRequest extends TypedStructure {
    
    private static final Set<String> FIELD_KEYS = fieldKeys( GetTimeDiffRequest.class );
    
    @JolieName("time1")
    private final String time1;
    @JolieName("time2")
    private final String time2;
    
    public GetTimeDiffRequest( String time1, String time2 ) {
        this.time1 = ValueManager.validated( "time1", time1 );
        this.time2 = ValueManager.validated( "time2", time2 );
    }
    
    public String time1() { return time1; }
    public String time2() { return time2; }
    
    public JolieVoid content() { return new JolieVoid(); }
    
    public static GetTimeDiffRequest from( JolieValue j ) {
        return new GetTimeDiffRequest(
            ValueManager.fieldFrom( j.getFirstChild( "time1" ), c -> c.content() instanceof JolieString content ? content.value() : null ),
            ValueManager.fieldFrom( j.getFirstChild( "time2" ), c -> c.content() instanceof JolieString content ? content.value() : null )
        );
    }
    
    public static GetTimeDiffRequest fromValue( Value v ) throws TypeCheckingException {
        ValueManager.requireChildren( v, FIELD_KEYS );
        return new GetTimeDiffRequest(
            ValueManager.singleFieldFrom( v, "time1", JolieString::fieldFromValue ),
            ValueManager.singleFieldFrom( v, "time2", JolieString::fieldFromValue )
        );
    }
    
    public static Value toValue( GetTimeDiffRequest t ) {
        final Value v = Value.create();
        
        v.getFirstChild( "time1" ).setValue( t.time1() );
        v.getFirstChild( "time2" ).setValue( t.time2() );
        
        return v;
    }
}