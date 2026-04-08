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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jolie.lang.parse.UnitOLVisitor;
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
import jolie.lang.parse.ast.EmbedServiceNode;
import jolie.lang.parse.ast.EmbeddedServiceNode;
import jolie.lang.parse.ast.ExecutionInfo;
import jolie.lang.parse.ast.ExitStatement;
import jolie.lang.parse.ast.ForEachArrayItemStatement;
import jolie.lang.parse.ast.ForEachSubNodeStatement;
import jolie.lang.parse.ast.ForStatement;
import jolie.lang.parse.ast.IfStatement;
import jolie.lang.parse.ast.ImportStatement;
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
import jolie.lang.parse.ast.PvalAssignStatement;
import jolie.lang.parse.ast.PvalDeepCopyStatement;
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
import jolie.lang.parse.ast.ServiceNode;
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
import jolie.lang.parse.ast.expression.*;
import jolie.lang.parse.ast.types.TypeChoiceDefinition;
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import jolie.lang.parse.ast.types.TypeInlineDefinition;
import jolie.lang.parse.util.ProgramInspector;
import jolie.util.Pair;

/**
 * Visitor for creating a {@link ProgramInspectorImpl} object.
 *
 * @author Fabrizio Montesi
 */
public class ProgramInspectorCreatorVisitor implements UnitOLVisitor {
	private final Map< URI, List< InterfaceDefinition > > interfaces = new HashMap<>();
	private final Map< URI, List< InputPortInfo > > inputPorts = new HashMap<>();
	private final Map< URI, List< OutputPortInfo > > outputPorts = new HashMap<>();
	private final Map< URI, List< TypeDefinition > > types = new HashMap<>();
	private final Map< URI, List< EmbeddedServiceNode > > embeddedServices = new HashMap<>();
	private final Map< URI, Map< OLSyntaxNode, List< OLSyntaxNode > > > behaviouralDependencies = new HashMap<>();
	private final Map< URI, List< ServiceNode > > serviceNodes = new HashMap<>();
	private final Set< URI > sources = new HashSet<>();

	private OLSyntaxNode currentFirstInput = null;

	public ProgramInspectorCreatorVisitor( Program program ) {
		program.accept( this );
	}

	public ProgramInspector createInspector() {
		return new ProgramInspectorImpl(
			sources.toArray( new URI[ 0 ] ),
			types,
			interfaces,
			inputPorts,
			outputPorts,
			embeddedServices,
			behaviouralDependencies,
			serviceNodes );
	}

	private void encounteredNode( OLSyntaxNode n ) {
		sources.add( n.context().source() );
	}

	@Override
	public void visit( Program n ) {
		for( OLSyntaxNode node : n.children() ) {
			node.accept( this );
		}
	}

	@Override
	public void visit( InterfaceDefinition n ) {
		List< InterfaceDefinition > list = interfaces.computeIfAbsent( n.context().source(), k -> new LinkedList<>() );
		list.add( n );

		encounteredNode( n );
	}

	@Override
	public void visit( TypeInlineDefinition n ) {
		List< TypeDefinition > list = types.computeIfAbsent( n.context().source(), k -> new LinkedList<>() );
		list.add( n );

		encounteredNode( n );
	}

	@Override
	public void visit( TypeDefinitionLink n ) {
		List< TypeDefinition > list = types.computeIfAbsent( n.context().source(), k -> new LinkedList<>() );
		list.add( n );

		encounteredNode( n );
	}

	@Override
	public void visit( InputPortInfo n ) {
		List< InputPortInfo > list = inputPorts.computeIfAbsent( n.context().source(), k -> new LinkedList<>() );
		list.add( n );
		encounteredNode( n );
	}

	@Override
	public void visit( OutputPortInfo n ) {
		List< OutputPortInfo > list = outputPorts.computeIfAbsent( n.context().source(), k -> new LinkedList<>() );
		list.add( n );

		encounteredNode( n );
	}

	@Override
	public void visit( EmbeddedServiceNode n ) {
		List< EmbeddedServiceNode > list =
			embeddedServices.computeIfAbsent( n.context().source(), k -> new LinkedList<>() );
		list.add( n );

		encounteredNode( n );
	}

	@Override
	public void visit( OneWayOperationDeclaration decl ) {}

	@Override
	public void visit( RequestResponseOperationDeclaration decl ) {}

	@Override
	public void visit( DefinitionNode n ) {
		n.body().accept( this );
	}

	@Override
	public void visit( ParallelStatement n ) {
		for( OLSyntaxNode node : n.children() ) {
			node.accept( this );
		}
	}

	@Override
	public void visit( SequenceStatement n ) {
		for( OLSyntaxNode node : n.children() ) {
			node.accept( this );
		}
	}

	@Override
	public void visit( NDChoiceStatement n ) {
		if( currentFirstInput != null ) {
			for( Pair< OLSyntaxNode, OLSyntaxNode > pair : n.children() ) {
				addOlSyntaxNodeToBehaviouralDependencies( pair.key() );
				pair.value().accept( this );
			}
		} else {
			for( Pair< OLSyntaxNode, OLSyntaxNode > pair : n.children() ) {
				if( pair.key() instanceof OneWayOperationStatement ) {
					currentFirstInput = pair.key();
				} else if( pair.key() instanceof RequestResponseOperationStatement ) {
					currentFirstInput = pair.key();
					((RequestResponseOperationStatement) pair.key()).process().accept( this );
				}
				pair.value().accept( this );
				currentFirstInput = null;
			}

		}
	}

