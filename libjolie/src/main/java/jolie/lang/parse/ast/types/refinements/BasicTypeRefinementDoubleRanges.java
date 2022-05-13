package jolie.lang.parse.ast.types.refinements;

import java.io.Serializable;
import java.util.ArrayList;

public class BasicTypeRefinementDoubleRanges implements Serializable, BasicTypeRefinement< Double > {
	public static class Interval {
		private final double min;
		private final double max;

		public Interval( double min, double max ) {
			this.min = min;
			this.max = max;
		}

		public double getMax() {
			return max;
		}

		public double getMin() {
			return min;
		}

		public boolean checkInterval( double value ) {
			return (value >= min) && (value <= max);
		}

		public boolean checkIntervalEqualness( Interval interval ) {
			return (this.min == interval.getMin()) && (this.max == interval.getMax());
		}
	}

	private final ArrayList< Interval > ranges = new ArrayList<>();

	public BasicTypeRefinementDoubleRanges() {}

	public void addInterval( Interval interval ) {
		this.ranges.add( interval );
	}

	@Override
	public boolean checkValue( Double value ) {
		return ranges.stream().anyMatch( i -> i.checkInterval( value ) );
	}

	@Override
	public boolean checkEqualness( BasicTypeRefinement< ? > basicTypeRefinement ) {
		if( basicTypeRefinement instanceof BasicTypeRefinementDoubleRanges ) {
			BasicTypeRefinementDoubleRanges basicTypeRefinementIntegerRanges =
				(BasicTypeRefinementDoubleRanges) basicTypeRefinement;
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
