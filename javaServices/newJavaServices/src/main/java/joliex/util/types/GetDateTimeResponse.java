package joliex.util.types;

import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import jolie.runtime.ByteArray;
import jolie.runtime.typing.TypeCheckingException;

import java.util.ArrayList;
import java.util.Map;
import java.util.SequencedCollection;
import java.util.List;
import java.util.Optional;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.function.BinaryOperator;
import java.util.stream.Stream;
import java.util.stream.Collectors;

import joliex.java.embedding.*;
import joliex.java.embedding.JolieNative.*;
import joliex.java.embedding.util.*;

/**
 * this class is an {@link JolieValue} which can be described as follows:
 * 
 * <pre>
 * contentValue: {@link String}
 *     month: {@link Integer}
 *     hour: {@link Integer}
 *     year: {@link Integer}
 *     day: {@link Integer}
 *     second: {@link Integer}
 *     minute: {@link Integer}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 * @see #construct()
 */
public final class GetDateTimeResponse implements JolieValue {
    
    private static final Set<String> FIELD_KEYS = Set.of( "month", "hour", "year", "day", "second", "minute" );
    
    private final String contentValue;
    private final Integer month;
    private final Integer hour;
    private final Integer year;
    private final Integer day;
    private final Integer second;
    private final Integer minute;
    
    public GetDateTimeResponse( String contentValue, Integer month, Integer hour, Integer year, Integer day, Integer second, Integer minute ) {
        this.contentValue = ValueManager.validated( contentValue );
        this.month = ValueManager.validated( month );
        this.hour = ValueManager.validated( hour );
        this.year = ValueManager.validated( year );
        this.day = ValueManager.validated( day );
        this.second = ValueManager.validated( second );
        this.minute = ValueManager.validated( minute );
    }
    
    public String contentValue() { return contentValue; }
    public Integer month() { return month; }
    public Integer hour() { return hour; }
    public Integer year() { return year; }
    public Integer day() { return day; }
    public Integer second() { return second; }
    public Integer minute() { return minute; }
    
    public JolieString content() { return new JolieString( contentValue ); }
    public Map<String, List<JolieValue>> children() {
        return one.util.streamex.EntryStream.of(
            "month", List.of( JolieValue.create( month ) ),
            "hour", List.of( JolieValue.create( hour ) ),
            "year", List.of( JolieValue.create( year ) ),
            "day", List.of( JolieValue.create( day ) ),
            "second", List.of( JolieValue.create( second ) ),
            "minute", List.of( JolieValue.create( minute ) )
        ).filterValues( Objects::nonNull ).toImmutableMap();
    }
    
    public static Builder construct() { return new Builder(); }
    
    public static ListBuilder constructList() { return new ListBuilder(); }
    
    public static Builder constructFrom( JolieValue j ) { return new Builder( j ); }
    
    public static ListBuilder constructListFrom( SequencedCollection<? extends JolieValue> c ) { return new ListBuilder( c ); }
    
    public static GetDateTimeResponse createFrom( JolieValue j ) {
        return new GetDateTimeResponse(
            JolieString.createFrom( j ).value(),
            ValueManager.fieldFrom( j.getFirstChild( "month" ), c -> c.content() instanceof JolieInt content ? content.value() : null ),
            ValueManager.fieldFrom( j.getFirstChild( "hour" ), c -> c.content() instanceof JolieInt content ? content.value() : null ),
            ValueManager.fieldFrom( j.getFirstChild( "year" ), c -> c.content() instanceof JolieInt content ? content.value() : null ),
            ValueManager.fieldFrom( j.getFirstChild( "day" ), c -> c.content() instanceof JolieInt content ? content.value() : null ),
            ValueManager.fieldFrom( j.getFirstChild( "second" ), c -> c.content() instanceof JolieInt content ? content.value() : null ),
            ValueManager.fieldFrom( j.getFirstChild( "minute" ), c -> c.content() instanceof JolieInt content ? content.value() : null )
        );
    }
    
