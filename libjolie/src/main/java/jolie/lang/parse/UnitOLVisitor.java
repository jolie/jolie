/*
 * Copyright (C) 2006-2020 Fabrizio Montesi <famontesi@gmail.com>
 * Copyright (C) 2020 Valentino Picotti <valentino.picotti@gmail.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package jolie.lang.parse;

import jolie.lang.parse.ast.expression.*;
import jolie.util.Unit;
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
import jolie.lang.parse.ast.types.TypeChoiceDefinition;
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import jolie.lang.parse.ast.types.TypeInlineDefinition;

public interface UnitOLVisitor extends OLVisitor< Unit, Unit > {
	default void go( OLSyntaxNode n ) {
		n.accept( this );
	}

	void visit( Program n );

	@Override
	default Unit visit( Program n, Unit c ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( OneWayOperationDeclaration decl );

	@Override
	default Unit visit( OneWayOperationDeclaration decl, Unit ctx ) {
		visit( decl );
		return Unit.INSTANCE;
	}

	void visit( RequestResponseOperationDeclaration decl );

	@Override
	default Unit visit( RequestResponseOperationDeclaration decl, Unit ctx ) {
		visit( decl );
		return Unit.INSTANCE;
	}

	void visit( DefinitionNode n );

	@Override
	default Unit visit( DefinitionNode n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( ParallelStatement n );

	@Override
	default Unit visit( ParallelStatement n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( SequenceStatement n );

	@Override
	default Unit visit( SequenceStatement n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( NDChoiceStatement n );

	@Override
	default Unit visit( NDChoiceStatement n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( OneWayOperationStatement n );

	@Override
	default Unit visit( OneWayOperationStatement n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( RequestResponseOperationStatement n );

	@Override
	default Unit visit( RequestResponseOperationStatement n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( NotificationOperationStatement n );

	@Override
	default Unit visit( NotificationOperationStatement n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( SolicitResponseOperationStatement n );

	@Override
	default Unit visit( SolicitResponseOperationStatement n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( LinkInStatement n );

	@Override
	default Unit visit( LinkInStatement n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( LinkOutStatement n );

	@Override
	default Unit visit( LinkOutStatement n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( AssignStatement n );

	@Override
	default Unit visit( AssignStatement n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( AddAssignStatement n );

	@Override
	default Unit visit( AddAssignStatement n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( SubtractAssignStatement n );

	@Override
	default Unit visit( SubtractAssignStatement n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( MultiplyAssignStatement n );

	@Override
	default Unit visit( MultiplyAssignStatement n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( DivideAssignStatement n );

	@Override
	default Unit visit( DivideAssignStatement n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( IfStatement n );

	@Override
	default Unit visit( IfStatement n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( DefinitionCallStatement n );

	@Override
	default Unit visit( DefinitionCallStatement n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( WhileStatement n );

	@Override
	default Unit visit( WhileStatement n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( OrConditionNode n );

	@Override
	default Unit visit( OrConditionNode n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( AndConditionNode n );

	@Override
	default Unit visit( AndConditionNode n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( NotExpressionNode n );

	@Override
	default Unit visit( NotExpressionNode n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( CompareConditionNode n );

	@Override
	default Unit visit( CompareConditionNode n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( ConstantIntegerExpression n );

	@Override
	default Unit visit( ConstantIntegerExpression n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( ConstantDoubleExpression n );

	@Override
	default Unit visit( ConstantDoubleExpression n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( ConstantBoolExpression n );

	@Override
	default Unit visit( ConstantBoolExpression n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( ConstantLongExpression n );

	@Override
	default Unit visit( ConstantLongExpression n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( ConstantStringExpression n );

	@Override
	default Unit visit( ConstantStringExpression n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( CurrentValueNode n );

	@Override
	default Unit visit( CurrentValueNode n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( PathsExpressionNode n );

	@Override
	default Unit visit( PathsExpressionNode n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( ValuesExpressionNode n );

	@Override
	default Unit visit( ValuesExpressionNode n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( PvalExpressionNode n );

	@Override
	default Unit visit( PvalExpressionNode n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( ProductExpressionNode n );

	@Override
	default Unit visit( ProductExpressionNode n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( SumExpressionNode n );

	@Override
	default Unit visit( SumExpressionNode n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( VariableExpressionNode n );

	@Override
	default Unit visit( VariableExpressionNode n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( NullProcessStatement n );

	@Override
	default Unit visit( NullProcessStatement n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( Scope n );

	@Override
	default Unit visit( Scope n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( InstallStatement n );

	@Override
	default Unit visit( InstallStatement n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( CompensateStatement n );

	@Override
	default Unit visit( CompensateStatement n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( ThrowStatement n );

	@Override
	default Unit visit( ThrowStatement n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( ExitStatement n );

	@Override
	default Unit visit( ExitStatement n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( ExecutionInfo n );

	@Override
	default Unit visit( ExecutionInfo n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( CorrelationSetInfo n );

	@Override
	default Unit visit( CorrelationSetInfo n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( InputPortInfo n );

	@Override
	default Unit visit( InputPortInfo n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( OutputPortInfo n );

	@Override
	default Unit visit( OutputPortInfo n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( PointerStatement n );

	@Override
	default Unit visit( PointerStatement n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( DeepCopyStatement n );

	@Override
	default Unit visit( DeepCopyStatement n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( RunStatement n );

	@Override
	default Unit visit( RunStatement n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( UndefStatement n );

	@Override
	default Unit visit( UndefStatement n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( ValueVectorSizeExpressionNode n );

	@Override
	default Unit visit( ValueVectorSizeExpressionNode n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( PreIncrementStatement n );

	@Override
	default Unit visit( PreIncrementStatement n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( PostIncrementStatement n );

	@Override
	default Unit visit( PostIncrementStatement n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( PreDecrementStatement n );

	@Override
	default Unit visit( PreDecrementStatement n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( PostDecrementStatement n );

	@Override
	default Unit visit( PostDecrementStatement n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( ForStatement n );

	@Override
	default Unit visit( ForStatement n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( ForEachSubNodeStatement n );

	@Override
	default Unit visit( ForEachSubNodeStatement n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( ForEachArrayItemStatement n );

	@Override
	default Unit visit( ForEachArrayItemStatement n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( SpawnStatement n );

	@Override
	default Unit visit( SpawnStatement n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( IsTypeExpressionNode n );

	@Override
	default Unit visit( IsTypeExpressionNode n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( InstanceOfExpressionNode n );

	@Override
	default Unit visit( InstanceOfExpressionNode n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( TypeCastExpressionNode n );

	@Override
	default Unit visit( TypeCastExpressionNode n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( SynchronizedStatement n );

	@Override
	default Unit visit( SynchronizedStatement n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( CurrentHandlerStatement n );

	@Override
	default Unit visit( CurrentHandlerStatement n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( EmbeddedServiceNode n );

	@Override
	default Unit visit( EmbeddedServiceNode n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( InstallFixedVariableExpressionNode n );

	@Override
	default Unit visit( InstallFixedVariableExpressionNode n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( VariablePathNode n );

	@Override
	default Unit visit( VariablePathNode n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( TypeInlineDefinition n );

	@Override
	default Unit visit( TypeInlineDefinition n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( TypeDefinitionLink n );

	@Override
	default Unit visit( TypeDefinitionLink n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( InterfaceDefinition n );

	@Override
	default Unit visit( InterfaceDefinition n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( DocumentationComment n );

	@Override
	default Unit visit( DocumentationComment n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( FreshValueExpressionNode n );

	@Override
	default Unit visit( FreshValueExpressionNode n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( CourierDefinitionNode n );

	@Override
	default Unit visit( CourierDefinitionNode n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( CourierChoiceStatement n );

	@Override
	default Unit visit( CourierChoiceStatement n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( NotificationForwardStatement n );

	@Override
	default Unit visit( NotificationForwardStatement n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( SolicitResponseForwardStatement n );

	@Override
	default Unit visit( SolicitResponseForwardStatement n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( InterfaceExtenderDefinition n );

	@Override
	default Unit visit( InterfaceExtenderDefinition n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( InlineTreeExpressionNode n );

	@Override
	default Unit visit( InlineTreeExpressionNode n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( VoidExpressionNode n );

	@Override
	default Unit visit( VoidExpressionNode n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( ProvideUntilStatement n );

	@Override
	default Unit visit( ProvideUntilStatement n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( TypeChoiceDefinition n );

	@Override
	default Unit visit( TypeChoiceDefinition n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( ImportStatement n );

	@Override
	default Unit visit( ImportStatement n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( ServiceNode n );

	@Override
	default Unit visit( ServiceNode n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( EmbedServiceNode n );

	@Override
	default Unit visit( EmbedServiceNode n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( SolicitResponseExpressionNode n );

	@Override
	default Unit visit( SolicitResponseExpressionNode n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}

	void visit( IfExpressionNode n );

	@Override
	default Unit visit( IfExpressionNode n, Unit ctx ) {
		visit( n );
		return Unit.INSTANCE;
	}
}
