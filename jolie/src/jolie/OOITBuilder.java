/***************************************************************************
 *   Copyright (C) 2006-2011 by Fabrizio Montesi <famontesi@gmail.com>     *
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import jolie.lang.Constants;
import jolie.lang.Constants.ExecutionMode;
import jolie.lang.Constants.OperandType;
import jolie.lang.NativeType;
import jolie.lang.parse.CorrelationFunctionInfo;
import jolie.lang.parse.CorrelationFunctionInfo.CorrelationPairInfo;
import jolie.lang.parse.OLParser;
import jolie.lang.parse.OLVisitor;
import jolie.lang.parse.context.ParsingContext;
import jolie.lang.parse.Scanner;
import jolie.lang.parse.ast.AddAssignStatement;
import jolie.lang.parse.ast.AndConditionNode;
import jolie.lang.parse.ast.AssignStatement;
import jolie.lang.parse.ast.CompareConditionNode;
import jolie.lang.parse.ast.CompensateStatement;
import jolie.lang.parse.ast.ConstantIntegerExpression;
import jolie.lang.parse.ast.ConstantRealExpression;
import jolie.lang.parse.ast.ConstantStringExpression;
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
import jolie.lang.parse.ast.ExpressionConditionNode;
import jolie.lang.parse.ast.ForEachStatement;
import jolie.lang.parse.ast.ForStatement;
import jolie.lang.parse.ast.IfStatement;
import jolie.lang.parse.ast.InputPortInfo;
import jolie.lang.parse.ast.InstallFixedVariableExpressionNode;
import jolie.lang.parse.ast.InstallFunctionNode;
import jolie.lang.parse.ast.InstallStatement;
import jolie.lang.parse.ast.InterfaceDefinition;
import jolie.lang.parse.ast.IsTypeExpressionNode;
import jolie.lang.parse.ast.LinkInStatement;
import jolie.lang.parse.ast.LinkOutStatement;
import jolie.lang.parse.ast.SubtractAssignStatement;
import jolie.lang.parse.ast.MultiplyAssignStatement;
import jolie.lang.parse.ast.NDChoiceStatement;
import jolie.lang.parse.ast.NotConditionNode;
import jolie.lang.parse.ast.NotificationOperationStatement;
import jolie.lang.parse.ast.NullProcessStatement;
import jolie.lang.parse.ast.OLSyntaxNode;
import jolie.lang.parse.ast.OneWayOperationDeclaration;
import jolie.lang.parse.ast.OneWayOperationStatement;
import jolie.lang.parse.ast.OperationDeclaration;
import jolie.lang.parse.ast.OrConditionNode;
import jolie.lang.parse.ast.OutputPortInfo;
import jolie.lang.parse.ast.ParallelStatement;
import jolie.lang.parse.ast.PointerStatement;
import jolie.lang.parse.ast.PostDecrementStatement;
import jolie.lang.parse.ast.PostIncrementStatement;
import jolie.lang.parse.ast.PreDecrementStatement;
import jolie.lang.parse.ast.PreIncrementStatement;
import jolie.lang.parse.ast.ProductExpressionNode;
import jolie.lang.parse.ast.Program;
import jolie.lang.parse.ast.RequestResponseOperationDeclaration;
import jolie.lang.parse.ast.RequestResponseOperationStatement;
import jolie.lang.parse.ast.RunStatement;
import jolie.lang.parse.ast.Scope;
import jolie.lang.parse.ast.SequenceStatement;
import jolie.lang.parse.ast.SolicitResponseOperationStatement;
import jolie.lang.parse.ast.SpawnStatement;
import jolie.lang.parse.ast.SumExpressionNode;
import jolie.lang.parse.ast.SynchronizedStatement;
import jolie.lang.parse.ast.ThrowStatement;
import jolie.lang.parse.ast.TypeCastExpressionNode;
import jolie.lang.parse.ast.UndefStatement;
import jolie.lang.parse.ast.ValueVectorSizeExpressionNode;
import jolie.lang.parse.ast.VariableExpressionNode;
import jolie.lang.parse.ast.VariablePathNode;
import jolie.lang.parse.ast.WhileStatement;
import jolie.lang.parse.ast.types.TypeDefinition;
import jolie.lang.parse.ast.types.TypeDefinitionLink;
import jolie.lang.parse.ast.types.TypeInlineDefinition;
import jolie.net.ports.OutputPort;
import jolie.net.ext.CommProtocolFactory;
import jolie.net.ports.Interface;
import jolie.process.AddAssignmentProcess;
import jolie.process.AssignmentProcess;
import jolie.process.CallProcess;
import jolie.process.CompensateProcess;
import jolie.process.CurrentHandlerProcess;
import jolie.process.DeepCopyProcess;
import jolie.process.DefinitionProcess;
import jolie.process.DivideAssignmentProcess;
import jolie.process.ExitProcess;
import jolie.process.ForEachProcess;
import jolie.process.ForProcess;
import jolie.process.IfProcess;
import jolie.process.InputOperationProcess;
import jolie.process.InstallProcess;
import jolie.process.LinkInProcess;
import jolie.process.LinkOutProcess;
import jolie.process.InitDefinitionProcess;
import jolie.process.MakePointerProcess;
import jolie.process.SubtractAssignmentProcess;
import jolie.process.MultiplyAssignmentProcess;
import jolie.process.NDChoiceProcess;
import jolie.process.NotificationProcess;
import jolie.process.NullProcess;
import jolie.process.OneWayProcess;
import jolie.process.ParallelProcess;
import jolie.process.PostDecrementProcess;
import jolie.process.PostIncrementProcess;
import jolie.process.PreDecrementProcess;
import jolie.process.PreIncrementProcess;
import jolie.process.Process;
import jolie.process.RequestResponseProcess;
import jolie.process.RunProcess;
import jolie.process.ScopeProcess;
import jolie.process.SequentialProcess;
import jolie.process.SolicitResponseProcess;
import jolie.process.SpawnProcess;
import jolie.process.SynchronizedProcess;
import jolie.process.ThrowProcess;
import jolie.process.UndefProcess;
import jolie.process.WhileProcess;
import jolie.runtime.AggregatedOperation;
import jolie.runtime.AndCondition;
import jolie.runtime.CastIntExpression;
import jolie.runtime.CastRealExpression;
import jolie.runtime.CastStringExpression;
import jolie.runtime.CompareCondition;
import jolie.runtime.CompareOperator;
import jolie.runtime.Condition;
import jolie.runtime.embedding.EmbeddedServiceLoader;
import jolie.runtime.embedding.EmbeddedServiceLoaderCreationException;
import jolie.runtime.Expression;
import jolie.runtime.Expression.Operand;
import jolie.runtime.ExpressionCondition;
import jolie.runtime.GlobalVariablePath;
import jolie.runtime.InstallFixedVariablePath;
import jolie.runtime.InvalidIdException;
import jolie.runtime.IsDefinedExpression;
import jolie.runtime.IsIntExpression;
import jolie.runtime.IsRealExpression;
import jolie.runtime.IsStringExpression;
import jolie.runtime.NotCondition;
import jolie.runtime.OneWayOperation;
import jolie.runtime.OrCondition;
import jolie.runtime.ProductExpression;
import jolie.runtime.RequestResponseOperation;
import jolie.runtime.SumExpression;
import jolie.runtime.Value;
import jolie.runtime.ValueVectorSizeExpression;
import jolie.runtime.VariablePath;
import jolie.runtime.VariablePathBuilder;
import jolie.runtime.correlation.CorrelationSet;
import jolie.runtime.correlation.CorrelationSet.CorrelationPair;
import jolie.runtime.typing.OneWayTypeDescription;
import jolie.runtime.typing.RequestResponseTypeDescription;
import jolie.runtime.typing.Type;
import jolie.util.ArrayListMultiMap;
import jolie.util.MultiMap;
import jolie.util.Pair;

/**
 * Builds an interpretation tree by visiting a Jolie abstract syntax tree.
 * @author Fabrizio Montesi
 * @see OLVisitor
 */
