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
     * format[0,1]: {@link String}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 */
public final class DateValuesRequestType extends TypedStructure {
    
    private static final Set<String> FIELD_KEYS = fieldKeys( DateValuesRequestType.class );
    
    private final String contentValue;
    @JolieName("format")
    private final String format;
    
    public DateValuesRequestType( String contentValue, String format ) {
        this.contentValue = ValueManager.validated( "contentValue", contentValue );
        this.format = format;
    }
    
    public String contentValue() { return contentValue; }
    public Optional<String> format() { return Optional.ofNullable( format ); }
    
    public JolieString content() { return new JolieString( contentValue ); }
    
    public static DateValuesRequestType createFrom( JolieValue j ) {
        return new DateValuesRequestType(
            JolieString.createFrom( j ).value(),
            ValueManager.fieldFrom( j.getFirstChild( "format" ), c -> c.content() instanceof JolieString content ? content.value() : null )
        );
    }
    
    public static DateValuesRequestType fromValue( Value v ) throws TypeCheckingException {
        ValueManager.requireChildren( v, FIELD_KEYS );
        return new DateValuesRequestType(
            JolieString.contentFromValue( v ),
            ValueManager.singleFieldFrom( v, "format", JolieString::fieldFromValue )
        );
    }
    
    public static Value toValue( DateValuesRequestType t ) {
        final Value v = Value.create( t.contentValue() );
        
        t.format().ifPresent( c -> v.getFirstChild( "format" ).setValue( c ) );
        
        return v;
    }
}