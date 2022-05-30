/****************************************************************************
 *   Copyright (C) 2022 by Mathias Christensen <mathi.christensen@gmail.com>*
 *                                                                          *
 *   This program is free software; you can redistribute it and/or modify   *
 *   it under the terms of the GNU Library General Public License as        *
 *   published by the Free Software Foundation; either version 2 of the     *
 *   License, or (at your option) any later version.                        *
 *                                                                          *
 *   This program is distributed in the hope that it will be useful,        *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of         *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the          *
 *   GNU General Public License for more details.                           *
 *                                                                          *
 *   You should have received a copy of the GNU Library General Public      *
 *   License along with this program; if not, write to the                  *
 *   Free Software Foundation, Inc.,                                        *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.              *
 *                                                                          *
 *   For details about the authors of this software, see the AUTHORS file.  *
 ***************************************************************************/
package jolie.lang.parse.ast.expression;

import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.context.ParsingContext;

public class SolicitResponseExpressionNode extends OLSyntaxNode {
	private final String id;
	private final String outputPortId;
	private final OLSyntaxNode outputExpression;

	public SolicitResponseExpressionNode(
		ParsingContext context,
		String id,
		String outputPortId,
		OLSyntaxNode outputExpression ) {
		super( context );
		this.id = id;
		this.outputPortId = outputPortId;
		this.outputExpression = outputExpression;
	}

	public String id() {
		return id;
	}

	public String outputPortId() {
		return outputPortId;
	}

	public OLSyntaxNode outputExpression() {
		return outputExpression;
	}

	@Override
	public < C, R > R accept( OLVisitor< C, R > visitor, C ctx ) {
		return visitor.visit( this, ctx );
	}

}
