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
 * A generic visitor for the jolie AST.
 *
 * @param <C> The type of the context carried along the visit
 * @param <R> The return type of the visit
 */

public interface OLVisitor< C, R > {
	default R go( OLSyntaxNode n, C ctx ) {
		return n.accept( this, ctx );
	}

	R visit( Program n, C ctx );

	R visit( OneWayOperationDeclaration decl, C ctx );

	R visit( RequestResponseOperationDeclaration decl, C ctx );

	R visit( DefinitionNode n, C ctx );

	R visit( ParallelStatement n, C ctx );

	R visit( SequenceStatement n, C ctx );

	R visit( NDChoiceStatement n, C ctx );

	R visit( OneWayOperationStatement n, C ctx );

	R visit( RequestResponseOperationStatement n, C ctx );

	R visit( NotificationOperationStatement n, C ctx );

	R visit( SolicitResponseOperationStatement n, C ctx );

	R visit( LinkInStatement n, C ctx );

	R visit( LinkOutStatement n, C ctx );

	R visit( AssignStatement n, C ctx );

	R visit( AddAssignStatement n, C ctx );

	R visit( SubtractAssignStatement n, C ctx );

	R visit( MultiplyAssignStatement n, C ctx );

	R visit( DivideAssignStatement n, C ctx );

	R visit( IfStatement n, C ctx );

	R visit( DefinitionCallStatement n, C ctx );

	R visit( WhileStatement n, C ctx );

	R visit( OrConditionNode n, C ctx );

	R visit( AndConditionNode n, C ctx );

	R visit( NotExpressionNode n, C ctx );

	R visit( CompareConditionNode n, C ctx );

	R visit( ConstantIntegerExpression n, C ctx );

	R visit( ConstantDoubleExpression n, C ctx );

	R visit( ConstantBoolExpression n, C ctx );

	R visit( ConstantLongExpression n, C ctx );

	R visit( ConstantStringExpression n, C ctx );

	R visit( CurrentValueNode n, C ctx );

	R visit( PathsExpressionNode n, C ctx );

	R visit( ValuesExpressionNode n, C ctx );

	R visit( ProductExpressionNode n, C ctx );

	R visit( SumExpressionNode n, C ctx );

	R visit( VariableExpressionNode n, C ctx );

	R visit( NullProcessStatement n, C ctx );

	R visit( Scope n, C ctx );

	R visit( InstallStatement n, C ctx );

	R visit( CompensateStatement n, C ctx );

	R visit( ThrowStatement n, C ctx );

	R visit( ExitStatement n, C ctx );

	R visit( ExecutionInfo n, C ctx );

	R visit( CorrelationSetInfo n, C ctx );

	R visit( InputPortInfo n, C ctx );

	R visit( OutputPortInfo n, C ctx );

	R visit( PointerStatement n, C ctx );

	R visit( DeepCopyStatement n, C ctx );

	R visit( RunStatement n, C ctx );

	R visit( UndefStatement n, C ctx );

	R visit( ValueVectorSizeExpressionNode n, C ctx );

	R visit( PreIncrementStatement n, C ctx );

	R visit( PostIncrementStatement n, C ctx );

	R visit( PreDecrementStatement n, C ctx );

	R visit( PostDecrementStatement n, C ctx );

	R visit( ForStatement n, C ctx );

	R visit( ForEachSubNodeStatement n, C ctx );

	R visit( ForEachArrayItemStatement n, C ctx );

	R visit( SpawnStatement n, C ctx );

	R visit( IsTypeExpressionNode n, C ctx );

	R visit( InstanceOfExpressionNode n, C ctx );

	R visit( TypeCastExpressionNode n, C ctx );

	R visit( SynchronizedStatement n, C ctx );

	R visit( CurrentHandlerStatement n, C ctx );

	R visit( EmbeddedServiceNode n, C ctx );

	R visit( InstallFixedVariableExpressionNode n, C ctx );

	R visit( VariablePathNode n, C ctx );

	R visit( TypeInlineDefinition n, C ctx );

	R visit( TypeDefinitionLink n, C ctx );

	R visit( InterfaceDefinition n, C ctx );

	R visit( DocumentationComment n, C ctx );

	R visit( FreshValueExpressionNode n, C ctx );

	R visit( CourierDefinitionNode n, C ctx );

	R visit( CourierChoiceStatement n, C ctx );

	R visit( NotificationForwardStatement n, C ctx );

	R visit( SolicitResponseForwardStatement n, C ctx );

	R visit( InterfaceExtenderDefinition n, C ctx );

	R visit( InlineTreeExpressionNode n, C ctx );

	R visit( VoidExpressionNode n, C ctx );

	R visit( ProvideUntilStatement n, C ctx );

	R visit( TypeChoiceDefinition n, C ctx );

	R visit( ImportStatement n, C ctx );

	R visit( ServiceNode n, C ctx );

	R visit( EmbedServiceNode n, C ctx );

	R visit( SolicitResponseExpressionNode n, C ctx );

	R visit( IfExpressionNode n, C Ctx );
}
