package jolie.runtime.embedding.java.util;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedCollection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import jolie.runtime.ByteArray;
import jolie.runtime.embedding.java.JolieNative;
import jolie.runtime.embedding.java.JolieValue;

public abstract class UntypedBuilder<B> {

    protected final ConcurrentHashMap<String, List<JolieValue>> children;

    protected UntypedBuilder() { this.children = new ConcurrentHashMap<>(); }
    protected UntypedBuilder( final Map<String, List<JolieValue>> children ) { this.children = new ConcurrentHashMap<>( children ); }

    protected abstract B self();

    /**
	 * Convenience method meant to make certain operations easier.
	 * 
	 * @param <T> the type returned by the given function
	 * @param f the function to apply to this Builder
	 * @return the result of applying the given function {@code f} to this Builder
	 * @apiNote The following is an example of using this method to make populating the builder easier:
	 * <pre>
	 * JolieValue.builder()
	 * 		.chain( b -> {
	 * 			for ( int i = 0; i < 10; i++ )
	 * 				b.putAs( "field" + i, i );
	 * 			return b;
	 * 		} )
	 * 		.build();
	 * </pre>
	 **/
    public final <T> T chain( Function<B,T> f ) { return f.apply( self() ); }

    /**
     * Maps the specified name to the specified nodes.
     * Does nothing if the specified nodes is {@code null}.
     * 
     * @param name name of the specified nodes
     * @param child the nodes with the specified name
     * @return this builder
     */
    public final B put( String name, SequencedCollection<? extends JolieValue> child ) {
        if ( child != null )
            children.put( name, makeChild( child ) );

        return self();
    }

    /**
     * Maps the specified name to the specified node.
     * Does nothing if the specified node is {@code null}.
     * 
     * @param name name of the specified node
     * @param childEntry the node with the specified name
     * @return this builder
     * 
     * @see #put(String,SequencedCollection)
     * 
     * @implNote if {@code childEntry} isn't {@code null} then this equivalent to {@code put( name, List.of( childEntry ) )}
     */
    public final B putAs( String name, JolieValue childEntry ) {
        if ( childEntry != null )
            children.put( name, List.of( childEntry ) );

        return self();
    }

    /**
     * Maps the specified name to the specified node.
     * 
     * @param name name of the specified node
     * @param contentEntry the node with the specified name
     * @return this builder
     * 
     * @see #putAs(String,JolieValue)
     * 
     * @implSpec implemented as {@code putAs( name, JolieValue.of( contentEntry ) )}
     */
    public final B putAs( String name, JolieNative<?> contentEntry ) { return putAs( name, JolieValue.of( contentEntry ) ); }

    /**
     * Maps the specified name to the specified node.
     * 
     * @param name name of the specified node
     * @param valueEntry the node with the specified name
     * @return this builder
     * 
     * @see #putAs(String,JolieNative)
     * 
     * @implSpec implemented as {@code putAs( name, JolieNative.of( valueEntry ) )}
     */
    public final B putAs( String name, Boolean valueEntry ) { return putAs( name, JolieNative.of( valueEntry ) ); }

    /**
     * Maps the specified name to the specified node.
     * 
     * @param name name of the specified node
     * @param valueEntry the node with the specified name
     * @return this builder
     * 
     * @see #putAs(String,JolieNative)
     * 
     * @implSpec implemented as {@code putAs( name, JolieNative.of( valueEntry ) )}
     */
    public final B putAs( String name, Integer valueEntry ) { return putAs( name, JolieNative.of( valueEntry ) ); }

    /**
     * Maps the specified name to the specified node.
     * 
     * @param name name of the specified node
     * @param valueEntry the node with the specified name
     * @return this builder
     * 
     * @see #putAs(String,JolieNative)
     * 
     * @implSpec implemented as {@code putAs( name, JolieNative.of( valueEntry ) )}
     */
    public final B putAs( String name, Long valueEntry ) { return putAs( name, JolieNative.of( valueEntry ) ); }

    /**
     * Maps the specified name to the specified node.
     * 
     * @param name name of the specified node
     * @param valueEntry the node with the specified name
     * @return this builder
     * 
     * @see #putAs(String,JolieNative)
     * 
     * @implSpec implemented as {@code putAs( name, JolieNative.of( valueEntry ) )}
     */
    public final B putAs( String name, Double valueEntry ) { return putAs( name, JolieNative.of( valueEntry ) ); }

    /**
     * Maps the specified name to the specified node.
     * 
     * @param name name of the specified node
     * @param valueEntry the node with the specified name
     * @return this builder
     * 
     * @see #putAs(String,JolieNative)
     * 
     * @implSpec implemented as {@code putAs( name, JolieNative.of( valueEntry ) )}
     */
    public final B putAs( String name, String valueEntry ) { return putAs( name, JolieNative.of( valueEntry ) ); }

    /**
     * Maps the specified name to the specified node.
     * 
     * @param name name of the specified node
     * @param valueEntry the node with the specified name
     * @return this builder
     * 
     * @see #putAs(String,JolieNative)
     * 
     * @implSpec implemented as {@code putAs( name, JolieNative.of( valueEntry ) )}
     */
    public final B putAs( String name, ByteArray valueEntry ) { return putAs( name, JolieNative.of( valueEntry ) ); }

