package jolie.lang.parse.ast.expression;

import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.context.ParsingContext;

import java.util.List;

/**
 * Represents a pval expression that dereferences a path value to get a reference to the actual
 * value. Example: pval(res.results[0]).field The path expression must evaluate to a path type.
 * Optional path operations can be applied to navigate the result (e.g., .field, [0]).
 */
public class PvalExpressionNode extends OLSyntaxNode {
	private final OLSyntaxNode pathExpression;
	private final List< PathOperation > pathOps;

	public PvalExpressionNode( ParsingContext context, OLSyntaxNode pathExpression,
		List< PathOperation > pathOps ) {
		super( context );
		this.pathExpression = pathExpression;
		this.pathOps = pathOps;
	}

	public OLSyntaxNode pathExpression() {
		return pathExpression;
	}

	public List< PathOperation > pathOperations() {
		return pathOps;
	}

	@Override
	public < C, R > R accept( OLVisitor< C, R > visitor, C ctx ) {
		return visitor.visit( this, ctx );
	}
}
