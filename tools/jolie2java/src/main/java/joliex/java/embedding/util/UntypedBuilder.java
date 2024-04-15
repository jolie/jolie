package joliex.java.embedding.util;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SequencedCollection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import joliex.java.embedding.JolieValue;

public abstract class UntypedBuilder<B> {

    protected final Map<String, List<JolieValue>> children;

    protected UntypedBuilder( final Map<String, List<JolieValue>> children ) { this.children = new ConcurrentHashMap<>( children ); }
    protected UntypedBuilder() { this( new ConcurrentHashMap<>() ); }

    protected abstract B self();

    public final B put( String name, SequencedCollection<? extends JolieValue> child ) {
        if ( child != null )
            children.put( name, makeChild( child ) );

        return self();
    }

    public final B putAs( String name, JolieValue childEntry ) {
        if ( childEntry != null )
            children.put( name, List.of( childEntry ) );

        return self();
    }

    public final B put( String name, Function<JolieValue.ListBuilder, List<JolieValue>> b ) { return put( name, b.apply( JolieValue.constructList() ) ); }

    public final B putAs( String name, Function<JolieValue.Builder, JolieValue> b ) { return putAs( name, b.apply( JolieValue.construct() ) ); }

    public final B compute( String name, BiFunction<? super String, ? super List<JolieValue>, ? extends SequencedCollection<? extends JolieValue>> remappingFunction ) {
        children.compute( name, remappingFunction.andThen( ls -> ls == null ? null : makeChild( ls ) ) );
        return self();
    }

    public final B computeAs( String name, BiFunction<? super String, ? super JolieValue, ? extends JolieValue> remappingFunction ) {
        children.compute( name, (n,c) -> remappingFunction
            .andThen( e -> e == null ? null : List.<JolieValue>of( e ) )
            .apply( n, c == null || c.isEmpty() ? null : c.getFirst() )
        );
        return self();
    }

    public final B remove( String name ) { children.remove( name ); return self(); }

    private static List<JolieValue> makeChild( SequencedCollection<? extends JolieValue> c ) {
        return c.parallelStream()
            .filter( Objects::nonNull )
            .map( JolieValue.class::cast )
            .toList();
    }
}