    /**
     * Returns a new {@link JolieValue.NestedListBuilder} that can be used to build the nodes that should be mapped to the specified name.
     * 
     * @param name name of the nodes being built
     * @return a new {@link JolieValue.NestedListBuilder} that can be used to build the nodes that should be mapped to the specified name
     */
    public final JolieValue.NestedListBuilder<B> listBuilder( String name ) { return JolieValue.nestedListBuilder( ls -> put( name, ls ) ); }

    /**
     * Returns a new {@link JolieValue.NestedListBuilder} that can be used to build the nodes that should be mapped to the specified name.
     * The returned builder will have been initialized with the specified nodes when returned.
     * 
     * @param name name of the nodes being built
     * @param from the initial entries of the returned builder
     * @return a new {@link JolieValue.NestedListBuilder} that can be used to build the nodes that should be mapped to the specified name
     */
    public final JolieValue.NestedListBuilder<B> listBuilder( String name, SequencedCollection<? extends JolieValue> from ) { return JolieValue.nestedListBuilder( from, ls -> put( name, ls ) ); }

    /**
     * Returns a new {@link JolieValue.NestedBuilder} that can be used to build the node that should be mapped to the specified name.
     * 
     * @param name name of the node being built
     * @return a new {@link JolieValue.NestedBuilder} that can be used to build the node that should be mapped to the specified name
     */
    public final JolieValue.NestedBuilder<B> builder( String name ) { return JolieValue.nestedBuilder( e -> putAs( name, e ) ); }

    /**
     * Returns a new {@link JolieValue.NestedBuilder} that can be used to build the node that should be mapped to the specified name.
     * 
     * @param name name of the node being built
     * @param content the root content of the node being built
     * @return a new {@link JolieValue.NestedBuilder} that can be used to build the node that should be mapped to the specified name
     * 
     * @see #builder(String)
     * 
     * @implSpec implemented as {@code builder( name ).content( content )}
     */
    public final JolieValue.NestedBuilder<B> builder( String name, JolieNative<?> content ) { return builder( name ).content( content ); }

    /**
     * Returns a new {@link JolieValue.NestedBuilder} that can be used to build the node that should be mapped to the specified name.
     * 
     * @param name name of the node being built
     * @param contentValue the root content value of the node being built
     * @return a new {@link JolieValue.NestedBuilder} that can be used to build the node that should be mapped to the specified name
     * 
     * @see #builder(String,JolieNative)
     * 
     * @implSpec implemented as {@code builder( name, JolieNative.of( content ) )}
     */
    public final JolieValue.NestedBuilder<B> builder( String name, Boolean contentValue ) { return builder( name, JolieNative.of( contentValue ) ); }

    /**
     * Returns a new {@link JolieValue.NestedBuilder} that can be used to build the node that should be mapped to the specified name.
     * 
     * @param name name of the node being built
     * @param contentValue the root content value of the node being built
     * @return a new {@link JolieValue.NestedBuilder} that can be used to build the node that should be mapped to the specified name
     * 
     * @see #builder(String,JolieNative)
     * 
     * @implSpec implemented as {@code builder( name, JolieNative.of( content ) )}
     */
    public final JolieValue.NestedBuilder<B> builder( String name, Integer contentValue ) { return builder( name, JolieNative.of( contentValue ) ); }

    /**
     * Returns a new {@link JolieValue.NestedBuilder} that can be used to build the node that should be mapped to the specified name.
     * 
     * @param name name of the node being built
     * @param contentValue the root content value of the node being built
     * @return a new {@link JolieValue.NestedBuilder} that can be used to build the node that should be mapped to the specified name
     * 
     * @see #builder(String,JolieNative)
     * 
     * @implSpec implemented as {@code builder( name, JolieNative.of( content ) )}
     */
    public final JolieValue.NestedBuilder<B> builder( String name, Long contentValue ) { return builder( name, JolieNative.of( contentValue ) ); }

    /**
     * Returns a new {@link JolieValue.NestedBuilder} that can be used to build the node that should be mapped to the specified name.
     * 
     * @param name name of the node being built
     * @param contentValue the root content value of the node being built
     * @return a new {@link JolieValue.NestedBuilder} that can be used to build the node that should be mapped to the specified name
     * 
     * @see #builder(String,JolieNative)
     * 
     * @implSpec implemented as {@code builder( name, JolieNative.of( content ) )}
     */
    public final JolieValue.NestedBuilder<B> builder( String name, Double contentValue ) { return builder( name, JolieNative.of( contentValue ) ); }

    /**
     * Returns a new {@link JolieValue.NestedBuilder} that can be used to build the node that should be mapped to the specified name.
     * 
     * @param name name of the node being built
     * @param contentValue the root content value of the node being built
     * @return a new {@link JolieValue.NestedBuilder} that can be used to build the node that should be mapped to the specified name
     * 
     * @see #builder(String,JolieNative)
     * 
     * @implSpec implemented as {@code builder( name, JolieNative.of( content ) )}
     */
    public final JolieValue.NestedBuilder<B> builder( String name, String contentValue ) { return builder( name, JolieNative.of( contentValue ) ); }

