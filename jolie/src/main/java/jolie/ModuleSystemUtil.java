package jolie;

import java.io.IOException;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.ast.VariablePathNode;
import jolie.lang.parse.ast.expression.ConstantStringExpression;
import jolie.lang.parse.ast.expression.VariableExpressionNode;
import jolie.net.CommCore;

/**
 * A Utility class to make Jolie Module system backward compatible
 * 
 */
public class ModuleSystemUtil {

	/**
	 * check if protocol string is supported by attempt to retrieve communication protocol factory
	 */
	private static boolean isProtocolSupported( String protocolId, CommCore commCore ) {
		try {
			return commCore.getCommProtocolFactory( protocolId ) != null ? true : false;
		} catch( IOException e ) {
			return false;
		}
	}

	/**
	 * transform protocol identifier which declared as VariableExpressionNode to proper expression for
	 * parametric communication port
	 * 
	 * @param protocolId abstraction node defines PortInfo's protocol identifier. eg. a, http , "http",
	 *        sodep, a.b
	 * @param commCore
	 * 
	 * @return ConstantStringExpression, if the protocolId is contain value of supported protocol
	 *         identifier eg. http -> "http", sodep -> "sodep". Otherwise, the protocol identifier is
	 *         consider as a variable and return the parameter itself
	 */
	public static OLSyntaxNode transformProtocolId( OLSyntaxNode protocolId, CommCore commCore ) {
		if( protocolId instanceof VariableExpressionNode ) {
			VariablePathNode varPathNode = ((VariableExpressionNode) protocolId).variablePath();
			if( varPathNode.path().size() == 1
				&& isProtocolSupported( varPathNode.path().get( 0 ).key().toString(), commCore ) ) {
				// variable path key is a support protocol identifier, return string expression
				return new ConstantStringExpression( protocolId.context(),
					varPathNode.path().get( 0 ).key().toString() );
			}
		}
		return protocolId;
	}

}
