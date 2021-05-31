package jolie.lang.parse.ast;

import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.context.ParsingContext;

public class SolicitResponseExpression extends OLSyntaxNode {
	private final String operationId, outputPortId;
	private final OLSyntaxNode expression;

	public SolicitResponseExpression(
		ParsingContext context,
		String operationId,
		String outputPortId,
		OLSyntaxNode expression ) {
		super( context );
		this.operationId = operationId;
		this.outputPortId = outputPortId;
		this.expression = expression;
	}

	public String operationId() {
		return operationId;
	}

	public String outputPortId() {
		return outputPortId;
	}

	public OLSyntaxNode expression() {
		return expression;
	}

	@Override
	public < C, R > R accept( OLVisitor< C, R > visitor, C ctx ) {
		return visitor.visit( this, ctx );
	}
}
