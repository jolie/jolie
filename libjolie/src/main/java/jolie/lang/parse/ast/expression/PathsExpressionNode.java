package jolie.lang.parse.ast.expression;

import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.context.ParsingContext;
import java.util.List;

/**
 * Represents a PATHS expression that returns path strings matching a pattern and filter. Example:
 * res << paths tree.* where $ > 5 Returns: res.results = ["tree.a", "tree.c"] (paths where value >
 * 5)
 */
public class PathsExpressionNode extends OLSyntaxNode {
	private final List< PathOperation > operations;
	private final OLSyntaxNode whereClause;

	public PathsExpressionNode(
		ParsingContext context,
		List< PathOperation > operations,
		OLSyntaxNode whereClause ) {
		super( context );
		this.operations = operations;
		this.whereClause = whereClause;
	}

	public List< PathOperation > operations() {
		return operations;
	}

	public OLSyntaxNode whereClause() {
		return whereClause;
	}

	@Override
	public < C, R > R accept( OLVisitor< C, R > visitor, C ctx ) {
		return visitor.visit( this, ctx );
	}
}
