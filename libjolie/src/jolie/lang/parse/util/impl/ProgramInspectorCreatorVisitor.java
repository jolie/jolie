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

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.ast.AddAssignStatement;
import jolie.lang.parse.ast.AssignStatement;
import jolie.lang.parse.ast.CompareConditionNode;
import jolie.lang.parse.ast.CompensateStatement;
import jolie.lang.parse.ast.CorrelationSetInfo;
import jolie.lang.parse.ast.CurrentHandlerStatement;
import jolie.lang.parse.ast.DeepCopyStatement;
import jolie.lang.parse.ast.DefinitionCallStatement;
import jolie.lang.parse.ast.DefinitionNode;
import jolie.lang.parse.ast.DivideAssignStatement;
import jolie.lang.parse.ast.DocumentationComment;
import jolie.lang.parse.ast.EmbeddedServiceNode;
import jolie.lang.parse.ast.ExecutionInfo;
import jolie.lang.parse.ast.ExitStatement;
import jolie.lang.parse.ast.ForEachArrayItemStatement;
import jolie.lang.parse.ast.ForEachSubNodeStatement;
import jolie.lang.parse.ast.ForStatement;
import jolie.lang.parse.ast.IfStatement;
import jolie.lang.parse.ast.InputPortInfo;
import jolie.lang.parse.ast.InstallFixedVariableExpressionNode;
import jolie.lang.parse.ast.InstallStatement;
import jolie.lang.parse.ast.InterfaceDefinition;
import jolie.lang.parse.ast.InterfaceExtenderDefinition;
import jolie.lang.parse.ast.LinkInStatement;
import jolie.lang.parse.ast.LinkOutStatement;
import jolie.lang.parse.ast.MultiplyAssignStatement;
import jolie.lang.parse.ast.NDChoiceStatement;
import jolie.lang.parse.ast.NotificationOperationStatement;
import jolie.lang.parse.ast.NullProcessStatement;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.ast.OneWayOperationDeclaration;
import jolie.lang.parse.ast.OneWayOperationStatement;
import jolie.lang.parse.ast.OutputPortInfo;
import jolie.lang.parse.ast.ParallelStatement;
import jolie.lang.parse.ast.PointerStatement;
import jolie.lang.parse.ast.PostDecrementStatement;
import jolie.lang.parse.ast.PostIncrementStatement;
import jolie.lang.parse.ast.PreDecrementStatement;
import jolie.lang.parse.ast.PreIncrementStatement;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.ast.ProvideUntilStatement;
import jolie.lang.parse.ast.RequestResponseOperationDeclaration;
import jolie.lang.parse.ast.RequestResponseOperationStatement;
import jolie.lang.parse.ast.RunStatement;
import jolie.lang.parse.ast.Scope;
import jolie.lang.parse.ast.SequenceStatement;
import jolie.lang.parse.ast.SolicitResponseOperationStatement;
import jolie.lang.parse.ast.SpawnStatement;
import jolie.lang.parse.ast.SubtractAssignStatement;
import jolie.lang.parse.ast.SynchronizedStatement;
import jolie.lang.parse.ast.ThrowStatement;
import jolie.lang.parse.ast.TypeCastExpressionNode;
import jolie.lang.parse.ast.UndefStatement;
import jolie.lang.parse.ast.ValueVectorSizeExpressionNode;
import jolie.lang.parse.ast.VariablePathNode;
import jolie.lang.parse.ast.WhileStatement;
import jolie.lang.parse.ast.courier.CourierChoiceStatement;
import jolie.lang.parse.ast.courier.CourierDefinitionNode;
import jolie.lang.parse.ast.courier.NotificationForwardStatement;
import jolie.lang.parse.ast.courier.SolicitResponseForwardStatement;
import jolie.lang.parse.ast.expression.AndConditionNode;
import jolie.lang.parse.ast.expression.ConstantBoolExpression;
import jolie.lang.parse.ast.expression.ConstantDoubleExpression;
import jolie.lang.parse.ast.expression.ConstantIntegerExpression;
import jolie.lang.parse.ast.expression.ConstantLongExpression;
import jolie.lang.parse.ast.expression.ConstantStringExpression;
import jolie.lang.parse.ast.expression.FreshValueExpressionNode;
import jolie.lang.parse.ast.expression.InlineTreeExpressionNode;
import jolie.lang.parse.ast.expression.InstanceOfExpressionNode;
import jolie.lang.parse.ast.expression.IsTypeExpressionNode;
import jolie.lang.parse.ast.expression.NotExpressionNode;
import jolie.lang.parse.ast.expression.OrConditionNode;
import jolie.lang.parse.ast.expression.ProductExpressionNode;
import jolie.lang.parse.ast.expression.SumExpressionNode;
import jolie.lang.parse.ast.expression.VariableExpressionNode;
import jolie.lang.parse.ast.expression.VoidExpressionNode;
import jolie.lang.parse.ast.types.TypeChoiceDefinition;
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import jolie.lang.parse.ast.types.TypeInlineDefinition;
import jolie.lang.parse.util.ProgramInspector;

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
	public void visit( ForEachSubNodeStatement n ) {}
	@Override
	public void visit( ForEachArrayItemStatement n ) {}
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
	public void visit( TypeChoiceDefinition n )
	{
		List< TypeDefinition > list = types.get( n.context().source() );
		if ( list == null ) {
			list = new LinkedList<>();
			types.put( n.context().source(), list );
		}
		list.add( n );

		encounteredNode( n );
	}
}
