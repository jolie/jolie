package jolie.lang.parse.ast.expression;

import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.context.ParsingContext;
import java.util.List;

/**
 * Represents the current value ($) in PATHS/VALUES WHERE expressions with optional path navigation.
 * Examples: - $ > 5 (just the current value) - $.field > 10 (navigate to field) - $.*[*] > 0
 * (navigate with wildcards)
 */
public class CurrentValueNode extends OLSyntaxNode {
	private final List< PathOperation > operations;

	public CurrentValueNode( ParsingContext context, List< PathOperation > operations ) {
		super( context );
		this.operations = operations;
	}

	public List< PathOperation > operations() {
		return operations;
	}

	@Override
	public < C, R > R accept( OLVisitor< C, R > visitor, C ctx ) {
		return visitor.visit( this, ctx );
	}
}
