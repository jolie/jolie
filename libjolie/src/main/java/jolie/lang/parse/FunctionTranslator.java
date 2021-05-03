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
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import jolie.lang.parse.ast.types.TypeInlineDefinition;
import jolie.util.Pair;
import jolie.util.Unit;
import jolie.lang.Constants;
import jolie.lang.Constants.EmbeddedServiceType;
import jolie.lang.Constants.OperandType;
import jolie.lang.parse.context.ParsingContext;

public class FunctionTranslator {

	private static class TranslationVisitor implements OLVisitor< Unit, OLSyntaxNode > {

		private static final String HIDDEN_VARIABLE_PREFIX = "#";

		private final List< OLSyntaxNode > solicitResponseList = new ArrayList<>();

		private int hiddenVariableCounter = 0;

		public Program translate( Program program ) {
			return visit( program, Unit.INSTANCE );
		}

		@Override
		public Program visit( Program n, Unit ctx ) {
			List< OLSyntaxNode > children = new ArrayList<>();
			for( OLSyntaxNode node : n.children() ) {
				children.add( node.accept( this ) );
			}
			return new Program( n.context(), children );
		}

		@Override
		public ServiceNode visit( ServiceNode n, Unit ctx ) {
			Optional< Pair< String, TypeDefinition > > parameter =
				n.parameterConfiguration().map( config -> new Pair<>( config.variablePath(), config.type() ) );

			if( n.type() == EmbeddedServiceType.SERVICENODE ) {
				return ServiceNode.create( n.context(), n.name(), n.accessModifier(),
					visit( n.program(), Unit.INSTANCE ), parameter.orElse( null ) );
			} else {
				return ServiceNode.create( n.context(), n.name(), n.accessModifier(),
					visit( n.program(), Unit.INSTANCE ), parameter.orElse( null ), n.type(),
					n.implementationConfiguration() );
			}
		}

		@Override
		public DefinitionNode visit( DefinitionNode n, Unit ctx ) {
			return new DefinitionNode( n.context(), n.id(), n.body().accept( this ) );
		}

		@Override
		public ParallelStatement visit( ParallelStatement n, Unit ctx ) {
			ParallelStatement parallelStatement = new ParallelStatement( n.context() );
			for( OLSyntaxNode node : n.children() ) {
				parallelStatement.addChild( node.accept( this ) );
			}
			return parallelStatement;
		}

		@Override
		public SequenceStatement visit( SequenceStatement n, Unit ctx ) {
			SequenceStatement sequenceStatement = new SequenceStatement( n.context() );
			for( OLSyntaxNode node : n.children() ) {
				node = node.accept( this );
				if( node instanceof SequenceStatement ) {
					for( OLSyntaxNode subNode : ((SequenceStatement) node).children() ) {
						sequenceStatement.addChild( subNode );
					}
				} else {
					sequenceStatement.addChild( node );
				}
			}
			return sequenceStatement;
		}

		private SequenceStatement makeSequence( ParsingContext context, OLSyntaxNode... additionalNodes ) {
			SequenceStatement sequenceStatement = new SequenceStatement( context );
			for( OLSyntaxNode node : solicitResponseList ) {
				sequenceStatement.addChild( node );
			}
			for( OLSyntaxNode node : additionalNodes ) {
				sequenceStatement.addChild( node );
			}
			solicitResponseList.clear();
			return sequenceStatement;
		}

		@Override
		public OLSyntaxNode visit( AssignStatement n, Unit ctx ) {
			OLSyntaxNode expressionNode = null;
			if( n.expression() != null ) {
				expressionNode = n.expression().accept( this );
			}
			if( !solicitResponseList.isEmpty() ) {
				return makeSequence( n.context(), create( n, expressionNode ) );
			}
			return create( n, expressionNode );
		}

		private AssignStatement create( AssignStatement n, OLSyntaxNode expression ) {
			return new AssignStatement( n.context(), n.variablePath(), expression );
		}

		@Override
		public VariableExpressionNode visit( SolicitResponseExpression n, Unit ctx ) {
			OLSyntaxNode expressionNode = null;
			if( n.expression() != null ) {
				expressionNode = n.expression().accept( this );
			}
			VariablePathNode variablePathNode = generateVariablePathNode( n.context() );
			solicitResponseList.add( new SolicitResponseOperationStatement( n.context(), n.operationId(),
				n.outputPortId(), expressionNode, variablePathNode, Optional.empty() ) );
			return new VariableExpressionNode( n.context(), variablePathNode );
		}

		private VariablePathNode generateVariablePathNode( ParsingContext context ) {
			String variable = String.format( "%s%d", HIDDEN_VARIABLE_PREFIX, hiddenVariableCounter++ );
			VariablePathNode variablePathNode = new VariablePathNode( context, Type.NORMAL );
			variablePathNode.append( new Pair<>( new ConstantStringExpression( context, variable ), null ) );
			return variablePathNode;
		}

		@Override
		public OLSyntaxNode visit( SolicitResponseOperationStatement n, Unit ctx ) {
			OLSyntaxNode outputExpressionNode = null;
			if( n.outputExpression() != null ) {
				outputExpressionNode = n.outputExpression().accept( this );
			}
			if( !solicitResponseList.isEmpty() ) {
				return makeSequence( n.context(), create( n, outputExpressionNode ) );
			}
			return create( n, outputExpressionNode );
		}