    public static GetDateTimeResponse fromValue( Value v ) throws TypeCheckingException {
        ValueManager.requireChildren( v, FIELD_KEYS );
        return new GetDateTimeResponse(
            JolieString.contentFromValue( v ),
            ValueManager.fieldFrom( v.firstChildOrDefault( "month", Function.identity(), null ), JolieInt::fieldFromValue ),
            ValueManager.fieldFrom( v.firstChildOrDefault( "hour", Function.identity(), null ), JolieInt::fieldFromValue ),
            ValueManager.fieldFrom( v.firstChildOrDefault( "year", Function.identity(), null ), JolieInt::fieldFromValue ),
            ValueManager.fieldFrom( v.firstChildOrDefault( "day", Function.identity(), null ), JolieInt::fieldFromValue ),
            ValueManager.fieldFrom( v.firstChildOrDefault( "second", Function.identity(), null ), JolieInt::fieldFromValue ),
            ValueManager.fieldFrom( v.firstChildOrDefault( "minute", Function.identity(), null ), JolieInt::fieldFromValue )
        );
    }
    
    public static Value toValue( GetDateTimeResponse t ) {
        final Value v = Value.create( t.contentValue() );
        
        v.getFirstChild( "month" ).setValue( t.month() );
        v.getFirstChild( "hour" ).setValue( t.hour() );
        v.getFirstChild( "year" ).setValue( t.year() );
        v.getFirstChild( "day" ).setValue( t.day() );
        v.getFirstChild( "second" ).setValue( t.second() );
        v.getFirstChild( "minute" ).setValue( t.minute() );
        
        return v;
    }
    
    public static class Builder {
        
        private String contentValue;
        private Integer month;
        private Integer hour;
        private Integer year;
        private Integer day;
        private Integer second;
        private Integer minute;
        
        private Builder() {}
        private Builder( JolieValue j ) {
            
            contentValue = j.content() instanceof JolieString content ? content.value() : null;
            this.month = ValueManager.fieldFrom( j.getFirstChild( "month" ), c -> c.content() instanceof JolieInt content ? content.value() : null );
            this.hour = ValueManager.fieldFrom( j.getFirstChild( "hour" ), c -> c.content() instanceof JolieInt content ? content.value() : null );
            this.year = ValueManager.fieldFrom( j.getFirstChild( "year" ), c -> c.content() instanceof JolieInt content ? content.value() : null );
            this.day = ValueManager.fieldFrom( j.getFirstChild( "day" ), c -> c.content() instanceof JolieInt content ? content.value() : null );
            this.second = ValueManager.fieldFrom( j.getFirstChild( "second" ), c -> c.content() instanceof JolieInt content ? content.value() : null );
            this.minute = ValueManager.fieldFrom( j.getFirstChild( "minute" ), c -> c.content() instanceof JolieInt content ? content.value() : null );
        }
        
        public Builder contentValue( String contentValue ) { this.contentValue = contentValue; return this; }
        public Builder month( Integer month ) { this.month = month; return this; }
        public Builder hour( Integer hour ) { this.hour = hour; return this; }
        public Builder year( Integer year ) { this.year = year; return this; }
        public Builder day( Integer day ) { this.day = day; return this; }
        public Builder second( Integer second ) { this.second = second; return this; }
        public Builder minute( Integer minute ) { this.minute = minute; return this; }
        
        public GetDateTimeResponse build() {
            return new GetDateTimeResponse( contentValue, month, hour, year, day, second, minute );
        }
    }
    
    public static class ListBuilder extends AbstractListBuilder<ListBuilder, GetDateTimeResponse> {
        
        private ListBuilder() {}
        private ListBuilder( SequencedCollection<? extends JolieValue> c ) { super( c, GetDateTimeResponse::createFrom ); }
        
        protected ListBuilder self() { return this; }
        
        public ListBuilder add( Function<Builder, GetDateTimeResponse> b ) { return add( b.apply( construct() ) ); }
        public ListBuilder set( int index, Function<Builder, GetDateTimeResponse> b ) { return set( index, b.apply( construct() ) ); }
        public ListBuilder reconstruct( int index, Function<Builder, GetDateTimeResponse> b ) { return replace( index, j -> b.apply( constructFrom( j ) ) ); }
    }
}