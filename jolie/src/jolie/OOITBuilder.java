/***************************************************************************
 *   Copyright (C) 2006-2015 by Fabrizio Montesi <famontesi@gmail.com>     *
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

package jolie;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;
import jolie.lang.Constants;
import jolie.lang.Constants.ExecutionMode;
import jolie.lang.Constants.OperandType;
import jolie.lang.parse.CorrelationFunctionInfo;
import jolie.lang.parse.CorrelationFunctionInfo.CorrelationPairInfo;
import jolie.lang.parse.OLParser;
import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.Scanner;
import jolie.lang.parse.ast.AddAssignStatement;
import jolie.lang.parse.ast.AssignStatement;
import jolie.lang.parse.ast.CompareConditionNode;
import jolie.lang.parse.ast.CompensateStatement;
import jolie.lang.parse.ast.CorrelationSetInfo;
import jolie.lang.parse.ast.CorrelationSetInfo.CorrelationVariableInfo;
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
import jolie.lang.parse.ast.OperationDeclaration;
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
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import jolie.lang.parse.ast.types.TypeInlineDefinition;
import jolie.lang.parse.context.ParsingContext;
import jolie.net.AggregatedOperation;
import jolie.net.ext.CommProtocolFactory;
import jolie.net.ports.InputPort;
import jolie.net.ports.Interface;
import jolie.net.ports.InterfaceExtender;
import jolie.net.ports.OutputPort;
import jolie.behaviours.AddAssignmentBehaviour;
import jolie.behaviours.AssignmentBehaviour;
import jolie.behaviours.CallBehaviour;
import jolie.behaviours.CompensateBehaviour;
import jolie.behaviours.CurrentHandlerBehaviour;
import jolie.behaviours.DeepCopyBehaviour;
import jolie.behaviours.DefinitionBehaviour;
import jolie.behaviours.DivideAssignmentBehaviour;
import jolie.behaviours.ExitBehaviour;
import jolie.behaviours.ForEachBehaviour;
import jolie.behaviours.ForBehaviour;
import jolie.behaviours.IfBehaviour;
import jolie.behaviours.InitDefinitionBehaviour;
import jolie.behaviours.InstallBehaviour;
import jolie.behaviours.LinkInBehaviour;
import jolie.behaviours.LinkOutBehaviour;
import jolie.behaviours.MakePointerBehaviour;
import jolie.behaviours.MultiplyAssignmentBehaviour;
import jolie.behaviours.NDChoiceBehaviour;
import jolie.behaviours.NotificationBehaviour;
import jolie.behaviours.NullBehaviour;
import jolie.behaviours.OneWayBehaviour;
import jolie.behaviours.ParallelBehaviour;
import jolie.behaviours.PostDecrementBehaviour;
import jolie.behaviours.PostIncrementBehaviour;
import jolie.behaviours.PreDecrementBehaviour;
import jolie.behaviours.PreIncrementBehaviour;
import jolie.behaviours.ProvideUntilBehaviour;
import jolie.behaviours.RequestResponseBehaviour;
import jolie.behaviours.RunBehaviour;
import jolie.behaviours.ScopeBehaviour;
import jolie.behaviours.SequentialBehaviour;
import jolie.behaviours.SolicitResponseBehaviour;
import jolie.behaviours.SpawnBehaviour;
import jolie.behaviours.SubtractAssignmentBehaviour;
import jolie.behaviours.SynchronizedBehaviour;
import jolie.behaviours.ThrowBehaviour;
import jolie.behaviours.UndefBehaviour;
import jolie.behaviours.WhileBehaviour;
import jolie.process.courier.ForwardNotificationProcess;
import jolie.process.courier.ForwardSolicitResponseProcess;
import jolie.runtime.ClosedVariablePath;
import jolie.runtime.CompareOperators;
import jolie.runtime.GlobalVariablePath;
import jolie.runtime.InstallFixedVariablePath;
import jolie.runtime.InvalidIdException;
import jolie.runtime.OneWayOperation;
import jolie.runtime.RequestResponseOperation;
import jolie.runtime.Value;
import jolie.runtime.ValueVectorSizeExpression;
import jolie.runtime.VariablePath;
import jolie.runtime.VariablePathBuilder;
import jolie.runtime.correlation.CorrelationSet;
import jolie.runtime.correlation.CorrelationSet.CorrelationPair;
import jolie.runtime.embedding.EmbeddedServiceLoader;
import jolie.runtime.embedding.EmbeddedServiceLoader.EmbeddedServiceConfiguration;
import jolie.runtime.embedding.EmbeddedServiceLoaderCreationException;
import jolie.runtime.expression.AndCondition;
import jolie.runtime.expression.CastBoolExpression;
import jolie.runtime.expression.CastDoubleExpression;
import jolie.runtime.expression.CastIntExpression;
import jolie.runtime.expression.CastLongExpression;
import jolie.runtime.expression.CastStringExpression;
import jolie.runtime.expression.CompareCondition;
import jolie.runtime.expression.Expression;
import jolie.runtime.expression.Expression.Operand;
import jolie.runtime.expression.FreshValueExpression;
import jolie.runtime.expression.InlineTreeExpression;
import jolie.runtime.expression.InstanceOfExpression;
import jolie.runtime.expression.IsBoolExpression;
import jolie.runtime.expression.IsDefinedExpression;
import jolie.runtime.expression.IsDoubleExpression;
import jolie.runtime.expression.IsIntExpression;
import jolie.runtime.expression.IsLongExpression;
import jolie.runtime.expression.IsStringExpression;
import jolie.runtime.expression.NotExpression;
import jolie.runtime.expression.OrCondition;
import jolie.runtime.expression.ProductExpression;
import jolie.runtime.expression.SumExpression;
import jolie.runtime.expression.VoidExpression;
import jolie.runtime.typing.OneWayTypeDescription;
import jolie.runtime.typing.RequestResponseTypeDescription;
import jolie.runtime.typing.Type;
import jolie.util.ArrayListMultiMap;
import jolie.util.MultiMap;
import jolie.util.Pair;
import jolie.behaviours.Behaviour;
import jolie.behaviours.InputOperationBehaviour;

/**
 * Builds an interpretation tree by visiting a Jolie abstract syntax tree.
 * @author Fabrizio Montesi
 * @see OLVisitor
 */
public class OOITBuilder implements OLVisitor
{
	private final Program program;
	private boolean valid = true;
	private final Interpreter interpreter;
	private String currentOutputPort = null;
	private Interface currentPortInterface = null;
	private final Map< String, Boolean > isConstantMap;
	private final Map< String, InputPort > inputPorts = new HashMap<>();
	private final List< Pair< Type.TypeLink, TypeDefinition > > typeLinks = new ArrayList<>();
	private final CorrelationFunctionInfo correlationFunctionInfo;
	private ExecutionMode executionMode = Constants.ExecutionMode.SINGLE;
	private boolean registerSessionStarters = false;
	private InputPort currCourierInputPort = null;
	private String currCourierOperationName = null;
	private final Map< String, Map< String, AggregationConfiguration > > aggregationConfigurations =
		new HashMap<>(); // Input port name -> (operation name -> aggregation configuration)
	private final Map< String, InterfaceExtender > interfaceExtenders =
		new HashMap<>();
	private final Deque< OLSyntaxNode > lazyVisits = new LinkedList<>();	
	private boolean firstPass = true;

