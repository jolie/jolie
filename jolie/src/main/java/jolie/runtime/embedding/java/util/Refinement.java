package jolie.runtime.embedding.java.util;

import java.util.List;
import java.util.function.Predicate;

public sealed interface Refinement<T> extends Predicate<T> {

    public static record Range<N extends Number & Comparable<N>>( N min, N max ) implements Refinement<N> {

        public boolean test( N value ) {
            return value.compareTo( min ) >= 0 && value.compareTo( max ) <= 0;
        }
    }

    public static record Length( int min, int max ) implements Refinement<String> {

        public boolean test( String value ) {
            return value.length() >= min && value.length() <= max;
        }
    }

    public static record Regex( String regexString ) implements Refinement<String> {

        public boolean test( String value ) {
            return value.matches( regexString );
        }
    }

    public static record Enumeration( List<String> values ) implements Refinement<String> {

        public boolean test( String value ) {
            return values.parallelStream().anyMatch( value::equals );
        }
    }

    public static Refinement<Integer> range( Integer min, Integer max ) { return new Range<>( min, max ); }
    public static Refinement<Long> range( Long min, Long max ) { return new Range<>( min, max ); }
    public static Refinement<Double> range( Double min, Double max ) { return new Range<>( min, max ); }
    public static Refinement<String> length( int min, int max ) { return new Length( min, max ); }
    public static Refinement<String> regex( String regex ) { return new Regex( regex ); }
    public static Refinement<String> enumeration( String... values ) { return new Enumeration( List.of( values ) ); }
}
