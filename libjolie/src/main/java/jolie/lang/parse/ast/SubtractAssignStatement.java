package jolie.lang.parse.ast;

import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.context.URIParsingContext;

/**
 * @author Karoly Szanto
 */
public class SubtractAssignStatement extends OLSyntaxNode {
	private final VariablePathNode variablePath;
	private final OLSyntaxNode expression;

	public SubtractAssignStatement( URIParsingContext context, VariablePathNode path, OLSyntaxNode expression ) {
		super( context );
		this.variablePath = path;
		this.expression = expression;
	}

	public VariablePathNode variablePath() {
		return variablePath;
	}

	public OLSyntaxNode expression() {
		return expression;
	}

	@Override
	public < C, R > R accept( OLVisitor< C, R > visitor, C ctx ) {
		return visitor.visit( this, ctx );
	}
}
