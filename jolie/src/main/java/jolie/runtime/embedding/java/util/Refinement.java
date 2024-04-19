package jolie.runtime.embedding.java.util;

import java.util.Collection;
import java.util.List;

import jolie.runtime.embedding.java.JolieNative;
import jolie.runtime.embedding.java.TypeValidationException;

public sealed interface Refinement<V> {

    boolean validate( V value );

    public static record Range<N extends Number & Comparable<N>>( N min, N max ) implements Refinement<N> {

        public boolean validate( N value ) {
            return value.compareTo( min ) >= 0 && value.compareTo( max ) <= 0;
        }
    }

    public static record Length( int min, int max ) implements Refinement<String> {

        public boolean validate( String value ) {
            return value.length() >= min && value.length() <= max;
        }
    }

    public static record Regex( String regexString ) implements Refinement<String> {

        public boolean validate( String value ) {
            return value.matches( regexString );
        }
    }

    public static record Enumeration( Collection<String> values ) implements Refinement<String> {

        public boolean validate( String value ) {
            return values.parallelStream().anyMatch( value::equals );
        }
    }

    
    public static <V> V validated( V contentValue, Collection<? extends Refinement<V>> refinements ) {
        if ( !refinements.parallelStream().anyMatch( r -> r.validate( contentValue ) ) )
            throw new TypeValidationException( "The given content doesn't abide by the constraints imposed by the refinements of this type's content." );

        return contentValue;
    }

    public static <V, R extends JolieNative<V>> R validated( R content, Collection<? extends Refinement<V>> refinements ) { 
        validated( content.value(), refinements );
        return content;
    }

    public static Refinement<Integer> createRange( Integer min, Integer max ) { return new Range<>( min, max ); }
    public static Refinement<Long> createRange( Long min, Long max ) { return new Range<>( min, max ); }
    public static Refinement<Double> createRange( Double min, Double max ) { return new Range<>( min, max ); }
    public static Refinement<String> createLength( int min, int max ) { return new Length( min, max ); }
    public static Refinement<String> createRegex( String regex ) { return new Regex( regex ); }
    public static Refinement<String> createEnum( String... enumerations ) { return new Enumeration( List.of( enumerations ) ); }
}