	private static class AggregationConfiguration {
		private final OutputPort defaultOutputPort;
		private final Interface aggregatedInterface;
		private final InterfaceExtender interfaceExtender;
		
		public AggregationConfiguration(
			OutputPort defaultOutputPort,
			Interface aggregatedInterface,
			InterfaceExtender interfaceExtender
		) {
			this.defaultOutputPort = defaultOutputPort;
			this.aggregatedInterface = aggregatedInterface;
			this.interfaceExtender = interfaceExtender;
		}
	}

	/**
	 * Constructor.
	 * @param interpreter the Interpreter requesting the interpretation tree building
	 * @param program the Program to generate the interpretation tree from
	 * @param isConstantMap
	 * @param correlationFunctionInfo
	 * @see Program
	 */
	public OOITBuilder(
		Interpreter interpreter,
		Program program,
		Map< String, Boolean > isConstantMap,
		CorrelationFunctionInfo correlationFunctionInfo
	) {
		this.interpreter = interpreter;
		this.program = new Program( program.context() );
		this.isConstantMap = isConstantMap;
		this.correlationFunctionInfo = correlationFunctionInfo;

		Map< String, TypeDefinition > builtInTypes = OLParser.createTypeDeclarationMap( program.context() );
		this.program.children().addAll( builtInTypes.values() );
		this.program.children().addAll( program.children() );
	}
	
	private void error( ParsingContext context, String message )
	{
		valid = false;
		String s = context.sourceName() + ":" + context.line() + ": " + message;
		interpreter.logSevere( s );
	}
	
	private void error( ParsingContext context, Exception e )
	{
		valid = false;
		e.printStackTrace();
		error( context, e.getMessage() );
	}
	
	/**
	 * Launches the build process.
	 * 
	 * The Program passed to the constructor gets visited and the Interpreter 
	 * passed to the constructor is set with the necessary references
	 * to the interpretation tree.
	 * @return true if the build process is successful, false otherwise
	 */
	public boolean build()
	{
        visit( program );
		checkForInit();
		resolveTypeLinks();
		lazyVisits();
		buildCorrelationSets();
		
        return valid;
	}
	
	private void lazyVisits()
	{
		firstPass = false;
		OLSyntaxNode node;
		while( (node = lazyVisits.poll()) != null ) {
			node.accept( this );
		}
	}
	
	private void visitLater( OLSyntaxNode n )
	{
		lazyVisits.add( n );
	}

	private void checkForInit()
	{
		try {
			interpreter.getDefinition( "init" );
		} catch( InvalidIdException e ) {
			interpreter.register("init",
			new InitDefinitionBehaviour(
				new ScopeBehaviour( "main", new InstallBehaviour( StatefulContext.createDefaultFaultHandlers( interpreter ) ), false )
			));
		}
	}

	private void resolveTypeLinks()
	{
		Type type;
		for( Pair< Type.TypeLink, TypeDefinition > pair : typeLinks ) {
			type = types.get( pair.key().linkedTypeName() );
			pair.key().setLinkedType( type );
			if ( type == null ) {
				error( pair.value().context(), "type link to " + pair.key().linkedTypeName() + " cannot be resolved" );
			}
		}
	}

	private void buildCorrelationSets()
	{
		Interpreter.SessionStarter starter;
		Collection< String > operations;
		VariablePath sessionVariablePath;
		VariablePath messageVariablePath;
		CorrelationSet currCorrelationSet;
		Set< Interpreter.SessionStarter > starters = new HashSet<>();
		List< VariablePath > correlationVariablePaths;
		MultiMap< String, CorrelationPair > correlationMap;

		for( CorrelationSetInfo csetInfo : correlationFunctionInfo.correlationSets() ) {
			correlationVariablePaths = new ArrayList<>();
			correlationMap = new ArrayListMultiMap<>();

			for( CorrelationVariableInfo csetVariableInfo : csetInfo.variables() ) {
				sessionVariablePath = buildCorrelationVariablePath( csetVariableInfo.correlationVariablePath() );
				correlationVariablePaths.add( sessionVariablePath );
			}
			operations = correlationFunctionInfo.correlationSetOperations().get( csetInfo );
			for( String operationName : operations ) {
				for( CorrelationPairInfo pairInfo : correlationFunctionInfo.getOperationCorrelationPairs( operationName ) ) {
					sessionVariablePath = buildCorrelationVariablePath( pairInfo.sessionPath() );
					messageVariablePath = buildVariablePath( pairInfo.messagePath() );
					correlationMap.put(
						operationName,
						new CorrelationPair( sessionVariablePath, messageVariablePath )
					);
					starter = interpreter.getSessionStarter( operationName );
					if ( starter != null && starter.correlationInitializer() == null ) {
						starters.add( starter );
					}
				}
			}

			currCorrelationSet = new CorrelationSet( correlationVariablePaths, correlationMap );
			interpreter.addCorrelationSet( currCorrelationSet );
			for( Interpreter.SessionStarter initStarter : starters ) {
				initStarter.setCorrelationInitializer( currCorrelationSet );
			}
			starters.clear();
		}
	}
	
	public void visit( ExecutionInfo n )
	{
		executionMode = n.mode();
		interpreter.setExecutionMode( n.mode() );
	}

	public void visit( VariablePathNode n )
	{}
	
	public void visit( CorrelationSetInfo n )
	{
//		correlationSetInfoList.add( n );
	}

	public void visit( OutputPortInfo n )
	{
		final Behaviour protocolConfigurationProcess =
			( n.protocolConfiguration() != null ) ? buildProcess( n.protocolConfiguration() )
			: NullBehaviour.getInstance();
		
		final boolean isConstant = isConstantMap.computeIfAbsent( n.id(), k -> false );

		currentOutputPort = n.id();
		notificationTypes.put( currentOutputPort, new HashMap<>() );
		solicitResponseTypes.put( currentOutputPort, new HashMap<>() );
		for( OperationDeclaration decl : n.operations() ) {
			decl.accept( this );
		}
		currentOutputPort = null;

		interpreter.register( n.id(), new OutputPort(
				interpreter,
				n.id(),
				n.protocolId(),
				protocolConfigurationProcess,
				n.location(),
				getOutputPortInterface( n.id() ),
				isConstant
			)
		);
	}

	private Interface getOutputPortInterface( String outputPortName )
	{
		Map< String, OneWayTypeDescription > oneWayMap = notificationTypes.get( outputPortName );
		if ( oneWayMap == null ) {
			oneWayMap = new HashMap< String, OneWayTypeDescription >();
		}
		Map< String, RequestResponseTypeDescription > rrMap = solicitResponseTypes.get( outputPortName );
		if ( rrMap == null ) {
			rrMap = new HashMap< String, RequestResponseTypeDescription >();
		}
		return new Interface( oneWayMap, rrMap );
	}
	
