package joliex.util.spec.types;

/**
 * this class is a {@link jolie.runtime.embedding.java.TypedStructure} which can be described as follows:
 * <pre>
 * result[0,2147483647]: {@link java.lang.String}
 * </pre>
 * 
 * @see jolie.runtime.embedding.java.JolieValue
 * @see jolie.runtime.embedding.java.JolieNative
 */
public final class SplitResult extends jolie.runtime.embedding.java.TypedStructure {
    
    private static final java.util.Set<java.lang.String> FIELD_KEYS = fieldKeys( SplitResult.class );
    
    @jolie.runtime.embedding.java.util.JolieName("result")
    private final java.util.List<java.lang.String> result;
    
    public SplitResult( java.util.SequencedCollection<java.lang.String> result ) {
        this.result = jolie.runtime.embedding.java.util.ValueManager.validated( "result", result, 0, 2147483647, t -> t );
    }
    
    public java.util.List<java.lang.String> result() { return result; }
    
    public jolie.runtime.embedding.java.JolieNative.JolieVoid content() { return new jolie.runtime.embedding.java.JolieNative.JolieVoid(); }
    
    public static SplitResult from( jolie.runtime.embedding.java.JolieValue j ) throws jolie.runtime.embedding.java.TypeValidationException {
        return new SplitResult(
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getChildOrDefault( "result", java.util.List.of() ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieString content ? content.value() : null )
        );
    }
    
    public static SplitResult fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
        jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
        return new SplitResult(
            jolie.runtime.embedding.java.util.ValueManager.vectorFieldFrom( v, "result", jolie.runtime.embedding.java.JolieNative.JolieString::fieldFromValue )
        );
    }
    
    public static jolie.runtime.Value toValue( SplitResult t ) {
        final jolie.runtime.Value v = jolie.runtime.Value.create();
        
        t.result().forEach( c -> v.getNewChild( "result" ).setValue( c ) );
        
        return v;
    }
}