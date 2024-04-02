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

    String createString();
    String definitionString();

    public static record NumberRefinement( Collection<Constraint.Range> constraints ) implements NativeRefinement {

        public String createString() {
            return constraints.parallelStream()
                .map( Constraint::createString )
                .reduce( (s1, s2) -> s1 + ", " + s2 )
                .map( s -> "List.of( " + s + " )" )
                .orElseThrow();
        }

        public String definitionString() {
            return constraints.parallelStream()
                .map( Constraint::definitionString )
                .reduce( (s1, s2) -> s1 + ", " + s2 )
                .map( s -> "ranges( " + s + " )" )
                .orElseThrow();
        }

        public boolean equals( Object obj ) {
            return obj != null && obj instanceof NumberRefinement other && constraints.size() == other.constraints().size() && constraints.containsAll( other.constraints() );
        }
    }

    public static record StringRefinement( Collection<Constraint.StringRestriction> constraints ) implements NativeRefinement {

        public String createString() {
            return constraints.parallelStream()
                .map( Constraint::createString )
                .reduce( (s1, s2) -> s1 + ", " + s2 )
                .map( s -> "List.of( " + s + " )" )
                .orElseThrow();
        }

        public String definitionString() {
            return constraints.parallelStream()
                .map( Constraint::definitionString )
                .reduce( (s1, s2) -> s1 + " | " + s2 )
                .orElseThrow();
        }

        public boolean equals( Object obj ) {
            return obj != null && obj instanceof StringRefinement other && constraints.size() == other.constraints().size() && constraints.containsAll( other.constraints() );
        }
    }

    public static NativeRefinement merge( NativeRefinement r1, NativeRefinement r2 ) {
        if ( r1 == null || r2 == null )
            return null;

        if ( r1 instanceof NumberRefinement nr1 && r2 instanceof NumberRefinement nr2 )
            return new NumberRefinement( Stream.concat( nr1.constraints().stream(), nr2.constraints().stream() ).toList() );
        
        if ( r1 instanceof StringRefinement sr1 && r2 instanceof StringRefinement sr2 )
            return new StringRefinement( Stream.concat( sr1.constraints().stream(), sr2.constraints().stream() ).toList() );

        throw new IllegalArgumentException( "Can only merge Refinements of the same type." );
    }

    public static NativeRefinement create( Collection<BasicTypeRefinement<?>> refinements ) {
        return refinements.stream()
            .map( NativeRefinement::create )
            .reduce( NativeRefinement::merge )
            .orElse( null );
    }

    public static NativeRefinement create( BasicTypeRefinement<?> refinement ) {
        return switch ( refinement ) {

            case BasicTypeRefinementStringLength r -> new StringRefinement( List.of( new Constraint.StringRestriction.Length( r.getMin(), r.getMax() ) ) );

            case BasicTypeRefinementStringList r -> new StringRefinement( List.of( new Constraint.StringRestriction.Enumeration( r.getList() ) ) );

            case BasicTypeRefinementStringRegex r -> new StringRefinement( List.of( new Constraint.StringRestriction.Regex( Matcher.quoteReplacement( r.getRegex() ) ) ) );

            case BasicTypeRefinementIntegerRanges r -> new NumberRefinement( r.getRanges().parallelStream().map( interval -> new Constraint.Range( interval.getMin(), interval.getMax() ) ).toList() );

            case BasicTypeRefinementLongRanges r -> new NumberRefinement( r.getRanges().parallelStream().map( interval -> new Constraint.Range( interval.getMin(), interval.getMax() ) ).toList() );

            case BasicTypeRefinementDoubleRanges r -> new NumberRefinement( r.getRanges().parallelStream().map( interval -> new Constraint.Range( interval.getMin(), interval.getMax() ) ).toList() );

            default -> throw new UnsupportedOperationException( "Got unexpected basic type refinement." );
        };
    }

    public static sealed interface Constraint {

        String createString();
        String definitionString();

        public static record Range( Number min, Number max ) implements Constraint {

            private String minString() { return min + (min instanceof Long ? "L" : ""); }
            private String maxString() { return max + (max instanceof Long ? "L" : ""); }
    
            public String createString() { return "Refinement.createRange( " + minString() + ", " + maxString() + " )"; }
            public String definitionString() { return "[" + minString() + ", " + maxString() + "]"; }
    
            public boolean equals( Object obj ) {
                return obj != null && obj instanceof Range other && min == other.min() && max == other.max();
            }
        }

        public static sealed interface StringRestriction extends Constraint {

            public static record Length( int min, int max ) implements StringRestriction {

                public String createString() {
                    return "Refinement.createLength( " + min + ", " + max + " )";
                }
        
                public String definitionString() {
                    return "length( [" + min + ", " + max + "] )";
                }
        
                public boolean equals( Object obj ) {
                    return obj != null && obj instanceof Length other && min == other.min() && max == other.max();
                }
            }
        
            public static record Enumeration( Collection<String> values ) implements StringRestriction {
        
                public String createString() {
                    return "Refinement.createEnum( " + values.stream().map( s -> "\"" + s + "\"" ).reduce( (s1, s2) -> s1 + ", " + s2 ).orElseThrow() + " )";
                }
        
                public String definitionString() {
                    return "enum( [" + values.stream().reduce( (v1, v2) -> v1 + ", " + v2 ).orElseThrow() + "] )";
                }
        
                public boolean equals( Object obj ) {
                    return obj != null && obj instanceof Enumeration other && values.size() == other.values().size() && values.containsAll( other.values() );
                }
            }
        
            public static record Regex( String regex ) implements StringRestriction {
        
                public String createString() {
                    return "Refinement.createRegex( \"" + regex + "\" )";
                }
        
                public String definitionString() {
                    return "regex( " + regex + " )";
                }
        
                public boolean equals( Object obj ) {
                    return obj != null && obj instanceof Regex other && regex.equals( other.regex() );
                }
            }
        }
    }
}