	public void visit( EmbeddedServiceNode n )
	{
		try {
			final VariablePath path =
				n.portId() == null ? null
				: interpreter.getOutputPort( n.portId() ).locationVariablePath();

			final EmbeddedServiceConfiguration embeddedServiceConfiguration =
				n.type().equals( Constants.EmbeddedServiceType.INTERNAL )
				? new EmbeddedServiceLoader.InternalEmbeddedServiceConfiguration( n.servicePath(), (Program) n.program() )
				: new EmbeddedServiceLoader.ExternalEmbeddedServiceConfiguration( n.type(), n.servicePath() );

			interpreter.addEmbeddedServiceLoader(
				EmbeddedServiceLoader.create(
					interpreter,
					embeddedServiceConfiguration,
					path
				) );
		} catch( EmbeddedServiceLoaderCreationException e ) {
			error( n.context(), e );
		} catch( InvalidIdException e ) {
			error( n.context(), "could not find port " + n.portId() );
		}
	}
	
	private AggregationConfiguration getAggregationConfiguration( String inputPortName, String operationName )
	{
		Map< String, AggregationConfiguration > map = aggregationConfigurations.get( inputPortName );
		if ( map == null ) {
			return null;
		}
		return map.get( operationName );
	}
	
	private void putAggregationConfiguration( String inputPortName, String operationName, AggregationConfiguration configuration )
	{
		Map< String, AggregationConfiguration > map = aggregationConfigurations.get( inputPortName );
		if ( map == null ) {
			map = new HashMap< String, AggregationConfiguration >();
			aggregationConfigurations.put( inputPortName, map );
		}
		map.put( operationName, configuration );
	}

	public void visit( InputPortInfo n )
	{
		currentPortInterface = new Interface(
			new HashMap< String, OneWayTypeDescription >(),
			new HashMap< String, RequestResponseTypeDescription >()
		);
		for( OperationDeclaration op : n.operations() ) {
			op.accept( this );
		}
		
		Map< String, OutputPort > redirectionMap =
			new HashMap< String, OutputPort > ();
		OutputPort oPort = null;
		for( Entry< String, String > entry : n.redirectionMap().entrySet() ) {
			try {
				oPort = interpreter.getOutputPort( entry.getValue() );
			} catch( InvalidIdException e ) {
				error( n.context(), "Unknown output port (" + entry.getValue() + ") in redirection for input port " + n.id() );
			}
			redirectionMap.put( entry.getKey(), oPort );
		}

		OutputPort outputPort;
		Map< String, OneWayTypeDescription > outputPortNotificationTypes;
		Map< String, RequestResponseTypeDescription > outputPortSolicitResponseTypes;
		Map< String, AggregatedOperation > aggregationMap = new HashMap< String, AggregatedOperation >();
		InterfaceExtender extender;
		for( InputPortInfo.AggregationItemInfo item : n.aggregationList() ) {
			String outputPortName = item.outputPortList()[0];
			if ( item.interfaceExtender() == null ) {
				extender = null;
			} else {
				extender = interfaceExtenders.get( item.interfaceExtender().name() );
			}
			try {
				outputPort = interpreter.getOutputPort( outputPortName );
				outputPortNotificationTypes = notificationTypes.get( outputPortName );
				outputPortSolicitResponseTypes = solicitResponseTypes.get( outputPortName );
				for( String operationName : outputPortNotificationTypes.keySet() ) {
					aggregationMap.put( operationName, AggregatedOperation.createDirect( operationName, Constants.OperationType.ONE_WAY, outputPort ) );
					putAggregationConfiguration( n.id(), operationName,
						new AggregationConfiguration( outputPort, outputPort.getInterface(), extender ) );
				}
				for( String operationName : outputPortSolicitResponseTypes.keySet() ) {
					aggregationMap.put( operationName, AggregatedOperation.createDirect( operationName, Constants.OperationType.REQUEST_RESPONSE, outputPort ) );
					putAggregationConfiguration( n.id(), operationName,
						new AggregationConfiguration( outputPort, outputPort.getInterface(), extender ) );
				}
			} catch( InvalidIdException e ) {
				error( n.context(), e );
			}
		}
		
		String pId = n.protocolId();
		CommProtocolFactory protocolFactory = null;

		VariablePath protocolConfigurationPath =
			new VariablePathBuilder( true )
			.add( Constants.INPUT_PORTS_NODE_NAME, 0 )
			.add( n.id(), 0 )
			.add( Constants.PROTOCOL_NODE_NAME, 0 )
			.toVariablePath();
		try {
			protocolFactory = interpreter.commCore().getCommProtocolFactory( pId );
		} catch( IOException e ) {
			error( n.context(), e );
		}

		VariablePath locationPath =
			new VariablePathBuilder( true )
			.add( Constants.INPUT_PORTS_NODE_NAME, 0 )
			.add( n.id(), 0 )
			.add( Constants.LOCATION_NODE_NAME, 0 )
			.toVariablePath();
		locationPath = new ClosedVariablePath( locationPath, interpreter.globalValue() );
		// Process assignLocation = new AssignmentProcess( locationPath, Value.create( n.location().toString() ) );
		locationPath.getValue().setValue( n.location().toString() );

		VariablePath protocolPath =
			new VariablePathBuilder( true )
			.add( Constants.INPUT_PORTS_NODE_NAME, 0 )
			.add( n.id(), 0 )
			.add( Constants.PROTOCOL_NODE_NAME, 0 )
			.toVariablePath();
		Behaviour assignProtocol = new AssignmentBehaviour( protocolPath, Value.create( n.protocolId() ) );
		Behaviour[] confChildren = new Behaviour[] { assignProtocol, buildProcess( n.protocolConfiguration() ) };
		SequentialBehaviour protocolConfigurationSequence = new SequentialBehaviour( confChildren );

		InputPort inputPort = new InputPort(
			n.id(),
			locationPath,
			protocolConfigurationPath,
			currentPortInterface,
			aggregationMap,
			redirectionMap
		);
		
		if ( n.location().toString().equals( Constants.LOCAL_LOCATION_KEYWORD ) ) {
			try {
				interpreter.commCore().addLocalInputPort( inputPort );
				inputPorts.put( inputPort.name(), inputPort );
			} catch( IOException e ) {
				error( n.context(), e );
			}
		} else if ( protocolFactory != null || n.location().getScheme().equals( Constants.LOCAL_LOCATION_KEYWORD ) ) {
			try {
				interpreter.commCore().addInputPort(
					inputPort,
					protocolFactory,
					protocolConfigurationSequence
				);
				inputPorts.put( inputPort.name(), inputPort );
			} catch( IOException ioe ) {
				error( n.context(), ioe );
			}
		} else {
			error( n.context(), "Communication protocol extension for protocol " + pId + " not found." );
		}
		currentPortInterface = null;
	}

	private Behaviour currProcess;
	private Expression currExpression;
	private Type currType;
	boolean insideType = false;
	
