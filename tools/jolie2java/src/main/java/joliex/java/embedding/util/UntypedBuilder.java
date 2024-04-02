package joliex.java.embedding.util;

import java.util.List;
import java.util.Map;
import java.util.SequencedCollection;
import java.util.function.UnaryOperator;

import jolie.runtime.ByteArray;
import joliex.java.embedding.BasicType;
import joliex.java.embedding.JolieType;
import joliex.java.embedding.StructureType;

public abstract class UntypedBuilder<T extends BasicType<?>,B> extends StructureBuilder<T,B> {

    protected UntypedBuilder() {}
    protected UntypedBuilder( final T root, final Map<String, List<StructureType>> children ) { super( root, children ); }
    
    public B putAs( String name, Boolean entryValue ) { return putAs( name, BasicType.create( entryValue ) ); }
    public B putAs( String name, Integer entryValue ) { return putAs( name, BasicType.create( entryValue ) ); }
    public B putAs( String name, Long entryValue ) { return putAs( name, BasicType.create( entryValue ) ); }
    public B putAs( String name, Double entryValue ) { return putAs( name, BasicType.create( entryValue ) ); }
    public B putAs( String name, String entryValue ) { return putAs( name, BasicType.create( entryValue ) ); }
    public B putAs( String name, ByteArray entryValue ) { return putAs( name, BasicType.create( entryValue ) ); }

    public StructureType.NestedListBuilder<B> construct( String name ) { return StructureType.constructNestedList( child -> put( name, child ) ); }
    public StructureType.NestedListBuilder<B> constructFrom( String name, SequencedCollection<? extends JolieType> c ) { return StructureType.constructNestedList( child -> put( name, child ), c ); }
    public StructureType.NestedListBuilder<B> reconstruct( String name ) { return constructFrom( name, child( name ) ); }

    public StructureType.NestedBuilder<B> constructAs( String name ) { return StructureType.constructNested( childEntry -> putAs( name, childEntry ) ); }
    public StructureType.NestedBuilder<B> constructAsFrom( String name, JolieType e ) { return StructureType.constructNested( childEntry -> putAs( name, childEntry ), e ); }
    public StructureType.NestedBuilder<B> reconstructAs( String name ) { return firstChild( name ).map( e -> constructAsFrom( name, e ) ).orElse( constructAs( name ) ); }

    public StructureType.NestedBuilder<B> constructAs( String name, BasicType<?> root ) { return constructAs( name ).root( root ); }
    public StructureType.NestedBuilder<B> constructAs( String name, Boolean rootValue ) { return constructAs( name, BasicType.create( rootValue ) ); }
    public StructureType.NestedBuilder<B> constructAs( String name, Integer rootValue ) { return constructAs( name, BasicType.create( rootValue ) ); }
    public StructureType.NestedBuilder<B> constructAs( String name, Long rootValue ) { return constructAs( name, BasicType.create( rootValue ) ); }
    public StructureType.NestedBuilder<B> constructAs( String name, Double rootValue ) { return constructAs( name, BasicType.create( rootValue ) ); }
    public StructureType.NestedBuilder<B> constructAs( String name, String rootValue ) { return constructAs( name, BasicType.create( rootValue ) ); }
    public StructureType.NestedBuilder<B> constructAs( String name, ByteArray rootValue ) { return constructAs( name, BasicType.create( rootValue ) ); }

    public StructureType.NestedBuilder<B> reconstructAs( String name, UnaryOperator<BasicType<?>> rootOperator ) { return reconstructAs( name ).root( rootOperator ); }
}
