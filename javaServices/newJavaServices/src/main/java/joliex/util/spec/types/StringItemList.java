package joliex.util.spec.types;

/**
 * this class is a {@link jolie.runtime.embedding.java.TypedStructure} which can be described as follows:
 * <pre>
 * item[0,2147483647]: {@link java.lang.String}
 * </pre>
 * 
 * @see jolie.runtime.embedding.java.JolieValue
 * @see jolie.runtime.embedding.java.JolieNative
 */
public final class StringItemList extends jolie.runtime.embedding.java.TypedStructure {
    
    private static final java.util.Set<java.lang.String> FIELD_KEYS = fieldKeys( StringItemList.class );
    
    @jolie.runtime.embedding.java.util.JolieName("item")
    private final java.util.List<java.lang.String> item;
    
    public StringItemList( java.util.SequencedCollection<java.lang.String> item ) {
        this.item = jolie.runtime.embedding.java.util.ValueManager.validated( "item", item, 0, 2147483647, t -> t );
    }
    
    public java.util.List<java.lang.String> item() { return item; }
    
    public jolie.runtime.embedding.java.JolieNative.JolieVoid content() { return new jolie.runtime.embedding.java.JolieNative.JolieVoid(); }
    
    public static StringItemList from( jolie.runtime.embedding.java.JolieValue j ) throws jolie.runtime.embedding.java.TypeValidationException {
        return new StringItemList(
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getChildOrDefault( "item", java.util.List.of() ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieString content ? content.value() : null )
        );
    }
    
    public static StringItemList fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
        jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
        return new StringItemList(
            jolie.runtime.embedding.java.util.ValueManager.vectorFieldFrom( v, "item", jolie.runtime.embedding.java.JolieNative.JolieString::fieldFromValue )
        );
    }
    
    public static jolie.runtime.Value toValue( StringItemList t ) {
        final jolie.runtime.Value v = jolie.runtime.Value.create();
        
        t.item().forEach( c -> v.getNewChild( "item" ).setValue( c ) );
        
        return v;
    }
}