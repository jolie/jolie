package jolie.lang.parse.ast.expression;

import java.util.Optional;

import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.ast.InstallFunctionNode;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.context.ParsingContext;

public class SolicitResponseExpressionNode extends OLSyntaxNode {
	private final String id, outputPortId;
	private final OLSyntaxNode outputExpression;
	//private final InstallFunctionNode handlersFunction;

	//TODO handle if id is null
	public SolicitResponseExpressionNode( ParsingContext context, String id, String outputPortId,
		OLSyntaxNode outputExpression ) {
		super( context );
		this.id = id;
		this.outputPortId = outputPortId;
		this.outputExpression = outputExpression;
		//this.handlersFunction = handlersFunction.orElse( null );
	}

	/*public InstallFunctionNode handlerFunction() {
		return handlersFunction;
	}*/

	public String id() {
		return id;
	}

	public String outputPortId() {
		return outputPortId;
	}

	public OLSyntaxNode outputExpression() {
		return outputExpression;
	}

	@Override
	public < C, R > R accept( OLVisitor< C, R > visitor, C ctx ) {
		return visitor.visit( this, ctx );
	}

}
