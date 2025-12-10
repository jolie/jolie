package jolie.runtime;

import java.util.List;

import jolie.lang.parse.ast.expression.PathOperation;
import jolie.lang.parse.context.ParsingContext;

/**
 * Shared utilities for pval operations (both rvalue and lvalue).
 */
public final class PvalHelper {
	private PvalHelper() {}

	/**
	 * Resolves a path value and optional path operations to a VariablePath. Throws TypeMismatch fault
	 * if the value is not a path type.
	 */
	public static VariablePath resolveVariablePath( Value value, List< PathOperation > pathOps,
		ParsingContext context ) {
		if( !value.isPath() ) {
			throw new FaultException( "TypeMismatch",
				"pval requires a path type value, got: " + value.strValue(), context )
				.toRuntimeFaultException();
		}
		String pathStr = value.pathValue().getPath();
		String fullPathStr = pathStr + pathOpsToString( pathOps );
		return parsePathString( fullPathStr );
	}

	/**
	 * Converts path operations to a string suffix (e.g., ".child[0].name").
	 */
	private static String pathOpsToString( List< PathOperation > ops ) {
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
	private static VariablePath parsePathString( String pathStr ) {
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
