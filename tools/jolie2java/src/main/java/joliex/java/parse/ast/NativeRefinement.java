package joliex.java.parse.ast;

import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.stream.Stream;

import jolie.lang.parse.ast.types.refinements.BasicTypeRefinement;
import jolie.lang.parse.ast.types.refinements.BasicTypeRefinementDoubleRanges;
import jolie.lang.parse.ast.types.refinements.BasicTypeRefinementIntegerRanges;
import jolie.lang.parse.ast.types.refinements.BasicTypeRefinementLongRanges;
import jolie.lang.parse.ast.types.refinements.BasicTypeRefinementStringLength;
import jolie.lang.parse.ast.types.refinements.BasicTypeRefinementStringList;
import jolie.lang.parse.ast.types.refinements.BasicTypeRefinementStringRegex;

public sealed interface NativeRefinement {

    String definitionString();

    public static record Ranges( List<Interval> intervals ) implements NativeRefinement {

        public static record Interval( Number min, Number max ) {

            public String minString() { return min + (min instanceof Long ? "L" : ""); }
            public String maxString() { return max + (max instanceof Long ? "L" : ""); }
    
            public boolean equals( Object obj ) {
                return obj != null && obj instanceof Interval other && min == other.min() && max == other.max();
            }
        }

        public String definitionString() {
            return intervals.parallelStream()
                .map( i -> "[" + i.minString() + ", " + i.maxString() + "]" )
                .reduce( (s1, s2) -> s1 + ", " + s2 )
                .map( s -> "ranges( " + s + " )" )
                .orElseThrow();
        }

        public boolean equals( Object obj ) {
            return obj != null && obj instanceof Ranges other && intervals.size() == other.intervals().size() && intervals.containsAll( other.intervals() );
        }
    }

    public static record Length( int min, int max ) implements NativeRefinement {
        
        public String definitionString() {
            return "length( [" + min + ", " + max + "] )";
        }

        public boolean equals( Object obj ) {
            return obj != null && obj instanceof Length other && min == other.min() && max == other.max();
        }
    }

    public static record Enumeration( List<String> values ) implements NativeRefinement {

        public String definitionString() {
            return "enum( [" + values.stream().reduce( (v1, v2) -> v1 + ", " + v2 ).orElseThrow() + "] )";
        }

        public boolean equals( Object obj ) {
            return obj != null && obj instanceof Enumeration other && values.size() == other.values().size() && values.containsAll( other.values() );
        }
    }

    public static record Regex( String regex ) implements NativeRefinement {

        public String definitionString() {
            return "regex( " + regex + " )";
        }

        public boolean equals( Object obj ) {
            return obj != null && obj instanceof Regex other && regex.equals( other.regex() );
        }
    }

    public static NativeRefinement merge( NativeRefinement r1, NativeRefinement r2 ) {
        if ( r1 == null || r2 == null )
            return null;

        if ( r1 instanceof Ranges nr1 && r2 instanceof Ranges nr2 )
            return new Ranges( Stream.concat( nr1.intervals().stream(), nr2.intervals().stream() ).toList() );

        throw new IllegalArgumentException( "Tried to merge refinements which weren't ranges." );
    }

    public static NativeRefinement create( Collection<BasicTypeRefinement<?>> refinements ) {
        return refinements.stream()
            .map( NativeRefinement::create )
            .reduce( NativeRefinement::merge )
            .orElse( null );
    }

    public static NativeRefinement create( BasicTypeRefinement<?> refinement ) {
        return switch ( refinement ) {

            case BasicTypeRefinementStringLength r -> new Length( r.getMin(), r.getMax() );

            case BasicTypeRefinementStringList r -> new Enumeration( r.getList() );

            case BasicTypeRefinementStringRegex r -> new Regex( Matcher.quoteReplacement( r.getRegex() ) );

            case BasicTypeRefinementIntegerRanges r -> new Ranges( r.getRanges().parallelStream().map( i -> new Ranges.Interval( i.getMin(), i.getMax() ) ).toList() );

            case BasicTypeRefinementLongRanges r -> new Ranges( r.getRanges().parallelStream().map( i -> new Ranges.Interval( i.getMin(), i.getMax() ) ).toList() );

            case BasicTypeRefinementDoubleRanges r -> new Ranges( r.getRanges().parallelStream().map( i -> new Ranges.Interval( i.getMin(), i.getMax() ) ).toList() );

            default -> throw new UnsupportedOperationException( "Got unexpected basic type refinement." );
        };
    }
}
