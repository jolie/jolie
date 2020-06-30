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

import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.ast.expression.InlineTreeExpressionNode;
import jolie.lang.parse.ast.expression.ProductExpressionNode;
import jolie.lang.parse.ast.expression.SumExpressionNode;
import jolie.lang.parse.context.ParsingContext;

public class OutputPortInfo extends PortInfo {

	private OLSyntaxNode protocol = null;
	private OLSyntaxNode location = null;

	public OutputPortInfo( ParsingContext context, String id ) {
		super( context, id );
	}

	public void setProtocol( OLSyntaxNode protocol ) {
		this.protocol = protocol;
	}

	public void setLocation( OLSyntaxNode location ) {
		this.location = location;
	}

	@Override
	public void accept( OLVisitor visitor ) {
		visitor.visit( this );
	}

	public OLSyntaxNode protocol() {
		return protocol;
	}

	/**
	 * @return an abstract node with defines protocol id this port is considering
	 */
	public OLSyntaxNode protocolId() {
		OLSyntaxNode p = this.protocol;
		if( p instanceof SumExpressionNode ) {
			// a non optimized protocol node
			OLSyntaxNode prodNode = ((SumExpressionNode) protocol).operands().iterator().next().value();
			p = ((ProductExpressionNode) prodNode).operands().iterator().next().value();
		}

		if( p instanceof InlineTreeExpressionNode ) {
			return ((InlineTreeExpressionNode) p).rootExpression();
		} else {
			return p;
		}
	}

	public OLSyntaxNode location() {
		return location;
	}
}
