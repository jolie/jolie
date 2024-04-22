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
 * hour: {@link Integer}
 * minute: {@link Integer}
 * second: {@link Integer}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 * @see #construct()
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
    
    public static Builder construct() { return new Builder(); }
    public static ListBuilder constructList() { return new ListBuilder(); }
    public static Builder constructFrom( JolieValue j ) { return new Builder( j ); }
    public static ListBuilder constructListFrom( SequencedCollection<? extends JolieValue> c ) { return new ListBuilder( c ); }
    
    public static TimeValuesType createFrom( JolieValue j ) {
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
    
    public static class ListBuilder extends AbstractListBuilder<ListBuilder, TimeValuesType> {
        
        private ListBuilder() {}
        private ListBuilder( SequencedCollection<? extends JolieValue> c ) { super( c, TimeValuesType::createFrom ); }
        
        protected ListBuilder self() { return this; }
        
        public ListBuilder add( Function<Builder, TimeValuesType> b ) { return add( b.apply( construct() ) ); }
        public ListBuilder set( int index, Function<Builder, TimeValuesType> b ) { return set( index, b.apply( construct() ) ); }
        public ListBuilder reconstruct( int index, Function<Builder, TimeValuesType> b ) { return replace( index, j -> b.apply( constructFrom( j ) ) ); }
        
        public List<TimeValuesType> build() { return super.build(); }
    }
}