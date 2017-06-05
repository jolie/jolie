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

import java.lang.reflect.Array;
import jolie.lang.Constants;
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
import jolie.lang.parse.ast.InstallFunctionNode;
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
import jolie.lang.parse.context.ParsingContext;
import jolie.util.Pair;


/** Builds an optimized version of an OL parse tree.
 * 
 * @author Fabrizio Montesi
 */
public class OLParseTreeOptimizer
{	
	/**
	 * TODO Optimize expressions and conditions
	 *
	 */
	private static class OptimizerVisitor implements OLVisitor
	{
		private final Program program;
		private OLSyntaxNode currNode;
		
		public OptimizerVisitor( ParsingContext context )
		{
			program = new Program( context );
		}
		
		public Program optimize( Program p )
		{
			visit( p );
			return program;
		}
		
		private OLSyntaxNode optimize( OLSyntaxNode n )
		{
			n.accept( this );
			return currNode;
		}
		
		@Override
		public void visit( Program p )
		{
			for( OLSyntaxNode node : p.children() ) {
				node.accept( this );
			}
		}
		
		@Override
		public void visit( ExecutionInfo p )
		{
			program.addChild( p );
		}
		
		@Override
		public void visit( CorrelationSetInfo p )
		{
			program.addChild( p );
		}
		
		@Override
		public void visit( OutputPortInfo p )
		{
			if ( p.protocolConfiguration() != null ) {
				p.protocolConfiguration().accept( this );
				p.setProtocolConfiguration( currNode );
			}
			program.addChild( p	);
		}

		@Override
		public void visit( InputPortInfo p )
		{
			if ( p.protocolConfiguration() != null ) {
				p.protocolConfiguration().accept( this );
				InputPortInfo iport =
						new InputPortInfo(
								p.context(),
								p.id(),
								p.location(),
								p.protocolId(),
								currNode,
								p.aggregationList(),
								p.redirectionMap()
						);
				iport.operationsMap().putAll( p.operationsMap() );
				iport.getInterfaceList().addAll( p.getInterfaceList() );
				program.addChild( iport );
			} else {
				program.addChild( p );
			}
		}

		@Override
		public void visit( OneWayOperationDeclaration decl )
		{}

		@Override
		public void visit( RequestResponseOperationDeclaration decl )
		{}

		@Override
		public void visit( EmbeddedServiceNode n )
		{
			program.addChild( n );
		}

		@Override
		public void visit( DefinitionNode n )
		{
			program.addChild( new DefinitionNode( n.context(), n.id(), optimizeNode( n.body() ) ) );
		}

