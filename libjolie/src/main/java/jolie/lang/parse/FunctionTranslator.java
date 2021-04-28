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
import jolie.lang.Constants.EmbeddedServiceType;
import jolie.lang.parse.context.ParsingContext;

public class FunctionTranslator {

	private static class TranslationVisitor implements FunctionVisitor {

		private static final String HIDDEN_VARIABLE_PREFIX = "#";

		private final List< OLSyntaxNode > statementOrderList = new ArrayList<>();

		private int hiddenVariableCounter = 0;

		public Program translate( Program program ) {
			return visit( program );
		}

		@Override
		public Program visit( Program n ) {
			List< OLSyntaxNode > children = new ArrayList<>();
			for( OLSyntaxNode node : n.children() ) {
				children.add( node.accept( this ) );
			}
			return new Program( n.context(), children );
		}

		@Override
		public ServiceNode visit( ServiceNode n ) {
			Optional< Pair< String, TypeDefinition > > parameter =
				n.parameterConfiguration().map( config -> new Pair<>( config.variablePath(), config.type() ) );

			if( n.type() == EmbeddedServiceType.SERVICENODE ) {
				return ServiceNode.create( n.context(), n.name(), n.accessModifier(),
					visit( n.program() ), parameter.orElse( null ) );
			} else {
				return ServiceNode.create( n.context(), n.name(), n.accessModifier(),
					visit( n.program() ), parameter.orElse( null ), n.type(), n.implementationConfiguration() );
			}
		}

		@Override
		public DefinitionNode visit( DefinitionNode n ) {
			return new DefinitionNode( n.context(), n.id(), n.body().accept( this ) );
		}

		@Override
		public ParallelStatement visit( ParallelStatement n ) {
			ParallelStatement parallelStatement = new ParallelStatement( n.context() );
			for( OLSyntaxNode node : n.children() ) {
				statementOrderList.add( node.accept( this ) );
			}
			for( OLSyntaxNode node : statementOrderList ) {
				parallelStatement.addChild( node );
			}
			statementOrderList.clear();
			return parallelStatement;
		}

		@Override
		public SequenceStatement visit( SequenceStatement n ) {
			SequenceStatement sequenceStatement = new SequenceStatement( n.context() );
			for( OLSyntaxNode node : n.children() ) {
				statementOrderList.add( node.accept( this ) );
			}
			for( OLSyntaxNode node : statementOrderList ) {
				sequenceStatement.addChild( node );
			}
			statementOrderList.clear();
			return sequenceStatement;
		}

		@Override
		public AssignStatement visit( AssignStatement n ) {
			OLSyntaxNode node = null;
			if( n.expression() != null ) {
				node = n.expression().accept( this );
			}
			if( node instanceof SolicitResponseExpression ) {
				VariablePathNode variablePathNode = getVariableAndIncrement( n.context() );
				node = new VariableExpressionNode( n.context(), variablePathNode );
			}
			return new AssignStatement( n.context(), n.variablePath(), node );
		}

		@Override
		public SolicitResponseExpression visit( SolicitResponseExpression n ) {
			OLSyntaxNode expressionNode = null;
			if( n.expression() != null ) {
				expressionNode = n.expression().accept( this );
			}
			VariablePathNode variablePathNode = getVariable( n.context() );
			statementOrderList.add( new SolicitResponseOperationStatement( n.context(), n.operationId(),
				n.outputPortId(), expressionNode, variablePathNode, Optional.empty() ) );
			return new SolicitResponseExpression( n.context(), n.operationId(), n.outputPortId(), expressionNode );
		}

		private VariablePathNode getVariableAndIncrement( ParsingContext context ) {
			return generateVariablePathNode( context, true );
		}

		private VariablePathNode getVariable( ParsingContext context ) {
			return generateVariablePathNode( context, false );
		}

		private VariablePathNode generateVariablePathNode( ParsingContext context, boolean increment ) {
			String variable = String.format( "%s%d", HIDDEN_VARIABLE_PREFIX, hiddenVariableCounter );
			if( increment ) {
				hiddenVariableCounter++;
			}
			VariablePathNode variablePathNode = new VariablePathNode( context, Type.NORMAL );
			variablePathNode.append( new Pair<>( new ConstantStringExpression( context, variable ), null ) );
			return variablePathNode;
		}