	private final Map< String, Type > types = new HashMap< String, Type >();
	private final Map< String, Map< String, OneWayTypeDescription > > notificationTypes =
		new HashMap< String, Map< String, OneWayTypeDescription > >(); // Maps output ports to their OW operation types
	private final Map< String, Map< String, RequestResponseTypeDescription > > solicitResponseTypes =
		new HashMap< String, Map< String, RequestResponseTypeDescription > >(); // Maps output ports to their RR operation types

	public void visit( TypeInlineDefinition n )
	{
		boolean backupInsideType = insideType;
		insideType = true;

		if ( n.untypedSubTypes() ) {
			currType = Type.create( n.nativeType(), n.cardinality(), true, null );
		} else {
			Map< String, Type > subTypes = new HashMap< String, Type >();
			if ( n.subTypes() != null ) {
				for( Entry< String, TypeDefinition > entry : n.subTypes() ) {
					subTypes.put( entry.getKey(), buildType( entry.getValue() ) );
				}
			}
			currType = Type.create(
				n.nativeType(),
				n.cardinality(),
				false,
				subTypes
			);
		}

		insideType = backupInsideType;

		if ( insideType == false && insideOperationDeclaration == false ) {
			types.put( n.id(), currType );
		}
	}

	public void visit( TypeDefinitionLink n )
	{
		Type.TypeLink link = Type.createLink( n.linkedTypeName(), n.cardinality() );
		currType = link;
		typeLinks.add( new Pair<>( link, n ) );

		if ( insideType == false && insideOperationDeclaration == false ) {
			types.put( n.id(), currType );
		}
	}

	public void visit( Program p )
	{
		for( OLSyntaxNode node : p.children() )
			node.accept( this );
	}

	private boolean insideOperationDeclaration = false;
	
	private OneWayTypeDescription buildOneWayTypeDescription( OneWayOperationDeclaration decl )
	{
		if ( decl == null ) {
			return null;
		}
		
		if ( currentOutputPort == null ) { // We are in an input port (TODO: why does this matter? junk code?)
			return new OneWayTypeDescription( types.get( decl.requestType().id() ) );
		} else {
			return new OneWayTypeDescription( buildType( decl.requestType() ) );
		}
	}
	
	private RequestResponseTypeDescription buildRequestResponseTypeDescription( RequestResponseOperationDeclaration decl )
	{
		if ( decl == null ) {
			return null;
		}
		
		RequestResponseTypeDescription typeDescription;
		Map< String, Type > faults = new HashMap< String, Type >();
		if ( currentOutputPort == null ) { // We are in an input port (TODO: why does this matter? junk code?)
			for( Entry< String, TypeDefinition > entry : decl.faults().entrySet() ) {
				faults.put( entry.getKey(), types.get( entry.getValue().id() ) );
			}
			typeDescription = new RequestResponseTypeDescription(
				types.get( decl.requestType().id() ),
				types.get( decl.responseType().id() ),
				faults
			);
		} else {
			for( Entry< String, TypeDefinition > entry : decl.faults().entrySet() ) {
				faults.put( entry.getKey(), types.get( entry.getValue().id() ) );
			}
			typeDescription = new RequestResponseTypeDescription(
				buildType( decl.requestType() ),
				buildType( decl.responseType() ),
				faults
			);
		}
		return typeDescription;
	}

	public void visit( OneWayOperationDeclaration decl )
	{
		boolean backup = insideOperationDeclaration;
		insideOperationDeclaration = true;
		OneWayTypeDescription typeDescription;
		if ( currentOutputPort == null ) { // We are in an input port
			// Register if not already present
			typeDescription = buildOneWayTypeDescription( decl );
			try {
				interpreter.getOneWayOperation( decl.id() );
			} catch( InvalidIdException e ) {
				interpreter.register( decl.id(), new OneWayOperation( decl.id(), types.get( decl.requestType().id() ) ) );
			}
		} else {
			typeDescription = buildOneWayTypeDescription( decl );
			notificationTypes.get( currentOutputPort ).put( decl.id(), typeDescription );
		}

		if ( currentPortInterface != null ) {
			currentPortInterface.oneWayOperations().put( decl.id(), typeDescription );
		}

		insideOperationDeclaration = backup;
	}

	public void visit( RequestResponseOperationDeclaration decl )
	{
		RequestResponseTypeDescription typeDescription;
		if ( currentOutputPort == null ) {
			// Register if not already present
			try {
				final RequestResponseOperation op = interpreter.getRequestResponseOperation( decl.id() );
				typeDescription = op.typeDescription();
			} catch( InvalidIdException e ) {
				typeDescription = buildRequestResponseTypeDescription( decl );
				interpreter.register(
					decl.id(),
					new RequestResponseOperation( decl.id(), typeDescription )
				);
			}
		} else {
			typeDescription = buildRequestResponseTypeDescription( decl );
			solicitResponseTypes.get( currentOutputPort ).put( decl.id(), typeDescription );
		}

		if ( currentPortInterface != null ) {
			currentPortInterface.requestResponseOperations().put( decl.id(), typeDescription );
		}
	}
	
	public void visit( DefinitionNode n )
	{
		final DefinitionBehaviour def;
		
		switch( n.id() ) {
		case "main":
			switch( executionMode ) {
			case SINGLE:
				// We are a single-session service, so we will not spawn sessions
				registerSessionStarters = false;
				def = new DefinitionBehaviour( buildProcess( n.body() ) );
				break;
			default:
				registerSessionStarters = true;
				def = new DefinitionBehaviour( buildProcess( n.body() ) );
				registerSessionStarters = false;
				break;
			}
			break;
		case "init":
			final Behaviour[] initChildren = {
				new InstallBehaviour( StatefulContext.createDefaultFaultHandlers( interpreter ) ),
				buildProcess( n.body() )
			};
			def = new InitDefinitionBehaviour( new ScopeBehaviour( "main", new SequentialBehaviour( initChildren ), false ) );
			break;
		default:
			def = new DefinitionBehaviour( buildProcess( n.body() ) );
			break;
		}

		interpreter.register( n.id(), def );
	}

	public void visit( ParallelStatement n )
	{
		Behaviour[] children = new Behaviour[ n.children().size() ];
		int i = 0;
		for( OLSyntaxNode node : n.children() ) {
			node.accept( this );
			children[ i++ ] = currProcess;
		}
		currProcess = new ParallelBehaviour( children );
	}
	
	public void visit( SynchronizedStatement n )
	{
		n.body().accept( this );
		currProcess = new SynchronizedBehaviour( n.id(), currProcess );
	}

	@Override
	public void visit( SequenceStatement n )
	{
		final boolean origRegisterSessionStarters = registerSessionStarters;
		registerSessionStarters = false;

		final List< Behaviour > children = new ArrayList<>( n.children().size() );
		n.children().forEach( ( child ) -> {
			children.add( buildProcess( child ) );
		} );
		currProcess = new SequentialBehaviour( children.toArray(new Behaviour[0] ) );

		if ( origRegisterSessionStarters && children.get( 0 ) instanceof InputOperationBehaviour ) {
			// We must register this sequence as a starter guarded by the first input process
			InputOperationBehaviour first = (InputOperationBehaviour) children.remove( 0 );
			registerSessionStarter(first,
				new SequentialBehaviour( children.toArray(new Behaviour[0] ) )
			);
		}

		registerSessionStarters = origRegisterSessionStarters;
	}

