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
 * format[0,1]: {@link String}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 */
public final class CurrentDateTimeRequestType extends TypedStructure {
    
    private static final Set<String> FIELD_KEYS = fieldKeys( CurrentDateTimeRequestType.class );
    
    @JolieName("format")
    private final String format;
    
    public CurrentDateTimeRequestType( String format ) {
        this.format = format;
    }
    
    public Optional<String> format() { return Optional.ofNullable( format ); }
    
    public JolieVoid content() { return new JolieVoid(); }
    
    public static CurrentDateTimeRequestType from( JolieValue j ) {
        return new CurrentDateTimeRequestType(
            ValueManager.fieldFrom( j.getFirstChild( "format" ), c -> c.content() instanceof JolieString content ? content.value() : null )
        );
    }
    
    public static CurrentDateTimeRequestType fromValue( Value v ) throws TypeCheckingException {
        ValueManager.requireChildren( v, FIELD_KEYS );
        return new CurrentDateTimeRequestType(
            ValueManager.singleFieldFrom( v, "format", JolieString::fieldFromValue )
        );
    }
    
    public static Value toValue( CurrentDateTimeRequestType t ) {
        final Value v = Value.create();
        
        t.format().ifPresent( c -> v.getFirstChild( "format" ).setValue( c ) );
        
        return v;
    }
}