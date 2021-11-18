/***************************************************************************
 *   Copyright (C) 2011 by Fabrizio Montesi <famontesi@gmail.com>          *
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
import jolie.lang.parse.context.URIParsingContext;

/**
 *
 * @author Fabrizio Montesi
 */
public class InterfaceExtenderDefinition extends InterfaceDefinition {
	private OneWayOperationDeclaration defaultOneWayOperation = null;
	private RequestResponseOperationDeclaration defaultRequestResponseOperation = null;

	public InterfaceExtenderDefinition( URIParsingContext context, String name, AccessModifier accessModifier ) {
		super( context, name, accessModifier );
	}

	@Override
	public < C, R > R accept( OLVisitor< C, R > visitor, C ctx ) {
		return visitor.visit( this, ctx );
	}

	public OneWayOperationDeclaration defaultOneWayOperation() {
		return defaultOneWayOperation;
	}

	public RequestResponseOperationDeclaration defaultRequestResponseOperation() {
		return defaultRequestResponseOperation;
	}

	public void setDefaultOneWayOperation( OneWayOperationDeclaration owDecl ) {
		defaultOneWayOperation = owDecl;
	}

	public void setDefaultRequestResponseOperation( RequestResponseOperationDeclaration rrDecl ) {
		defaultRequestResponseOperation = rrDecl;
	}
}
