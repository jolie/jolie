package joliex.java.embedding;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ImmutableStructure<T extends BasicType<?>> implements StructureType {

    private final T root;
    private final Map<String, List<StructureType>> children;

    public ImmutableStructure( T root, Map<String, List<StructureType>> children ) throws TypeValidationException {
        if ( root == null )
            throw new TypeValidationException( "The root of this structure wasn't assigned a valid value." );

        this.root = root;
        this.children = children == null 
            ? Map.of() 
            : children.entrySet()
                .parallelStream()
                .collect( Collectors.toUnmodifiableMap(
                    Map.Entry::getKey, 
                    e -> Collections.unmodifiableList( e.getValue() )
                ) );
    }

    public T root() { return root; }
    public Map<String, List<StructureType>> children() { return children; }

    public boolean equals( Object obj ) {
        return obj != null && switch ( obj ) {

            case BasicType<?> other -> root().equals( other ) && children().isEmpty();

            case StructureType other -> root().equals( other.root() ) && children().equals( other.children() );

            default -> false;
        };
    }

    public String toString() {
        return (root() instanceof BasicType.JolieString ? "\"" + root().toString() + "\"" : root().toString())
            + children()
                .entrySet()
                .stream()
                .flatMap( e -> {
                    List<StructureType> ls = e.getValue();
                    return ls.size() == 1
                        ? Stream.of( e.getKey() + " = " + ls.getFirst().toString() )
                        : IntStream.range( 0, ls.size() ).mapToObj( i -> e.getKey() + "[" + i + "] = " + ls.get( i ).toString() );
                } )
                .reduce( ( s1, s2 ) -> s1 + "\n" + s2 )
                .map( s -> "\n" + s.indent( 4 ) )
                .orElse( "" );
    }

    protected final <S extends StructureType> List<S> child( String name, Class<S> structureType ) {
        return childOrDefault( name, List.of() )
            .stream()
            .map( structureType::cast )
            .filter( t -> t != null )
            .toList();
    }

    protected final List<BasicType<?>> childRoots( String name ) {
        return childOrDefault( name, List.of() ).stream().map( StructureType::root ).collect( Collectors.toUnmodifiableList() );
    }

    protected final <R extends BasicType<?>> List<R> childRoots( String name, Class<R> wrapperType ) {
        return childOrDefault( name, List.of() ).stream().map( StructureType::root ).map( wrapperType::cast ).toList();
    }

    protected final <U, R extends BasicType<U>> List<U> childValues( String name, Class<R> wrapperType ) {
        return childOrDefault( name, List.of() ).stream().map( StructureType::root ).map( r -> wrapperType.cast( r ).value() ).toList();
    }

    protected final <S extends StructureType> Optional<S> firstChild( String name, Class<S> structureType ) {
        return firstChild( name ).map( structureType::cast );
    }

    protected final Optional<BasicType<?>> firstChildRoot( String name ) {
        return firstChild( name ).map( StructureType::root );
    }

    protected final <R extends BasicType<?>> Optional<R> firstChildRoot( String name, Class<R> wrapperType ) {
        return firstChildRoot( name ).map( wrapperType::cast );
    }

    protected final <U, R extends BasicType<U>> Optional<U> firstChildValue( String name, Class<R> wrapperType ) {
        return firstChildRoot( name ).map( r -> wrapperType.cast( r ).value() );
    }
}
