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

import java.io.Serializable;

import jolie.lang.Constants;
import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.context.ParsingContext;
import jolie.util.Unit;

public abstract class OLSyntaxNode implements Serializable {
	private static final long serialVersionUID = Constants.serialVersionUID();

	private final ParsingContext context;

	public OLSyntaxNode( ParsingContext context ) {
		this.context = context;
	}

	public ParsingContext context() {
		return context;
	}

	abstract public < C, R > R accept( OLVisitor< C, R > v, C ctx );

	public < R > R accept( OLVisitor< Unit, R > v ) {
		return accept( v, Unit.INSTANCE );
	}
}
