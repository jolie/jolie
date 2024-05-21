package joliex.util.spec.types;

/**
 * this class is a {@link jolie.runtime.embedding.java.TypedStructure} which can be described as follows:
 * <pre>
 * piece[0,2147483647]: {@link java.lang.String}
 * delimiter: {@link java.lang.String}
 * </pre>
 * 
 * @see jolie.runtime.embedding.java.JolieValue
 * @see jolie.runtime.embedding.java.JolieNative
 */
public final class JoinRequest extends jolie.runtime.embedding.java.TypedStructure {
    
    private static final java.util.Set<java.lang.String> FIELD_KEYS = fieldKeys( JoinRequest.class );
    
    @jolie.runtime.embedding.java.util.JolieName("piece")
    private final java.util.List<java.lang.String> piece;
    @jolie.runtime.embedding.java.util.JolieName("delimiter")
    private final java.lang.String delimiter;
    
    public JoinRequest( java.util.SequencedCollection<java.lang.String> piece, java.lang.String delimiter ) {
        this.piece = jolie.runtime.embedding.java.util.ValueManager.validated( "piece", piece, 0, 2147483647, t -> t );
        this.delimiter = jolie.runtime.embedding.java.util.ValueManager.validated( "delimiter", delimiter );
    }
    
    public java.util.List<java.lang.String> piece() { return piece; }
    public java.lang.String delimiter() { return delimiter; }
    
    public jolie.runtime.embedding.java.JolieNative.JolieVoid content() { return new jolie.runtime.embedding.java.JolieNative.JolieVoid(); }
    
    public static JoinRequest from( jolie.runtime.embedding.java.JolieValue j ) throws jolie.runtime.embedding.java.TypeValidationException {
        return new JoinRequest(
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getChildOrDefault( "piece", java.util.List.of() ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieString content ? content.value() : null ),
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "delimiter" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieString content ? content.value() : null )
        );
    }
    
    public static JoinRequest fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
        jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
        return new JoinRequest(
            jolie.runtime.embedding.java.util.ValueManager.vectorFieldFrom( v, "piece", jolie.runtime.embedding.java.JolieNative.JolieString::fieldFromValue ),
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "delimiter", jolie.runtime.embedding.java.JolieNative.JolieString::fieldFromValue )
        );
    }
    
    public static jolie.runtime.Value toValue( JoinRequest t ) {
        final jolie.runtime.Value v = jolie.runtime.Value.create();
        
        t.piece().forEach( c -> v.getNewChild( "piece" ).setValue( c ) );
        v.getFirstChild( "delimiter" ).setValue( t.delimiter() );
        
        return v;
    }
}