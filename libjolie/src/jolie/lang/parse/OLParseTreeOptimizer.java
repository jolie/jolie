/***************************************************************************
 *   Copyright (C) by Fabrizio Montesi                                     *
 *   Copyright (C) by Claudio Guidi										   *
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

import jolie.lang.parse.ast.AndConditionNode;
import jolie.lang.parse.ast.AssignStatement;
import jolie.lang.parse.ast.CompareConditionNode;
import jolie.lang.parse.ast.CompensateStatement;
import jolie.lang.parse.ast.ConstantIntegerExpression;
import jolie.lang.parse.ast.ConstantRealExpression;
import jolie.lang.parse.ast.ConstantStringExpression;
import jolie.lang.parse.ast.CorrelationSetInfo;
import jolie.lang.parse.ast.CurrentHandlerStatement;
import jolie.lang.parse.ast.DeepCopyStatement;
import jolie.lang.parse.ast.ExecutionInfo;
import jolie.lang.parse.ast.ExitStatement;
import jolie.lang.parse.ast.ExpressionConditionNode;
import jolie.lang.parse.ast.ForEachStatement;
import jolie.lang.parse.ast.ForStatement;
import jolie.lang.parse.ast.IfStatement;
import jolie.lang.parse.ast.InStatement;
import jolie.lang.parse.ast.InputPortTypeInfo;
import jolie.lang.parse.ast.InstallStatement;
import jolie.lang.parse.ast.IsTypeExpressionNode;
import jolie.lang.parse.ast.LinkInStatement;
import jolie.lang.parse.ast.LinkOutStatement;
import jolie.lang.parse.ast.NDChoiceStatement;
import jolie.lang.parse.ast.NotConditionNode;
import jolie.lang.parse.ast.NotificationOperationDeclaration;
import jolie.lang.parse.ast.NotificationOperationStatement;
import jolie.lang.parse.ast.NullProcessStatement;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.ast.OneWayOperationDeclaration;
import jolie.lang.parse.ast.OneWayOperationStatement;
import jolie.lang.parse.ast.OrConditionNode;
import jolie.lang.parse.ast.OutStatement;
import jolie.lang.parse.ast.OutputPortTypeInfo;
import jolie.lang.parse.ast.ParallelStatement;
import jolie.lang.parse.ast.PointerStatement;
import jolie.lang.parse.ast.PortInfo;
import jolie.lang.parse.ast.PostDecrementStatement;
import jolie.lang.parse.ast.PostIncrementStatement;
import jolie.lang.parse.ast.PreDecrementStatement;
import jolie.lang.parse.ast.PreIncrementStatement;
import jolie.lang.parse.ast.Procedure;
import jolie.lang.parse.ast.ProcedureCallStatement;
import jolie.lang.parse.ast.ProductExpressionNode;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.ast.RequestResponseOperationDeclaration;
import jolie.lang.parse.ast.RequestResponseOperationStatement;
import jolie.lang.parse.ast.RunStatement;
import jolie.lang.parse.ast.Scope;
import jolie.lang.parse.ast.SequenceStatement;
import jolie.lang.parse.ast.ServiceInfo;
import jolie.lang.parse.ast.SleepStatement;
import jolie.lang.parse.ast.SolicitResponseOperationDeclaration;
import jolie.lang.parse.ast.SolicitResponseOperationStatement;
import jolie.lang.parse.ast.SumExpressionNode;
import jolie.lang.parse.ast.SynchronizedStatement;
import jolie.lang.parse.ast.ThrowStatement;
import jolie.lang.parse.ast.TypeCastExpressionNode;
import jolie.lang.parse.ast.UndefStatement;
import jolie.lang.parse.ast.ValueVectorSizeExpressionNode;
import jolie.lang.parse.ast.VariableExpressionNode;
import jolie.lang.parse.ast.WhileStatement;
import jolie.util.Pair;


/** Builds an optimized version of an OL parse tree.
 * 
 * @author Fabrizio Montesi
 */
public class OLParseTreeOptimizer
{	
	/**
	 * @todo Optimize expressions and conditions
	 *
	 */
	private class OptimizerVisitor implements OLVisitor
	{
		private Program program;
		private OLSyntaxNode currNode;
		
		public OptimizerVisitor()
		{
			program = new Program( new ParsingContext() );
		}
		
		public Program optimize( Program p )
		{
			visit( p );
			return program;
		}
		
