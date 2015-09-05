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
	private final Map< URI, List< InterfaceDefinition > > interfaces = new HashMap<>();
	private final Map< URI, List< InputPortInfo > > inputPorts = new HashMap<>();
	private final Map< URI, List< OutputPortInfo > > outputPorts = new HashMap<>();
	private final Map< URI, List< TypeDefinition > > types = new HashMap<>();
	private final Map< URI, List< EmbeddedServiceNode > > embeddedServices = new HashMap<>();
	private final Set< URI > sources = new HashSet<>();

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

	@Override
	public void visit( Program n )
	{
		for( OLSyntaxNode node : n.children() ) {
			node.accept( this );
		}
	}

	@Override
	public void visit( InterfaceDefinition n )
	{
		List< InterfaceDefinition > list = interfaces.get( n.context().source() );
		if ( list == null ) {
			list = new LinkedList<>();
			interfaces.put( n.context().source(), list );
		}
		list.add( n );

		encounteredNode( n );
	}

	@Override
	public void visit( TypeInlineDefinition n )
	{
		List< TypeDefinition > list = types.get( n.context().source() );
		if ( list == null ) {
			list = new LinkedList<>();
			types.put( n.context().source(), list );
		}
		list.add( n );

		encounteredNode( n );
	}

	@Override
	public void visit( TypeDefinitionLink n )
	{
		List< TypeDefinition > list = types.get( n.context().source() );
		if ( list == null ) {
			list = new LinkedList<>();
			types.put( n.context().source(), list );
		}
		list.add( n );

		encounteredNode( n );
	}

	@Override
	public void visit( InputPortInfo n )
	{
		List< InputPortInfo > list = inputPorts.get( n.context().source() );
		if ( list == null ) {
			list = new LinkedList<>();
			inputPorts.put( n.context().source(), list );
		}
		list.add( n );
		encounteredNode( n );
	}

	@Override
	public void visit( OutputPortInfo n )
	{
		List< OutputPortInfo > list = outputPorts.get( n.context().source() );
		if ( list == null ) {
			list = new LinkedList<>();
			outputPorts.put( n.context().source(), list );
		}
		list.add( n );

		encounteredNode( n );
	}

	@Override
	public void visit( EmbeddedServiceNode n )
	{
		List< EmbeddedServiceNode> list = embeddedServices.get( n.context().source() );
		if ( list == null ) {
			list = new LinkedList< >();
			embeddedServices.put( n.context().source(), list );
		}
		list.add( n );

		encounteredNode( n );
	}
    
	@Override
	public void visit( OneWayOperationDeclaration decl ) {}
	@Override
	public void visit( RequestResponseOperationDeclaration decl ) {}
	@Override
	public void visit( DefinitionNode n ) {}
	@Override
	public void visit( ParallelStatement n ) {}
	@Override
	public void visit( SequenceStatement n ) {}
	@Override
	public void visit( NDChoiceStatement n ) {}
	@Override
	public void visit( OneWayOperationStatement n ) {}
	@Override
	public void visit( RequestResponseOperationStatement n ) {}
	@Override
	public void visit( NotificationOperationStatement n ) {}
	@Override
	public void visit( SolicitResponseOperationStatement n ) {}
	@Override
	public void visit( LinkInStatement n ) {}
	@Override
	public void visit( LinkOutStatement n ) {}
	@Override
	public void visit( AssignStatement n ) {}
	@Override
	public void visit( IfStatement n ) {}
	@Override
	public void visit( DefinitionCallStatement n ) {}
	@Override
	public void visit( WhileStatement n ) {}
	@Override
	public void visit( OrConditionNode n ) {}
	@Override
	public void visit( AndConditionNode n ) {}
	@Override
	public void visit( NotExpressionNode n ) {}
	@Override
	public void visit( CompareConditionNode n ) {}
	@Override
	public void visit( ConstantIntegerExpression n ) {}
	@Override
	public void visit( ConstantLongExpression n ) {}
	@Override
	public void visit( ConstantBoolExpression n ) {}
	@Override
	public void visit( ConstantDoubleExpression n ) {}
	@Override
	public void visit( ConstantStringExpression n ) {}
	@Override
	public void visit( ProductExpressionNode n ) {}
	@Override
	public void visit( SumExpressionNode n ) {}
	@Override
	public void visit( VariableExpressionNode n ) {}
	@Override
	public void visit( NullProcessStatement n ) {}
	@Override
	public void visit( Scope n ) {}
	@Override
	public void visit( InstallStatement n ) {}
	@Override
	public void visit( CompensateStatement n ) {}
	@Override
	public void visit( ThrowStatement n ) {}
	@Override
	public void visit( ExitStatement n ) {}
	@Override
	public void visit( ExecutionInfo n ) {}
	@Override
	public void visit( CorrelationSetInfo n ) {}
	@Override
	public void visit( PointerStatement n ) {}
	@Override
	public void visit( DeepCopyStatement n ) {}
	@Override
	public void visit( RunStatement n ) {}
	@Override
	public void visit( UndefStatement n ) {}
	@Override
	public void visit( ValueVectorSizeExpressionNode n ) {}
	@Override
	public void visit( PreIncrementStatement n ) {}
	@Override
	public void visit( PostIncrementStatement n ) {}
	@Override
	public void visit( PreDecrementStatement n ) {}
	@Override
	public void visit( PostDecrementStatement n ) {}
	@Override
	public void visit( ForStatement n ) {}
	@Override
	public void visit( ForEachStatement n ) {}
	@Override
	public void visit( SpawnStatement n ) {}
	@Override
	public void visit( IsTypeExpressionNode n ) {}
	@Override
	public void visit( TypeCastExpressionNode n ) {}
	@Override
	public void visit( SynchronizedStatement n ) {}
	@Override
	public void visit( CurrentHandlerStatement n ) {}
	@Override
	public void visit( InstallFixedVariableExpressionNode n ) {}
	@Override
	public void visit( VariablePathNode n ) {}
	@Override
	public void visit( DocumentationComment n ) {}
	@Override
	public void visit( AddAssignStatement n ) {}
	@Override
	public void visit( SubtractAssignStatement n ) {}
	@Override
	public void visit( MultiplyAssignStatement n ) {}
	@Override
	public void visit( DivideAssignStatement n ) {}
	@Override
	public void visit( FreshValueExpressionNode n ) {}
	@Override
	public void visit( InterfaceExtenderDefinition n ) {}
	@Override
	public void visit( CourierDefinitionNode n ) {}
	@Override
	public void visit( CourierChoiceStatement n ) {}
	@Override
	public void visit( NotificationForwardStatement n ) {}
	@Override
	public void visit( InstanceOfExpressionNode n ) {}
	@Override
	public void visit( SolicitResponseForwardStatement n ) {}
	@Override
	public void visit( InlineTreeExpressionNode n ) {}
	@Override
	public void visit( VoidExpressionNode n ) {}
	@Override
	public void visit( ProvideUntilStatement n ) {}

	@Override
	public void visit(TypeChoiceDefinition n) {
		//todo
	}
}
