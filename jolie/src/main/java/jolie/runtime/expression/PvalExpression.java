package jolie.runtime.expression;

import java.util.List;

import jolie.lang.parse.ast.expression.PathOperation;
import jolie.lang.parse.context.ParsingContext;
import jolie.process.TransformationReason;
import jolie.runtime.PvalHelper;
import jolie.runtime.Value;
import jolie.runtime.VariablePath;

/**
 * PVAL expression (rvalue): dereferences a path value to get a reference to the actual value.
 * Example: pval(res.results[0]).field
 */
public class PvalExpression implements Expression {
	private final Expression pathExpression;
	private final List< PathOperation > pathOps;
	private final ParsingContext context;

	public PvalExpression( Expression pathExpression, List< PathOperation > pathOps, ParsingContext context ) {
		this.pathExpression = pathExpression;
		this.pathOps = pathOps;
		this.context = context;
	}

	@Override
	public Expression cloneExpression( TransformationReason reason ) {
		return new PvalExpression( pathExpression.cloneExpression( reason ), pathOps, context );
	}

	@Override
	public Value evaluate() {
		VariablePath fullPath = PvalHelper.resolveVariablePath( pathExpression.evaluate(), pathOps, context );
		return Value.createLink( fullPath );
	}
}