		public void visit( Program p )
		{
			for( OLSyntaxNode node : p.children() )
				node.accept( this );
		}
		
		public void visit( ExecutionInfo p )
		{
			program.addChild( p );
		}
		
		public void visit( CorrelationSetInfo p )
		{
			program.addChild( p );
		}
		
		public void visit( InputPortTypeInfo p )
		{
			program.addChild( p );
		}
		
		public void visit( OutputPortTypeInfo p )
		{
			program.addChild( p );
		}
		
		public void visit( PortInfo p )
		{
			program.addChild( p );
		}
		
		public void visit( ServiceInfo p )
		{
			program.addChild( p );
		}
	
		public void visit( OneWayOperationDeclaration decl )
		{}
		
		public void visit( NotificationOperationDeclaration decl )
		{}
		
		public void visit( RequestResponseOperationDeclaration decl )
		{}
		
		public void visit( SolicitResponseOperationDeclaration decl )
		{}

		public void visit( Procedure procedure )
		{
			procedure.body().accept( this );
			program.addChild( new Procedure( procedure.context(), procedure.id(), currNode ) );
		}
		
		public void visit( ParallelStatement stm )
		{
			if ( stm.children().size() > 1 ) {
				ParallelStatement tmp = new ParallelStatement( stm.context() );
				for( OLSyntaxNode node : stm.children() ) {
					node.accept( this );
					if ( currNode instanceof ParallelStatement ) {
						/*
						 * A || (B || C) === A || B || C
						 */
						ParallelStatement curr = (ParallelStatement) currNode;
						for( OLSyntaxNode subNode : curr.children() )
							tmp.addChild( subNode );
					} else if ( !( currNode instanceof NullProcessStatement ) ) {
						/*
						 * The check is for:
						 * A || nullProcess === A
						 */
						tmp.addChild( currNode );
					}
				}
				
				/*
				 * If we ended up with an empty composition, return nullProcess
				 */
				if ( tmp.children().size() == 0 )
					currNode = new NullProcessStatement( new ParsingContext() );
				else
					currNode = tmp;
			} else
				stm.children().get( 0 ).accept( this );
		}
		
		public void visit( SequenceStatement stm )
		{
			if ( stm.children().size() > 1 ) {
				SequenceStatement tmp = new SequenceStatement( stm.context() );
				for( OLSyntaxNode node : stm.children() ) {
					node.accept( this );
					if ( currNode instanceof SequenceStatement ) {
						/*
						 * A ;; (B ;; C) === A ;; B ;; C
						 */
						SequenceStatement curr = (SequenceStatement) currNode;
						for( OLSyntaxNode subNode : curr.children() )
							tmp.addChild( subNode );
					} else if ( !( currNode instanceof NullProcessStatement ) ) {
						/*
						 * The check is for:
						 * seq ;; nullProcess ;; seq2 === seq ;; seq2
						 */
						tmp.addChild( currNode );
					}
				}
				
				/*
				 * If we ended up with an empty composition, return nullProcess
				 */
				if ( tmp.children().size() == 0 )
					currNode = new NullProcessStatement( new ParsingContext() );
				else
					currNode = tmp;
			} else
				stm.children().get( 0 ).accept( this );
		}
		
		public void visit( NDChoiceStatement stm )
		{
			//if ( stm.children().size() > 1 ) {
				NDChoiceStatement tmp = new NDChoiceStatement( stm.context() );
				for( Pair< OLSyntaxNode, OLSyntaxNode > pair : stm.children() ) {
					pair.key().accept( this );
					OLSyntaxNode n = currNode;
					pair.value().accept( this );
					tmp.addChild( new Pair< OLSyntaxNode, OLSyntaxNode >( n, currNode ) );
				}
				currNode = tmp;
			//} else {
				/*
				 * ( [ I ] A ) === I ;; A
				 * 
				 * An NDChoice formed by only one element
				 * is equivalent to a sequence beginning with the
				 * same input.
				 * 
				 * This is not true as of 19 Nov 07,
				 * because of InProcess special behaviour inside an NDChoiceProcess
				 */
				/*SequenceStatement sequence = new SequenceStatement();
				Pair< OLSyntaxNode, OLSyntaxNode > pair = stm.children().get( 0 );
				sequence.addChild( pair.key() );
				sequence.addChild( pair.value() );
				sequence.accept( this );
			}*/
		}
		
