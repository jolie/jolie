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


public class SolicitResponseOperationStatement implements OLSyntaxNode
{
	private VariablePath outputVarPath, inputVarPath;
	private String id;
	private OLSyntaxNode locationExpression;
	
	public SolicitResponseOperationStatement(
			String id,
			OLSyntaxNode locationExpression,
			VariablePath outputVarPath,
			VariablePath inputVarPath
		)
	{
		this.id = id;
		this.outputVarPath = outputVarPath;
		this.inputVarPath = inputVarPath;
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
	
	public VariablePath outputVarPath()
	{
		return outputVarPath;
	}
	
	public VariablePath inputVarPath()
	{
		return inputVarPath;
	}
	
	public void accept( OLVisitor visitor )
	{
		visitor.visit( this );
	}
}
