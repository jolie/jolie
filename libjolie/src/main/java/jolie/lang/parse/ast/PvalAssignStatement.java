package jolie.lang.parse.ast;

import java.util.List;

import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.ast.expression.PathOperation;
import jolie.lang.parse.context.ParsingContext;

public class PvalAssignStatement extends OLSyntaxNode {
	private final OLSyntaxNode pathExpression;
	private final List< PathOperation > pathOps;
	private final OLSyntaxNode expression;

	public PvalAssignStatement( ParsingContext context, OLSyntaxNode pathExpression,
		List< PathOperation > pathOps, OLSyntaxNode expression ) {
		super( context );
		this.pathExpression = pathExpression;
		this.pathOps = pathOps;
		this.expression = expression;
	}

	public OLSyntaxNode pathExpression() {
		return pathExpression;
	}

	public List< PathOperation > pathOperations() {
		return pathOps;
	}

	public OLSyntaxNode expression() {
		return expression;
	}

	@Override
	public < C, R > R accept( OLVisitor< C, R > visitor, C ctx ) {
		return visitor.visit( this, ctx );
	}
}
