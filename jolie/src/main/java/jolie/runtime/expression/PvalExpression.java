package jolie.runtime.expression;

import jolie.lang.parse.ast.expression.PathOperation;
import jolie.lang.parse.context.ParsingContext;
import jolie.process.TransformationReason;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.VariablePath;
import jolie.runtime.VariablePathBuilder;

import java.util.List;

/**
 * PVAL expression: dereferences a path value to get a reference to the actual value. Example:
 * pval(res.results[0]).field The path expression must evaluate to a path type. Optional path
 * operations extend the base path.
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
		Value pathValue = pathExpression.evaluate();

		// pval requires a path type value
		if( !pathValue.isPath() ) {
			throw new FaultException( "TypeMismatch",
				"pval requires a path type value, got: " + pathValue.strValue(), context )
				.toRuntimeFaultException();
		}

		String pathStr = pathValue.pathValue().getPath();

		// Extend path string with additional operations
		String fullPathStr = pathStr + pathOpsToString( pathOps );

		// Convert to VariablePath and return link
		VariablePath fullPath = parsePathString( fullPathStr );
		return Value.createLink( fullPath );
	}

	/**
	 * Converts path operations to a path string suffix (e.g., ".child[0].name").
	 */
	private String pathOpsToString( List< PathOperation > ops ) {
		StringBuilder sb = new StringBuilder();
		for( PathOperation op : ops ) {
			switch( op ) {
			case PathOperation.Field(String name) -> sb.append( "." ).append( name );
			case PathOperation.ArrayIndex(var idx) -> sb.append( "[" ).append( idx.intValue() ).append( "]" );
			default -> throw new IllegalArgumentException( "Unsupported path operation in pval: " + op );
			}
		}
		return sb.toString();
	}

	/**
	 * Parses a path string like "data[1].field[2].subfield" into a VariablePath.
	 */
	private VariablePath parsePathString( String pathStr ) {
		VariablePathBuilder b = new VariablePathBuilder( false );
		for( String s : pathStr.split( "\\." ) ) {
			int i = s.indexOf( '[' );
			if( i < 0 )
				b.add( s );
			else
				b.add( s.substring( 0, i ), Integer.parseInt( s.substring( i + 1, s.length() - 1 ) ) );
		}
		return b.toVariablePath();
	}
}
