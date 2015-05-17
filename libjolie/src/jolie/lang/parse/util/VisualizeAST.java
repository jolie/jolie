/***************************************************************************
 *   Copyright (C) 2015 by Martin Wolf <mw@martinwolf.eu>                  *
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
package jolie.lang.parse.util;

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
import jolie.lang.parse.ast.ForEachStatement;
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
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import jolie.lang.parse.ast.types.TypeInlineDefinition;

/**
 * Print out the Jolie AST for debugging purposes.
 */
public class VisualizeAST implements OLVisitor {

    private int tabs = 1;

    public static String padRight(String s, int n) {
        return new String(new char[n]).replace("\0", "\t") + s;
    }

    public void visit(Program n) {
        System.out.println(padRight("Program", tabs));
        tabs++;
        for (OLSyntaxNode node : n.children()) {
            node.accept(this);
        }
        tabs--;
    }

    public void visit(OneWayOperationDeclaration n) {
        System.out.println(padRight("OneWayOperationDeclaration", tabs));
    }

    public void visit(RequestResponseOperationDeclaration n) {
        System.out.println(padRight("RequestResponseOperationDeclaration", tabs));
    }

    public void visit(DefinitionNode n) {
        System.out.println(padRight("DefinitionNode", tabs));
        tabs++;
        if ( "main".equals( n.id() ) ) {
            n.body().accept(this);
        }
        tabs--;
    }

    public void visit(ParallelStatement n) {
        System.out.println(padRight("ParallelStatement", tabs));
        tabs++;
        for (OLSyntaxNode node : n.children()) {
            node.accept(this);
        }
        tabs--;
    }

    public void visit(SequenceStatement n) {
        System.out.println(padRight("SequenceStatement", tabs));
        tabs++;
        for (OLSyntaxNode node : n.children()) {
            node.accept(this);
        }
        tabs--;
    }

    public void visit(NDChoiceStatement n) {
        System.out.println(padRight("NDChoiceStatement", tabs));
    }

    public void visit(OneWayOperationStatement n) {
        System.out.println(padRight("OneWayOperationStatement", tabs));
    }

    public void visit(RequestResponseOperationStatement n) {
        System.out.println(padRight("RequestResponseOperationStatement", tabs));
    }

    public void visit(NotificationOperationStatement n) {
        System.out.println(padRight("NotificationOperationStatement", tabs));
    }

    public void visit(SolicitResponseOperationStatement n) {
        System.out.println(padRight("SolicitResponseOperationStatement", tabs));
    }

    public void visit(LinkInStatement n) {
        System.out.println(padRight("LinkInStatement", tabs));
    }

    public void visit(LinkOutStatement n) {
        System.out.println(padRight("LinkOutStatement", tabs));
    }

    public void visit(AssignStatement n) {
        System.out.println(padRight("AssignStatement", tabs));
    }

    public void visit(AddAssignStatement n) {
        System.out.println(padRight("AddAssignStatement", tabs));
    }

    public void visit(SubtractAssignStatement n) {
        System.out.println(padRight("SubtractAssignStatement", tabs));
    }

    public void visit(MultiplyAssignStatement n) {
        System.out.println(padRight("MultiplyAssignStatement", tabs));
    }

    public void visit(DivideAssignStatement n) {
        System.out.println(padRight("DivideAssignStatement", tabs));
    }

    public void visit(IfStatement n) {
        System.out.println(padRight("IfStatement", tabs));
    }

    public void visit(DefinitionCallStatement n) {
        System.out.println(padRight("DefinitionCallStatement", tabs));
    }

    public void visit(WhileStatement n) {
        System.out.println(padRight("WhileStatement", tabs));
    }

    public void visit(OrConditionNode n) {
        System.out.println(padRight("OrConditionNode", tabs));
        tabs++;
        for (OLSyntaxNode node : n.children()) {
            node.accept(this);
        }
        tabs--;
    }

    public void visit(AndConditionNode n) {
        System.out.println(padRight("AndConditionNode", tabs));
        tabs++;
        for (OLSyntaxNode node : n.children()) {
            node.accept(this);
        }
        tabs--;
    }

    public void visit(NotExpressionNode n) {
        System.out.println(padRight("NotExpressionNode", tabs));
    }

    public void visit(CompareConditionNode n) {
        System.out.println(padRight("CompareConditionNode", tabs));
    }

    public void visit(ConstantIntegerExpression n) {
        System.out.println(padRight("ConstantIntegerExpression", tabs));
    }

    public void visit(ConstantDoubleExpression n) {
        System.out.println(padRight("ConstantDoubleExpression", tabs));
    }

    public void visit(ConstantBoolExpression n) {
        System.out.println(padRight("ConstantBoolExpression", tabs));
    }

    public void visit(ConstantLongExpression n) {
        System.out.println(padRight("ConstantLongExpression", tabs));
    }

    public void visit(ConstantStringExpression n) {
        System.out.println(padRight("ConstantStringExpression", tabs));
    }

    public void visit(ProductExpressionNode n) {
        System.out.println(padRight("ProductExpressionNode", tabs));
    }

    public void visit(SumExpressionNode n) {
        System.out.println(padRight("SumExpressionNode", tabs));
    }

    public void visit(VariableExpressionNode n) {
        System.out.println(padRight("VariableExpressionNode", tabs));
    }

    public void visit(NullProcessStatement n) {
        System.out.println(padRight("NullProcessStatement", tabs));
    }

    public void visit(Scope n) {
        System.out.println(padRight("Scope", tabs));
    }

