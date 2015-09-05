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

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import jolie.lang.Constants;
import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.ast.InterfaceDefinition;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.ast.VariablePathNode;
import jolie.lang.parse.context.ParsingContext;

/**
 * 
 * @author Fabrizio Montesi
 */
public class CourierChoiceStatement extends OLSyntaxNode
{
	public static class InterfaceOneWayBranch implements Serializable {
		private static final long serialVersionUID = Constants.serialVersionUID();
		public final InterfaceDefinition interfaceDefinition;
		public final VariablePathNode inputVariablePath;
		public final OLSyntaxNode body;
		public InterfaceOneWayBranch(
			InterfaceDefinition interfaceDefinition,
			VariablePathNode inputVariablePath,
			OLSyntaxNode body )
		{
			this.interfaceDefinition = interfaceDefinition;
			this.inputVariablePath = inputVariablePath;
			this.body = body;
		}
	}
	
	public static class InterfaceRequestResponseBranch implements Serializable {
		private static final long serialVersionUID = Constants.serialVersionUID();
		public final InterfaceDefinition interfaceDefinition;
		public final VariablePathNode inputVariablePath;
		public final VariablePathNode outputVariablePath;
		public final OLSyntaxNode body;
		public InterfaceRequestResponseBranch(
			InterfaceDefinition interfaceDefinition,
			VariablePathNode inputVariablePath,
			VariablePathNode outputVariablePath,
			OLSyntaxNode body )
		{
			this.interfaceDefinition = interfaceDefinition;
			this.inputVariablePath = inputVariablePath;
			this.outputVariablePath = outputVariablePath;
			this.body = body;
		}
	}
	
	public static class OperationOneWayBranch implements Serializable {
		private static final long serialVersionUID = Constants.serialVersionUID();
		public final String operation;
		public final VariablePathNode inputVariablePath;
		public final OLSyntaxNode body;
		public OperationOneWayBranch(
			String operation,
			VariablePathNode inputVariablePath,
			OLSyntaxNode body )
		{
			this.operation = operation;
			this.inputVariablePath = inputVariablePath;
			this.body = body;
		}
	}
	
	public static class OperationRequestResponseBranch implements Serializable {
		private static final long serialVersionUID = Constants.serialVersionUID();
		public final String operation;
		public final VariablePathNode inputVariablePath;
		public final VariablePathNode outputVariablePath;
		public final OLSyntaxNode body;
		public OperationRequestResponseBranch(
			String operation,
			VariablePathNode inputVariablePath,
			VariablePathNode outputVariablePath,
			OLSyntaxNode body )
		{
			this.operation = operation;
			this.inputVariablePath = inputVariablePath;
			this.outputVariablePath = outputVariablePath;
			this.body = body;
		}
	}
	
	private final List< InterfaceOneWayBranch > interfaceOneWayBranches =
		new LinkedList<>();
	private final List< InterfaceRequestResponseBranch > interfaceRequestResponseBranches =
		new LinkedList<>();
	private final List< OperationOneWayBranch > operationOneWayBranches =
		new LinkedList<>();
	private final List< OperationRequestResponseBranch > operationRequestResponseBranches =
		new LinkedList<>();

	public CourierChoiceStatement( ParsingContext context )
	{
		super( context );
	}
	
	public List< InterfaceOneWayBranch > interfaceOneWayBranches()
	{
		return interfaceOneWayBranches;
	}
	
	public List< InterfaceRequestResponseBranch > interfaceRequestResponseBranches()
	{
		return interfaceRequestResponseBranches;
	}
	
	public List< OperationOneWayBranch > operationOneWayBranches()
	{
		return operationOneWayBranches;
	}
	
	public List< OperationRequestResponseBranch > operationRequestResponseBranches()
	{
		return operationRequestResponseBranches;
	}
	
	@Override
	public void accept( OLVisitor visitor )
	{
		visitor.visit( this );
	}
}
