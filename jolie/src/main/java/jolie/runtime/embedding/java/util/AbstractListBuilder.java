package jolie.runtime.embedding.java.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.SequencedCollection;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import jolie.runtime.embedding.java.JolieValue;

public abstract class AbstractListBuilder<B,E> {

	private final List<E> elements;

	private AbstractListBuilder( ArrayList<E> elements ) { this.elements = Collections.synchronizedList( elements ); }
	protected AbstractListBuilder() { this( new ArrayList<>() ); }
	protected AbstractListBuilder( SequencedCollection<? extends JolieValue> elements, Function<JolieValue, E> mapper ) { 
		this( elements.parallelStream().map( mapper ).collect( Collectors.toCollection( ArrayList::new ) ) );
	}

	protected abstract B self();

	/**
	 * Convenience method meant to make certain operations easier.
	 * 
	 * @param <T> the type returned by the given function, and therefore also by this method
	 * @param f the function to apply to this Builder
	 * @return the result of applying the given function {@code f} to this Builder
	 * @apiNote The following is an example of using the method to make adding multiple elements easier:
	 * <pre>
	 * JolieNative.constructList()
	 * 		.chain( b -> {
	 * 			for ( int i = 0; i < 10; i++ )
	 * 				b.add( i );
	 * 			return b;
	 * 		} )
	 * 		.build();
	 * </pre>
	 **/
	public final <T> T chain( Function<B,T> f ) { return f.apply( self() ); }

	public final B add( E e ) { elements.add( e ); return self(); }
	public final B addFirst( E e ) { elements.addFirst( e ); return self(); }
	public final B addLast( E e ) { elements.addLast( e ); return self(); }
	public final B addAll( Collection<? extends E> c ) { elements.addAll( c ); return self(); }
	public final B addAll( int index, Collection<? extends E> c ) { elements.addAll( index, c ); return self(); }
	public final B set( int index, E e ) { elements.set( index, e ); return self(); }
	public final B replace( int index, UnaryOperator<E> operator ) { return set( index, operator.apply( elements.get( index ) ) ); }
	public final B replaceAll( UnaryOperator<E> operator ) { elements.replaceAll( operator ); return self(); }
	public final B retainAll( Collection<? extends E> c ) { elements.retainAll( c ); return self(); }
	public final B remove( int index ) { elements.remove( index ); return self(); }
	public final B remove( Object o ) { elements.remove( o ); return self(); }
	public final B removeFirst() { elements.removeFirst(); return self(); }
	public final B removeLast() { elements.removeLast(); return self(); }
	public final B removeIf( Predicate<? super E> filter ) { elements.removeIf( filter ); return self(); }

	public final List<E> build() { return elements; }
}
