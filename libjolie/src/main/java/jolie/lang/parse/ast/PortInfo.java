/***************************************************************************
 *   Copyright (C) by Fabrizio Montesi                                     *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU Library General Public License as       *
 *   published by the Free Software Foundation; either version 2 of the    *
 *   License, or (at your option) any later version.                       *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU Library General Public     *
 *   License along with this program; if not, write to the                 *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 *                                                                         *
 *   For details about the authors of this software, see the AUTHORS file. *
 ***************************************************************************/

package jolie.lang.parse.ast;

import jolie.lang.parse.DocumentedNode;
import jolie.lang.parse.ast.expression.InlineTreeExpressionNode;
import jolie.lang.parse.ast.expression.ProductExpressionNode;
import jolie.lang.parse.ast.expression.SumExpressionNode;
import jolie.lang.parse.context.ParsingContext;

import java.util.*;

/**
 * Abstract class representing a port (shared between input and output ports).
 *
 * @author Fabrizio Montesi
 */
public abstract class PortInfo extends OLSyntaxNode implements OperationCollector, DocumentedNode {
	private final String id;
	private final Map< String, OperationDeclaration > operationsMap =
		new HashMap<>();
	private final List< InterfaceDefinition > interfaceList = new ArrayList<>();
	private String document;

	public PortInfo( ParsingContext context, String id ) {
		super( context );
		this.id = id;
	}

	/**
	 * Returns the name identifier of the port.
	 *
	 * @return the name identifier of the port
	 */
	public String id() {
		return id;
	}

	/**
	 * Returns the operations of the port.
	 *
	 * @return the operations of the port.
	 */
	public Collection< OperationDeclaration > operations() {
		return operationsMap.values();
	}

	/**
	 * Returns the operations of the port, mapped by their names.
	 *
	 * @return the operations of the port, mapped by their names.
	 */
	@Override
	public Map< String, OperationDeclaration > operationsMap() {
		return operationsMap;
	}

	@Override
	public void addOperation( OperationDeclaration decl ) {
		operationsMap.put( decl.id(), decl );
	}

	@Override
	public void setDocumentation( String document ) {
		this.document = document;
	}

	@Override
	public Optional< String > getDocumentation() {
		return Optional.ofNullable( document );
	}

	/**
	 * Returns the interfaces implemented by this port.
	 *
	 * @return the interfaces implemented by this port.
	 */
	public List< InterfaceDefinition > getInterfaceList() {
		return interfaceList;
	}

	public void addInterface( InterfaceDefinition iface ) {
		interfaceList.add( iface );
	}

	protected static String extractProtocolId( OLSyntaxNode protocolNode ) {
		if( protocolNode == null ) {
			return "";
		}

		OLSyntaxNode p = protocolNode;
		if( p instanceof SumExpressionNode ) {
			// a non optimized protocol node
			OLSyntaxNode prodVal = ((SumExpressionNode) p).operands().iterator().next().value();
			p = ((ProductExpressionNode) prodVal).operands().iterator().next().value();
		}

		if( p instanceof InlineTreeExpressionNode ) {
			if( ((InlineTreeExpressionNode) p).rootExpression() == null ) {
				return "";
			}
			return ((InlineTreeExpressionNode) p).rootExpression().toString();
		}

		return p.toString();
	}

}
