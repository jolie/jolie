package jolie.lang.parse.ast;

import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.context.ParsingContext;

/**
 * @author Karoly Szanto
 */
public class SubtractAssignStatement extends OLSyntaxNode
{
	private final VariablePathNode variablePath;
	private final OLSyntaxNode expression;

	public SubtractAssignStatement( ParsingContext context, VariablePathNode path, OLSyntaxNode expression )
	{
		super( context );
		this.variablePath = path;
		this.expression = expression;
	}

	public VariablePathNode variablePath()
	{
		return variablePath;
	}

	public OLSyntaxNode expression()
	{
		return expression;
	}

	@Override
	public void accept( OLVisitor visitor )
	{
		visitor.visit( this );
	}
}
