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
import java.util.Vector;

import jolie.lang.parse.OLVisitor;


public class Program implements OLSyntaxNode
{
	private Collection< LocationDeclaration > locationDeclarations;
	private Collection< OperationDeclaration > operationDeclarations;
	private Collection< InternalLinkDeclaration > linkDeclarations;
	private Collection< VariableDeclaration > variableDeclarations;
	private Collection< Procedure > procedures;
	
	public Program()
	{
		locationDeclarations = new Vector< LocationDeclaration >();
		operationDeclarations = new Vector< OperationDeclaration >();
		linkDeclarations = new Vector< InternalLinkDeclaration >();
		variableDeclarations = new Vector< VariableDeclaration >();
		procedures = new Vector< Procedure >();
	}
	
	public void addLocationDeclaration( LocationDeclaration locationDeclaration )
	{
		locationDeclarations.add( locationDeclaration );
	}
	
	public void addOperationDeclaration( OperationDeclaration operationDeclaration )
	{
		operationDeclarations.add( operationDeclaration );
	}
	
	public void addVariableDeclaration( VariableDeclaration variableDeclaration )
	{
		variableDeclarations.add( variableDeclaration );
	}
	
	public void addLinkDeclaration( InternalLinkDeclaration linkDeclaration )
	{
		linkDeclarations.add( linkDeclaration );
	}
	
	public void addProcedure( Procedure procedure )
	{
		procedures.add( procedure );
	}
	
	public Program(
			 	Collection< LocationDeclaration > locationDeclarations,
			 	Collection< OperationDeclaration > operationDeclarations,
			 	Collection< InternalLinkDeclaration > linkDeclarations,
			 	Collection< VariableDeclaration > variableDeclarations,
			 	Collection< Procedure > procedures
			 )
	{
		this.locationDeclarations = locationDeclarations;
		this.operationDeclarations = operationDeclarations;
		this.linkDeclarations = linkDeclarations;
		this.variableDeclarations = variableDeclarations;
		this.procedures = procedures;
	}
	
	public Collection< LocationDeclaration > locationDeclarations()
	{
		return locationDeclarations;
	}
	
	public Collection< OperationDeclaration > operationDeclarations()
	{
		return operationDeclarations;
	}
	
	public Collection< InternalLinkDeclaration > linkDeclarations()
	{
		return linkDeclarations;
	}
	
	public Collection< VariableDeclaration > variableDeclarations()
	{
		return variableDeclarations;
	}
	
	public Collection< Procedure > procedures()
	{
		return procedures;
	}
	
	public void accept( OLVisitor visitor )
	{
		visitor.visit( this );
	}
}
