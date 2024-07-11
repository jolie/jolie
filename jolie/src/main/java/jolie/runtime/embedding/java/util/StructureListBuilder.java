package jolie.runtime.embedding.java.util;

import java.util.SequencedCollection;
import java.util.function.Function;
import java.util.function.Supplier;
import jolie.runtime.embedding.java.JolieValue;

/**
 * Specialization of an {@link AbstractListBuilder} to build lists of
 * {@link jolie.runtime.embedding.java.TypedStructure}s or
 * {@link jolie.runtime.embedding.java.UntypedStructure}s.
 *
 * @param <E> the type being built
 * @param <B> the builder class of the type being built
 */
public class StructureListBuilder< E extends JolieValue, B >
	extends AbstractListBuilder< StructureListBuilder< E, B >, E > {

	private final Supplier< B > builder;

	public StructureListBuilder( Supplier< B > builder ) {
		this.builder = builder;
	}

	public StructureListBuilder( SequencedCollection< ? extends JolieValue > elements, Function< JolieValue, E > mapper,
		Supplier< B > builder ) {
		super( elements, mapper );
		this.builder = builder;
	}

	@Override
	protected StructureListBuilder< E, B > self() {
		return this;
	}

	/**
	 * Applies the specified function to a new builder instance, and appends the result to the end of
	 * the list being built.
	 *
	 * @param f the function to apply to a new builder instance
	 * @return this builder
	 * @see AbstractListBuilder#add(Object)
	 */
	public StructureListBuilder< E, B > add( Function< B, E > f ) {
		return add( f.apply( builder.get() ) );
	}

	/**
	 * Applies the specified function to a new builder instance, and inserts the result at the specified
	 * position of the list being built.
	 *
	 * @param f the function to apply to a new builder instance
	 * @return this builder
	 * @see AbstractListBuilder#add(int,Object)
	 */
	public StructureListBuilder< E, B > add( int index, Function< B, E > f ) {
		return add( index, f.apply( builder.get() ) );
	}

	/**
	 * Applies the specified function to a new builder instance, and replaces the element at the
	 * specified position in the list being built with the result.
	 *
	 * @param index index of the element to replace
	 * @param f the function to apply to a new builder instance
	 * @return this builder
	 * @see AbstractListBuilder#set(int,Object)
	 */
	public StructureListBuilder< E, B > set( int index, Function< B, E > f ) {
		return set( index, f.apply( builder.get() ) );
	}
}