	private void registerSessionStarter( InputOperationBehaviour guard, Behaviour body )
	{
		guard.setSessionStarter( true );
		interpreter.registerSessionStarter( guard, body );
	}

	public void visit( NDChoiceStatement n )
	{
		boolean origRegisterSessionStarters = registerSessionStarters;
		registerSessionStarters = false;

		List< Pair< InputOperationBehaviour, Behaviour > > branches =
					new ArrayList<>( n.children().size() );
		InputOperationBehaviour guard;
		for( Pair< OLSyntaxNode, OLSyntaxNode > pair : n.children() ) {
			pair.key().accept( this );
			try {
				guard = (InputOperationBehaviour) currProcess;
				pair.value().accept( this );
				branches.add(new Pair< InputOperationBehaviour, Behaviour >( guard, currProcess )
				);
				if ( origRegisterSessionStarters ) {
					registerSessionStarter( guard, currProcess );
				}
			} catch( Exception e ) {
				e.printStackTrace();
			}
		}
		
		currProcess = new NDChoiceBehaviour( branches.toArray( new Pair[0] ) );
		
		registerSessionStarters = origRegisterSessionStarters;
	}
		
	public void visit( OneWayOperationStatement n )
	{
		boolean origRegisterSessionStarters = registerSessionStarters;
		registerSessionStarters = false;

		try {
			InputOperationBehaviour inputProcess;
			currProcess = inputProcess =
				new OneWayBehaviour(
					interpreter.getOneWayOperation( n.id() ),
					buildVariablePath( n.inputVarPath() )
				);
			if ( origRegisterSessionStarters ) {
				registerSessionStarter(inputProcess, NullBehaviour.getInstance() );
			}
		} catch( InvalidIdException e ) {
			error( n.context(), e ); 
		}

		registerSessionStarters = origRegisterSessionStarters;
	}

	public void visit( RequestResponseOperationStatement n )
	{
		boolean origRegisterSessionStarters = registerSessionStarters;
		registerSessionStarters = false;
		
		Expression outputExpression = null;
		if ( n.outputExpression() != null ) {
			n.outputExpression().accept( this );
			outputExpression = currExpression;
		}
		try {
			InputOperationBehaviour inputProcess;
			currProcess = inputProcess =
				new RequestResponseBehaviour(
						interpreter.getRequestResponseOperation( n.id() ),
						buildVariablePath( n.inputVarPath() ),
						outputExpression,
						buildProcess( n.process() )
						);
			if ( origRegisterSessionStarters ) {
				registerSessionStarter(inputProcess, NullBehaviour.getInstance() );
			}
		} catch( InvalidIdException e ) {
			error( n.context(), e ); 
		}

		registerSessionStarters = origRegisterSessionStarters;
	}
		
	public void visit( NotificationOperationStatement n )
	{
		Expression outputExpression = null;
		if ( n.outputExpression() != null ) {
			n.outputExpression().accept( this );
			outputExpression = currExpression;
		}
		try {
			currProcess =
				new NotificationBehaviour(
						n.id(),
						interpreter.getOutputPort( n.outputPortId() ),
						outputExpression,
						notificationTypes.get( n.outputPortId() ).get( n.id() )
					);
		} catch( InvalidIdException e ) {
			error( n.context(), e );
		}
	}
		
	public void visit( SolicitResponseOperationStatement n )
	{
		try {
			Behaviour installProcess = NullBehaviour.getInstance();
			if ( n.handlersFunction() != null )
				installProcess = new InstallBehaviour( getHandlersFunction( n.handlersFunction() ) );
			currProcess =
				new SolicitResponseBehaviour(
						n.id(),
						interpreter.getOutputPort( n.outputPortId() ),
						buildExpression( n.outputExpression() ),
						buildVariablePath( n.inputVarPath() ),
						installProcess,
						solicitResponseTypes.get( n.outputPortId() ).get( n.id() )
					);
		} catch( InvalidIdException e ) {
			error( n.context(), e );
		}
	}
		
	public void visit( LinkInStatement n )
	{
		currProcess = new LinkInBehaviour( n.id() );
	}
	
	public void visit( LinkOutStatement n )
	{
		currProcess = new LinkOutBehaviour( n.id() );
	}
	
	public void visit( ThrowStatement n )
	{
		Expression expression = null;
		if ( n.expression() != null ) {
			n.expression().accept( this );
			expression = currExpression;
		}
		currProcess = new ThrowBehaviour( n.id(), expression );
	}
	
	public void visit( CompensateStatement n )
	{
		currProcess = new CompensateBehaviour( n.id() );
	}
		
	public void visit( Scope n )
	{
		n.body().accept( this );
		currProcess = new ScopeBehaviour( n.id(), currProcess );
	}
		
	public void visit( InstallStatement n )
	{
		currProcess = new InstallBehaviour( getHandlersFunction( n.handlersFunction() ) );
	}
	
	private List< Pair< String, Behaviour > > getHandlersFunction( InstallFunctionNode n )
	{
		List< Pair< String, Behaviour > > pairs = new ArrayList<>( n.pairs().length );
		for( Pair< String, OLSyntaxNode > pair : n.pairs() ) {
			pair.value().accept( this );
			pairs.add(new Pair< String, Behaviour >( pair.key(), currProcess ) );
		}
		return pairs;
	}
			
	public void visit( AssignStatement n )
	{
		n.expression().accept( this );
			
		AssignmentBehaviour p = 
			new AssignmentBehaviour(
				buildVariablePath( n.variablePath() ),
				currExpression
				);
		currProcess = p;
		currExpression = p;
	}

	public void visit( AddAssignStatement n )
	{
		n.expression().accept( this );

		AddAssignmentBehaviour p =
			new AddAssignmentBehaviour(
			buildVariablePath( n.variablePath() ),
			currExpression );
		currProcess = p;
		currExpression = p;
	}

	public void visit( SubtractAssignStatement n )
	{
		n.expression().accept( this );

		SubtractAssignmentBehaviour p =
			new SubtractAssignmentBehaviour(
			buildVariablePath( n.variablePath() ),
			currExpression );
		currProcess = p;
		currExpression = p;
	}

	public void visit( MultiplyAssignStatement n )
	{
		n.expression().accept( this );

		MultiplyAssignmentBehaviour p =
			new MultiplyAssignmentBehaviour(
			buildVariablePath( n.variablePath() ),
			currExpression );
		currProcess = p;
		currExpression = p;
	}

	public void visit( DivideAssignStatement n )
	{
		n.expression().accept( this );

		DivideAssignmentBehaviour p =
			new DivideAssignmentBehaviour(
			buildVariablePath( n.variablePath() ),
			currExpression );
		currProcess = p;
		currExpression = p;
	}

