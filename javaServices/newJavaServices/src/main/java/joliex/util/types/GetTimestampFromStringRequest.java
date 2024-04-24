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
     * format[0,1]: {@link String}
     * language[0,1]: {@link String}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 */
public final class GetTimestampFromStringRequest extends TypedStructure {
    
    private static final Set<String> FIELD_KEYS = fieldKeys( GetTimestampFromStringRequest.class );
    
    private final String contentValue;
    @JolieName("format")
    private final String format;
    @JolieName("language")
    private final String language;
    
    public GetTimestampFromStringRequest( String contentValue, String format, String language ) {
        this.contentValue = ValueManager.validated( "contentValue", contentValue );
        this.format = format;
        this.language = language;
    }
    
    public String contentValue() { return contentValue; }
    public Optional<String> format() { return Optional.ofNullable( format ); }
    public Optional<String> language() { return Optional.ofNullable( language ); }
    
    public JolieString content() { return new JolieString( contentValue ); }
    
    public static GetTimestampFromStringRequest from( JolieValue j ) {
        return new GetTimestampFromStringRequest(
            JolieString.from( j ).value(),
            ValueManager.fieldFrom( j.getFirstChild( "format" ), c -> c.content() instanceof JolieString content ? content.value() : null ),
            ValueManager.fieldFrom( j.getFirstChild( "language" ), c -> c.content() instanceof JolieString content ? content.value() : null )
        );
    }
    
    public static GetTimestampFromStringRequest fromValue( Value v ) throws TypeCheckingException {
        ValueManager.requireChildren( v, FIELD_KEYS );
        return new GetTimestampFromStringRequest(
            JolieString.contentFromValue( v ),
            ValueManager.singleFieldFrom( v, "format", JolieString::fieldFromValue ),
            ValueManager.singleFieldFrom( v, "language", JolieString::fieldFromValue )
        );
    }
    
    public static Value toValue( GetTimestampFromStringRequest t ) {
        final Value v = Value.create( t.contentValue() );
        
        t.format().ifPresent( c -> v.getFirstChild( "format" ).setValue( c ) );
        t.language().ifPresent( c -> v.getFirstChild( "language" ).setValue( c ) );
        
        return v;
    }
}