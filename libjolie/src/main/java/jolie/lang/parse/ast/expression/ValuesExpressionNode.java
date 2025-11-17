package jolie.lang.parse.ast.expression;

import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.context.ParsingContext;
import java.util.List;

/**
 * Represents a VALUES expression that returns actual values matching a pattern and filter. Example:
 * res << values tree.* where $ > 5 Returns: res.results = [10, 7] (actual values where value > 5)
 */
public class ValuesExpressionNode extends OLSyntaxNode {
	private final List< PathOperation > operations;
	private final OLSyntaxNode whereClause;

	public ValuesExpressionNode(
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
