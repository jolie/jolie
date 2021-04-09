package jolie.lang.parse;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
import jolie.lang.parse.ast.SolicitResponseExpression;
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
import jolie.lang.parse.ast.VariablePathNode.Type;
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
import jolie.util.Pair;
import jolie.lang.Constants;
import jolie.lang.parse.context.ParsingContext;

public class FunctionTranslator {

	private static class TranslationVisitor implements UnitOLVisitor {

		private static final String HIDDEN_VARIABLE_PREFIX = "#";

		private int hiddenVariableCounter = 0;
		private final List< OLSyntaxNode > sequenceList = new ArrayList<>();

		public Program translate( Program program ) {
			visit( program );
			return program;
		}

		@Override
		public void visit( Program n ) {
			for( OLSyntaxNode node : n.children() ) {
				node.accept( this );
			}
		}

		@Override
		public void visit( ServiceNode n ) {
			visit( n.program() );
		}

		@Override
		public void visit( DefinitionNode n ) {
			n.body().accept( this );
		}

		@Override
		public void visit( ParallelStatement n ) {
			for( OLSyntaxNode node : n.children() ) {
				node.accept( this );
			}
		}

		@Override
		public void visit( SequenceStatement n ) {
			for( OLSyntaxNode node : n.children() ) {
				node.accept( this );
				sequenceList.add( node );
			}
			n.children().clear();
			n.children().addAll( sequenceList );
		}

		@Override
		public void visit( AssignStatement n ) {
			n.expression().accept( this );
		}

		@Override
		public void visit( OrConditionNode n ) {
			for( OLSyntaxNode node : n.children() ) {
				node.accept( this );
			}
		}

		@Override
		public void visit( AndConditionNode n ) {
			for( OLSyntaxNode node : n.children() ) {
				node.accept( this );
			}
		}

		@Override
		public void visit( SumExpressionNode n ) {
			expressionNodeOperandTranslation( n.context(), n.operands() );
		}

		@Override
		public void visit( ProductExpressionNode n ) {
			expressionNodeOperandTranslation( n.context(), n.operands() );
		}

		private void expressionNodeOperandTranslation( ParsingContext context,
			List< Pair< Constants.OperandType, OLSyntaxNode > > operands ) {
			for( int i = 0; i < operands.size(); i++ ) {
				Pair< Constants.OperandType, OLSyntaxNode > operand = operands.get( i );
				operand.value().accept( this );
				if( operand.value() instanceof SolicitResponseExpression ) {
					VariablePathNode variablePathNode = getVariableAndIncrement( context );

					VariableExpressionNode variableExpressionNode =
						new VariableExpressionNode( context, variablePathNode );

					operands.set( i, new Pair<>( operand.key(), variableExpressionNode ) );
				}
			}
		}

		@Override
		public void visit( SolicitResponseExpression n ) {
			n.expression().accept( this );

			VariablePathNode variablePathNode = getVariable( n.context() );

			sequenceList.add( new SolicitResponseOperationStatement( n.context(), n.operationId(),
				n.outputPortId(), n.expression(), variablePathNode, Optional.empty() ) );
		}

		private VariablePathNode getVariableAndIncrement( ParsingContext context ) {
			return generateVariablePathNode( context, true );
		}

		private VariablePathNode getVariable( ParsingContext context ) {
			return generateVariablePathNode( context, false );
		}

		private VariablePathNode generateVariablePathNode( ParsingContext context, boolean increment ) {
			String variable = String.format( "%s%d", HIDDEN_VARIABLE_PREFIX, hiddenVariableCounter );

			if( increment )
				hiddenVariableCounter++;

			VariablePathNode variablePathNode = new VariablePathNode( context, Type.NORMAL );
			variablePathNode.append( new Pair<>( new ConstantStringExpression( context, variable ), null ) );

			return variablePathNode;
		}

		@Override
		public void visit( SolicitResponseOperationStatement n ) {
			n.outputExpression().accept( this );
		}

		@Override
		public void visit( OneWayOperationDeclaration decl ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( RequestResponseOperationDeclaration decl ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( NDChoiceStatement n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( OneWayOperationStatement n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( RequestResponseOperationStatement n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( NotificationOperationStatement n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( LinkInStatement n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( LinkOutStatement n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( AddAssignStatement n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( SubtractAssignStatement n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( MultiplyAssignStatement n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( DivideAssignStatement n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( IfStatement n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( DefinitionCallStatement n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( WhileStatement n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( NotExpressionNode n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( CompareConditionNode n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( ConstantIntegerExpression n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( ConstantDoubleExpression n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( ConstantBoolExpression n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( ConstantLongExpression n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( ConstantStringExpression n ) {}

		@Override
		public void visit( VariableExpressionNode n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( NullProcessStatement n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( Scope n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( InstallStatement n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( CompensateStatement n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( ThrowStatement n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( ExitStatement n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( ExecutionInfo n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( CorrelationSetInfo n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( InputPortInfo n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( OutputPortInfo n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( PointerStatement n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( DeepCopyStatement n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( RunStatement n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( UndefStatement n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( ValueVectorSizeExpressionNode n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( PreIncrementStatement n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( PostIncrementStatement n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( PreDecrementStatement n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( PostDecrementStatement n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( ForStatement n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( ForEachSubNodeStatement n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( ForEachArrayItemStatement n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( SpawnStatement n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( IsTypeExpressionNode n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( InstanceOfExpressionNode n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( TypeCastExpressionNode n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( SynchronizedStatement n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( CurrentHandlerStatement n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( EmbeddedServiceNode n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( InstallFixedVariableExpressionNode n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( VariablePathNode n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( TypeInlineDefinition n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( TypeDefinitionLink n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( InterfaceDefinition n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( DocumentationComment n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( FreshValueExpressionNode n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( CourierDefinitionNode n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( CourierChoiceStatement n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( NotificationForwardStatement n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( SolicitResponseForwardStatement n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( InterfaceExtenderDefinition n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( InlineTreeExpressionNode n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( VoidExpressionNode n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( ProvideUntilStatement n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( TypeChoiceDefinition n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( ImportStatement n ) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit( EmbedServiceNode n ) {
			// TODO Auto-generated method stub

		}
	}

	public static Program run( Program program ) {
		return new TranslationVisitor().translate( program );
	}
}
