package jolie.lang.parse.ast.types.refinements;

import java.io.Serializable;

public class BasicTypeRefinementStringLength implements Serializable, BasicTypeRefinement< String > {
	private final int max;
	private final int min;

	public BasicTypeRefinementStringLength( int min, int max ) {
		this.max = max;
		this.min = min;
	}

	@Override
	public boolean checkValue( String value ) {
		return (value.length() <= max) && (value.length() >= min);
	}

	@Override
	public boolean checkEqualness( BasicTypeRefinement< ? > basicTypeRefinement ) {
		if( basicTypeRefinement instanceof BasicTypeRefinementStringLength ) {
			BasicTypeRefinementStringLength basicTypeRefinementStringLength =
				(BasicTypeRefinementStringLength) basicTypeRefinement;
			if( this.max != basicTypeRefinementStringLength.getMax()
				|| this.min != basicTypeRefinementStringLength.getMin() ) {
				return false;
			}
		} else {
			return false;
		}
		return true;
	}

	@Override
	public String getDocumentation() {
		return "length([" + min + "," + max + "])";
	}

	public int getMax() {
		return this.max;
	}

	public int getMin() {
		return this.min;
	}
}