	private VariablePath buildCorrelationVariablePath( VariablePathNode path )
	{
		VariablePathNode csetVarPathNode = new VariablePathNode( path.context(), VariablePathNode.Type.CSET );
		csetVarPathNode.append( new Pair<OLSyntaxNode, OLSyntaxNode>(
			new ConstantStringExpression( path.context(), Constants.CSETS ),
			new ConstantIntegerExpression( path.context(), 0 )
		));
		csetVarPathNode.path().addAll( path.path() );
		return buildVariablePath( csetVarPathNode );
	}
	
	private VariablePath buildVariablePath( VariablePathNode path )
	{
		if ( path == null )
			return null;

		final Expression backupExpr = currExpression;

		Pair< Expression, Expression >[] internalPath = new Pair[ path.path().size() ];
		int i = 0;
		for( Pair< OLSyntaxNode, OLSyntaxNode > pair : path.path() ) {
			pair.key().accept( this );
			Expression keyExpr = currExpression;
			if ( pair.value() != null ) {
				pair.value().accept( this );
			} else {
				currExpression = null;
			}
			internalPath[ i++ ] = new Pair<>( keyExpr, currExpression );
		}
		
		currExpression = backupExpr;

		return
			path.isGlobal() ?
				new GlobalVariablePath( internalPath )
			:
				new VariablePath( internalPath );
	}

	public void visit( PointerStatement n )
	{
		currProcess =
			new MakePointerBehaviour(
				buildVariablePath( n.leftPath() ),
				buildVariablePath( n.rightPath() )
			);
	}

	public void visit( DeepCopyStatement n )
	{
		currProcess =
			new DeepCopyBehaviour(
				buildVariablePath( n.leftPath() ),
				buildExpression( n.rightExpression() )
			);
	}

	public void visit( IfStatement n )
	{
		IfBehaviour.CPPair[] pairs = new IfBehaviour.CPPair[ n.children().size() ];
		Behaviour elseProcess = null;

		Expression condition;
		int i = 0;
		for( Pair< OLSyntaxNode, OLSyntaxNode > pair : n.children() ) {
			pair.key().accept( this );
			condition = currExpression;
			pair.value().accept( this );
			pairs[ i++ ] = new IfBehaviour.CPPair( condition, currProcess );
		}
		
		if ( n.elseProcess() != null ) {
			n.elseProcess().accept( this );
			elseProcess = currProcess;
		}
		
		currProcess = new IfBehaviour( pairs, elseProcess );
	}

	public void visit( CurrentHandlerStatement n )
	{
		currProcess = CurrentHandlerBehaviour.getInstance();
	}

	public void visit( DefinitionCallStatement n )
	{
		currProcess = new CallBehaviour( n.id() );
	}

	public void visit( RunStatement n )
	{
		n.expression().accept( this );
		currProcess = new RunBehaviour( currExpression );
	}

	public void visit( WhileStatement n )
	{
		n.condition().accept( this );
		Expression condition = currExpression;
		n.body().accept( this );
		currProcess = new WhileBehaviour( condition, currProcess );
	}
	
	public void visit( OrConditionNode n )
	{
		Expression[] children = new Expression[ n.children().size() ];
		int i = 0;
		for( OLSyntaxNode node : n.children() ) {
			node.accept( this );
			children[ i++ ] = currExpression;
		}
		currExpression = new OrCondition( children );
	}
	
	public void visit( AndConditionNode n )
	{
		Expression[] children = new Expression[ n.children().size() ];
		int i = 0;
		for( OLSyntaxNode node : n.children() ) {
			node.accept( this );
			children[ i++ ] = currExpression;
		}
		currExpression = new AndCondition( children );
	}
	
	public void visit( NotExpressionNode n )
	{
		n.expression().accept( this );
		currExpression = new NotExpression( currExpression );
	}
	
	public void visit( CompareConditionNode n )
	{
		n.leftExpression().accept( this );
		Expression left = currExpression;
		n.rightExpression().accept( this );
		Scanner.TokenType opType = n.opType();

		final BiPredicate< Value, Value > operator =
			opType == Scanner.TokenType.EQUAL ? CompareOperators.EQUAL
			: opType == Scanner.TokenType.NOT_EQUAL ? CompareOperators.NOT_EQUAL
			: opType == Scanner.TokenType.LANGLE ? CompareOperators.MINOR
			: opType == Scanner.TokenType.RANGLE ? CompareOperators.MAJOR
			: opType == Scanner.TokenType.MINOR_OR_EQUAL ? CompareOperators.MINOR_OR_EQUAL
			: opType == Scanner.TokenType.MAJOR_OR_EQUAL ? CompareOperators.MAJOR_OR_EQUAL
			: null;
		Objects.requireNonNull( operator );
		currExpression = new CompareCondition( left, currExpression, operator );
	}

	public void visit( FreshValueExpressionNode n )
	{
		currExpression = FreshValueExpression.getInstance();
	}
	
	public void visit( ConstantIntegerExpression n )
	{
		currExpression = Value.create( n.value() );
	}
	
	public void visit( ConstantLongExpression n )
	{
		currExpression = Value.create( n.value() );
	}
	
	public void visit( ConstantBoolExpression n )
	{
		currExpression = Value.create( n.value() );
	}
	
	public void visit( ConstantDoubleExpression n )
	{
		currExpression = Value.create( n.value() );
	}
	
	public void visit( ConstantStringExpression n )
	{
		currExpression = Value.create( n.value() );
	}
	
	public void visit( ProductExpressionNode n )
	{
		Operand[] operands = new Operand[ n.operands().size() ];
		int i = 0;
		for( Pair< OperandType, OLSyntaxNode > pair : n.operands() ) {
			pair.value().accept( this );
			operands[i++] = new Operand( pair.key(), currExpression );
		}

		currExpression = new ProductExpression( operands );
	}
	
	public void visit( SumExpressionNode n )
	{
		Operand[] operands = new Operand[ n.operands().size() ];
		int i = 0;
		for( Pair< OperandType, OLSyntaxNode > pair : n.operands() ) {
			pair.value().accept( this );
			operands[i++] = new Operand( pair.key(), currExpression );
		}

		currExpression = new SumExpression( operands );
	}

	public void visit( VariableExpressionNode n )
	{
		currExpression = buildVariablePath( n.variablePath() );
	}
	
	public void visit( InstallFixedVariableExpressionNode n )
	{
		currExpression =
				new InstallFixedVariablePath(
					buildVariablePath( n.variablePath() )
				);
	}
	
	public void visit( NullProcessStatement n )
	{
		currProcess = NullBehaviour.getInstance();
	}
	
	public void visit( ExitStatement n )
	{
		currProcess = ExitBehaviour.getInstance();
	}
	
	public void visit( VoidExpressionNode n )
	{
		currExpression = new VoidExpression();
	}
	
	public void visit( ValueVectorSizeExpressionNode n )
	{
		currExpression = new ValueVectorSizeExpression( buildVariablePath( n.variablePath() ) );
	}
	
