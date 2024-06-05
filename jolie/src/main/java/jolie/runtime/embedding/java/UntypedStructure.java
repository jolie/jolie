package jolie.runtime.embedding.java;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import jolie.runtime.embedding.java.util.ValueManager;

public class UntypedStructure<T extends JolieNative<?>> implements JolieValue {

    private static final Map<String, List<JolieValue>> EMPTY_CHILDREN = Map.of();

    private final T content;
    private final Map<String, List<JolieValue>> children;

    public UntypedStructure( T content, Map<String, List<JolieValue>> children ) throws TypeValidationException {
        this.content = ValueManager.validated( "content", content );
        this.children = Objects.requireNonNullElse( children, EMPTY_CHILDREN );
    }

    public T content() { return content; }
    public Map<String, List<JolieValue>> children() { return children; }

    public boolean equals( Object obj ) { return obj != null && obj instanceof JolieValue j && content.equals( j.content() ) && children.equals( j.children() ); }
    public int hashCode() {
        if ( children.isEmpty() )
            return content.hashCode();
            
        int hash = 7;
        hash = 31 * hash + content.hashCode();
        hash = 31 * hash + children.hashCode();
        return hash;
    }
    public String toString() {
        return (content instanceof JolieNative.JolieString ? "\"" + content.toString() + "\"" : content.toString())
            + children.entrySet()
                .parallelStream()
                .flatMap( e -> {
                    final List<JolieValue> ls = e.getValue();
                    return ls.size() == 1
                        ? Stream.of( e.getKey() + " = " + ls.getFirst().toString() )
                        : IntStream.range( 0, ls.size() ).mapToObj( i -> e.getKey() + "[" + i + "] = " + ls.get( i ).toString() );
                } )
                .reduce( ( s1, s2 ) -> s1 + "\n" + s2 )
                .map( s -> "\n" + s.indent( 4 ) )
                .orElse( "" );
    }
}