public class OOITBuilder implements OLVisitor
{
	private Program program;
	private boolean valid = true;
	private final Interpreter interpreter;
	private String currentOutputPort = null;
	private Interface currentPortInterface = null;
	private final Map< String, Boolean > isConstantMap;
	private final List< Pair< Type.TypeLink, TypeDefinition > > typeLinks = new LinkedList< Pair< Type.TypeLink, TypeDefinition > >();
	private final List< CorrelationSetInfo > correlationSetInfoList = new LinkedList< CorrelationSetInfo >();
	private final CorrelationFunctionInfo correlationFunctionInfo;
	private ExecutionMode executionMode = Constants.ExecutionMode.SINGLE;
	private boolean registerSessionStarters = false;

	/**
	 * Constructor.
	 * @param interpreter the Interpreter requesting the interpretation tree building
	 * @param program the Program to generate the interpretation tree from
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
		buildCorrelationSets();
		
		return valid;
	}

	private void checkForInit()
	{
		try {
			interpreter.getDefinition( "init" );
		} catch( InvalidIdException e ) {
			interpreter.register( "init",
			new InitDefinitionProcess(
				new ScopeProcess( "main", new InstallProcess( SessionThread.createDefaultFaultHandlers( interpreter ) ), false )
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
		Set< Interpreter.SessionStarter > starters = new HashSet< Interpreter.SessionStarter >();
		List< VariablePath > correlationVariablePaths;
		MultiMap< String, CorrelationPair > correlationMap;

		for( CorrelationSetInfo csetInfo : correlationFunctionInfo.correlationSets() ) {
			correlationVariablePaths = new ArrayList< VariablePath >();
			correlationMap = new ArrayListMultiMap< String, CorrelationPair >();

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
		
		/*for( CorrelationSetInfo csetInfo : correlationSetInfoList ) {
			correlationVariablePaths = new ArrayList< VariablePath >();
			correlationMap = new ArrayListMultiMap< String, CorrelationPair >();
			
			for( CorrelationVariableInfo csetVariableInfo : csetInfo.variables() ) {
				sessionVariablePath = buildCorrelationVariablePath( csetVariableInfo.correlationVariablePath() );
				correlationVariablePaths.add( sessionVariablePath );
				for( CorrelationSetInfo.CorrelationAliasInfo aliasInfo : csetVariableInfo.aliases() ) {
					operations = inputTypeNameMap.get( aliasInfo.guardName() );
					messageVariablePath = buildVariablePath( aliasInfo.variablePath() );
					for( String operationName : operations ) {
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
			}
			currCorrelationSet = new CorrelationSet( correlationVariablePaths, correlationMap );
			interpreter.addCorrelationSet( currCorrelationSet );
			for( Interpreter.SessionStarter initStarter : starters ) {
				initStarter.setCorrelationInitializer( currCorrelationSet );
			}
			starters.clear();
		}*/
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
		correlationSetInfoList.add( n );
	}

	public void visit( OutputPortInfo n )
	{
		Process protocolConfigurationProcess = NullProcess.getInstance();
		if ( n.protocolConfiguration() != null ) {
			n.protocolConfiguration().accept( this );
			protocolConfigurationProcess = currProcess;
		}

		Boolean isConstant;
		if ( (isConstant = isConstantMap.get( n.id() )) == null ) {
			isConstant = false;
		}

		currentOutputPort = n.id();
		notificationTypes.put( currentOutputPort, new HashMap< String, OneWayTypeDescription >() );
		solicitResponseTypes.put( currentOutputPort, new HashMap< String, RequestResponseTypeDescription >() );
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
		VariablePath path = null;
		try {
			path = interpreter.getOutputPort( n.portId() ).locationVariablePath();
		} catch( InvalidIdException iie ) {}

		try {
			interpreter.addEmbeddedServiceLoader(
				EmbeddedServiceLoader.create(
						interpreter,
						n.type(),
						n.servicePath(),
						path
						) );
		} catch( EmbeddedServiceLoaderCreationException e ) {
			error( n.context(), e );
		}
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
		for( String outputPortName : n.aggregationList() ) {
			try {
				outputPort = interpreter.getOutputPort( outputPortName );
				outputPortNotificationTypes = notificationTypes.get( outputPortName );
				outputPortSolicitResponseTypes = solicitResponseTypes.get( outputPortName );
				for( String operationName : outputPortNotificationTypes.keySet() ) {
					aggregationMap.put( operationName, new AggregatedOperation( operationName, Constants.OperationType.ONE_WAY, outputPort ) );
				}
				for( String operationName : outputPortSolicitResponseTypes.keySet() ) {
					aggregationMap.put( operationName, new AggregatedOperation( operationName, Constants.OperationType.REQUEST_RESPONSE, outputPort ) );
				}
			} catch( InvalidIdException e ) {
				error( n.context(), e );
			}
		}
		
		if ( n.location().toString().equals( Constants.LOCAL_LOCATION_KEYWORD ) ) {
			try {
				interpreter.commCore().addLocalInputPort( n.id(), currentPortInterface, aggregationMap, redirectionMap );
			} catch( IOException e ) {
				error( n.context(), e );
			}
			return;
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

		VariablePath path =
			new VariablePathBuilder( true )
			.add( Constants.INPUT_PORTS_NODE_NAME, 0 )
			.add( n.id(), 0 )
			.add( Constants.LOCATION_NODE_NAME, 0 )
			.toVariablePath();
		Process assignLocation = new AssignmentProcess( path, Value.create( n.location().toString() ) );
		path =
			new VariablePathBuilder( true )
			.add( Constants.INPUT_PORTS_NODE_NAME, 0 )
			.add( n.id(), 0 )
			.add( Constants.PROTOCOL_NODE_NAME, 0 )
			.toVariablePath();
		Process assignProtocol = new AssignmentProcess( path, Value.create( n.protocolId() ) );
		Process[] confChildren = new Process[] { assignLocation, assignProtocol, buildProcess( n.protocolConfiguration() ) };
		SequentialProcess protocolConfigurationSequence = new SequentialProcess( confChildren );

		if ( protocolFactory != null ) {
			try {
				interpreter.commCore().addInputPort(
					n.id(),
					n.location(),
					protocolFactory,
					protocolConfigurationPath,
					protocolConfigurationSequence,
					currentPortInterface,
					aggregationMap,
					redirectionMap
				);
			} catch( IOException ioe ) {
				error( n.context(), ioe );
			}
		} else {
			error( n.context(), "Communication protocol extension for protocol " + pId + " not found." );
		}
		currentPortInterface = null;
	}

	private Process currProcess;
	private Expression currExpression;
	private Condition currCondition;
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
			currType = Type.create( n.nativeType(), n.cardinality(), false, subTypes );
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
		typeLinks.add( new Pair< Type.TypeLink, TypeDefinition >( link, n ) );

		if ( insideType == false && insideOperationDeclaration == false ) {
			types.put( n.id(), currType );
		}
		/*if ( n.untypedSubTypes() ) {
			currType = new Type( n.nativeType(), n.cardinality(), true, null );
		} else {
			Map< String, Type > subTypes = new HashMap< String, Type >();
			if ( n.subTypes() != null ) {
				for( Entry< String, TypeDefinition > entry : n.subTypes() ) {
					subTypes.put( entry.getKey(), buildType( entry.getValue() ) );
				}
			}
			currType = new Type( n.nativeType(), n.cardinality(), false, subTypes );
		}*/
	}

	public void visit( Program p )
	{
		for( OLSyntaxNode node : p.children() )
			node.accept( this );
	}

	private boolean insideOperationDeclaration = false;

	public void visit( OneWayOperationDeclaration decl )
	{
		boolean backup = insideOperationDeclaration;
		insideOperationDeclaration = true;
		OneWayTypeDescription typeDescription = null;
		if ( currentOutputPort == null ) {
			// Register if not already present
			typeDescription = new OneWayTypeDescription( types.get( decl.requestType().id() ) );
			try {
				interpreter.getOneWayOperation( decl.id() );
			} catch( InvalidIdException e ) {
				interpreter.register( decl.id(), new OneWayOperation( decl.id(), types.get( decl.requestType().id() ) ) );
			}
		} else {
			typeDescription = new OneWayTypeDescription( buildType( decl.requestType() ) );
			notificationTypes.get( currentOutputPort ).put( decl.id(), typeDescription );
		}

		if ( currentPortInterface != null ) {
			currentPortInterface.oneWayOperations().put( decl.id(), typeDescription );
		}

		insideOperationDeclaration = backup;
	}

	public void visit( RequestResponseOperationDeclaration decl )
	{
		RequestResponseTypeDescription typeDescription = null;
		if ( currentOutputPort == null ) {
			// Register if not already present
			try {
				RequestResponseOperation op = interpreter.getRequestResponseOperation( decl.id() );
				typeDescription = op.typeDescription();
			} catch( InvalidIdException e ) {
				Map< String, Type > faults = new HashMap< String, Type >();
				for( Entry< String, TypeDefinition > entry : decl.faults().entrySet() ) {
					faults.put( entry.getKey(), types.get( entry.getValue().id() ) );
				}
				typeDescription = new RequestResponseTypeDescription(
					types.get( decl.requestType().id() ),
					types.get( decl.responseType().id() ),
					faults
				);
				interpreter.register(
					decl.id(),
					new RequestResponseOperation( decl.id(), typeDescription )
				);
			}
		} else {
			Type requestType, responseType;
			Map< String, Type > faultTypes = new HashMap< String, Type >();
			requestType = buildType( decl.requestType() );
			responseType = buildType( decl.responseType() );
			for( Entry< String, TypeDefinition > entry : decl.faults().entrySet() ) {
				faultTypes.put( entry.getKey(), types.get( entry.getValue().id() ) );
			}
			typeDescription = new RequestResponseTypeDescription( requestType, responseType, faultTypes );
			solicitResponseTypes.get( currentOutputPort ).put( decl.id(), typeDescription );
		}

		if ( currentPortInterface != null ) {
			currentPortInterface.requestResponseOperations().put( decl.id(), typeDescription );
		}
	}
	
	public void visit( DefinitionNode n )
	{
		DefinitionProcess def;
		
		if ( "main".equals( n.id() ) ) {
			if ( executionMode == ExecutionMode.SINGLE ) {
				// We are a single-session service, so we will not spawn sessions
				registerSessionStarters = false;
				buildProcess( n.body() );
			} else {
				registerSessionStarters = true;
				buildProcess( n.body() );
				registerSessionStarters = false;
			}
			def = new DefinitionProcess( currProcess );
		} else if ( "init".equals( n.id() ) ) {
			Process[] initChildren = {
				new InstallProcess( SessionThread.createDefaultFaultHandlers( interpreter ) ),
				buildProcess( n.body() )
			};

			def = new InitDefinitionProcess( new ScopeProcess( "main", new SequentialProcess( initChildren ), false ) );
		} else {
			def = new DefinitionProcess( buildProcess( n.body() ) );
		}

		interpreter.register( n.id(), def );
	}

	public void visit( ParallelStatement n )
	{
		Process[] children = new Process[ n.children().size() ];
		int i = 0;
		for( OLSyntaxNode node : n.children() ) {
			node.accept( this );
			children[ i++ ] = currProcess;
		}
		currProcess = new ParallelProcess( children );
	}
	
	public void visit( SynchronizedStatement n )
	{
		n.body().accept( this );
		currProcess = new SynchronizedProcess( n.id(), currProcess );
	}
		
	public void visit( SequenceStatement n )
	{
		boolean origRegisterSessionStarters = registerSessionStarters;
		registerSessionStarters = false;

		List< Process > children = new LinkedList< Process >();
		for( OLSyntaxNode child : n.children() ) {
			children.add( buildProcess( child ) );
		}
		currProcess = new SequentialProcess( children.toArray( new Process[0] ) );

		if ( origRegisterSessionStarters && children.get( 0 ) instanceof InputOperationProcess ) {
			// We must register this sequence as a starter guarded by the first input process
			InputOperationProcess first = (InputOperationProcess) children.remove( 0 );
			registerSessionStarter(
				first,
				new SequentialProcess( children.toArray( new Process[0] ) )
			);
		}

		registerSessionStarters = origRegisterSessionStarters;
	}

	private void registerSessionStarter( InputOperationProcess guard, Process body )
	{
		interpreter.registerSessionStarter( guard, body );
	}

	public void visit( NDChoiceStatement n )
	{
		boolean origRegisterSessionStarters = registerSessionStarters;
		registerSessionStarters = false;

		List< Pair< InputOperationProcess, Process > > branches =
					new LinkedList< Pair< InputOperationProcess, Process > >();
		InputOperationProcess guard;
		for( Pair< OLSyntaxNode, OLSyntaxNode > pair : n.children() ) {
			pair.key().accept( this );
			try {
				guard = (InputOperationProcess) currProcess;
				pair.value().accept( this );
				branches.add(
					new Pair< InputOperationProcess, Process >( guard, currProcess )
				);
				if ( origRegisterSessionStarters ) {
					registerSessionStarter( guard, currProcess );
				}
			} catch( Exception e ) {
				e.printStackTrace();
			}
		}
		
		currProcess = new NDChoiceProcess( branches.toArray( new Pair[0] ) );
		
		registerSessionStarters = origRegisterSessionStarters;
	}
		
	public void visit( OneWayOperationStatement n )
	{
		boolean origRegisterSessionStarters = registerSessionStarters;
		registerSessionStarters = false;

		try {
			InputOperationProcess inputProcess;
			currProcess = inputProcess =
				new OneWayProcess(
						interpreter.getOneWayOperation( n.id() ),
						buildVariablePath( n.inputVarPath() )
						);
			if ( origRegisterSessionStarters ) {
				registerSessionStarter( inputProcess, NullProcess.getInstance() );
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
			InputOperationProcess inputProcess;
			currProcess = inputProcess =
				new RequestResponseProcess(
						interpreter.getRequestResponseOperation( n.id() ),
						buildVariablePath( n.inputVarPath() ),
						outputExpression,
						buildProcess( n.process() )
						);
			if ( origRegisterSessionStarters ) {
				registerSessionStarter( inputProcess, NullProcess.getInstance() );
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
				new NotificationProcess(
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
		Expression outputExpression = null;
		if ( n.outputExpression() != null ) {
			n.outputExpression().accept( this );
			outputExpression = currExpression;
		}
		try {
			Process installProcess = NullProcess.getInstance();
			if ( n.handlersFunction() != null )
				installProcess = new InstallProcess( getHandlersFunction( n.handlersFunction() ) );
			currProcess =
				new SolicitResponseProcess(
						n.id(),
						interpreter.getOutputPort( n.outputPortId() ),
						outputExpression,
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
		currProcess = new LinkInProcess( n.id() );
	}
	
	public void visit( LinkOutStatement n )
	{
		currProcess = new LinkOutProcess( n.id() );
	}
	
	public void visit( ThrowStatement n )
	{
		Expression expression = null;
		if ( n.expression() != null ) {
			n.expression().accept( this );
			expression = currExpression;
		}
		currProcess = new ThrowProcess( n.id(), expression );
	}
	
	public void visit( CompensateStatement n )
	{
		currProcess = new CompensateProcess( n.id() );
	}
		
	public void visit( Scope n )
	{
		n.body().accept( this );
		currProcess = new ScopeProcess( n.id(), currProcess );
	}
		
	public void visit( InstallStatement n )
	{
		currProcess = new InstallProcess( getHandlersFunction( n.handlersFunction() ) );
	}
	
	private List< Pair< String, Process > > getHandlersFunction( InstallFunctionNode n )
	{
		List< Pair< String, Process > > pairs = new LinkedList< Pair< String, Process > >();
		for( Pair< String, OLSyntaxNode > pair : n.pairs() ) {
			pair.value().accept( this );
			pairs.add( new Pair< String, Process >( pair.key(), currProcess ) );
		}
		return pairs;
	}
			
	public void visit( AssignStatement n )
	{
		n.expression().accept( this );
			
		AssignmentProcess p = 
			new AssignmentProcess(
				buildVariablePath( n.variablePath() ),
				currExpression
				);
		currProcess = p;
		currExpression = p;
	}

	public void visit( AddAssignStatement n )
	{
		n.expression().accept( this );

		AddAssignmentProcess p =
			new AddAssignmentProcess(
			buildVariablePath( n.variablePath() ),
			currExpression );
		currProcess = p;
		currExpression = p;
	}

	public void visit( SubtractAssignStatement n )
	{
		n.expression().accept( this );

		SubtractAssignmentProcess p =
			new SubtractAssignmentProcess(
			buildVariablePath( n.variablePath() ),
			currExpression );
		currProcess = p;
		currExpression = p;
	}

	public void visit( MultiplyAssignStatement n )
	{
		n.expression().accept( this );

		MultiplyAssignmentProcess p =
			new MultiplyAssignmentProcess(
			buildVariablePath( n.variablePath() ),
			currExpression );
		currProcess = p;
		currExpression = p;
	}

	public void visit( DivideAssignStatement n )
	{
		n.expression().accept( this );

		DivideAssignmentProcess p =
			new DivideAssignmentProcess(
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
		
		Expression backupExpr = currExpression;
		
		LinkedList< Pair< Expression, Expression > > list =
							new LinkedList< Pair< Expression, Expression > >();
		for( Pair< OLSyntaxNode, OLSyntaxNode > pair : path.path() ) {
			pair.key().accept( this );
			Expression keyExpr = currExpression;
			if ( pair.value() != null ) {
				pair.value().accept( this );
			} else {
				currExpression = null;
			}
			list.add( new Pair< Expression, Expression >( keyExpr, currExpression ) );
		}
		
		currExpression = backupExpr;

		Pair< Expression, Expression >[] internalPath = new Pair[ list.size() ];
		for( int i = 0; i < internalPath.length; i++ ) {
			internalPath[i] = list.get( i );
		}

		if ( path.isGlobal() ) {
			return new GlobalVariablePath( internalPath );
		} else {
			return new VariablePath( internalPath );
		}
	}
	
	public void visit( PointerStatement n )
	{
		currProcess =
			new MakePointerProcess(
				buildVariablePath( n.leftPath() ),
				buildVariablePath( n.rightPath() )
				);
	}
	
	public void visit( DeepCopyStatement n )
	{
		currProcess =
			new DeepCopyProcess(
				buildVariablePath( n.leftPath() ),
				buildVariablePath( n.rightPath() )
				);
	}
	
	public void visit( IfStatement n )
	{
		IfProcess.CPPair[] pairs = new IfProcess.CPPair[ n.children().size() ];
		Process elseProcess = null;

		Condition condition;
		int i = 0;
		for( Pair< OLSyntaxNode, OLSyntaxNode > pair : n.children() ) {
			pair.key().accept( this );
			condition = currCondition;
			pair.value().accept( this );
			pairs[ i++ ] = new IfProcess.CPPair( condition, currProcess );
		}
		
		if ( n.elseProcess() != null ) {
			n.elseProcess().accept( this );
			elseProcess = currProcess;
		}
		
		currProcess = new IfProcess( pairs, elseProcess );
	}
	
	public void visit( CurrentHandlerStatement n )
	{
		currProcess = CurrentHandlerProcess.getInstance();
	}

	public void visit( DefinitionCallStatement n )
	{
		try {
			currProcess = new CallProcess( interpreter.getDefinition( n.id() ) );
		} catch( InvalidIdException e ) {
			error( n.context(), e );
		}
	}
	
	public void visit( RunStatement n )
	{
		n.expression().accept( this );
		currProcess = new RunProcess( currExpression );
	}
	
	public void visit( WhileStatement n )
	{
		n.condition().accept( this );
		Condition condition = currCondition;
		n.body().accept( this );
		currProcess = new WhileProcess( condition, currProcess );
	}
	
	public void visit( OrConditionNode n )
	{
		Condition[] children = new Condition[ n.children().size() ];
		int i = 0;
		for( OLSyntaxNode node : n.children() ) {
			node.accept( this );
			children[ i++ ] = currCondition;
		}
		currCondition = new OrCondition( children );
	}
	
	public void visit( AndConditionNode n )
	{
		Condition[] children = new Condition[ n.children().size() ];
		int i = 0;
		for( OLSyntaxNode node : n.children() ) {
			node.accept( this );
			children[ i++ ] = currCondition;
		}
		currCondition = new AndCondition( children );
	}
	
	public void visit( NotConditionNode n )
	{
		n.condition().accept( this );
		currCondition = new NotCondition( currCondition );
	}
	
	public void visit( CompareConditionNode n )
	{
		n.leftExpression().accept( this );
		Expression left = currExpression;
		n.rightExpression().accept( this );
		CompareOperator operator = null;
		Scanner.TokenType opType = n.opType();
		if ( opType == Scanner.TokenType.EQUAL ) {
			operator = CompareOperator.EQUAL;
		} else if ( opType == Scanner.TokenType.NOT_EQUAL ) {
			operator = CompareOperator.NOT_EQUAL;
		} else if ( opType == Scanner.TokenType.LANGLE ) {
			operator = CompareOperator.MINOR;
		} else if ( opType == Scanner.TokenType.RANGLE ) {
			operator = CompareOperator.MAJOR;
		} else if ( opType == Scanner.TokenType.MINOR_OR_EQUAL ) {
			operator = CompareOperator.MINOR_OR_EQUAL;
		} else if ( opType == Scanner.TokenType.MAJOR_OR_EQUAL ) {
			operator = CompareOperator.MAJOR_OR_EQUAL;
		}
		assert( operator != null );
		currCondition = new CompareCondition( left, currExpression, operator );
	}
	
	public void visit( ExpressionConditionNode n )
	{
		n.expression().accept( this );
		currCondition = new ExpressionCondition( currExpression );
	}
	
	public void visit( ConstantIntegerExpression n )
	{
		currExpression = Value.create( n.value() );
	}
	
	public void visit( ConstantRealExpression n )
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
		currProcess = NullProcess.getInstance();
	}
	
	public void visit( ExitStatement n )
	{
		currProcess = ExitProcess.getInstance();
	}
	
	public void visit( ValueVectorSizeExpressionNode n )
	{
		currExpression = new ValueVectorSizeExpression( buildVariablePath( n.variablePath() ) );
	}
	
	public void visit( PreDecrementStatement n )
	{
		PreDecrementProcess p =
			new PreDecrementProcess( buildVariablePath( n.variablePath() ) );
		currProcess = p;
		currExpression = p; 
	}
	
	public void visit( PostDecrementStatement n )
	{
		PostDecrementProcess p =
			new PostDecrementProcess( buildVariablePath( n.variablePath() ) );
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
		} else if ( type == IsTypeExpressionNode.CheckType.REAL ) {
			currExpression =
				new IsRealExpression( buildVariablePath( n.variablePath() ) );
		} else if ( type == IsTypeExpressionNode.CheckType.STRING ) {
			currExpression =
				new IsStringExpression( buildVariablePath( n.variablePath() ) );
		}
	}
	
	public void visit( TypeCastExpressionNode n )
	{
		n.expression().accept( this );
		if ( n.type() == NativeType.INT ) {
			currExpression = new CastIntExpression( currExpression );
		} else if ( n.type() == NativeType.DOUBLE ) {
			currExpression = new CastRealExpression( currExpression );
		} else if ( n.type() == NativeType.STRING ) {
			currExpression = new CastStringExpression( currExpression );
		}
	}
	
	public void visit( PreIncrementStatement n )
	{
		PreIncrementProcess p =
			new PreIncrementProcess( buildVariablePath( n.variablePath() ) );
		currProcess = p;
		currExpression = p; 
	}
	
	public void visit( PostIncrementStatement n )
	{
		PostIncrementProcess p =
			new PostIncrementProcess( buildVariablePath( n.variablePath() ) );
		currProcess = p;
		currExpression = p;
	}
	
	public void visit( ForStatement n )
	{
		n.init().accept( this );
		Process init = currProcess;
		n.post().accept( this );
		Process post = currProcess;
		n.condition().accept( this );
		Condition condition = currCondition;
		n.body().accept( this );
		currProcess = new ForProcess( init, condition, post, currProcess );
	}
	
	public void visit( ForEachStatement n )
	{
		n.body().accept( this );
		currProcess =
			new ForEachProcess(
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

	private Process buildProcess( OLSyntaxNode n )
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
		Process[] children = new Process[2];
		children[ 0 ] = new InstallProcess( SessionThread.createDefaultFaultHandlers( interpreter ) );
		children[ 1 ] = buildProcess( n.body() );
		currProcess = new SpawnProcess(
			buildVariablePath( n.indexVariablePath() ),
			buildExpression( n.upperBoundExpression() ),
			buildVariablePath( n.inVariablePath() ),
			new SequentialProcess( children )
		);
	}
	
	public void visit( UndefStatement n )
	{
		currProcess = new UndefProcess( buildVariablePath( n.variablePath() ) );
	}

	public void visit( InterfaceDefinition n ) {}
	public void visit( DocumentationComment n ) {}
}