		@Override
		public SolicitResponseOperationStatement visit( SolicitResponseOperationStatement n ) {
			OLSyntaxNode outputExpression = null;
			if( n.outputExpression() != null ) {
				outputExpression = n.outputExpression().accept( this );
			}
			Optional< InstallFunctionNode > function = Optional.empty();
			if( n.handlersFunction() != null ) {
				function = Optional.of( n.handlersFunction() );
			}
			return new SolicitResponseOperationStatement( n.context(), n.id(), n.outputPortId(),
				outputExpression, n.inputVarPath(), function );
		}

		@Override
		public OrConditionNode visit( OrConditionNode n ) {
			return n;
		}

		@Override
		public AndConditionNode visit( AndConditionNode n ) {
			return n;
		}

		@Override
		public CompareConditionNode visit( CompareConditionNode n ) {
			return n;
		}

		@Override
		public SumExpressionNode visit( SumExpressionNode n ) {
			return n;
		}

		@Override
		public ProductExpressionNode visit( ProductExpressionNode n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( OneWayOperationDeclaration decl ) {
			return decl;
		}

		@Override
		public OLSyntaxNode visit( RequestResponseOperationDeclaration decl ) {
			return decl;
		}

		@Override
		public OLSyntaxNode visit( NDChoiceStatement n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( OneWayOperationStatement n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( RequestResponseOperationStatement n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( NotificationOperationStatement n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( LinkInStatement n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( LinkOutStatement n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( AddAssignStatement n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( SubtractAssignStatement n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( MultiplyAssignStatement n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( DivideAssignStatement n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( IfStatement n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( DefinitionCallStatement n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( WhileStatement n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( NotExpressionNode n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( ConstantIntegerExpression n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( ConstantDoubleExpression n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( ConstantBoolExpression n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( ConstantLongExpression n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( ConstantStringExpression n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( VariableExpressionNode n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( NullProcessStatement n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( Scope n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( InstallStatement n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( CompensateStatement n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( ThrowStatement n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( ExitStatement n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( ExecutionInfo n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( CorrelationSetInfo n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( InputPortInfo n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( OutputPortInfo n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( PointerStatement n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( DeepCopyStatement n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( RunStatement n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( UndefStatement n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( ValueVectorSizeExpressionNode n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( PreIncrementStatement n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( PostIncrementStatement n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( PreDecrementStatement n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( PostDecrementStatement n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( ForStatement n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( ForEachSubNodeStatement n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( ForEachArrayItemStatement n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( SpawnStatement n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( IsTypeExpressionNode n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( InstanceOfExpressionNode n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( TypeCastExpressionNode n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( SynchronizedStatement n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( CurrentHandlerStatement n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( EmbeddedServiceNode n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( InstallFixedVariableExpressionNode n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( VariablePathNode n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( TypeInlineDefinition n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( TypeDefinitionLink n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( InterfaceDefinition n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( DocumentationComment n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( FreshValueExpressionNode n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( CourierDefinitionNode n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( CourierChoiceStatement n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( NotificationForwardStatement n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( SolicitResponseForwardStatement n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( InterfaceExtenderDefinition n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( InlineTreeExpressionNode n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( VoidExpressionNode n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( ProvideUntilStatement n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( TypeChoiceDefinition n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( ImportStatement n ) {
			return n;
		}

		@Override
		public OLSyntaxNode visit( EmbedServiceNode n ) {
			return n;
		}
	}

	interface FunctionVisitor extends OLVisitor< Unit, OLSyntaxNode > {
		default OLSyntaxNode go( OLSyntaxNode n ) {
			return n.accept( this );
		}

		OLSyntaxNode visit( Program n );

		default OLSyntaxNode visit( Program n, Unit c ) {
			return visit( n );
		}

		OLSyntaxNode visit( OneWayOperationDeclaration decl );

		default OLSyntaxNode visit( OneWayOperationDeclaration decl, Unit ctx ) {
			return visit( decl );
		}

		OLSyntaxNode visit( RequestResponseOperationDeclaration decl );

		default OLSyntaxNode visit( RequestResponseOperationDeclaration decl, Unit ctx ) {
			return visit( decl );
		}

		OLSyntaxNode visit( DefinitionNode n );

		default OLSyntaxNode visit( DefinitionNode n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( ParallelStatement n );

		default OLSyntaxNode visit( ParallelStatement n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( SequenceStatement n );

		default OLSyntaxNode visit( SequenceStatement n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( NDChoiceStatement n );

		default OLSyntaxNode visit( NDChoiceStatement n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( OneWayOperationStatement n );

		default OLSyntaxNode visit( OneWayOperationStatement n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( RequestResponseOperationStatement n );

		default OLSyntaxNode visit( RequestResponseOperationStatement n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( NotificationOperationStatement n );

		default OLSyntaxNode visit( NotificationOperationStatement n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( SolicitResponseOperationStatement n );

		default OLSyntaxNode visit( SolicitResponseOperationStatement n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( LinkInStatement n );

		default OLSyntaxNode visit( LinkInStatement n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( LinkOutStatement n );

		default OLSyntaxNode visit( LinkOutStatement n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( AssignStatement n );

		default OLSyntaxNode visit( AssignStatement n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( AddAssignStatement n );

		default OLSyntaxNode visit( AddAssignStatement n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( SubtractAssignStatement n );

		default OLSyntaxNode visit( SubtractAssignStatement n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( MultiplyAssignStatement n );

		default OLSyntaxNode visit( MultiplyAssignStatement n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( DivideAssignStatement n );

		default OLSyntaxNode visit( DivideAssignStatement n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( IfStatement n );

		default OLSyntaxNode visit( IfStatement n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( DefinitionCallStatement n );

		default OLSyntaxNode visit( DefinitionCallStatement n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( WhileStatement n );

		default OLSyntaxNode visit( WhileStatement n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( OrConditionNode n );

		default OLSyntaxNode visit( OrConditionNode n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( AndConditionNode n );

		default OLSyntaxNode visit( AndConditionNode n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( NotExpressionNode n );

		default OLSyntaxNode visit( NotExpressionNode n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( CompareConditionNode n );

		default OLSyntaxNode visit( CompareConditionNode n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( ConstantIntegerExpression n );

		default OLSyntaxNode visit( ConstantIntegerExpression n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( ConstantDoubleExpression n );

		default OLSyntaxNode visit( ConstantDoubleExpression n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( ConstantBoolExpression n );

		default OLSyntaxNode visit( ConstantBoolExpression n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( ConstantLongExpression n );

		default OLSyntaxNode visit( ConstantLongExpression n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( ConstantStringExpression n );

		default OLSyntaxNode visit( ConstantStringExpression n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( ProductExpressionNode n );

		default OLSyntaxNode visit( ProductExpressionNode n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( SumExpressionNode n );

		default OLSyntaxNode visit( SumExpressionNode n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( VariableExpressionNode n );

		default OLSyntaxNode visit( VariableExpressionNode n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( NullProcessStatement n );

		default OLSyntaxNode visit( NullProcessStatement n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( Scope n );

		default OLSyntaxNode visit( Scope n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( InstallStatement n );

		default OLSyntaxNode visit( InstallStatement n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( CompensateStatement n );

		default OLSyntaxNode visit( CompensateStatement n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( ThrowStatement n );

		default OLSyntaxNode visit( ThrowStatement n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( ExitStatement n );

		default OLSyntaxNode visit( ExitStatement n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( ExecutionInfo n );

		default OLSyntaxNode visit( ExecutionInfo n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( CorrelationSetInfo n );

		default OLSyntaxNode visit( CorrelationSetInfo n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( InputPortInfo n );

		default OLSyntaxNode visit( InputPortInfo n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( OutputPortInfo n );

		default OLSyntaxNode visit( OutputPortInfo n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( PointerStatement n );

		default OLSyntaxNode visit( PointerStatement n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( DeepCopyStatement n );

		default OLSyntaxNode visit( DeepCopyStatement n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( RunStatement n );

		default OLSyntaxNode visit( RunStatement n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( UndefStatement n );

		default OLSyntaxNode visit( UndefStatement n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( ValueVectorSizeExpressionNode n );

		default OLSyntaxNode visit( ValueVectorSizeExpressionNode n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( PreIncrementStatement n );

		default OLSyntaxNode visit( PreIncrementStatement n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( PostIncrementStatement n );

		default OLSyntaxNode visit( PostIncrementStatement n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( PreDecrementStatement n );

		default OLSyntaxNode visit( PreDecrementStatement n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( PostDecrementStatement n );

		default OLSyntaxNode visit( PostDecrementStatement n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( ForStatement n );

		default OLSyntaxNode visit( ForStatement n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( ForEachSubNodeStatement n );

		default OLSyntaxNode visit( ForEachSubNodeStatement n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( ForEachArrayItemStatement n );

		default OLSyntaxNode visit( ForEachArrayItemStatement n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( SpawnStatement n );

		default OLSyntaxNode visit( SpawnStatement n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( IsTypeExpressionNode n );

		default OLSyntaxNode visit( IsTypeExpressionNode n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( InstanceOfExpressionNode n );

		default OLSyntaxNode visit( InstanceOfExpressionNode n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( TypeCastExpressionNode n );

		default OLSyntaxNode visit( TypeCastExpressionNode n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( SynchronizedStatement n );

		default OLSyntaxNode visit( SynchronizedStatement n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( CurrentHandlerStatement n );

		default OLSyntaxNode visit( CurrentHandlerStatement n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( EmbeddedServiceNode n );

		default OLSyntaxNode visit( EmbeddedServiceNode n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( InstallFixedVariableExpressionNode n );

		default OLSyntaxNode visit( InstallFixedVariableExpressionNode n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( VariablePathNode n );

		default OLSyntaxNode visit( VariablePathNode n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( TypeInlineDefinition n );

		default OLSyntaxNode visit( TypeInlineDefinition n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( TypeDefinitionLink n );

		default OLSyntaxNode visit( TypeDefinitionLink n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( InterfaceDefinition n );

		default OLSyntaxNode visit( InterfaceDefinition n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( DocumentationComment n );

		default OLSyntaxNode visit( DocumentationComment n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( FreshValueExpressionNode n );

		default OLSyntaxNode visit( FreshValueExpressionNode n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( CourierDefinitionNode n );

		default OLSyntaxNode visit( CourierDefinitionNode n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( CourierChoiceStatement n );

		default OLSyntaxNode visit( CourierChoiceStatement n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( NotificationForwardStatement n );

		default OLSyntaxNode visit( NotificationForwardStatement n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( SolicitResponseForwardStatement n );

		default OLSyntaxNode visit( SolicitResponseForwardStatement n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( InterfaceExtenderDefinition n );

		default OLSyntaxNode visit( InterfaceExtenderDefinition n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( InlineTreeExpressionNode n );

		default OLSyntaxNode visit( InlineTreeExpressionNode n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( VoidExpressionNode n );

		default OLSyntaxNode visit( VoidExpressionNode n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( ProvideUntilStatement n );

		default OLSyntaxNode visit( ProvideUntilStatement n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( TypeChoiceDefinition n );

		default OLSyntaxNode visit( TypeChoiceDefinition n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( ImportStatement n );

		default OLSyntaxNode visit( ImportStatement n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( ServiceNode n );

		default OLSyntaxNode visit( ServiceNode n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( EmbedServiceNode n );

		default OLSyntaxNode visit( EmbedServiceNode n, Unit ctx ) {
			return visit( n );
		}

		OLSyntaxNode visit( SolicitResponseExpression n );

		default OLSyntaxNode visit( SolicitResponseExpression n, Unit ctx ) {
			return visit( n );
		}
	}

	public static Program run( Program program ) {
		return new TranslationVisitor().translate( program );
	}
}
