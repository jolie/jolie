package joliex.util.spec.types;

/**
 * this class is a {@link jolie.runtime.embedding.java.TypedStructure} which can be described as follows:
 * <pre>
 * 
 * contentValue: {@link java.lang.String}
     * regex: {@link java.lang.String}
     * replacement: {@link java.lang.String}
 * </pre>
 * 
 * @see jolie.runtime.embedding.java.JolieValue
 * @see jolie.runtime.embedding.java.JolieNative
 */
public final class ReplaceRequest extends jolie.runtime.embedding.java.TypedStructure {
    
    private static final java.util.Set<java.lang.String> FIELD_KEYS = fieldKeys( ReplaceRequest.class );
    
    private final java.lang.String contentValue;
    @jolie.runtime.embedding.java.util.JolieName("regex")
    private final java.lang.String regex;
    @jolie.runtime.embedding.java.util.JolieName("replacement")
    private final java.lang.String replacement;
    
    public ReplaceRequest( java.lang.String contentValue, java.lang.String regex, java.lang.String replacement ) {
        this.contentValue = jolie.runtime.embedding.java.util.ValueManager.validated( "contentValue", contentValue );
        this.regex = jolie.runtime.embedding.java.util.ValueManager.validated( "regex", regex );
        this.replacement = jolie.runtime.embedding.java.util.ValueManager.validated( "replacement", replacement );
    }
    
    public java.lang.String contentValue() { return contentValue; }
    public java.lang.String regex() { return regex; }
    public java.lang.String replacement() { return replacement; }
    
    public jolie.runtime.embedding.java.JolieNative.JolieString content() { return new jolie.runtime.embedding.java.JolieNative.JolieString( contentValue ); }
    
    public static ReplaceRequest from( jolie.runtime.embedding.java.JolieValue j ) throws jolie.runtime.embedding.java.TypeValidationException {
        return new ReplaceRequest(
            jolie.runtime.embedding.java.JolieNative.JolieString.from( j ).value(),
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "regex" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieString content ? content.value() : null ),
            jolie.runtime.embedding.java.util.ValueManager.fieldFrom( j.getFirstChild( "replacement" ), c -> c.content() instanceof jolie.runtime.embedding.java.JolieNative.JolieString content ? content.value() : null )
        );
    }
    
    public static ReplaceRequest fromValue( jolie.runtime.Value v ) throws jolie.runtime.typing.TypeCheckingException {
        jolie.runtime.embedding.java.util.ValueManager.requireChildren( v, FIELD_KEYS );
        return new ReplaceRequest(
            jolie.runtime.embedding.java.JolieNative.JolieString.contentFromValue( v ),
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "regex", jolie.runtime.embedding.java.JolieNative.JolieString::fieldFromValue ),
            jolie.runtime.embedding.java.util.ValueManager.singleFieldFrom( v, "replacement", jolie.runtime.embedding.java.JolieNative.JolieString::fieldFromValue )
        );
    }
    
    public static jolie.runtime.Value toValue( ReplaceRequest t ) {
        final jolie.runtime.Value v = jolie.runtime.Value.create( t.contentValue() );
        
        v.getFirstChild( "regex" ).setValue( t.regex() );
        v.getFirstChild( "replacement" ).setValue( t.replacement() );
        
        return v;
    }
}