		private SolicitResponseOperationStatement create( SolicitResponseOperationStatement n,
			OLSyntaxNode outputExpression ) {
			return new SolicitResponseOperationStatement( n.context(), n.id(), n.outputPortId(),
				outputExpression, n.inputVarPath(), Optional.ofNullable( n.handlersFunction() ) );
		}

		@Override
		public OLSyntaxNode visit( OneWayOperationDeclaration decl, Unit ctx ) {
			return decl;
		}

		@Override
		public OLSyntaxNode visit( RequestResponseOperationDeclaration decl, Unit ctx ) {
			return decl;
		}

		@Override
		public OLSyntaxNode visit( NDChoiceStatement n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( OneWayOperationStatement n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( RequestResponseOperationStatement n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( NotificationOperationStatement n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( LinkInStatement n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( LinkOutStatement n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( AddAssignStatement n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( SubtractAssignStatement n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( MultiplyAssignStatement n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( DivideAssignStatement n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( IfStatement n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( DefinitionCallStatement n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( WhileStatement n, Unit ctx ) {
			return n;
		}

		@Override
		public OrConditionNode visit( OrConditionNode n, Unit ctx ) {
			OrConditionNode conditionNode = new OrConditionNode( n.context() );
			for( OLSyntaxNode node : n.children() ) {
				conditionNode.addChild( node.accept( this ) );
			}
			return conditionNode;
		}

		@Override
		public AndConditionNode visit( AndConditionNode n, Unit ctx ) {
			AndConditionNode conditionNode = new AndConditionNode( n.context() );
			for( OLSyntaxNode node : n.children() ) {
				conditionNode.addChild( node.accept( this ) );
			}
			return conditionNode;
		}

		@Override
		public OLSyntaxNode visit( NotExpressionNode n, Unit ctx ) {
			return new NotExpressionNode( n.context(), n.expression().accept( this ) );
		}

		@Override
		public CompareConditionNode visit( CompareConditionNode n, Unit ctx ) {
			return new CompareConditionNode( n.context(), n.leftExpression().accept( this ),
				n.rightExpression().accept( this ), n.opType() );
		}

		@Override
		public OLSyntaxNode visit( ConstantIntegerExpression n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( ConstantDoubleExpression n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( ConstantBoolExpression n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( ConstantLongExpression n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( ConstantStringExpression n, Unit ctx ) {
			return n;
		}

		@Override
		public ProductExpressionNode visit( ProductExpressionNode n, Unit ctx ) {
			ProductExpressionNode productExpressionNode = new ProductExpressionNode( n.context() );
			for( Pair< Constants.OperandType, OLSyntaxNode > pair : n.operands() ) {
				if( pair.key() == OperandType.MULTIPLY ) {
					productExpressionNode.multiply( pair.value().accept( this ) );
				} else if( pair.key() == OperandType.DIVIDE ) {
					productExpressionNode.divide( pair.value().accept( this ) );
				} else {
					productExpressionNode.modulo( pair.value().accept( this ) );
				}
			}
			return productExpressionNode;
		}

		@Override
		public SumExpressionNode visit( SumExpressionNode n, Unit ctx ) {
			SumExpressionNode sumExpressionNode = new SumExpressionNode( n.context() );
			for( Pair< Constants.OperandType, OLSyntaxNode > pair : n.operands() ) {
				if( pair.key() == OperandType.ADD ) {
					sumExpressionNode.add( pair.value().accept( this ) );
				} else {
					sumExpressionNode.subtract( pair.value().accept( this ) );
				}
			}
			return sumExpressionNode;
		}

		@Override
		public OLSyntaxNode visit( VariableExpressionNode n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( NullProcessStatement n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( Scope n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( InstallStatement n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( CompensateStatement n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( ThrowStatement n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( ExitStatement n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( ExecutionInfo n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( CorrelationSetInfo n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( InputPortInfo n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( OutputPortInfo n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( PointerStatement n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( DeepCopyStatement n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( RunStatement n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( UndefStatement n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( ValueVectorSizeExpressionNode n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( PreIncrementStatement n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( PostIncrementStatement n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( PreDecrementStatement n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( PostDecrementStatement n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( ForStatement n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( ForEachSubNodeStatement n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( ForEachArrayItemStatement n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( SpawnStatement n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( IsTypeExpressionNode n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( InstanceOfExpressionNode n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( TypeCastExpressionNode n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( SynchronizedStatement n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( CurrentHandlerStatement n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( EmbeddedServiceNode n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( InstallFixedVariableExpressionNode n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( VariablePathNode n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( TypeInlineDefinition n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( TypeDefinitionLink n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( InterfaceDefinition n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( DocumentationComment n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( FreshValueExpressionNode n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( CourierDefinitionNode n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( CourierChoiceStatement n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( NotificationForwardStatement n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( SolicitResponseForwardStatement n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( InterfaceExtenderDefinition n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( InlineTreeExpressionNode n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( VoidExpressionNode n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( ProvideUntilStatement n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( TypeChoiceDefinition n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( ImportStatement n, Unit ctx ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( EmbedServiceNode n, Unit ctx ) {
			return n;
		}
	}

	public static Program run( Program program ) {
		return new TranslationVisitor().translate( program );
	}
}