	@Override
	public void visit( OneWayOperationStatement n ) {
		if( currentFirstInput == null ) {
			currentFirstInput = n;
		} else {
			addOlSyntaxNodeToBehaviouralDependencies( n );
		}
	}

	@Override
	public void visit( RequestResponseOperationStatement n ) {
		if( currentFirstInput == null ) {
			currentFirstInput = n;
		} else {
			addOlSyntaxNodeToBehaviouralDependencies( n );
		}
		n.process().accept( this );
	}

	@Override
	public void visit( NotificationOperationStatement n ) {
		addOlSyntaxNodeToBehaviouralDependencies( n );
	}

	@Override
	public void visit( SolicitResponseOperationStatement n ) {
		addOlSyntaxNodeToBehaviouralDependencies( n );
	}

	@Override
	public void visit( LinkInStatement n ) {}

	@Override
	public void visit( LinkOutStatement n ) {}

	@Override
	public void visit( AssignStatement n ) {}

	@Override
	public void visit( IfStatement n ) {
		for( Pair< OLSyntaxNode, OLSyntaxNode > pair : n.children() ) {
			pair.key().accept( this );
			pair.value().accept( this );
		}
		if( n.elseProcess() != null ) {
			n.elseProcess().accept( this );
		}
	}

	@Override
	public void visit( DefinitionCallStatement n ) {}

	@Override
	public void visit( WhileStatement n ) {
		n.body().accept( this );
	}

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
	public void visit( CurrentValueNode n ) {}

	@Override
	public void visit( PathsExpressionNode n ) {}

	@Override
	public void visit( ValuesExpressionNode n ) {}

	@Override
	public void visit( PvalExpressionNode n ) {}

	@Override
	public void visit( PvalAssignStatement n ) {}

	@Override
	public void visit( PvalDeepCopyStatement n ) {}

	@Override
	public void visit( ProductExpressionNode n ) {}

	@Override
	public void visit( SumExpressionNode n ) {}

	@Override
	public void visit( VariableExpressionNode n ) {}

	@Override
	public void visit( NullProcessStatement n ) {}

	@Override
	public void visit( Scope n ) {
		n.body().accept( this );
	}

	@Override
	public void visit( InstallStatement n ) {
		for( int i = 0; i < n.handlersFunction().pairs().length; i++ ) {
			n.handlersFunction().pairs()[ i ].value().accept( this );
		}
	}

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
	public void visit( ForStatement n ) {
		n.body().accept( this );
	}

	@Override
	public void visit( ForEachSubNodeStatement n ) {
		n.body().accept( this );
	}

	@Override
	public void visit( ForEachArrayItemStatement n ) {
		n.body().accept( this );
	}

	@Override
	public void visit( SpawnStatement n ) {
		n.body().accept( this );
	}

	@Override
	public void visit( IsTypeExpressionNode n ) {}

	@Override
	public void visit( TypeCastExpressionNode n ) {}

	@Override
	public void visit( SynchronizedStatement n ) {
		n.body().accept( this );
	}

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
	public void visit( SolicitResponseForwardStatement n ) {
		addOlSyntaxNodeToBehaviouralDependencies( n );
	}

	@Override
	public void visit( InlineTreeExpressionNode n ) {}

	@Override
	public void visit( VoidExpressionNode n ) {}

	@Override
	public void visit( ProvideUntilStatement n ) {
		n.provide().accept( this );
		n.until().accept( this );
	}

	@Override
	public void visit( TypeChoiceDefinition n ) {
		List< TypeDefinition > list = types.computeIfAbsent( n.context().source(), k -> new LinkedList<>() );
		list.add( n );

		encounteredNode( n );
	}

	private void addOlSyntaxNodeToBehaviouralDependencies( OLSyntaxNode n ) {
		if( currentFirstInput != null ) {
			behaviouralDependencies.computeIfAbsent( n.context().source(), k -> new HashMap<>() );
			Map< OLSyntaxNode, List< OLSyntaxNode > > sourceBehaviouralDependencies =
				behaviouralDependencies.get( n.context().source() );
			sourceBehaviouralDependencies.computeIfAbsent( currentFirstInput, k -> new ArrayList<>() );
			sourceBehaviouralDependencies.get( currentFirstInput ).add( n );
		}
	}

	@Override
	public void visit( ImportStatement n ) {}

	@Override
	public void visit( ServiceNode n ) {
		List< ServiceNode > list = serviceNodes.get( n.context().source() );
		if( list == null ) {
			list = new LinkedList<>();
			serviceNodes.put( n.context().source(), list );
		}
		list.add( n );
		encounteredNode( n );
		n.program().accept( this );
	}

	@Override
	public void visit( EmbedServiceNode n ) {}

	@Override
	public void visit( SolicitResponseExpressionNode n ) {}

	@Override
	public void visit( IfExpressionNode n ) {
		n.guard().accept( this );
		n.thenExpression().accept( this );
		n.elseExpression().accept( this );
	}
}
