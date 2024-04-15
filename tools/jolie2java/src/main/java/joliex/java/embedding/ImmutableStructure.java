package joliex.java.embedding;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import joliex.java.embedding.util.ValueManager;

public class ImmutableStructure<T extends JolieNative<?>> implements JolieValue {

    private final T content;
    private final Map<String, List<JolieValue>> children;

    public ImmutableStructure( T content, Map<String, List<JolieValue>> children ) throws TypeValidationException {
        this.content = ValueManager.validated( content );
        this.children = children == null ? Map.of() : children;
    }

    public T content() { return content; }
    public Map<String, List<JolieValue>> children() { return children; }

    public boolean equals( Object obj ) { return obj instanceof JolieValue j && content.equals( j.content() ) && children.equals( j.children() ); }
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + (content == null ? 0 : content.hashCode());
        hash = 31 * hash + (children == null ? 0 : children.hashCode());
        return hash;
    }
    public String toString() {
        return (content() instanceof JolieNative.JolieString ? "\"" + content().toString() + "\"" : content().toString())
            + children()
                .entrySet()
                .stream()
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
