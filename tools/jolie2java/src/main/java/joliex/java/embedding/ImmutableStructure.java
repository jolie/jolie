package joliex.java.embedding;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ImmutableStructure<T extends JolieNative<?>> implements JolieValue {

    private final T content;
    private final Map<String, List<JolieValue>> children;

    public ImmutableStructure( T content, Map<String, List<JolieValue>> children ) throws TypeValidationException {
        if ( content == null )
            throw new TypeValidationException( "The content of this structure wasn't assigned a valid value." );

        this.content = content;
        this.children = children == null 
            ? Map.of() 
            : children.entrySet()
                .parallelStream()
                .collect( Collectors.toUnmodifiableMap(
                    Map.Entry::getKey, 
                    e -> Collections.unmodifiableList( e.getValue() )
                ) );
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

    protected final <S extends JolieValue> List<S> getChild( String name, Class<S> structureType ) {
        return getChildOrDefault( name, List.of() )
            .stream()
            .map( structureType::cast )
            .filter( t -> t != null )
            .toList();
    }

    protected final List<JolieNative<?>> getChildContents( String name ) {
        return getChildOrDefault( name, List.of() ).stream().map( JolieValue::content ).collect( Collectors.toUnmodifiableList() );
    }

    protected final <R extends JolieNative<?>> List<R> getChildContents( String name, Class<R> wrapperType ) {
        return getChildOrDefault( name, List.of() ).stream().map( JolieValue::content ).map( wrapperType::cast ).toList();
    }

    protected final <U, R extends JolieNative<U>> List<U> getChildValues( String name, Class<R> wrapperType ) {
        return getChildOrDefault( name, List.of() ).stream().map( JolieValue::content ).map( r -> wrapperType.cast( r ).value() ).toList();
    }

    protected final <S extends JolieValue> Optional<S> getFirstChild( String name, Class<S> structureType ) {
        return getFirstChild( name ).map( structureType::cast );
    }

    protected final Optional<JolieNative<?>> getFirstChildContent( String name ) {
        return getFirstChild( name ).map( JolieValue::content );
    }

    protected final <R extends JolieNative<?>> Optional<R> getFirstChildContent( String name, Class<R> wrapperType ) {
        return getFirstChildContent( name ).map( wrapperType::cast );
    }

    protected final <U, R extends JolieNative<U>> Optional<U> getFirstChildValue( String name, Class<R> wrapperType ) {
        return getFirstChildContent( name ).map( r -> wrapperType.cast( r ).value() );
    }
}
