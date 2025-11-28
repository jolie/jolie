package jolie.process;

import java.util.List;

import jolie.lang.parse.ast.expression.PathOperation;
import jolie.lang.parse.context.ParsingContext;
import jolie.runtime.PvalHelper;
import jolie.runtime.Value;
import jolie.runtime.VariablePath;
import jolie.runtime.expression.Expression;

/**
 * PVAL assignment (lvalue): assigns a value to the location specified by a path. Example:
 * pval(res.results[0]).field = "newValue"
 */
public class PvalAssignmentProcess implements Process, Expression {
	private final Expression pathExpression;
	private final List< PathOperation > pathOps;
	private final Expression expression;
	private final ParsingContext context;

	public PvalAssignmentProcess( Expression pathExpression, List< PathOperation > pathOps,
		Expression expression, ParsingContext context ) {
		this.pathExpression = pathExpression;
		this.pathOps = pathOps;
		this.expression = expression;
		this.context = context;
	}

	@Override
	public Process copy( TransformationReason reason ) {
		return new PvalAssignmentProcess(
			pathExpression.cloneExpression( reason ),
			pathOps,
			expression.cloneExpression( reason ),
			context );
	}

	@Override
	public Expression cloneExpression( TransformationReason reason ) {
		return new PvalAssignmentProcess(
			pathExpression.cloneExpression( reason ),
			pathOps,
			expression.cloneExpression( reason ),
			context );
	}

	@Override
	public void run() {
		evaluate();
	}

	@Override
	public Value evaluate() {
		VariablePath varPath = PvalHelper.resolveVariablePath( pathExpression.evaluate(), pathOps, context );
		Value val = varPath.getValue();
		val.assignValue( expression.evaluate() );
		return val;
	}

	@Override
	public boolean isKillable() {
		return true;
	}
}
