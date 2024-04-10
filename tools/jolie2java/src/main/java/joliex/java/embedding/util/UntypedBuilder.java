package joliex.java.embedding.util;

import java.util.List;
import java.util.Map;
import java.util.SequencedCollection;
import java.util.function.UnaryOperator;

import jolie.runtime.ByteArray;
import joliex.java.embedding.JolieNative;
import joliex.java.embedding.JolieValue;

public abstract class UntypedBuilder<T extends JolieNative<?>,B> extends StructureBuilder<T,B> {

    protected UntypedBuilder() {}
    protected UntypedBuilder( final T content, final Map<String, List<JolieValue>> children ) { super( content, children ); }
    
    public B putAs( String name, Boolean entryValue ) { return putAs( name, JolieValue.create( entryValue ) ); }
    public B putAs( String name, Integer entryValue ) { return putAs( name, JolieValue.create( entryValue ) ); }
    public B putAs( String name, Long entryValue ) { return putAs( name, JolieValue.create( entryValue ) ); }
    public B putAs( String name, Double entryValue ) { return putAs( name, JolieValue.create( entryValue ) ); }
    public B putAs( String name, String entryValue ) { return putAs( name, JolieValue.create( entryValue ) ); }
    public B putAs( String name, ByteArray entryValue ) { return putAs( name, JolieValue.create( entryValue ) ); }

    public JolieValue.NestedListBuilder<B> construct( String name ) { return JolieValue.constructNestedList( child -> put( name, child ) ); }
    public JolieValue.NestedListBuilder<B> constructFrom( String name, SequencedCollection<? extends JolieValue> c ) { return JolieValue.constructNestedList( child -> put( name, child ), c ); }
    public JolieValue.NestedListBuilder<B> reconstruct( String name ) { return constructFrom( name, child( name ) ); }

    public JolieValue.NestedBuilder<B> constructAs( String name ) { return JolieValue.constructNested( childEntry -> putAs( name, childEntry ) ); }
    public JolieValue.NestedBuilder<B> constructAsFrom( String name, JolieValue e ) { return JolieValue.constructNested( childEntry -> putAs( name, childEntry ), e ); }
    public JolieValue.NestedBuilder<B> reconstructAs( String name ) { return firstChild( name ).map( e -> constructAsFrom( name, e ) ).orElse( constructAs( name ) ); }

    public JolieValue.NestedBuilder<B> constructAs( String name, JolieNative<?> content ) { return constructAs( name ).content( content ); }
    public JolieValue.NestedBuilder<B> constructAs( String name, Boolean contentValue ) { return constructAs( name, JolieNative.create( contentValue ) ); }
    public JolieValue.NestedBuilder<B> constructAs( String name, Integer contentValue ) { return constructAs( name, JolieNative.create( contentValue ) ); }
    public JolieValue.NestedBuilder<B> constructAs( String name, Long contentValue ) { return constructAs( name, JolieNative.create( contentValue ) ); }
    public JolieValue.NestedBuilder<B> constructAs( String name, Double contentValue ) { return constructAs( name, JolieNative.create( contentValue ) ); }
    public JolieValue.NestedBuilder<B> constructAs( String name, String contentValue ) { return constructAs( name, JolieNative.create( contentValue ) ); }
    public JolieValue.NestedBuilder<B> constructAs( String name, ByteArray contentValue ) { return constructAs( name, JolieNative.create( contentValue ) ); }

    public JolieValue.NestedBuilder<B> reconstructAs( String name, UnaryOperator<JolieNative<?>> contentOperator ) { return reconstructAs( name ).content( contentOperator ); }
}
