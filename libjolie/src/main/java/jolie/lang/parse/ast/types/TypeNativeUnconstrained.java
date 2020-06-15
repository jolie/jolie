package jolie.lang.parse.ast.types;

public class TypeNativeUnconstrained extends TypeNativeConstraint< Object > {


	@Override
	public boolean checkConstraints( Object value ) {
		// always return true
		return true;
	}

	@Override
	public boolean checkEqualness( TypeNativeConstraint typeNativeConstraint ) {
		if( typeNativeConstraint instanceof TypeNativeUnconstrained ) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public String getConstraintDefinition() {
		return new String();
	}
}
