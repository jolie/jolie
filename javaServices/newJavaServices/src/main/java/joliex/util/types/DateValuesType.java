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
 * month: {@link Integer}
 * year: {@link Integer}
 * day: {@link Integer}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 * @see #construct()
 */
public final class DateValuesType extends TypedStructure {
    
    private static final Set<String> FIELD_KEYS = fieldKeys( DateValuesType.class );
    
    @JolieName("month")
    private final Integer month;
    @JolieName("year")
    private final Integer year;
    @JolieName("day")
    private final Integer day;
    
    public DateValuesType( Integer month, Integer year, Integer day ) {
        this.month = ValueManager.validated( "month", month );
        this.year = ValueManager.validated( "year", year );
        this.day = ValueManager.validated( "day", day );
    }
    
    public Integer month() { return month; }
    public Integer year() { return year; }
    public Integer day() { return day; }
    
    public JolieVoid content() { return new JolieVoid(); }
    
    public static Builder construct() { return new Builder(); }
    public static ListBuilder constructList() { return new ListBuilder(); }
    public static Builder constructFrom( JolieValue j ) { return new Builder( j ); }
    public static ListBuilder constructListFrom( SequencedCollection<? extends JolieValue> c ) { return new ListBuilder( c ); }
    
    public static DateValuesType createFrom( JolieValue j ) {
        return new DateValuesType(
            ValueManager.fieldFrom( j.getFirstChild( "month" ), c -> c.content() instanceof JolieInt content ? content.value() : null ),
            ValueManager.fieldFrom( j.getFirstChild( "year" ), c -> c.content() instanceof JolieInt content ? content.value() : null ),
            ValueManager.fieldFrom( j.getFirstChild( "day" ), c -> c.content() instanceof JolieInt content ? content.value() : null )
        );
    }
    
    public static DateValuesType fromValue( Value v ) throws TypeCheckingException {
        ValueManager.requireChildren( v, FIELD_KEYS );
        return new DateValuesType(
            ValueManager.singleFieldFrom( v, "month", JolieInt::fieldFromValue ),
            ValueManager.singleFieldFrom( v, "year", JolieInt::fieldFromValue ),
            ValueManager.singleFieldFrom( v, "day", JolieInt::fieldFromValue )
        );
    }
    
    public static Value toValue( DateValuesType t ) {
        final Value v = Value.create();
        
        v.getFirstChild( "month" ).setValue( t.month() );
        v.getFirstChild( "year" ).setValue( t.year() );
        v.getFirstChild( "day" ).setValue( t.day() );
        
        return v;
    }
    
    public static class Builder {
        
        private Integer month;
        private Integer year;
        private Integer day;
        
        private Builder() {}
        private Builder( JolieValue j ) {
            this.month = ValueManager.fieldFrom( j.getFirstChild( "month" ), c -> c.content() instanceof JolieInt content ? content.value() : null );
            this.year = ValueManager.fieldFrom( j.getFirstChild( "year" ), c -> c.content() instanceof JolieInt content ? content.value() : null );
            this.day = ValueManager.fieldFrom( j.getFirstChild( "day" ), c -> c.content() instanceof JolieInt content ? content.value() : null );
        }
        
        public Builder month( Integer month ) { this.month = month; return this; }
        public Builder year( Integer year ) { this.year = year; return this; }
        public Builder day( Integer day ) { this.day = day; return this; }
        
        public DateValuesType build() {
            return new DateValuesType( month, year, day );
        }
    }
    
    public static class ListBuilder extends AbstractListBuilder<ListBuilder, DateValuesType> {
        
        private ListBuilder() {}
        private ListBuilder( SequencedCollection<? extends JolieValue> c ) { super( c, DateValuesType::createFrom ); }
        
        protected ListBuilder self() { return this; }
        
        public ListBuilder add( Function<Builder, DateValuesType> b ) { return add( b.apply( construct() ) ); }
        public ListBuilder set( int index, Function<Builder, DateValuesType> b ) { return set( index, b.apply( construct() ) ); }
        public ListBuilder reconstruct( int index, Function<Builder, DateValuesType> b ) { return replace( index, j -> b.apply( constructFrom( j ) ) ); }
        
        public List<DateValuesType> build() { return super.build(); }
    }
}