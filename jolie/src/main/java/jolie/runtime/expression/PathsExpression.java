package jolie.runtime.expression;

import jolie.lang.parse.ast.expression.PathOperation;
import jolie.process.TransformationReason;
import jolie.runtime.Value;
import jolie.runtime.VariablePath;
import java.util.List;

/**
 * PATHS expression: returns path strings matching pattern and WHERE filter. Example: paths data[*]
 * where $ > 5 → ["data[0]", "data[2]"]
 */
public class PathsExpression implements Expression {
	private final VariablePath path;
	private final List< PathOperation > ops;
	private final Expression where;

	public PathsExpression(
		VariablePath path,
		List< PathOperation > ops,
		Expression where ) {
		this.path = path;
		this.ops = ops;
		this.where = where;
	}

	@Override
	public Expression cloneExpression( TransformationReason reason ) {
		return new PathsExpression(
			(VariablePath) path.cloneExpression( reason ),
			ops,
			where.cloneExpression( reason ) );
	}

	@Override
	public Value evaluate() {
		Value result = Value.create();
		for( Candidate c : ValueNavigator.navigate( path, ops ) ) {
			if( WhereEvaluator.matches( where, c ) ) {
				int index = result.getChildren( "results" ).size();
				result.getChildren( "results" ).get( index ).setValue( c.path() );
			}
		}
		return result;
	}
}
