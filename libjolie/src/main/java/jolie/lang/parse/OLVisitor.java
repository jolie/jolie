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
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import jolie.lang.parse.ast.types.TypeInlineDefinition;

public interface OLVisitor {
	default void go( OLSyntaxNode n ) {
		n.accept( this );
	}

	void visit( Program n );

	void visit( OneWayOperationDeclaration decl );

	void visit( RequestResponseOperationDeclaration decl );

	void visit( DefinitionNode n );

	void visit( ParallelStatement n );

	void visit( SequenceStatement n );

	void visit( NDChoiceStatement n );

	void visit( OneWayOperationStatement n );

	void visit( RequestResponseOperationStatement n );

	void visit( NotificationOperationStatement n );

	void visit( SolicitResponseOperationStatement n );

	void visit( LinkInStatement n );

	void visit( LinkOutStatement n );

	void visit( AssignStatement n );

	void visit( AddAssignStatement n );

	void visit( SubtractAssignStatement n );

	void visit( MultiplyAssignStatement n );

	void visit( DivideAssignStatement n );

	void visit( IfStatement n );

	void visit( DefinitionCallStatement n );

	void visit( WhileStatement n );

	void visit( OrConditionNode n );

	void visit( AndConditionNode n );

	void visit( NotExpressionNode n );

	void visit( CompareConditionNode n );

	void visit( ConstantIntegerExpression n );

	void visit( ConstantDoubleExpression n );

	void visit( ConstantBoolExpression n );

	void visit( ConstantLongExpression n );

	void visit( ConstantStringExpression n );

	void visit( ProductExpressionNode n );

	void visit( SumExpressionNode n );

	void visit( VariableExpressionNode n );

	void visit( NullProcessStatement n );

	void visit( Scope n );

	void visit( InstallStatement n );

	void visit( CompensateStatement n );

	void visit( ThrowStatement n );

	void visit( ExitStatement n );

	void visit( ExecutionInfo n );

	void visit( CorrelationSetInfo n );

	void visit( InputPortInfo n );

	void visit( OutputPortInfo n );

	void visit( PointerStatement n );

	void visit( DeepCopyStatement n );

	void visit( RunStatement n );

	void visit( UndefStatement n );

	void visit( ValueVectorSizeExpressionNode n );

	void visit( PreIncrementStatement n );

	void visit( PostIncrementStatement n );

	void visit( PreDecrementStatement n );

	void visit( PostDecrementStatement n );

	void visit( ForStatement n );

	void visit( ForEachSubNodeStatement n );

	void visit( ForEachArrayItemStatement n );

	void visit( SpawnStatement n );

	void visit( IsTypeExpressionNode n );

	void visit( InstanceOfExpressionNode n );

	void visit( TypeCastExpressionNode n );

	void visit( SynchronizedStatement n );

	void visit( CurrentHandlerStatement n );

	void visit( EmbeddedServiceNode n );

	void visit( InstallFixedVariableExpressionNode n );

	void visit( VariablePathNode n );

	void visit( TypeInlineDefinition n );

	void visit( TypeDefinitionLink n );

	void visit( InterfaceDefinition n );

	void visit( DocumentationComment n );

	void visit( FreshValueExpressionNode n );

	void visit( CourierDefinitionNode n );

	void visit( CourierChoiceStatement n );

	void visit( NotificationForwardStatement n );

	void visit( SolicitResponseForwardStatement n );

	void visit( InterfaceExtenderDefinition n );

	void visit( InlineTreeExpressionNode n );

	void visit( VoidExpressionNode n );

	void visit( ProvideUntilStatement n );

	void visit( TypeChoiceDefinition n );

	void visit( ImportStatement n );

	void visit( ServiceNode n );
}
