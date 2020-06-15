package jolie.lang.parse.ast.types;

public abstract class TypeNativeConstraint< T > {

	public abstract boolean checkConstraints( T value );

	public abstract boolean checkEqualness( TypeNativeConstraint typeNativeConstraint );

	public abstract String getConstraintDefinition();
}
