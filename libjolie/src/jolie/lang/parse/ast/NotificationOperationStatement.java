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
import jolie.lang.parse.ParsingContext;


public class NotificationOperationStatement extends OLSyntaxNode
{
	private VariablePathNode outputVarPath;
	private String id;
	private OLSyntaxNode locationExpression;
	
	public NotificationOperationStatement(
			ParsingContext context,
			String id,
			OLSyntaxNode locationExpression,
			VariablePathNode outputVarPath
			)
	{
		super( context );
		this.id = id;
		this.outputVarPath = outputVarPath;
		this.locationExpression = locationExpression;
	}
	
	public String id()
	{
		return id;
	}
	
	public OLSyntaxNode locationExpression()
	{
		return locationExpression;
	}
	
	public VariablePathNode outputVarPath()
	{
		return outputVarPath;
	}
	
	public void accept( OLVisitor visitor )
	{
		visitor.visit( this );
	}
}
