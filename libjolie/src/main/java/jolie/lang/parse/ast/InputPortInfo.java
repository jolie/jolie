/***************************************************************************
 *   Copyright (C) 2007-2011 by Fabrizio Montesi <famontesi@gmail.com>     *
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

import java.io.Serializable;
import java.util.Map;
import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.ast.expression.InlineTreeExpressionNode;
import jolie.lang.parse.ast.expression.ProductExpressionNode;
import jolie.lang.parse.ast.expression.SumExpressionNode;
import jolie.lang.parse.context.ParsingContext;

public class InputPortInfo extends PortInfo {
	public static class AggregationItemInfo implements Serializable {
		private final String[] outputPortList;
		private final InterfaceExtenderDefinition interfaceExtender;

		public AggregationItemInfo( String[] outputPortList, InterfaceExtenderDefinition extender ) {
			this.outputPortList = outputPortList;
			this.interfaceExtender = extender;
		}

		public String[] outputPortList() {
			return outputPortList;
		}

		public InterfaceExtenderDefinition interfaceExtender() {
			return interfaceExtender;
		}
	}

	private final OLSyntaxNode location;
	private final OLSyntaxNode protocol;
	private final AggregationItemInfo[] aggregationList;
	private final Map< String, String > redirectionMap;

	public InputPortInfo(
		ParsingContext context,
		String id,
		OLSyntaxNode location,
		OLSyntaxNode protocol,
		AggregationItemInfo[] aggregationList,
		Map< String, String > redirectionMap ) {
		super( context, id );
		this.location = location;
		this.protocol = protocol;
		this.aggregationList = aggregationList;
		this.redirectionMap = redirectionMap;
	}

	public AggregationItemInfo[] aggregationList() {
		return aggregationList;
	}

	public Map< String, String > redirectionMap() {
		return redirectionMap;
	}

	public OLSyntaxNode protocol() {
		return protocol;
	}

	public OLSyntaxNode location() {
		return location;
	}

	@Override
	public void accept( OLVisitor visitor ) {
		visitor.visit( this );
	}

	/**
	 * @return an abstract node with defines protocol id this port is considering
	 */
	public OLSyntaxNode protocolId() {
		OLSyntaxNode p = this.protocol;
		if( p instanceof SumExpressionNode ) {
			// a non optimized protocol node
			OLSyntaxNode prodVal = ((SumExpressionNode) protocol).operands().iterator().next().value();
			p = ((ProductExpressionNode) prodVal).operands().iterator().next().value();
		}

		if( p instanceof InlineTreeExpressionNode ) {
			return ((InlineTreeExpressionNode) p)
				.rootExpression();
		} else {
			return p;
		}
	}
}
