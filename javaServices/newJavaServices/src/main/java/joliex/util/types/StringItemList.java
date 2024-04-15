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
 *     item[0,2147483647]: {@link String}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 * @see #construct()
 */
public final class StringItemList implements JolieValue {
    
    private static final Set<String> FIELD_KEYS = Set.of( "item" );
    
    private final List<String> item;
    
    public StringItemList( SequencedCollection<String> item ) {
        this.item = ValueManager.validated( item, 0, 2147483647 );
    }
    
    public List<String> item() { return item; }
    
    public JolieVoid content() { return new JolieVoid(); }
    public Map<String, List<JolieValue>> children() {
        return one.util.streamex.EntryStream.of(
            "item", item.parallelStream().map( JolieValue::create ).toList()
        ).filterValues( Objects::nonNull ).toImmutableMap();
    }
    
    public static Builder construct() { return new Builder(); }
    
    public static ListBuilder constructList() { return new ListBuilder(); }
    
    public static Builder constructFrom( JolieValue j ) { return new Builder( j ); }
    
    public static ListBuilder constructListFrom( SequencedCollection<? extends JolieValue> c ) { return new ListBuilder( c ); }
    
    public static StringItemList createFrom( JolieValue j ) {
        return new StringItemList(
            ValueManager.fieldFrom( j.getChildOrDefault( "item", List.of() ), c -> c.content() instanceof JolieString content ? content.value() : null )
        );
    }
    
    public static StringItemList fromValue( Value v ) throws TypeCheckingException {
        ValueManager.requireChildren( v, FIELD_KEYS );
        return new StringItemList(
            ValueManager.fieldFrom( v.children().getOrDefault( "item", ValueVector.create() ), JolieString::fieldFromValue )
        );
    }
    
    public static Value toValue( StringItemList t ) {
        final Value v = Value.create();
        
        t.item().forEach( c -> v.getNewChild( "item" ).setValue( c ) );
        
        return v;
    }
    
    public static class Builder {
        
        private SequencedCollection<String> item;
        
        private Builder() {}
        private Builder( JolieValue j ) {
            this.item = ValueManager.fieldFrom( j.getChildOrDefault( "item", List.of() ), c -> c.content() instanceof JolieString content ? content.value() : null );
        }
        
        public Builder item( SequencedCollection<String> item ) { this.item = item; return this; }
        
        public StringItemList build() {
            return new StringItemList( item );
        }
    }
    
    public static class ListBuilder extends AbstractListBuilder<ListBuilder, StringItemList> {
        
        private ListBuilder() {}
        private ListBuilder( SequencedCollection<? extends JolieValue> c ) { super( c, StringItemList::createFrom ); }
        
        protected ListBuilder self() { return this; }
        
        public ListBuilder add( Function<Builder, StringItemList> b ) { return add( b.apply( construct() ) ); }
        public ListBuilder set( int index, Function<Builder, StringItemList> b ) { return set( index, b.apply( construct() ) ); }
        public ListBuilder reconstruct( int index, Function<Builder, StringItemList> b ) { return replace( index, j -> b.apply( constructFrom( j ) ) ); }
    }
}