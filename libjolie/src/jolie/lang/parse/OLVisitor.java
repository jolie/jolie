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

import jolie.lang.parse.ast.*;

public interface OLVisitor
{
	public void visit( Program n );
	//public void visit( LocationDefinition n );
	public void visit( OneWayOperationDeclaration decl );
	public void visit( NotificationOperationDeclaration decl );
	public void visit( RequestResponseOperationDeclaration decl );
	public void visit( SolicitResponseOperationDeclaration decl );
	//public void visit( InternalLinkDeclaration n );
	//public void visit( VariableDeclaration n );
	public void visit( Procedure n );
	public void visit( ParallelStatement n );
	public void visit( SequenceStatement n );
	public void visit( NDChoiceStatement n );
	public void visit( OneWayOperationStatement n );
	public void visit( RequestResponseOperationStatement n );
	public void visit( NotificationOperationStatement n );
	public void visit( SolicitResponseOperationStatement n );
	public void visit( LinkInStatement n );
	public void visit( LinkOutStatement n );
	public void visit( AssignStatement n );
	public void visit( IfStatement n );
	public void visit( InStatement n );
	public void visit( OutStatement n );
	public void visit( ProcedureCallStatement n );
	public void visit( SleepStatement n );
	public void visit( WhileStatement n );
	public void visit( OrConditionNode n );
	public void visit( AndConditionNode n );
	public void visit( NotConditionNode n );
	public void visit( CompareConditionNode n );
	public void visit( ExpressionConditionNode n );
	public void visit( ConstantIntegerExpression n );
	public void visit( ConstantStringExpression n );
	public void visit( ProductExpressionNode n );
	public void visit( SumExpressionNode n );
	public void visit( VariableExpressionNode n );
	//public void visit( VariableSizeExpressionNode n );
	public void visit( NullProcessStatement n );
	public void visit( Scope n );
	public void visit( InstallCompensationStatement n );
	public void visit( InstallFaultHandlerStatement n );
	public void visit( CompensateStatement n );
	public void visit( ThrowStatement n );
	public void visit( ExitStatement n );
	public void visit( StateInfo n );
	public void visit( ExecutionInfo n );
	public void visit( CorrelationSetInfo n );
	public void visit( InputPortTypeInfo n );
	public void visit( OutputPortTypeInfo n );
	public void visit( PortInfo n );
	public void visit( ServiceInfo n );
	public void visit( PointerStatement n );
	public void visit( DeepCopyStatement n );
	//public void visit( UndefStatement n );
}
