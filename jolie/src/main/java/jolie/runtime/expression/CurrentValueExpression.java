package jolie.runtime.expression;

import jolie.lang.parse.ast.expression.PathOperation;
import jolie.process.TransformationReason;
import jolie.runtime.Value;
import java.util.List;
import java.util.Objects;

/**
 * Represents $ with optional path navigation. Returns single value or multi-values (in .results)
 * for existential evaluation.
 */
public class CurrentValueExpression implements Expression {
	public Candidate currentCandidate;
	public final List< PathOperation > operations;

	public CurrentValueExpression( List< PathOperation > operations ) {
		this.operations = operations;
	}

	public void setCurrentCandidate( Candidate candidate ) {
		this.currentCandidate = Objects.requireNonNull( candidate, "$ cannot be null" );
	}

	@Override
	public Expression cloneExpression( TransformationReason reason ) {
		return new CurrentValueExpression( operations );
	}

	@Override
	public Value evaluate() {
		return currentCandidate.value();
	}
}
