package jolie.lang.parse.util;

import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.ast.VariablePathNode;
import jolie.lang.parse.ast.expression.ConstantStringExpression;
import jolie.lang.parse.ast.expression.InlineTreeExpressionNode;
import jolie.lang.parse.ast.expression.VariableExpressionNode;

/**
 * A Utility class to make Jolie Module system backward compatible
 * 
 */
public class ModuleSystemUtil {

	/**
	 * transform protocol OLSyntaxNode which declared as identifier token to an expression
	 * 
	 * @param node an abstraction node defines PortInfo's protocol configuration
	 * @return Either a ConstantStringExpression or an InlineTreeExpression with
	 *         ConstantStringExpression as root expression
	 */
	public static OLSyntaxNode transformProtocolExpression( OLSyntaxNode node ) {
		// case http -> "http" return ConstantString
		// case http { .... } -> "http" {} return InlineTreeExpression with Constant string as root
		// expression
		// case some.proc -> do noting. return VariablePathNode
		// case "http" {} -> do noting. return InlineTreeExpression

		// case 1
		if( node instanceof VariableExpressionNode ) {
			VariableExpressionNode varExprNode = (VariableExpressionNode) node;
			VariablePathNode varPathNode = varExprNode.variablePath();
			if( varPathNode.path().size() == 1 ) {
				return varPathNode.path().get( 0 ).key();
			}
		}

		// case 2 checks node is an InlineTreeExpressionNode with VariableExpressionNode as root expression
		if( node instanceof InlineTreeExpressionNode && ((InlineTreeExpressionNode) node)
			.rootExpression() instanceof VariableExpressionNode ) {
			InlineTreeExpressionNode inlineTreeNodeProtocol = (InlineTreeExpressionNode) node;
			if( inlineTreeNodeProtocol.rootExpression() instanceof VariableExpressionNode ) {
				String protocolSymbolStr =
					((VariableExpressionNode) inlineTreeNodeProtocol.rootExpression())
						.toString();
				node = new InlineTreeExpressionNode( inlineTreeNodeProtocol.context(),
					new ConstantStringExpression( inlineTreeNodeProtocol.context(),
						protocolSymbolStr ),
					inlineTreeNodeProtocol.operations() );
			}
		}

		return node;
	}

}
