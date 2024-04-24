package jolie.runtime.embedding.java.util;

import java.util.SequencedCollection;
import java.util.function.Function;
import java.util.function.Supplier;
import jolie.runtime.embedding.java.JolieValue;

public class StructureListBuilder<E extends JolieValue, B> extends AbstractListBuilder<StructureListBuilder<E,B>, E> {
	
	private final Supplier<B> builder;
	private final Function<JolieValue,B> rebuilder;

	public StructureListBuilder( Supplier<B> builder, Function<JolieValue,B> rebuilder ) { 
		this.builder = builder; 
		this.rebuilder = rebuilder;
	}
	public StructureListBuilder( SequencedCollection<? extends JolieValue> elements, Function<JolieValue, E> mapper, Supplier<B> builder, Function<JolieValue,B> rebuilder ) { 
		super( elements, mapper );
		this.builder = builder;
		this.rebuilder = rebuilder;
	}
	
	protected StructureListBuilder<E,B> self() { return this; }

	public StructureListBuilder<E,B> add( Function<B,E> f ) { return add( f.apply( builder.get() ) ); }
    public StructureListBuilder<E,B> set( int index, Function<B,E> f ) { return set( index, f.apply( builder.get() ) ); }
    public StructureListBuilder<E,B> rebuild( int index, Function<B,E> f ) { return replace( index, j -> f.apply( rebuilder.apply( j ) ) ); }
}