		public void visit( IfStatement n )
		{
			IfStatement stm = new IfStatement( n.context() );
			OLSyntaxNode condition;
			for( Pair< OLSyntaxNode, OLSyntaxNode > pair : n.children() ) {
				pair.key().accept( this );
				condition = currNode;
				pair.value().accept( this );
				stm.addChild( new Pair< OLSyntaxNode, OLSyntaxNode >( condition, currNode ) );
			}
			
			if ( n.elseProcess() != null ) {
				n.elseProcess().accept( this );
				stm.setElseProcess( currNode );
			}
			
			currNode = stm;
		}
		
		public void visit( WhileStatement n )
		{
			n.condition().accept( this );
			OLSyntaxNode condition = currNode;
			n.body().accept( this );
			currNode = new WhileStatement( n.context(), condition, currNode );
		}
		
		public void visit( RequestResponseOperationStatement n )
		{
			n.process().accept( this );
			currNode =
				new RequestResponseOperationStatement(
						n.context(),
						n.id(),
						n.inputVarPath(),
						n.outputVarPath(),
						currNode,
						n.channelInfo() );
		}
		
		public void visit( Scope n )
		{
			n.body().accept( this );
			currNode = new Scope( n.context(), n.id(), currNode );
		}
		
		/**
		 * @todo Optimize the associated processes
		 */
		public void visit( InstallStatement n )
		{
			currNode = n;
		}
		
		public void visit( SynchronizedStatement n )
		{
			n.body().accept( this );
			currNode = new SynchronizedStatement( n.context(), n.id(), currNode );
		}
				
		public void visit( CompensateStatement n ) { currNode = n; }
		public void visit( ThrowStatement n ) { currNode = n; }
		public void visit( OneWayOperationStatement n ) { currNode = n; }
		public void visit( NotificationOperationStatement n ) { currNode = n; }
		public void visit( SolicitResponseOperationStatement n ) { currNode = n; }
		public void visit( LinkInStatement n ) { currNode = n; }
		public void visit( LinkOutStatement n ) { currNode = n; }
		public void visit( AssignStatement n ) { currNode = n; }
		public void visit( DeepCopyStatement n ) { currNode = n; }
		public void visit( PointerStatement n ) { currNode = n; }
		public void visit( InStatement n ) { currNode = n; }
		public void visit( OutStatement n ) { currNode = n; }
		public void visit( ProcedureCallStatement n ) { currNode = n; }
		public void visit( SleepStatement n ) { currNode = n; }
		public void visit( OrConditionNode n ) { currNode = n; }
		public void visit( AndConditionNode n ) { currNode = n; }
		public void visit( NotConditionNode n ) { currNode = n; }
		public void visit( CompareConditionNode n ) { currNode = n; }
		public void visit( ExpressionConditionNode n ) { currNode = n; }
		public void visit( ConstantIntegerExpression n ) { currNode = n; }
		public void visit( ConstantRealExpression n ) { currNode = n; }
		public void visit( ConstantStringExpression n ) { currNode = n; }
		public void visit( ProductExpressionNode n ) { currNode = n; }
		public void visit( SumExpressionNode n ) { currNode = n; }
		public void visit( VariableExpressionNode n ) { currNode = n; }
		public void visit( NullProcessStatement n ) { currNode = n; }
		public void visit( ExitStatement n ) { currNode = n; }
		public void visit( RunStatement n ) { currNode = n; }
		public void visit( ValueVectorSizeExpressionNode n ) { currNode = n; }
		public void visit( PreIncrementStatement n ) { currNode = n; }
		public void visit( PostIncrementStatement n ) { currNode = n; }
		public void visit( PreDecrementStatement n ) { currNode = n; }
		public void visit( PostDecrementStatement n ) { currNode = n; }
		public void visit( UndefStatement n ) { currNode = n; }
		public void visit( ForStatement n ) { currNode = n; }
		public void visit( ForEachStatement n ) { currNode = n; }
		public void visit( IsTypeExpressionNode n ) { currNode = n; }
		public void visit( TypeCastExpressionNode n ) { currNode = n; }
		public void visit( CurrentHandlerStatement n ) { currNode = n; }
	}
	
	private Program originalProgram;
	
	public OLParseTreeOptimizer( Program originalProgram )
	{
		this.originalProgram = originalProgram;
	}

	public Program optimize()
	{
		return (new OptimizerVisitor()).optimize( originalProgram );
	}
}
