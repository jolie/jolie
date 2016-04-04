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

package jolie.lang.parse.ast.courier;

import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.ast.VariablePathNode;
import jolie.lang.parse.context.ParsingContext;

/**
 * 
 * @author Fabrizio Montesi
 */
public class SolicitResponseForwardStatement extends OLSyntaxNode
{
	private final String outputPortName;
	private final VariablePathNode outputVariablePath;
	private final VariablePathNode inputVariablePath;
	
	public SolicitResponseForwardStatement(
			ParsingContext context,
			String outputPortName,
			VariablePathNode outputVariablePath,
			VariablePathNode inputVariablePath
	) {
		super( context );
		this.outputPortName = outputPortName;
		this.outputVariablePath = outputVariablePath;
		this.inputVariablePath = inputVariablePath;
	}
	
	public String outputPortName()
	{
		return outputPortName;
	}
	
	public VariablePathNode outputVariablePath()
	{
		return outputVariablePath;
	}
	
	public VariablePathNode inputVariablePath()
	{
		return inputVariablePath;
	}
	
	@Override
	public void accept( OLVisitor visitor )
	{
		visitor.visit( this );
	}
}
