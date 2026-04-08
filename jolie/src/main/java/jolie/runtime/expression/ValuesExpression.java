package jolie.runtime.expression;

import jolie.lang.parse.ast.expression.PathOperation;
import jolie.process.TransformationReason;
import jolie.runtime.Value;
import jolie.runtime.VariablePath;
import java.util.List;

/**
 * VALUES expression: returns actual values matching pattern and WHERE filter. Example: values
 * data[*] where $ > 5 → [10, 7] (the actual values)
 */
public class ValuesExpression implements Expression {
	private final VariablePath path;
	private final List< PathOperation > ops;
	private final Expression where;

	public ValuesExpression(
		VariablePath path,
		List< PathOperation > ops,
		Expression where ) {
		this.path = path;
		this.ops = ops;
		this.where = where;
	}

	@Override
	public Expression cloneExpression( TransformationReason reason ) {
		return new ValuesExpression(
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
				result.getChildren( "results" ).get( index ).deepCopy( c.value() );
			}
		}
		return result;
	}
}
