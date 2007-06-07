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

import jolie.lang.parse.nodes.ol.AndConditionNode;
import jolie.lang.parse.nodes.ol.AssignStatement;
import jolie.lang.parse.nodes.ol.CompareConditionNode;
import jolie.lang.parse.nodes.ol.CompensateStatement;
import jolie.lang.parse.nodes.ol.ConstantIntegerExpression;
import jolie.lang.parse.nodes.ol.ConstantStringExpression;
import jolie.lang.parse.nodes.ol.ExpressionConditionNode;
import jolie.lang.parse.nodes.ol.IfStatement;
import jolie.lang.parse.nodes.ol.InStatement;
import jolie.lang.parse.nodes.ol.InstallCompensationStatement;
import jolie.lang.parse.nodes.ol.InstallFaultHandlerStatement;
import jolie.lang.parse.nodes.ol.InternalLinkDeclaration;
import jolie.lang.parse.nodes.ol.LinkInStatement;
import jolie.lang.parse.nodes.ol.LinkOutStatement;
import jolie.lang.parse.nodes.ol.LocationDeclaration;
import jolie.lang.parse.nodes.ol.NDChoiceStatement;
import jolie.lang.parse.nodes.ol.NotConditionNode;
import jolie.lang.parse.nodes.ol.NotificationOperationDeclaration;
import jolie.lang.parse.nodes.ol.NotificationOperationStatement;
import jolie.lang.parse.nodes.ol.NullProcessStatement;
import jolie.lang.parse.nodes.ol.OLSyntaxNode;
import jolie.lang.parse.nodes.ol.OneWayOperationDeclaration;
import jolie.lang.parse.nodes.ol.OneWayOperationStatement;
import jolie.lang.parse.nodes.ol.OperationDeclaration;
import jolie.lang.parse.nodes.ol.OrConditionNode;
import jolie.lang.parse.nodes.ol.OutStatement;
import jolie.lang.parse.nodes.ol.ParallelStatement;
import jolie.lang.parse.nodes.ol.Procedure;
import jolie.lang.parse.nodes.ol.ProcedureCallStatement;
import jolie.lang.parse.nodes.ol.ProductExpressionNode;
import jolie.lang.parse.nodes.ol.Program;
import jolie.lang.parse.nodes.ol.RequestResponseOperationDeclaration;
import jolie.lang.parse.nodes.ol.RequestResponseOperationStatement;
import jolie.lang.parse.nodes.ol.Scope;
import jolie.lang.parse.nodes.ol.SequenceStatement;
import jolie.lang.parse.nodes.ol.SleepStatement;
import jolie.lang.parse.nodes.ol.SolicitResponseOperationDeclaration;
import jolie.lang.parse.nodes.ol.SolicitResponseOperationStatement;
import jolie.lang.parse.nodes.ol.SumExpressionNode;
import jolie.lang.parse.nodes.ol.ThrowStatement;
import jolie.lang.parse.nodes.ol.VariableDeclaration;
import jolie.lang.parse.nodes.ol.VariableExpressionNode;
import jolie.lang.parse.nodes.ol.WhileStatement;
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
			program = new Program();
		}
		
		public Program optimize( Program p )
		{
			visit( p );
			return program;
		}
		
		public void visit( Program p )
		{
			for( LocationDeclaration decl : p.locationDeclarations() )
				decl.accept( this );
			for( OperationDeclaration decl : p.operationDeclarations() )
				decl.accept( this );
			for( InternalLinkDeclaration decl : p.linkDeclarations() )
				decl.accept( this );
			for( VariableDeclaration decl : p.variableDeclarations() )
				decl.accept( this );
			
			for( Procedure proc : p.procedures() )
				visit( proc );
		}
	
		public void visit( LocationDeclaration decl )
		{
			program.addLocationDeclaration( decl );
		}
		
		public void visit( OneWayOperationDeclaration decl )
		{
			program.addOperationDeclaration( decl );
		}
		
		public void visit( NotificationOperationDeclaration decl )
		{
			program.addOperationDeclaration( decl );
		}
		
		public void visit( RequestResponseOperationDeclaration decl )
		{
			program.addOperationDeclaration( decl );
		}
		
		public void visit( SolicitResponseOperationDeclaration decl )
		{
			program.addOperationDeclaration( decl );
		}
		
		public void visit( InternalLinkDeclaration decl )
		{
			program.addLinkDeclaration( decl );
		}
		
		public void visit( VariableDeclaration decl )
		{
			program.addVariableDeclaration( decl );
		}
		
		public void visit( Procedure procedure )
		{
			procedure.body().accept( this );
			program.addProcedure( new Procedure( procedure.id(), currNode ) );
		}
		
		public void visit( ParallelStatement stm )
		{
			if ( stm.children().size() > 1 ) {
				ParallelStatement tmp = new ParallelStatement();
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
					currNode = NullProcessStatement.getInstance();
				else
					currNode = tmp;
			} else
				stm.children().get( 0 ).accept( this );
		}
		
		public void visit( SequenceStatement stm )
		{
			if ( stm.children().size() > 1 ) {
				SequenceStatement tmp = new SequenceStatement();
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
					currNode = NullProcessStatement.getInstance();
				else
					currNode = tmp;
			} else
				stm.children().get( 0 ).accept( this );
		}
		
		public void visit( NDChoiceStatement stm )
		{
			if ( stm.children().size() > 1 ) {
				NDChoiceStatement tmp = new NDChoiceStatement();
				for( Pair< OLSyntaxNode, OLSyntaxNode > pair : stm.children() ) {
					pair.key().accept( this );
					OLSyntaxNode n = currNode;
					pair.value().accept( this );
					tmp.addChild( new Pair< OLSyntaxNode, OLSyntaxNode >( n, currNode ) );
				}
				currNode = tmp;
			} else {
				/*
				 * ( [ I ] A ) === I ;; A
				 * 
				 * An NDChoice formed by only one element
				 * is equivalent to a sequence beginning with the
				 * same input.
				 */
				SequenceStatement sequence = new SequenceStatement();
				Pair< OLSyntaxNode, OLSyntaxNode > pair = stm.children().get( 0 );
				sequence.addChild( pair.key() );
				sequence.addChild( pair.value() );
				sequence.accept( this );
			}
		}
		
		public void visit( IfStatement n )
		{
			IfStatement stm = new IfStatement();
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
			currNode = new WhileStatement( condition, currNode );
		}
		
		public void visit( RequestResponseOperationStatement n )
		{
			n.process().accept( this );
			currNode = new RequestResponseOperationStatement( n.id(), n.inVars(), n.outVars(), currNode );
		}
		
		public void visit( Scope n )
		{
			n.body().accept( this );
			currNode = new Scope( n.id(), currNode );
		}
		
		public void visit( InstallFaultHandlerStatement n )
		{
			n.body().accept( this );
			currNode = new InstallFaultHandlerStatement( n.id(), currNode );
		}
		
		public void visit( InstallCompensationStatement n )
		{
			n.body().accept( this );
			currNode = new InstallCompensationStatement( currNode );
		}
		
		public void visit( CompensateStatement n ) { currNode = n; }
		public void visit( ThrowStatement n ) { currNode = n; }
		public void visit( OneWayOperationStatement n ) { currNode = n; }
		public void visit( NotificationOperationStatement n ) { currNode = n; }
		public void visit( SolicitResponseOperationStatement n ) { currNode = n; }
		public void visit( LinkInStatement n ) { currNode = n; }
		public void visit( LinkOutStatement n ) { currNode = n; }
		public void visit( AssignStatement n ) { currNode = n; }
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
		public void visit( ConstantStringExpression n ) { currNode = n; }
		public void visit( ProductExpressionNode n ) { currNode = n; }
		public void visit( SumExpressionNode n ) { currNode = n; }
		public void visit( VariableExpressionNode n ) { currNode = n; }
		public void visit( NullProcessStatement n ) { currNode = n; }
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
