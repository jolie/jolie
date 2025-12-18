package jolie.lang.parse.ast;

import java.util.List;
import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.ast.expression.PathOperation;
import jolie.lang.parse.context.ParsingContext;

public class PvalDeepCopyStatement extends OLSyntaxNode {
	private final OLSyntaxNode pathExpression;
	private final List< PathOperation > pathOps;
	private final OLSyntaxNode rightExpression;

	public PvalDeepCopyStatement( ParsingContext context, OLSyntaxNode pathExpression,
		List< PathOperation > pathOps, OLSyntaxNode rightExpression ) {
		super( context );
		this.pathExpression = pathExpression;
		this.pathOps = pathOps;
		this.rightExpression = rightExpression;
	}

	public OLSyntaxNode pathExpression() {
		return pathExpression;
	}

	public List< PathOperation > pathOperations() {
		return pathOps;
	}

	public OLSyntaxNode rightExpression() {
		return rightExpression;
	}

	@Override
	public < C, R > R accept( OLVisitor< C, R > visitor, C ctx ) {
		return visitor.visit( this, ctx );
	}
}
