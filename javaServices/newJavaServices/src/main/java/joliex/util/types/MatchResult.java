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
 * contentValue: {@link Integer}
     * group[0,2147483647]: {@link String}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 * @see #construct()
 */
public final class MatchResult extends TypedStructure {
    
    private static final Set<String> FIELD_KEYS = fieldKeys( MatchResult.class );
    
    private final Integer contentValue;
    @JolieName("group")
    private final List<String> group;
    
    public MatchResult( Integer contentValue, SequencedCollection<String> group ) {
        this.contentValue = ValueManager.validated( "contentValue", contentValue );
        this.group = ValueManager.validated( "group", group, 0, 2147483647 );
    }
    
    public Integer contentValue() { return contentValue; }
    public List<String> group() { return group; }
    
    public JolieInt content() { return new JolieInt( contentValue ); }
    
    public static Builder construct() { return new Builder(); }
    public static ListBuilder constructList() { return new ListBuilder(); }
    public static Builder constructFrom( JolieValue j ) { return new Builder( j ); }
    public static ListBuilder constructListFrom( SequencedCollection<? extends JolieValue> c ) { return new ListBuilder( c ); }
    
    public static Builder construct( Integer contentValue ) { return construct().contentValue( contentValue ); }
    
    public static MatchResult createFrom( JolieValue j ) {
        return new MatchResult(
            JolieInt.createFrom( j ).value(),
            ValueManager.fieldFrom( j.getChildOrDefault( "group", List.of() ), c -> c.content() instanceof JolieString content ? content.value() : null )
        );
    }
    
    public static MatchResult fromValue( Value v ) throws TypeCheckingException {
        ValueManager.requireChildren( v, FIELD_KEYS );
        return new MatchResult(
            JolieInt.contentFromValue( v ),
            ValueManager.fieldFrom( v.children().getOrDefault( "group", ValueVector.create() ), JolieString::fieldFromValue )
        );
    }
    
    public static Value toValue( MatchResult t ) {
        final Value v = Value.create( t.contentValue() );
        
        t.group().forEach( c -> v.getNewChild( "group" ).setValue( c ) );
        
        return v;
    }
    
    public static class Builder {
        
        private Integer contentValue;
        private SequencedCollection<String> group;
        
        private Builder() {}
        private Builder( JolieValue j ) {
            
            contentValue = j.content() instanceof JolieInt content ? content.value() : null;
            this.group = ValueManager.fieldFrom( j.getChildOrDefault( "group", List.of() ), c -> c.content() instanceof JolieString content ? content.value() : null );
        }
        
        public Builder contentValue( Integer contentValue ) { this.contentValue = contentValue; return this; }
        public Builder group( SequencedCollection<String> group ) { this.group = group; return this; }
        
        public MatchResult build() {
            return new MatchResult( contentValue, group );
        }
    }
    
    public static class ListBuilder extends AbstractListBuilder<ListBuilder, MatchResult> {
        
        private ListBuilder() {}
        private ListBuilder( SequencedCollection<? extends JolieValue> c ) { super( c, MatchResult::createFrom ); }
        
        protected ListBuilder self() { return this; }
        
        public ListBuilder add( Function<Builder, MatchResult> b ) { return add( b.apply( construct() ) ); }
        public ListBuilder set( int index, Function<Builder, MatchResult> b ) { return set( index, b.apply( construct() ) ); }
        public ListBuilder reconstruct( int index, Function<Builder, MatchResult> b ) { return replace( index, j -> b.apply( constructFrom( j ) ) ); }
    }
}