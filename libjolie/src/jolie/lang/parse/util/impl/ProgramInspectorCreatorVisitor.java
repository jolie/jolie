/***************************************************************************
 *   Copyright (C) 2010 by Fabrizio Montesi <famontesi@gmail.com>          *
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

package jolie.lang.parse.util.impl;

import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.ast.*;
import jolie.lang.parse.ast.courier.CourierChoiceStatement;
import jolie.lang.parse.ast.courier.CourierDefinitionNode;
import jolie.lang.parse.ast.courier.NotificationForwardStatement;
import jolie.lang.parse.ast.courier.SolicitResponseForwardStatement;
import jolie.lang.parse.ast.expression.*;
import jolie.lang.parse.ast.types.TypeChoiceDefinition;
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import jolie.lang.parse.ast.types.TypeInlineDefinition;
import jolie.lang.parse.util.ProgramInspector;

import java.net.URI;
import java.util.*;

/**
 * Visitor for creating a {@link ProgramInspectorImpl} object.
 * @author Fabrizio Montesi
 */
public class ProgramInspectorCreatorVisitor implements OLVisitor
{
	private final Map< URI, List< InterfaceDefinition > > interfaces = new HashMap< URI, List< InterfaceDefinition > >();
	private final Map< URI, List< InputPortInfo > > inputPorts = new HashMap< URI, List< InputPortInfo > >();
	private final Map< URI, List< OutputPortInfo > > outputPorts = new HashMap< URI, List< OutputPortInfo > >();
	private final Map< URI, List< TypeDefinition > > types = new HashMap< URI, List< TypeDefinition > >();
	private final Map< URI, List< EmbeddedServiceNode > > embeddedServices = new HashMap< URI, List< EmbeddedServiceNode > >();
	private final Set< URI > sources = new HashSet< URI >();

	public ProgramInspectorCreatorVisitor( Program program )
	{
		program.accept( this );
	}

	public ProgramInspector createInspector()
	{
		return new ProgramInspectorImpl(
			sources.toArray( new URI[0] ),
			types,
			interfaces,
			inputPorts,
			outputPorts,
			embeddedServices
		);
	}

	private void encounteredNode( OLSyntaxNode n )
	{
		sources.add( n.context().source() );
	}

	public void visit( Program n )
	{
		for( OLSyntaxNode node : n.children() ) {
			node.accept( this );
		}
	}

	public void visit( InterfaceDefinition n )
	{
		List< InterfaceDefinition > list = interfaces.get( n.context().source() );
		if ( list == null ) {
			list = new LinkedList< InterfaceDefinition >();
			interfaces.put( n.context().source(), list );
		}
		list.add( n );

		encounteredNode( n );
	}

	public void visit( TypeInlineDefinition n )
	{
		List< TypeDefinition > list = types.get( n.context().source() );
		if ( list == null ) {
			list = new LinkedList< TypeDefinition >();
			types.put( n.context().source(), list );
		}
		list.add( n );

		encounteredNode( n );
	}

	public void visit( TypeDefinitionLink n )
	{
		List< TypeDefinition > list = types.get( n.context().source() );
		if ( list == null ) {
			list = new LinkedList< TypeDefinition >();
			types.put( n.context().source(), list );
		}
		list.add( n );

		encounteredNode( n );
	}

	public void visit( InputPortInfo n )
	{
		List< InputPortInfo > list = inputPorts.get( n.context().source() );
		if ( list == null ) {
			list = new LinkedList< InputPortInfo >();
			inputPorts.put( n.context().source(), list );
		}
		list.add( n );
		encounteredNode( n );
	}

	public void visit( OutputPortInfo n )
	{
		List< OutputPortInfo > list = outputPorts.get( n.context().source() );
		if ( list == null ) {
			list = new LinkedList< OutputPortInfo >();
			outputPorts.put( n.context().source(), list );
		}
		list.add( n );

		encounteredNode( n );
	}

