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

    protected final Map<String, List<JolieValue>> children;

    protected UntypedBuilder() { this.children = new ConcurrentHashMap<>(); }
    protected UntypedBuilder( final Map<String, List<JolieValue>> children ) { this.children = new ConcurrentHashMap<>( children ); }

    protected abstract B self();

    public final <T> T chain( Function<B,T> f ) { return f.apply( self() ); }

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
    public final B putAs( String name, JolieNative<?> contentEntry ) { return putAs( name, JolieValue.create( contentEntry ) ); }
    public final B putAs( String name, Boolean valueEntry ) { return putAs( name, JolieNative.create( valueEntry ) ); }
    public final B putAs( String name, Integer valueEntry ) { return putAs( name, JolieNative.create( valueEntry ) ); }
    public final B putAs( String name, Long valueEntry ) { return putAs( name, JolieNative.create( valueEntry ) ); }
    public final B putAs( String name, Double valueEntry ) { return putAs( name, JolieNative.create( valueEntry ) ); }
    public final B putAs( String name, String valueEntry ) { return putAs( name, JolieNative.create( valueEntry ) ); }
    public final B putAs( String name, ByteArray valueEntry ) { return putAs( name, JolieNative.create( valueEntry ) ); }

    public final JolieValue.NestedListBuilder<B> construct( String name ) { return JolieValue.constructNestedList( ls -> put( name, ls ) ); }

    public final JolieValue.NestedBuilder<B> constructAs( String name ) { return JolieValue.constructNested( e -> putAs( name, e ) ); }
    public final JolieValue.NestedBuilder<B> constructAs( String name, JolieNative<?> content ) { return constructAs( name ).content( content ); }
    public final JolieValue.NestedBuilder<B> constructAs( String name, Boolean contentValue ) { return constructAs( name, JolieNative.create( contentValue ) ); }
    public final JolieValue.NestedBuilder<B> constructAs( String name, Integer contentValue ) { return constructAs( name, JolieNative.create( contentValue ) ); }
    public final JolieValue.NestedBuilder<B> constructAs( String name, Long contentValue ) { return constructAs( name, JolieNative.create( contentValue ) ); }
    public final JolieValue.NestedBuilder<B> constructAs( String name, Double contentValue ) { return constructAs( name, JolieNative.create( contentValue ) ); }
    public final JolieValue.NestedBuilder<B> constructAs( String name, String contentValue ) { return constructAs( name, JolieNative.create( contentValue ) ); }
    public final JolieValue.NestedBuilder<B> constructAs( String name, ByteArray contentValue ) { return constructAs( name, JolieNative.create( contentValue ) ); }

    public final JolieValue.NestedListBuilder<B> constructFrom( String name, SequencedCollection<? extends JolieValue> c ) { return JolieValue.constructNestedListFrom( c, ls -> put( name, ls ) ); }

    public final JolieValue.NestedBuilder<B> constructAsFrom( String name, JolieValue j ) { return JolieValue.constructNestedFrom( j, e -> putAs( name, e ) ); }

    public final JolieValue.NestedListBuilder<B> reconstruct( String name ) { return constructFrom( name, children.getOrDefault( name, List.of() ) ); }

    public final JolieValue.NestedBuilder<B> reconstructAs( String name ) { 
        return Optional.ofNullable( children.getOrDefault( name, null ) )
            .map( c -> c.isEmpty() ? null : c.getFirst() )
            .map( e -> constructAsFrom( name, e ) )
            .orElse( constructAs( name ) );
    }

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
