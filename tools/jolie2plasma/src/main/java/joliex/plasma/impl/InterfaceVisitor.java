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

package joliex.plasma.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import jolie.lang.parse.ast.types.TypeInlineDefinition;

/**
 *
 * @author Fabrizio Montesi
 */
public class InterfaceVisitor implements OLVisitor
{
	private final Program program;
	private final List< InterfaceDefinition > interfaceDefinitions =
		new ArrayList< InterfaceDefinition >();
	private final Set< String > interfaceNames = new HashSet< String >();

	public class InterfaceNotFound extends Exception
	{
		public InterfaceNotFound( String message )
		{
			super( message );
		}
	}

	public InterfaceVisitor( Program program, String[] interfaceNames )
	{
		this.program = program;
		for( String name : interfaceNames ) {
			this.interfaceNames.add( name );
		}
	}

	public InterfaceDefinition[] getInterfaceDefinitions()
		throws InterfaceNotFound
	{
		program.accept( this );
		if ( interfaceNames.isEmpty() == false ) {
			throw new InterfaceNotFound( interfaceNames.iterator().next() );
		}
		return interfaceDefinitions.toArray( new InterfaceDefinition[] {} );
	}

	public void visit( Program n )
	{
		for( OLSyntaxNode child : n.children() ) {
			child.accept( this );
		}
	}

	public void visit( InterfaceDefinition n )
	{
		if ( interfaceNames.contains( n.name() ) ) {
			interfaceDefinitions.add( n );
			interfaceNames.remove( n.name() );
		}
	}

	public void visit( FreshValueExpressionNode n ) {}
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
	public void visit( ConstantDoubleExpression n ) {}
	public void visit( InstanceOfExpressionNode n ) {}
	public void visit( ConstantStringExpression n ) {}
	public void visit( ConstantBoolExpression n ) {}
	public void visit( ConstantLongExpression n ) {}
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
	public void visit( InputPortInfo n ) {}
	public void visit( OutputPortInfo n ) {}
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
	public void visit( ForEachSubNodeStatement n ) {}
	public void visit( ForEachArrayItemStatement n ) {}
	public void visit( SpawnStatement n ) {}
	public void visit( IsTypeExpressionNode n ) {}
	public void visit( TypeCastExpressionNode n ) {}
	public void visit( SynchronizedStatement n ) {}
	public void visit( CurrentHandlerStatement n ) {}
	public void visit( EmbeddedServiceNode n ) {}
	public void visit( InstallFixedVariableExpressionNode n ) {}
	public void visit( VariablePathNode n ) {}
	public void visit( TypeInlineDefinition n ) {}
	public void visit( TypeDefinitionLink n ) {}
	public void visit( DocumentationComment n ) {}
	public void visit( AddAssignStatement n ) {}
	public void visit( SubtractAssignStatement n ) {}
	public void visit( MultiplyAssignStatement n ) {}
	public void visit( DivideAssignStatement n ) {}
	public void visit( InterfaceExtenderDefinition n ) {}
	public void visit( CourierDefinitionNode n ) {}
	public void visit( CourierChoiceStatement n ) {}
	public void visit( NotificationForwardStatement n ) {}
	public void visit( SolicitResponseForwardStatement n ) {}
	public void visit( InlineTreeExpressionNode n ) {}
	public void visit( VoidExpressionNode n ) {}
	public void visit( ProvideUntilStatement n ) {}

	@Override
	public void visit(TypeChoiceDefinition typeChoiceDefinition) {}
}
