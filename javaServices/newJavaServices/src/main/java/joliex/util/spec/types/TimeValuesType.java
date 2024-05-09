package joliex.util.spec.types;

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
 * hour: {@link Integer}
 * minute: {@link Integer}
 * second: {@link Integer}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 * @see #builder()
 */
public final class TimeValuesType extends TypedStructure {
    
    private static final Set<String> FIELD_KEYS = fieldKeys( TimeValuesType.class );
    
    @JolieName("hour")
    private final Integer hour;
    @JolieName("minute")
    private final Integer minute;
    @JolieName("second")
    private final Integer second;
    
    public TimeValuesType( Integer hour, Integer minute, Integer second ) {
        this.hour = ValueManager.validated( "hour", hour );
        this.minute = ValueManager.validated( "minute", minute );
        this.second = ValueManager.validated( "second", second );
    }
    
    public Integer hour() { return hour; }
    public Integer minute() { return minute; }
    public Integer second() { return second; }
    
    public JolieVoid content() { return new JolieVoid(); }
    
    public static Builder builder() { return new Builder(); }
    public static Builder builder( JolieValue from ) { return new Builder( from ); }
    
    public static StructureListBuilder<TimeValuesType,Builder> listBuilder() { return new StructureListBuilder<>( TimeValuesType::builder, TimeValuesType::builder ); }
    public static StructureListBuilder<TimeValuesType,Builder> listBuilder( SequencedCollection<? extends JolieValue> from ) {
        return new StructureListBuilder<>( from, TimeValuesType::from, TimeValuesType::builder, TimeValuesType::builder );
    }
    
    public static TimeValuesType from( JolieValue j ) {
        return new TimeValuesType(
            ValueManager.fieldFrom( j.getFirstChild( "hour" ), c -> c.content() instanceof JolieInt content ? content.value() : null ),
            ValueManager.fieldFrom( j.getFirstChild( "minute" ), c -> c.content() instanceof JolieInt content ? content.value() : null ),
            ValueManager.fieldFrom( j.getFirstChild( "second" ), c -> c.content() instanceof JolieInt content ? content.value() : null )
        );
    }
    
    public static TimeValuesType fromValue( Value v ) throws TypeCheckingException {
        ValueManager.requireChildren( v, FIELD_KEYS );
        return new TimeValuesType(
            ValueManager.singleFieldFrom( v, "hour", JolieInt::fieldFromValue ),
            ValueManager.singleFieldFrom( v, "minute", JolieInt::fieldFromValue ),
            ValueManager.singleFieldFrom( v, "second", JolieInt::fieldFromValue )
        );
    }
    
    public static Value toValue( TimeValuesType t ) {
        final Value v = Value.create();
        
        v.getFirstChild( "hour" ).setValue( t.hour() );
        v.getFirstChild( "minute" ).setValue( t.minute() );
        v.getFirstChild( "second" ).setValue( t.second() );
        
        return v;
    }
    
    public static class Builder {
        
        private Integer hour;
        private Integer minute;
        private Integer second;
        
        private Builder() {}
        private Builder( JolieValue j ) {
            this.hour = ValueManager.fieldFrom( j.getFirstChild( "hour" ), c -> c.content() instanceof JolieInt content ? content.value() : null );
            this.minute = ValueManager.fieldFrom( j.getFirstChild( "minute" ), c -> c.content() instanceof JolieInt content ? content.value() : null );
            this.second = ValueManager.fieldFrom( j.getFirstChild( "second" ), c -> c.content() instanceof JolieInt content ? content.value() : null );
        }
        
        public Builder hour( Integer hour ) { this.hour = hour; return this; }
        public Builder minute( Integer minute ) { this.minute = minute; return this; }
        public Builder second( Integer second ) { this.second = second; return this; }
        
        public TimeValuesType build() {
            return new TimeValuesType( hour, minute, second );
        }
    }
}