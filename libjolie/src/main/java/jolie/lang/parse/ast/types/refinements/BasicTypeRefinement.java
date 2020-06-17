package jolie.lang.parse.ast.types.refinements;

public abstract class BasicTypeRefinement< T > {

	public abstract boolean checkTypeRefinment( T value );

	public abstract boolean checkEqualness( BasicTypeRefinement basicTypeRefinement );

	public abstract String getRefinementDocumentationDefinition();
}
