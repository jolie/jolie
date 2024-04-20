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
 * item[0,2147483647]: {@link String}
 * </pre>
 * 
 * @see JolieValue
 * @see JolieNative
 */
public final class StringItemList extends TypedStructure {
    
    private static final Set<String> FIELD_KEYS = fieldKeys( StringItemList.class );
    
    @JolieName("item")
    private final List<String> item;
    
    public StringItemList( SequencedCollection<String> item ) {
        this.item = ValueManager.validated( "item", item, 0, 2147483647 );
    }
    
    public List<String> item() { return item; }
    
    public JolieVoid content() { return new JolieVoid(); }
    
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
}