package jolie.runtime.embedding.java.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.SequencedCollection;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import jolie.runtime.embedding.java.JolieValue;

public abstract class AbstractListBuilder< B, E > {

	private final List< E > elements;

	private AbstractListBuilder( ArrayList< E > elements ) {
		this.elements = Collections.synchronizedList( elements );
	}

	protected AbstractListBuilder() {
		this( new ArrayList<>() );
	}

	protected AbstractListBuilder( SequencedCollection< ? extends JolieValue > elements,
		Function< JolieValue, E > mapper ) {
		this( elements.parallelStream().map( mapper ).collect( Collectors.toCollection( ArrayList::new ) ) );
	}

	protected abstract B self();

	protected final E get( int index ) {
		return elements.get( index );
	}

	/**
	 * Convenience method meant to make certain operations easier.
	 *
	 * @param <T> the type returned by the given function
	 * @param f the function to apply to this Builder
	 * @return the result of applying the given function {@code f} to this Builder
	 * @custom.apiNote The following is an example of using this method to make adding multiple elements
	 *                 easier:
	 *
	 *                 <pre>
	 * JolieNative.listBuilder()
	 * 		.chain( b -> {
	 * 			for ( int i = 0; i {@literal <} 10; i++ )
	 * 				b.add( i );
	 * 			return b;
	 * 		} )
	 * 		.build();
	 *                 </pre>
	 **/
	public final < T > T chain( Function< B, T > f ) {
		return f.apply( self() );
	}

	/**
	 * Appends the specified element to the end of the list being built, or does nothing if the element
	 * is {@code null}.
	 *
	 * @param e element to be appended to the list being built
	 * @return this builder
	 * @see List#add(Object)
	 */
	public final B add( E e ) {
		if( e != null )
			elements.add( e );

		return self();
	}

	/**
	 * Inserts the specified element into the list being built at the specified position, or does
	 * nothing if the specified element is {@code null}.
	 *
	 * @param index index of the element to replace
	 * @param e element to be stored at the specified position
	 * @return this builder
	 * @see List#add(int,Object)
	 */
	public final B add( int index, E e ) {
		if( e != null )
			elements.add( index, e );

		return self();
	}

	/**
	 * Adds an element as the first element of the list being built, or does nothing if the element is
	 * {@code null}.
	 *
	 * @param e element to be appended to the list being built
	 * @return this builder
	 * @see List#addFirst(Object)
	 */
	public final B addFirst( E e ) {
		return add( 0, e );
	}

	/**
	 * Adds an element as the last element of the list being built, or does nothing if the element is
	 * {@code null}.
	 *
	 * @param e element to be appended to the list being built
	 * @return this builder
	 * @see List#addLast(Object)
	 */
	public final B addLast( E e ) {
		return add( e );
	}

	/**
	 * Appends all of the elements in the specified collection to the end of the list being built, in
	 * the order that they are returned by the specified collection's iterator
	 *
	 * @param c collection containing elements to be added to this list
	 * @return this builder
	 * @throws NullPointerException if the specified collection or any of its elements are {@code null}
	 *
	 * @see List#addAll(Collection)
	 */
	public final B addAll( Collection< ? extends E > c ) {
		if( Objects.requireNonNull( c ).contains( null ) )
			throw new NullPointerException( "Cannot add the elements of a collection that contains null." );

		elements.addAll( c );
		return self();
	}

	/**
	 * Inserts all of the elements in the specified collection into the list being built at the
	 * specified position.
	 *
	 * @param index index at which to insert the first element from the specified collection
	 * @param c collection containing elements to be added to this list
	 * @return this builder
	 * @throws NullPointerException if the specified collection or any of its elements are {@code null}
	 *
	 * @see List#addAll(int, Collection)
	 */
	public final B addAll( int index, Collection< ? extends E > c ) {
		if( c.contains( null ) )
			throw new NullPointerException( "Cannot add the elements of a collection that contains null." );

		elements.addAll( index, c );
		return self();
	}


	/**
	 * Replaces the element at the specified position in the list being built with the specified
	 * element, or removes the element if the specified element is {@code null}.
	 *
	 * @param index index of the element to replace
	 * @param e element to be stored at the specified position
	 * @return this builder
	 * @see List#set(int,Object)
	 */
	public final B set( int index, E e ) {
		if( e == null )
			return remove( index );

		elements.set( index, e );
		return self();
	}

	/**
	 * Replaces the element at the specified position in the list being built with the result of
	 * applying the specified operator to it.
	 *
	 * @param index index of the element to replace
	 * @param operator operator to apply to the element
	 * @return this builder
	 * @see #set(int,Object)
	 */
	public final B replace( int index, UnaryOperator< E > operator ) {
		return set( index, operator.apply( elements.get( index ) ) );
	}

	/**
	 * Replaces each element of the list being built with the result of applying the operator to that
	 * element.
	 *
	 * @param operator the operator to apply to each element
	 * @return this builder
	 * @see List#replaceAll(UnaryOperator)
	 */
	public final B replaceAll( UnaryOperator< E > operator ) {
		elements.replaceAll( operator );
		return self();
	}

	/**
	 * Retains only the elements in the list being built that are contained in the specified collection.
	 *
	 * @param c collection containing elements to be retained in the list being built
	 * @return this builder
	 * @see List#retainAll(Collection)
	 */
	public final B retainAll( Collection< ? extends E > c ) {
		elements.retainAll( c );
		return self();
	}

	/**
	 * Removes the element at the specified position in the list being built.
	 *
	 * @param index the index of the element to be removed
	 * @return this builder
	 * @see List#remove(int)
	 */
	public final B remove( int index ) {
		elements.remove( index );
		return self();
	}

	/**
	 * Removes the first occurrence of the specified element from the list being built, if it is
	 * present.
	 *
	 * @param o element to be removed from this list, if present
	 * @return this builder
	 * @see List#remove(Object)
	 */
	public final B remove( Object o ) {
		elements.remove( o );
		return self();
	}

	/**
	 * Removes the first element of the list being built.
	 *
	 * @return this builder
	 */
	public final B removeFirst() {
		elements.removeFirst();
		return self();
	}

	/**
	 * Removes the last element of the list being built.
	 *
	 * @return this builder
	 */
	public final B removeLast() {
		elements.removeLast();
		return self();
	}

	/**
	 * Removes all of the elements of the list being built that satisfy the given predicate.
	 *
	 * @param filter a predicate that returns true for elements to be removed
	 * @return this builder
	 */
	public final B removeIf( Predicate< ? super E > filter ) {
		elements.removeIf( filter );
		return self();
	}

	/**
	 * Returns the list being built by this builder.
	 *
	 * @return the list being built by this builder
	 */
	protected List< E > build() {
		return elements;
	}
}
