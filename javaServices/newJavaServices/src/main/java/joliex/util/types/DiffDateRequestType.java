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
 * format[0,1]: {@link String}
 * date2: {@link String}
 * date1: {@link String}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 */
public final class DiffDateRequestType extends TypedStructure {
    
    private static final Set<String> FIELD_KEYS = fieldKeys( DiffDateRequestType.class );
    
    @JolieName("format")
    private final String format;
    @JolieName("date2")
    private final String date2;
    @JolieName("date1")
    private final String date1;
    
    public DiffDateRequestType( String format, String date2, String date1 ) {
        this.format = format;
        this.date2 = ValueManager.validated( "date2", date2 );
        this.date1 = ValueManager.validated( "date1", date1 );
    }
    
    public Optional<String> format() { return Optional.ofNullable( format ); }
    public String date2() { return date2; }
    public String date1() { return date1; }
    
    public JolieVoid content() { return new JolieVoid(); }
    
    public static DiffDateRequestType createFrom( JolieValue j ) {
        return new DiffDateRequestType(
            ValueManager.fieldFrom( j.getFirstChild( "format" ), c -> c.content() instanceof JolieString content ? content.value() : null ),
            ValueManager.fieldFrom( j.getFirstChild( "date2" ), c -> c.content() instanceof JolieString content ? content.value() : null ),
            ValueManager.fieldFrom( j.getFirstChild( "date1" ), c -> c.content() instanceof JolieString content ? content.value() : null )
        );
    }
    
    public static DiffDateRequestType fromValue( Value v ) throws TypeCheckingException {
        ValueManager.requireChildren( v, FIELD_KEYS );
        return new DiffDateRequestType(
            ValueManager.fieldFrom( v.firstChildOrDefault( "format", Function.identity(), null ), JolieString::fieldFromValue ),
            ValueManager.fieldFrom( v.firstChildOrDefault( "date2", Function.identity(), null ), JolieString::fieldFromValue ),
            ValueManager.fieldFrom( v.firstChildOrDefault( "date1", Function.identity(), null ), JolieString::fieldFromValue )
        );
    }
    
    public static Value toValue( DiffDateRequestType t ) {
        final Value v = Value.create();
        
        t.format().ifPresent( c -> v.getFirstChild( "format" ).setValue( c ) );
        v.getFirstChild( "date2" ).setValue( t.date2() );
        v.getFirstChild( "date1" ).setValue( t.date1() );
        
        return v;
    }
}