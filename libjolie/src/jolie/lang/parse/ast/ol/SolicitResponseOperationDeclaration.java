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

package jolie.lang.parse.ast.ol;

import java.util.Collection;

import jolie.Constants;
import jolie.lang.parse.OLVisitor;


public class SolicitResponseOperationDeclaration extends OperationDeclaration
{
	private Collection< Constants.VariableType > outVarTypes, inVarTypes;
	private String boundOperationId;

	public SolicitResponseOperationDeclaration(
			String id,
			Collection< Constants.VariableType > outVarTypes,
			Collection< Constants.VariableType > inVarTypes,
			String boundOperationId
		)
	{
		super( id );
		this.outVarTypes = outVarTypes;
		this.inVarTypes = inVarTypes;
		this.boundOperationId = boundOperationId;
	}

	public Collection< Constants.VariableType > outVarTypes()
	{
		return outVarTypes;
	}
	
	public Collection< Constants.VariableType > inVarTypes()
	{
		return inVarTypes;
	}
	
	public void accept( OLVisitor visitor )
	{
		visitor.visit( this );
	}
	
	public String boundOperationId()
	{
		return boundOperationId;
	}
}