	public void visit( PreDecrementStatement n )
	{
		PreDecrementBehaviour p =
			new PreDecrementBehaviour( buildVariablePath( n.variablePath() ) );
		currProcess = p;
		currExpression = p; 
	}
	
	public void visit( PostDecrementStatement n )
	{
		PostDecrementBehaviour p =
			new PostDecrementBehaviour( buildVariablePath( n.variablePath() ) );
		currProcess = p;
		currExpression = p;
	}
	
	public void visit( IsTypeExpressionNode n )
	{
		IsTypeExpressionNode.CheckType type = n.type();
		if ( type == IsTypeExpressionNode.CheckType.DEFINED ) {
			currExpression =
				new IsDefinedExpression( buildVariablePath( n.variablePath() ) );
		} else if ( type == IsTypeExpressionNode.CheckType.INT ) {
			currExpression =
				new IsIntExpression( buildVariablePath( n.variablePath() ) );
		} else if ( type == IsTypeExpressionNode.CheckType.DOUBLE ) {
			currExpression =
				new IsDoubleExpression( buildVariablePath( n.variablePath() ) );
		} else if ( type == IsTypeExpressionNode.CheckType.BOOL ) {
			currExpression =
				new IsBoolExpression( buildVariablePath( n.variablePath() ) );
		} else if ( type == IsTypeExpressionNode.CheckType.LONG ) {
			currExpression =
				new IsLongExpression( buildVariablePath( n.variablePath() ) );
		} else if ( type == IsTypeExpressionNode.CheckType.STRING ) {
			currExpression =
				new IsStringExpression( buildVariablePath( n.variablePath() ) );
		}
	}
	
	public void visit( InlineTreeExpressionNode n )
	{
		Expression rootExpression = buildExpression( n.rootExpression() );

		Pair< VariablePath, Expression >[] assignments = new Pair[ n.assignments().length ];
		int i = 0;
		for( Pair< VariablePathNode, OLSyntaxNode > pair : n.assignments() ) {
			assignments[ i++ ] = new Pair< VariablePath, Expression >(
				buildVariablePath( pair.key() ),
				buildExpression( pair.value() )
			);
		}
		
		currExpression = new InlineTreeExpression( rootExpression, assignments );
	}
	
	public void visit( InstanceOfExpressionNode n )
	{
		currExpression = new InstanceOfExpression( buildExpression( n.expression() ), buildType( n.type() ) );
	}
	
	public void visit( TypeCastExpressionNode n )
	{
		n.expression().accept( this );
		switch( n.type() ) {
		case INT:
			currExpression = new CastIntExpression( currExpression );
			break;
		case DOUBLE:
			currExpression = new CastDoubleExpression( currExpression );
			break;
		case STRING:
			currExpression = new CastStringExpression( currExpression );
			break;
		case BOOL:
			currExpression = new CastBoolExpression( currExpression );
			break;
		case LONG:
			currExpression = new CastLongExpression( currExpression );
			break;
		}
	}
	
	public void visit( PreIncrementStatement n )
	{
		PreIncrementBehaviour p =
			new PreIncrementBehaviour( buildVariablePath( n.variablePath() ) );
		currProcess = p;
		currExpression = p; 
	}
	
	public void visit( PostIncrementStatement n )
	{
		PostIncrementBehaviour p =
			new PostIncrementBehaviour( buildVariablePath( n.variablePath() ) );
		currProcess = p;
		currExpression = p;
	}
	
	public void visit( ForStatement n )
	{
		n.init().accept( this );
		Behaviour init = currProcess;
		n.post().accept( this );
		Behaviour post = currProcess;
		n.condition().accept( this );
		Expression condition = currExpression;
		n.body().accept( this );
		currProcess = new ForBehaviour( init, condition, post, currProcess );
	}
	
	public void visit( ForEachStatement n )
	{
		n.body().accept( this );
		currProcess =
			new ForEachBehaviour(
				buildVariablePath( n.keyPath() ),
				buildVariablePath( n.targetPath() ),
				currProcess
				);
	}

	private Expression buildExpression( OLSyntaxNode n )
	{
		if ( n == null ) {
			return null;
		}
		n.accept( this );
		return currExpression;
	}

	private Behaviour buildProcess( OLSyntaxNode n )
	{
		if ( n == null ) {
			return null;
		}
		n.accept( this );
		return currProcess;
	}

	private Type buildType( OLSyntaxNode n )
	{
		if ( n == null ) {
			return null;
		}
		n.accept( this );
		return currType;
	}

	public void visit( SpawnStatement n )
	{
		Behaviour[] children = new Behaviour[2];
		children[ 0 ] = new InstallBehaviour( StatefulContext.createDefaultFaultHandlers( interpreter ) );
		children[ 1 ] = buildProcess( n.body() );
		currProcess = new SpawnBehaviour(
			buildVariablePath( n.indexVariablePath() ),
			buildExpression( n.upperBoundExpression() ),
			buildVariablePath( n.inVariablePath() ),
			new SequentialBehaviour( children )
		);
	}
	
	public void visit( UndefStatement n )
	{
		currProcess = new UndefBehaviour( buildVariablePath( n.variablePath() ) );
	}

	public void visit( InterfaceDefinition n ) {}
	public void visit( DocumentationComment n ) {}
	
	public void visit( InterfaceExtenderDefinition n )
	{
		Map< String, OneWayTypeDescription > oneWayDescs = new HashMap< String, OneWayTypeDescription >();
		Map< String, RequestResponseTypeDescription > rrDescs = new HashMap< String, RequestResponseTypeDescription >();
		for( Entry< String, OperationDeclaration > entry : n.operationsMap().entrySet() ) {
			if ( entry.getValue() instanceof OneWayOperationDeclaration ) {
				oneWayDescs.put( entry.getKey(), buildOneWayTypeDescription( (OneWayOperationDeclaration)entry.getValue() ) );
			} else { // Request-Response
				rrDescs.put( entry.getKey(), buildRequestResponseTypeDescription( (RequestResponseOperationDeclaration)entry.getValue() ) );
			}
		}
		
		InterfaceExtender extender = new InterfaceExtender(
			oneWayDescs,
			rrDescs,
			buildOneWayTypeDescription( n.defaultOneWayOperation() ),
			buildRequestResponseTypeDescription( n.defaultRequestResponseOperation() )
		);
		
		interfaceExtenders.put( n.name(), extender );
	}

	public void visit( CourierDefinitionNode n )
	{
		if ( firstPass ) {
			visitLater( n );
		} else {
			currCourierInputPort = inputPorts.get( n.inputPortName() );
			if ( currCourierInputPort == null ) {
				error( n.context(), "cannot find input port: " + n.inputPortName() );
			} else {
				n.body().accept( this );
				if ( currCourierInputPort.location().toString().equals( Constants.LOCAL_LOCATION_KEYWORD ) ) {
					interpreter.commCore().localListener().inputPort().aggregationMap().putAll( currCourierInputPort.aggregationMap() );
				}
			}
			currCourierInputPort = null;
		}
	}
	
