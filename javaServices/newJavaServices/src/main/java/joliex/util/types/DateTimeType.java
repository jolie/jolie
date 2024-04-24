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
 * month: {@link Integer}
 * hour: {@link Integer}
 * year: {@link Integer}
 * day: {@link Integer}
 * minute: {@link Integer}
 * second: {@link Integer}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 * @see #builder()
 */
public final class DateTimeType extends TypedStructure {
    
    private static final Set<String> FIELD_KEYS = fieldKeys( DateTimeType.class );
    
    @JolieName("month")
    private final Integer month;
    @JolieName("hour")
    private final Integer hour;
    @JolieName("year")
    private final Integer year;
    @JolieName("day")
    private final Integer day;
    @JolieName("minute")
    private final Integer minute;
    @JolieName("second")
    private final Integer second;
    
    public DateTimeType( Integer month, Integer hour, Integer year, Integer day, Integer minute, Integer second ) {
        this.month = ValueManager.validated( "month", month );
        this.hour = ValueManager.validated( "hour", hour );
        this.year = ValueManager.validated( "year", year );
        this.day = ValueManager.validated( "day", day );
        this.minute = ValueManager.validated( "minute", minute );
        this.second = ValueManager.validated( "second", second );
    }
    
    public Integer month() { return month; }
    public Integer hour() { return hour; }
    public Integer year() { return year; }
    public Integer day() { return day; }
    public Integer minute() { return minute; }
    public Integer second() { return second; }
    
    public JolieVoid content() { return new JolieVoid(); }
    
    public static Builder builder() { return new Builder(); }
    public static Builder builder( JolieValue from ) { return new Builder( from ); }
    public static StructureListBuilder<DateTimeType,Builder> listBuilder() { return new StructureListBuilder<>( DateTimeType::builder, DateTimeType::builder ); }
    public static StructureListBuilder<DateTimeType,Builder> listBuilder( SequencedCollection<? extends JolieValue> from ) {
        return new StructureListBuilder<>( from, DateTimeType::from, DateTimeType::builder, DateTimeType::builder );
    }
    
    public static DateTimeType from( JolieValue j ) {
        return new DateTimeType(
            ValueManager.fieldFrom( j.getFirstChild( "month" ), c -> c.content() instanceof JolieInt content ? content.value() : null ),
            ValueManager.fieldFrom( j.getFirstChild( "hour" ), c -> c.content() instanceof JolieInt content ? content.value() : null ),
            ValueManager.fieldFrom( j.getFirstChild( "year" ), c -> c.content() instanceof JolieInt content ? content.value() : null ),
            ValueManager.fieldFrom( j.getFirstChild( "day" ), c -> c.content() instanceof JolieInt content ? content.value() : null ),
            ValueManager.fieldFrom( j.getFirstChild( "minute" ), c -> c.content() instanceof JolieInt content ? content.value() : null ),
            ValueManager.fieldFrom( j.getFirstChild( "second" ), c -> c.content() instanceof JolieInt content ? content.value() : null )
        );
    }
    
    public static DateTimeType fromValue( Value v ) throws TypeCheckingException {
        ValueManager.requireChildren( v, FIELD_KEYS );
        return new DateTimeType(
            ValueManager.singleFieldFrom( v, "month", JolieInt::fieldFromValue ),
            ValueManager.singleFieldFrom( v, "hour", JolieInt::fieldFromValue ),
            ValueManager.singleFieldFrom( v, "year", JolieInt::fieldFromValue ),
            ValueManager.singleFieldFrom( v, "day", JolieInt::fieldFromValue ),
            ValueManager.singleFieldFrom( v, "minute", JolieInt::fieldFromValue ),
            ValueManager.singleFieldFrom( v, "second", JolieInt::fieldFromValue )
        );
    }
    
    public static Value toValue( DateTimeType t ) {
        final Value v = Value.create();
        
        v.getFirstChild( "month" ).setValue( t.month() );
        v.getFirstChild( "hour" ).setValue( t.hour() );
        v.getFirstChild( "year" ).setValue( t.year() );
        v.getFirstChild( "day" ).setValue( t.day() );
        v.getFirstChild( "minute" ).setValue( t.minute() );
        v.getFirstChild( "second" ).setValue( t.second() );
        
        return v;
    }
    
    public static class Builder {
        
        private Integer month;
        private Integer hour;
        private Integer year;
        private Integer day;
        private Integer minute;
        private Integer second;
        
        private Builder() {}
        private Builder( JolieValue j ) {
            this.month = ValueManager.fieldFrom( j.getFirstChild( "month" ), c -> c.content() instanceof JolieInt content ? content.value() : null );
            this.hour = ValueManager.fieldFrom( j.getFirstChild( "hour" ), c -> c.content() instanceof JolieInt content ? content.value() : null );
            this.year = ValueManager.fieldFrom( j.getFirstChild( "year" ), c -> c.content() instanceof JolieInt content ? content.value() : null );
            this.day = ValueManager.fieldFrom( j.getFirstChild( "day" ), c -> c.content() instanceof JolieInt content ? content.value() : null );
            this.minute = ValueManager.fieldFrom( j.getFirstChild( "minute" ), c -> c.content() instanceof JolieInt content ? content.value() : null );
            this.second = ValueManager.fieldFrom( j.getFirstChild( "second" ), c -> c.content() instanceof JolieInt content ? content.value() : null );
        }
        
        public Builder month( Integer month ) { this.month = month; return this; }
        public Builder hour( Integer hour ) { this.hour = hour; return this; }
        public Builder year( Integer year ) { this.year = year; return this; }
        public Builder day( Integer day ) { this.day = day; return this; }
        public Builder minute( Integer minute ) { this.minute = minute; return this; }
        public Builder second( Integer second ) { this.second = second; return this; }
        
        public DateTimeType build() {
            return new DateTimeType( month, hour, year, day, minute, second );
        }
    }
}