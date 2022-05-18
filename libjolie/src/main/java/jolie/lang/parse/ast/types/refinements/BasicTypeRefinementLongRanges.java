package jolie.lang.parse.ast.types.refinements;

import java.io.Serializable;
import java.util.ArrayList;

public class BasicTypeRefinementLongRanges implements Serializable, BasicTypeRefinement< Long > {
	public static class Interval {
		private final long min;
		private final long max;

		public Interval( long min, long max ) {
			this.min = min;
			this.max = max;
		}

		public long getMax() {
			return max;
		}

		public long getMin() {
			return min;
		}

		public boolean checkInterval( long value ) {
			return (value >= min) && (value <= max);
		}

		public boolean checkIntervalEqualness( Interval interval ) {
			return (this.min == interval.getMin()) && (this.max == interval.getMax());
		}


	}

	private final ArrayList< Interval > ranges = new ArrayList<>();

	public BasicTypeRefinementLongRanges() {}

	public void addInterval( Interval interval ) {
		this.ranges.add( interval );
	}

	@Override
	public boolean checkValue( Long value ) {
		return ranges.stream().anyMatch( i -> i.checkInterval( value ) );
	}

	@Override
	public boolean checkEqualness( BasicTypeRefinement< ? > basicTypeRefinement ) {
		if( basicTypeRefinement instanceof BasicTypeRefinementLongRanges ) {
			BasicTypeRefinementLongRanges basicTypeRefinementIntegerRanges =
				(BasicTypeRefinementLongRanges) basicTypeRefinement;
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
