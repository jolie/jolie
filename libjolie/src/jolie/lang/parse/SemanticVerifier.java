/***************************************************************************
 *   Copyright (C) by Fabrizio Montesi                                     *
 *   Copyright (C) by Claudio Guidi										   *
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

package jolie.lang.parse;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import jolie.Constants;
import jolie.lang.parse.ast.AndConditionNode;
import jolie.lang.parse.ast.AssignStatement;
import jolie.lang.parse.ast.CompareConditionNode;
import jolie.lang.parse.ast.CompensateStatement;
import jolie.lang.parse.ast.ConstantIntegerExpression;
import jolie.lang.parse.ast.ConstantRealExpression;
import jolie.lang.parse.ast.ConstantStringExpression;
import jolie.lang.parse.ast.CorrelationSetInfo;
import jolie.lang.parse.ast.CurrentHandlerStatement;
import jolie.lang.parse.ast.DeepCopyStatement;
import jolie.lang.parse.ast.EmbeddedServiceNode;
import jolie.lang.parse.ast.ExecutionInfo;
import jolie.lang.parse.ast.ExitStatement;
import jolie.lang.parse.ast.ExpressionConditionNode;
import jolie.lang.parse.ast.ForEachStatement;
import jolie.lang.parse.ast.ForStatement;
import jolie.lang.parse.ast.IfStatement;
import jolie.lang.parse.ast.InputPortTypeInfo;
import jolie.lang.parse.ast.InstallStatement;
import jolie.lang.parse.ast.IsTypeExpressionNode;
import jolie.lang.parse.ast.LinkInStatement;
import jolie.lang.parse.ast.LinkOutStatement;
import jolie.lang.parse.ast.NDChoiceStatement;
import jolie.lang.parse.ast.NotConditionNode;
import jolie.lang.parse.ast.NotificationOperationDeclaration;
import jolie.lang.parse.ast.NotificationOperationStatement;
import jolie.lang.parse.ast.NullProcessStatement;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.ast.OneWayOperationDeclaration;
import jolie.lang.parse.ast.OneWayOperationStatement;
import jolie.lang.parse.ast.OperationDeclaration;
import jolie.lang.parse.ast.OrConditionNode;
import jolie.lang.parse.ast.OutputPortTypeInfo;
import jolie.lang.parse.ast.ParallelStatement;
import jolie.lang.parse.ast.PointerStatement;
import jolie.lang.parse.ast.PortInfo;
import jolie.lang.parse.ast.PostDecrementStatement;
import jolie.lang.parse.ast.PostIncrementStatement;
import jolie.lang.parse.ast.PreDecrementStatement;
import jolie.lang.parse.ast.PreIncrementStatement;
import jolie.lang.parse.ast.Procedure;
import jolie.lang.parse.ast.ProcedureCallStatement;
import jolie.lang.parse.ast.ProductExpressionNode;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.ast.RequestResponseOperationDeclaration;
import jolie.lang.parse.ast.RequestResponseOperationStatement;
import jolie.lang.parse.ast.RunStatement;
import jolie.lang.parse.ast.Scope;
import jolie.lang.parse.ast.SequenceStatement;
import jolie.lang.parse.ast.ServiceInfo;
import jolie.lang.parse.ast.SolicitResponseOperationDeclaration;
import jolie.lang.parse.ast.SolicitResponseOperationStatement;
import jolie.lang.parse.ast.SumExpressionNode;
import jolie.lang.parse.ast.SynchronizedStatement;
import jolie.lang.parse.ast.ThrowStatement;
import jolie.lang.parse.ast.TypeCastExpressionNode;
import jolie.lang.parse.ast.UndefStatement;
import jolie.lang.parse.ast.ValueVectorSizeExpressionNode;
import jolie.lang.parse.ast.VariableExpressionNode;
import jolie.lang.parse.ast.WhileStatement;
import jolie.util.Pair;

public class SemanticVerifier implements OLVisitor
{
	private Program program;
	private boolean valid = true;
	
	private Set< String > inputPortTypes = new HashSet< String >();
	private Set< String > outputPortTypes = new HashSet< String >();
	private HashMap< String, PortInfo > ports = new HashMap< String, PortInfo >();
	
	private Set< String > procedureNames = new HashSet< String > ();
	private HashMap< String, OperationDeclaration > operations = 
						new HashMap< String, OperationDeclaration >();
	private boolean mainDefined = false;
	
	private final Logger logger = Logger.getLogger( "JOLIE" );
	
	public SemanticVerifier( Program program )
	{
		this.program = program;
	}
	
	private void error( OLSyntaxNode node, String message )
	{
		valid = false;
		if ( node != null ) {
			ParsingContext context = node.context();
			logger.severe( context.sourceName() + ":" + context.line() + ": " + message );
		} else
			logger.severe( message );
	}
	
	public boolean validate()
	{
		visit( program );

		if ( mainDefined == false )
			error( null, "Main procedure not defined" );
		
		if ( !valid  ) {
			logger.severe( "Aborting: input file semantically invalid." );
			return false;
		}

		return valid;
	}
	
	public void visit( Program n )
	{
		for( OLSyntaxNode node : n.children() )
			node.accept( this );
	}
	
	public void visit( InputPortTypeInfo n )
	{
		if ( inputPortTypes.contains( n.id() ) )
			error( n, "input port type " + n.id() + " has been already defined" );
		inputPortTypes.add( n.id() );
		for( OperationDeclaration op : n.operations() )
			op.accept( this );
	}
	
	public void visit( OutputPortTypeInfo n )
	{
		if ( outputPortTypes.contains( n.id() ) )
			error( n, "output port type " + n.id() + " has been already defined" );
		outputPortTypes.add( n.id() );
		for( OperationDeclaration op : n.operations() )
			op.accept( this );
	}
	
	public void visit( PortInfo n )
	{
		if ( inputPortTypes.contains( n.id() ) )
			error( n, "Port name " + n.id() +
					" has been already defined as an input port type" );
		
		if ( outputPortTypes.contains( n.id() ) )
			error( n, "Port name " + n.id() +
					" has been already defined as an output port type" );
		
		if ( ports.get( n.id() ) != null )
			error( n, "Port name " + n.id() + " has been already defined" );
		else
			ports.put( n.id(), n );
		
		if (	!( inputPortTypes.contains( n.portType() ) ) &&
				!( outputPortTypes.contains( n.portType() ) ) )
			error( n, "Port " + n.id() + " tries to use an undefined port type (" + n.portType() + ")" );
	}
	
	public void visit( ServiceInfo n )
	{
		PortInfo port;
		Constants.ProtocolId protocolId = null;
		for( String p : n.inputPorts() ) {
			port = ports.get( p );
			if ( port == null )
				error( n, "Service at URI " + n.uri().toString() + " specifies an undefined port (" + p + ")" );
			else {
				if ( protocolId == null )
					protocolId = port.protocolId();
				else if ( protocolId != port.protocolId() )
					error( n, "Service at URI " + n.uri().toString() + " specifies ports with different protocols" );
			}
		}
	}
	
	private boolean isDefined( String id )
	{
		if (	operations.get( id ) != null ||
				procedureNames.contains( id )
				)
			return true;
			
		return false;
	}
		
	public void visit( OneWayOperationDeclaration n )
	{
		if ( isDefined( n.id() ) )
			error( n, "Operation " + n.id() + " uses an already defined identifier" );
		else
			operations.put( n.id(), n );
	}
		
	public void visit( RequestResponseOperationDeclaration n )
	{
		if ( isDefined( n.id() ) )
			error( n, "Operation " + n.id() + " uses an already defined identifier" );
		else
			operations.put( n.id(), n );
	}
		
	public void visit( NotificationOperationDeclaration n )
	{
		if ( isDefined( n.id() ) )
			error( n, "Operation " + n.id() + " uses an already defined identifier" );
		else
			operations.put( n.id(), n );
	}
		
	public void visit( SolicitResponseOperationDeclaration n )
	{
		if ( isDefined( n.id() ) )
			error( n, "Operation " + n.id() + " uses an already defined identifier" );
		else
			operations.put( n.id(), n );
	}
		
	public void visit( Procedure n )
	{
		if ( isDefined( n.id() ) )
			error( n, "Procedure " + n.id() + " uses an already defined identifier" );
		else
			procedureNames.add( n.id() );
		
		if ( "main".equals( n.id() ) )
			mainDefined = true;
		
		n.body().accept( this );
	}
		
	public void visit( ParallelStatement stm )
	{
		for( OLSyntaxNode node : stm.children() )
			node.accept( this );
	}
		
	public void visit( SequenceStatement stm )
	{
		for( OLSyntaxNode node : stm.children() )
			node.accept( this );
	}
		
	public void visit( NDChoiceStatement stm )
	{
		for( Pair< OLSyntaxNode, OLSyntaxNode > pair : stm.children() ) {
			pair.key().accept( this );
			pair.value().accept( this );
		}
	}
		
	public void visit( ThrowStatement n ) {}
	public void visit( CompensateStatement n ) {}
	public void visit( InstallStatement n ) {}
	public void visit( Scope n ) {}
	
	/*
	 * @todo Must check operation names in opNames and links in linkNames and locks in lockNames
	 */
	public void visit( OneWayOperationStatement n ) {}
	public void visit( RequestResponseOperationStatement n ) {}
	public void visit( NotificationOperationStatement n ) {}
	public void visit( SolicitResponseOperationStatement n ) {}
	public void visit( LinkInStatement n ) {}
	public void visit( LinkOutStatement n ) {}
	public void visit( SynchronizedStatement n ) {}
		
	/**
	 * @todo Must assign to a variable in varNames
	 */
	public void visit( AssignStatement n ) {}
	public void visit( PointerStatement n ) {}
	public void visit( DeepCopyStatement n ) {}
	public void visit( IfStatement n ) {}
	public void visit( ProcedureCallStatement n ) {}
	public void visit( WhileStatement n ) {}
	public void visit( OrConditionNode n ) {}
	public void visit( AndConditionNode n ) {}
	public void visit( NotConditionNode n ) {}
	public void visit( CompareConditionNode n ) {}
	public void visit( ExpressionConditionNode n ) {}
	public void visit( ConstantIntegerExpression n ) {}
	public void visit( ConstantRealExpression n ) {}
	public void visit( ConstantStringExpression n ) {}
	public void visit( ProductExpressionNode n ) {}
	public void visit( SumExpressionNode n ) {}
	public void visit( VariableExpressionNode n ) {}
	public void visit( NullProcessStatement n ) {}
	public void visit( ExitStatement n ) {}
	public void visit( ExecutionInfo n ) {}
	public void visit( CorrelationSetInfo n ) {}
	public void visit( RunStatement n ) {}
	public void visit( ValueVectorSizeExpressionNode n ) {}
	public void visit( PreIncrementStatement n ) {}
	public void visit( PostIncrementStatement n ) {}
	public void visit( PreDecrementStatement n ) {}
	public void visit( PostDecrementStatement n ) {}
	public void visit( UndefStatement n ) {}
	public void visit( ForStatement n ) {}
	public void visit( ForEachStatement n ) {}
	public void visit( IsTypeExpressionNode n ) {}
	public void visit( TypeCastExpressionNode n ) {}
	public void visit( EmbeddedServiceNode n ) {}
	
	/**
	 * @todo Must check if it's inside an install function
	 */
	public void visit( CurrentHandlerStatement n ) {}
}
