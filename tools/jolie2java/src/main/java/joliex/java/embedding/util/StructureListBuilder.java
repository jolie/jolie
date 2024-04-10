package joliex.java.embedding.util;

import java.util.ArrayList;
import java.util.SequencedCollection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public abstract class StructureListBuilder<T, B> {

    protected final ArrayList<T> elements;

    protected StructureListBuilder( SequencedCollection<? extends T> elements ) { this.elements = new ArrayList<>( elements ); }
    protected StructureListBuilder() { elements = new ArrayList<>(); }

    protected final Optional<T> get( int index ) { return elements.size() > index ? Optional.of( elements.get( index ) ) : Optional.empty(); }

    protected abstract B self();

    public final <U> U chain( Function<B,U> f ) { return f.apply( self() ); }

    public final B add( T e ) { if ( e != null ) elements.add( e ); return self(); }

    public final B addAll( SequencedCollection<? extends T> c ) { if ( c != null ) elements.addAll( c ); return self(); }

    public final B set( int index, T e ) { if ( e != null ) elements.set( index, e ); return self(); }

    public final B replace( int index, UnaryOperator<T> operator ) { elements.set( index, operator.apply( elements.get( index ) ) ); return self(); }
    
    public final B replaceAll( UnaryOperator<T> operator ) { elements.replaceAll( operator ); return self(); }

    public final B remove( int index ) { elements.remove( index ); return self(); }
    public final B remove( Object e ) { elements.remove( e ); return self(); }

    public final B removeAll( SequencedCollection<?> c ) { elements.removeAll( c ); return self(); }

    public final B removeIf( Predicate<? super T> filter ) { elements.removeIf( filter ); return self(); }

    protected List<T> build() { return elements; }
}