	private OneWayOperation getExtendedOneWayOperation( String inputPortName, String operationName )
	{
		AggregationConfiguration conf = getAggregationConfiguration( inputPortName, operationName );
		OneWayTypeDescription desc = conf.aggregatedInterface.oneWayOperations().get( operationName );
		Type extenderType = null;
		if ( conf.interfaceExtender != null ) {
			OneWayTypeDescription extenderDesc = conf.interfaceExtender.getOneWayTypeDescription( operationName );
			if ( extenderDesc != null ) {
				extenderType = extenderDesc.requestType();
			}
		}

		return new OneWayOperation(
			operationName,
			Type.extend( desc.requestType(), extenderType )
		);
	}
	
	private RequestResponseOperation getExtendedRequestResponseOperation( String inputPortName, String operationName )
	{
		AggregationConfiguration conf = getAggregationConfiguration( inputPortName, operationName );
		RequestResponseTypeDescription desc = conf.aggregatedInterface.requestResponseOperations().get( operationName );

		Map< String, Type > extendedFaultMap = new HashMap< String, Type >();
		extendedFaultMap.putAll( desc.faults() );
		
		Type requestExtenderType = null;
		Type responseExtenderType = null;
		
		if ( conf.interfaceExtender != null ) {
			RequestResponseTypeDescription extenderDesc = conf.interfaceExtender.getRequestResponseTypeDescription( operationName );
			if ( extenderDesc != null ) {
				requestExtenderType = extenderDesc.requestType();
				responseExtenderType = extenderDesc.responseType();
				extendedFaultMap.putAll( extenderDesc.faults() );
			}
		}
		
		
		
		return new RequestResponseOperation(
			operationName,
			new RequestResponseTypeDescription(
				( requestExtenderType == null ) ?
					desc.requestType() : Type.extend( desc.requestType(), requestExtenderType ),
				( responseExtenderType == null ) ?
					desc.responseType() : Type.extend( desc.responseType(), responseExtenderType ),
				extendedFaultMap
			)
		);
	}

	public void visit( CourierChoiceStatement n )
	{
		for( CourierChoiceStatement.InterfaceOneWayBranch branch : n.interfaceOneWayBranches() ) {
			for( Map.Entry< String, OperationDeclaration > entry : branch.interfaceDefinition.operationsMap().entrySet() ) {
				if ( entry.getValue() instanceof OneWayOperationDeclaration ) {
					currCourierOperationName = entry.getKey();
					currCourierInputPort.aggregationMap().put(
						entry.getKey(),
						AggregatedOperation.createWithCourier(
							getExtendedOneWayOperation( currCourierInputPort.name(), currCourierOperationName ),
							buildVariablePath( branch.inputVariablePath ),
							buildProcess( branch.body )
						)
					);
				}
			}
		}
		
		for( CourierChoiceStatement.InterfaceRequestResponseBranch branch : n.interfaceRequestResponseBranches() ) {
			for( Map.Entry< String, OperationDeclaration > entry : branch.interfaceDefinition.operationsMap().entrySet() ) {
				if ( entry.getValue() instanceof RequestResponseOperationDeclaration ) {
					currCourierOperationName = entry.getKey();
					currCourierInputPort.aggregationMap().put(
						entry.getKey(),
						AggregatedOperation.createWithCourier(
							getExtendedRequestResponseOperation( currCourierInputPort.name(), currCourierOperationName ),
							buildVariablePath( branch.inputVariablePath ), buildVariablePath( branch.outputVariablePath ),
							buildProcess( branch.body )
						)
					);
				}
			}
		}
		
		for( CourierChoiceStatement.OperationOneWayBranch branch : n.operationOneWayBranches() ) {
			currCourierOperationName = branch.operation;
			currCourierInputPort.aggregationMap().put(
				branch.operation,
				AggregatedOperation.createWithCourier(
					getExtendedOneWayOperation( currCourierInputPort.name(), currCourierOperationName ),
					buildVariablePath( branch.inputVariablePath ),
					buildProcess( branch.body )
				)
			);
		}
		
		for( CourierChoiceStatement.OperationRequestResponseBranch branch : n.operationRequestResponseBranches() ) {
			currCourierOperationName = branch.operation;
			currCourierInputPort.aggregationMap().put(
				branch.operation,
				AggregatedOperation.createWithCourier(
					getExtendedRequestResponseOperation( currCourierInputPort.name(), currCourierOperationName ),
					buildVariablePath( branch.inputVariablePath ), buildVariablePath( branch.outputVariablePath ),
					buildProcess( branch.body )
				)
			);
		}
		
		currCourierOperationName = null;
	}

	public void visit( NotificationForwardStatement n )
	{
		AggregationConfiguration conf = getAggregationConfiguration( currCourierInputPort.name(), currCourierOperationName );
		try {
			OutputPort outputPort;
			if ( n.outputPortName() != null ) {
				outputPort = interpreter.getOutputPort( n.outputPortName() );
			} else {
				outputPort = conf.defaultOutputPort;
			}
			currProcess = new ForwardNotificationProcess(
				currCourierOperationName,
				outputPort,
				buildVariablePath( n.outputVariablePath() ),
				conf.aggregatedInterface.oneWayOperations().get( currCourierOperationName ),
				(conf.interfaceExtender == null) ? null : conf.interfaceExtender.getOneWayTypeDescription( currCourierOperationName )
			);
		} catch( InvalidIdException e ) {
			error( n.context(), e );
		}
	}
	
	public void visit( ProvideUntilStatement n )
	{
		currProcess = new ProvideUntilBehaviour( (NDChoiceBehaviour)buildProcess( n.provide() ), (NDChoiceBehaviour)buildProcess( n.until() ) );
	}

	@Override
	public void visit( TypeChoiceDefinition n )
	{
		final boolean wasInsideType = insideType;
		insideType = true;
		
		currType = Type.createChoice( n.cardinality(), buildType( n.left() ), buildType( n.right() ) );
		
		insideType = wasInsideType;		
		if ( insideType == false && insideOperationDeclaration == false ) {
			types.put( n.id(), currType );
		}
	}

	public void visit( SolicitResponseForwardStatement n )
	{
		AggregationConfiguration conf = getAggregationConfiguration( currCourierInputPort.name(), currCourierOperationName );
		try {
			OutputPort outputPort;
			if ( n.outputPortName() != null ) {
				outputPort = interpreter.getOutputPort( n.outputPortName() );
			} else {
				outputPort = conf.defaultOutputPort;
			}
			currProcess = new ForwardSolicitResponseProcess(
				currCourierOperationName,
				outputPort,
				buildVariablePath( n.outputVariablePath() ),
				buildVariablePath( n.inputVariablePath() ),
				conf.aggregatedInterface.requestResponseOperations().get( currCourierOperationName ),
				(conf.interfaceExtender == null) ? null : conf.interfaceExtender.getRequestResponseTypeDescription( currCourierOperationName )
			);
		} catch( InvalidIdException e ) {
			error( n.context(), e );
		}
	}
}

