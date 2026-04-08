package jolie.process;

import java.util.List;
import jolie.lang.parse.ast.expression.PathOperation;
import jolie.lang.parse.context.ParsingContext;
import jolie.runtime.PvalHelper;
import jolie.runtime.Value;
import jolie.runtime.VariablePath;
import jolie.runtime.expression.Expression;

public class PvalDeepCopyProcess implements Process {
	private final Expression pathExpression;
	private final List< PathOperation > pathOps;
	private final Expression rightExpression;
	private final ParsingContext context;

	public PvalDeepCopyProcess( Expression pathExpression, List< PathOperation > pathOps,
		Expression rightExpression, ParsingContext context ) {
		this.pathExpression = pathExpression;
		this.pathOps = pathOps;
		this.rightExpression = rightExpression;
		this.context = context;
	}

	@Override
	public Process copy( TransformationReason reason ) {
		return new PvalDeepCopyProcess(
			pathExpression.cloneExpression( reason ),
			pathOps,
			rightExpression.cloneExpression( reason ),
			context );
	}

	@Override
	public void run() {
		VariablePath leftPath = PvalHelper.resolveVariablePath( pathExpression.evaluate(), pathOps, context );
		if( rightExpression instanceof VariablePath ) {
			leftPath.deepCopy( (VariablePath) rightExpression );
		} else {
			Value targetValue = leftPath.getValue();
			targetValue.deepCopy( rightExpression.evaluate() );
		}
	}

	@Override
	public boolean isKillable() {
		return true;
	}
}
