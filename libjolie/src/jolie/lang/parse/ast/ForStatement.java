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
import jolie.lang.parse.context.ParsingContext;


public class ForStatement extends OLSyntaxNode
{
	private final OLSyntaxNode init, condition, post, body;

	public ForStatement(
			ParsingContext context,
			OLSyntaxNode init,
			OLSyntaxNode condition,
			OLSyntaxNode post,
			OLSyntaxNode body
			)
	{
		super( context );
		this.init = init;
		this.condition = condition;
		this.post = post;
		this.body = body;
	}
	
	public OLSyntaxNode init()
	{
		return init;
	}
	
	public OLSyntaxNode condition()
	{
		return condition;
	}
	
	public OLSyntaxNode post()
	{
		return post;
	}
	
	public OLSyntaxNode body()
	{
		return body;
	}
	
	@Override
	public void accept( OLVisitor visitor )
	{
		visitor.visit( this );
	}
}
