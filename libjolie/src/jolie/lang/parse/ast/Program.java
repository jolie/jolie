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

import java.util.Vector;

import jolie.lang.parse.OLVisitor;


public class Program implements OLSyntaxNode
{
	private Vector< OLSyntaxNode > children = new Vector< OLSyntaxNode > ();
	/*private Vector< OperationDeclaration > operationDeclarations;
	private Vector< Procedure > procedures;*/
	
	public Program()
	{
		/*operationDeclarations = new Vector< OperationDeclaration >();
		procedures = new Vector< Procedure >();*/
	}
	
	public void addChild( OLSyntaxNode node )
	{
		children.add( node );
	}
	
	public Vector< OLSyntaxNode > children()
	{
		return children;
	}
	
	/*public void addOperationDeclaration( OperationDeclaration operationDeclaration )
	{
		operationDeclarations.add( operationDeclaration );
	}*/
	
	/*public void addVariableDeclaration( VariableDeclaration variableDeclaration )
	{
		variableDeclarations.add( variableDeclaration );
	}*/
	
	/*public void addLinkDeclaration( InternalLinkDeclaration linkDeclaration )
	{
		linkDeclarations.add( linkDeclaration );
	}*/
	
	/*public void addProcedure( Procedure procedure )
	{
		procedures.add( procedure );
	}
	
	public Vector< OperationDeclaration > operationDeclarations()
	{
		return operationDeclarations;
	}*/
	
	/*public Vector< InternalLinkDeclaration > linkDeclarations()
	{
		return linkDeclarations;
	}*/
	
	/*public Collection< VariableDeclaration > variableDeclarations()
	{
		return variableDeclarations;
	}*/
	
	/*public Vector< Procedure > procedures()
	{
		return procedures;
	}*/
	
	public void accept( OLVisitor visitor )
	{
		visitor.visit( this );
	}
}