		@Override
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
				if ( tmp.children().isEmpty() ) {
					currNode = new NullProcessStatement( stm.context() );
				} else {
					currNode = tmp;
				}
			} else if ( stm.children().isEmpty() == false ) {
				stm.children().get( 0 ).accept( this );
			} else {
				currNode = new NullProcessStatement( stm.context() );
			}
		}
		
		@Override
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
				if ( tmp.children().isEmpty() ) {
					currNode = new NullProcessStatement( stm.context() );
				} else {
					currNode = tmp;
				}
			} else if ( stm.children().isEmpty() == false ) {
				stm.children().get( 0 ).accept( this );
			} else {
				currNode = new NullProcessStatement( stm.context() );
			}
		}

		@Override
		public void visit( NDChoiceStatement stm )
		{
			if ( stm.children().size() > 0 ) {
				NDChoiceStatement tmp = new NDChoiceStatement( stm.context() );
				for( Pair< OLSyntaxNode, OLSyntaxNode > pair : stm.children() ) {
					pair.key().accept( this );
					OLSyntaxNode n = currNode;
					pair.value().accept( this );
					tmp.addChild(new Pair<>( n, currNode ) );
				}
				currNode = tmp;
			} else {
				currNode = new NullProcessStatement( stm.context() );
			}
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
		
		@Override
		public void visit( IfStatement n )
		{
			IfStatement stm = new IfStatement( n.context() );
			OLSyntaxNode condition;
			for( Pair< OLSyntaxNode, OLSyntaxNode > pair : n.children() ) {
				pair.key().accept( this );
				condition = currNode;
				pair.value().accept( this );
				stm.addChild(new Pair<>( condition, currNode ) );
			}
			
			if ( n.elseProcess() != null ) {
				n.elseProcess().accept( this );
				stm.setElseProcess( currNode );
			}
			
			currNode = stm;
		}

		@Override
		public void visit( SpawnStatement n )
		{
			currNode = new SpawnStatement(
				n.context(),
				optimizePath( n.indexVariablePath() ),
				optimizeNode( n.upperBoundExpression() ),
				optimizePath( n.inVariablePath() ),
				optimizeNode( n.body() )
			);
		}
		
		@Override
		public void visit( WhileStatement n )
		{
			currNode = new WhileStatement(
				n.context(),
				optimizeNode( n.condition() ),
				optimizeNode( n.body() )
			);
		}
		
		@Override
		public void visit( ForStatement n )
		{
			currNode = new ForStatement(
				n.context(),
				optimizeNode( n.init() ),
				optimizeNode( n.condition() ),
				optimizeNode( n.post() ),
				optimizeNode( n.body() )
			);
		}
		
		@Override
		public void visit( ForEachSubNodeStatement n )
		{
			currNode = new ForEachSubNodeStatement(
				n.context(),
				optimizePath( n.keyPath() ),
				optimizePath( n.targetPath() ),
				optimizeNode( n.body() )
			);
		}

        @Override
        public void visit( ForEachArrayItemStatement n )
        {
            currNode = new ForEachArrayItemStatement(
				n.context(),
				optimizePath( n.keyPath() ),
				optimizePath( n.targetPath() ),
				optimizeNode( n.body() )
            );
        }

		@Override
		public void visit( VariablePathNode n )
		{
			VariablePathNode varPath = new VariablePathNode( n.context(), n.type() );
			for( Pair< OLSyntaxNode, OLSyntaxNode > node : n.path() ) {
				varPath.append( new Pair<>( optimizeNode( node.key() ), optimizeNode( node.value() ) ) );
			}
			currNode = varPath;
		}
		
		private VariablePathNode optimizePath( VariablePathNode n )
		{
			if ( n == null ) {
				return null;
			}
			n.accept( this );
			return (VariablePathNode)currNode;
		}

		private OLSyntaxNode optimizeNode( OLSyntaxNode n )
		{
			if ( n == null ) {
				return null;
			}
			n.accept( this );
			return currNode;
		}
		
		@Override
		public void visit( RequestResponseOperationStatement n )
		{
			OLSyntaxNode outputExpression = null;
			if ( n.outputExpression() != null ) {
				n.outputExpression().accept( this );
				outputExpression = currNode;
			}
			currNode =
				new RequestResponseOperationStatement(
						n.context(),
						n.id(),
						optimizePath( n.inputVarPath() ),
						outputExpression,
						optimizeNode( n.process() ) );
		}
		
		@Override
		public void visit( Scope n )
		{
			n.body().accept( this );
			currNode = new Scope( n.context(), n.id(), currNode );
		}
		
		@Override
		public void visit( InstallStatement n )
		{
			currNode = new InstallStatement( n.context(), optimizeInstallFunctionNode( n.handlersFunction() ) );
		}

		private InstallFunctionNode optimizeInstallFunctionNode( InstallFunctionNode n )
		{
			if ( n == null ) {
				return null;
			}

			Pair< String, OLSyntaxNode >[] pairs =
				(Pair< String, OLSyntaxNode >[]) Array.newInstance( Pair.class, n.pairs().length );
			int i = 0;
			for( Pair< String, OLSyntaxNode > pair : n.pairs() ) {
				pair.value().accept( this );
				pairs[ i++ ] = new Pair<>( pair.key(), currNode );
			}
			return new InstallFunctionNode( pairs );
		}
		
		@Override
		public void visit( SynchronizedStatement n )
		{
			n.body().accept( this );
			currNode = new SynchronizedStatement( n.context(), n.id(), currNode );
		}
				
		@Override
		public void visit( CompensateStatement n ) { currNode = n; }

		@Override
		public void visit( ThrowStatement n )
		{
			if ( n.expression() == null ) {
				currNode = null;
			} else {
				n.expression().accept( this );
			}
			currNode = new ThrowStatement( n.context(), n.id(), currNode );
		}

		@Override
		public void visit( OneWayOperationStatement n )
		{
			currNode = new OneWayOperationStatement(
				n.context(),
				n.id(),
				optimizePath( n.inputVarPath() )
			);
		}
		
		@Override
		public void visit( NotificationOperationStatement n )
		{
			OLSyntaxNode outputExpression = null;
			if ( n.outputExpression() != null ) {
				n.outputExpression().accept( this );
				outputExpression = currNode;
			}
			currNode = new NotificationOperationStatement(
							n.context(),
							n.id(),
							n.outputPortId(),
							outputExpression
						);
		}

		@Override
		public void visit( SolicitResponseOperationStatement n )
		{
			OLSyntaxNode outputExpression = null;
			if ( n.outputExpression() != null ) {
				n.outputExpression().accept( this );
				outputExpression = currNode;
			}
			currNode = new SolicitResponseOperationStatement(
							n.context(),
							n.id(),
							n.outputPortId(),
							outputExpression,
							optimizePath( n.inputVarPath() ),
							optimizeInstallFunctionNode( n.handlersFunction() )
						);
		}

		@Override
		public void visit( LinkInStatement n ) { currNode = n; }
		@Override
		public void visit( LinkOutStatement n ) { currNode = n; }

		@Override
		public void visit( AssignStatement n )
		{
			currNode = new AssignStatement(
				n.context(),
				optimizePath( n.variablePath() ),
				optimizeNode( n.expression() )
			);
		}

		@Override
		public void visit( AddAssignStatement n )
		{
			currNode = new AddAssignStatement(
				n.context(),
				optimizePath( n.variablePath() ),
				optimizeNode( n.expression() ) );
		}

		@Override
		public void visit( SubtractAssignStatement n )
		{
			currNode = new SubtractAssignStatement(
				n.context(),
				optimizePath( n.variablePath() ),
				optimizeNode( n.expression() ) );
		}

		@Override
		public void visit( MultiplyAssignStatement n )
		{
			currNode = new MultiplyAssignStatement(
				n.context(),
				optimizePath( n.variablePath() ),
				optimizeNode( n.expression() ) );
		}

		@Override
		public void visit( DivideAssignStatement n )
		{
			currNode = new DivideAssignStatement(
				n.context(),
				optimizePath( n.variablePath() ),
				optimizeNode( n.expression() ) );
		}

		@Override
		public void visit( DeepCopyStatement n )
		{
			currNode = new DeepCopyStatement(
				n.context(),
				optimizePath( n.leftPath() ),
				optimizeNode( n.rightExpression() )
			);
		}

		@Override
		public void visit( PointerStatement n )
		{
			currNode = new PointerStatement(
				n.context(),
				optimizePath( n.leftPath() ),
				optimizePath( n.rightPath() )
			);
		}

		@Override
		public void visit( DefinitionCallStatement n ) { currNode = n; }

		@Override
		public void visit( OrConditionNode n )
		{
			if ( n.children().size() > 1 ) {
				OrConditionNode ret = new OrConditionNode( n.context() );
				for( OLSyntaxNode child : n.children() ) {
					child.accept( this );
					ret.addChild( currNode );
				}
				currNode = ret;
			} else {
				n.children().get( 0 ).accept( this );
			}
		}
		
		@Override
		public void visit( AndConditionNode n )
		{
			if ( n.children().size() > 1 ) {
				AndConditionNode ret = new AndConditionNode( n.context() );
				for( OLSyntaxNode child : n.children() ) {
					child.accept( this );
					ret.addChild( currNode );
				}
				currNode = ret;
			} else {
				n.children().get( 0 ).accept( this );
			}
		}

		@Override
		public void visit( NotExpressionNode n )
		{
			n.expression().accept( this );
			currNode = new NotExpressionNode( n.context(), currNode );
		}

		@Override
		public void visit( CompareConditionNode n )
		{
			n.leftExpression().accept( this );
			OLSyntaxNode leftExpression = currNode;
			n.rightExpression().accept( this );
			currNode = new CompareConditionNode( n.context(), leftExpression, currNode, n.opType() );
		}

		@Override
		public void visit( ConstantIntegerExpression n ) { currNode = n; }
		
		@Override
		public void visit( ConstantLongExpression n ) { currNode = n; }
		
		@Override
		public void visit( ConstantBoolExpression n ) { currNode = n; }

		@Override
		public void visit( ConstantDoubleExpression n ) { currNode = n; }

		@Override
		public void visit( ConstantStringExpression n )
		{
			currNode = new ConstantStringExpression( n.context(), n.value().intern() );
		}

		@Override
		public void visit( ProductExpressionNode n )
		{
			if ( n.operands().size() > 1 ) {
				ProductExpressionNode ret = new ProductExpressionNode( n.context() );
				for( Pair< Constants.OperandType, OLSyntaxNode > pair : n.operands() ) {
					pair.value().accept( this );
					if ( pair.key() == Constants.OperandType.MULTIPLY ) {
						ret.multiply( currNode );
					} else if ( pair.key() == Constants.OperandType.DIVIDE ) {
						ret.divide( currNode );
					} else if ( pair.key() == Constants.OperandType.MODULUS ) {
						ret.modulo( currNode );
					}
				}
				currNode = ret;
			} else {
				n.operands().iterator().next().value().accept( this );
			}
		}
		
		@Override
		public void visit( SumExpressionNode n )
		{
			if ( n.operands().size() > 1 ) {
				SumExpressionNode ret = new SumExpressionNode( n.context() );
				for( Pair< Constants.OperandType, OLSyntaxNode > pair : n.operands() ) {
					pair.value().accept( this );
					if ( pair.key() == Constants.OperandType.ADD ) {
						ret.add( currNode );
					} else {
						ret.subtract( currNode );
					}
				}
				currNode = ret;
			} else {
				n.operands().iterator().next().value().accept( this );
			}
		}

		@Override
		public void visit( VariableExpressionNode n )
		{
			currNode = new VariableExpressionNode(
				n.context(),
				optimizePath( n.variablePath() )
			);
		}
		
		@Override
		public void visit( InstallFixedVariableExpressionNode n )
		{
			currNode = new InstallFixedVariableExpressionNode(
				n.context(),
				optimizePath( n.variablePath() )
			);
		}

		@Override
		public void visit( NullProcessStatement n ) { currNode = n; }
		@Override
		public void visit( ExitStatement n ) { currNode = n; }
		@Override
		public void visit( RunStatement n ) { currNode = n; }

		@Override
		public void visit( TypeInlineDefinition n )
		{
			program.addChild( n );
		}

		@Override
		public void visit( TypeDefinitionLink n )
		{
			program.addChild( n );
		}

		public void visit(TypeChoiceDefinition n) {
			program.addChild( n );
		}
		
		@Override
		public void visit( ValueVectorSizeExpressionNode n )
		{
			currNode = new ValueVectorSizeExpressionNode(
				n.context(),
				optimizePath( n.variablePath() )
			);
		}

		@Override
		public void visit( PreIncrementStatement n )
		{
			currNode = new PreIncrementStatement(
				n.context(),
				optimizePath( n.variablePath() )
			);
		}

		@Override
		public void visit( PostIncrementStatement n )
		{
			currNode = new PostIncrementStatement(
				n.context(),
				optimizePath( n.variablePath() )
			);
		}

		@Override
		public void visit( PreDecrementStatement n )
		{
			currNode = new PreDecrementStatement(
				n.context(),
				optimizePath( n.variablePath() )
			);
		}

		@Override
		public void visit( PostDecrementStatement n )
		{
			currNode = new PostDecrementStatement(
				n.context(),
				optimizePath( n.variablePath() )
			);
		}

		@Override
		public void visit( UndefStatement n )
		{
			currNode = new UndefStatement(
				n.context(),
				optimizePath( n.variablePath() )
			);
		}
		
		@Override
		public void visit( FreshValueExpressionNode n )
		{
			currNode = n;
		}

		@Override
		public void visit( IsTypeExpressionNode n )
		{
			currNode = new IsTypeExpressionNode(
					n.context(),
					n.type(),
					optimizePath( n.variablePath() )
				);
		}
		
		@Override
		public void visit( InstanceOfExpressionNode n )
		{
			currNode = new InstanceOfExpressionNode(
					n.context(),
					optimizeNode( n.expression() ),
					n.type()
				);
		}

		@Override
		public void visit( TypeCastExpressionNode n )
		{
			currNode = new TypeCastExpressionNode(
					n.context(),
					n.type(),
					optimizeNode( n.expression() )
				);
		}

		@Override
		public void visit( CurrentHandlerStatement n ) { currNode = n; }
		
		@Override
		public void visit( InterfaceDefinition n )
		{
			program.addChild( n );
		}
		
		@Override
		public void visit( InterfaceExtenderDefinition n )
		{
			program.addChild( n );
		}
	
		@Override
		public void visit( CourierDefinitionNode n )
		{
			program.addChild( new CourierDefinitionNode( n.context(), n.inputPortName(), optimizeNode( n.body() ) ) );
		}
	
		@Override
		public void visit( CourierChoiceStatement n )
		{
			CourierChoiceStatement courierChoice = new CourierChoiceStatement( n.context() );
			for( CourierChoiceStatement.InterfaceOneWayBranch branch : n.interfaceOneWayBranches() ) {
				courierChoice.interfaceOneWayBranches().add( new CourierChoiceStatement.InterfaceOneWayBranch( branch.interfaceDefinition, branch.inputVariablePath, optimizeNode( branch.body ) ) );
			}
			
			for( CourierChoiceStatement.InterfaceRequestResponseBranch branch : n.interfaceRequestResponseBranches() ) {
				courierChoice.interfaceRequestResponseBranches().add( new CourierChoiceStatement.InterfaceRequestResponseBranch( branch.interfaceDefinition, branch.inputVariablePath, branch.outputVariablePath, optimizeNode( branch.body ) ) );
			}
			
			for( CourierChoiceStatement.OperationOneWayBranch branch : n.operationOneWayBranches() ) {
				courierChoice.operationOneWayBranches().add( new CourierChoiceStatement.OperationOneWayBranch( branch.operation, branch.inputVariablePath, optimizeNode( branch.body ) ) );
			}
			
			for( CourierChoiceStatement.OperationRequestResponseBranch branch : n.operationRequestResponseBranches() ) {
				courierChoice.operationRequestResponseBranches().add( new CourierChoiceStatement.OperationRequestResponseBranch( branch.operation, branch.inputVariablePath, branch.outputVariablePath, optimizeNode( branch.body ) ) );
			}
			
			currNode = courierChoice;
		}

		@Override
		public void visit( NotificationForwardStatement n )
		{
			currNode = new NotificationForwardStatement( n.context(), n.outputPortName(), optimizePath( n.outputVariablePath() ) );
		}

		@Override
		public void visit( SolicitResponseForwardStatement n )
		{
			currNode = new SolicitResponseForwardStatement( n.context(), n.outputPortName(), optimizePath( n.outputVariablePath() ), optimizePath( n.inputVariablePath() ) );
		}
		
		@Override
		public void visit( VoidExpressionNode n )
		{
			currNode = n;
		}
		
		@Override
		public void visit( InlineTreeExpressionNode n )
		{
			Pair< VariablePathNode, OLSyntaxNode >[] optAssignments = new Pair[ n.assignments().length ];
			VariablePathNode currVarPath;
			OLSyntaxNode currExpr;
			int i = 0;
			for( Pair< VariablePathNode, OLSyntaxNode > pair : n.assignments() ) {
				currVarPath = optimizePath( pair.key() );
				currExpr = optimizeNode( pair.value() );
				optAssignments[i++] = new Pair<>( currVarPath, currExpr );
			}
			currNode = new InlineTreeExpressionNode( n.context(), optimizeNode( n.rootExpression() ), optAssignments );
		}
		
		@Override
		public void visit( ProvideUntilStatement n )
		{
			currNode = new ProvideUntilStatement(
				n.context(),
				optimizeNode( n.provide() ),
				optimizeNode( n.until() )
			);
		}

		@Override
		public void visit( DocumentationComment n ) {}
	}

	public static Program optimize( Program originalProgram )
	{
		return (new OptimizerVisitor( originalProgram.context() )).optimize( originalProgram );
	}
	
	public static OLSyntaxNode optimize( OLSyntaxNode node )
	{
		return (new OptimizerVisitor( node.context() )).optimize( node );
	}
}
