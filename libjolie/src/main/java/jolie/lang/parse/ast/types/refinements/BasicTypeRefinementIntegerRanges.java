package jolie.lang.parse.ast.types.refinements;

import java.io.Serializable;
import java.util.ArrayList;

public class BasicTypeRefinementIntegerRanges implements Serializable, BasicTypeRefinement< Integer > {
	public static class Interval {
		private final int min;
		private final int max;

		public Interval( int min, int max ) {
			this.min = min;
			this.max = max;
		}

		public int getMax() {
			return max;
		}

		public int getMin() {
			return min;
		}

		public boolean checkInterval( int value ) {
			return (value >= min) && (value <= max);
		}

		public boolean checkIntervalEqualness( Interval interval ) {
			return (this.min == interval.getMin()) && (this.max == interval.getMax());
		}


	}

	private final ArrayList< Interval > ranges = new ArrayList<>();

	public BasicTypeRefinementIntegerRanges() {}

	public void addInterval( Interval interval ) {
		this.ranges.add( interval );
	}

	@Override
	public boolean checkValue( Integer value ) {
		return ranges.stream().anyMatch( i -> i.checkInterval( value ) );
	}

	@Override
	public boolean checkEqualness( BasicTypeRefinement< ? > basicTypeRefinement ) {
		if( basicTypeRefinement instanceof BasicTypeRefinementIntegerRanges ) {
			BasicTypeRefinementIntegerRanges basicTypeRefinementIntegerRanges =
				(BasicTypeRefinementIntegerRanges) basicTypeRefinement;
			return ranges.stream()
				.allMatch( i -> checkIntervalToIntervals( i, basicTypeRefinementIntegerRanges.getRanges() ) );
		} else {
			return false;
		}
	}

	@Override
	public String getDocumentation() {
		return "ranges([[min1,max1],[min2,max2],...])";
	}

	public ArrayList< Interval > getRanges() {
		return this.ranges;
	}

	private static boolean checkIntervalToIntervals( Interval interval, ArrayList< Interval > intervalArrayList ) {
		return intervalArrayList.stream().anyMatch( i -> i.checkIntervalEqualness( interval ) );
	}

}
