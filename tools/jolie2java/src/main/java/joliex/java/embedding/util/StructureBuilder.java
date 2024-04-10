package joliex.java.embedding.util;

import java.util.SequencedCollection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.Function;

import joliex.java.embedding.JolieNative;
import joliex.java.embedding.JolieValue;
import joliex.java.embedding.TypeValidationException;

public abstract class StructureBuilder<T extends JolieNative<?>, B> {

    protected T content;
    protected final ConcurrentMap<String, List<JolieValue>> children;

    protected StructureBuilder( final T content, final Map<String, List<JolieValue>> children ) {
        this.content = content;
        this.children = new ConcurrentHashMap<>( children );
    }

    protected StructureBuilder() {
        this.content = null;
        this.children = new ConcurrentHashMap<>();
    }

    private static List<JolieValue> makeChild( SequencedCollection<? extends JolieValue> c ) {
        return c.parallelStream().map( JolieValue.class::cast ).toList();
    }

    private static <T,U> U applyNullable( T obj, Function<T,U> func ) {
        return Optional.ofNullable( obj ).map( func ).orElse( null );
    }

    protected final List<JolieValue> child( String name ) {
        return children.getOrDefault( name, List.of() );
    }

    protected final Optional<JolieValue> firstChild( String name ) {
        return Optional.ofNullable( children.getOrDefault( name, null ) ).map( child -> child.isEmpty() ? null : child.getFirst() );
    }

    protected abstract B self();

    protected B content( T content ) { this.content = content; return self(); }

    public final <U> U chain( Function<B,U> f ) { return f.apply( self() ); }

    /**
     * stores the mapping {@code name}: {@code child} in this builder, 
     * unless {@code child} is {@code null}, 
     * in which case this builder remains unchanged.
     * @param name the key of the mapping
     * @param child the value of the mapping
     * @return the {@code Object} which this method was called on
     * 
     * @see JolieValue
     */
    public final B put( String name, SequencedCollection<? extends JolieValue> child ) {
        if ( child != null )
            children.put( name, makeChild( child ) );

        return self();
    }

    protected final <V> B put( String name, SequencedCollection<V> childValues, Function<? super V, ? extends JolieValue> wrapper ) {
        return put( name, applyNullable( childValues, c -> c.parallelStream().map( wrapper ).toList() ) );
    }

    /**
     * equivalent to calling {@link #put(String,SequencedCollection)} with the arguments
     * {@code name} and {@code List.of( childEntry )}, 
     * unless {@code childEntry} is {@code null},
     * in which case this builder remains unchanged.
     * @param name the key of the mapping
     * @param childEntry the value of the first entry of the mapping
     * @return the {@code Object} which this method was called on
     * 
     * @see JolieValue
     * @see List#of
     */
    public final B putAs( String name, JolieValue childEntry ) {
        if ( childEntry != null )
            children.put( name, List.of( childEntry ) );

        return self();
    }

    protected final <V> B putAs( String name, V childValue, Function<? super V, ? extends JolieValue> wrapper ) {
        return putAs( name, applyNullable( childValue, wrapper ) );
    }

    public final B compute( String name, BiFunction<? super String, ? super List<JolieValue>, ? extends SequencedCollection<? extends JolieValue>> remappingFunction ) {
        children.compute( 
            name, 
            remappingFunction.andThen( ls -> ls == null ? null : makeChild( ls ) ) 
        );
        return self();
    }

    protected final <V> B compute( String name, BiFunction<? super String, ? super List<V>, ? extends SequencedCollection<? extends V>> remappingFunction, Function<? super JolieValue, V> unwrapper, Function<? super V, ? extends JolieValue> wrapper ) {
        return compute( name, (n,c) -> remappingFunction.apply( n, c.parallelStream().map( unwrapper ).toList() ).parallelStream().map( wrapper ).toList() );
    }

    public final B computeAs( String name, BiFunction<? super String, ? super JolieValue, ? extends JolieValue> remappingFunction ) {
        children.compute( 
            name,
            (n,c) ->
                remappingFunction.andThen( e -> e == null ? null : List.of( JolieValue.class.cast( e ) ) )
                    .apply(
                        n,
                        c == null || c.isEmpty() ? null : c.getFirst()
                    )
        );
        return self();
    }

    protected final <V> B computeAs( String name, BiFunction<? super String, ? super V, ? extends V> remappingFunction, Function<? super JolieValue, ? extends V> unwrapper, Function<? super V, ? extends JolieValue> wrapper ) {
        return computeAs( name, (n,e) -> Optional.ofNullable( remappingFunction.apply( n, Optional.ofNullable( e ).map( unwrapper ).orElse( null ) ) ).map( wrapper ).orElse( null ) );
    }

    public final B remove( String name ) { children.remove( name ); return self(); }

    protected final void validateChildren( Map<String, FieldManager<?>> fieldManagers ) throws TypeValidationException {
        fieldManagers.forEach( (name, manager) -> {
            try {
                manager.validateCardinality( children.getOrDefault( name, List.of() ) );
            } catch ( TypeValidationException e ) {
                throw new TypeValidationException( "validating the field \"" + name + "\" failed: " + e.getMessage() );
            }
        } );
    }
}
