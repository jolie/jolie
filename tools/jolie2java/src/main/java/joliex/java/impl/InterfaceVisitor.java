/***************************************************************************
 *   Copyright (C) 2009 by Fabrizio Montesi                                *
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

package joliex.java.impl;

import java.util.*;

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
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import jolie.lang.parse.ast.types.TypeInlineDefinition;

/**
 *
 * @author Fabrizio Montesi
 */
public class InterfaceVisitor implements UnitOLVisitor {
	private final Program program;
	private final List< InterfaceDefinition > interfaceDefinitions =
		new ArrayList<>();
	private final Set< String > interfaceNames = new HashSet<>();

	public static class InterfaceNotFound extends Exception {
		public InterfaceNotFound( String message ) {
			super( message );
		}
	}

	public InterfaceVisitor( Program program, String[] interfaceNames ) {
		this.program = program;
		this.interfaceNames.addAll( Arrays.asList( interfaceNames ) );
	}

	public InterfaceDefinition[] getInterfaceDefinitions()
		throws InterfaceNotFound {
		program.accept( this );
		if( interfaceNames.isEmpty() == false ) {
			throw new InterfaceNotFound( interfaceNames.iterator().next() );
		}
		return interfaceDefinitions.toArray( new InterfaceDefinition[] {} );
	}

	@Override
	public void visit( Program n ) {
		for( OLSyntaxNode child : n.children() ) {
			child.accept( this );
		}
	}

	@Override
	public void visit( InterfaceDefinition n ) {
		if( interfaceNames.contains( n.name() ) ) {
			interfaceDefinitions.add( n );
			interfaceNames.remove( n.name() );
		}
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
	public void visit( ConstantDoubleExpression n ) {}

	@Override
	public void visit( ConstantStringExpression n ) {}

	@Override
	public void visit( ConstantLongExpression n ) {}

	@Override
	public void visit( ConstantBoolExpression n ) {}

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
	public void visit( InputPortInfo n ) {}

	@Override
	public void visit( OutputPortInfo n ) {}

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
	public void visit( InstanceOfExpressionNode n ) {}

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
	public void visit( EmbeddedServiceNode n ) {}

	@Override
	public void visit( InstallFixedVariableExpressionNode n ) {}

	@Override
	public void visit( VariablePathNode n ) {}

	@Override
	public void visit( TypeInlineDefinition n ) {}

	@Override
	public void visit( TypeDefinitionLink n ) {}

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
	public void visit( SolicitResponseForwardStatement n ) {}

	@Override
	public void visit( InlineTreeExpressionNode n ) {}

	@Override
	public void visit( VoidExpressionNode n ) {}

	@Override
	public void visit( ProvideUntilStatement n ) {}

	@Override
	public void visit( ForEachArrayItemStatement n ) {}

	@Override
	public void visit( TypeChoiceDefinition typeChoiceDefinition ) {}

	@Override
	public void visit( ImportStatement n ) {}

	@Override
	public void visit( ServiceNode n ) {}

	@Override
	public void visit( EmbedServiceNode n ) {}

	@Override
	public void visit( SolicitResponseExpressionNode n ) {}

	@Override
	public void visit( IfExpressionNode n ) {}
}