    public void visit(InstallStatement n) {
        System.out.println(padRight("InstallStatement", tabs));
    }

    public void visit(CompensateStatement n) {
        System.out.println(padRight("CompensateStatement", tabs));
    }

    public void visit(ThrowStatement n) {
        System.out.println(padRight("ThrowStatement", tabs));
    }

    public void visit(ExitStatement n) {
        System.out.println(padRight("ExitStatement", tabs));
    }

    public void visit(ExecutionInfo n) {
        System.out.println(padRight("ExecutionInfo", tabs));
    }

    public void visit(CorrelationSetInfo n) {
        System.out.println(padRight("CorrelationSetInfo", tabs));
    }

    public void visit(InputPortInfo n) {
        System.out.println(padRight("InputPortInfo", tabs));
        for ( String key : n.operationsMap().keySet()) {
            System.out.println(padRight("Key: " + key +"; " + n.operationsMap().get(key).id(), tabs+1));
        }
    }

    public void visit(OutputPortInfo n) {
        System.out.println(padRight("OutputPortInfo", tabs));
    }

    public void visit(PointerStatement n) {
        System.out.println(padRight("PointerStatement", tabs));
    }

    public void visit(DeepCopyStatement n) {
        System.out.println(padRight("DeepCopyStatement", tabs));
    }

    public void visit(RunStatement n) {
        System.out.println(padRight("RunStatement", tabs));
    }

    public void visit(UndefStatement n) {
        System.out.println(padRight("UndefStatement", tabs));
    }

    public void visit(ValueVectorSizeExpressionNode n) {
        System.out.println(padRight("ValueVectorSizeExpressionNode", tabs));
    }

    public void visit(PreIncrementStatement n) {
        System.out.println(padRight("PreIncrementStatement", tabs));
    }

    public void visit(PostIncrementStatement n) {
        System.out.println(padRight("PostIncrementStatement", tabs));
    }

    public void visit(PreDecrementStatement n) {
        System.out.println(padRight("PreDecrementStatement", tabs));
    }

    public void visit(PostDecrementStatement n) {
        System.out.println(padRight("PostDecrementStatement", tabs));
    }

    public void visit(ForStatement n) {
        System.out.println(padRight("ForStatement", tabs));
    }

    public void visit(ForEachStatement n) {
        System.out.println(padRight("ForEachStatement", tabs));
    }

    public void visit(SpawnStatement n) {
        System.out.println(padRight("SpawnStatement", tabs));
    }

    public void visit(IsTypeExpressionNode n) {
        System.out.println(padRight("IsTypeExpressionNode", tabs));
    }

    public void visit(InstanceOfExpressionNode n) {
        System.out.println(padRight("InstanceOfExpressionNode", tabs));
    }

    public void visit(TypeCastExpressionNode n) {
        System.out.println(padRight("TypeCastExpressionNode", tabs));
    }

    public void visit(SynchronizedStatement n) {
        System.out.println(padRight("SynchronizedStatement", tabs));
    }

    public void visit(CurrentHandlerStatement n) {
        System.out.println(padRight("CurrentHandlerStatement", tabs));
    }

    public void visit(EmbeddedServiceNode n) {
        System.out.println(padRight("EmbeddedServiceNode", tabs));
        tabs++;
        for (OLSyntaxNode node : n.children()) {
            node.accept(this);
        }
        tabs--;
    }

    public void visit(InstallFixedVariableExpressionNode n) {
        System.out.println(padRight("InstallFixedVariableExpressionNode", tabs));
    }

    public void visit(VariablePathNode n) {
        System.out.println(padRight("VariablePathNode", tabs));
    }

    public void visit(TypeInlineDefinition n) {
        System.out.println(padRight("TypeInlineDefinition: " + n.id(), tabs));
    }

    public void visit(TypeDefinitionLink n) {
        System.out.println(padRight("TypeDefinitionLink", tabs));
    }

    public void visit(InterfaceDefinition n) {
        System.out.println(padRight("InterfaceDefinition", tabs));
        for ( String key : n.operationsMap().keySet()) {
            System.out.println(padRight("Key: " + key +"; " + n.operationsMap().get(key).id(), tabs+1));
        }
    }

    public void visit(DocumentationComment n) {
        System.out.println(padRight("DocumentationComment", tabs));
    }

    public void visit(FreshValueExpressionNode n) {
        System.out.println(padRight("FreshValueExpressionNode", tabs));
    }

    public void visit(CourierDefinitionNode n) {
        System.out.println(padRight("CourierDefinitionNode", tabs));
    }

    public void visit(CourierChoiceStatement n) {
        System.out.println(padRight("CourierChoiceStatement", tabs));
    }

    public void visit(NotificationForwardStatement n) {
        System.out.println(padRight("NotificationForwardStatement", tabs));
    }

    public void visit(SolicitResponseForwardStatement n) {
        System.out.println(padRight("SolicitResponseForwardStatement", tabs));
    }

    public void visit(InterfaceExtenderDefinition n) {
        System.out.println(padRight("InterfaceExtenderDefinition", tabs));
    }

    public void visit(InlineTreeExpressionNode n) {
        System.out.println(padRight("InlineTreeExpressionNode", tabs));
    }

    public void visit(VoidExpressionNode n) {
        System.out.println(padRight("VoidExpressionNode", tabs));
    }

    public void visit(ProvideUntilStatement n) {
        System.out.println(padRight("ProvideUntilStatement", tabs));
    }

}
