package jolie.lang.parse.ast.types;

public class TypeNativeConstrainedString extends TypeNativeConstraint< String > {

	private final int max;
	private final int min;

	public TypeNativeConstrainedString( int min, int max ) {
		this.max = max;
		this.min = min;
	}

	@Override
	public boolean checkConstraints( String value ) {
		return (value.length() <= max) && (value.length() >= min);
	}

	@Override
	public boolean checkEqualness( TypeNativeConstraint typeNativeConstraint ) {
		if( typeNativeConstraint instanceof TypeNativeConstrainedString ) {
			TypeNativeConstrainedString typeNativeConstrainedStringCast =
				(TypeNativeConstrainedString) typeNativeConstraint;
			if( this.max != typeNativeConstrainedStringCast.getMax()
				|| this.min != typeNativeConstrainedStringCast.getMin() ) {
				return false;
			}
		} else {
			return false;
		}
		return true;
	}

	@Override
	public String getConstraintDefinition() {
		return "( min length:" + min + ", max length:" + max + " )";
	}

	public int getMax() {
		return this.max;
	}

	public int getMin() {
		return this.min;
	}
}
