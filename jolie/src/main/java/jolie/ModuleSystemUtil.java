/*
 * Copyright (C) 2020 Narongrit Unwerawattana <narongrit.kie@gmail.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package jolie;

import java.io.IOException;

import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.ast.VariablePathNode;
import jolie.lang.parse.ast.expression.ConstantStringExpression;
import jolie.lang.parse.ast.expression.InlineTreeExpressionNode;
import jolie.lang.parse.ast.expression.VariableExpressionNode;
import jolie.net.CommCore;

/**
 * A Utility class to make Jolie Module system backward compatible
 *
 */
public class ModuleSystemUtil {

	/**
	 * transform protocol OLSyntaxNode which declared as identifier token to an expression
	 *
	 * @param node an abstraction node defines PortInfo's protocol configuration
	 * @param commCore
	 * @return Abstract node where supported protocol identifier transform into ConstantStringExpression
	 *
	 */
	public static OLSyntaxNode transformProtocolExpression( OLSyntaxNode node, CommCore commCore ) {
		// case http -> "http" return ConstantString if content is a supported protocol, otherwise return
		// VariableExpressionNode
		// case http { .... } -> "http" {} return InlineTreeExpression with ConstantString as root
		// expression if content is a supported protocol
		// case some.proc -> do noting. return VariablePathNode
		// case "http" {} -> do noting. return InlineTreeExpression

		// case 1
		if( node instanceof VariableExpressionNode ) {
			return ModuleSystemUtil.checkAndTransformProtocolIDNode( (VariableExpressionNode) node, commCore );
		}

		// case 2 checks node is an InlineTreeExpressionNode with VariableExpressionNode as root expression
		if( node instanceof InlineTreeExpressionNode && ((InlineTreeExpressionNode) node)
			.rootExpression() instanceof VariableExpressionNode ) {

			InlineTreeExpressionNode inlineTreeNodeProtocol = (InlineTreeExpressionNode) node;
			VariableExpressionNode protocolIdVarExprNode =
				(VariableExpressionNode) inlineTreeNodeProtocol.rootExpression();

			OLSyntaxNode protocolIdNode = ModuleSystemUtil.checkAndTransformProtocolIDNode( protocolIdVarExprNode,
				commCore );

			return new InlineTreeExpressionNode( inlineTreeNodeProtocol.context(), protocolIdNode,
				inlineTreeNodeProtocol.operations() );
		}

		return node;
	}

	/**
	 * transform protocol identifier which declared as VariableExpressionNode to proper expression for
	 * parametric communication port
	 *
	 * @param protocolIdNode VariableExpressionNode defines PortInfo's protocol identifier. eg. a, http
	 *        , sodep, a.b
	 * @param commCore
	 *
	 * @return ConstantStringExpression, if the protocolId is contain value of supported protocol
	 *         identifier eg. http -> "http", sodep -> "sodep". Otherwise, the protocol identifier is
	 *         consider as a variable and return the parameter itself
	 */
	private static OLSyntaxNode checkAndTransformProtocolIDNode( VariableExpressionNode protocolIdNode,
		CommCore commCore ) {
		VariablePathNode varPathNode = protocolIdNode.variablePath();
		if( varPathNode.path().size() == 1
			&& isProtocolSupported( varPathNode.path().get( 0 ).key().toString(), commCore ) ) {
			// variable path key is a support protocol identifier, return string expression
			return new ConstantStringExpression( protocolIdNode.context(),
				varPathNode.path().get( 0 ).key().toString() );
		}
		return protocolIdNode;
	}

	/**
	 * check if protocol string is supported by attempt to retrieve communication protocol factory
	 *
	 * @param protocolId Protocol identifier
	 * @param commCore
	 *
	 * @return flag indicate if protocolId is a supported protocol
	 */
	private static boolean isProtocolSupported( String protocolId, CommCore commCore ) {
		try {
			return commCore.getCommProtocolFactory( protocolId ) != null;
		} catch( IOException e ) {
			return false;
		}
	}
}