	public void visit( EmbeddedServiceNode n )
	{
		List< EmbeddedServiceNode> list = embeddedServices.get( n.context().source() );
		if ( list == null ) {
			list = new LinkedList< EmbeddedServiceNode>();
			embeddedServices.put( n.context().source(), list );
		}
		list.add( n );

		encounteredNode( n );
	}
    
	public void visit( OneWayOperationDeclaration decl ) {}
	public void visit( RequestResponseOperationDeclaration decl ) {}
	public void visit( DefinitionNode n ) {}
	public void visit( ParallelStatement n ) {}
	public void visit( SequenceStatement n ) {}
	public void visit( NDChoiceStatement n ) {}
	public void visit( OneWayOperationStatement n ) {}
	public void visit( RequestResponseOperationStatement n ) {}
	public void visit( NotificationOperationStatement n ) {}
	public void visit( SolicitResponseOperationStatement n ) {}
	public void visit( LinkInStatement n ) {}
	public void visit( LinkOutStatement n ) {}
	public void visit( AssignStatement n ) {}
	public void visit( IfStatement n ) {}
	public void visit( DefinitionCallStatement n ) {}
	public void visit( WhileStatement n ) {}
	public void visit( OrConditionNode n ) {}
	public void visit( AndConditionNode n ) {}
	public void visit( NotExpressionNode n ) {}
	public void visit( CompareConditionNode n ) {}
	public void visit( ConstantIntegerExpression n ) {}
	public void visit( ConstantLongExpression n ) {}
	public void visit( ConstantBoolExpression n ) {}
	public void visit( ConstantDoubleExpression n ) {}
	public void visit( ConstantStringExpression n ) {}
	public void visit( ProductExpressionNode n ) {}
	public void visit( SumExpressionNode n ) {}
	public void visit( VariableExpressionNode n ) {}
	public void visit( NullProcessStatement n ) {}
	public void visit( Scope n ) {}
	public void visit( InstallStatement n ) {}
	public void visit( CompensateStatement n ) {}
	public void visit( ThrowStatement n ) {}
	public void visit( ExitStatement n ) {}
	public void visit( ExecutionInfo n ) {}
	public void visit( CorrelationSetInfo n ) {}
	public void visit( PointerStatement n ) {}
	public void visit( DeepCopyStatement n ) {}
	public void visit( RunStatement n ) {}
	public void visit( UndefStatement n ) {}
	public void visit( ValueVectorSizeExpressionNode n ) {}
	public void visit( PreIncrementStatement n ) {}
	public void visit( PostIncrementStatement n ) {}
	public void visit( PreDecrementStatement n ) {}
	public void visit( PostDecrementStatement n ) {}
	public void visit( ForStatement n ) {}
	public void visit( ForEachStatement n ) {}
	public void visit( SpawnStatement n ) {}
	public void visit( IsTypeExpressionNode n ) {}
	public void visit( TypeCastExpressionNode n ) {}
	public void visit( SynchronizedStatement n ) {}
	public void visit( CurrentHandlerStatement n ) {}
	public void visit( InstallFixedVariableExpressionNode n ) {}
	public void visit( VariablePathNode n ) {}
	public void visit( DocumentationComment n ) {}
	public void visit( AddAssignStatement n ) {}
	public void visit( SubtractAssignStatement n ) {}
	public void visit( MultiplyAssignStatement n ) {}
	public void visit( DivideAssignStatement n ) {}
	public void visit( FreshValueExpressionNode n ) {}
	public void visit( InterfaceExtenderDefinition n ) {}
	public void visit( CourierDefinitionNode n ) {}
	public void visit( CourierChoiceStatement n ) {}
	public void visit( NotificationForwardStatement n ) {}
	public void visit( InstanceOfExpressionNode n ) {}
	public void visit( SolicitResponseForwardStatement n ) {}
	public void visit( InlineTreeExpressionNode n ) {}
	public void visit( VoidExpressionNode n ) {}
	public void visit( ProvideUntilStatement n ) {}

	@Override
	public void visit(TypeChoiceDefinition n) {
		//todo
	}
}