    /**
     * Returns a new {@link JolieValue.NestedBuilder} that can be used to build the node that should be mapped to the specified name.
     * 
     * @param name name of the node being built
     * @param contentValue the root content value of the node being built
     * @return a new {@link JolieValue.NestedBuilder} that can be used to build the node that should be mapped to the specified name
     * 
     * @see #builder(String,JolieNative)
     * 
     * @implSpec implemented as {@code builder( name, JolieNative.of( content ) )}
     */
    public final JolieValue.NestedBuilder<B> builder( String name, ByteArray contentValue ) { return builder( name, JolieNative.of( contentValue ) ); }

    /**
     * Returns a new {@link JolieValue.NestedBuilder} that can be used to build the node that should be mapped to the specified name.
     * The returned builder will be initialized to the specified node.
     * 
     * @param name name of the node being built
     * @param from the node that should serve as the starting point for the returned builder
     * @return a new {@link JolieValue.NestedBuilder} that can be used to build the node that should be mapped to the specified name
     * 
     * @see #builder(String,JolieNative)
     * 
     * @implNote calling {@code builder( name, from ).done()} will result in the node with the specified name being equivalent to {@code from}
     */
    public final JolieValue.NestedBuilder<B> builder( String name, JolieValue from ) { return JolieValue.nestedBuilder( from, e -> putAs( name, e ) ); }

    /**
     * Returns a {@link JolieValue.NestedListBuilder} that can be used to build the nodes that should be mapped to the specified name.
     * The returned builder will have been initialized with the nodes that currently have the specified name in this builder.
     * 
     * @param name name of the nodes being rebuilt
     * @return a {@link JolieValue.NestedListBuilder} that can be used to build the nodes that should be mapped to the specified name
     * 
     * @see #listBuilder(String,SequencedCollection)
     * 
     * @implNote if {@code from} is the non-empty collection of nodes currently mapped to the specified name then this is equivalent to {@code listBuilder( name, from )}
     * otherwise it is equivalent to {@code listBuilder( name )}
     */
    public final JolieValue.NestedListBuilder<B> listRebuilder( String name ) { return listBuilder( name, children.getOrDefault( name, List.of() ) ); }

    /**
     * Returns a {@link JolieValue.NestedBuilder} that can be used to build the node that should be mapped to the specified name.
     * The returned builder will have been initialized with the first node that currently has the specified name in this builder.
     * 
     * @param name name of the node being rebuilt
     * @return a {@link JolieValue.NestedBuilder} that can be used to build the node that should be mapped to the specified name
     * 
     * @see #builder(String,JolieValue)
     * 
     * @implNote if {@code from} is the non-empty collection of nodes currently mapped to the specified name then this is equivalent to {@code builder( name, from.getFirst() )}
     * otherwise it is equivalent to {@code builder( name )}
     */
    public final JolieValue.NestedBuilder<B> rebuilder( String name ) { 
        return Optional.ofNullable( children.getOrDefault( name, null ) )
            .map( c -> c.isEmpty() ? null : c.getFirst() )
            .map( e -> builder( name, e ) )
            .orElse( builder( name ) );
    }

    /**
     * Computes a mapping for the nodes with the specified name.
     * 
     * @param name name of the nodes being computed
     * @param remappingFunction the function to compute the nodes.
     * @return this builder
     * @implNote {@code null} is provided as the nodes when no nodes with the specified names exist
     */
    public final B compute( String name, BiFunction<? super String, ? super List<JolieValue>, ? extends SequencedCollection<? extends JolieValue>> remappingFunction ) {
        children.compute( name, remappingFunction.andThen( ls -> ls == null ? null : makeChild( ls ) ) );
        return self();
    }

    /**
     * Computes a mapping for the first node with the specified name.
     * 
     * @param name name of the node being computed
     * @param remappingFunction the function to compute the node.
     * @return this builder
     * @implNote {@code null} is provided as the node when no nodes with the specified names exist, and
     * if multiple nodes with the specified name existed previously the first one will be provided and only the computed node will have the specified name afterwards
     */
    public final B computeAs( String name, BiFunction<? super String, ? super JolieValue, ? extends JolieValue> remappingFunction ) {
        children.compute( name, (n,c) -> remappingFunction
            .andThen( e -> e == null ? null : List.<JolieValue>of( e ) )
            .apply( n, c == null || c.isEmpty() ? null : c.getFirst() )
        );
        return self();
    }

    /**
     * Removes every node with the specified name in this builder.
     * 
     * @param name name of the nodes being removed
     * @return this builder
     */
    public final B remove( String name ) { children.remove( name ); return self(); }

    private static List<JolieValue> makeChild( SequencedCollection<? extends JolieValue> c ) {
        return c.parallelStream()
            .filter( Objects::nonNull )
            .map( JolieValue.class::cast )
            .toList();
    }
}
